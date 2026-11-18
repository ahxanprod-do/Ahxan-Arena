package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.BorderGlow
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.CrimsonDanger
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.EmeraldProfit
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.ObsidianCardGlass
import com.example.ui.theme.ObsidianElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary

fun bannerDrawableForKey(key: String): Int = when (key.lowercase()) {
    "freefire" -> R.drawable.img_banner_freefire
    "codm" -> R.drawable.img_banner_codm
    "hero" -> R.drawable.img_arena_hero
    else -> R.drawable.img_banner_bgmi
}

@Composable
fun ArenaBrandEmblem(
    modifier: Modifier = Modifier,
    size: Dp = 76.dp
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.28f))
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFF0A1124), Color(0xFF1A1038))
                )
            )
            .border(
                width = 1.8.dp,
                brush = Brush.linearGradient(listOf(NeonCyan, ElectricViolet, EmeraldProfit)),
                shape = RoundedCornerShape(size * 0.28f)
            )
            .padding(size * 0.1f),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_arena_emblem),
            contentDescription = "Ahxan Arena Logo",
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
fun CyberGlassCard(
    modifier: Modifier = Modifier,
    glowColor: Color = NeonCyan,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(18.dp)
    val baseModifier = modifier
        .fillMaxWidth()
        .clip(shape)
        .background(
            Brush.verticalGradient(
                listOf(ObsidianElevated.copy(alpha = 0.92f), ObsidianCardGlass)
            )
        )
        .border(
            width = 1.dp,
            brush = Brush.linearGradient(
                listOf(glowColor.copy(alpha = 0.45f), ElectricViolet.copy(alpha = 0.25f))
            ),
            shape = shape
        )
        .let { if (onClick != null) it.clickable { onClick() } else it }

    Column(
        modifier = baseModifier.padding(16.dp),
        content = content
    )
}

@Composable
fun NeonPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    accentColor: Color = NeonCyan
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(50.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = accentColor,
            contentColor = ObsidianBg,
            disabledContainerColor = ObsidianElevated,
            disabledContentColor = TextMuted
        )
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun StatusPill(
    label: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = color.copy(alpha = 0.16f),
        shape = RoundedCornerShape(50),
        border = BorderStroke(1.dp, color.copy(alpha = 0.55f))
    ) {
        Text(
            text = label.uppercase(),
            color = color,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun StatusToastBanner(
    banner: Pair<Boolean, String>?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = banner != null,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        banner?.let { (ok, message) ->
            val accent = if (ok) EmeraldProfit else CrimsonDanger
            Card(
                colors = CardDefaults.cardColors(containerColor = ObsidianElevated),
                border = BorderStroke(1.dp, accent.copy(alpha = 0.7f)),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Icon(
                        imageVector = if (ok) Icons.Default.CheckCircle else Icons.Default.Error,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = message,
                        color = TextPrimary,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("dismiss_status_banner")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = TextMuted, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun SectionHeader(
    title: String,
    subtitle: String? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted
                )
            }
        }
        if (actionLabel != null && onAction != null) {
            Text(
                text = actionLabel,
                style = MaterialTheme.typography.labelLarge,
                color = NeonCyan,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onAction() }
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            )
        }
    }
}

@Composable
fun SlotsProgressBar(filled: Long, max: Long) {
    val progress = if (max > 0) (filled.toFloat() / max.toFloat()).coerceIn(0f, 1f) else 0f
    val barColor = when {
        progress >= 0.9f -> CrimsonDanger
        progress >= 0.6f -> AmberWarning
        else -> NeonCyan
    }
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "${(max - filled).coerceAtLeast(0)} spots left",
                style = MaterialTheme.typography.labelSmall,
                color = barColor
            )
            Text(
                text = "$filled / $max Slots",
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted
            )
        }
        Spacer(Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(CircleShape),
            color = barColor,
            trackColor = BorderSubtle
        )
    }
}

@Composable
fun KpiMiniStat(
    label: String,
    value: String,
    accent: Color = NeonCyan,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(ObsidianElevated.copy(alpha = 0.85f))
            .border(1.dp, BorderGlow.copy(alpha = 0.25f), RoundedCornerShape(14.dp))
            .padding(12.dp)
    ) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = TextMuted, maxLines = 1)
        Spacer(Modifier.height(4.dp))
        Text(text = value, style = MaterialTheme.typography.titleLarge, color = accent, maxLines = 1)
    }
}
