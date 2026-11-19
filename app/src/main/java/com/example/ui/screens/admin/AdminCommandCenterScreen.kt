package com.example.ui.screens.admin

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.DepositOrder
import com.example.data.model.GameCategory
import com.example.data.model.StaffRole
import com.example.data.model.SupportTicket
import com.example.data.model.Tournament
import com.example.data.model.UserProfile
import com.example.data.model.WithdrawalRequest
import com.example.ui.components.CoinBadge
import com.example.ui.components.CyberGlassCard
import com.example.ui.components.KpiMiniStat
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusPill
import com.example.ui.components.StatusToastBanner
import com.example.ui.theme.ArenaBlue
import com.example.ui.theme.ArenaCharcoal
import com.example.ui.theme.ArenaGreen
import com.example.ui.theme.ArenaOrange
import com.example.ui.theme.ArenaPurple
import com.example.ui.theme.CoinGold
import com.example.ui.theme.CoinYellow
import com.example.ui.theme.HeaderNavy
import com.example.ui.theme.ObsidianElevated
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.utils.ArenaFormatters
import com.example.viewmodel.AdminViewModel
import com.example.viewmodel.ArenaViewModel

private data class AdminNavModule(
    val key: String,
    val title: String,
    val badge: String = "",
    val accent: Color = ArenaOrange,
    val icon: ImageVector,
    val allowed: Boolean
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminCommandCenterScreen(
    isSuperAdmin: Boolean,
    staffRole: StaffRole?,
    arenaVm: ArenaViewModel,
    adminVm: AdminViewModel,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    val users by adminVm.allUsers.collectAsStateWithLifecycle()
    val deposits by adminVm.allDeposits.collectAsStateWithLifecycle()
    val withdrawals by adminVm.allWithdrawals.collectAsStateWithLifecycle()
    val tickets by adminVm.allTickets.collectAsStateWithLifecycle()
    val coupons by adminVm.allCoupons.collectAsStateWithLifecycle()
    val referrals by adminVm.allReferrals.collectAsStateWithLifecycle()
    val allBanners by adminVm.allBanners.collectAsStateWithLifecycle()
    val allCategories by adminVm.allCategories.collectAsStateWithLifecycle()
    val staffRoles by adminVm.staffRoles.collectAsStateWithLifecycle()
    val auditLogs by adminVm.auditLogs.collectAsStateWithLifecycle()
    val pushDevices by adminVm.pushDevices.collectAsStateWithLifecycle()
    val broadcasts by adminVm.broadcasts.collectAsStateWithLifecycle()
    val roster by adminVm.selectedRoster.collectAsStateWithLifecycle()
    val ticketMsgs by adminVm.ticketMessages.collectAsStateWithLifecycle()
    val adminBanner by adminVm.adminBanner.collectAsStateWithLifecycle()
    val busy by adminVm.busy.collectAsStateWithLifecycle()

    val tournaments by arenaVm.tournaments.collectAsStateWithLifecycle()
    val generalConfig by arenaVm.generalConfig.collectAsStateWithLifecycle()
    val gatewayConfig by arenaVm.gatewayConfig.collectAsStateWithLifecycle()
    val referralConfig by arenaVm.referralConfig.collectAsStateWithLifecycle()

    val pendingDeposits = remember(deposits) { deposits.count { it.status == "pending" } }
    val pendingWithdrawals = remember(withdrawals) { withdrawals.count { it.status == "pending" } }
    val openTickets = remember(tickets) { tickets.count { it.status == "open" } }
    val canManageUsers = isSuperAdmin || (staffRole?.manageUsers == true)

    val modules = remember(isSuperAdmin, staffRole, users.size, tournaments.size, allCategories.size, pendingDeposits, pendingWithdrawals, openTickets) {
        listOf(
            AdminNavModule("dashboard", "Dashboard", "", ArenaOrange, Icons.Default.Dashboard, true),
            AdminNavModule("users", "Users & Coins (${users.size})", "${users.size}", ArenaGreen, Icons.Default.Group, canManageUsers),
            AdminNavModule("matches", "Matches (${tournaments.size})", "${tournaments.size}", ArenaPurple, Icons.Default.EmojiEvents, isSuperAdmin || (staffRole?.manageMatches == true) || (staffRole?.publishRooms == true)),
            AdminNavModule("categories", "Categories (${allCategories.size})", "", ArenaOrange, Icons.Default.Category, isSuperAdmin || (staffRole?.manageMatches == true) || (staffRole?.systemSettings == true)),
            AdminNavModule("gateway", "Deposits" + if (pendingDeposits > 0) " ($pendingDeposits)" else "", if (pendingDeposits > 0) "$pendingDeposits" else "", CoinYellow, Icons.Default.Payment, isSuperAdmin || (staffRole?.verifyDeposits == true)),
            AdminNavModule("withdrawals", "Payouts" + if (pendingWithdrawals > 0) " ($pendingWithdrawals)" else "", if (pendingWithdrawals > 0) "$pendingWithdrawals" else "", ArenaGreen, Icons.Default.AccountBalanceWallet, isSuperAdmin || (staffRole?.approveWithdrawals == true)),
            AdminNavModule("tickets", "Tickets" + if (openTickets > 0) " ($openTickets)" else "", if (openTickets > 0) "$openTickets" else "", ArenaBlue, Icons.Default.SupportAgent, isSuperAdmin || (staffRole?.replyTickets == true)),
            AdminNavModule("push", "Push Studio", "", ArenaPurple, Icons.Default.NotificationsActive, isSuperAdmin || (staffRole?.sendPush == true)),
            AdminNavModule("coupons", "Coupons", "", CoinYellow, Icons.Default.LocalOffer, isSuperAdmin || (staffRole?.manageCoupons == true)),
            AdminNavModule("referrals", "Referrals", "", ArenaGreen, Icons.Default.CardGiftcard, canManageUsers),
            AdminNavModule("cms", "CMS & Pages", "", ArenaOrange, Icons.Default.Settings, isSuperAdmin || (staffRole?.systemSettings == true)),
            AdminNavModule("staff", "Staff RBAC", "", ArenaPurple, Icons.Default.AdminPanelSettings, isSuperAdmin),
            AdminNavModule("audit", "Audit Logs", "", ArenaBlue, Icons.Default.History, isSuperAdmin || (staffRole?.systemSettings == true))
        ).filter { it.allowed }
    }

    var activeModule by remember { mutableStateOf("dashboard") }
    var showAllToolsSheet by remember { mutableStateOf(false) }
    var quickCoinUser by remember { mutableStateOf<UserProfile?>(null) }
    var showQuickCoinPicker by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().background(ArenaCharcoal)) {
        // Home-Themed Admin Top Bar
        Surface(
            color = HeaderNavy,
            shadowElevation = 4.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("admin_back_btn")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = CoinYellow)
                    }
                    Column {
                        Text(
                            text = "AHXAN ADMIN HUB",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            maxLines = 1
                        )
                        Text(
                            text = if (isSuperAdmin) "Super Admin • Full Control" else "${staffRole?.roleLabel ?: "Staff"} RBAC",
                            style = MaterialTheme.typography.labelSmall,
                            color = ArenaOrange,
                            maxLines = 1
                        )
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (canManageUsers) {
                        Surface(
                            color = ArenaGreen,
                            shape = RoundedCornerShape(50),
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .clickable { showQuickCoinPicker = true }
                                .testTag("admin_header_add_coin_btn")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                CoinBadge(size = 15.dp)
                                Spacer(Modifier.width(4.dp))
                                Text("+ Coin", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    Surface(
                        color = ObsidianElevated,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, ArenaOrange.copy(alpha = 0.6f)),
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { showAllToolsSheet = true }
                            .testTag("admin_all_tools_btn")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.Apps, contentDescription = "All Tools", tint = CoinYellow, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Tools", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }

        StatusToastBanner(banner = adminBanner, onDismiss = { adminVm.clearBanner() })

        BoxWithConstraints(modifier = Modifier.weight(1f).fillMaxWidth()) {
            val wide = maxWidth >= 720.dp
            if (wide) {
                Row(modifier = Modifier.fillMaxSize()) {
                    LazyColumn(
                        modifier = Modifier.width(220.dp).fillMaxHeight().background(HeaderNavy).padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(modules, key = { it.key }) { mod ->
                            val selected = activeModule == mod.key
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (selected) ArenaOrange.copy(alpha = 0.22f) else Color.Transparent)
                                    .clickable { activeModule = mod.key }
                                    .padding(horizontal = 10.dp, vertical = 10.dp)
                            ) {
                                Icon(mod.icon, contentDescription = null, tint = if (selected) CoinYellow else TextMuted, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(mod.title, style = MaterialTheme.typography.labelLarge, color = if (selected) Color.White else TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                    Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        AdminModuleContent(
                            activeModule = activeModule,
                            users = users,
                            deposits = deposits,
                            withdrawals = withdrawals,
                            tickets = tickets,
                            tournaments = tournaments,
                            coupons = coupons,
                            referrals = referrals,
                            banners = allBanners,
                            categories = allCategories,
                            staffRoles = staffRoles,
                            auditLogs = auditLogs,
                            pushDevices = pushDevices,
                            broadcasts = broadcasts,
                            roster = roster,
                            ticketMsgs = ticketMsgs,
                            generalConfig = generalConfig,
                            gatewayConfig = gatewayConfig,
                            referralConfig = referralConfig,
                            isSuperAdmin = isSuperAdmin,
                            busy = busy,
                            adminVm = adminVm,
                            onNavigateModule = { activeModule = it },
                            onQuickAddCoins = { user ->
                                if (user == null) showQuickCoinPicker = true else quickCoinUser = user
                            }
                        )
                    }
                }
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.background(HeaderNavy)
                    ) {
                        items(modules, key = { it.key }) { mod ->
                            FilterChip(
                                selected = activeModule == mod.key,
                                onClick = { activeModule = mod.key },
                                leadingIcon = {
                                    Icon(mod.icon, contentDescription = null, modifier = Modifier.size(15.dp))
                                },
                                label = { Text(mod.title, fontWeight = FontWeight.SemiBold) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = ArenaOrange,
                                    selectedLabelColor = Color.White,
                                    selectedLeadingIconColor = Color.White,
                                    containerColor = ObsidianElevated,
                                    labelColor = TextSecondary
                                )
                            )
                        }
                    }
                    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        AdminModuleContent(
                            activeModule = activeModule,
                            users = users,
                            deposits = deposits,
                            withdrawals = withdrawals,
                            tickets = tickets,
                            tournaments = tournaments,
                            coupons = coupons,
                            referrals = referrals,
                            banners = allBanners,
                            categories = allCategories,
                            staffRoles = staffRoles,
                            auditLogs = auditLogs,
                            pushDevices = pushDevices,
                            broadcasts = broadcasts,
                            roster = roster,
                            ticketMsgs = ticketMsgs,
                            generalConfig = generalConfig,
                            gatewayConfig = gatewayConfig,
                            referralConfig = referralConfig,
                            isSuperAdmin = isSuperAdmin,
                            busy = busy,
                            adminVm = adminVm,
                            onNavigateModule = { activeModule = it },
                            onQuickAddCoins = { user ->
                                if (user == null) showQuickCoinPicker = true else quickCoinUser = user
                            }
                        )
                    }
                }
            }
        }
    }

    if (showAllToolsSheet) {
        ModalBottomSheet(onDismissRequest = { showAllToolsSheet = false }, containerColor = ObsidianSurface) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                Text("All Admin Power Modules", style = MaterialTheme.typography.headlineSmall, color = CoinYellow)
                Text("Tap any tool to jump instantly", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                Spacer(Modifier.height(12.dp))
                modules.chunked(2).forEach { rowMods ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowMods.forEach { mod ->
                            val isSelected = activeModule == mod.key
                            Surface(
                                color = if (isSelected) mod.accent.copy(alpha = 0.22f) else ObsidianElevated,
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, if (isSelected) mod.accent else mod.accent.copy(alpha = 0.4f)),
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        activeModule = mod.key
                                        showAllToolsSheet = false
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(mod.icon, contentDescription = null, tint = mod.accent, modifier = Modifier.size(20.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = mod.title,
                                        color = TextPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                        if (rowMods.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
                Spacer(Modifier.height(20.dp))
            }
        }
    }

    if (showQuickCoinPicker || quickCoinUser != null) {
        AdminCoinAdjustmentSheet(
            initialUser = quickCoinUser,
            allUsers = users,
            busy = busy,
            adminVm = adminVm,
            onDismiss = {
                quickCoinUser = null
                showQuickCoinPicker = false
            }
        )
    }
}

@Composable
private fun AdminModuleContent(
    activeModule: String,
    users: List<UserProfile>,
    deposits: List<DepositOrder>,
    withdrawals: List<WithdrawalRequest>,
    tickets: List<SupportTicket>,
    tournaments: List<Tournament>,
    coupons: List<com.example.data.model.Coupon>,
    referrals: List<com.example.data.model.ReferralRecord>,
    banners: List<com.example.data.model.CmsBanner>,
    categories: List<GameCategory>,
    staffRoles: List<StaffRole>,
    auditLogs: List<com.example.data.model.AuditLog>,
    pushDevices: List<com.example.data.model.PushDevice>,
    broadcasts: List<com.example.data.model.BroadcastNotification>,
    roster: List<com.example.data.model.MatchSlot>,
    ticketMsgs: List<com.example.data.model.TicketMessage>,
    generalConfig: com.example.data.model.GeneralConfig,
    gatewayConfig: com.example.data.model.GatewayConfig,
    referralConfig: com.example.data.model.ReferralConfig,
    isSuperAdmin: Boolean,
    busy: Boolean,
    adminVm: AdminViewModel,
    onNavigateModule: (String) -> Unit,
    onQuickAddCoins: (UserProfile?) -> Unit
) {
    when (activeModule) {
        "dashboard" -> ExecutiveDashboardModule(
            users = users,
            deposits = deposits,
            withdrawals = withdrawals,
            tournaments = tournaments,
            tickets = tickets,
            onNavigateModule = onNavigateModule,
            onQuickAddCoins = onQuickAddCoins
        )
        "push" -> AdminNotificationStudioModule(broadcasts, pushDevices, busy, adminVm)
        "categories" -> AdminCategoriesModule(categories, busy, adminVm)
        "matches" -> AdminTournamentEngineModule(tournaments, roster, categories, busy, adminVm)
        "coupons" -> AdminCouponsModule(coupons, busy, adminVm)
        "referrals" -> AdminReferralsModule(referralConfig, referrals, users, busy, adminVm)
        "tickets" -> AdminSupportDeskModule(tickets, users, ticketMsgs, busy, adminVm)
        "gateway" -> AdminGatewayDepositsModule(deposits, gatewayConfig, referralConfig, busy, adminVm)
        "withdrawals" -> AdminWithdrawalsModule(withdrawals, gatewayConfig, busy, adminVm)
        "users" -> AdminUsersCrmModule(users, busy, adminVm)
        "staff" -> AdminStaffRolesModule(staffRoles, isSuperAdmin, busy, adminVm)
        "cms" -> AdminCmsEngagementModule(generalConfig, banners, busy, adminVm)
        "audit" -> AdminAuditLogsModule(auditLogs)
    }
}

@Composable
private fun ExecutiveDashboardModule(
    users: List<UserProfile>,
    deposits: List<DepositOrder>,
    withdrawals: List<WithdrawalRequest>,
    tournaments: List<Tournament>,
    tickets: List<SupportTicket>,
    onNavigateModule: (String) -> Unit,
    onQuickAddCoins: (UserProfile?) -> Unit
) {
    val completedDeposits = deposits.filter { it.status == "completed" }.sumOf { it.amount }
    val pendingDepositsCount = deposits.count { it.status == "pending" }
    val approvedWithdrawals = withdrawals.filter { it.status == "approved" }.sumOf { it.grossAmount }
    val pendingWithdrawals = withdrawals.filter { it.status == "pending" }.sumOf { it.netAmount }
    val pendingWdCount = withdrawals.count { it.status == "pending" }
    val liveMatches = tournaments.count { it.status == "live" || it.status == "upcoming" }
    val openTickets = tickets.count { it.status == "open" }
    val totalUserCoins = users.sumOf { it.totalBalance }
    val netProfit = completedDeposits - approvedWithdrawals

    val now = System.currentTimeMillis()
    val dayMs = 86400_000L
    val chartBars = remember(deposits, withdrawals) {
        (6 downTo 0).map { offset ->
            val start = now - (offset + 1) * dayMs
            val end = now - offset * dayMs
            val rev = deposits.filter { it.status == "completed" && it.paidAtMillis in start..end }.sumOf { it.amount }.toFloat()
            val pay = withdrawals.filter { it.status == "approved" && (it.updatedAt?.toDate()?.time ?: 0L) in start..end }.sumOf { it.grossAmount }.toFloat()
            rev to pay
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SectionHeader("Power Command Center", "Live economy, player wallets & quick actions")
        }

        // Quick Action Command Grid (2 rows of 3 tiles)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DashboardCommandTile(
                        title = "+ Add Coin",
                        subtitle = "Credit Any User",
                        icon = Icons.Default.AccountBalanceWallet,
                        accent = ArenaGreen,
                        modifier = Modifier.weight(1f).testTag("dash_add_coin_tile"),
                        onClick = { onQuickAddCoins(null) }
                    )
                    DashboardCommandTile(
                        title = "All Users",
                        subtitle = "${users.size} Players",
                        icon = Icons.Default.Group,
                        accent = ArenaOrange,
                        modifier = Modifier.weight(1f).testTag("dash_all_users_tile"),
                        onClick = { onNavigateModule("users") }
                    )
                    DashboardCommandTile(
                        title = "Matches",
                        subtitle = "$liveMatches Active",
                        icon = Icons.Default.EmojiEvents,
                        accent = ArenaPurple,
                        modifier = Modifier.weight(1f).testTag("dash_matches_tile"),
                        onClick = { onNavigateModule("matches") }
                    )
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DashboardCommandTile(
                        title = "Deposits",
                        subtitle = "$pendingDepositsCount Pending",
                        icon = Icons.Default.Payment,
                        accent = CoinYellow,
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigateModule("gateway") }
                    )
                    DashboardCommandTile(
                        title = "Payouts",
                        subtitle = "$pendingWdCount Pending",
                        icon = Icons.Default.AccountBalanceWallet,
                        accent = ArenaBlue,
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigateModule("withdrawals") }
                    )
                    DashboardCommandTile(
                        title = "Categories",
                        subtitle = "CMS & App",
                        icon = Icons.Default.Category,
                        accent = ArenaOrange,
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigateModule("categories") }
                    )
                }
            }
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                KpiMiniStat("Players", "${users.size}", ArenaOrange, Modifier.weight(1f))
                KpiMiniStat("User Coins", "${totalUserCoins.toInt()} C", CoinYellow, Modifier.weight(1f))
                KpiMiniStat("Open Tickets", "$openTickets", ArenaBlue, Modifier.weight(1f))
            }
        }
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                KpiMiniStat("Deposits", ArenaFormatters.formatInr(completedDeposits), ArenaGreen, Modifier.weight(1f))
                KpiMiniStat("Pending Payout", ArenaFormatters.formatInr(pendingWithdrawals), CoinGold, Modifier.weight(1f))
                KpiMiniStat("Net Balance", ArenaFormatters.formatInr(netProfit), ArenaPurple, Modifier.weight(1f))
            }
        }

        // Quick Users & Wallets Section right inside Dashboard
        item {
            SectionHeader(
                title = "Players & Wallets (${users.size})",
                subtitle = "1-tap Add Coin directly from Dashboard",
                actionLabel = "View All ->",
                onAction = { onNavigateModule("users") }
            )
        }
        if (users.isEmpty()) {
            item {
                CyberGlassCard(glowColor = ArenaOrange) {
                    Text("Waiting for player records...", color = TextMuted, style = MaterialTheme.typography.bodyMedium)
                }
            }
        } else {
            items(users.sortedByDescending { it.totalBalance }.take(5), key = { "dash_u_${it.userId}" }) { u ->
                CyberGlassCard(glowColor = ArenaGreen) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(ArenaPurple.copy(alpha = 0.35f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = u.displayName.ifBlank { u.email }.take(1).uppercase(),
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(u.displayName.ifBlank { "Player" }, color = TextPrimary, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(
                                    "${u.totalBalance.toInt()} Coins • ${u.email.ifBlank { u.inGameName }}",
                                    color = CoinYellow,
                                    style = MaterialTheme.typography.labelSmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        Spacer(Modifier.width(8.dp))
                        Button(
                            onClick = { onQuickAddCoins(u) },
                            colors = ButtonDefaults.buttonColors(containerColor = ArenaGreen, contentColor = Color.White),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(Modifier.width(3.dp))
                            Text("Coin", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        item {
            CyberGlassCard(glowColor = ArenaOrange) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Assessment, contentDescription = null, tint = ArenaOrange)
                    Spacer(Modifier.width(8.dp))
                    Text("7-Day Deposits (Orange) vs Payouts (Green)", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                }
                Spacer(Modifier.height(12.dp))
                val maxVal = (chartBars.maxOfOrNull { maxOf(it.first, it.second) } ?: 1f).coerceAtLeast(100f)
                Canvas(modifier = Modifier.fillMaxWidth().height(130.dp)) {
                    val slotW = size.width / chartBars.size.coerceAtLeast(1)
                    val barW = (slotW * 0.28f).coerceAtLeast(8f)
                    chartBars.forEachIndexed { idx, (rev, pay) ->
                        val xBase = idx * slotW + slotW * 0.18f
                        val revH = (rev / maxVal) * size.height * 0.9f
                        val payH = (pay / maxVal) * size.height * 0.9f
                        drawRect(color = ArenaOrange, topLeft = Offset(xBase, size.height - revH), size = Size(barW, revH.coerceAtLeast(4f)))
                        drawRect(color = ArenaGreen, topLeft = Offset(xBase + barW + 4f, size.height - payH), size = Size(barW, payH.coerceAtLeast(4f)))
                    }
                }
            }
        }

        item { SectionHeader("Live Financial Feed") }
        items(deposits.take(4), key = { "fd_${it.id}" }) { d ->
            CyberGlassCard(glowColor = if (d.status == "completed") ArenaGreen else CoinYellow) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Deposit: ${d.userName} (${d.provider.uppercase()})", color = TextPrimary, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("+${ArenaFormatters.formatInr(d.amount)} • ${d.status}", color = if (d.status == "completed") ArenaGreen else CoinYellow, fontWeight = FontWeight.Bold)
                }
            }
        }
        items(withdrawals.take(4), key = { "fw_${it.id}" }) { w ->
            CyberGlassCard(glowColor = CoinYellow) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Withdraw: ${w.userName} (${w.method})", color = TextPrimary, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("${ArenaFormatters.formatInr(w.netAmount)} • ${w.status}", color = CoinYellow, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun DashboardCommandTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accent: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        color = HeaderNavy,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.55f)),
        shadowElevation = 2.dp,
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(accent.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(18.dp))
            }
            Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(subtitle, color = accent, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

