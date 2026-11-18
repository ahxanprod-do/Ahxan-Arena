package com.example.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.model.BroadcastNotification
import com.example.data.model.CmsBanner
import com.example.data.model.GeneralConfig
import com.example.data.model.Tournament
import com.example.data.model.UserNotification
import com.example.data.model.UserProfile
import com.example.notifications.ArenaNotificationHelper
import com.example.ui.components.CyberGlassCard
import com.example.ui.components.NeonPrimaryButton
import com.example.ui.components.SectionHeader
import com.example.ui.components.SlotsProgressBar
import com.example.ui.components.StatusPill
import com.example.ui.components.bannerDrawableForKey
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.EmeraldProfit
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.ObsidianElevated
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.utils.ArenaFormatters

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeLobbyScreen(
    user: UserProfile?,
    banners: List<CmsBanner>,
    generalConfig: GeneralConfig,
    tournaments: List<Tournament>,
    notifications: List<UserNotification>,
    broadcasts: List<BroadcastNotification>,
    onClaimCheckIn: () -> Unit,
    onNavigate: (String) -> Unit,
    onSelectTournament: (Tournament) -> Unit,
    onMarkNotificationRead: (String) -> Unit
) {
    val context = LocalContext.current
    var selectedGame by remember { mutableStateOf("All") }
    var showNotificationDrawer by remember { mutableStateOf(false) }
    var hasPushPerm by remember { mutableStateOf(ArenaNotificationHelper.hasNotificationPermission(context)) }

    val notifPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPushPerm = granted
    }

    val games = listOf("All", "BGMI", "Free Fire MAX", "COD Mobile", "Valorant")
    val filteredTournaments = remember(tournaments, selectedGame) {
        tournaments.filter {
            (it.status == "upcoming" || it.status == "live") &&
                (selectedGame == "All" || it.game.equals(selectedGame, ignoreCase = true))
        }.take(8)
    }

    val unreadCount = notifications.count { !it.read }
    val today = remember { ArenaFormatters.todayKey() }
    val alreadyCheckedIn = user?.lastCheckInDay == today

    val displayBanners = remember(banners) {
        banners.ifEmpty {
            listOf(
                CmsBanner(id = "d1", title = "AHXAN CYBER SERIES", subtitle = "Daily Scrims • Instant PayZapi Payouts", drawableKey = "bgmi", redirectRoute = "tournaments"),
                CmsBanner(id = "d2", title = "PAYZAPI +20% BONUS", subtitle = "Use WELCOME20 on first deposit", drawableKey = "freefire", redirectRoute = "wallet"),
                CmsBanner(id = "d3", title = "REFER & EARN", subtitle = "Invite squadmates for instant bonus", drawableKey = "codm", redirectRoute = "refer")
            )
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Brand + Wallet Pill + Bell Drawer
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("AHXAN ARENA", style = MaterialTheme.typography.headlineMedium, color = NeonCyan)
                    Text(
                        text = "Welcome, ${user?.displayName?.ifBlank { "Warrior" } ?: "Warrior"}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        color = ObsidianElevated,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .border(1.dp, EmeraldProfit.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                            .clickable { onNavigate("wallet") }
                            .testTag("home_wallet_pill")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Icon(Icons.Default.AccountBalanceWallet, contentDescription = "Wallet", tint = EmeraldProfit, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = ArenaFormatters.formatInr(user?.totalBalance ?: 0.0),
                                style = MaterialTheme.typography.labelLarge,
                                color = EmeraldProfit
                            )
                        }
                    }
                    IconButton(
                        onClick = { showNotificationDrawer = true },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(ObsidianElevated)
                            .testTag("notification_bell_button")
                    ) {
                        BadgedBox(
                            badge = {
                                if (unreadCount > 0) {
                                    Badge(containerColor = NeonCyan, contentColor = ObsidianBg) {
                                        Text("$unreadCount")
                                    }
                                }
                            }
                        ) {
                            Icon(Icons.Default.Notifications, contentDescription = "Notifications", tint = TextPrimary)
                        }
                    }
                }
            }
        }

        // Scrolling Announcement Ticker
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(ElectricViolet.copy(alpha = 0.22f))
                    .border(1.dp, ElectricViolet.copy(alpha = 0.55f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Campaign, contentDescription = "Announcement", tint = AmberWarning, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    text = generalConfig.tickerText,
                    style = MaterialTheme.typography.labelLarge,
                    color = TextPrimary,
                    maxLines = 1,
                    modifier = Modifier.basicMarquee()
                )
            }
        }

        // Enable Mobile Shutter Push Banner if not granted
        if (!hasPushPerm && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            item {
                CyberGlassCard(glowColor = AmberWarning) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = AmberWarning, modifier = Modifier.size(28.dp))
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Enable Mobile Shutter Alerts", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                            Text("Get instant lock-screen alerts for Room IDs, prizes & offers.", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                        }
                        Spacer(Modifier.width(8.dp))
                        NeonPrimaryButton(
                            text = "Enable",
                            onClick = { notifPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) },
                            accentColor = AmberWarning,
                            modifier = Modifier.testTag("enable_push_banner_btn")
                        )
                    }
                }
            }
        }

        // Promo Banners Carousel
        item {
            val pagerState = rememberPagerState(pageCount = { displayBanners.size })
            HorizontalPager(
                state = pagerState,
                pageSpacing = 12.dp,
                modifier = Modifier.fillMaxWidth()
            ) { page ->
                val banner = displayBanners[page]
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(175.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .border(1.dp, NeonCyan.copy(alpha = 0.45f), RoundedCornerShape(20.dp))
                        .clickable { onNavigate(banner.redirectRoute.ifBlank { "tournaments" }) }
                ) {
                    if (banner.imageUrl.isNotBlank()) {
                        AsyncImage(
                            model = banner.imageUrl,
                            contentDescription = banner.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Image(
                            painter = painterResource(id = bannerDrawableForKey(banner.drawableKey)),
                            contentDescription = banner.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(listOf(Color.Transparent, ObsidianBg.copy(alpha = 0.92f)))
                            )
                    )
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(16.dp)
                    ) {
                        StatusPill(label = "FEATURED", color = NeonCyan)
                        Spacer(Modifier.height(6.dp))
                        Text(banner.title, style = MaterialTheme.typography.titleLarge, color = TextPrimary)
                        if (banner.subtitle.isNotBlank()) {
                            Text(banner.subtitle, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                        }
                    }
                }
            }
        }

        // Daily Check-In Streak Banner
        if (generalConfig.checkinEnabled) {
            item {
                val streak = user?.streakDays ?: 0L
                val nextDay = ((streak % 7L) + 1L).toInt()
                val rewards = generalConfig.checkinRewards.ifEmpty { listOf(5L, 8L, 10L, 12L, 15L, 20L, 50L) }
                val todayReward = rewards[(nextDay - 1).coerceIn(0, rewards.lastIndex)]

                CyberGlassCard(glowColor = EmeraldProfit) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(EmeraldProfit.copy(alpha = 0.16f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.LocalFireDepartment, contentDescription = null, tint = EmeraldProfit)
                            }
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text("Daily Check-In Streak ($streak/7)", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                                Text(
                                    text = if (alreadyCheckedIn) "Today's ₹$todayReward bonus claimed!" else "Claim Day $nextDay reward: +₹$todayReward Bonus",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (alreadyCheckedIn) TextMuted else EmeraldProfit
                                )
                            }
                        }
                        Spacer(Modifier.width(8.dp))
                        NeonPrimaryButton(
                            text = if (alreadyCheckedIn) "Claimed" else "Claim ₹$todayReward",
                            enabled = !alreadyCheckedIn,
                            onClick = onClaimCheckIn,
                            accentColor = EmeraldProfit,
                            modifier = Modifier.testTag("claim_checkin_button")
                        )
                    }
                }
            }
        }

        // Quick Actions (Refer & Earn / Support)
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CyberGlassCard(
                    glowColor = ElectricViolet,
                    onClick = { onNavigate("refer") },
                    modifier = Modifier.weight(1f).testTag("home_refer_card")
                ) {
                    Icon(Icons.Default.CardGiftcard, contentDescription = null, tint = ElectricViolet)
                    Spacer(Modifier.height(6.dp))
                    Text("Refer & Earn", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                    Text("Invite friends & earn bonus", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                }
                CyberGlassCard(
                    glowColor = NeonCyan,
                    onClick = { onNavigate("tournaments") },
                    modifier = Modifier.weight(1f).testTag("home_browse_matches_card")
                ) {
                    Icon(Icons.Default.SportsEsports, contentDescription = null, tint = NeonCyan)
                    Spacer(Modifier.height(6.dp))
                    Text("All Scrims", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                    Text("${tournaments.count { it.status == "upcoming" || it.status == "live" }} active matches", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                }
            }
        }

        // Game Quick Filters
        item {
            SectionHeader(title = "Arena Lobby", subtitle = "Filter live & upcoming tournaments", actionLabel = "See All") {
                onNavigate("tournaments")
            }
            Spacer(Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(games) { game ->
                    val selected = selectedGame == game
                    FilterChip(
                        selected = selected,
                        onClick = { selectedGame = game },
                        label = { Text(game) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = NeonCyan,
                            selectedLabelColor = ObsidianBg,
                            containerColor = ObsidianElevated,
                            labelColor = TextSecondary
                        )
                    )
                }
            }
        }

        if (filteredTournaments.isEmpty()) {
            item {
                CyberGlassCard {
                    Text("No active matches in $selectedGame yet", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                    Spacer(Modifier.height(4.dp))
                    Text("Check back soon or switch game filters. Admins can launch new scrims anytime.", style = MaterialTheme.typography.bodyMedium, color = TextMuted)
                }
            }
        } else {
            items(filteredTournaments, key = { it.id }) { match ->
                TournamentLobbyCard(match = match, onClick = { onSelectTournament(match) })
            }
        }
    }

    if (showNotificationDrawer) {
        ModalBottomSheet(
            onDismissRequest = { showNotificationDrawer = false },
            containerColor = ObsidianSurface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text("Arena Alerts & Broadcasts", style = MaterialTheme.typography.headlineSmall, color = NeonCyan)
                Spacer(Modifier.height(10.dp))
                val mergedCount = notifications.size + broadcasts.size
                if (mergedCount == 0) {
                    Text("No alerts yet. Room IDs, prize wins, and broadcasts appear here.", color = TextMuted, modifier = Modifier.padding(vertical = 20.dp))
                } else {
                    LazyColumn(
                        modifier = Modifier.height(360.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(notifications, key = { "u_${it.id}" }) { n ->
                            CyberGlassCard(
                                glowColor = if (n.read) TextMuted else NeonCyan,
                                onClick = {
                                    onMarkNotificationRead(n.id)
                                    showNotificationDrawer = false
                                    onNavigate(n.route)
                                }
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                    Text(n.title, style = MaterialTheme.typography.titleMedium, color = TextPrimary, modifier = Modifier.weight(1f))
                                    if (!n.read) StatusPill("NEW", NeonCyan)
                                }
                                Spacer(Modifier.height(4.dp))
                                Text(n.body, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                            }
                        }
                        items(broadcasts, key = { "b_${it.id}" }) { b ->
                            CyberGlassCard(
                                glowColor = ElectricViolet,
                                onClick = {
                                    showNotificationDrawer = false
                                    onNavigate(b.redirectRoute)
                                }
                            ) {
                                StatusPill("BROADCAST", ElectricViolet)
                                Spacer(Modifier.height(6.dp))
                                Text(b.title, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                                Spacer(Modifier.height(4.dp))
                                Text(b.body, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(20.dp))
            }
        }
    }
}

@Composable
fun TournamentLobbyCard(
    match: Tournament,
    onClick: () -> Unit
) {
    val statusColor = when (match.status) {
        "live" -> EmeraldProfit
        "completed" -> ElectricViolet
        "cancelled" -> Color(0xFFFF3B5C)
        else -> NeonCyan
    }
    CyberGlassCard(
        glowColor = statusColor,
        onClick = onClick,
        modifier = Modifier.testTag("tournament_card_${match.id}")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                StatusPill(label = match.game, color = NeonCyan)
                StatusPill(label = match.mode, color = ElectricViolet)
            }
            StatusPill(label = match.status, color = statusColor)
        }
        Spacer(Modifier.height(10.dp))
        Text(
            text = match.title,
            style = MaterialTheme.typography.titleLarge,
            color = TextPrimary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = "${match.mapName} • ${ArenaFormatters.formatTimestampMillis(match.scheduledAtMillis)}",
            style = MaterialTheme.typography.labelSmall,
            color = TextMuted
        )
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("PRIZE POOL", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                Text(ArenaFormatters.formatInr(match.prizePool), style = MaterialTheme.typography.titleMedium, color = EmeraldProfit)
            }
            Column {
                Text("PER KILL", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                Text(ArenaFormatters.formatInr(match.perKillReward), style = MaterialTheme.typography.titleMedium, color = AmberWarning)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("ENTRY", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                Text(
                    text = if (match.entryFee <= 0.0) "FREE" else ArenaFormatters.formatInr(match.entryFee),
                    style = MaterialTheme.typography.titleMedium,
                    color = NeonCyan
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        SlotsProgressBar(filled = match.filledSlots, max = match.maxSlots)
    }
}
