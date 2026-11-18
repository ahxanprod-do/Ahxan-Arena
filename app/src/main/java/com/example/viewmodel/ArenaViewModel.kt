package com.example.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.BroadcastNotification
import com.example.data.model.CmsBanner
import com.example.data.model.Coupon
import com.example.data.model.DepositOrder
import com.example.data.model.GatewayConfig
import com.example.data.model.GeneralConfig
import com.example.data.model.LeaderboardEntry
import com.example.data.model.MatchSlot
import com.example.data.model.ReferralConfig
import com.example.data.model.ReferralRecord
import com.example.data.model.RoomSecret
import com.example.data.model.StaffRole
import com.example.data.model.SupportTicket
import com.example.data.model.TicketMessage
import com.example.data.model.Tournament
import com.example.data.model.UserNotification
import com.example.data.model.UserProfile
import com.example.data.model.WalletTransaction
import com.example.data.model.WithdrawalRequest
import com.example.data.repository.ArenaAdminRepository
import com.example.data.repository.ArenaTournamentRepository
import com.example.data.repository.ArenaUserRepository
import com.example.data.repository.ArenaWalletRepository
import com.example.notifications.ArenaNotificationHelper
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ArenaViewModel(
    private val appContext: Context,
    private val currentUserId: String,
    private val userRepo: ArenaUserRepository,
    private val tournamentRepo: ArenaTournamentRepository,
    private val walletRepo: ArenaWalletRepository,
    private val adminRepo: ArenaAdminRepository
) : ViewModel() {

    val userProfile: StateFlow<UserProfile?> = userRepo.observeUserProfile(currentUserId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), null)

    val staffRole: StateFlow<StaffRole?> = userRepo.observeStaffRole(currentUserId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), null)

    val tournaments: StateFlow<List<Tournament>> = tournamentRepo.observeTournaments()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val joinedMatches: StateFlow<List<MatchSlot>> = tournamentRepo.observeMyJoinedMatches(currentUserId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val transactions: StateFlow<List<WalletTransaction>> = walletRepo.observeTransactions(currentUserId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val deposits: StateFlow<List<DepositOrder>> = walletRepo.observeMyDeposits(currentUserId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val withdrawals: StateFlow<List<WithdrawalRequest>> = walletRepo.observeMyWithdrawals(currentUserId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val activeCoupons: StateFlow<List<Coupon>> = walletRepo.observeActiveCoupons()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val banners: StateFlow<List<CmsBanner>> = userRepo.observeBanners()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val generalConfig: StateFlow<GeneralConfig> = userRepo.observeGeneralConfig()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), GeneralConfig())

    val gatewayConfig: StateFlow<GatewayConfig> = userRepo.observeGatewayConfig()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), GatewayConfig())

    val referralConfig: StateFlow<ReferralConfig> = userRepo.observeReferralConfig()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), ReferralConfig())

    val leaderboard: StateFlow<List<LeaderboardEntry>> = userRepo.observeLeaderboard()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val myReferrals: StateFlow<List<ReferralRecord>> = userRepo.observeMyReferrals(currentUserId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val myTickets: StateFlow<List<SupportTicket>> = userRepo.observeMyTickets(currentUserId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val userNotifications: StateFlow<List<UserNotification>> = userRepo.observeUserNotifications(currentUserId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val broadcasts: StateFlow<List<BroadcastNotification>> = userRepo.observeBroadcasts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    private val _selectedTicketMessages = MutableStateFlow<List<TicketMessage>>(emptyList())
    val selectedTicketMessages: StateFlow<List<TicketMessage>> = _selectedTicketMessages.asStateFlow()

    private val _activeCheckoutOrder = MutableStateFlow<DepositOrder?>(null)
    val activeCheckoutOrder: StateFlow<DepositOrder?> = _activeCheckoutOrder.asStateFlow()

    private val _statusBanner = MutableStateFlow<Pair<Boolean, String>?>(null)
    val statusBanner: StateFlow<Pair<Boolean, String>?> = _statusBanner.asStateFlow()

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    private val _roomSecretCache = MutableStateFlow<Map<String, RoomSecret>>(emptyMap())
    val roomSecretCache: StateFlow<Map<String, RoomSecret>> = _roomSecretCache.asStateFlow()

    private var pollingJob: Job? = null
    private var ticketMessagesJob: Job? = null

    private val prefs = appContext.getSharedPreferences("ahxan_push_seen_$currentUserId", Context.MODE_PRIVATE)

    init {
        viewModelScope.launch {
            val fbUser = Firebase.auth.currentUser
            if (fbUser != null) {
                val appPrefs = appContext.getSharedPreferences("ahxan_app", Context.MODE_PRIVATE)
                val pendingRef = appPrefs.getString("pending_ref_code", "").orEmpty()
                val pendingUsername = appPrefs.getString("pending_reg_username", "").orEmpty()
                val pendingIgn = appPrefs.getString("pending_reg_ign", "").orEmpty()
                val pendingGameUid = appPrefs.getString("pending_reg_uid", "").orEmpty()
                val pendingPhone = appPrefs.getString("pending_reg_phone", "").orEmpty()
                val profileRes = userRepo.ensureUserProfile(
                    firebaseUser = fbUser,
                    pendingReferralCode = pendingRef,
                    pendingUsername = pendingUsername,
                    pendingGameName = pendingIgn,
                    pendingGameUid = pendingGameUid,
                    pendingPhone = pendingPhone
                )
                if (profileRes.isSuccess) {
                    if (pendingRef.isNotBlank() && profileRes.getOrNull()?.referredByUid.isNullOrBlank()) {
                        val isCoupon = userRepo.checkPromoCouponExists(pendingRef)
                        if (isCoupon) {
                            val cleanCode = pendingRef.trim().uppercase()
                            appPrefs.edit().putString("pending_promo_code", cleanCode).apply()
                            showMsg(true, "Promo code $cleanCode saved! Apply it in Wallet or Match Join.")
                        }
                    }
                    appPrefs.edit()
                        .remove("pending_ref_code")
                        .remove("pending_reg_username")
                        .remove("pending_reg_ign")
                        .remove("pending_reg_uid")
                        .remove("pending_reg_phone")
                        .apply()
                }
                if (fbUser.email.equals("ahxantalib@gmail.com", ignoreCase = true) && fbUser.isEmailVerified) {
                    adminRepo.seedInitialDataIfEmpty()
                }
            }
        }
        observeShutterNotifications()
    }

    private fun observeShutterNotifications() {
        viewModelScope.launch {
            userNotifications.collectLatest { list ->
                val enabled = userProfile.value?.pushEnabled ?: true
                if (!enabled) return@collectLatest
                list.filter { !it.read }.take(3).forEach { notif ->
                    val key = "notif_${notif.id}"
                    if (notif.id.isNotBlank() && !prefs.getBoolean(key, false)) {
                        prefs.edit().putBoolean(key, true).apply()
                        ArenaNotificationHelper.showShutterNotification(
                            context = appContext,
                            notificationId = notif.id.hashCode(),
                            title = notif.title,
                            body = notif.body,
                            imageUrl = notif.imageUrl,
                            route = notif.route,
                            isMatchAlert = notif.category == "room"
                        )
                    }
                }
            }
        }
        viewModelScope.launch {
            broadcasts.collectLatest { list ->
                val profile = userProfile.value
                if (profile?.pushEnabled == false) return@collectLatest
                list.take(3).forEach { b ->
                    val key = "bcast_${b.id}"
                    if (b.id.isNotBlank() && !prefs.getBoolean(key, false) && matchesAudience(b, profile)) {
                        prefs.edit().putBoolean(key, true).apply()
                        ArenaNotificationHelper.showShutterNotification(
                            context = appContext,
                            notificationId = b.id.hashCode(),
                            title = b.title,
                            body = b.body,
                            imageUrl = b.imageUrl,
                            route = b.redirectRoute,
                            isMatchAlert = false
                        )
                    }
                }
            }
        }
    }

    private fun matchesAudience(b: BroadcastNotification, profile: UserProfile?): Boolean {
        if (profile == null) return b.audienceType == "all"
        return when (b.audienceType) {
            "all" -> true
            "user" -> b.audienceTargetId == profile.userId
            "depositors" -> deposits.value.any { it.status == "completed" }
            "zero_matches" -> profile.matchesPlayed == 0L
            "match" -> joinedMatches.value.any { it.matchId == b.audienceTargetId }
            else -> true
        }
    }

    fun clearBanner() {
        _statusBanner.value = null
    }

    private fun showMsg(ok: Boolean, msg: String) {
        _statusBanner.value = ok to msg
    }

    fun updateProfile(displayName: String, ign: String, gameUid: String, phoneNumber: String, pushEnabled: Boolean) {
        viewModelScope.launch {
            _busy.value = true
            val res = userRepo.updateProfile(displayName, ign, gameUid, phoneNumber, pushEnabled)
            _busy.value = false
            res.onSuccess { showMsg(true, "Profile updated successfully") }
                .onFailure { showMsg(false, it.localizedMessage ?: "Failed to update profile") }
        }
    }

    fun applyReferralCode(code: String) {
        viewModelScope.launch {
            _busy.value = true
            val res = userRepo.applyReferralCode(code)
            _busy.value = false
            res.onSuccess { showMsg(true, it) }
                .onFailure { showMsg(false, it.localizedMessage ?: "Invalid referral code") }
        }
    }

    fun claimDailyCheckIn() {
        viewModelScope.launch {
            _busy.value = true
            val res = userRepo.claimDailyCheckIn(generalConfig.value)
            _busy.value = false
            res.onSuccess { showMsg(true, "Claimed ₹$it Daily Check-In Bonus!") }
                .onFailure { showMsg(false, it.localizedMessage ?: "Check-in unavailable") }
        }
    }

    fun loadRoomSecret(matchId: String) {
        viewModelScope.launch {
            val res = tournamentRepo.fetchRoomSecretIfJoined(matchId)
            res.getOrNull()?.let { secret ->
                _roomSecretCache.value = _roomSecretCache.value + (matchId to secret)
            }
        }
    }

    suspend fun previewMatchCoupon(code: String, entryFee: Double): Result<Coupon> =
        tournamentRepo.validateMatchCoupon(code, entryFee)

    suspend fun previewDepositPromo(code: String, amount: Double): Result<Coupon> =
        walletRepo.validateDepositPromo(code, amount)

    fun joinMatch(
        match: Tournament,
        inGameName: String,
        gameUid: String,
        squadMembers: List<String>,
        couponCode: String,
        onJoined: () -> Unit
    ) {
        viewModelScope.launch {
            _busy.value = true
            val res = tournamentRepo.atomicJoinMatch(
                match = match,
                inGameName = inGameName,
                gameUid = gameUid,
                squadMembers = squadMembers,
                couponCodeRaw = couponCode,
                bonusUsablePercent = generalConfig.value.bonusUsablePercent
            )
            _busy.value = false
            res.onSuccess { slot ->
                showMsg(true, "Joined ${match.title} • Slot #${slot.slotNumber}")
                onJoined()
            }.onFailure {
                showMsg(false, it.localizedMessage ?: "Could not join match")
            }
        }
    }

    fun initiatePayZapiDeposit(amount: Double, provider: String, promoCode: String) {
        val profile = userProfile.value ?: return
        viewModelScope.launch {
            _busy.value = true
            val res = walletRepo.createPayZapiDeposit(
                user = profile,
                amount = amount,
                provider = provider,
                promoCodeRaw = promoCode,
                gatewayConfig = gatewayConfig.value
            )
            _busy.value = false
            res.onSuccess { order ->
                _activeCheckoutOrder.value = order
                startCheckoutAutoPolling(order)
            }.onFailure {
                showMsg(false, it.localizedMessage ?: "PayZapi order failed")
            }
        }
    }

    fun openExistingCheckout(order: DepositOrder) {
        _activeCheckoutOrder.value = order
        startCheckoutAutoPolling(order)
    }

    fun closeCheckoutModal() {
        pollingJob?.cancel()
        pollingJob = null
        _activeCheckoutOrder.value = null
    }

    private fun startCheckoutAutoPolling(order: DepositOrder) {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            while (true) {
                delay(6000L)
                val current = _activeCheckoutOrder.value ?: break
                if (current.status != "pending") break
                val res = walletRepo.verifyPayZapiDeposit(current, gatewayConfig.value, referralConfig.value)
                if (res.isSuccess) {
                    showMsg(true, res.getOrThrow())
                    closeCheckoutModal()
                    break
                }
            }
        }
    }

    fun checkDepositPaymentNow(order: DepositOrder) {
        viewModelScope.launch {
            _busy.value = true
            val res = walletRepo.verifyPayZapiDeposit(order, gatewayConfig.value, referralConfig.value)
            _busy.value = false
            res.onSuccess { msg ->
                showMsg(true, msg)
                if (_activeCheckoutOrder.value?.id == order.id) {
                    closeCheckoutModal()
                }
            }.onFailure {
                showMsg(false, it.localizedMessage ?: "Payment still pending")
            }
        }
    }

    fun verifyOrderFromDeepLink(clientOrderId: String) {
        viewModelScope.launch {
            val match = deposits.value.firstOrNull { it.clientOrderId == clientOrderId || it.id == clientOrderId }
            if (match != null && match.status == "pending") {
                checkDepositPaymentNow(match)
            }
        }
    }

    fun submitWithdrawal(method: String, destination: String, amount: Double, onDone: () -> Unit) {
        val profile = userProfile.value ?: return
        viewModelScope.launch {
            _busy.value = true
            val res = walletRepo.requestWithdrawal(profile, method, destination, amount, gatewayConfig.value)
            _busy.value = false
            res.onSuccess {
                showMsg(true, "Withdrawal request submitted!")
                onDone()
            }.onFailure {
                showMsg(false, it.localizedMessage ?: "Withdrawal failed")
            }
        }
    }

    fun createTicket(
        category: String,
        subject: String,
        message: String,
        matchId: String,
        reportedUid: String,
        screenshotBase64: String,
        onCreated: () -> Unit
    ) {
        val profile = userProfile.value ?: return
        viewModelScope.launch {
            _busy.value = true
            val res = userRepo.createSupportTicket(profile, category, subject, message, matchId, reportedUid, screenshotBase64)
            _busy.value = false
            res.onSuccess {
                showMsg(true, "Support ticket opened!")
                onCreated()
            }.onFailure {
                showMsg(false, it.localizedMessage ?: "Failed to create ticket")
            }
        }
    }

    fun observeTicketThread(ticketId: String) {
        ticketMessagesJob?.cancel()
        ticketMessagesJob = viewModelScope.launch {
            userRepo.observeTicketMessages(ticketId).collectLatest {
                _selectedTicketMessages.value = it
            }
        }
    }

    fun sendTicketReply(ticketId: String, text: String) {
        val name = userProfile.value?.displayName ?: "Player"
        viewModelScope.launch {
            val res = userRepo.sendTicketReply(ticketId, name, text)
            res.onFailure { showMsg(false, it.localizedMessage ?: "Reply failed") }
        }
    }

    fun markNotificationRead(id: String) {
        viewModelScope.launch { userRepo.markNotificationRead(id) }
    }

    fun sendTestNotification() {
        viewModelScope.launch {
            ArenaNotificationHelper.sendTestShutterNotification(appContext)
            showMsg(true, "Test alert sent to your phone's notification shutter!")
        }
    }
}
