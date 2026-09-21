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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.andy.englishcoach.billing.BillingProductCatalog
import com.andy.englishcoach.billing.BillingResult
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
    statusMessage: String? = null,
    products: List<PremiumProductInfo> = emptyList(),
    catalog: BillingProductCatalog? = null
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedProduct by remember { mutableStateOf(PremiumProduct.ANNUAL) }

    val availableProducts = (catalog as? BillingProductCatalog.Available)?.products ?: products
    val localizedPrices = availableProducts.associate { it.product to it.displayPrice }

    val effectiveStatusMessage = when {
        statusMessage != null -> statusMessage
        catalog is BillingProductCatalog.Error -> "方案資訊暫時無法載入，請稍後再試"
        catalog is BillingProductCatalog.Empty -> "目前尚未開放正式購買，敬請期待正式上架"
        else -> null
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
                    .background(EnglishCoachColors.Orange.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = EnglishCoachIcons.Crown,
                    contentDescription = "Premium Crown",
                    tint = EnglishCoachColors.Orange,
                    modifier = Modifier.size(28.dp)
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(EnglishCoachSpacing.xxs)
            ) {
                Text(
                    text = "EnglishCoach Premium",
                    style = EnglishCoachTypography.screenTitle,
                    color = EnglishCoachColors.TextPrimary
                )
                Text(
                    text = "解鎖完整 3,600 多益單字庫與無限複習測驗",
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
                PremiumTierCard(
                    product = PremiumProduct.ANNUAL,
                    displayPrice = localizedPrices[PremiumProduct.ANNUAL] ?: PremiumProduct.ANNUAL.displayPrice,
                    isSelected = selectedProduct == PremiumProduct.ANNUAL,
                    isRecommended = true,
                    onClick = { selectedProduct = PremiumProduct.ANNUAL }
                )
                PremiumTierCard(
                    product = PremiumProduct.MONTHLY,
                    displayPrice = localizedPrices[PremiumProduct.MONTHLY] ?: PremiumProduct.MONTHLY.displayPrice,
                    isSelected = selectedProduct == PremiumProduct.MONTHLY,
                    isRecommended = false,
                    onClick = { selectedProduct = PremiumProduct.MONTHLY }
                )
                PremiumTierCard(
                    product = PremiumProduct.LIFETIME,
                    displayPrice = localizedPrices[PremiumProduct.LIFETIME] ?: PremiumProduct.LIFETIME.displayPrice,
                    isSelected = selectedProduct == PremiumProduct.LIFETIME,
                    isRecommended = false,
                    onClick = { selectedProduct = PremiumProduct.LIFETIME }
                )
            }

            // CTA Button
            val ctaText = when (selectedProduct) {
                PremiumProduct.ANNUAL -> "開始 7 天免費試用"
                PremiumProduct.MONTHLY -> "立即開通月繳方案"
                PremiumProduct.LIFETIME -> "立即解鎖終身尊榮"
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(EnglishCoachShapes.button)
                    .background(EnglishCoachGradients.purpleBlue)
                    .clickable(
                        role = Role.Button,
                        onClick = { onPurchaseProduct(selectedProduct) }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = ctaText,
                    style = EnglishCoachTypography.button,
                    color = Color.White
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
    isRecommended: Boolean,
    onClick: () -> Unit,
    displayPrice: String = product.displayPrice
) {
    val borderColor = if (isSelected) EnglishCoachColors.Purple else EnglishCoachColors.CardBorder
    val borderWidth = if (isSelected) 2.dp else 1.dp
    val bgColor = if (isSelected) EnglishCoachColors.Purple.copy(alpha = 0.06f) else EnglishCoachColors.Surface

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(EnglishCoachShapes.card)
            .background(bgColor)
            .border(borderWidth, borderColor, EnglishCoachShapes.card)
            .clickable(onClick = onClick)
            .padding(EnglishCoachSpacing.md)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(EnglishCoachSpacing.xxs)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(EnglishCoachSpacing.xs)
                ) {
                    Text(
                        text = product.title,
                        style = EnglishCoachTypography.button,
                        color = EnglishCoachColors.TextPrimary
                    )
                    if (isRecommended) {
                        Box(
                            modifier = Modifier
                                .clip(EnglishCoachShapes.pill)
                                .background(EnglishCoachColors.Orange.copy(alpha = 0.15f))
                                .padding(horizontal = EnglishCoachSpacing.xs, vertical = 2.dp)
                        ) {
                            Text(
                                text = "7天免費",
                                style = EnglishCoachTypography.badge,
                                color = EnglishCoachColors.Orange
                            )
                        }
                    }
                }
                Text(
                    text = product.description,
                    style = EnglishCoachTypography.caption,
                    color = EnglishCoachColors.TextSecondary
                )
            }

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
