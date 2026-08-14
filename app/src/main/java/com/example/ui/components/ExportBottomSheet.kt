package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ArrangementRecipe
import com.example.ui.theme.StudioBgDark
import com.example.ui.theme.StudioMint
import com.example.ui.theme.StudioSurfaceBorder
import com.example.ui.theme.StudioSurfaceDark
import com.example.ui.theme.StudioSurfaceRack
import com.example.ui.theme.StudioTextMuted
import com.example.ui.theme.StudioTextPrimary
import com.example.ui.theme.StudioTextSecondary
import com.example.ui.theme.StudioTurquoise
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportBottomSheet(
    recipe: ArrangementRecipe,
    isExporting: Boolean,
    exportedFile: File?,
    onDismiss: () -> Unit,
    onSaveToLibrary: () -> Unit,
    onShare: () -> Unit,
    sheetState: SheetState
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = StudioSurfaceDark,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .size(width = 36.dp, height = 4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(StudioSurfaceBorder)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
                .padding(bottom = 28.dp)
                .testTag("export_bottom_sheet")
        ) {
            // Title
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(StudioTurquoise.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = StudioTurquoise,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column {
                    Text(
                        text = "EXPORT MASTER AUDIO",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        ),
                        color = StudioTextPrimary
                    )
                    Text(
                        text = "Uncompressed 16-bit 44.1 kHz WAV Master",
                        style = MaterialTheme.typography.bodySmall,
                        color = StudioTextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Track summary card
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = StudioSurfaceRack,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, StudioSurfaceBorder, RoundedCornerShape(18.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "${recipe.genre} Arrangement",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = StudioTurquoise
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Key: ${recipe.musicalKey}  •  Tempo: ${recipe.bpm} BPM  •  Stems: 6 Tracks Mixed",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = StudioTextMuted
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "DSP: Vocal Auto-Tune + Sidechain Ducking + Analog Tape Master + Brickwall Limiter",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = StudioTurquoise
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            if (isExporting) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        color = StudioTurquoise,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Rendering Master Mixdown...",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = StudioTurquoise
                    )
                }
            } else {
                // Action Buttons: Save & Share
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Save to Library
                    Button(
                        onClick = onSaveToLibrary,
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = StudioSurfaceRack,
                            contentColor = StudioTextPrimary
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .border(1.dp, StudioSurfaceBorder, RoundedCornerShape(16.dp))
                            .testTag("save_to_library_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = "Save",
                            tint = StudioTurquoise
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Save to Music",
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    // Share
                    Button(
                        onClick = onShare,
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = StudioTurquoise,
                            contentColor = StudioBgDark
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .testTag("share_master_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share",
                            tint = StudioBgDark
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Share Master",
                            fontWeight = FontWeight.Bold,
                            color = StudioBgDark
                        )
                    }
                }
            }
        }
    }
}

