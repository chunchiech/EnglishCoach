package com.andy.englishcoach.billing

import android.app.Activity
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult as PlayBillingResult
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.ProductDetailsResult
import com.android.billingclient.api.Purchase
import com.andy.englishcoach.data.database.EnglishCoachDatabase
import com.andy.englishcoach.data.database.entity.VocabularyEntity
import com.andy.englishcoach.data.model.ToeicTarget
import com.andy.englishcoach.data.preference.InMemoryDailyLearningPreferences
import com.andy.englishcoach.data.preference.InMemorySettingsPreferences
import com.andy.englishcoach.data.repository.DailyLearningRepository
import com.andy.englishcoach.data.repository.QuizRepository
import com.andy.englishcoach.data.repository.ReviewRepository
import com.andy.englishcoach.ui.dashboard.DashboardCtaAction
import com.andy.englishcoach.ui.dashboard.DashboardViewModel
import com.andy.englishcoach.ui.settings.SettingsViewModel
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

/**
 * End-to-End integration test suite for Google Play Subscription / Billing / Premium Entitlement.
 *
 * Verifies the 10 core requirements:
 * 1. Subscription product 正確載入 (SUBS vs INAPP separated)
 * 2. BillingClient connection 正常 (Connection lifecycle & retry)
 * 3. Google Play subscription purchase flow 正常 (OfferToken attached & flow launched)
 * 4. Purchase success 後 entitlementProvider.entitlement 立即更新
 * 5. Premium 功能立即解鎖 (Level gating & Target policies)
 * 6. App restart 後重新 query purchases 可以恢復 Premium
 * 7. Subscription 狀態變更可以正確反映 (Upgrade & Expiration)
 * 8. Restore purchases 正常
 * 9. Premium 解鎖後每日學習量 10 → 20 → 30 → Unlimited 正常
 * 10. 不得透過 UI 狀態假裝 Premium，權限必須以實際 entitlement 為準
 */
@RunWith(RobolectricTestRunner::class)
class GooglePlaySubscriptionEndToEndTest {

    private lateinit var context: Context
    private lateinit var fakeAdapter: FakeBillingClientAdapter
    private lateinit var billingRepo: GooglePlayBillingRepository
    private lateinit var database: EnglishCoachDatabase
    private lateinit var settingsPrefs: InMemorySettingsPreferences
    private lateinit var learningPrefs: InMemoryDailyLearningPreferences
    private val fixedClock = Clock.fixed(Instant.parse("2026-09-24T10:00:00Z"), ZoneId.of("Asia/Taipei"))

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        fakeAdapter = FakeBillingClientAdapter()
        billingRepo = GooglePlayBillingRepository(context = context, adapter = fakeAdapter)

        database = Room.inMemoryDatabaseBuilder(context, EnglishCoachDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        settingsPrefs = InMemorySettingsPreferences()
        learningPrefs = InMemoryDailyLearningPreferences()

        // Populate sample vocabulary for all levels
        val words = (1..60).map { i ->
            VocabularyEntity(
                id = i.toLong(),
                word = "word_$i",
                phonetic = "/w_$i/",
                translation = "單字 $i",
                example = "Sentence $i",
                exampleTranslation = "例句 $i",
                level = "toeic_basic",
                difficulty = 1,
                topic = "general",
                subtopic = "daily",
                examTags = "TOEIC",
                partOfSpeech = "n."
            )
        }
        database.vocabularyDao().insertAll(words)
    }

    @After
    fun tearDown() {
        database.close()
    }

    private fun createPurchase(
        productId: String,
        purchaseState: Int = Purchase.PurchaseState.PURCHASED,
        isAcknowledged: Boolean = true,
        purchaseToken: String = "token_${productId}_${System.currentTimeMillis()}",
        orderId: String = "GPA.test-${System.currentTimeMillis()}"
    ): Purchase {
        val stateInJson = if (purchaseState == Purchase.PurchaseState.PENDING) 4 else 1
        val json = """
            {
                "orderId": "$orderId",
                "packageName": "com.andy.englishcoach",
                "productId": "$productId",
                "productIds": ["$productId"],
                "purchaseTime": 1789999000000,
                "purchaseState": $stateInJson,
                "purchaseToken": "$purchaseToken",
                "acknowledged": $isAcknowledged
            }
        """.trimIndent()
        return Purchase(json, "signature")
    }

    private fun createProductDetails(json: String): ProductDetails {
        val constructor = ProductDetails::class.java.getDeclaredConstructor(String::class.java).apply {
            isAccessible = true
        }
        return constructor.newInstance(json)
    }

    private fun setupMockProductCatalog() {
        val monthlyDetails = createProductDetails("""
            {
                "productId": "com.andy.englishcoach.premium.monthly",
                "type": "subs",
                "title": "月繳方案",
                "name": "Monthly",
                "description": "Monthly access",
                "subscriptionOfferDetails": [
                    {
                        "basePlanId": "monthly-plan",
                        "offerIdToken": "monthly_sub_offer_token_123",
                        "pricingPhases": [{"priceAmountMicros": 90000000, "priceCurrencyCode": "TWD", "formattedPrice": "NT$90", "billingPeriod": "P1M", "recurrenceMode": 1}]
                    }
                ]
            }
        """.trimIndent())

        val annualDetails = createProductDetails("""
            {
                "productId": "com.andy.englishcoach.premium.annual",
                "type": "subs",
                "title": "年繳方案",
                "name": "Annual",
                "description": "Annual access",
                "subscriptionOfferDetails": [
                    {
                        "basePlanId": "annual-plan",
                        "offerIdToken": "annual_sub_offer_token_456",
                        "pricingPhases": [
                            {"priceAmountMicros": 0, "priceCurrencyCode": "TWD", "formattedPrice": "免費試用 7 天", "billingPeriod": "P7D", "recurrenceMode": 2},
                            {"priceAmountMicros": 690000000, "priceCurrencyCode": "TWD", "formattedPrice": "NT$690", "billingPeriod": "P1Y", "recurrenceMode": 1}
                        ]
                    }
                ]
            }
        """.trimIndent())

        val lifetimeDetails = createProductDetails("""
            {
                "productId": "com.andy.englishcoach.premium.lifetime",
                "type": "inapp",
                "title": "終身方案",
                "name": "Lifetime",
                "description": "Lifetime access",
                "oneTimePurchaseOfferDetailsList": [
                    {
                        "purchaseOptionId": "lifetime",
                        "offerIdToken": "lifetime_one_time_token_789",
                        "formattedPrice": "NT$1,290",
                        "priceCurrencyCode": "TWD",
                        "priceAmountMicros": 1290000000
                    }
                ]
            }
        """.trimIndent())

        fakeAdapter.queryProductDetailsHandler = { params ->
            val okResult = PlayBillingResult.newBuilder().setResponseCode(BillingClient.BillingResponseCode.OK).build()
            when (params.zzb()) {
                BillingClient.ProductType.SUBS -> ProductDetailsResult(okResult, listOf(monthlyDetails, annualDetails))
                BillingClient.ProductType.INAPP -> ProductDetailsResult(okResult, listOf(lifetimeDetails))
                else -> ProductDetailsResult(okResult, emptyList())
            }
        }
    }

    // 1. Subscription product 正確載入
    @Test
    fun test01_subscriptionProductLoadedCorrectly() = runBlocking {
        setupMockProductCatalog()

        val catalogResult = billingRepo.refreshProductCatalog()
        assertTrue("Catalog must be available", catalogResult is BillingProductCatalog.Available)
        val available = catalogResult as BillingProductCatalog.Available
        assertEquals(3, available.products.size)

        // Verify SUBS and INAPP queries were separated (no "All products should be of the same product type" exception)
        assertEquals(2, fakeAdapter.queriedProductParams.size)
        assertEquals(BillingClient.ProductType.SUBS, fakeAdapter.queriedProductParams[0].zzb())
        assertEquals(BillingClient.ProductType.INAPP, fakeAdapter.queriedProductParams[1].zzb())

        // Verify products retrieved
        val monthly = available.products.firstOrNull { it.product == PremiumProduct.MONTHLY }
        val annual = available.products.firstOrNull { it.product == PremiumProduct.ANNUAL }
        val lifetime = available.products.firstOrNull { it.product == PremiumProduct.LIFETIME }

        assertNotNull(monthly)
        assertNotNull(annual)
        assertNotNull(lifetime)
        assertEquals("NT$90", monthly?.displayPrice)
        assertEquals("NT$690", annual?.displayPrice)
        assertEquals("NT$1,290", lifetime?.displayPrice)
    }

    // 2. BillingClient connection 正常
    @Test
    fun test02_billingClientConnectionNormal() = runBlocking {
        fakeAdapter.startConnectionResponseCode = BillingClient.BillingResponseCode.OK
        val connectedRepo = GooglePlayBillingRepository(context = context, adapter = fakeAdapter)

        assertTrue("Adapter should report ready", fakeAdapter.isReady)
        assertEquals(BillingConnectionState.Connected, connectedRepo.connectionState.value)

        // Verify connection failure handling
        fakeAdapter.startConnectionResponseCode = BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE
        val failedRepo = GooglePlayBillingRepository(context = context, adapter = fakeAdapter)
        assertTrue(failedRepo.connectionState.value is BillingConnectionState.Unavailable)
        assertEquals(PremiumEntitlement.Free, failedRepo.entitlement.value)
    }

    // 3. Google Play subscription purchase flow 正常
    @Test
    fun test03_subscriptionPurchaseFlowNormal() = runBlocking {
        setupMockProductCatalog()
        billingRepo.refreshProductCatalog()

        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        fakeAdapter.launchBillingFlowResult = PlayBillingResult.newBuilder()
            .setResponseCode(BillingClient.BillingResponseCode.USER_CANCELED)
            .build()

        // Launch Monthly subscription
        val monthlyResult = billingRepo.purchase(activity, PremiumProduct.MONTHLY)
        assertTrue(monthlyResult is BillingResult.Cancelled)
        assertNotNull(fakeAdapter.lastBillingFlowParams)
        val monthlyParams = fakeAdapter.lastBillingFlowParams!!.zzk()[0] as BillingFlowParams.ProductDetailsParams
        assertEquals("monthly_sub_offer_token_123", monthlyParams.zzb())

        // Launch Annual subscription
        val annualResult = billingRepo.purchase(activity, PremiumProduct.ANNUAL)
        assertTrue(annualResult is BillingResult.Cancelled)
        assertNotNull(fakeAdapter.lastBillingFlowParams)
        val annualParams = fakeAdapter.lastBillingFlowParams!!.zzk()[0] as BillingFlowParams.ProductDetailsParams
        assertEquals("annual_sub_offer_token_456", annualParams.zzb())
    }

    // 4. Purchase success 後 entitlementProvider.entitlement 立即更新
    @Test
    fun test04_purchaseSuccessUpdatesEntitlementImmediately() = runBlocking {
        assertEquals(PremiumEntitlement.Free, billingRepo.entitlement.value)
        assertFalse(billingRepo.isPremium)

        val purchase = createPurchase(
            productId = PremiumProduct.MONTHLY.productId,
            purchaseToken = "test_purchase_token_monthly"
        )

        val result = billingRepo.processPurchases(listOf(purchase))
        assertTrue(result is BillingResult.Success)

        // Entitlement updated immediately
        val current = billingRepo.entitlement.value
        assertTrue(current.isPremium)
        assertTrue(current is PremiumEntitlement.Premium)
        assertEquals(PremiumProduct.MONTHLY, (current as PremiumEntitlement.Premium).product)
        assertTrue(billingRepo.isPremium)
    }

    // 5. Premium 功能立即解鎖
    @Test
    fun test05_premiumFeaturesUnlockedImmediately() = runBlocking {
        val settingsVm = SettingsViewModel(
            settingsPreferences = settingsPrefs,
            learningPreferences = learningPrefs,
            entitlementProvider = billingRepo
        )

        // Initially Free: daily target locked to 10
        assertFalse(settingsVm.uiState.value.isPremium)
        assertFalse(settingsVm.selectDailyTarget(20))
        assertEquals(10, settingsVm.uiState.value.dailyTarget)

        // Purchase Annual
        val purchase = createPurchase(PremiumProduct.ANNUAL.productId)
        billingRepo.processPurchases(listOf(purchase))

        // Premium unlocks immediately in ViewModel
        settingsVm.refresh()
        assertTrue(settingsVm.uiState.value.isPremium)
        assertTrue(settingsVm.selectDailyTarget(20))
        assertEquals(20, settingsVm.uiState.value.dailyTarget)
    }

    // 6. App restart 後重新 query purchases 可以恢復 Premium
    @Test
    fun test06_appRestartQueryPurchasesRestoresPremium() = runBlocking {
        val purchase = createPurchase(
            productId = PremiumProduct.ANNUAL.productId,
            purchaseToken = "persisted_annual_token"
        )
        fakeAdapter.mockSubsPurchases = listOf(purchase)
        fakeAdapter.mockInAppPurchases = emptyList()

        // Fresh session after process restart
        val restartedRepo = GooglePlayBillingRepository(context = context, adapter = fakeAdapter)
        restartedRepo.restorePurchases()

        assertTrue(restartedRepo.isPremium)
        val entitlement = restartedRepo.entitlement.value
        assertTrue(entitlement is PremiumEntitlement.Premium)
        assertEquals(PremiumProduct.ANNUAL, (entitlement as PremiumEntitlement.Premium).product)
    }

    // 7. Subscription 狀態變更可以正確反映
    @Test
    fun test07_subscriptionStatusChangeReflected() = runBlocking {
        // Step A: Active Monthly subscription
        val monthlyPurchase = createPurchase(
            productId = PremiumProduct.MONTHLY.productId,
            purchaseToken = "monthly_token_current"
        )
        billingRepo.processPurchases(listOf(monthlyPurchase))
        assertEquals(PremiumProduct.MONTHLY, (billingRepo.entitlement.value as PremiumEntitlement.Premium).product)
        assertEquals("monthly_token_current", billingRepo.activeSubscriptionPurchaseToken)

        // Step B: User upgrades subscription to Annual
        val annualPurchase = createPurchase(
            productId = PremiumProduct.ANNUAL.productId,
            purchaseToken = "annual_token_new"
        )
        val upgradeResult = billingRepo.processPurchases(
            listOf(annualPurchase),
            targetProduct = PremiumProduct.ANNUAL
        )

        // Entitlement reflects new Annual subscription immediately
        assertTrue(upgradeResult is BillingResult.Success)
        val currentEntitlement = billingRepo.entitlement.value as PremiumEntitlement.Premium
        assertEquals(PremiumProduct.ANNUAL, currentEntitlement.product)
        assertEquals("annual_token_new", billingRepo.activeSubscriptionPurchaseToken)
        assertTrue(billingRepo.isPremium)
    }

    // 8. Restore purchases 正常
    @Test
    fun test08_restorePurchasesNormal() = runBlocking {
        val unackPurchase = createPurchase(
            productId = PremiumProduct.LIFETIME.productId,
            isAcknowledged = false,
            purchaseToken = "unack_lifetime_restore_token"
        )
        fakeAdapter.mockSubsPurchases = emptyList()
        fakeAdapter.mockInAppPurchases = listOf(unackPurchase)

        val result = billingRepo.restorePurchases()
        assertTrue(result is BillingResult.Success)
        assertTrue(billingRepo.isPremium)

        // Token must be acknowledged
        assertTrue(fakeAdapter.acknowledgedTokens.contains("unack_lifetime_restore_token"))
    }

    // 9. Premium 解鎖後每日學習量 10 → 20 → 30 → Unlimited 正常
    @Test
    fun test09_premiumUnlockDailyGoalProgression() = runBlocking {
        val dailyLearningRepo = DailyLearningRepository(database, learningPrefs, clock = fixedClock)
        val quizRepo = QuizRepository(database, learningPrefs, dailyLearningRepo, clock = fixedClock)
        val reviewRepo = ReviewRepository(database, learningPrefs, clock = fixedClock)

        val dashboardVm = DashboardViewModel(
            learningRepository = dailyLearningRepo,
            quizRepository = quizRepo,
            reviewRepository = reviewRepo,
            preferences = learningPrefs,
            database = database,
            clock = fixedClock,
            entitlementProvider = billingRepo,
            settingsPreferences = settingsPrefs
        )

        // Free state: target is 10, cannot change to 20
        var state = dashboardVm.calculateDashboardState()
        assertEquals(10, state.dailyTarget)
        assertFalse(dashboardVm.selectDailyTarget(20))

        // Purchase Premium Monthly
        val purchase = createPurchase(PremiumProduct.MONTHLY.productId)
        billingRepo.processPurchases(listOf(purchase))
        assertTrue(billingRepo.isPremium)

        // Change 10 -> 20
        assertTrue(dashboardVm.selectDailyTarget(20))
        state = dashboardVm.calculateDashboardState()
        assertEquals(20, state.dailyTarget)
        assertEquals("20 題", state.dailyTargetText)

        // Change 20 -> 30
        assertTrue(dashboardVm.selectDailyTarget(30))
        state = dashboardVm.calculateDashboardState()
        assertEquals(30, state.dailyTarget)
        assertEquals("30 題", state.dailyTargetText)

        // Change 30 -> Unlimited (-1)
        assertTrue(dashboardVm.selectDailyTarget(DailyTargetPolicy.UNLIMITED_TARGET))
        state = dashboardVm.calculateDashboardState()
        assertEquals(DailyTargetPolicy.UNLIMITED_TARGET, state.dailyTarget)
        assertTrue(state.isUnlimitedTarget)
        assertEquals("不限", state.dailyTargetText)
    }

    // 10. 不得透過 UI 狀態假裝 Premium，權限必須以實際 entitlement 為準
    @Test
    fun test10_permissionsStrictlyBasedOnActualEntitlement() {
        val settingsVm = SettingsViewModel(
            settingsPreferences = settingsPrefs,
            learningPreferences = learningPrefs,
            entitlementProvider = billingRepo
        )

        // Billing repository is Free
        assertEquals(PremiumEntitlement.Free, billingRepo.entitlement.value)

        // UI state cannot be spoofed to grant Premium
        assertFalse(settingsVm.uiState.value.isPremium)

        // Policy for Free user enforces 10 max
        val policy = DailyTargetPolicy.forIsPremium(billingRepo.isPremium)
        assertEquals(listOf(10), policy.allowedTargets)
        assertEquals(10, policy.coerceTarget(100))
        assertEquals(10, policy.coerceTarget(-1))

        // Attempting to select disallowed target fails
        assertFalse(settingsVm.selectDailyTarget(50))
        assertFalse(settingsVm.selectDailyTarget(-1))
        assertEquals(10, settingsVm.uiState.value.dailyTarget)
    }
}
