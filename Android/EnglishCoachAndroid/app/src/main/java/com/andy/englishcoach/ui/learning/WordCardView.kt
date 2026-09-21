package com.andy.englishcoach.ui.learning

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.andy.englishcoach.data.model.Word
import com.andy.englishcoach.ui.theme.EnglishCoachColors
import com.andy.englishcoach.ui.theme.EnglishCoachGradients
import com.andy.englishcoach.ui.theme.EnglishCoachIcons
import com.andy.englishcoach.ui.theme.EnglishCoachShapes
import com.andy.englishcoach.ui.theme.EnglishCoachSpacing
import com.andy.englishcoach.ui.theme.EnglishCoachTypography

/**
 * Word card component featuring 3D flip animation between English prompt and Chinese translation.
 * Matches iOS EnglishCoach WordCardView design language.
 */
@Composable
fun WordCardView(
    word: Word,
    isFlipped: Boolean,
    onCardClick: () -> Unit,
    onSpeakWord: () -> Unit,
    onSpeakSentence: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val rotation by animateFloatAsState(
        targetValue = if (isFlipped) 180f else 0f,
        animationSpec = tween(durationMillis = 400),
        label = "cardFlipAnimation"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(400.dp)
            .graphicsLayer {
                rotationY = rotation
                cameraDistance = 12f * density
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Button,
                onClick = onCardClick
            ),
        shape = EnglishCoachShapes.card,
        colors = CardDefaults.cardColors(containerColor = EnglishCoachColors.Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .border(
                    width = 1.dp,
                    brush = EnglishCoachGradients.cardBorder,
                    shape = EnglishCoachShapes.card
                )
                .padding(EnglishCoachSpacing.cardPadding)
        ) {
            if (rotation <= 90f) {
                // Front Face
                CardFront(
                    word = word,
                    onSpeakWord = onSpeakWord
                )
            } else {
                // Back Face (counter-rotated so text is not mirrored)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { rotationY = 180f }
                ) {
                    CardBack(
                        word = word,
                        onSpeakSentence = onSpeakSentence
                    )
                }
            }
        }
    }
}

@Composable
private fun CardFront(
    word: Word,
    onSpeakWord: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Spacer(modifier = Modifier.height(EnglishCoachSpacing.md))

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(EnglishCoachSpacing.md)
        ) {
            Text(
                text = word.word,
                style = EnglishCoachTypography.cardWord,
                color = EnglishCoachColors.TextPrimary,
                textAlign = TextAlign.Center
            )

            if (word.phonetic.isNotBlank()) {
                Text(
                    text = word.phonetic,
                    style = EnglishCoachTypography.ipa,
                    color = EnglishCoachColors.Purple,
                    modifier = Modifier
                        .clip(EnglishCoachShapes.pill)
                        .background(EnglishCoachColors.Purple.copy(alpha = 0.1f))
                        .padding(horizontal = EnglishCoachSpacing.lg, vertical = EnglishCoachSpacing.xs)
                )
            }

            if (word.partOfSpeech.isNotBlank()) {
                Text(
                    text = word.partOfSpeech,
                    style = EnglishCoachTypography.pos,
                    color = EnglishCoachColors.TextSecondary,
                    modifier = Modifier
                        .clip(EnglishCoachShapes.secondaryButton)
                        .background(EnglishCoachColors.Border.copy(alpha = 0.5f))
                        .padding(horizontal = EnglishCoachSpacing.md, vertical = EnglishCoachSpacing.xxs)
                )
            }

            // Speaker pronunciation button
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(EnglishCoachGradients.purpleBlue)
                    .clickable(
                        role = Role.Button,
                        onClickLabel = "朗讀單字",
                        onClick = onSpeakWord
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = EnglishCoachIcons.Volume,
                    contentDescription = "朗讀單字",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        // Tap hint prompt
        Box(
            modifier = Modifier
                .clip(EnglishCoachShapes.pill)
                .background(EnglishCoachColors.Purple.copy(alpha = 0.1f))
                .padding(horizontal = EnglishCoachSpacing.lg, vertical = EnglishCoachSpacing.sm),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "點一下查看中文意思",
                style = EnglishCoachTypography.caption.copy(
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = EnglishCoachColors.Purple
            )
        }
    }
}

@Composable
private fun CardBack(
    word: Word,
    onSpeakSentence: (String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = word.word,
                style = EnglishCoachTypography.cardWordBack.copy(
                    fontSize = 20.sp
                ),
                color = EnglishCoachColors.TextSecondary
            )

            Spacer(modifier = Modifier.height(EnglishCoachSpacing.xs))

            Text(
                text = word.translation,
                style = EnglishCoachTypography.cardWordBack,
                color = EnglishCoachColors.TextPrimary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(EnglishCoachSpacing.md))
            HorizontalDivider(color = EnglishCoachColors.Border.copy(alpha = 0.6f))
            Spacer(modifier = Modifier.height(EnglishCoachSpacing.md))

            // Example sentence section
            if (word.example.isNotBlank()) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(EnglishCoachSpacing.sm)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(EnglishCoachSpacing.sm)
                    ) {
                        Text(
                            text = "例句：",
                            style = EnglishCoachTypography.caption.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = EnglishCoachColors.Purple
                        )

                        Row(
                            modifier = Modifier
                                .clip(EnglishCoachShapes.badge)
                                .background(EnglishCoachColors.Purple.copy(alpha = 0.1f))
                                .clickable(
                                    role = Role.Button,
                                    onClickLabel = "朗讀例句"
                                ) { onSpeakSentence(word.example) }
                                .padding(horizontal = EnglishCoachSpacing.sm, vertical = EnglishCoachSpacing.xxs),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(EnglishCoachSpacing.xxs)
                        ) {
                            Icon(
                                imageVector = EnglishCoachIcons.Volume,
                                contentDescription = "朗讀例句",
                                tint = EnglishCoachColors.Purple,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = "朗讀例句",
                                style = EnglishCoachTypography.badge,
                                color = EnglishCoachColors.Purple
                            )
                        }
                    }

                    Text(
                        text = word.example,
                        style = EnglishCoachTypography.bodyMedium,
                        color = EnglishCoachColors.TextPrimary
                    )

                    if (word.exampleTranslation.isNotBlank()) {
                        Text(
                            text = word.exampleTranslation,
                            style = EnglishCoachTypography.secondary.copy(
                                fontSize = 13.sp
                            ),
                            color = EnglishCoachColors.TextSecondary
                        )
                    }
                }
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(EnglishCoachSpacing.xxs)
        ) {
            Icon(
                imageVector = EnglishCoachIcons.Refresh,
                contentDescription = null,
                tint = EnglishCoachColors.TextSecondary,
                modifier = Modifier.size(13.dp)
            )
            Text(
                text = "點擊查看單字",
                style = EnglishCoachTypography.caption,
                color = EnglishCoachColors.TextSecondary
            )
        }
    }
}
