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
import com.example.ui.theme.ArenaCharcoal
import com.example.ui.theme.ArenaOrange
import com.example.ui.theme.ArenaPurple
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.CoinYellow
import com.example.ui.theme.HeaderNavy
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun SplashScreen() {
    val scaleAnim = remember { Animatable(0.78f) }
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
                Brush.verticalGradient(
                    colors = listOf(HeaderNavy, ArenaCharcoal, HeaderNavy)
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("splash_screen"),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(210.dp)
                .scale(scaleAnim.value * 1.06f)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(
                            ArenaOrange.copy(alpha = glowAlpha * 0.42f),
                            ArenaPurple.copy(alpha = glowAlpha * 0.25f),
                            Color.Transparent
                        )
                    )
                )
                .border(
                    width = 1.5.dp,
                    color = CoinYellow.copy(alpha = glowAlpha * 0.55f),
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

            StatusPill(label = "ESPORTS TOURNAMENT ARENA", color = CoinYellow)

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
                color = ArenaOrange,
                trackColor = BorderSubtle
            )

            Spacer(Modifier.height(10.dp))

            Text(
                text = "LOADING TOURNAMENT LOBBY...",
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted
            )
        }
    }
}
