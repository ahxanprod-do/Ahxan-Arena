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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.BroadcastNotification
import com.example.data.model.GameCategory
import com.example.data.model.Tournament
import com.example.data.model.UserNotification
import com.example.ui.theme.ArenaCharcoal
import com.example.ui.theme.ArenaGreen
import com.example.ui.theme.ArenaOrange
import com.example.ui.theme.ArenaPurple
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.CardDarkText
import com.example.ui.theme.CoinGold
import com.example.ui.theme.CoinYellow
import com.example.ui.theme.CrimsonDanger
import com.example.ui.theme.HeaderNavy
import com.example.ui.theme.ObsidianElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.utils.QrCodeUtils

data class GameCategoryItem(
    val filterKey: String,
    val title: String,
    val drawableKey: String,
    val imageUrl: String = ""
)

val DEFAULT_GAME_CATEGORIES = listOf(
    GameCategoryItem("BGMI", "BGMI", "bgmi"),
    GameCategoryItem("Free Fire MAX", "FF MAX", "freefire"),
    GameCategoryItem("Clash Squad", "CS I", "freefire"),
    GameCategoryItem("BGMI TDM", "BGMI TDM", "bgmi"),
    GameCategoryItem("FF Survival", "FF SURVIVAL", "freefire"),
    GameCategoryItem("Lone Wolf", "LONE WOLF", "hero"),
    GameCategoryItem("Sniper TDM", "SNIPER TDM", "bgmi"),
    GameCategoryItem("WOW Mode", "WOW MODE", "bgmi"),
    GameCategoryItem("CS 4v4", "CS 4v4", "freefire"),
    GameCategoryItem("COD Mobile", "CODM", "codm"),
    GameCategoryItem("Valorant", "VALORANT", "hero"),
    GameCategoryItem("All", "ALL MATCHES", "hero")
)

fun resolveGameCategories(dbCategories: List<GameCategory>): List<GameCategoryItem> {
    if (dbCategories.isEmpty()) return DEFAULT_GAME_CATEGORIES
    return dbCategories.map { c ->
        GameCategoryItem(
            filterKey = c.filterKey.ifBlank { c.title },
            title = c.title.ifBlank { c.filterKey },
            drawableKey = c.drawableKey.ifBlank { "bgmi" },
            imageUrl = c.imageUrl
        )
    }
}

fun matchesCategoryFilter(match: Tournament, categoryKey: String): Boolean {
    if (categoryKey.isBlank() || categoryKey.equals("All", ignoreCase = true)) return true
    if (match.game.equals(categoryKey, ignoreCase = true)) return true
    if (categoryKey.equals("Free Fire MAX", ignoreCase = true) && match.game.contains("Free Fire", ignoreCase = true)) return true
    if (categoryKey.equals("COD Mobile", ignoreCase = true) && match.game.contains("COD", ignoreCase = true)) return true
    return match.game.contains(categoryKey, ignoreCase = true) ||
        match.mode.contains(categoryKey, ignoreCase = true) ||
        match.mapName.contains(categoryKey, ignoreCase = true) ||
        match.title.contains(categoryKey, ignoreCase = true)
}

@Composable
fun GameCategoryGrid(
    categories: List<GameCategoryItem> = DEFAULT_GAME_CATEGORIES,
    tournaments: List<Tournament> = emptyList(),
    onCategoryClick: (GameCategoryItem) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        categories.chunked(3).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                rowItems.forEach { cat ->
                    val openCount = remember(tournaments, cat.filterKey) {
                        tournaments.count {
                            (it.status == "upcoming" || it.status == "live") &&
                                matchesCategoryFilter(it, cat.filterKey)
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        shadowElevation = 3.dp,
                        color = HeaderNavy,
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(0.84f)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onCategoryClick(cat) }
                            .testTag("game_tile_${cat.filterKey}")
                    ) {
                        Column(modifier = Modifier.fillMaxSize()) {
                            Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                                MatchBannerImage(
                                    bannerUrl = cat.imageUrl,
                                    bannerKey = cat.drawableKey,
                                    contentDescription = cat.title,
                                    modifier = Modifier.fillMaxSize()
                                )
                                if (openCount > 0) {
                                    Surface(
                                        color = HeaderNavy.copy(alpha = 0.86f),
                                        shape = RoundedCornerShape(50),
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(5.dp)
                                    ) {
                                        Text(
                                            text = "$openCount",
                                            color = CoinYellow,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(ArenaOrange)
                                    .padding(vertical = 6.dp, horizontal = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = cat.title,
                                    color = Color(0xFF111827),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
                repeat(3 - rowItems.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

fun bannerDrawableForKey(key: String): Int = when (key.lowercase()) {
    "freefire" -> R.drawable.img_banner_freefire
    "codm" -> R.drawable.img_banner_codm
    "hero" -> R.drawable.img_arena_hero
    else -> R.drawable.img_banner_bgmi
}

@Composable
fun CoinBadge(
    modifier: Modifier = Modifier,
    size: Dp = 18.dp
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(
                Brush.radialGradient(listOf(CoinYellow, CoinGold))
            )
            .border(1.2.dp, Color(0xFFD97706), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "C",
            color = Color(0xFF78350F),
            fontWeight = FontWeight.ExtraBold,
            fontSize = (size.value * 0.56f).sp,
            lineHeight = (size.value * 0.6f).sp
        )
    }
}

@Composable
fun ArenaTopHeader(
    coinBalance: Double,
    unreadCount: Int,
    onOpenNotifications: () -> Unit,
    onOpenWallet: () -> Unit
) {
    Surface(
        color = HeaderNavy,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF0E131E))
                    .border(1.5.dp, CoinGold, CircleShape)
                    .padding(4.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_arena_emblem),
                    contentDescription = "Ahxan Arena",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
            ) {
                Text(
                    text = "Welcome Back,",
                    color = Color(0xFFE2E8F0),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Ahxan Arena",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(
                    onClick = onOpenNotifications,
                    modifier = Modifier
                        .size(40.dp)
                        .testTag("notification_bell_button")
                ) {
                    BadgedBox(
                        badge = {
                            Box(
                                modifier = Modifier
                                    .background(CoinYellow, RoundedCornerShape(4.dp))
                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = "$unreadCount",
                                    color = CardDarkText,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "Notifications",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                Surface(
                    color = Color.White,
                    shape = RoundedCornerShape(50),
                    modifier = Modifier
                        .clickable { onOpenWallet() }
                        .testTag("home_wallet_pill")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        CoinBadge(size = 20.dp)
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = coinBalance.toLong().toString(),
                            color = CardDarkText,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SubScreenTopBar(
    title: String,
    containerColor: Color = HeaderNavy,
    onBack: (() -> Unit)? = null,
    trailingContent: @Composable (() -> Unit)? = null
) {
    Surface(
        color = containerColor,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 8.dp)
        ) {
            if (onBack != null) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.align(Alignment.CenterStart).size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
            }
            Text(
                text = title,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.align(Alignment.Center)
            )
            if (trailingContent != null) {
                Box(modifier = Modifier.align(Alignment.CenterEnd)) {
                    trailingContent()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationDrawerSheet(
    notifications: List<UserNotification>,
    broadcasts: List<BroadcastNotification>,
    onMarkNotificationRead: (String) -> Unit,
    onNavigate: (String) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = HeaderNavy
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text("Arena Alerts & Broadcasts", style = MaterialTheme.typography.headlineSmall, color = CoinYellow)
            Spacer(Modifier.height(10.dp))
            val mergedCount = notifications.size + broadcasts.size
            if (mergedCount == 0) {
                Text(
                    "No alerts yet. Room IDs, prize wins, and broadcasts appear here.",
                    color = TextSecondary,
                    modifier = Modifier.padding(vertical = 20.dp)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.height(360.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(notifications, key = { "u_${it.id}" }) { n ->
                        Surface(
                            color = Color.White,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onMarkNotificationRead(n.id)
                                    onDismiss()
                                    onNavigate(n.route)
                                }
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(n.title, fontWeight = FontWeight.Bold, color = CardDarkText, modifier = Modifier.weight(1f))
                                    if (!n.read) StatusPill("NEW", ArenaOrange)
                                }
                                Spacer(Modifier.height(4.dp))
                                Text(n.body, style = MaterialTheme.typography.bodyMedium, color = Color(0xFF475569))
                            }
                        }
                    }
                    items(broadcasts, key = { "b_${it.id}" }) { b ->
                        Surface(
                            color = Color.White,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onDismiss()
                                    onNavigate(b.redirectRoute)
                                }
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                StatusPill("BROADCAST", ArenaPurple)
                                Spacer(Modifier.height(6.dp))
                                Text(b.title, fontWeight = FontWeight.Bold, color = CardDarkText)
                                Spacer(Modifier.height(4.dp))
                                Text(b.body, style = MaterialTheme.typography.bodyMedium, color = Color(0xFF475569))
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
fun MatchBannerImage(
    bannerUrl: String,
    bannerKey: String,
    contentDescription: String,
    modifier: Modifier = Modifier
) {
    val trimmed = bannerUrl.trim()
    val decodedBitmap = remember(trimmed) {
        if (trimmed.isNotEmpty() && !trimmed.startsWith("http", ignoreCase = true)) {
            QrCodeUtils.decodeBase64Image(trimmed)
        } else null
    }
    when {
        trimmed.startsWith("http", ignoreCase = true) -> {
            AsyncImage(
                model = trimmed,
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                modifier = modifier
            )
        }
        decodedBitmap != null -> {
            Image(
                bitmap = decodedBitmap.asImageBitmap(),
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                modifier = modifier
            )
        }
        else -> {
            Image(
                painter = painterResource(id = bannerDrawableForKey(bannerKey)),
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                modifier = modifier
            )
        }
    }
}

@Composable
fun ArenaBrandEmblem(
    modifier: Modifier = Modifier,
    size: Dp = 76.dp
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(HeaderNavy)
            .border(
                width = 2.4.dp,
                brush = Brush.linearGradient(listOf(CoinYellow, ArenaOrange, ArenaPurple)),
                shape = CircleShape
            )
            .padding(size * 0.12f),
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
    glowColor: Color = ArenaOrange,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(14.dp)
    val baseModifier = modifier
        .fillMaxWidth()
        .clip(shape)
        .background(HeaderNavy)
        .border(
            width = 1.dp,
            color = glowColor.copy(alpha = 0.42f),
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
    accentColor: Color = ArenaOrange
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(50.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = accentColor,
            contentColor = if (accentColor == CoinYellow) Color(0xFF111827) else Color.White,
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
            val accent = if (ok) ArenaGreen else CrimsonDanger
            Card(
                colors = CardDefaults.cardColors(containerColor = HeaderNavy),
                border = BorderStroke(1.dp, accent.copy(alpha = 0.7f)),
                shape = RoundedCornerShape(12.dp),
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
                color = ArenaOrange,
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
        progress >= 0.6f -> ArenaOrange
        else -> ArenaPurple
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
    accent: Color = ArenaOrange,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(HeaderNavy)
            .border(1.dp, accent.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = TextMuted, maxLines = 1)
        Spacer(Modifier.height(4.dp))
        Text(text = value, style = MaterialTheme.typography.titleLarge, color = accent, maxLines = 1)
    }
}
