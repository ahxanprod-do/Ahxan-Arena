package com.example.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.BroadcastNotification
import com.example.data.model.CmsBanner
import com.example.data.model.GeneralConfig
import com.example.data.model.Tournament
import com.example.data.model.UserNotification
import com.example.data.model.UserProfile
import com.example.notifications.ArenaNotificationHelper
import com.example.ui.components.ArenaTopHeader
import com.example.ui.components.CoinBadge
import com.example.ui.components.DEFAULT_GAME_CATEGORIES
import com.example.ui.components.GameCategoryGrid
import com.example.ui.components.MatchBannerImage
import com.example.ui.components.NotificationDrawerSheet
import com.example.ui.components.StatusPill
import com.example.ui.components.bannerDrawableForKey
import com.example.ui.theme.ArenaBlue
import com.example.ui.theme.ArenaCharcoal
import com.example.ui.theme.ArenaGreen
import com.example.ui.theme.ArenaOrange
import com.example.ui.theme.ArenaPurple
import com.example.ui.theme.CardDarkText
import com.example.ui.theme.CardMutedText
import com.example.ui.theme.CrimsonDanger
import com.example.ui.theme.HeaderNavy
import com.example.ui.theme.TextSecondary
import com.example.utils.ArenaFormatters

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
    onOpenGameTournaments: (String) -> Unit = { onNavigate("tournaments") },
    onOpenContestTab: (Int) -> Unit = { onNavigate("tournaments") },
    onSelectTournament: (Tournament) -> Unit = {},
    onMarkNotificationRead: (String) -> Unit
) {
    val context = LocalContext.current
    var showNotificationDrawer by remember { mutableStateOf(false) }
    var hasPushPerm by remember { mutableStateOf(ArenaNotificationHelper.hasNotificationPermission(context)) }

    val notifPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPushPerm = granted
    }

    val unreadCount = notifications.count { !it.read }
    val today = remember { ArenaFormatters.todayKey() }
    val alreadyCheckedIn = user?.lastCheckInDay == today

    val displayBanners = remember(banners) {
        banners.ifEmpty {
            listOf(
                CmsBanner(id = "d1", title = "AHXAN ARENA CHAMPIONSHIPS", subtitle = "Daily Scrims • Instant PayZapi Payouts", drawableKey = "bgmi", redirectRoute = "tournaments"),
                CmsBanner(id = "d2", title = "PAYZAPI +20% BONUS", subtitle = "Use WELCOME20 on your deposit", drawableKey = "freefire", redirectRoute = "wallet"),
                CmsBanner(id = "d3", title = "REFER & EARN", subtitle = "Invite squadmates for instant coins", drawableKey = "codm", redirectRoute = "refer")
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ArenaCharcoal)
    ) {
        ArenaTopHeader(
            coinBalance = user?.totalBalance ?: 0.0,
            unreadCount = unreadCount,
            onOpenNotifications = { showNotificationDrawer = true },
            onOpenWallet = { onNavigate("wallet") }
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Scrolling Notice Ticker Card (White card with Orange Speaker block)
            item {
                Surface(
                    color = Color.White,
                    shape = RoundedCornerShape(10.dp),
                    shadowElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .width(52.dp)
                                .fillMaxSize()
                                .background(ArenaOrange),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Campaign,
                                contentDescription = "Notice",
                                tint = Color.White,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                        Text(
                            text = generalConfig.tickerText.ifBlank { "WELCOME TO AHXAN ARENA" },
                            color = CardDarkText,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 12.dp)
                                .basicMarquee()
                        )
                    }
                }
            }

            // Enable Push Alert Prompt if needed on Android 13+
            if (!hasPushPerm && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                item {
                    Surface(
                        color = HeaderNavy,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, ArenaOrange.copy(alpha = 0.6f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = ArenaOrange, modifier = Modifier.size(24.dp))
                            Spacer(Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Enable Match & Room Alerts", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text("Get instant lock-screen push alerts.", color = TextSecondary, fontSize = 11.sp)
                            }
                            Button(
                                onClick = { notifPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) },
                                colors = ButtonDefaults.buttonColors(containerColor = ArenaOrange, contentColor = Color.White),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.testTag("enable_push_banner_btn")
                            ) {
                                Text("Enable", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            // Account Details (4 Orange Circular Action Icons)
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Account Details",
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        AccountCircleAction(
                            label = "My Profile",
                            icon = Icons.Default.Person,
                            onClick = { onNavigate("edit_profile") },
                            modifier = Modifier.weight(1f).testTag("home_profile_action")
                        )
                        AccountCircleAction(
                            label = "My Wallet",
                            icon = Icons.Default.AccountBalanceWallet,
                            onClick = { onNavigate("wallet") },
                            modifier = Modifier.weight(1f).testTag("home_wallet_action")
                        )
                        AccountCircleAction(
                            label = "Top Players",
                            icon = Icons.Default.EmojiEvents,
                            onClick = { onNavigate("my_matches") },
                            modifier = Modifier.weight(1f).testTag("home_top_players_action")
                        )
                        AccountCircleAction(
                            label = "Contact Us",
                            icon = Icons.Default.SupportAgent,
                            onClick = { onNavigate("support") },
                            modifier = Modifier.weight(1f).testTag("home_contact_action")
                        )
                    }
                }
            }

            // Promo Banners Carousel + Dots
            item {
                val pagerState = rememberPagerState(pageCount = { displayBanners.size })
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    HorizontalPager(
                        state = pagerState,
                        pageSpacing = 12.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) { page ->
                        val banner = displayBanners[page]
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(168.dp)
                                .clip(RoundedCornerShape(14.dp))
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
                                        Brush.verticalGradient(listOf(Color.Transparent, HeaderNavy.copy(alpha = 0.88f)))
                                    )
                            )
                            Column(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(14.dp)
                            ) {
                                Text(banner.title, color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                                if (banner.subtitle.isNotBlank()) {
                                    Text(banner.subtitle, color = Color(0xFFE2E8F0), fontSize = 12.sp)
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        repeat(displayBanners.size) { index ->
                            val selected = pagerState.currentPage == index
                            Box(
                                modifier = Modifier
                                    .size(if (selected) 8.dp else 7.dp)
                                    .clip(CircleShape)
                                    .background(if (selected) ArenaPurple else Color(0xFF4B5563))
                            )
                        }
                    }
                }
            }

            // My Contest (3 White Trophy Cards: Upcoming, Ongoing, Completed)
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "My Contest",
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        ContestSquareCard(
                            label = "Upcoming",
                            onClick = { onOpenContestTab(1) },
                            modifier = Modifier.weight(1f).testTag("home_contest_upcoming")
                        )
                        ContestSquareCard(
                            label = "Ongoing",
                            onClick = { onOpenContestTab(0) },
                            modifier = Modifier.weight(1f).testTag("home_contest_ongoing")
                        )
                        ContestSquareCard(
                            label = "Completed",
                            onClick = { onOpenContestTab(2) },
                            modifier = Modifier.weight(1f).testTag("home_contest_completed")
                        )
                    }
                }
            }

            // Daily Check-In Streak (Admin Controlled)
            if (generalConfig.checkinEnabled) {
                item {
                    val streak = user?.streakDays ?: 0L
                    val nextDay = ((streak % 7L) + 1L).toInt()
                    val rewards = generalConfig.checkinRewards.ifEmpty { listOf(5L, 8L, 10L, 12L, 15L, 20L, 50L) }
                    val todayReward = rewards[(nextDay - 1).coerceIn(0, rewards.lastIndex)]

                    Surface(
                        color = HeaderNavy,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, ArenaGreen.copy(alpha = 0.45f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(ArenaGreen.copy(alpha = 0.16f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.LocalFireDepartment, contentDescription = null, tint = ArenaGreen)
                                }
                                Spacer(Modifier.width(10.dp))
                                Column {
                                    Text("Daily Check-In ($streak/7)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text(
                                        text = if (alreadyCheckedIn) "Today's +$todayReward Bonus claimed!" else "Claim Day $nextDay reward: +$todayReward Coins",
                                        color = if (alreadyCheckedIn) TextSecondary else ArenaGreen,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                            Button(
                                onClick = onClaimCheckIn,
                                enabled = !alreadyCheckedIn,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = ArenaGreen,
                                    contentColor = Color.White,
                                    disabledContainerColor = Color(0xFF374151),
                                    disabledContentColor = Color(0xFF9CA3AF)
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.testTag("claim_checkin_button")
                            ) {
                                Text(if (alreadyCheckedIn) "Claimed" else "Claim +$todayReward", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            // 3-Column eSport Games Categories Grid (matches Screenshot 162310 — click any category to view its matches)
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "eSport Games",
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    GameCategoryGrid(
                        categories = DEFAULT_GAME_CATEGORIES,
                        tournaments = tournaments,
                        onCategoryClick = { cat -> onOpenGameTournaments(cat.filterKey) }
                    )
                }
            }

            item { Spacer(Modifier.height(12.dp)) }
        }
    }

    if (showNotificationDrawer) {
        NotificationDrawerSheet(
            notifications = notifications,
            broadcasts = broadcasts,
            onMarkNotificationRead = onMarkNotificationRead,
            onNavigate = onNavigate,
            onDismiss = { showNotificationDrawer = false }
        )
    }
}

@Composable
private fun AccountCircleAction(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .padding(vertical = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(ArenaOrange),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = Color.White,
                modifier = Modifier.size(28.dp)
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = label,
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}

@Composable
private fun ContestSquareCard(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Color.White,
        shape = RoundedCornerShape(14.dp),
        shadowElevation = 3.dp,
        modifier = modifier
            .height(98.dp)
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.EmojiEvents,
                contentDescription = label,
                tint = ArenaPurple,
                modifier = Modifier.size(32.dp)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = label,
                color = CardDarkText,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
        }
    }
}

@Composable
fun TournamentLobbyCard(
    match: Tournament,
    isJoined: Boolean = false,
    onClick: () -> Unit
) {
    val isFull = match.filledSlots >= match.maxSlots
    val spotsLeft = (match.maxSlots - match.filledSlots).coerceAtLeast(0L)
    val progress = if (match.maxSlots > 0) (match.filledSlots.toFloat() / match.maxSlots.toFloat()).coerceIn(0f, 1f) else 0f

    Surface(
        color = Color.White,
        shape = RoundedCornerShape(12.dp),
        shadowElevation = 4.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("tournament_card_${match.id}")
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Top Full-Bleed Match Banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(172.dp)
            ) {
                MatchBannerImage(
                    bannerUrl = match.bannerUrl,
                    bannerKey = match.bannerKey,
                    contentDescription = match.title,
                    modifier = Modifier.fillMaxSize()
                )
                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (isJoined) {
                        StatusPill("JOINED", ArenaGreen)
                    } else if (match.status == "live") {
                        StatusPill("LIVE", ArenaGreen)
                    }
                }
            }

            Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
                // Logo + Match Title & Time
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(HeaderNavy)
                            .padding(5.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_arena_emblem),
                            contentDescription = "Ahxan Arena",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = match.title,
                            color = CardDarkText,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = "Time: ${ArenaFormatters.formatTimestampMillis(match.scheduledAtMillis)}",
                            color = CardMutedText,
                            fontSize = 12.sp
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))

                // Row 1: PRIZE POOL / PER KILL / ENTRY FEE
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                        Text("PRIZE POOL", color = CardMutedText, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                        Spacer(Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CoinBadge(size = 16.dp)
                            Spacer(Modifier.width(4.dp))
                            Text("${match.prizePool.toLong()}", color = CardDarkText, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                        }
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                        Text("PER KILL", color = CardMutedText, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                        Spacer(Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CoinBadge(size = 16.dp)
                            Spacer(Modifier.width(4.dp))
                            Text("${match.perKillReward.toLong()}", color = CardDarkText, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                        }
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                        Text("ENTRY FEE", color = CardMutedText, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                        Spacer(Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CoinBadge(size = 16.dp)
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = if (match.entryFee <= 0.0) "FREE" else "${match.entryFee.toLong()}",
                                color = CardDarkText,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 15.sp
                            )
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Row 2: TYPE / VERSION / MAP
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                        Text("TYPE", color = CardMutedText, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                        Spacer(Modifier.height(3.dp))
                        Text(match.mode, color = CardDarkText, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                        Text("VERSION", color = CardMutedText, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                        Spacer(Modifier.height(3.dp))
                        Text("TPP", color = CardDarkText, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                        Text("MAP", color = CardMutedText, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                        Spacer(Modifier.height(3.dp))
                        Text(match.mapName.ifBlank { "Classic" }, color = CardDarkText, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1)
                    }
                }

                Spacer(Modifier.height(14.dp))

                // Progress bar + Spots + Action Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(5.dp)
                                .clip(CircleShape),
                            color = ArenaPurple,
                            trackColor = Color(0xFFE5E7EB)
                        )
                        Spacer(Modifier.height(5.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = if (isFull) "No Spots Left! Match is Full." else "Only $spotsLeft spots left",
                                color = if (isFull) CrimsonDanger else CardMutedText,
                                fontSize = 11.sp
                            )
                            Text(
                                text = "${match.filledSlots}/${match.maxSlots}",
                                color = CardDarkText,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    when {
                        isJoined -> {
                            Button(
                                onClick = onClick,
                                colors = ButtonDefaults.buttonColors(containerColor = ArenaGreen, contentColor = Color.White),
                                shape = RoundedCornerShape(6.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Text("JOINED", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                        isFull || match.status != "upcoming" -> {
                            Surface(
                                color = Color(0xFF9CA3AF),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = if (isFull) "MATCH FULL" else match.status.uppercase(),
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                                )
                            }
                        }
                        else -> {
                            OutlinedButton(
                                onClick = onClick,
                                border = BorderStroke(1.2.dp, ArenaBlue),
                                shape = RoundedCornerShape(4.dp),
                                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 6.dp)
                            ) {
                                Text("JOIN", color = ArenaBlue, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
