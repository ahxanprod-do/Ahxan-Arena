package com.example

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.data.auth.AuthHelper
import com.example.data.auth.authStateFlow
import com.example.data.model.Tournament
import com.example.data.network.PayZapiClient
import com.example.data.repository.ArenaAdminRepository
import com.example.data.repository.ArenaTournamentRepository
import com.example.data.repository.ArenaUserRepository
import com.example.data.repository.ArenaWalletRepository
import com.example.notifications.ArenaNotificationHelper
import com.example.ui.components.CyberGlassCard
import com.example.ui.components.NeonPrimaryButton
import com.example.ui.components.StatusPill
import com.example.ui.components.StatusToastBanner
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.HomeLobbyScreen
import com.example.ui.screens.MyMatchesScreen
import com.example.ui.screens.ProfileAndMoreScreen
import com.example.ui.screens.ReferEarnScreen
import com.example.ui.screens.SupportDeskScreen
import com.example.ui.screens.TournamentDetailSheet
import com.example.ui.screens.TournamentsScreen
import com.example.ui.screens.WalletScreen
import com.example.ui.screens.admin.AdminCommandCenterScreen
import com.example.ui.theme.AhxanArenaTheme
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CrimsonDanger
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.ObsidianElevated
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.viewmodel.AdminViewModel
import com.example.viewmodel.ArenaViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

private data class BottomNavTab(
    val route: String,
    val label: String,
    val icon: ImageVector
)

class MainActivity : ComponentActivity() {
    private val deepLinkRouteState = mutableStateOf<String?>(null)
    private val deepLinkCheckOrderState = mutableStateOf<String?>(null)
    private val deepLinkRefCodeState = mutableStateOf("")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        ArenaNotificationHelper.ensureChannels(this)
        handleIncomingIntent(intent)

        setContent {
            AhxanArenaTheme {
                val authUser by remember { Firebase.auth.authStateFlow() }
                    .collectAsStateWithLifecycle(initialValue = Firebase.auth.currentUser)

                val currentUser = authUser
                if (currentUser == null) {
                    AuthScreen(
                        initialReferralCode = deepLinkRefCodeState.value,
                        onAuthSuccess = { }
                    )
                } else {
                    val uid = currentUser.uid
                    key(uid) {
                        AuthenticatedArenaApp(
                            uid = uid,
                            email = currentUser.email.orEmpty(),
                            emailVerified = currentUser.isEmailVerified,
                            pendingRoute = deepLinkRouteState.value,
                            pendingOrderCheck = deepLinkCheckOrderState.value,
                            onConsumedRoute = { deepLinkRouteState.value = null },
                            onConsumedOrderCheck = { deepLinkCheckOrderState.value = null }
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(intent: Intent?) {
        if (intent == null) return
        val extraRoute = intent.getStringExtra(ArenaNotificationHelper.EXTRA_ROUTE)
        if (!extraRoute.isNullOrBlank()) {
            deepLinkRouteState.value = extraRoute
        }
        val uri = intent.data ?: return
        when (uri.host) {
            "wallet" -> {
                deepLinkRouteState.value = "wallet"
                uri.getQueryParameter("check_order")?.let { orderId ->
                    if (orderId.isNotBlank()) deepLinkCheckOrderState.value = orderId
                }
            }
            "register" -> {
                uri.getQueryParameter("ref")?.let { ref ->
                    if (ref.isNotBlank()) {
                        deepLinkRefCodeState.value = ref.uppercase()
                        getSharedPreferences("ahxan_app", Context.MODE_PRIVATE)
                            .edit().putString("pending_ref_code", ref.uppercase()).apply()
                    }
                }
            }
        }
    }
}

@Composable
private fun AuthenticatedArenaApp(
    uid: String,
    email: String,
    emailVerified: Boolean,
    pendingRoute: String?,
    pendingOrderCheck: String?,
    onConsumedRoute: () -> Unit,
    onConsumedOrderCheck: () -> Unit
) {
    val context = LocalContext.current
    val appContext = context.applicationContext

    val arenaVm: ArenaViewModel = viewModel(
        key = "arena_$uid",
        factory = viewModelFactory {
            initializer {
                val db = FirebaseFirestore.getInstance(appContext.getString(R.string.firestore_database_id))
                val userRepo = ArenaUserRepository(db)
                val tournamentRepo = ArenaTournamentRepository(db)
                val walletRepo = ArenaWalletRepository(db, PayZapiClient(), userRepo)
                val adminRepo = ArenaAdminRepository(db)
                ArenaViewModel(appContext, uid, userRepo, tournamentRepo, walletRepo, adminRepo)
            }
        }
    )

    val adminVm: AdminViewModel = viewModel(
        key = "admin_$uid",
        factory = viewModelFactory {
            initializer {
                val db = FirebaseFirestore.getInstance(appContext.getString(R.string.firestore_database_id))
                val userRepo = ArenaUserRepository(db)
                val walletRepo = ArenaWalletRepository(db, PayZapiClient(), userRepo)
                val adminRepo = ArenaAdminRepository(db)
                AdminViewModel(adminRepo, userRepo, walletRepo)
            }
        }
    )

    val user by arenaVm.userProfile.collectAsStateWithLifecycle()
    val staffRole by arenaVm.staffRole.collectAsStateWithLifecycle()
    val tournaments by arenaVm.tournaments.collectAsStateWithLifecycle()
    val joinedMatches by arenaVm.joinedMatches.collectAsStateWithLifecycle()
    val transactions by arenaVm.transactions.collectAsStateWithLifecycle()
    val deposits by arenaVm.deposits.collectAsStateWithLifecycle()
    val withdrawals by arenaVm.withdrawals.collectAsStateWithLifecycle()
    val activeCoupons by arenaVm.activeCoupons.collectAsStateWithLifecycle()
    val banners by arenaVm.banners.collectAsStateWithLifecycle()
    val generalConfig by arenaVm.generalConfig.collectAsStateWithLifecycle()
    val gatewayConfig by arenaVm.gatewayConfig.collectAsStateWithLifecycle()
    val referralConfig by arenaVm.referralConfig.collectAsStateWithLifecycle()
    val leaderboard by arenaVm.leaderboard.collectAsStateWithLifecycle()
    val myReferrals by arenaVm.myReferrals.collectAsStateWithLifecycle()
    val myTickets by arenaVm.myTickets.collectAsStateWithLifecycle()
    val userNotifications by arenaVm.userNotifications.collectAsStateWithLifecycle()
    val broadcasts by arenaVm.broadcasts.collectAsStateWithLifecycle()
    val selectedTicketMessages by arenaVm.selectedTicketMessages.collectAsStateWithLifecycle()
    val activeCheckoutOrder by arenaVm.activeCheckoutOrder.collectAsStateWithLifecycle()
    val statusBanner by arenaVm.statusBanner.collectAsStateWithLifecycle()
    val busy by arenaVm.busy.collectAsStateWithLifecycle()
    val roomSecretCache by arenaVm.roomSecretCache.collectAsStateWithLifecycle()

    val isSuperAdmin = email.equals("ahxantalib@gmail.com", ignoreCase = true) && emailVerified
    val isAdminAccess = isSuperAdmin || staffRole != null

    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: "home"
    var selectedTournament by remember { mutableStateOf<Tournament?>(null) }

    val navigateTo: (String) -> Unit = remember(navController) {
        { route ->
            val target = route.lowercase().ifBlank { "home" }
            if (target in listOf("home", "tournaments", "my_matches", "wallet", "profile")) {
                navController.navigate(target) {
                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            } else if (target in listOf("refer", "support", "admin")) {
                navController.navigate(target) { launchSingleTop = true }
            }
        }
    }

    LaunchedEffect(pendingRoute) {
        if (!pendingRoute.isNullOrBlank()) {
            navigateTo(pendingRoute)
            onConsumedRoute()
        }
    }

    LaunchedEffect(pendingOrderCheck, deposits.size) {
        if (!pendingOrderCheck.isNullOrBlank() && deposits.isNotEmpty()) {
            arenaVm.verifyOrderFromDeepLink(pendingOrderCheck)
            onConsumedOrderCheck()
        }
    }

    // Account Ban Enforcement
    if (user?.banned == true && !isSuperAdmin) {
        BannedOverlay(reason = user?.banReason.orEmpty())
        return
    }

    // Global Maintenance Mode (Admin/Staff allowed to bypass into Command Center)
    if (generalConfig.maintenanceMode && !isAdminAccess) {
        MaintenanceOverlay(message = generalConfig.maintenanceMessage, eta = generalConfig.maintenanceEta)
        return
    }

    val tabs = remember {
        listOf(
            BottomNavTab("home", "Home", Icons.Default.Home),
            BottomNavTab("tournaments", "Scrims", Icons.Default.EmojiEvents),
            BottomNavTab("my_matches", "Matches", Icons.Default.MilitaryTech),
            BottomNavTab("wallet", "Wallet", Icons.Default.AccountBalanceWallet),
            BottomNavTab("profile", "More", Icons.Default.Person)
        )
    }
    val showMainNav = currentRoute in tabs.map { it.route }

    BoxWithConstraints(modifier = Modifier.fillMaxSize().background(ObsidianBg)) {
        val isWide = maxWidth >= 600.dp

        Scaffold(
            containerColor = ObsidianBg,
            contentWindowInsets = WindowInsets.safeDrawing,
            bottomBar = {
                if (showMainNav && !isWide) {
                    NavigationBar(containerColor = ObsidianSurface) {
                        tabs.forEach { tab ->
                            val selected = currentRoute == tab.route
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateTo(tab.route) },
                                icon = { Icon(tab.icon, contentDescription = tab.label) },
                                label = { Text(tab.label, maxLines = 1) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = NeonCyan,
                                    selectedTextColor = NeonCyan,
                                    indicatorColor = ObsidianElevated,
                                    unselectedIconColor = TextMuted,
                                    unselectedTextColor = TextMuted
                                ),
                                modifier = Modifier.testTag("nav_tab_${tab.route}")
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                if (showMainNav && isWide) {
                    NavigationRail(containerColor = ObsidianSurface, modifier = Modifier.fillMaxHeight()) {
                        Spacer(Modifier.height(16.dp))
                        tabs.forEach { tab ->
                            val selected = currentRoute == tab.route
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateTo(tab.route) },
                                icon = { Icon(tab.icon, contentDescription = tab.label) },
                                label = { Text(tab.label) },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = NeonCyan,
                                    selectedTextColor = NeonCyan,
                                    indicatorColor = ObsidianElevated,
                                    unselectedIconColor = TextMuted,
                                    unselectedTextColor = TextMuted
                                ),
                                modifier = Modifier.testTag("rail_tab_${tab.route}")
                            )
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.TopCenter) {
                    Column(
                        modifier = Modifier
                            .fillMaxHeight()
                            .then(if (currentRoute == "admin") Modifier.fillMaxWidth() else Modifier.widthIn(max = 900.dp).fillMaxWidth())
                    ) {
                        if (generalConfig.maintenanceMode && isAdminAccess && currentRoute != "admin") {
                            Row(
                                modifier = Modifier.fillMaxWidth().background(CrimsonDanger.copy(alpha = 0.25f)).padding(horizontal = 14.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("MAINTENANCE MODE ACTIVE (ADMIN BYPASS)", style = MaterialTheme.typography.labelSmall, color = AmberWarning)
                                Text("Open Admin", style = MaterialTheme.typography.labelLarge, color = NeonCyan)
                            }
                        }

                        StatusToastBanner(banner = statusBanner, onDismiss = { arenaVm.clearBanner() })

                        NavHost(
                            navController = navController,
                            startDestination = "home",
                            modifier = Modifier.weight(1f).fillMaxWidth()
                        ) {
                            composable("home") {
                                HomeLobbyScreen(
                                    user = user,
                                    banners = banners,
                                    generalConfig = generalConfig,
                                    tournaments = tournaments,
                                    notifications = userNotifications,
                                    broadcasts = broadcasts,
                                    onClaimCheckIn = { arenaVm.claimDailyCheckIn() },
                                    onNavigate = navigateTo,
                                    onSelectTournament = { selectedTournament = it },
                                    onMarkNotificationRead = { arenaVm.markNotificationRead(it) }
                                )
                            }
                            composable("tournaments") {
                                TournamentsScreen(
                                    tournaments = tournaments,
                                    joinedMatches = joinedMatches,
                                    onSelectTournament = { selectedTournament = it }
                                )
                            }
                            composable("my_matches") {
                                MyMatchesScreen(
                                    joinedMatches = joinedMatches,
                                    tournaments = tournaments,
                                    roomSecretCache = roomSecretCache,
                                    leaderboard = leaderboard,
                                    onLoadRoomSecret = { arenaVm.loadRoomSecret(it) },
                                    onSelectTournament = { selectedTournament = it }
                                )
                            }
                            composable("wallet") {
                                WalletScreen(
                                    user = user,
                                    transactions = transactions,
                                    deposits = deposits,
                                    withdrawals = withdrawals,
                                    activeCoupons = activeCoupons,
                                    gatewayConfig = gatewayConfig,
                                    activeCheckoutOrder = activeCheckoutOrder,
                                    busy = busy,
                                    onValidatePromo = { code, amt -> arenaVm.previewDepositPromo(code, amt) },
                                    onCreateDeposit = { amt, provider, promo -> arenaVm.initiatePayZapiDeposit(amt, provider, promo) },
                                    onCheckPaymentNow = { arenaVm.checkDepositPaymentNow(it) },
                                    onOpenCheckout = { arenaVm.openExistingCheckout(it) },
                                    onCloseCheckout = { arenaVm.closeCheckoutModal() },
                                    onSubmitWithdrawal = { method, dest, amt, done -> arenaVm.submitWithdrawal(method, dest, amt, done) }
                                )
                            }
                            composable("profile") {
                                ProfileAndMoreScreen(
                                    user = user,
                                    staffRole = staffRole,
                                    generalConfig = generalConfig,
                                    isAdminAccess = isAdminAccess,
                                    busy = busy,
                                    onSaveProfile = { name, ign, gameUid, push -> arenaVm.updateProfile(name, ign, gameUid, push) },
                                    onSendTestPush = { arenaVm.sendTestNotification() },
                                    onNavigate = navigateTo,
                                    onOpenAdminCenter = { navigateTo("admin") }
                                )
                            }
                            composable("refer") {
                                ReferEarnScreen(
                                    user = user,
                                    referralConfig = referralConfig,
                                    referrals = myReferrals,
                                    onApplyReferralCode = { arenaVm.applyReferralCode(it) },
                                    onBack = { navController.popBackStack() }
                                )
                            }
                            composable("support") {
                                SupportDeskScreen(
                                    tickets = myTickets,
                                    selectedMessages = selectedTicketMessages,
                                    busy = busy,
                                    onObserveMessages = { arenaVm.observeTicketThread(it) },
                                    onCreateTicket = { cat, subj, msg, mId, rUid, shot, done ->
                                        arenaVm.createTicket(cat, subj, msg, mId, rUid, shot, done)
                                    },
                                    onSendReply = { id, txt -> arenaVm.sendTicketReply(id, txt) },
                                    onBack = { navController.popBackStack() }
                                )
                            }
                            composable("admin") {
                                if (isAdminAccess) {
                                    AdminCommandCenterScreen(
                                        isSuperAdmin = isSuperAdmin,
                                        staffRole = staffRole,
                                        arenaVm = arenaVm,
                                        adminVm = adminVm,
                                        onBack = { navController.popBackStack() }
                                    )
                                } else {
                                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        NeonPrimaryButton("Access Denied • Return Home", onClick = { navigateTo("home") })
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    selectedTournament?.let { match ->
        val freshMatch = tournaments.firstOrNull { it.id == match.id } ?: match
        val joinedSlot = joinedMatches.firstOrNull { it.matchId == freshMatch.id && it.status != "kicked" && it.status != "refunded" }
        TournamentDetailSheet(
            match = freshMatch,
            user = user,
            joinedSlot = joinedSlot,
            roomSecret = roomSecretCache[freshMatch.id],
            busy = busy,
            onLoadRoomSecret = { arenaVm.loadRoomSecret(it) },
            onValidateCoupon = { code, fee -> arenaVm.previewMatchCoupon(code, fee) },
            onJoinMatch = { m, ign, gUid, squad, coupon, onDone ->
                arenaVm.joinMatch(m, ign, gUid, squad, coupon, onDone)
            },
            onDismiss = { selectedTournament = null }
        )
    }
}

@Composable
private fun BannedOverlay(reason: String) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }

    Box(modifier = Modifier.fillMaxSize().background(ObsidianBg).padding(24.dp), contentAlignment = Alignment.Center) {
        CyberGlassCard(glowColor = CrimsonDanger, modifier = Modifier.widthIn(max = 460.dp)) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Block, contentDescription = null, tint = CrimsonDanger, modifier = Modifier.size(48.dp))
                Spacer(Modifier.height(10.dp))
                StatusPill("ACCOUNT SUSPENDED", CrimsonDanger)
                Spacer(Modifier.height(8.dp))
                Text("Access Restricted", style = MaterialTheme.typography.headlineSmall, color = TextPrimary)
                Spacer(Modifier.height(6.dp))
                Text(reason.ifBlank { "Your account has been suspended for fair-play violations." }, color = TextSecondary, textAlign = TextAlign.Center)
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = { AuthHelper.signOut(credentialManager, scope) },
                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonDanger, contentColor = TextPrimary)
                ) { Text("Sign Out") }
            }
        }
    }
}

@Composable
private fun MaintenanceOverlay(message: String, eta: String) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }

    Box(modifier = Modifier.fillMaxSize().background(ObsidianBg).padding(24.dp), contentAlignment = Alignment.Center) {
        CyberGlassCard(glowColor = AmberWarning, modifier = Modifier.widthIn(max = 460.dp)) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Build, contentDescription = null, tint = AmberWarning, modifier = Modifier.size(48.dp))
                Spacer(Modifier.height(10.dp))
                StatusPill("CYBER UPGRADE", AmberWarning)
                Spacer(Modifier.height(8.dp))
                Text("Maintenance Mode", style = MaterialTheme.typography.headlineSmall, color = TextPrimary)
                Spacer(Modifier.height(6.dp))
                Text(message, color = TextSecondary, textAlign = TextAlign.Center)
                if (eta.isNotBlank()) {
                    Spacer(Modifier.height(6.dp))
                    Text("Estimated return: $eta", style = MaterialTheme.typography.labelLarge, color = NeonCyan)
                }
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = { AuthHelper.signOut(credentialManager, scope) },
                    colors = ButtonDefaults.buttonColors(containerColor = ObsidianElevated, contentColor = TextPrimary)
                ) { Text("Sign Out") }
            }
        }
    }
}
