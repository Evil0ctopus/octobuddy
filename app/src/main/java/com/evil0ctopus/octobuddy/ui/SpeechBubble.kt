package com.evil0ctopus.octobuddy.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.evil0ctopus.octobuddy.ui.theme.Brand

/**
 * PorkChop-style speech bubble above the pet (paper + ink, stepped tail).
 */
@Composable
fun SpeechBubble(
    text: String?,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = !text.isNullOrBlank(),
        enter = fadeIn() + scaleIn(initialScale = 0.85f),
        exit = fadeOut() + scaleOut(targetScale = 0.9f),
        modifier = modifier,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .widthIn(min = 72.dp, max = 260.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFFAFAF4))
                    .border(1.5.dp, Color(0xFF18121C), RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = text.orEmpty(),
                    style = MaterialTheme.typography.labelLarge,
                    color = Color(0xFF18121C),
                    textAlign = TextAlign.Center,
                )
            }
            // Stepped tail
            Box(
                modifier = Modifier
                    .size(width = 16.dp, height = 10.dp)
                    .clip(
                        GenericShape { size, _ ->
                            moveTo(size.width * 0.15f, 0f)
                            lineTo(size.width * 0.85f, 0f)
                            lineTo(size.width * 0.55f, size.height)
                            close()
                        },
                    )
                    .background(Color(0xFFFAFAF4)),
            )
        }
    }
}
