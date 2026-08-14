package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ArrangementRecipe
import com.example.ui.theme.StudioBgDark
import com.example.ui.theme.StudioCyan
import com.example.ui.theme.StudioMint
import com.example.ui.theme.StudioSurfaceBorder
import com.example.ui.theme.StudioSurfaceCard
import com.example.ui.theme.StudioSurfaceDark
import com.example.ui.theme.StudioSurfaceRack
import com.example.ui.theme.StudioTextMuted
import com.example.ui.theme.StudioTextPrimary
import com.example.ui.theme.StudioTextSecondary
import com.example.ui.theme.StudioTurquoise
import com.example.ui.theme.StudioVUMeterGreen
import com.example.ui.theme.StudioVUMeterRed
import com.example.ui.theme.StudioVUMeterYellow

@Composable
fun StudioTopBar(
    recipe: ArrangementRecipe,
    masterPeakLevel: Float,
    modifier: Modifier = Modifier
) {
    Surface(
        color = StudioBgDark,
        modifier = modifier
            .fillMaxWidth()
            .testTag("studio_top_bar")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // App Branding & Professional Diamond Logo
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(StudioTurquoise),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .rotate(45f)
                                .clip(RoundedCornerShape(2.dp))
                                .background(StudioBgDark)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "AI Producer",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = (-0.3).sp,
                                    fontSize = 18.sp
                                ),
                                color = StudioTextPrimary
                            )
                        }
                        Text(
                            text = "24-bit DSP Master Engine",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                letterSpacing = 0.5.sp
                            ),
                            color = StudioTextMuted
                        )
                    }
                }

                // Live Master Stereo VU Level Meter & Action Button
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MasterVUMeter(peakLevel = masterPeakLevel)

                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(StudioSurfaceRack)
                            .border(1.dp, StudioSurfaceBorder, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Options",
                            tint = StudioTextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Studio Info Badges (Key, BPM, Genre) in Professional Polish Pill Format
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                StudioBadgeChip(
                    icon = Icons.Default.MusicNote,
                    label = "Key",
                    value = recipe.musicalKey,
                    accentColor = StudioTurquoise,
                    modifier = Modifier.weight(1f)
                )
                StudioBadgeChip(
                    icon = Icons.Default.Speed,
                    label = "Tempo",
                    value = "${recipe.bpm} BPM",
                    accentColor = StudioTurquoise,
                    modifier = Modifier.weight(1f)
                )
                StudioBadgeChip(
                    icon = Icons.Default.AutoAwesome,
                    label = "Genre",
                    value = recipe.genre,
                    accentColor = StudioTurquoise,
                    modifier = Modifier.weight(1.2f)
                )
            }
        }
    }
}

@Composable
private fun StudioBadgeChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(StudioSurfaceCard)
            .border(1.dp, StudioSurfaceBorder, RoundedCornerShape(16.dp))
            .padding(horizontal = 10.dp, vertical = 7.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(13.dp)
            )
            Column {
                Text(
                    text = label.uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    ),
                    color = StudioTextMuted
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = StudioTextPrimary,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun MasterVUMeter(peakLevel: Float) {
    val animatedPeak by animateFloatAsState(
        targetValue = peakLevel.coerceIn(0f, 1f),
        label = "master_vu_anim"
    )

    Column(
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "OUT",
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            ),
            color = StudioTextMuted
        )
        Spacer(modifier = Modifier.height(2.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 8 LED Segments for VU
            for (i in 0 until 8) {
                val threshold = (i + 1) / 8.0f
                val isLit = animatedPeak >= threshold
                val ledColor = when {
                    i >= 7 -> StudioVUMeterRed
                    i >= 5 -> StudioVUMeterYellow
                    else -> StudioTurquoise
                }
                Box(
                    modifier = Modifier
                        .size(width = 4.dp, height = 12.dp)
                        .clip(RoundedCornerShape(1.dp))
                        .background(if (isLit) ledColor else StudioSurfaceBorder.copy(alpha = 0.5f))
                )
            }
        }
    }
}

