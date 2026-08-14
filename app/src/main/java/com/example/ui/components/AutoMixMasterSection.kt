package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AutoMasterSettings
import com.example.model.AutoMixSettings
import com.example.ui.theme.StudioAnalogWarm
import com.example.ui.theme.StudioBgDark
import com.example.ui.theme.StudioCyan
import com.example.ui.theme.StudioMint
import com.example.ui.theme.StudioSurfaceBorder
import com.example.ui.theme.StudioSurfaceCard
import com.example.ui.theme.StudioSurfaceRack
import com.example.ui.theme.StudioTextMuted
import com.example.ui.theme.StudioTextPrimary
import com.example.ui.theme.StudioTextSecondary
import com.example.ui.theme.StudioTurquoise

@Composable
fun AutoMixMasterSection(
    mixSettings: AutoMixSettings,
    masterSettings: AutoMasterSettings,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onMixSettingsChanged: (AutoMixSettings) -> Unit,
    onMasterSettingsChanged: (AutoMasterSettings) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = StudioSurfaceCard,
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, StudioSurfaceBorder, RoundedCornerShape(24.dp))
            .testTag("auto_mix_master_section")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Header with Expand/Collapse toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleExpand() },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "AUTO-MIX & AUTO-MASTER RACK",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 1.8.sp,
                            fontSize = 11.sp
                        ),
                        color = StudioTextSecondary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Sidechain • Reverb • Analog Tape • Brickwall Limiter",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = StudioTurquoise
                    )
                }

                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = "Expand/Collapse",
                    tint = StudioTextSecondary
                )
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {

                    // ==========================================
                    // 1. AUTO-MIX MODULE
                    // ==========================================
                    Text(
                        text = "1. AUTO-MIX PROCESSING",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            fontSize = 10.sp
                        ),
                        color = StudioTurquoise
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Sidechain & Vocal Clarity Switches
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        DspToggleChip(
                            label = "Kick Sidechain",
                            detail = "Ducks bass on kick hits",
                            isChecked = mixSettings.kickSidechainDucking,
                            onToggle = { onMixSettingsChanged(mixSettings.copy(kickSidechainDucking = it)) },
                            modifier = Modifier.weight(1f)
                        )
                        DspToggleChip(
                            label = "Vocal Clarity",
                            detail = "Presence EQ boost",
                            isChecked = mixSettings.vocalClarityBoost,
                            onToggle = { onMixSettingsChanged(mixSettings.copy(vocalClarityBoost = it)) },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Reverb Space Slider
                    DspSliderRow(
                        label = "Reverb Room Ambience",
                        value = mixSettings.reverbSpace,
                        valueDisplay = "${(mixSettings.reverbSpace * 100).toInt()}%",
                        onValueChange = { onMixSettingsChanged(mixSettings.copy(reverbSpace = it)) },
                        color = StudioTurquoise
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Stereo Width Slider
                    DspSliderRow(
                        label = "Stereo Width Expander",
                        value = mixSettings.stereoWidth,
                        valueDisplay = "${(mixSettings.stereoWidth * 100).toInt()}%",
                        onValueChange = { onMixSettingsChanged(mixSettings.copy(stereoWidth = it)) },
                        color = StudioTurquoise
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = StudioSurfaceBorder)
                    Spacer(modifier = Modifier.height(16.dp))

                    // ==========================================
                    // 2. AUTO-MASTER MODULE
                    // ==========================================
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "2. AUTO-MASTERING CHAIN",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                fontSize = 10.sp
                            ),
                            color = StudioAnalogWarm
                        )

                        // Limiter Safe Badge
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = StudioTurquoise,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Limiter -0.3 dBFS",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = StudioTurquoise
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Analog Tape Warmth Slider
                    DspSliderRow(
                        label = "Analog Tape Saturation",
                        value = masterSettings.analogWarmth,
                        valueDisplay = "${(masterSettings.analogWarmth * 100).toInt()}%",
                        onValueChange = { onMasterSettingsChanged(masterSettings.copy(analogWarmth = it)) },
                        color = StudioAnalogWarm
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Target Loudness
                    DspSliderRow(
                        label = "Master Output Loudness",
                        value = masterSettings.loudnessTarget,
                        valueDisplay = "${(masterSettings.loudnessTarget * 100).toInt()}%",
                        onValueChange = { onMasterSettingsChanged(masterSettings.copy(loudnessTarget = it)) },
                        color = StudioTurquoise
                    )
                }
            }
        }
    }
}

@Composable
private fun DspToggleChip(
    label: String,
    detail: String,
    isChecked: Boolean,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = StudioSurfaceRack,
        modifier = modifier
            .border(
                1.dp,
                if (isChecked) StudioTurquoise.copy(alpha = 0.5f) else StudioSurfaceBorder,
                RoundedCornerShape(14.dp)
            )
            .clickable { onToggle(!isChecked) }
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    ),
                    color = if (isChecked) StudioTurquoise else StudioTextPrimary
                )
                Text(
                    text = detail,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                    color = StudioTextMuted
                )
            }
            Switch(
                checked = isChecked,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = StudioBgDark,
                    checkedTrackColor = StudioTurquoise,
                    uncheckedThumbColor = StudioTextMuted,
                    uncheckedTrackColor = StudioBgDark
                ),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun DspSliderRow(
    label: String,
    value: Float,
    valueDisplay: String,
    onValueChange: (Float) -> Unit,
    color: Color
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                color = StudioTextSecondary
            )
            Text(
                text = valueDisplay,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                ),
                color = color
            )
        }

        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = 0f..1f,
            colors = SliderDefaults.colors(
                thumbColor = color,
                activeTrackColor = color,
                inactiveTrackColor = StudioSurfaceRack
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

