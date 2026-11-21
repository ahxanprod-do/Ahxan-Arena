package com.example.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

@Keep
@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val email: String = "",
    val displayName: String = "",
    val photoUrl: String? = null,
    val inGameName: String = "",
    val gameUid: String = "",
    val phoneNumber: String = "",
    val depositBalance: Double = 0.0,
    val winningBalance: Double = 0.0,
    val bonusBalance: Double = 0.0,
    val totalWinnings: Double = 0.0,
    val totalKills: Long = 0L,
    val matchesPlayed: Long = 0L,
    val referralCode: String = "",
    val referredByCode: String = "",
    val referredByUid: String = "",
    val referralQualified: Boolean = false,
    val referralCount: Long = 0L,
    val referralEarnings: Double = 0.0,
    val streakDays: Long = 0L,
    val lastCheckInDay: String = "",
    val role: String = "user",
    val banned: Boolean = false,
    val banReason: String = "",
    val pushEnabled: Boolean = true,
    val deviceLabel: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val totalBalance: Double get() = depositBalance + winningBalance + bonusBalance
}

@Keep
@IgnoreExtraProperties
data class LeaderboardEntry(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val photoUrl: String? = null,
    val totalWinnings: Double = 0.0,
    val totalKills: Long = 0L,
    val matchesPlayed: Long = 0L,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class Tournament(
    @DocumentId val id: String = "",
    val title: String = "",
    val game: String = "BGMI",
    val mode: String = "Squad",
    val mapName: String = "Erangel",
    val entryType: String = "Paid",
    val entryFee: Double = 0.0,
    val prizePool: Double = 0.0,
    val perKillReward: Double = 0.0,
    val maxSlots: Long = 100L,
    val filledSlots: Long = 0L,
    val status: String = "upcoming",
    val scheduledAtMillis: Long = 0L,
    val registrationOpenAtMillis: Long = 0L,
    val registrationCloseAtMillis: Long = 0L,
    val bannerKey: String = "bgmi",
    val bannerUrl: String = "",
    val rulesText: String = "",
    val prizeBreakdown: String = "",
    val roomPublished: Boolean = false,
    val createdBy: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun isRegistrationNotStarted(nowMillis: Long = System.currentTimeMillis()): Boolean =
        registrationOpenAtMillis > 0L && nowMillis < registrationOpenAtMillis

    fun isRegistrationClosed(nowMillis: Long = System.currentTimeMillis()): Boolean =
        registrationCloseAtMillis > 0L && nowMillis >= registrationCloseAtMillis

    fun isRegistrationOpen(nowMillis: Long = System.currentTimeMillis()): Boolean =
        status == "upcoming" &&
            filledSlots < maxSlots &&
            !isRegistrationNotStarted(nowMillis) &&
            !isRegistrationClosed(nowMillis)
}

@Keep
@IgnoreExtraProperties
data class RoomSecret(
    @DocumentId val id: String = "",
    val roomId: String = "",
    val roomPassword: String = "",
    val serverRegion: String = "Asia / India",
    val publishedAtMillis: Long = 0L,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class MatchSlot(
    @DocumentId val id: String = "",
    val userId: String = "",
    val matchId: String = "",
    val matchTitle: String = "",
    val displayName: String = "",
    val inGameName: String = "",
    val gameUid: String = "",
    val whatsappNumber: String = "",
    val squadMembers: List<String> = emptyList(),
    val slotNumber: Long = 1L,
    val entryPaid: Double = 0.0,
    val depositUsed: Double = 0.0,
    val winningUsed: Double = 0.0,
    val bonusUsed: Double = 0.0,
    val couponCodeUsed: String = "",
    val discountAmount: Double = 0.0,
    val kills: Long = 0L,
    val rank: Long = 0L,
    val prizeWon: Double = 0.0,
    val status: String = "joined",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class WalletTransaction(
    @DocumentId val id: String = "",
    val userId: String = "",
    val type: String = "",
    val amount: Double = 0.0,
    val direction: String = "credit",
    val walletBucket: String = "deposit",
    val title: String = "",
    val subtitle: String = "",
    val referenceId: String = "",
    val status: String = "completed",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class DepositOrder(
    @DocumentId val id: String = "",
    val userId: String = "",
    val userEmail: String = "",
    val userName: String = "",
    val clientOrderId: String = "",
    val payzapiOrderId: String = "",
    val amount: Double = 0.0,
    val provider: String = "fampay",
    val promoCode: String = "",
    val expectedPromoBonus: Double = 0.0,
    val promoBonusCredited: Double = 0.0,
    val status: String = "pending",
    val utr: String = "",
    val qrCodeUrl: String = "",
    val upiIntentUrl: String = "",
    val checkoutUrl: String = "",
    val expiryMillis: Long = 0L,
    val paidAtMillis: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class WithdrawalRequest(
    @DocumentId val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val userEmail: String = "",
    val method: String = "UPI",
    val destination: String = "",
    val grossAmount: Double = 0.0,
    val feeAmount: Double = 0.0,
    val netAmount: Double = 0.0,
    val status: String = "pending",
    val utrOrTxHash: String = "",
    val rejectReason: String = "",
    val processedBy: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class Coupon(
    @DocumentId val id: String = "",
    val code: String = "",
    val couponType: String = "deposit_bonus",
    val valueType: String = "percentage",
    val value: Double = 0.0,
    val minAmount: Double = 0.0,
    val maxCap: Double = 0.0,
    val maxGlobalUses: Long = 100L,
    val maxPerUserUses: Long = 1L,
    val usedCount: Long = 0L,
    val audience: String = "all",
    val active: Boolean = true,
    val expiresAtMillis: Long = 0L,
    val totalDiscountGiven: Double = 0.0,
    val description: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun computeBenefit(baseAmount: Double): Double {
        if (!active || baseAmount < minAmount || usedCount >= maxGlobalUses) return 0.0
        if (expiresAtMillis > 0L && System.currentTimeMillis() > expiresAtMillis) return 0.0
        val raw = if (valueType == "percentage") (baseAmount * value / 100.0) else value
        return if (maxCap > 0.0) minOf(raw, maxCap) else raw
    }
}

@Keep
@IgnoreExtraProperties
data class ReferralRecord(
    @DocumentId val id: String = "",
    val referrerUid: String = "",
    val referrerCode: String = "",
    val referredUid: String = "",
    val referredName: String = "",
    val deviceFingerprint: String = "",
    val triggerType: String = "first_deposit",
    val rewardAmount: Double = 0.0,
    val friendBonus: Double = 0.0,
    val status: String = "pending",
    val flaggedFraud: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class SupportTicket(
    @DocumentId val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val userEmail: String = "",
    val category: String = "Payment Issue",
    val subject: String = "",
    val matchId: String = "",
    val reportedPlayerUid: String = "",
    val screenshotBase64: String = "",
    val status: String = "open",
    val lastMessage: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class TicketMessage(
    @DocumentId val id: String = "",
    val ticketId: String = "",
    val senderUid: String = "",
    val senderName: String = "",
    val senderRole: String = "user",
    val text: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class UserNotification(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val body: String = "",
    val imageUrl: String = "",
    val route: String = "home",
    val category: String = "general",
    val read: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class BroadcastNotification(
    @DocumentId val id: String = "",
    val title: String = "",
    val body: String = "",
    val imageUrl: String = "",
    val redirectRoute: String = "tournaments",
    val priority: String = "high",
    val audienceType: String = "all",
    val audienceTargetId: String = "",
    val sentByEmail: String = "",
    val deliveredCount: Long = 0L,
    val failedCount: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class CmsBanner(
    @DocumentId val id: String = "",
    val title: String = "",
    val subtitle: String = "",
    val imageUrl: String = "",
    val drawableKey: String = "bgmi",
    val redirectRoute: String = "tournaments",
    val active: Boolean = true,
    val sortOrder: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class GameCategory(
    @DocumentId val id: String = "",
    val title: String = "",
    val filterKey: String = "",
    val imageUrl: String = "",
    val drawableKey: String = "bgmi",
    val sortOrder: Long = 0L,
    val active: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class GeneralConfig(
    @DocumentId val id: String = "",
    val tickerText: String = "WELCOME TO AHXAN ARENA • LIVE BGMI & FREE FIRE SCRIMS • INSTANT PAYZAPI DEPOSITS & WITHDRAWALS",
    val maintenanceMode: Boolean = false,
    val maintenanceMessage: String = "Arena servers are undergoing scheduled maintenance.",
    val maintenanceEta: String = "15 mins",
    val rulesText: String = "1. Emulators & third-party hacks result in instant permanent ban.\n2. Join room 10 minutes before match start.\n3. Screenshots required for dispute resolution.",
    val termsText: String = "Fair play is mandatory. Hacking, teaming, or emulator spoofing leads to permanent account suspension. All prizes are credited to Winning Wallet after match verification.",
    val privacyText: String = "We store only your profile identity (Name, IGN, Game UID, Phone, Email) and tournament ledger securely in Google Firebase Firestore. We never sell player information to third parties.",
    val faqText: String = "1. How do I join a match?\nAdd coins via In-App QR in My Wallet, pick a game category & match, choose your slot number, and confirm your IGN & UID.\n\n2. When do I get Room ID & Password?\n15 minutes before match start inside Match Details and My Matches.\n\n3. How are winnings redeemed?\nGo to Wallet > Redeem Coins via UPI or Binance BEP-20 USDT.",
    val aboutUsText: String = "Ahxan Arena is a competitive esports tournament platform hosting daily BGMI, Free Fire Max, CODM, and Valorant scrims with verified slot management and instant wallet payouts.",
    val shareMessage: String = "Join me on Ahxan Arena Esports Tournaments! Use my referral code {CODE} to get bonus coins.",
    val supportHours: String = "Support Desk open 10:00 AM – 11:00 PM IST daily • Average reply under 15 mins.",
    val appTagline: String = "BGMI • Free Fire MAX • COD Mobile • Valorant",
    val whatsappUrl: String = "https://wa.me/",
    val telegramUrl: String = "https://t.me/",
    val discordUrl: String = "https://discord.com/",
    val instagramUrl: String = "https://instagram.com/",
    val checkinEnabled: Boolean = true,
    val checkinRewards: List<Long> = listOf(5L, 8L, 10L, 12L, 15L, 20L, 50L),
    val bonusUsablePercent: Long = 10L,
    val matchesHeroBannerUrl: String = "",
    val matchesHeroBannerKey: String = "bgmi",
    val matchesHeroTitle: String = "AHXAN ARENA CHAMPIONSHIPS",
    val matchesHeroSubtitle: String = "Daily Competitive Scrims • Instant PayZapi Payouts",
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class GatewayConfig(
    @DocumentId val id: String = "",
    val payzapiApiKeyOverride: String = "",
    val fampayEnabled: Boolean = true,
    val paytmEnabled: Boolean = true,
    val bharatpeEnabled: Boolean = true,
    val binanceEnabled: Boolean = true,
    val minDeposit: Double = 10.0,
    val maxDeposit: Double = 50000.0,
    val depositMaintenance: Boolean = false,
    val minWithdrawal: Double = 50.0,
    val maxWithdrawal: Double = 25000.0,
    val withdrawalFeePercent: Double = 2.0,
    val dailyWithdrawalLimit: Double = 50000.0,
    val payoutsPaused: Boolean = false,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class ReferralConfig(
    @DocumentId val id: String = "",
    val enabled: Boolean = true,
    val referrerRewardType: String = "flat",
    val referrerRewardValue: Double = 25.0,
    val friendSignupBonus: Double = 15.0,
    val triggerRule: String = "first_deposit",
    val minDepositThreshold: Double = 50.0,
    val websiteBaseUrl: String = "",
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun buildInviteLink(referralCode: String): String {
        val code = referralCode.trim().ifBlank { "AHXAN" }
        val raw = websiteBaseUrl.trim()
        if (raw.isEmpty()) return "ahxanarena://register?ref=$code"
        val normalized = if (raw.contains("://")) raw else "https://$raw"
        return when {
            normalized.contains("{CODE}", ignoreCase = true) ->
                normalized.replace("{CODE}", code, ignoreCase = true)
            normalized.contains("?") ->
                if (normalized.endsWith("?") || normalized.endsWith("&")) "${normalized}ref=$code" else "${normalized}&ref=$code"
            else ->
                "${normalized}?ref=$code"
        }
    }
}

@Keep
@IgnoreExtraProperties
data class StaffRole(
    @DocumentId val id: String = "",
    val userId: String = "",
    val email: String = "",
    val name: String = "",
    val roleLabel: String = "Moderator",
    val manageMatches: Boolean = false,
    val publishRooms: Boolean = false,
    val verifyDeposits: Boolean = false,
    val approveWithdrawals: Boolean = false,
    val manageCoupons: Boolean = false,
    val replyTickets: Boolean = false,
    val sendPush: Boolean = false,
    val manageUsers: Boolean = false,
    val systemSettings: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun hasAnyPermission(): Boolean =
        manageMatches || publishRooms || verifyDeposits || approveWithdrawals ||
            manageCoupons || replyTickets || sendPush || manageUsers || systemSettings

    fun grantedPermissionLabels(): List<String> = buildList {
        if (manageMatches) add("Manage Matches")
        if (publishRooms) add("Publish Rooms")
        if (verifyDeposits) add("Verify Deposits")
        if (approveWithdrawals) add("Approve Payouts")
        if (manageCoupons) add("Manage Coupons")
        if (replyTickets) add("Reply Tickets")
        if (sendPush) add("Push Studio")
        if (manageUsers) add("Users & Coins")
        if (systemSettings) add("CMS & Settings")
    }
}

@Keep
@IgnoreExtraProperties
data class AppUpdateConfig(
    @DocumentId val id: String = "",
    val forceUpdateEnabled: Boolean = false,
    val latestVersionCode: Long = 1L,
    val latestVersionName: String = "1.0",
    val updateDownloadUrl: String = "",
    val releaseNotes: String = "",
    val updatedBy: String = "",
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun normalizedDownloadUrl(): String {
        val raw = updateDownloadUrl.trim()
        if (raw.isEmpty()) return ""
        return if (raw.contains("://")) raw else "https://$raw"
    }

    fun isUpdateRequired(currentVersionCode: Int): Boolean =
        forceUpdateEnabled &&
            updateDownloadUrl.trim().isNotBlank() &&
            latestVersionCode > currentVersionCode.toLong()
}

@Keep
@IgnoreExtraProperties
data class AuditLog(
    @DocumentId val id: String = "",
    val adminUid: String = "",
    val adminEmail: String = "",
    val actionType: String = "",
    val targetId: String = "",
    val summary: String = "",
    val oldValue: String = "",
    val newValue: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class PushDevice(
    @DocumentId val id: String = "",
    val userId: String = "",
    val deviceName: String = "",
    val platform: String = "android",
    val enabled: Boolean = true,
    @ServerTimestamp val updatedAt: Timestamp? = null
)
