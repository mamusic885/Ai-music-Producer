package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.GenerationStep
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
import com.example.ui.theme.StudioTurquoiseBright

@Composable
fun AiPromptArrangementSection(
    promptText: String,
    onPromptChanged: (String) -> Unit,
    isGenerating: Boolean,
    generationStep: GenerationStep,
    onGenerate: () -> Unit,
    modifier: Modifier = Modifier
) {
    val promptSuggestions = listOf(
        "Reggaeton beat, soft plucks in intro, guitar in background, deep bass, emotional chords",
        "Trap 808 beat, dark piano chords, flute melody, fast hi-hats, hard bass",
        "Afrobeats groove, emotional electric guitar, bouncy log drum, warm pads",
        "Synthwave 80s neon beat, retro arpeggio, analog synth chords, punchy drums",
        "Acoustic Lo-Fi beat, vinyl crackle, gentle nylon guitar, chill sub bass"
    )

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = StudioSurfaceCard,
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, StudioSurfaceBorder, RoundedCornerShape(24.dp))
            .testTag("ai_prompt_section")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Header
            Text(
                text = "AI GENERATION INSTRUCTION",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.8.sp,
                    fontSize = 11.sp
                ),
                color = StudioTextSecondary
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Prompt Text Field with Dark Input Canvas
            OutlinedTextField(
                value = promptText,
                onValueChange = onPromptChanged,
                placeholder = {
                    Text(
                        text = "e.g., Reggaeton beat, soft plucks in intro, guitar in background, deep bass...",
                        color = StudioTextMuted,
                        fontSize = 13.sp
                    )
                },
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = StudioTurquoise.copy(alpha = 0.8f),
                    unfocusedBorderColor = StudioSurfaceBorder,
                    focusedTextColor = StudioTextPrimary,
                    unfocusedTextColor = StudioTextPrimary,
                    focusedContainerColor = StudioBgDark,
                    unfocusedContainerColor = StudioBgDark
                ),
                minLines = 2,
                maxLines = 4,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("ai_prompt_input")
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Professional Pill Feature Chips (Auto-Tune, Auto Mix, Auto Master & Presets)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Active DSP Feature Badges
                DspFeatureBadge(label = "Auto-Tune", isHighlit = true)
                DspFeatureBadge(label = "Auto Mix", isHighlit = true)
                DspFeatureBadge(label = "Auto Master", isHighlit = false)

                promptSuggestions.forEach { suggestion ->
                    val tagTitle = when {
                        suggestion.startsWith("Reggaeton") -> "Reggaeton"
                        suggestion.startsWith("Trap") -> "Trap 808"
                        suggestion.startsWith("Afrobeats") -> "Afrobeats"
                        suggestion.startsWith("Synthwave") -> "Synthwave"
                        else -> "Lo-Fi"
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(StudioSurfaceRack)
                            .border(1.dp, StudioSurfaceBorder, RoundedCornerShape(20.dp))
                            .clickable { onPromptChanged(suggestion) }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = tagTitle,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = StudioTextSecondary,
                                fontSize = 10.sp,
                                letterSpacing = 0.5.sp
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Primary Generate Button in Professional Polish style
            if (isGenerating) {
                GeneratingProgressCard(step = generationStep)
            } else {
                Button(
                    onClick = onGenerate,
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = StudioTurquoise,
                        contentColor = StudioBgDark
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("generate_arrangement_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.ElectricBolt,
                        contentDescription = "Generate",
                        tint = StudioBgDark,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Generate Arrangement",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            letterSpacing = 0.2.sp
                        ),
                        color = StudioBgDark
                    )
                }
            }
        }
    }
}

@Composable
private fun DspFeatureBadge(label: String, isHighlit: Boolean) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(
                if (isHighlit) StudioTurquoise.copy(alpha = 0.12f) else StudioSurfaceRack
            )
            .border(
                1.dp,
                if (isHighlit) StudioTurquoise.copy(alpha = 0.35f) else StudioSurfaceBorder,
                RoundedCornerShape(20.dp)
            )
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp,
                letterSpacing = 1.sp
            ),
            color = if (isHighlit) StudioTurquoise else StudioTextSecondary
        )
    }
}

@Composable
private fun GeneratingProgressCard(step: GenerationStep) {
    val infiniteTransition = rememberInfiniteTransition(label = "gen_glow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_alpha"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(StudioSurfaceRack)
            .border(1.dp, StudioTurquoise.copy(alpha = glowAlpha), RoundedCornerShape(16.dp))
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    color = StudioTurquoise,
                    strokeWidth = 2.5.dp
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = step.stepName,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = StudioTurquoiseBright
                    )
                )
            }
            Text(
                text = "${(step.progress * 100).toInt()}%",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = StudioTurquoise
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = step.detail,
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
            color = StudioTextSecondary
        )

        Spacer(modifier = Modifier.height(8.dp))

        LinearProgressIndicator(
            progress = { step.progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp)),
            color = StudioTurquoise,
            trackColor = StudioBgDark
        )
    }
}

