package com.example.ui.screens.admin

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.DepositOrder
import com.example.data.model.StaffRole
import com.example.data.model.SupportTicket
import com.example.data.model.Tournament
import com.example.data.model.UserProfile
import com.example.data.model.WithdrawalRequest
import com.example.ui.components.CyberGlassCard
import com.example.ui.components.KpiMiniStat
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusPill
import com.example.ui.components.StatusToastBanner
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
import com.example.viewmodel.AdminViewModel
import com.example.viewmodel.ArenaViewModel

private data class AdminNavModule(
    val key: String,
    val title: String,
    val icon: ImageVector,
    val allowed: Boolean
)

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

    val modules = remember(isSuperAdmin, staffRole) {
        listOf(
            AdminNavModule("dashboard", "1. Dashboard", Icons.Default.Dashboard, true),
            AdminNavModule("push", "2. Push Studio", Icons.Default.NotificationsActive, isSuperAdmin || (staffRole?.sendPush == true)),
            AdminNavModule("matches", "3. Matches & Rooms", Icons.Default.EmojiEvents, isSuperAdmin || (staffRole?.manageMatches == true) || (staffRole?.publishRooms == true)),
            AdminNavModule("coupons", "4. Coupons", Icons.Default.LocalOffer, isSuperAdmin || (staffRole?.manageCoupons == true)),
            AdminNavModule("referrals", "5. Referrals", Icons.Default.CardGiftcard, isSuperAdmin || (staffRole?.manageUsers == true)),
            AdminNavModule("tickets", "6. Support Desk", Icons.Default.SupportAgent, isSuperAdmin || (staffRole?.replyTickets == true)),
            AdminNavModule("gateway", "7. PayZapi & Deposits", Icons.Default.Payment, isSuperAdmin || (staffRole?.verifyDeposits == true)),
            AdminNavModule("withdrawals", "8. Withdrawals", Icons.Default.AccountBalanceWallet, isSuperAdmin || (staffRole?.approveWithdrawals == true)),
            AdminNavModule("users", "9. Users CRM", Icons.Default.Group, isSuperAdmin || (staffRole?.manageUsers == true)),
            AdminNavModule("staff", "10. Staff RBAC", Icons.Default.AdminPanelSettings, isSuperAdmin),
            AdminNavModule("cms", "11. CMS & App", Icons.Default.Settings, isSuperAdmin || (staffRole?.systemSettings == true)),
            AdminNavModule("audit", "12. Audit Logs", Icons.Default.History, isSuperAdmin || (staffRole?.systemSettings == true))
        ).filter { it.allowed }
    }

    var activeModule by remember { mutableStateOf("dashboard") }

    Column(modifier = Modifier.fillMaxSize().background(ObsidianBg)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(ObsidianSurface)
                .padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack, modifier = Modifier.testTag("admin_back_btn")) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = AmberWarning)
                }
                Column {
                    Text("ADMIN COMMAND CENTER", style = MaterialTheme.typography.titleLarge, color = AmberWarning)
                    Text(if (isSuperAdmin) "Super Admin • Full Access" else "${staffRole?.roleLabel ?: "Staff"} RBAC", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                }
            }
            StatusPill("${modules.size} MODULES", NeonCyan)
        }

        StatusToastBanner(banner = adminBanner, onDismiss = { adminVm.clearBanner() })

        BoxWithConstraints(modifier = Modifier.weight(1f).fillMaxWidth()) {
            val wide = maxWidth >= 720.dp
            if (wide) {
                Row(modifier = Modifier.fillMaxSize()) {
                    LazyColumn(
                        modifier = Modifier.width(210.dp).fillMaxHeight().background(ObsidianSurface).padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(modules, key = { it.key }) { mod ->
                            val selected = activeModule == mod.key
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (selected) NeonCyan.copy(alpha = 0.16f) else Color.Transparent)
                                    .clickable { activeModule = mod.key }
                                    .padding(horizontal = 10.dp, vertical = 10.dp)
                            ) {
                                Icon(mod.icon, contentDescription = null, tint = if (selected) NeonCyan else TextMuted, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(mod.title, style = MaterialTheme.typography.labelLarge, color = if (selected) NeonCyan else TextSecondary)
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
                            adminVm = adminVm
                        )
                    }
                }
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.background(ObsidianSurface)
                    ) {
                        items(modules, key = { it.key }) { mod ->
                            FilterChip(
                                selected = activeModule == mod.key,
                                onClick = { activeModule = mod.key },
                                label = { Text(mod.title) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = AmberWarning,
                                    selectedLabelColor = ObsidianBg,
                                    containerColor = ObsidianElevated
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
                            adminVm = adminVm
                        )
                    }
                }
            }
        }
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
    adminVm: AdminViewModel
) {
    when (activeModule) {
        "dashboard" -> ExecutiveDashboardModule(users, deposits, withdrawals, tournaments, tickets)
        "push" -> AdminNotificationStudioModule(broadcasts, pushDevices, busy, adminVm)
        "matches" -> AdminTournamentEngineModule(tournaments, roster, busy, adminVm)
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
    tickets: List<SupportTicket>
) {
    val completedDeposits = deposits.filter { it.status == "completed" }.sumOf { it.amount }
    val approvedWithdrawals = withdrawals.filter { it.status == "approved" }.sumOf { it.grossAmount }
    val pendingWithdrawals = withdrawals.filter { it.status == "pending" }.sumOf { it.netAmount }
    val liveMatches = tournaments.count { it.status == "live" || it.status == "upcoming" }
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

    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { SectionHeader("Executive Command Dashboard", "Real-time Firestore telemetry") }
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                KpiMiniStat("Players", "${users.size}", NeonCyan, Modifier.weight(1f))
                KpiMiniStat("Active Scrims", "$liveMatches", ElectricViolet, Modifier.weight(1f))
                KpiMiniStat("Open Tickets", "${tickets.count { it.status == "open" }}", AmberWarning, Modifier.weight(1f))
            }
        }
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                KpiMiniStat("Deposits", ArenaFormatters.formatInr(completedDeposits), EmeraldProfit, Modifier.weight(1f))
                KpiMiniStat("Pending Payout", ArenaFormatters.formatInr(pendingWithdrawals), AmberWarning, Modifier.weight(1f))
                KpiMiniStat("Net Balance", ArenaFormatters.formatInr(netProfit), NeonCyan, Modifier.weight(1f))
            }
        }
        item {
            CyberGlassCard(glowColor = NeonCyan) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Assessment, contentDescription = null, tint = NeonCyan)
                    Spacer(Modifier.width(8.dp))
                    Text("7-Day Deposits (Cyan) vs Payouts (Emerald)", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
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
                        drawRect(color = NeonCyan, topLeft = Offset(xBase, size.height - revH), size = Size(barW, revH.coerceAtLeast(4f)))
                        drawRect(color = EmeraldProfit, topLeft = Offset(xBase + barW + 4f, size.height - payH), size = Size(barW, payH.coerceAtLeast(4f)))
                    }
                }
            }
        }
        item { SectionHeader("Live Financial Feed") }
        items(deposits.take(4), key = { "fd_${it.id}" }) { d ->
            CyberGlassCard(glowColor = if (d.status == "completed") EmeraldProfit else AmberWarning) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Deposit: ${d.userName} (${d.provider.uppercase()})", color = TextPrimary)
                    Text("+${ArenaFormatters.formatInr(d.amount)} • ${d.status}", color = if (d.status == "completed") EmeraldProfit else AmberWarning)
                }
            }
        }
        items(withdrawals.take(4), key = { "fw_${it.id}" }) { w ->
            CyberGlassCard(glowColor = AmberWarning) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Withdraw: ${w.userName} (${w.method})", color = TextPrimary)
                    Text("${ArenaFormatters.formatInr(w.netAmount)} • ${w.status}", color = AmberWarning)
                }
            }
        }
    }
}
