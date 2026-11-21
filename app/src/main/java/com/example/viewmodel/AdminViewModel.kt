package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.AuditLog
import com.example.data.model.BroadcastNotification
import com.example.data.model.CmsBanner
import com.example.data.model.Coupon
import com.example.data.model.DepositOrder
import com.example.data.model.GameCategory
import com.example.data.model.GatewayConfig
import com.example.data.model.GeneralConfig
import com.example.data.model.MatchSlot
import com.example.data.model.PushDevice
import com.example.data.model.ReferralConfig
import com.example.data.model.ReferralRecord
import com.example.data.model.StaffRole
import com.example.data.model.SupportTicket
import com.example.data.model.TicketMessage
import com.example.data.model.Tournament
import com.example.data.model.UserProfile
import com.example.data.model.WithdrawalRequest
import com.example.data.repository.ArenaAdminRepository
import com.example.data.repository.ArenaUserRepository
import com.example.data.repository.ArenaWalletRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AdminViewModel(
    private val adminRepo: ArenaAdminRepository,
    private val userRepo: ArenaUserRepository,
    private val walletRepo: ArenaWalletRepository
) : ViewModel() {

    val allUsers: StateFlow<List<UserProfile>> = adminRepo.observeAllUsers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val allDeposits: StateFlow<List<DepositOrder>> = adminRepo.observeAllDeposits()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val allWithdrawals: StateFlow<List<WithdrawalRequest>> = adminRepo.observeAllWithdrawals()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val allTickets: StateFlow<List<SupportTicket>> = adminRepo.observeAllTickets()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val allCoupons: StateFlow<List<Coupon>> = adminRepo.observeAllCoupons()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val allReferrals: StateFlow<List<ReferralRecord>> = adminRepo.observeAllReferrals()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val allBanners: StateFlow<List<CmsBanner>> = adminRepo.observeAllBanners()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val allCategories: StateFlow<List<GameCategory>> = adminRepo.observeAllCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val staffRoles: StateFlow<List<StaffRole>> = adminRepo.observeStaffRoles()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val auditLogs: StateFlow<List<AuditLog>> = adminRepo.observeAuditLogs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val pushDevices: StateFlow<List<PushDevice>> = adminRepo.observePushDevices()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val broadcasts: StateFlow<List<BroadcastNotification>> = userRepo.observeBroadcasts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    private val _selectedRoster = MutableStateFlow<List<MatchSlot>>(emptyList())
    val selectedRoster: StateFlow<List<MatchSlot>> = _selectedRoster.asStateFlow()

    private val _ticketMessages = MutableStateFlow<List<TicketMessage>>(emptyList())
    val ticketMessages: StateFlow<List<TicketMessage>> = _ticketMessages.asStateFlow()

    private val _adminBanner = MutableStateFlow<Pair<Boolean, String>?>(null)
    val adminBanner: StateFlow<Pair<Boolean, String>?> = _adminBanner.asStateFlow()

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    private var ticketThreadJob: Job? = null

    fun clearBanner() { _adminBanner.value = null }
    private fun msg(ok: Boolean, text: String) { _adminBanner.value = ok to text }

    fun sendBroadcast(title: String, body: String, imageUrl: String, route: String, audience: String, targetId: String) {
        viewModelScope.launch {
            _busy.value = true
            val devicesCount = pushDevices.value.count { it.enabled }
            val res = adminRepo.sendBroadcastNotification(title, body, imageUrl, route, audience, targetId, devicesCount)
            _busy.value = false
            res.onSuccess { msg(true, "Shutter Push Broadcast dispatched to $devicesCount devices!") }
                .onFailure { msg(false, it.localizedMessage ?: "Broadcast failed") }
        }
    }

    fun deleteBroadcast(b: BroadcastNotification) {
        viewModelScope.launch {
            _busy.value = true
            val res = adminRepo.deleteBroadcastNotification(b)
            _busy.value = false
            res.onSuccess { msg(true, "Notification '${b.title}' deleted!") }
                .onFailure { msg(false, it.localizedMessage ?: "Delete notification failed") }
        }
    }

    fun clearAllBroadcasts() {
        viewModelScope.launch {
            _busy.value = true
            val res = adminRepo.clearAllBroadcasts(broadcasts.value)
            _busy.value = false
            res.onSuccess { count -> msg(true, "Deleted $count notifications!") }
                .onFailure { msg(false, it.localizedMessage ?: "Clear notifications failed") }
        }
    }

    fun saveTournament(t: Tournament) {
        viewModelScope.launch {
            _busy.value = true
            val res = adminRepo.saveTournament(t)
            _busy.value = false
            res.onSuccess { msg(true, "Tournament saved!") }
                .onFailure { msg(false, it.localizedMessage ?: "Save failed") }
        }
    }

    fun cloneTournament(t: Tournament) {
        viewModelScope.launch {
            _busy.value = true
            val res = adminRepo.cloneTournament(t)
            _busy.value = false
            res.onSuccess { msg(true, "Cloned ${t.title}!") }
                .onFailure { msg(false, it.localizedMessage ?: "Clone failed") }
        }
    }

    fun deleteTournament(t: Tournament) {
        viewModelScope.launch {
            _busy.value = true
            val res = adminRepo.deleteTournament(t)
            _busy.value = false
            res.onSuccess { msg(true, "Match '${t.title}' deleted!") }
                .onFailure { msg(false, it.localizedMessage ?: "Delete failed") }
        }
    }

    fun publishRoom(match: Tournament, roomId: String, roomPass: String, region: String, autoPush: Boolean) {
        viewModelScope.launch {
            _busy.value = true
            val res = adminRepo.publishRoomCredentials(match, roomId, roomPass, region, autoPush)
            _busy.value = false
            res.onSuccess { msg(true, "Room ID & Password published!") }
                .onFailure { msg(false, it.localizedMessage ?: "Publish failed") }
        }
    }

    fun loadRoster(matchId: String) {
        viewModelScope.launch {
            val res = adminRepo.fetchMatchRoster(matchId)
            res.onSuccess { _selectedRoster.value = it }
                .onFailure { msg(false, it.localizedMessage ?: "Failed to load roster") }
        }
    }

    fun kickAndRefund(match: Tournament, slot: MatchSlot) {
        viewModelScope.launch {
            _busy.value = true
            val res = adminRepo.kickAndRefundSlot(match, slot)
            _busy.value = false
            res.onSuccess {
                msg(true, "Kicked & refunded ${slot.inGameName}")
                loadRoster(match.id)
            }.onFailure { msg(false, it.localizedMessage ?: "Kick failed") }
        }
    }

    fun cancelAndRefundAll(match: Tournament) {
        viewModelScope.launch {
            _busy.value = true
            val res = adminRepo.cancelMatchAndRefundAll(match)
            _busy.value = false
            res.onSuccess { count -> msg(true, "Match cancelled & refunded $count players") }
                .onFailure { msg(false, it.localizedMessage ?: "Cancel failed") }
        }
    }

    fun distributeResults(match: Tournament, slots: List<MatchSlot>) {
        viewModelScope.launch {
            _busy.value = true
            val res = adminRepo.distributeMatchResults(match, slots)
            _busy.value = false
            res.onSuccess { paid -> msg(true, "Distributed ₹${paid.toInt()} in prizes & updated Leaderboard!") }
                .onFailure { msg(false, it.localizedMessage ?: "Distribution failed") }
        }
    }

    fun saveCoupon(c: Coupon) {
        viewModelScope.launch {
            _busy.value = true
            val res = adminRepo.saveCoupon(c)
            _busy.value = false
            res.onSuccess { msg(true, "Coupon ${c.code.uppercase()} saved!") }
                .onFailure { msg(false, it.localizedMessage ?: "Failed to save coupon") }
        }
    }

    fun toggleCoupon(code: String, active: Boolean) {
        viewModelScope.launch { adminRepo.toggleCouponActive(code, active) }
    }

    fun deleteCoupon(code: String) {
        viewModelScope.launch { adminRepo.deleteCoupon(code) }
    }

    fun saveReferralConfig(cfg: ReferralConfig) {
        viewModelScope.launch {
            _busy.value = true
            val res = adminRepo.saveReferralConfig(cfg)
            _busy.value = false
            res.onSuccess { msg(true, "Referral config updated!") }
                .onFailure { msg(false, it.localizedMessage ?: "Update failed") }
        }
    }

    fun toggleBlockReferral(refId: String, block: Boolean) {
        viewModelScope.launch {
            adminRepo.toggleBlockReferral(refId, block)
                .onSuccess { msg(true, if (block) "Referral blocked" else "Referral unblocked") }
        }
    }

    fun observeTicketThread(ticketId: String) {
        ticketThreadJob?.cancel()
        ticketThreadJob = viewModelScope.launch {
            userRepo.observeTicketMessages(ticketId).collectLatest { _ticketMessages.value = it }
        }
    }

    fun replyTicket(ticket: SupportTicket, replyText: String, newStatus: String) {
        viewModelScope.launch {
            _busy.value = true
            val res = adminRepo.replyToTicketAsAdmin(ticket, replyText, newStatus)
            _busy.value = false
            res.onSuccess { msg(true, "Reply sent & user notified!") }
                .onFailure { msg(false, it.localizedMessage ?: "Reply failed") }
        }
    }

    fun verifyDepositWithPayZapi(dep: DepositOrder, gatewayConfig: GatewayConfig, referralConfig: ReferralConfig) {
        viewModelScope.launch {
            _busy.value = true
            val res = walletRepo.verifyPayZapiDeposit(dep, gatewayConfig, referralConfig)
            _busy.value = false
            res.onSuccess { msg(true, it) }
                .onFailure { msg(false, it.localizedMessage ?: "Verify failed") }
        }
    }

    fun manuallyApproveDeposit(dep: DepositOrder, utr: String, referralConfig: ReferralConfig) {
        viewModelScope.launch {
            _busy.value = true
            val res = adminRepo.manuallyApproveDeposit(dep, utr, referralConfig)
            _busy.value = false
            res.onSuccess { msg(true, it) }
                .onFailure { msg(false, it.localizedMessage ?: "Manual approve failed") }
        }
    }

    fun saveGatewayConfig(cfg: GatewayConfig) {
        viewModelScope.launch {
            _busy.value = true
            val res = adminRepo.saveGatewayConfig(cfg)
            _busy.value = false
            res.onSuccess { msg(true, "Gateway & Withdrawal rules saved!") }
                .onFailure { msg(false, it.localizedMessage ?: "Save failed") }
        }
    }

    fun approveWithdrawal(wd: WithdrawalRequest, utr: String) {
        viewModelScope.launch {
            _busy.value = true
            val res = adminRepo.approveWithdrawal(wd, utr)
            _busy.value = false
            res.onSuccess { msg(true, "Withdrawal approved & user notified!") }
                .onFailure { msg(false, it.localizedMessage ?: "Approval failed") }
        }
    }

    fun rejectWithdrawal(wd: WithdrawalRequest, reason: String) {
        viewModelScope.launch {
            _busy.value = true
            val res = adminRepo.rejectWithdrawalAndRefund(wd, reason)
            _busy.value = false
            res.onSuccess { msg(true, "Withdrawal rejected & refunded!") }
                .onFailure { msg(false, it.localizedMessage ?: "Reject failed") }
        }
    }

    fun adjustUserWallet(uid: String, bucket: String, isCredit: Boolean, amount: Double, reason: String) {
        viewModelScope.launch {
            _busy.value = true
            val res = adminRepo.adjustUserWallet(uid, bucket, isCredit, amount, reason)
            _busy.value = false
            res.onSuccess {
                msg(true, "${if (isCredit) "Added" else "Deducted"} ${amount.toInt()} Coins (${bucket.uppercase()}) & notified user!")
            }.onFailure { msg(false, it.localizedMessage ?: "Adjustment failed") }
        }
    }

    fun bulkAddCoins(users: List<UserProfile>, bucket: String, amount: Double, reason: String, sendNotif: Boolean) {
        viewModelScope.launch {
            _busy.value = true
            val res = adminRepo.bulkAdjustCoins(users, bucket, amount, reason, sendNotif)
            _busy.value = false
            res.onSuccess { count ->
                msg(true, "Airdropped ${amount.toInt()} Coins to $count active users!")
            }.onFailure { msg(false, it.localizedMessage ?: "Bulk drop failed") }
        }
    }

    fun updateUserDetails(uid: String, name: String, ign: String, gameUid: String, phone: String) {
        viewModelScope.launch {
            _busy.value = true
            val res = adminRepo.adminUpdateUserDetails(uid, name, ign, gameUid, phone)
            _busy.value = false
            res.onSuccess { msg(true, "Updated user profile details!") }
                .onFailure { msg(false, it.localizedMessage ?: "Update failed") }
        }
    }

    fun toggleBanUser(uid: String, ban: Boolean, reason: String) {
        viewModelScope.launch {
            _busy.value = true
            val res = adminRepo.toggleBanUser(uid, ban, reason)
            _busy.value = false
            res.onSuccess { msg(true, if (ban) "User banned" else "User unbanned") }
                .onFailure { msg(false, it.localizedMessage ?: "Action failed") }
        }
    }

    fun saveStaffRole(role: StaffRole) {
        viewModelScope.launch {
            _busy.value = true
            val res = adminRepo.saveStaffRole(role)
            _busy.value = false
            res.onSuccess { msg(true, "Staff role saved!") }
                .onFailure { msg(false, it.localizedMessage ?: "Failed to save role") }
        }
    }

    fun removeStaffRole(uid: String) {
        viewModelScope.launch { adminRepo.removeStaffRole(uid) }
    }

    fun saveGeneralConfig(cfg: GeneralConfig) {
        viewModelScope.launch {
            _busy.value = true
            val res = adminRepo.saveGeneralConfig(cfg)
            _busy.value = false
            res.onSuccess { msg(true, "CMS & Platform settings saved!") }
                .onFailure { msg(false, it.localizedMessage ?: "Save failed") }
        }
    }

    fun saveBanner(banner: CmsBanner) {
        viewModelScope.launch {
            adminRepo.saveBanner(banner)
                .onSuccess { msg(true, "Banner saved!") }
                .onFailure { msg(false, it.localizedMessage ?: "Banner save failed") }
        }
    }

    fun deleteBanner(id: String) {
        viewModelScope.launch { adminRepo.deleteBanner(id) }
    }

    fun saveCategory(cat: GameCategory) {
        viewModelScope.launch {
            _busy.value = true
            val res = adminRepo.saveGameCategory(cat)
            _busy.value = false
            res.onSuccess { msg(true, "Category '${cat.title}' saved!") }
                .onFailure { msg(false, it.localizedMessage ?: "Category save failed") }
        }
    }

    fun deleteCategory(id: String) {
        viewModelScope.launch {
            _busy.value = true
            val res = adminRepo.deleteGameCategory(id)
            _busy.value = false
            res.onSuccess { msg(true, "Category deleted!") }
                .onFailure { msg(false, it.localizedMessage ?: "Delete failed") }
        }
    }

    fun seedDefaultCategories() {
        viewModelScope.launch {
            _busy.value = true
            val res = adminRepo.seedDefaultCategories()
            _busy.value = false
            res.onSuccess { count -> msg(true, "Loaded $count default game categories!") }
                .onFailure { msg(false, it.localizedMessage ?: "Seed failed") }
        }
    }
}
