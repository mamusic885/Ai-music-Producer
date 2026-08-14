package com.example.ui.components

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
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.PitchDetectorAndAutoTune
import com.example.model.AutoTuneSettings
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
fun AutoTuneSection(
    settings: AutoTuneSettings,
    onSettingsChanged: (AutoTuneSettings) -> Unit,
    modifier: Modifier = Modifier
) {
    var showKeyMenu by remember { mutableStateOf(false) }
    val availableKeys = remember { PitchDetectorAndAutoTune.getAvailableKeys() }

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = StudioSurfaceCard,
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, StudioSurfaceBorder, RoundedCornerShape(24.dp))
            .testTag("autotune_section")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Title Bar & Master Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "VOCAL AUTO-TUNE DSP",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 1.8.sp,
                            fontSize = 11.sp
                        ),
                        color = StudioTextSecondary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (settings.isEnabled) "Pitch scale quantization active" else "Bypassed (Original Pitch)",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = if (settings.isEnabled) StudioTurquoise else StudioTextMuted
                    )
                }

                Switch(
                    checked = settings.isEnabled,
                    onCheckedChange = { onSettingsChanged(settings.copy(isEnabled = it)) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = StudioBgDark,
                        checkedTrackColor = StudioTurquoise,
                        uncheckedThumbColor = StudioTextMuted,
                        uncheckedTrackColor = StudioSurfaceRack
                    ),
                    modifier = Modifier.testTag("autotune_switch")
                )
            }

            if (settings.isEnabled) {
                Spacer(modifier = Modifier.height(16.dp))

                // Scale / Key Selector Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Target Scale & Key",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Medium,
                            fontSize = 13.sp
                        ),
                        color = StudioTextPrimary
                    )

                    Box {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .background(StudioSurfaceRack)
                                .border(1.dp, StudioSurfaceBorder, RoundedCornerShape(14.dp))
                                .clickable { showKeyMenu = true }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                                .testTag("autotune_key_selector")
                        ) {
                            Text(
                                text = "${settings.targetKey} Scale ▾",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = StudioTurquoise
                                )
                            )
                        }

                        DropdownMenu(
                            expanded = showKeyMenu,
                            onDismissRequest = { showKeyMenu = false },
                            modifier = Modifier.background(StudioSurfaceRack)
                        ) {
                            availableKeys.forEach { keyName ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = keyName,
                                            color = if (keyName == settings.targetKey) StudioTurquoise else StudioTextPrimary
                                        )
                                    },
                                    onClick = {
                                        onSettingsChanged(settings.copy(targetKey = keyName))
                                        showKeyMenu = false
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Speed / Retune Speed Slider
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Correction Speed",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                            color = StudioTextSecondary
                        )
                        Text(
                            text = "${(settings.speed * 100).toInt()}% • ${getSpeedLabel(settings.speed)}",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            ),
                            color = StudioTurquoise
                        )
                    }

                    Slider(
                        value = settings.speed,
                        onValueChange = { onSettingsChanged(settings.copy(speed = it)) },
                        valueRange = 0.1f..1.0f,
                        colors = SliderDefaults.colors(
                            thumbColor = StudioTurquoise,
                            activeTrackColor = StudioTurquoise,
                            inactiveTrackColor = StudioSurfaceRack
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("autotune_speed_slider")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Natural",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = StudioTextMuted
                        )
                        Text(
                            text = "Modern Pop",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = StudioTextMuted
                        )
                        Text(
                            text = "Hard Snap",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = StudioTextMuted
                        )
                    }
                }
            }
        }
    }
}

private fun getSpeedLabel(speed: Float): String {
    return when {
        speed < 0.4f -> "Natural Vibrato"
        speed < 0.75f -> "Modern Vocal"
        else -> "Robotic Hard Auto-Tune"
    }
}

