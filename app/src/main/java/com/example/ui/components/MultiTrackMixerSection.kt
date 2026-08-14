package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Piano
import androidx.compose.material.icons.filled.Speaker
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.StemTrack
import com.example.model.StemType
import com.example.ui.theme.StudioBgDark
import com.example.ui.theme.StudioCyan
import com.example.ui.theme.StudioMuteRed
import com.example.ui.theme.StudioSoloYellow
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
fun MultiTrackMixerSection(
    stems: List<StemTrack>,
    stemPeaks: Map<String, Float>,
    onVolumeChanged: (String, Float) -> Unit,
    onMuteToggle: (String) -> Unit,
    onSoloToggle: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = StudioSurfaceCard,
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, StudioSurfaceBorder, RoundedCornerShape(24.dp))
            .testTag("multitrack_mixer_section")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "MULTI-TRACK STEM MIXER",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.8.sp,
                        fontSize = 11.sp
                    ),
                    color = StudioTextSecondary
                )

                Text(
                    text = "${stems.size} Channels",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    ),
                    color = StudioTurquoise
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Channel Strips List
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                stems.forEach { stem ->
                    val peak = stemPeaks[stem.id] ?: 0f
                    StemChannelStrip(
                        stem = stem,
                        livePeak = peak,
                        onVolumeChanged = { onVolumeChanged(stem.id, it) },
                        onMuteToggle = { onMuteToggle(stem.id) },
                        onSoloToggle = { onSoloToggle(stem.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun StemChannelStrip(
    stem: StemTrack,
    livePeak: Float,
    onVolumeChanged: (Float) -> Unit,
    onMuteToggle: () -> Unit,
    onSoloToggle: () -> Unit
) {
    val animatedPeak by animateFloatAsState(
        targetValue = if (stem.isMuted) 0f else livePeak.coerceIn(0f, 1f),
        label = "peak_${stem.id}"
    )

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = StudioSurfaceRack,
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                if (stem.isSolo) StudioSoloYellow.copy(alpha = 0.6f) else StudioSurfaceBorder,
                RoundedCornerShape(16.dp)
            )
            .testTag("channel_strip_${stem.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Stem Icon & Name
            Row(
                modifier = Modifier.width(105.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(stem.color.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = getStemIcon(stem.type),
                        contentDescription = null,
                        tint = stem.color,
                        modifier = Modifier.size(14.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = stem.name,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp
                        ),
                        color = if (stem.isMuted) StudioTextMuted else StudioTextPrimary,
                        maxLines = 1
                    )
                    Text(
                        text = "${(stem.volume * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = StudioTextSecondary
                    )
                }
            }

            // Live Channel VU Meter
            StemMiniVUMeter(
                peak = animatedPeak,
                modifier = Modifier.width(16.dp).height(28.dp)
            )

            Spacer(modifier = Modifier.width(8.dp))

            // Volume Slider
            Slider(
                value = stem.volume,
                onValueChange = onVolumeChanged,
                valueRange = 0f..1f,
                colors = SliderDefaults.colors(
                    thumbColor = stem.color,
                    activeTrackColor = stem.color,
                    inactiveTrackColor = StudioBgDark
                ),
                modifier = Modifier
                    .weight(1f)
                    .testTag("volume_slider_${stem.id}")
            )

            Spacer(modifier = Modifier.width(8.dp))

            // Solo (S) Button
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (stem.isSolo) StudioSoloYellow else StudioSurfaceDark)
                    .border(1.dp, if (stem.isSolo) StudioSoloYellow else StudioSurfaceBorder, RoundedCornerShape(8.dp))
                    .clickable { onSoloToggle() }
                    .testTag("solo_button_${stem.id}"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "S",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    ),
                    color = if (stem.isSolo) StudioBgDark else StudioTextSecondary
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Mute (M) Button
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (stem.isMuted) StudioMuteRed else StudioSurfaceDark)
                    .border(1.dp, if (stem.isMuted) StudioMuteRed else StudioSurfaceBorder, RoundedCornerShape(8.dp))
                    .clickable { onMuteToggle() }
                    .testTag("mute_button_${stem.id}"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "M",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    ),
                    color = if (stem.isMuted) Color.White else StudioTextSecondary
                )
            }
        }
    }
}

@Composable
private fun StemMiniVUMeter(peak: Float, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(2.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 6 vertical LED segments
        for (i in 5 downTo 0) {
            val threshold = (i + 1) / 6.0f
            val isLit = peak >= threshold
            val color = when {
                i >= 5 -> StudioVUMeterRed
                i >= 4 -> StudioVUMeterYellow
                else -> StudioTurquoise
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .clip(RoundedCornerShape(0.5.dp))
                    .background(if (isLit) color else StudioBgDark)
            )
        }
    }
}

private fun getStemIcon(type: StemType): ImageVector {
    return when (type) {
        StemType.VOCAL -> Icons.Default.Mic
        StemType.DRUMS -> Icons.Default.GraphicEq
        StemType.BASS -> Icons.Default.Speaker
        StemType.CHORDS -> Icons.Default.Piano
        StemType.MELODY -> Icons.Default.MusicNote
        StemType.FX -> Icons.Default.VolumeUp
    }
}

