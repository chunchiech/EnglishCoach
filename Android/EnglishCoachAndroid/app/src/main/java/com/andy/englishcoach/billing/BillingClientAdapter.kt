package com.andy.englishcoach.billing

import android.app.Activity
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult as PlayBillingResult
import com.android.billingclient.api.ProductDetailsResult
import com.android.billingclient.api.PurchasesResult
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.acknowledgePurchase
import com.android.billingclient.api.queryProductDetails
import com.android.billingclient.api.queryPurchasesAsync

/**
 * Adapter interface abstracting BillingClient operations.
 * Enables 100% deterministic unit testing without a physical Android device or Play Store service.
 */
interface BillingClientAdapter {
    val isReady: Boolean
    fun startConnection(listener: BillingClientStateListener)
    fun endConnection()
    suspend fun queryProductDetails(params: QueryProductDetailsParams): ProductDetailsResult
    suspend fun queryPurchases(params: QueryPurchasesParams): PurchasesResult
    suspend fun acknowledgePurchase(params: AcknowledgePurchaseParams): PlayBillingResult
    fun launchBillingFlow(activity: Activity, params: BillingFlowParams): PlayBillingResult
}

/**
 * Production implementation backed directly by the official Google Play BillingClient.
 */
class DefaultBillingClientAdapter(private val billingClient: BillingClient) : BillingClientAdapter {
    override val isReady: Boolean get() = billingClient.isReady
    override fun startConnection(listener: BillingClientStateListener) = billingClient.startConnection(listener)
    override fun endConnection() = billingClient.endConnection()
    override suspend fun queryProductDetails(params: QueryProductDetailsParams): ProductDetailsResult =
        billingClient.queryProductDetails(params)
    override suspend fun queryPurchases(params: QueryPurchasesParams): PurchasesResult =
        billingClient.queryPurchasesAsync(params)
    override suspend fun acknowledgePurchase(params: AcknowledgePurchaseParams): PlayBillingResult =
        billingClient.acknowledgePurchase(params)
    override fun launchBillingFlow(activity: Activity, params: BillingFlowParams): PlayBillingResult =
        billingClient.launchBillingFlow(activity, params)
}
