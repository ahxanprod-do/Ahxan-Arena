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
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CalendarMonth
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
import androidx.compose.material.icons.filled.SystemUpdate
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
import androidx.compose.runtime.LaunchedEffect
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
import com.example.ui.theme.CrimsonDanger
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

    val canManageUsers = isSuperAdmin || (staffRole?.manageUsers == true)
    val canManageMatches = isSuperAdmin || (staffRole?.manageMatches == true)
    val canPublishRooms = isSuperAdmin || (staffRole?.publishRooms == true)
    val canVerifyDeposits = isSuperAdmin || (staffRole?.verifyDeposits == true)
    val canApproveWithdrawals = isSuperAdmin || (staffRole?.approveWithdrawals == true)
    val canManageCoupons = isSuperAdmin || (staffRole?.manageCoupons == true)
    val canReplyTickets = isSuperAdmin || (staffRole?.replyTickets == true)
    val canSendPush = isSuperAdmin || (staffRole?.sendPush == true)
    val canSystemSettings = isSuperAdmin || (staffRole?.systemSettings == true)

    val users by if (canManageUsers) adminVm.allUsers.collectAsStateWithLifecycle() else remember { mutableStateOf(emptyList()) }
    val deposits by if (canVerifyDeposits) adminVm.allDeposits.collectAsStateWithLifecycle() else remember { mutableStateOf(emptyList()) }
    val withdrawals by if (canApproveWithdrawals) adminVm.allWithdrawals.collectAsStateWithLifecycle() else remember { mutableStateOf(emptyList()) }
    val tickets by if (canReplyTickets) adminVm.allTickets.collectAsStateWithLifecycle() else remember { mutableStateOf(emptyList()) }
    val coupons by if (canManageCoupons) adminVm.allCoupons.collectAsStateWithLifecycle() else remember { mutableStateOf(emptyList()) }
    val referrals by if (canManageUsers) adminVm.allReferrals.collectAsStateWithLifecycle() else remember { mutableStateOf(emptyList()) }
    val allBanners by if (canSystemSettings) adminVm.allBanners.collectAsStateWithLifecycle() else remember { mutableStateOf(emptyList()) }
    val allCategories by if (canManageMatches || canSystemSettings) adminVm.allCategories.collectAsStateWithLifecycle() else remember { mutableStateOf(emptyList()) }
    val staffRoles by if (isSuperAdmin) adminVm.staffRoles.collectAsStateWithLifecycle() else remember { mutableStateOf(emptyList()) }
    val auditLogs by if (canSystemSettings) adminVm.auditLogs.collectAsStateWithLifecycle() else remember { mutableStateOf(emptyList()) }
    val pushDevices by if (canSendPush) adminVm.pushDevices.collectAsStateWithLifecycle() else remember { mutableStateOf(emptyList()) }
    val broadcasts by if (canSendPush) adminVm.broadcasts.collectAsStateWithLifecycle() else remember { mutableStateOf(emptyList()) }
    val roster by adminVm.selectedRoster.collectAsStateWithLifecycle()
    val ticketMsgs by adminVm.ticketMessages.collectAsStateWithLifecycle()
    val adminBanner by adminVm.adminBanner.collectAsStateWithLifecycle()
    val busy by adminVm.busy.collectAsStateWithLifecycle()

    val tournaments by arenaVm.tournaments.collectAsStateWithLifecycle()
    val generalConfig by arenaVm.generalConfig.collectAsStateWithLifecycle()
    val gatewayConfig by arenaVm.gatewayConfig.collectAsStateWithLifecycle()
    val referralConfig by arenaVm.referralConfig.collectAsStateWithLifecycle()
    val appUpdateConfig by arenaVm.appUpdateConfig.collectAsStateWithLifecycle()

    val pendingDeposits = remember(deposits) { deposits.count { it.status == "pending" } }
    val pendingWithdrawals = remember(withdrawals) { withdrawals.count { it.status == "pending" } }
    val openTickets = remember(tickets) { tickets.count { it.status == "open" } }

    val modules = remember(
        isSuperAdmin,
        staffRole,
        users.size,
        tournaments.size,
        allCategories.size,
        pendingDeposits,
        pendingWithdrawals,
        openTickets
    ) {
        listOf(
            AdminNavModule("dashboard", "Dashboard", "", ArenaOrange, Icons.Default.Dashboard, true),
            AdminNavModule("users", "Users & Coins (${users.size})", "${users.size}", ArenaGreen, Icons.Default.Group, canManageUsers),
            AdminNavModule("scheduler", "Scheduler", "", ArenaOrange, Icons.Default.CalendarMonth, canManageMatches),
            AdminNavModule("matches", "Matches (${tournaments.size})", "${tournaments.size}", ArenaPurple, Icons.Default.EmojiEvents, canManageMatches || canPublishRooms),
            AdminNavModule("categories", "Categories (${allCategories.size})", "", ArenaOrange, Icons.Default.Category, canManageMatches || canSystemSettings),
            AdminNavModule("gateway", "Deposits" + if (pendingDeposits > 0) " ($pendingDeposits)" else "", if (pendingDeposits > 0) "$pendingDeposits" else "", CoinYellow, Icons.Default.Payment, canVerifyDeposits),
            AdminNavModule("withdrawals", "Payouts" + if (pendingWithdrawals > 0) " ($pendingWithdrawals)" else "", if (pendingWithdrawals > 0) "$pendingWithdrawals" else "", ArenaGreen, Icons.Default.AccountBalanceWallet, canApproveWithdrawals),
            AdminNavModule("tickets", "Tickets" + if (openTickets > 0) " ($openTickets)" else "", if (openTickets > 0) "$openTickets" else "", ArenaBlue, Icons.Default.SupportAgent, canReplyTickets),
            AdminNavModule("push", "Push Studio", "", ArenaPurple, Icons.Default.NotificationsActive, canSendPush),
            AdminNavModule("coupons", "Coupons", "", CoinYellow, Icons.Default.LocalOffer, canManageCoupons),
            AdminNavModule("referrals", "Referrals", "", ArenaGreen, Icons.Default.CardGiftcard, canManageUsers),
            AdminNavModule("cms", "CMS & Pages", "", ArenaOrange, Icons.Default.Settings, canSystemSettings),
            AdminNavModule("app_update", "New Update", "", ArenaBlue, Icons.Default.SystemUpdate, canSystemSettings),
            AdminNavModule("staff", "Staff RBAC", "", ArenaPurple, Icons.Default.AdminPanelSettings, isSuperAdmin),
            AdminNavModule("audit", "Audit Logs", "", ArenaBlue, Icons.Default.History, canSystemSettings)
        ).filter { it.allowed }
    }

    var activeModule by remember { mutableStateOf("dashboard") }
    var showAllToolsSheet by remember { mutableStateOf(false) }
    var quickCoinUser by remember { mutableStateOf<UserProfile?>(null) }
    var showQuickCoinPicker by remember { mutableStateOf(false) }

    LaunchedEffect(modules, activeModule) {
        if (modules.none { it.key == activeModule }) {
            activeModule = "dashboard"
        }
    }

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
                            allowedKeys = modules.map { it.key }.toSet(),
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
                            appUpdateConfig = appUpdateConfig,
                            isSuperAdmin = isSuperAdmin,
                            staffRole = staffRole,
                            busy = busy,
                            adminVm = adminVm,
                            onNavigateModule = { target ->
                                if (modules.any { it.key == target }) activeModule = target
                            },
                            onQuickAddCoins = { user ->
                                if (canManageUsers) {
                                    if (user == null) showQuickCoinPicker = true else quickCoinUser = user
                                }
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
                            allowedKeys = modules.map { it.key }.toSet(),
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
                            appUpdateConfig = appUpdateConfig,
                            isSuperAdmin = isSuperAdmin,
                            staffRole = staffRole,
                            busy = busy,
                            adminVm = adminVm,
                            onNavigateModule = { target ->
                                if (modules.any { it.key == target }) activeModule = target
                            },
                            onQuickAddCoins = { user ->
                                if (canManageUsers) {
                                    if (user == null) showQuickCoinPicker = true else quickCoinUser = user
                                }
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

    if (canManageUsers && (showQuickCoinPicker || quickCoinUser != null)) {
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
    allowedKeys: Set<String>,
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
    appUpdateConfig: com.example.data.model.AppUpdateConfig,
    isSuperAdmin: Boolean,
    staffRole: StaffRole?,
    busy: Boolean,
    adminVm: AdminViewModel,
    onNavigateModule: (String) -> Unit,
    onQuickAddCoins: (UserProfile?) -> Unit
) {
    val canManageMatches = isSuperAdmin || (staffRole?.manageMatches == true)
    val canPublishRooms = isSuperAdmin || (staffRole?.publishRooms == true)

    if (activeModule != "dashboard" && activeModule !in allowedKeys) {
        Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            CyberGlassCard(glowColor = CrimsonDanger) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Block, contentDescription = null, tint = CrimsonDanger, modifier = Modifier.size(40.dp))
                    Spacer(Modifier.height(8.dp))
                    Text("Permission Not Granted", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                    Text(
                        "Only modules assigned by the Super Admin are accessible.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                    Spacer(Modifier.height(10.dp))
                    Button(onClick = { onNavigateModule("dashboard") }) {
                        Text("Return to Dashboard")
                    }
                }
            }
        }
        return
    }

    when (activeModule) {
        "dashboard" -> ExecutiveDashboardModule(
            users = users,
            deposits = deposits,
            withdrawals = withdrawals,
            tournaments = tournaments,
            tickets = tickets,
            isSuperAdmin = isSuperAdmin,
            staffRole = staffRole,
            onNavigateModule = onNavigateModule,
            onQuickAddCoins = onQuickAddCoins
        )
        "push" -> AdminNotificationStudioModule(broadcasts, pushDevices, busy, adminVm)
        "scheduler" -> AdminTournamentSchedulerModule(tournaments, categories, busy, adminVm)
        "categories" -> AdminCategoriesModule(categories, busy, adminVm)
        "matches" -> AdminTournamentEngineModule(
            tournaments = tournaments,
            roster = roster,
            categories = categories,
            busy = busy,
            adminVm = adminVm,
            canManageMatches = canManageMatches,
            canPublishRooms = canPublishRooms
        )
        "coupons" -> AdminCouponsModule(coupons, busy, adminVm)
        "referrals" -> AdminReferralsModule(referralConfig, referrals, users, busy, adminVm)
        "tickets" -> AdminSupportDeskModule(tickets, users, ticketMsgs, busy, adminVm)
        "gateway" -> AdminGatewayDepositsModule(deposits, gatewayConfig, referralConfig, busy, adminVm)
        "withdrawals" -> AdminWithdrawalsModule(withdrawals, gatewayConfig, busy, adminVm)
        "users" -> AdminUsersCrmModule(users, busy, adminVm)
        "staff" -> AdminStaffRolesModule(staffRoles, isSuperAdmin, busy, adminVm)
        "cms" -> AdminCmsEngagementModule(generalConfig, banners, busy, adminVm)
        "app_update" -> AdminAppUpdateModule(appUpdateConfig, busy, adminVm)
        "audit" -> AdminAuditLogsModule(auditLogs)
    }
}

private data class QuickTileSpec(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val accent: Color,
    val tag: String = "",
    val onClick: () -> Unit
)

@Composable
private fun ExecutiveDashboardModule(
    users: List<UserProfile>,
    deposits: List<DepositOrder>,
    withdrawals: List<WithdrawalRequest>,
    tournaments: List<Tournament>,
    tickets: List<SupportTicket>,
    isSuperAdmin: Boolean,
    staffRole: StaffRole?,
    onNavigateModule: (String) -> Unit,
    onQuickAddCoins: (UserProfile?) -> Unit
) {
    val canManageUsers = isSuperAdmin || (staffRole?.manageUsers == true)
    val canManageMatches = isSuperAdmin || (staffRole?.manageMatches == true)
    val canPublishRooms = isSuperAdmin || (staffRole?.publishRooms == true)
    val canVerifyDeposits = isSuperAdmin || (staffRole?.verifyDeposits == true)
    val canApproveWithdrawals = isSuperAdmin || (staffRole?.approveWithdrawals == true)
    val canManageCoupons = isSuperAdmin || (staffRole?.manageCoupons == true)
    val canReplyTickets = isSuperAdmin || (staffRole?.replyTickets == true)
    val canSendPush = isSuperAdmin || (staffRole?.sendPush == true)
    val canSystemSettings = isSuperAdmin || (staffRole?.systemSettings == true)

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

    val quickTiles = buildList {
        if (canManageUsers) {
            add(QuickTileSpec("+ Add Coin", "Credit Any User", Icons.Default.AccountBalanceWallet, ArenaGreen, "dash_add_coin_tile") { onQuickAddCoins(null) })
            add(QuickTileSpec("All Users", "${users.size} Players", Icons.Default.Group, ArenaOrange, "dash_all_users_tile") { onNavigateModule("users") })
        }
        if (canManageMatches || canPublishRooms) {
            add(QuickTileSpec("Matches", if (canManageMatches) "$liveMatches Active" else "Room IDs", Icons.Default.EmojiEvents, ArenaPurple, "dash_matches_tile") { onNavigateModule("matches") })
        }
        if (canVerifyDeposits) {
            add(QuickTileSpec("Deposits", "$pendingDepositsCount Pending", Icons.Default.Payment, CoinYellow) { onNavigateModule("gateway") })
        }
        if (canApproveWithdrawals) {
            add(QuickTileSpec("Payouts", "$pendingWdCount Pending", Icons.Default.AccountBalanceWallet, ArenaBlue) { onNavigateModule("withdrawals") })
        }
        if (canManageMatches) {
            add(QuickTileSpec("Schedule", "Auto Open/Close", Icons.Default.CalendarMonth, ArenaOrange, "dash_scheduler_tile") { onNavigateModule("scheduler") })
        }
        if (canReplyTickets) {
            add(QuickTileSpec("Tickets", "$openTickets Open", Icons.Default.SupportAgent, ArenaBlue) { onNavigateModule("tickets") })
        }
        if (canSendPush) {
            add(QuickTileSpec("Push Studio", "Send / Delete", Icons.Default.NotificationsActive, ArenaPurple) { onNavigateModule("push") })
        }
        if (canManageCoupons) {
            add(QuickTileSpec("Coupons", "Promo Codes", Icons.Default.LocalOffer, CoinYellow) { onNavigateModule("coupons") })
        }
        if (canSystemSettings) {
            add(QuickTileSpec("New Update", "Force Update Link", Icons.Default.SystemUpdate, ArenaBlue, "dash_app_update_tile") { onNavigateModule("app_update") })
            add(QuickTileSpec("CMS & Pages", "Policies & Banners", Icons.Default.Settings, ArenaOrange) { onNavigateModule("cms") })
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SectionHeader(
                title = if (isSuperAdmin) "Power Command Center" else "${staffRole?.roleLabel ?: "Staff"} Workspace",
                subtitle = if (isSuperAdmin) "Live economy, player wallets & quick actions" else "Only modules assigned to you by Admin are active"
            )
        }

        if (!isSuperAdmin) {
            item {
                val granted = staffRole?.grantedPermissionLabels().orEmpty()
                CyberGlassCard(glowColor = ArenaPurple) {
                    Text("Your Assigned Permissions (${granted.size})", style = MaterialTheme.typography.titleSmall, color = CoinYellow)
                    Spacer(Modifier.height(6.dp))
                    @OptIn(ExperimentalLayoutApi::class)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        granted.forEach { label ->
                            StatusPill(label, ArenaGreen)
                        }
                    }
                }
            }
        }

        if (quickTiles.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    quickTiles.chunked(3).forEach { rowTiles ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            rowTiles.forEach { tile ->
                                DashboardCommandTile(
                                    title = tile.title,
                                    subtitle = tile.subtitle,
                                    icon = tile.icon,
                                    accent = tile.accent,
                                    modifier = Modifier.weight(1f).let { m -> if (tile.tag.isNotBlank()) m.testTag(tile.tag) else m },
                                    onClick = tile.onClick
                                )
                            }
                            repeat(3 - rowTiles.size) {
                                Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }

        if (canManageUsers || canReplyTickets) {
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (canManageUsers) {
                        KpiMiniStat("Players", "${users.size}", ArenaOrange, Modifier.weight(1f))
                        KpiMiniStat("User Coins", "${totalUserCoins.toInt()} C", CoinYellow, Modifier.weight(1f))
                    }
                    if (canReplyTickets) {
                        KpiMiniStat("Open Tickets", "$openTickets", ArenaBlue, Modifier.weight(1f))
                    }
                }
            }
        }

        if (canVerifyDeposits || canApproveWithdrawals) {
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (canVerifyDeposits) {
                        KpiMiniStat("Deposits", ArenaFormatters.formatInr(completedDeposits), ArenaGreen, Modifier.weight(1f))
                    }
                    if (canApproveWithdrawals) {
                        KpiMiniStat("Pending Payout", ArenaFormatters.formatInr(pendingWithdrawals), CoinGold, Modifier.weight(1f))
                    }
                    if (isSuperAdmin || (canVerifyDeposits && canApproveWithdrawals)) {
                        KpiMiniStat("Net Balance", ArenaFormatters.formatInr(netProfit), ArenaPurple, Modifier.weight(1f))
                    }
                }
            }
        }

        if (canManageUsers) {
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
        }

        if (isSuperAdmin || (canVerifyDeposits && canApproveWithdrawals)) {
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
        }

        if (canVerifyDeposits || canApproveWithdrawals) {
            item { SectionHeader("Live Financial Feed") }
            if (canVerifyDeposits) {
                items(deposits.take(4), key = { "fd_${it.id}" }) { d ->
                    CyberGlassCard(glowColor = if (d.status == "completed") ArenaGreen else CoinYellow) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Deposit: ${d.userName} (${d.provider.uppercase()})", color = TextPrimary, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("+${ArenaFormatters.formatInr(d.amount)} • ${d.status}", color = if (d.status == "completed") ArenaGreen else CoinYellow, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            if (canApproveWithdrawals) {
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

