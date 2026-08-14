package com.example.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.model.StemTrack
import com.example.ui.theme.StudioBgDark
import com.example.ui.theme.StudioCyan
import com.example.ui.theme.StudioMint
import com.example.ui.theme.StudioRecRed
import com.example.ui.theme.StudioSurfaceBorder
import com.example.ui.theme.StudioSurfaceCard
import com.example.ui.theme.StudioSurfaceDark
import com.example.ui.theme.StudioSurfaceRack
import com.example.ui.theme.StudioTextMuted
import com.example.ui.theme.StudioTextPrimary
import com.example.ui.theme.StudioTextSecondary
import com.example.ui.theme.StudioTurquoise

@Composable
fun VocalInputSection(
    vocalStem: StemTrack?,
    isRecording: Boolean,
    recordingDurationSec: Int,
    liveMicLevel: Float,
    selectedVocalSource: String,
    playbackProgress: Float,
    onStartRecord: () -> Unit,
    onStopRecord: () -> Unit,
    onImportAudio: (Uri) -> Unit,
    onSelectPreset: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showPresetMenu by remember { mutableStateOf(false) }

    // Audio file picker launcher
    val audioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { onImportAudio(it) }
    }

    // Microphone runtime permission request
    val recordPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            onStartRecord()
        }
    }

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = StudioSurfaceCard,
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, StudioSurfaceBorder, RoundedCornerShape(24.dp))
            .testTag("vocal_input_section")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Header: Section label & Time/Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "VOCAL INPUT",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.8.sp,
                        fontSize = 11.sp
                    ),
                    color = StudioTextSecondary
                )

                Text(
                    text = if (isRecording) "REC 00:${String.format("%02d", recordingDurationSec)}" else selectedVocalSource,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    ),
                    color = if (isRecording) StudioRecRed else StudioTurquoise
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Waveform Visualizer
            VocalWaveformCanvas(
                waveformPoints = vocalStem?.waveformPoints ?: emptyList(),
                playbackProgress = playbackProgress,
                isRecording = isRecording,
                liveMicLevel = liveMicLevel,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Action Buttons Row: Replace / Preset, Record, Import
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Preset / Replace Menu Button
                Box(modifier = Modifier.weight(1f)) {
                    Button(
                        onClick = { showPresetMenu = true },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = StudioSurfaceRack,
                            contentColor = StudioTextPrimary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .border(1.dp, StudioSurfaceBorder, RoundedCornerShape(14.dp))
                            .testTag("preset_vocal_button")
                    ) {
                        Text(
                            text = "Preset Vocals ▾",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Medium,
                                fontSize = 13.sp
                            )
                        )
                    }

                    DropdownMenu(
                        expanded = showPresetMenu,
                        onDismissRequest = { showPresetMenu = false },
                        modifier = Modifier.background(StudioSurfaceRack)
                    ) {
                        DropdownMenuItem(
                            text = { Text("🎤 Reggaeton Hook (96 BPM)", color = StudioTextPrimary) },
                            onClick = {
                                onSelectPreset("Reggaeton")
                                showPresetMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("✨ Pop Melody Vocal (120 BPM)", color = StudioTextPrimary) },
                            onClick = {
                                onSelectPreset("Pop")
                                showPresetMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("🌙 R&B Chill Acapella (80 BPM)", color = StudioTextPrimary) },
                            onClick = {
                                onSelectPreset("R&B")
                                showPresetMenu = false
                            }
                        )
                    }
                }

                // Import Audio File Button
                Button(
                    onClick = { audioPickerLauncher.launch("audio/*") },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = StudioSurfaceRack,
                        contentColor = StudioTextPrimary
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .height(46.dp)
                        .border(1.dp, StudioSurfaceBorder, RoundedCornerShape(14.dp))
                        .testTag("import_audio_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.FileDownload,
                        contentDescription = "Import",
                        tint = StudioTextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Import",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Medium,
                            fontSize = 13.sp
                        )
                    )
                }

                // Quick Record Toggle Button
                if (isRecording) {
                    Button(
                        onClick = onStopRecord,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = StudioRecRed,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .height(46.dp)
                            .testTag("stop_recording_button")
                    ) {
                        Icon(
                            Icons.Default.Stop,
                            contentDescription = "Stop",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                } else {
                    Button(
                        onClick = {
                            val hasPermission = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.RECORD_AUDIO
                            ) == PackageManager.PERMISSION_GRANTED

                            if (hasPermission) {
                                onStartRecord()
                            } else {
                                recordPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = StudioTurquoise,
                            contentColor = StudioBgDark
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .height(46.dp)
                            .testTag("record_vocal_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Record",
                            tint = StudioBgDark,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun VocalWaveformCanvas(
    waveformPoints: List<Float>,
    playbackProgress: Float,
    isRecording: Boolean,
    liveMicLevel: Float,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(StudioBgDark)
            .border(1.dp, StudioSurfaceBorder.copy(alpha = 0.6f), RoundedCornerShape(14.dp))
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val width = size.width
            val height = size.height
            val centerY = height / 2f

            if (isRecording) {
                // Live glowing recording visualizer
                val barCount = 32
                val barWidth = width / (barCount * 1.6f)
                for (i in 0 until barCount) {
                    val x = i * (barWidth * 1.6f) + barWidth * 0.3f
                    val waveAmp = (liveMicLevel * (0.4f + 0.6f * kotlin.math.sin(i.toDouble()).toFloat())).coerceIn(0.12f, 1f)
                    val barH = (height * 0.8f * waveAmp).coerceAtLeast(4f)
                    drawRoundRect(
                        color = StudioRecRed,
                        topLeft = Offset(x, centerY - barH / 2f),
                        size = Size(barWidth, barH),
                        cornerRadius = CornerRadius(4f, 4f)
                    )
                }
            } else if (waveformPoints.isNotEmpty()) {
                val barCount = waveformPoints.size
                val barSpacing = width / barCount.toFloat()
                val barW = (barSpacing * 0.65f).coerceAtLeast(2.5f)

                for (i in 0 until barCount) {
                    val x = i * barSpacing
                    val amp = waveformPoints[i]
                    val barH = (height * 0.82f * amp).coerceAtLeast(4f)

                    val isPastPlayhead = (x / width) <= playbackProgress
                    val color = if (isPastPlayhead) {
                        StudioTurquoise
                    } else {
                        StudioTurquoise.copy(alpha = 0.35f)
                    }

                    drawRoundRect(
                        color = color,
                        topLeft = Offset(x, centerY - barH / 2f),
                        size = Size(barW, barH),
                        cornerRadius = CornerRadius(3f, 3f)
                    )
                }

                // Playhead cursor line
                val cursorX = width * playbackProgress
                drawLine(
                    color = StudioTurquoise,
                    start = Offset(cursorX, 0f),
                    end = Offset(cursorX, height),
                    strokeWidth = 2.dp.toPx()
                )
            } else {
                // Default subtle harmonic bars pattern
                val barCount = 28
                val barSpacing = width / barCount.toFloat()
                val barW = (barSpacing * 0.55f).coerceAtLeast(2.5f)
                val defaultAmps = floatArrayOf(
                    0.25f, 0.45f, 0.85f, 0.65f, 0.35f, 0.95f, 0.55f, 0.30f, 0.70f, 0.40f,
                    0.80f, 0.50f, 0.30f, 0.60f, 0.85f, 0.40f, 0.75f, 0.90f, 0.50f, 0.35f,
                    0.65f, 0.80f, 0.45f, 0.30f, 0.70f, 0.55f, 0.40f, 0.20f
                )
                for (i in 0 until barCount) {
                    val x = i * barSpacing
                    val amp = defaultAmps[i % defaultAmps.size]
                    val barH = (height * 0.75f * amp).coerceAtLeast(4f)
                    val alpha = (0.3f + 0.7f * amp).coerceIn(0.2f, 1f)
                    drawRoundRect(
                        color = StudioTurquoise.copy(alpha = alpha),
                        topLeft = Offset(x, centerY - barH / 2f),
                        size = Size(barW, barH),
                        cornerRadius = CornerRadius(3f, 3f)
                    )
                }
            }
        }
    }
}

