package com.andy.englishcoach.ui.paywall

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.andy.englishcoach.billing.BillingProductCatalog
import com.andy.englishcoach.billing.BillingResult
import com.andy.englishcoach.billing.PremiumEntitlement
import com.andy.englishcoach.billing.PremiumProduct
import com.andy.englishcoach.billing.PremiumProductInfo
import com.andy.englishcoach.ui.theme.EnglishCoachColors
import com.andy.englishcoach.ui.theme.EnglishCoachGradients
import com.andy.englishcoach.ui.theme.EnglishCoachIcons
import com.andy.englishcoach.ui.theme.EnglishCoachShapes
import com.andy.englishcoach.ui.theme.EnglishCoachSpacing
import com.andy.englishcoach.ui.theme.EnglishCoachTypography

/**
 * Paywall bottom sheet for EnglishCoach Premium.
 * Displays subscription tiers (Monthly, Annual with 7-day trial, Lifetime),
 * feature highlights, purchase CTA, and Restore Purchases.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaywallBottomSheet(
    onDismissRequest: () -> Unit,
    onPurchaseProduct: (PremiumProduct) -> Unit,
    onRestorePurchases: () -> Unit,
    currentEntitlement: PremiumEntitlement = PremiumEntitlement.Free,
    statusMessage: String? = null,
    products: List<PremiumProductInfo> = emptyList(),
    catalog: BillingProductCatalog? = null
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val currentProduct = (currentEntitlement as? PremiumEntitlement.Premium)?.product
    val isPremiumUser = currentEntitlement.isPremium

    var selectedProduct by remember(currentEntitlement) {
        mutableStateOf(
            when (currentProduct) {
                PremiumProduct.LIFETIME -> PremiumProduct.LIFETIME
                PremiumProduct.ANNUAL -> PremiumProduct.ANNUAL
                PremiumProduct.MONTHLY -> PremiumProduct.MONTHLY
                null -> PremiumProduct.ANNUAL
            }
        )
    }

    val availableProducts = (catalog as? BillingProductCatalog.Available)?.products ?: products
    val localizedPrices = availableProducts.associate { it.product to it.displayPrice }

    val effectiveStatusMessage = when {
        statusMessage != null -> statusMessage
        catalog is BillingProductCatalog.Error -> "方案資訊暫時無法載入，請稍後再試"
        catalog is BillingProductCatalog.Empty -> "目前尚未開放正式購買，敬請期待正式上架"
        else -> null
    }

    val title = if (isPremiumUser) "管理會員方案" else "EnglishCoach Premium"
    val subtitle = when {
        !isPremiumUser -> "解鎖 3,600 單字庫與無限複習"
        currentProduct == PremiumProduct.MONTHLY -> "目前使用月訂閱"
        currentProduct == PremiumProduct.ANNUAL -> "目前使用年訂閱"
        currentProduct == PremiumProduct.LIFETIME -> "已擁有終身尊榮會員"
        else -> "檢視或管理您的會員方案權限"
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = EnglishCoachColors.Surface,
        shape = EnglishCoachShapes.card
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = EnglishCoachSpacing.lg, vertical = EnglishCoachSpacing.sm)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(EnglishCoachSpacing.md)
        ) {
            // Header Icon & Title
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(
                        if (isPremiumUser) EnglishCoachColors.Purple.copy(alpha = 0.15f)
                        else EnglishCoachColors.Orange.copy(alpha = 0.15f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = EnglishCoachIcons.Crown,
                    contentDescription = "Premium Crown",
                    tint = if (isPremiumUser) EnglishCoachColors.Purple else EnglishCoachColors.Orange,
                    modifier = Modifier.size(28.dp)
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(EnglishCoachSpacing.xxs)
            ) {
                Text(
                    text = title,
                    style = EnglishCoachTypography.screenTitle,
                    color = EnglishCoachColors.TextPrimary
                )
                Text(
                    text = subtitle,
                    style = EnglishCoachTypography.caption,
                    color = EnglishCoachColors.TextSecondary,
                    textAlign = TextAlign.Center
                )
            }

            // Status message if any
            if (effectiveStatusMessage != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(EnglishCoachShapes.card)
                        .background(EnglishCoachColors.Blue.copy(alpha = 0.1f))
                        .padding(EnglishCoachSpacing.sm),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = effectiveStatusMessage,
                        style = EnglishCoachTypography.caption,
                        color = EnglishCoachColors.Blue,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Feature Highlights
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(EnglishCoachShapes.card)
                    .background(EnglishCoachColors.Background)
                    .padding(EnglishCoachSpacing.md),
                verticalArrangement = Arrangement.spacedBy(EnglishCoachSpacing.xs)
            ) {
                PaywallFeatureRow("每日學習目標自由選（5 ~ 100 題或無限制）")
                PaywallFeatureRow("SM-2 智慧複習中心無限制測驗練習")
                PaywallFeatureRow("涵蓋基礎 550+、進階 750+、金證 860+ 全庫")
            }

            // Pricing Options
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(EnglishCoachSpacing.sm)
            ) {
                val isAnnualCurrent = (currentProduct == PremiumProduct.ANNUAL)
                val isAnnualDisabled = (currentProduct == PremiumProduct.LIFETIME)
                val annualBadge = when {
                    isAnnualCurrent -> "目前方案"
                    isAnnualDisabled -> null
                    else -> "7天免費"
                }
                val annualDisabledReason = if (isAnnualDisabled) "您已享有終身權益，無需訂閱" else null

                PremiumTierCard(
                    product = PremiumProduct.ANNUAL,
                    displayPrice = localizedPrices[PremiumProduct.ANNUAL] ?: PremiumProduct.ANNUAL.displayPrice,
                    isSelected = selectedProduct == PremiumProduct.ANNUAL,
                    isCurrentPlan = isAnnualCurrent,
                    isDisabled = isAnnualDisabled,
                    disabledReason = annualDisabledReason,
                    badgeText = annualBadge,
                    onClick = {
                        if (!isAnnualDisabled) {
                            selectedProduct = PremiumProduct.ANNUAL
                        }
                    }
                )

                val isMonthlyCurrent = (currentProduct == PremiumProduct.MONTHLY)
                val isMonthlyDisabled = (currentProduct == PremiumProduct.LIFETIME)
                val monthlyBadge = if (isMonthlyCurrent) "目前方案" else null
                val monthlyDisabledReason = if (isMonthlyDisabled) "您已享有終身權益，無需訂閱" else null

                PremiumTierCard(
                    product = PremiumProduct.MONTHLY,
                    displayPrice = localizedPrices[PremiumProduct.MONTHLY] ?: PremiumProduct.MONTHLY.displayPrice,
                    isSelected = selectedProduct == PremiumProduct.MONTHLY,
                    isCurrentPlan = isMonthlyCurrent,
                    isDisabled = isMonthlyDisabled,
                    disabledReason = monthlyDisabledReason,
                    badgeText = monthlyBadge,
                    onClick = {
                        if (!isMonthlyDisabled) {
                            selectedProduct = PremiumProduct.MONTHLY
                        }
                    }
                )

                val isLifetimeCurrent = (currentProduct == PremiumProduct.LIFETIME)
                val lifetimeBadge = if (isLifetimeCurrent) "目前方案" else "終身有效"

                PremiumTierCard(
                    product = PremiumProduct.LIFETIME,
                    displayPrice = localizedPrices[PremiumProduct.LIFETIME] ?: PremiumProduct.LIFETIME.displayPrice,
                    isSelected = selectedProduct == PremiumProduct.LIFETIME,
                    isCurrentPlan = isLifetimeCurrent,
                    isDisabled = false,
                    badgeText = lifetimeBadge,
                    onClick = {
                        selectedProduct = PremiumProduct.LIFETIME
                    }
                )
            }

            // CTA Button
            val isCtaEnabled = when {
                !isPremiumUser -> true
                currentProduct == null -> true
                currentProduct == PremiumProduct.LIFETIME -> false
                selectedProduct == currentProduct -> false
                else -> true
            }

            val ctaText = when {
                !isPremiumUser -> when (selectedProduct) {
                    PremiumProduct.ANNUAL -> "開始 7 天免費試用 ➜"
                    PremiumProduct.MONTHLY -> "立即開通月繳方案"
                    PremiumProduct.LIFETIME -> "立即解鎖終身尊榮"
                }
                currentProduct == PremiumProduct.LIFETIME -> "已擁有終身尊榮會員"
                selectedProduct == currentProduct -> "目前生效方案"
                currentProduct == PremiumProduct.MONTHLY && selectedProduct == PremiumProduct.ANNUAL -> "變更為年訂閱"
                currentProduct == PremiumProduct.MONTHLY && selectedProduct == PremiumProduct.LIFETIME -> "升級為終身買斷"
                currentProduct == PremiumProduct.ANNUAL && selectedProduct == PremiumProduct.MONTHLY -> "變更為月訂閱"
                currentProduct == PremiumProduct.ANNUAL && selectedProduct == PremiumProduct.LIFETIME -> "升級為終身買斷"
                else -> "變更方案"
            }

            // Notice when switching from Subscription to Lifetime
            if (currentProduct != null && currentProduct.isSubscription && selectedProduct == PremiumProduct.LIFETIME) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(EnglishCoachShapes.card)
                        .background(EnglishCoachColors.Orange.copy(alpha = 0.12f))
                        .padding(EnglishCoachSpacing.sm)
                ) {
                    Text(
                        text = "⚠️ 注意：購買終身方案（一次性買斷）不會自動取消既有的 Google Play 訂閱，購買後請自行至 Google Play「付款與訂閱」取消原訂閱之自動續約。",
                        style = EnglishCoachTypography.caption.copy(
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        color = EnglishCoachColors.Orange,
                        textAlign = TextAlign.Start
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(EnglishCoachShapes.button)
                    .background(
                        if (isCtaEnabled) EnglishCoachGradients.purpleBlue
                        else Brush.linearGradient(
                            listOf(
                                EnglishCoachColors.CardBorder,
                                EnglishCoachColors.CardBorder
                            )
                        )
                    )
                    .clickable(
                        enabled = isCtaEnabled,
                        role = Role.Button,
                        onClick = { onPurchaseProduct(selectedProduct) }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = ctaText,
                    style = EnglishCoachTypography.button,
                    color = if (isCtaEnabled) Color.White else EnglishCoachColors.TextSecondary
                )
            }

            // Restore Purchases & Terms
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onRestorePurchases) {
                    Text(
                        text = "恢復購買",
                        style = EnglishCoachTypography.caption,
                        color = EnglishCoachColors.Purple
                    )
                }
                TextButton(onClick = onDismissRequest) {
                    Text(
                        text = "暫時不用",
                        style = EnglishCoachTypography.caption,
                        color = EnglishCoachColors.TextSecondary
                    )
                }
            }

            Text(
                text = "訂閱將由 Google Play 帳戶自動扣款，可隨時取消。\n非正式付費階段不會產生真實扣款。",
                style = EnglishCoachTypography.caption.copy(fontWeight = FontWeight.Normal),
                color = EnglishCoachColors.TextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = EnglishCoachSpacing.sm)
            )
        }
    }
}

@Composable
private fun PaywallFeatureRow(text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(EnglishCoachSpacing.xs)
    ) {
        Icon(
            imageVector = EnglishCoachIcons.CheckCircle,
            contentDescription = null,
            tint = EnglishCoachColors.Green,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = text,
            style = EnglishCoachTypography.caption,
            color = EnglishCoachColors.TextPrimary
        )
    }
}

@Composable
private fun PremiumTierCard(
    product: PremiumProduct,
    isSelected: Boolean,
    isCurrentPlan: Boolean = false,
    isDisabled: Boolean = false,
    disabledReason: String? = null,
    badgeText: String? = null,
    onClick: () -> Unit,
    displayPrice: String = product.displayPrice
) {
    val borderColor = when {
        isSelected -> EnglishCoachColors.Purple
        isCurrentPlan -> EnglishCoachColors.Green
        else -> EnglishCoachColors.CardBorder
    }
    val borderWidth = if (isSelected || isCurrentPlan) 2.dp else 1.dp
    val bgColor = when {
        isDisabled -> EnglishCoachColors.Surface.copy(alpha = 0.6f)
        isSelected -> EnglishCoachColors.Purple.copy(alpha = 0.06f)
        isCurrentPlan -> EnglishCoachColors.Green.copy(alpha = 0.04f)
        else -> EnglishCoachColors.Surface
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(EnglishCoachShapes.card)
            .background(bgColor)
            .border(borderWidth, borderColor, EnglishCoachShapes.card)
            .clickable(enabled = !isDisabled, onClick = onClick)
            .padding(EnglishCoachSpacing.md)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(EnglishCoachSpacing.xxs)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(EnglishCoachSpacing.xs)
                ) {
                    Text(
                        text = product.title,
                        style = EnglishCoachTypography.button,
                        color = if (isDisabled) EnglishCoachColors.TextSecondary else EnglishCoachColors.TextPrimary
                    )
                    if (badgeText != null) {
                        val badgeColor = if (isCurrentPlan) EnglishCoachColors.Green else EnglishCoachColors.Orange
                        Box(
                            modifier = Modifier
                                .clip(EnglishCoachShapes.pill)
                                .background(badgeColor.copy(alpha = 0.15f))
                                .padding(horizontal = EnglishCoachSpacing.xs, vertical = 2.dp)
                        ) {
                            Text(
                                text = badgeText,
                                style = EnglishCoachTypography.badge,
                                color = badgeColor
                            )
                        }
                    }
                }
                Text(
                    text = if (isDisabled && !disabledReason.isNullOrBlank()) disabledReason else product.description,
                    style = EnglishCoachTypography.caption,
                    color = EnglishCoachColors.TextSecondary
                )
            }

            Spacer(modifier = Modifier.width(EnglishCoachSpacing.sm))

            if (!isDisabled) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = displayPrice,
                        style = EnglishCoachTypography.sectionHeader,
                        color = if (isSelected) EnglishCoachColors.Purple else EnglishCoachColors.TextPrimary
                    )
                    Text(
                        text = " " + product.billingPeriod,
                        style = EnglishCoachTypography.caption,
                        color = EnglishCoachColors.TextSecondary,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                }
            }
        }
    }
}
