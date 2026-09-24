package com.andy.englishcoach.billing

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult as PlayBillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.TimeoutCancellationException
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap

/**
 * Production BillingRepository connected to the official Google Play Billing Library.
 *
 * Architecture:
 * UI / ViewModel -> BillingRepository -> GooglePlayBillingRepository -> BillingClientAdapter -> BillingClient
 *
 * Implements complete purchase lifecycle:
 * Purchase -> Purchase Result -> Acknowledge -> Entitlement -> Restore
 *
 * Important Architecture & Security Invariants:
 * 1. Client-side billing state != Server verified purchase (Server verification in STEP 16-D).
 * 2. Purchase tokens are sensitive transaction data and must NEVER be logged, rendered in UI, or leaked.
 * 3. Only PurchaseState.PURCHASED can receive entitlement and acknowledgement.
 * 4. PENDING purchases must not grant Premium.
 * 5. Already acknowledged purchases must not be re-acknowledged.
 * 6. User cancellation preserves existing entitlement state.
 */
class GooglePlayBillingRepository(
    context: Context,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob()),
    private val activityProvider: (() -> Activity?)? = null,
    adapter: BillingClientAdapter? = null
) : BillingRepository {

    private val _entitlement = MutableStateFlow<PremiumEntitlement>(PremiumEntitlement.Free)
    override val entitlement: StateFlow<PremiumEntitlement> = _entitlement.asStateFlow()

    private val _connectionState = MutableStateFlow<BillingConnectionState>(BillingConnectionState.Disconnected)
    override val connectionState: StateFlow<BillingConnectionState> = _connectionState.asStateFlow()

    private val _catalogState = MutableStateFlow<BillingProductCatalog>(BillingProductCatalog.Loading)
    override val catalogState: StateFlow<BillingProductCatalog> = _catalogState.asStateFlow()

    private val productDetailsCache = ConcurrentHashMap<String, ProductDetails>()
    private val processedPurchaseTokens = Collections.synchronizedSet(mutableSetOf<String>())
    private val recordedPurchases = Collections.synchronizedList(mutableListOf<PurchaseRecord>())

    val records: List<PurchaseRecord>
        get() = synchronized(recordedPurchases) { recordedPurchases.toList() }

    var currentActivity: Activity? = null

    private var pendingPurchaseDeferred: CompletableDeferred<BillingResult>? = null
    private var pendingTargetProduct: PremiumProduct? = null

    private val purchasesUpdatedListener = PurchasesUpdatedListener { billingResult, purchases ->
        handlePurchasesUpdated(billingResult, purchases)
    }

    private val internalClient: BillingClient? = if (adapter == null) {
        BillingClient.newBuilder(context.applicationContext)
            .setListener(purchasesUpdatedListener)
            .enablePendingPurchases(
                PendingPurchasesParams.newBuilder()
                    .enableOneTimeProducts()
                    .build()
            )
            .build()
    } else null

    private val clientAdapter: BillingClientAdapter = adapter ?: DefaultBillingClientAdapter(internalClient!!)

    init {
        startConnection()
    }

    /**
     * Connects to Google Play Billing Service and initiates product & purchase synchronization.
     */
    fun startConnection() {
        if (_connectionState.value == BillingConnectionState.Connecting ||
            _connectionState.value == BillingConnectionState.Connected
        ) {
            return
        }

        _connectionState.value = BillingConnectionState.Connecting
        clientAdapter.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: PlayBillingResult) {
                Log.i(TAG, "onBillingSetupFinished: code=${billingResult.responseCode}, debugMessage='${billingResult.debugMessage}'")
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    _connectionState.value = BillingConnectionState.Connected
                    scope.launch {
                        refreshProductCatalog()
                        restorePurchasesSilently()
                    }
                } else {
                    val error = BillingError.fromBillingResponseCode(billingResult.responseCode)
                    Log.e(TAG, "onBillingSetupFinished FAILED: code=${billingResult.responseCode}, error=$error, debugMessage='${billingResult.debugMessage}'")
                    _connectionState.value = BillingConnectionState.Unavailable(
                        billingResult.debugMessage.ifBlank { error.toUserFacingMessage() }
                    )
                    _catalogState.value = BillingProductCatalog.Error(error, billingResult.debugMessage)
                }
            }

            override fun onBillingServiceDisconnected() {
                Log.w(TAG, "onBillingServiceDisconnected")
                _connectionState.value = BillingConnectionState.Disconnected
            }
        })
    }

    /**
     * Queries Google Play for registered product IDs and caches ProductDetails.
     */
    suspend fun refreshProductCatalog(): BillingProductCatalog {
        if (!clientAdapter.isReady) {
            val error = BillingProductCatalog.Error(BillingError.SERVICE_UNAVAILABLE, "BillingClient is not ready")
            _catalogState.value = error
            return error
        }

        _catalogState.value = BillingProductCatalog.Loading

        return withContext(Dispatchers.IO) {
            try {
                // Subscription products (MONTHLY, ANNUAL) must be queried with ProductType.SUBS
                val subsProductList = listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(PremiumProduct.MONTHLY.productId)
                        .setProductType(BillingClient.ProductType.SUBS)
                        .build(),
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(PremiumProduct.ANNUAL.productId)
                        .setProductType(BillingClient.ProductType.SUBS)
                        .build()
                )

                // One-time products (LIFETIME) must be queried separately with ProductType.INAPP
                // (Google Play Billing Library requires all products in a query to have the same productType)
                val inAppProductList = listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(PremiumProduct.LIFETIME.productId)
                        .setProductType(BillingClient.ProductType.INAPP)
                        .build()
                )

                val subsParams = QueryProductDetailsParams.newBuilder()
                    .setProductList(subsProductList)
                    .build()

                val inAppParams = QueryProductDetailsParams.newBuilder()
                    .setProductList(inAppProductList)
                    .build()

                val subsResult = clientAdapter.queryProductDetails(subsParams)
                val subsCode = subsResult.billingResult.responseCode
                Log.i(TAG, "queryProductDetails (SUBS) responseCode=$subsCode, debugMessage='${subsResult.billingResult.debugMessage}', count=${subsResult.productDetailsList?.size ?: 0}")

                val inAppResult = clientAdapter.queryProductDetails(inAppParams)
                val inAppCode = inAppResult.billingResult.responseCode
                Log.i(TAG, "queryProductDetails (INAPP) responseCode=$inAppCode, debugMessage='${inAppResult.billingResult.debugMessage}', count=${inAppResult.productDetailsList?.size ?: 0}")

                val isAnyOk = subsCode == BillingClient.BillingResponseCode.OK || inAppCode == BillingClient.BillingResponseCode.OK
                if (isAnyOk) {
                    val rawDetailsList = subsResult.productDetailsList.orEmpty() + inAppResult.productDetailsList.orEmpty()
                    rawDetailsList.forEach { details ->
                        Log.i(TAG, "  [ProductDetails] id=${details.productId}, type=${details.productType}, title='${details.title}'")
                        details.subscriptionOfferDetails?.forEach { subOffer ->
                            Log.i(TAG, "    SubOffer: offerId=${subOffer.offerId}, basePlanId=${subOffer.basePlanId}, tokenSuffix=${subOffer.offerToken.takeLast(4)}")
                        }
                        val oneTime = details.oneTimePurchaseOfferDetails
                        if (oneTime != null) {
                            Log.i(TAG, "    OneTimeOffer: price=${oneTime.formattedPrice}, currency=${oneTime.priceCurrencyCode}, optionId=${oneTime.purchaseOptionId}, offerId=${oneTime.offerId}, tokenSuffix=${oneTime.offerToken?.takeLast(4)}")
                        }
                        details.oneTimePurchaseOfferDetailsList?.forEach { item ->
                            Log.i(TAG, "    OneTimeOfferListItem: price=${item.formattedPrice}, optionId=${item.purchaseOptionId}, offerId=${item.offerId}, tokenSuffix=${item.offerToken?.takeLast(4)}")
                        }
                    }

                    if (rawDetailsList.isEmpty()) {
                        if (subsCode != BillingClient.BillingResponseCode.OK) {
                            val error = BillingProductCatalog.Error(
                                BillingError.fromBillingResponseCode(subsCode),
                                subsResult.billingResult.debugMessage
                            )
                            _catalogState.value = error
                            error
                        } else if (inAppCode != BillingClient.BillingResponseCode.OK) {
                            val error = BillingProductCatalog.Error(
                                BillingError.fromBillingResponseCode(inAppCode),
                                inAppResult.billingResult.debugMessage
                            )
                            _catalogState.value = error
                            error
                        } else {
                            val empty = BillingProductCatalog.Empty
                            _catalogState.value = empty
                            empty
                        }
                    } else {
                        rawDetailsList.forEach { details ->
                            productDetailsCache[details.productId] = details
                        }
                        val mapped = rawDetailsList.mapNotNull { details ->
                            GooglePlayProductDetailsMapper.map(details)
                        }
                        val available = if (mapped.isNotEmpty()) {
                            BillingProductCatalog.Available(mapped)
                        } else {
                            BillingProductCatalog.Empty
                        }
                        _catalogState.value = available
                        available
                    }
                } else {
                    Log.e(TAG, "queryProductDetails FAILED: SUBS responseCode=$subsCode, debugMessage='${subsResult.billingResult.debugMessage}'; INAPP responseCode=$inAppCode, debugMessage='${inAppResult.billingResult.debugMessage}'")
                    val error = BillingProductCatalog.Error(
                        BillingError.fromBillingResponseCode(subsCode),
                        subsResult.billingResult.debugMessage
                    )
                    _catalogState.value = error
                    error
                }
            } catch (e: Exception) {
                Log.e(TAG, "refreshProductCatalog exception: ${e.message}", e)
                val error = BillingProductCatalog.Error(BillingError.UNKNOWN, e.message ?: "Unknown error")
                _catalogState.value = error
                error
            }
        }
    }

    /**
     * Launches the Google Play billing flow for the specified [product].
     * UI components must invoke this via BillingRepository rather than calling BillingClient directly.
     */
    override suspend fun purchase(product: PremiumProduct): BillingResult {
        val activity = activityProvider?.invoke() ?: currentActivity
        return if (activity != null) {
            purchase(activity, product)
        } else {
            BillingResult.Error("目前無法啟動購買介面 (Activity unavailable)")
        }
    }

    /**
     * Explicit overload allowing callers to provide the current hosting [Activity].
     */
    suspend fun purchase(activity: Activity, product: PremiumProduct): BillingResult {
        Log.i(TAG, "purchase() requested for ${product.productId} (isSubscription=${product.isSubscription})")
        if (!clientAdapter.isReady) {
            Log.w(TAG, "purchase() failed: BillingClient is not ready")
            return BillingResult.Error("目前無法使用 Google Play 付款服務")
        }

        var productDetails = productDetailsCache[product.productId]
        if (productDetails == null) {
            Log.i(TAG, "ProductDetails not in cache for ${product.productId}, refreshing catalog...")
            refreshProductCatalog()
            productDetails = productDetailsCache[product.productId]
        }

        if (productDetails == null) {
            Log.e(TAG, "purchase() failed: ProductDetails is NULL after refresh for ${product.productId}")
            return BillingResult.Error("此方案目前無法購買")
        }

        val productDetailsParamsBuilder = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(productDetails)

        // Subscriptions require a valid offerToken from Google Play
        if (product.isSubscription) {
            val offers = productDetails.subscriptionOfferDetails.orEmpty()
            val offerToken = if (product == PremiumProduct.ANNUAL) {
                // Priority: select 7-day free trial offer (priceAmountMicros == 0L), fallback to base plan
                val trialOffer = offers.firstOrNull { offer ->
                    offer.pricingPhases.pricingPhaseList.any { it.priceAmountMicros == 0L }
                }
                trialOffer?.offerToken ?: offers.firstOrNull()?.offerToken
            } else {
                offers.firstOrNull()?.offerToken
            }

            Log.i(TAG, "Subscription offerToken selected for ${product.productId}: offerTokenPresent=${!offerToken.isNullOrBlank()}, suffix=${offerToken?.takeLast(4)}")
            if (offerToken.isNullOrBlank()) {
                Log.e(TAG, "Subscription offerToken is null or blank for ${product.productId}!")
                return BillingResult.Error("此方案目前無法購買")
            }
            productDetailsParamsBuilder.setOfferToken(offerToken)
        } else {
            // One-time products (Lifetime)
            // In Google Play Billing Library 8 & 9, one-time products use Purchase Options and require an offerToken
            val oneTimeOffers = productDetails.oneTimePurchaseOfferDetailsList.orEmpty()
            val selectedOffer = oneTimeOffers.firstOrNull { it.purchaseOptionId == "lifetime" }
                ?: oneTimeOffers.firstOrNull()
                ?: productDetails.oneTimePurchaseOfferDetails

            val offerToken = selectedOffer?.offerToken
            Log.i(TAG, "One-time offer selected for ${product.productId}: purchaseOptionId=${selectedOffer?.purchaseOptionId}, offerTokenPresent=${!offerToken.isNullOrBlank()}, suffix=${offerToken?.takeLast(4)}")

            if (offerToken.isNullOrBlank()) {
                Log.e(TAG, "One-time offerToken is null or blank for ${product.productId}!")
                return BillingResult.Error("此方案目前無法購買")
            }
            productDetailsParamsBuilder.setOfferToken(offerToken)
        }

        val flowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(productDetailsParamsBuilder.build()))
            .build()

        val deferred = CompletableDeferred<BillingResult>()
        pendingPurchaseDeferred = deferred
        pendingTargetProduct = product

        Log.i(TAG, "Calling launchBillingFlow for ${product.productId}...")
        val launchResult = clientAdapter.launchBillingFlow(activity, flowParams)
        Log.i(TAG, "launchBillingFlow returned: responseCode=${launchResult.responseCode}, debugMessage='${launchResult.debugMessage}'")
        if (launchResult.responseCode != BillingClient.BillingResponseCode.OK) {
            pendingPurchaseDeferred = null
            pendingTargetProduct = null
            val error = BillingError.fromBillingResponseCode(launchResult.responseCode)
            Log.e(TAG, "launchBillingFlow FAILED: responseCode=${launchResult.responseCode} ($error), debugMessage='${launchResult.debugMessage}', userFacingMessage='${error.toUserFacingMessage()}'")
            return when (launchResult.responseCode) {
                BillingClient.BillingResponseCode.USER_CANCELED -> BillingResult.Cancelled("已取消購買")
                else -> {
                    BillingResult.Error(error.toUserFacingMessage())
                }
            }
        }

        return try {
            withTimeout(120_000L) {
                deferred.await()
            }
        } catch (e: TimeoutCancellationException) {
            pendingPurchaseDeferred = null
            pendingTargetProduct = null
            BillingResult.Error("購買連線逾時，請至 Google Play 重新確認")
        }
    }

    /**
     * Handles Google Play purchase updates delivered to [PurchasesUpdatedListener].
     */
    fun handlePurchasesUpdated(billingResult: PlayBillingResult, purchases: List<Purchase>?) {
        Log.i(TAG, "handlePurchasesUpdated: responseCode=${billingResult.responseCode}, debugMessage='${billingResult.debugMessage}', purchasesCount=${purchases?.size ?: 0}")
        val deferred = pendingPurchaseDeferred
        pendingPurchaseDeferred = null
        val targetProduct = pendingTargetProduct
        pendingTargetProduct = null

        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                if (purchases.isNullOrEmpty()) {
                    deferred?.complete(BillingResult.Pending("付款處理中，完成付款後 Premium 將會啟用"))
                } else {
                    scope.launch {
                        val result = processPurchases(purchases, targetProduct)
                        deferred?.complete(result)
                    }
                }
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                // Section XXIV: User cancel preserves current entitlement state
                deferred?.complete(BillingResult.Cancelled("已取消購買"))
            }
            else -> {
                val error = BillingError.fromBillingResponseCode(billingResult.responseCode)
                deferred?.complete(BillingResult.Error(error.toUserFacingMessage()))
            }
        }
    }

    /**
     * Processes raw [Purchase] items:
     * - Filters for PURCHASED vs PENDING (PENDING never grants Premium)
     * - Acknowledges unacknowledged purchases
     * - Deduplicates purchase tokens
     * - Preserves PurchaseRecord (protecting purchaseToken)
     * - Updates entitlement to Premium
     */
    suspend fun processPurchases(
        purchases: List<Purchase>,
        targetProduct: PremiumProduct? = null
    ): BillingResult {
        var hasPending = false
        val grantedProducts = mutableListOf<PremiumProduct>()

        for (purchase in purchases) {
            when (purchase.purchaseState) {
                Purchase.PurchaseState.PENDING -> {
                    hasPending = true
                    // Section VIII & IX: Pending cannot grant Premium, do not acknowledge
                }
                Purchase.PurchaseState.PURCHASED -> {
                    // Section X: Acknowledge if unacknowledged
                    if (!purchase.isAcknowledged) {
                        val ackParams = AcknowledgePurchaseParams.newBuilder()
                            .setPurchaseToken(purchase.purchaseToken)
                            .build()
                        val ackResult = clientAdapter.acknowledgePurchase(ackParams)
                        if (ackResult.responseCode != BillingClient.BillingResponseCode.OK) {
                            // Acknowledgement failed; do not grant entitlement
                            continue
                        }
                    }

                    // Section XVII: Deduplicate purchase token
                    processedPurchaseTokens.add(purchase.purchaseToken)

                    // Section XII: Retain PurchaseRecord (purchaseToken is protected in toString)
                    val record = PurchaseRecord(
                        productId = purchase.products.firstOrNull() ?: "",
                        purchaseToken = purchase.purchaseToken,
                        purchaseState = purchase.purchaseState,
                        isAcknowledged = true,
                        purchaseTime = purchase.purchaseTime,
                        orderId = purchase.orderId
                    )
                    synchronized(recordedPurchases) {
                        recordedPurchases.add(record)
                    }

                    // Section XIII: Identify product from purchase.products
                    val matched = PremiumProduct.entries.firstOrNull { it.productId in purchase.products }
                    if (matched != null) {
                        grantedProducts.add(matched)
                    }
                }
                else -> {
                    // UNSPECIFIED_STATE -> do nothing
                }
            }
        }

        if (grantedProducts.isNotEmpty()) {
            // Section XVIII: If multiple products purchased, grant Premium = true
            val productToGrant = if (targetProduct != null && targetProduct in grantedProducts) {
                targetProduct
            } else {
                grantedProducts.find { it == PremiumProduct.LIFETIME }
                    ?: grantedProducts.find { it == PremiumProduct.ANNUAL }
                    ?: grantedProducts.first()
            }
            val newEntitlement = PremiumEntitlement.Premium(productToGrant)
            _entitlement.value = newEntitlement
            return BillingResult.Success(newEntitlement)
        }

        if (hasPending) {
            return BillingResult.Pending("付款處理中，完成付款後 Premium 將會啟用")
        }

        return BillingResult.Error("尚未找到已購買的 Google Play 訂閱記錄")
    }

    /**
     * Restores existing Google Play purchases across both SUBS and INAPP catalogs.
     */
    override suspend fun restorePurchases(): BillingResult {
        if (!clientAdapter.isReady) {
            return BillingResult.Error("目前無法使用 Google Play 付款服務")
        }

        return withContext(Dispatchers.IO) {
            try {
                val subsParams = QueryPurchasesParams.newBuilder()
                    .setProductType(BillingClient.ProductType.SUBS)
                    .build()
                val subsResult = clientAdapter.queryPurchases(subsParams)

                val inAppParams = QueryPurchasesParams.newBuilder()
                    .setProductType(BillingClient.ProductType.INAPP)
                    .build()
                val inAppResult = clientAdapter.queryPurchases(inAppParams)

                val allPurchases = (subsResult.purchasesList.orEmpty() + inAppResult.purchasesList.orEmpty())

                val purchasedItems = allPurchases.filter { it.purchaseState == Purchase.PurchaseState.PURCHASED }
                val pendingItems = allPurchases.filter { it.purchaseState == Purchase.PurchaseState.PENDING }

                if (purchasedItems.isNotEmpty()) {
                    processPurchases(purchasedItems)
                } else if (pendingItems.isNotEmpty()) {
                    BillingResult.Pending("付款處理中，完成付款後 Premium 將會啟用")
                } else {
                    // Section XV: No valid purchase found -> maintain FREE.
                    // Do not permanently delete server entitlement.
                    if (_entitlement.value is PremiumEntitlement.Free) {
                        _entitlement.value = PremiumEntitlement.Free
                    }
                    BillingResult.Error("尚未找到已購買的 Google Play 訂閱記錄")
                }
            } catch (e: Exception) {
                BillingResult.Error("恢復購買失敗：${e.message}")
            }
        }
    }

    /**
     * Queries purchases on startup without blocking or failing UI rendering.
     */
    private suspend fun restorePurchasesSilently() {
        try {
            val subsParams = QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
            val subsResult = clientAdapter.queryPurchases(subsParams)

            val inAppParams = QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
            val inAppResult = clientAdapter.queryPurchases(inAppParams)

            val purchasedItems = (subsResult.purchasesList.orEmpty() + inAppResult.purchasesList.orEmpty())
                .filter { it.purchaseState == Purchase.PurchaseState.PURCHASED }

            if (purchasedItems.isNotEmpty()) {
                processPurchases(purchasedItems)
            }
        } catch (_: Exception) {
            // Background check failure must not crash the application
        }
    }

    override fun getProducts(): List<PremiumProductInfo> {
        val currentCatalog = _catalogState.value
        return if (currentCatalog is BillingProductCatalog.Available && currentCatalog.products.isNotEmpty()) {
            currentCatalog.products
        } else {
            PremiumProduct.entries.map { it.productInfo }
        }
    }

    override suspend fun refreshEntitlements(): PremiumEntitlement {
        return _entitlement.value
    }

    /**
     * Cleanly terminates the BillingClient connection on process teardown.
     */
    fun destroy() {
        if (clientAdapter.isReady) {
            clientAdapter.endConnection()
        }
        _connectionState.value = BillingConnectionState.Disconnected
    }

    companion object {
        private const val TAG = "EnglishCoachBilling"
    }
}
