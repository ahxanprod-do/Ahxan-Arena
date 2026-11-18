package com.example.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.ui.components.ArenaBrandEmblem
import com.example.ui.components.StatusPill
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.EmeraldProfit
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun SplashScreen() {
    val scaleAnim = remember { Animatable(0.76f) }
    val alphaAnim = remember { Animatable(0f) }
    val progressAnim = remember { Animatable(0.08f) }

    val infiniteTransition = rememberInfiniteTransition(label = "splash_glow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.22f,
        targetValue = 0.65f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_alpha"
    )

    LaunchedEffect(Unit) {
        scaleAnim.animateTo(1f, animationSpec = tween(550, easing = FastOutSlowInEasing))
    }
    LaunchedEffect(Unit) {
        alphaAnim.animateTo(1f, animationSpec = tween(450))
    }
    LaunchedEffect(Unit) {
        progressAnim.animateTo(1f, animationSpec = tween(1450, easing = FastOutSlowInEasing))
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(Color(0xFF121C38), Color(0xFF090D18), ObsidianBg)
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("splash_screen"),
        contentAlignment = Alignment.Center
    ) {
        // Ambient pulsing cyber halo behind emblem
        Box(
            modifier = Modifier
                .size(210.dp)
                .scale(scaleAnim.value * 1.06f)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(
                            NeonCyan.copy(alpha = glowAlpha * 0.42f),
                            ElectricViolet.copy(alpha = glowAlpha * 0.25f),
                            Color.Transparent
                        )
                    )
                )
                .border(
                    width = 1.dp,
                    color = NeonCyan.copy(alpha = glowAlpha * 0.45f),
                    shape = CircleShape
                )
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp)
                .alpha(alphaAnim.value),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            ArenaBrandEmblem(
                size = 112.dp,
                modifier = Modifier.scale(scaleAnim.value)
            )

            Spacer(Modifier.height(24.dp))

            StatusPill(label = "CYBER ESPORTS PLATFORM", color = EmeraldProfit)

            Spacer(Modifier.height(12.dp))

            Text(
                text = "AHXAN ARENA",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.ExtraBold,
                color = TextPrimary,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(6.dp))

            Text(
                text = "BGMI • Free Fire MAX • COD Mobile • Valorant",
                style = MaterialTheme.typography.labelLarge,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(36.dp))

            LinearProgressIndicator(
                progress = { progressAnim.value },
                modifier = Modifier
                    .width(210.dp)
                    .height(6.dp)
                    .clip(RoundedCornerShape(50)),
                color = NeonCyan,
                trackColor = BorderSubtle
            )

            Spacer(Modifier.height(10.dp))

            Text(
                text = "INITIALIZING ARENA COMMAND...",
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted
            )
        }
    }
}
