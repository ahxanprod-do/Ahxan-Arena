package com.example

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
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
import androidx.compose.material.icons.filled.CurrencyRupee
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.SentimentSatisfied
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import com.example.ui.screens.EditProfileScreen
import com.example.ui.screens.HomeLobbyScreen
import com.example.ui.screens.MyMatchesScreen
import com.example.ui.screens.ProfileAndMoreScreen
import com.example.ui.screens.ReferEarnScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.screens.SupportDeskScreen
import com.example.ui.screens.TournamentDetailSheet
import com.example.ui.screens.TournamentsScreen
import com.example.ui.screens.WalletScreen
import com.example.ui.screens.admin.AdminCommandCenterScreen
import com.example.ui.theme.AhxanArenaTheme
import com.example.ui.theme.ArenaCharcoal
import com.example.ui.theme.ArenaOrange
import com.example.ui.theme.ArenaPurple
import com.example.ui.theme.BottomNavBg
import com.example.ui.theme.CoinYellow
import com.example.ui.theme.CrimsonDanger
import com.example.ui.theme.HeaderNavy
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.viewmodel.AdminViewModel
import com.example.viewmodel.ArenaViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.delay

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
                var showSplash by rememberSaveable { mutableStateOf(true) }
                LaunchedEffect(Unit) {
                    delay(1600L)
                    showSplash = false
                }

                val authUser by remember { Firebase.auth.authStateFlow() }
                    .collectAsStateWithLifecycle(initialValue = Firebase.auth.currentUser)

                if (showSplash) {
                    SplashScreen()
                } else {
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
    val gameCategories by arenaVm.gameCategories.collectAsStateWithLifecycle()
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
    val participantsCache by arenaVm.participantsCache.collectAsStateWithLifecycle()

    val isSuperAdmin = email.equals("ahxantalib@gmail.com", ignoreCase = true) && emailVerified
    val isAdminAccess = isSuperAdmin || staffRole != null

    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: "home"
    var selectedTournament by remember { mutableStateOf<Tournament?>(null) }
    var showNotAdminDialog by remember { mutableStateOf(false) }
    var selectedMatchCategory by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedStatusTab by rememberSaveable { mutableIntStateOf(1) }

    val navigateTo: (String) -> Unit = remember(navController) {
        { route ->
            val target = route.lowercase().ifBlank { "home" }
            if (target in listOf("home", "refer", "tournaments", "wallet", "profile")) {
                navController.navigate(target) {
                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            } else if (target in listOf("my_matches", "edit_profile", "support", "admin")) {
                navController.navigate(target) { launchSingleTop = true }
            }
        }
    }

    LaunchedEffect(user?.userId, isAdminAccess) {
        val prefs = context.getSharedPreferences("ahxan_app", Context.MODE_PRIVATE)
        val loginMode = prefs.getString("pending_login_mode", "").orEmpty()
        if (loginMode == "admin") {
            if (isAdminAccess) {
                prefs.edit().remove("pending_login_mode").apply()
                navigateTo("admin")
            } else if (user != null) {
                prefs.edit().remove("pending_login_mode").apply()
                showNotAdminDialog = true
            }
        } else if (loginMode == "player" && user != null) {
            prefs.edit().remove("pending_login_mode").apply()
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

    // 5-Tab Bottom Bar matching reference UI: Home | Refer (₹) | Matches | Wallet | Me
    val tabs = remember {
        listOf(
            BottomNavTab("home", "Home", Icons.Default.Home),
            BottomNavTab("refer", "Refer", Icons.Default.CurrencyRupee),
            BottomNavTab("tournaments", "Matches", Icons.Default.EmojiEvents),
            BottomNavTab("wallet", "Wallet", Icons.Default.AccountBalanceWallet),
            BottomNavTab("profile", "Me", Icons.Default.SentimentSatisfied)
        )
    }
    val showMainNav = currentRoute in tabs.map { it.route }

    BoxWithConstraints(modifier = Modifier.fillMaxSize().background(ArenaCharcoal)) {
        val isWide = maxWidth >= 600.dp

        Scaffold(
            containerColor = ArenaCharcoal,
            contentWindowInsets = WindowInsets.safeDrawing,
            bottomBar = {
                if (showMainNav && !isWide) {
                    NavigationBar(containerColor = BottomNavBg) {
                        tabs.forEach { tab ->
                            val selected = currentRoute == tab.route
                            NavigationBarItem(
                                selected = selected,
                                onClick = {
                                    if (tab.route == "tournaments") {
                                        selectedMatchCategory = null
                                    }
                                    navigateTo(tab.route)
                                },
                                icon = { Icon(tab.icon, contentDescription = tab.label) },
                                label = { Text(tab.label, maxLines = 1) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = Color.White,
                                    selectedTextColor = Color.White,
                                    indicatorColor = Color(0xFF3E4552),
                                    unselectedIconColor = Color(0xFF94A3B8),
                                    unselectedTextColor = Color(0xFF94A3B8)
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
                    NavigationRail(containerColor = BottomNavBg, modifier = Modifier.fillMaxHeight()) {
                        Spacer(Modifier.height(16.dp))
                        tabs.forEach { tab ->
                            val selected = currentRoute == tab.route
                            NavigationRailItem(
                                selected = selected,
                                onClick = {
                                    if (tab.route == "tournaments") {
                                        selectedMatchCategory = null
                                    }
                                    navigateTo(tab.route)
                                },
                                icon = { Icon(tab.icon, contentDescription = tab.label) },
                                label = { Text(tab.label) },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = Color.White,
                                    selectedTextColor = Color.White,
                                    indicatorColor = Color(0xFF3E4552),
                                    unselectedIconColor = Color(0xFF94A3B8),
                                    unselectedTextColor = Color(0xFF94A3B8)
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
                                Text("MAINTENANCE MODE ACTIVE (ADMIN BYPASS)", style = MaterialTheme.typography.labelSmall, color = CoinYellow)
                                Text("Open Admin", style = MaterialTheme.typography.labelLarge, color = ArenaOrange)
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
                                    categories = gameCategories,
                                    notifications = userNotifications,
                                    broadcasts = broadcasts,
                                    onClaimCheckIn = { arenaVm.claimDailyCheckIn() },
                                    onNavigate = { route ->
                                        if (route.equals("tournaments", true)) {
                                            selectedMatchCategory = null
                                        }
                                        navigateTo(route)
                                    },
                                    onOpenGameTournaments = { gameKey ->
                                        selectedMatchCategory = gameKey
                                        selectedStatusTab = 1
                                        navigateTo("tournaments")
                                    },
                                    onOpenContestTab = { statusIdx ->
                                        selectedMatchCategory = "All"
                                        selectedStatusTab = statusIdx
                                        navigateTo("tournaments")
                                    },
                                    onSelectTournament = { selectedTournament = it },
                                    onMarkNotificationRead = { arenaVm.markNotificationRead(it) }
                                )
                            }
                            composable("tournaments") {
                                TournamentsScreen(
                                    tournaments = tournaments,
                                    joinedMatches = joinedMatches,
                                    generalConfig = generalConfig,
                                    categories = gameCategories,
                                    selectedCategory = selectedMatchCategory,
                                    initialStatusTab = selectedStatusTab,
                                    onSelectCategory = { catKey -> selectedMatchCategory = catKey },
                                    onClearCategory = { selectedMatchCategory = null },
                                    onOpenMyMatches = { navigateTo("my_matches") },
                                    onSelectTournament = { selectedTournament = it }
                                )
                            }
                            composable("my_matches") {
                                BackHandler { navController.popBackStack() }
                                MyMatchesScreen(
                                    joinedMatches = joinedMatches,
                                    tournaments = tournaments,
                                    roomSecretCache = roomSecretCache,
                                    leaderboard = leaderboard,
                                    onLoadRoomSecret = { arenaVm.loadRoomSecret(it) },
                                    onSelectTournament = { selectedTournament = it },
                                    onBack = { navController.popBackStack() }
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
                                    onSubmitWithdrawal = { method, dest, amt, done -> arenaVm.submitWithdrawal(method, dest, amt, done) },
                                    onBack = { navigateTo("home") }
                                )
                            }
                            composable("profile") {
                                ProfileAndMoreScreen(
                                    user = user,
                                    staffRole = staffRole,
                                    generalConfig = generalConfig,
                                    isAdminAccess = isAdminAccess,
                                    notifications = userNotifications,
                                    broadcasts = broadcasts,
                                    onSendTestPush = { arenaVm.sendTestNotification() },
                                    onTogglePush = { enabled ->
                                        val u = user
                                        if (u != null) {
                                            arenaVm.updateProfile(u.displayName, u.inGameName, u.gameUid, u.phoneNumber, enabled)
                                        }
                                    },
                                    onMarkNotificationRead = { arenaVm.markNotificationRead(it) },
                                    onNavigate = navigateTo,
                                    onOpenAdminCenter = { navigateTo("admin") }
                                )
                            }
                            composable("edit_profile") {
                                BackHandler { navController.popBackStack() }
                                EditProfileScreen(
                                    user = user,
                                    busy = busy,
                                    onSaveProfile = { name, ign, gameUid, phone, push ->
                                        arenaVm.updateProfile(name, ign, gameUid, phone, push)
                                    },
                                    onBack = { navController.popBackStack() }
                                )
                            }
                            composable("refer") {
                                ReferEarnScreen(
                                    user = user,
                                    referralConfig = referralConfig,
                                    referrals = myReferrals,
                                    onApplyReferralCode = { arenaVm.applyReferralCode(it) },
                                    onBack = { navigateTo("home") }
                                )
                            }
                            composable("support") {
                                BackHandler { navController.popBackStack() }
                                SupportDeskScreen(
                                    tickets = myTickets,
                                    selectedMessages = selectedTicketMessages,
                                    generalConfig = generalConfig,
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
                                BackHandler { navController.popBackStack() }
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
            participants = participantsCache[freshMatch.id].orEmpty(),
            busy = busy,
            onLoadRoomSecret = { arenaVm.loadRoomSecret(it) },
            onLoadParticipants = { arenaVm.loadMatchParticipants(it) },
            onOpenMyMatches = {
                selectedTournament = null
                navigateTo("my_matches")
            },
            onValidateCoupon = { code, fee -> arenaVm.previewMatchCoupon(code, fee) },
            onJoinMatch = { m, ign, gUid, whatsapp, squad, coupon, preferredSlot, onDone ->
                arenaVm.joinMatch(m, ign, gUid, whatsapp, squad, coupon, preferredSlot, onDone)
            },
            onDismiss = { selectedTournament = null }
        )
    }

    if (showNotAdminDialog) {
        val scope = rememberCoroutineScope()
        val credentialManager = remember { CredentialManager.create(context) }
        AlertDialog(
            onDismissRequest = { showNotAdminDialog = false },
            containerColor = HeaderNavy,
            title = { Text("Admin Access Required", color = CoinYellow) },
            text = {
                Text(
                    "The Google account ($email) is not authorized as an Admin or Staff member. Continue as a Player or Sign Out.",
                    color = TextSecondary
                )
            },
            confirmButton = {
                TextButton(onClick = { showNotAdminDialog = false }) {
                    Text("Continue as Player", color = ArenaOrange)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = {
                    showNotAdminDialog = false
                    AuthHelper.signOut(credentialManager, scope)
                }) {
                    Text("Sign Out", color = CrimsonDanger)
                }
            }
        )
    }
}

@Composable
private fun BannedOverlay(reason: String) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }

    Box(modifier = Modifier.fillMaxSize().background(ArenaCharcoal).padding(24.dp), contentAlignment = Alignment.Center) {
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
                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonDanger, contentColor = Color.White)
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

    Box(modifier = Modifier.fillMaxSize().background(ArenaCharcoal).padding(24.dp), contentAlignment = Alignment.Center) {
        CyberGlassCard(glowColor = ArenaOrange, modifier = Modifier.widthIn(max = 460.dp)) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Build, contentDescription = null, tint = ArenaOrange, modifier = Modifier.size(48.dp))
                Spacer(Modifier.height(10.dp))
                StatusPill("SYSTEM MAINTENANCE", CoinYellow)
                Spacer(Modifier.height(8.dp))
                Text("Maintenance Mode", style = MaterialTheme.typography.headlineSmall, color = TextPrimary)
                Spacer(Modifier.height(6.dp))
                Text(message, color = TextSecondary, textAlign = TextAlign.Center)
                if (eta.isNotBlank()) {
                    Spacer(Modifier.height(6.dp))
                    Text("Estimated return: $eta", style = MaterialTheme.typography.labelLarge, color = ArenaOrange)
                }
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = { AuthHelper.signOut(credentialManager, scope) },
                    colors = ButtonDefaults.buttonColors(containerColor = ArenaPurple, contentColor = Color.White)
                ) { Text("Sign Out") }
            }
        }
    }
}
