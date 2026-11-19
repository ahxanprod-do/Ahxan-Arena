package com.example.data.repository

import android.os.Build
import com.example.data.model.BroadcastNotification
import com.example.data.model.CmsBanner
import com.example.data.model.GameCategory
import com.example.data.model.GatewayConfig
import com.example.data.model.GeneralConfig
import com.example.data.model.LeaderboardEntry
import com.example.data.model.OperationType
import com.example.data.model.ReferralConfig
import com.example.data.model.ReferralRecord
import com.example.data.model.StaffRole
import com.example.data.model.SupportTicket
import com.example.data.model.TicketMessage
import com.example.data.model.UserNotification
import com.example.data.model.UserProfile
import com.example.data.model.handleFirestoreError
import com.example.utils.ArenaFormatters
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

class ArenaUserRepository(private val db: FirebaseFirestore) {
    private val auth = Firebase.auth

    private fun requireUserId(): String =
        auth.currentUser?.uid ?: throw IllegalStateException("User must be signed in.")

    private fun sanitizeId(raw: String): String =
        raw.replace(Regex("[^a-zA-Z0-9_\\-]"), "").take(48).ifBlank { "ID_${System.currentTimeMillis()}" }

    suspend fun ensureUserProfile(
        firebaseUser: FirebaseUser,
        pendingReferralCode: String = "",
        pendingUsername: String = "",
        pendingGameName: String = "",
        pendingGameUid: String = "",
        pendingPhone: String = ""
    ): Result<UserProfile> {
        val uid = firebaseUser.uid
        val docPath = "users/$uid"
        return try {
            val userRef = db.collection("users").document(uid)
            val snap = userRef.get().await()
            if (snap.exists()) {
                val existing = snap.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                if (existing != null) {
                    if (pendingUsername.isNotBlank() || pendingGameName.isNotBlank() || pendingGameUid.isNotBlank() || pendingPhone.isNotBlank()) {
                        userRef.update(
                            mapOf(
                                "displayName" to pendingUsername.ifBlank { existing.displayName }.take(80),
                                "inGameName" to pendingGameName.ifBlank { existing.inGameName }.take(60),
                                "gameUid" to pendingGameUid.ifBlank { existing.gameUid }.take(60),
                                "phoneNumber" to pendingPhone.ifBlank { existing.phoneNumber }.take(30),
                                "updatedAt" to FieldValue.serverTimestamp()
                            )
                        ).await()
                    }
                    if (pendingReferralCode.isNotBlank() && existing.referredByUid.isBlank()) {
                        applyReferralCode(pendingReferralCode)
                    }
                    registerPushDevice(uid, existing.pushEnabled)
                    return Result.success(existing)
                }
            }

            val cleanName = pendingUsername.trim().ifBlank {
                firebaseUser.displayName?.take(80)?.ifBlank { "Arena Player" } ?: "Arena Player"
            }.take(80)
            val cleanIgn = pendingGameName.trim().ifBlank { cleanName.take(24) }.take(60)
            val cleanGameUid = pendingGameUid.trim().take(60)
            val cleanPhone = pendingPhone.trim().take(30)
            val email = firebaseUser.email.orEmpty()
            val photoUrl = firebaseUser.photoUrl?.toString()?.take(500)
            val generatedCode = ("AX" + uid.take(4) + UUID.randomUUID().toString().take(3)).uppercase()
                .replace(Regex("[^A-Z0-9]"), "X")
            val isSuperAdmin = email.equals("ahxantalib@gmail.com", ignoreCase = true) && firebaseUser.isEmailVerified
            val role = if (isSuperAdmin) "admin" else "user"
            val deviceLabel = "${Build.MANUFACTURER} ${Build.MODEL}".take(100)

            val userPayload = mapOf(
                "userId" to uid,
                "email" to email,
                "displayName" to cleanName,
                "photoUrl" to photoUrl,
                "inGameName" to cleanIgn,
                "gameUid" to cleanGameUid,
                "phoneNumber" to cleanPhone,
                "depositBalance" to 0.0,
                "winningBalance" to 0.0,
                "bonusBalance" to 0.0,
                "totalWinnings" to 0.0,
                "totalKills" to 0L,
                "matchesPlayed" to 0L,
                "referralCode" to generatedCode,
                "referredByCode" to "",
                "referredByUid" to "",
                "referralQualified" to false,
                "referralCount" to 0L,
                "referralEarnings" to 0.0,
                "streakDays" to 0L,
                "lastCheckInDay" to "",
                "role" to role,
                "banned" to false,
                "banReason" to "",
                "pushEnabled" to true,
                "deviceLabel" to deviceLabel,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null } as Map<String, Any>

            val lbPayload = mapOf(
                "userId" to uid,
                "displayName" to cleanName,
                "photoUrl" to photoUrl,
                "totalWinnings" to 0.0,
                "totalKills" to 0L,
                "matchesPlayed" to 0L,
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null } as Map<String, Any>

            val refCodePayload = mapOf(
                "code" to generatedCode,
                "ownerUid" to uid,
                "ownerName" to cleanName,
                "createdAt" to FieldValue.serverTimestamp()
            )

            val batch = db.batch()
            batch.set(userRef, userPayload)
            batch.set(db.collection("leaderboard").document(uid), lbPayload)
            batch.set(db.collection("referral_codes").document(generatedCode), refCodePayload)
            batch.commit().await()

            registerPushDevice(uid, true)
            if (pendingReferralCode.isNotBlank()) {
                applyReferralCode(pendingReferralCode)
            }

            val created = userRef.get().await()
                .toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                ?: UserProfile(
                    userId = uid,
                    email = email,
                    displayName = cleanName,
                    inGameName = cleanIgn,
                    gameUid = cleanGameUid,
                    phoneNumber = cleanPhone,
                    referralCode = generatedCode,
                    role = role
                )
            Result.success(created)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    private suspend fun registerPushDevice(uid: String, enabled: Boolean) {
        val deviceId = sanitizeId("${uid}_android_${Build.MODEL}")
        runCatching {
            db.collection("push_devices").document(deviceId).set(
                mapOf(
                    "userId" to uid,
                    "deviceName" to "${Build.MANUFACTURER} ${Build.MODEL}".take(100),
                    "platform" to "android",
                    "enabled" to enabled,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        }
    }

    fun observeUserProfile(uid: String): Flow<UserProfile?> =
        db.collection("users").document(uid)
            .snapshots()
            .map { snap ->
                if (!snap.exists()) null
                else snap.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }
            .catch { error ->
                val fe = generateSequence(error) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
                if (fe == null && error is CancellationException) throw error
                if (auth.currentUser?.uid == uid) {
                    handleFirestoreError(fe ?: error, OperationType.GET, "users/$uid")
                }
                throw error
            }

    fun observeStaffRole(uid: String): Flow<StaffRole?> =
        db.collection("staff_roles").document(uid)
            .snapshots()
            .map { snap ->
                if (snap.exists()) snap.toObject(StaffRole::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                else null
            }
            .catch { error ->
                if (error is CancellationException && generateSequence<Throwable>(error) { it.cause }.none { it is FirebaseFirestoreException }) {
                    throw error
                }
                emit(null)
            }

    fun observeLeaderboard(): Flow<List<LeaderboardEntry>> =
        db.collection("leaderboard")
            .orderBy("totalWinnings", Query.Direction.DESCENDING)
            .limit(50)
            .snapshots()
            .map { snap -> snap.toObjects(LeaderboardEntry::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
            .catch { error ->
                val fe = generateSequence(error) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
                if (fe == null && error is CancellationException) throw error
                handleFirestoreError(fe ?: error, OperationType.LIST, "leaderboard")
                throw error
            }

    fun observeBanners(): Flow<List<CmsBanner>> =
        db.collection("cms_banners")
            .snapshots()
            .map { snap ->
                snap.toObjects(CmsBanner::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .filter { it.active }
                    .sortedBy { it.sortOrder }
            }
            .catch { error ->
                val fe = generateSequence(error) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
                if (fe == null && error is CancellationException) throw error
                handleFirestoreError(fe ?: error, OperationType.LIST, "cms_banners")
                emit(emptyList())
            }

    fun observeGameCategories(): Flow<List<GameCategory>> =
        db.collection("game_categories")
            .snapshots()
            .map { snap ->
                snap.toObjects(GameCategory::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .filter { it.active }
                    .sortedBy { it.sortOrder }
            }
            .catch { error ->
                val fe = generateSequence(error) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
                if (fe == null && error is CancellationException) throw error
                handleFirestoreError(fe ?: error, OperationType.LIST, "game_categories")
                emit(emptyList())
            }

    fun observeGeneralConfig(): Flow<GeneralConfig> =
        db.collection("app_config").document("general")
            .snapshots()
            .map { snap ->
                snap.toObject(GeneralConfig::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    ?: GeneralConfig()
            }
            .catch { error ->
                if (error is CancellationException && generateSequence<Throwable>(error) { it.cause }.none { it is FirebaseFirestoreException }) {
                    throw error
                }
                emit(GeneralConfig())
            }

    fun observeGatewayConfig(): Flow<GatewayConfig> =
        db.collection("app_config").document("gateway")
            .snapshots()
            .map { snap ->
                snap.toObject(GatewayConfig::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    ?: GatewayConfig()
            }
            .catch { error ->
                if (error is CancellationException && generateSequence<Throwable>(error) { it.cause }.none { it is FirebaseFirestoreException }) {
                    throw error
                }
                emit(GatewayConfig())
            }

    fun observeReferralConfig(): Flow<ReferralConfig> =
        db.collection("app_config").document("referral")
            .snapshots()
            .map { snap ->
                snap.toObject(ReferralConfig::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    ?: ReferralConfig()
            }
            .catch { error ->
                if (error is CancellationException && generateSequence<Throwable>(error) { it.cause }.none { it is FirebaseFirestoreException }) {
                    throw error
                }
                emit(ReferralConfig())
            }

    fun observeUserNotifications(uid: String): Flow<List<UserNotification>> =
        db.collection("users").document(uid).collection("notifications")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(40)
            .snapshots()
            .map { snap -> snap.toObjects(UserNotification::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
            .catch { error ->
                val fe = generateSequence(error) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
                if (fe == null && error is CancellationException) throw error
                if (auth.currentUser?.uid == uid) {
                    handleFirestoreError(fe ?: error, OperationType.LIST, "users/$uid/notifications")
                }
                emit(emptyList())
            }

    fun observeBroadcasts(): Flow<List<BroadcastNotification>> =
        db.collection("broadcasts")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(30)
            .snapshots()
            .map { snap -> snap.toObjects(BroadcastNotification::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
            .catch { error ->
                val fe = generateSequence(error) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
                if (fe == null && error is CancellationException) throw error
                handleFirestoreError(fe ?: error, OperationType.LIST, "broadcasts")
                emit(emptyList())
            }

    fun observeMyReferrals(uid: String): Flow<List<ReferralRecord>> =
        db.collection("referrals")
            .whereEqualTo("referrerUid", uid)
            .snapshots()
            .map { snap -> snap.toObjects(ReferralRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
            .catch { error ->
                val fe = generateSequence(error) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
                if (fe == null && error is CancellationException) throw error
                handleFirestoreError(fe ?: error, OperationType.LIST, "referrals")
                emit(emptyList())
            }

    fun observeMyTickets(uid: String): Flow<List<SupportTicket>> =
        db.collection("tickets")
            .whereEqualTo("userId", uid)
            .snapshots()
            .map { snap ->
                snap.toObjects(SupportTicket::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.updatedAt?.toDate()?.time ?: 0L }
            }
            .catch { error ->
                val fe = generateSequence(error) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
                if (fe == null && error is CancellationException) throw error
                handleFirestoreError(fe ?: error, OperationType.LIST, "tickets")
                emit(emptyList())
            }

    fun observeTicketMessages(ticketId: String): Flow<List<TicketMessage>> =
        db.collection("tickets").document(ticketId).collection("messages")
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .snapshots()
            .map { snap -> snap.toObjects(TicketMessage::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
            .catch { error ->
                val fe = generateSequence(error) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
                if (fe == null && error is CancellationException) throw error
                handleFirestoreError(fe ?: error, OperationType.LIST, "tickets/$ticketId/messages")
                emit(emptyList())
            }

    suspend fun updateProfile(
        displayName: String,
        inGameName: String,
        gameUid: String,
        phoneNumber: String,
        pushEnabled: Boolean
    ): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val path = "users/$uid"
        return try {
            require(displayName.trim().isNotEmpty()) { "Display name required" }
            db.collection("users").document(uid).update(
                mapOf(
                    "displayName" to displayName.trim().take(80),
                    "inGameName" to inGameName.trim().take(60),
                    "gameUid" to gameUid.trim().take(60),
                    "phoneNumber" to phoneNumber.trim().take(30),
                    "pushEnabled" to pushEnabled,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            registerPushDevice(uid, pushEnabled)
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun applyReferralCode(rawCode: String): Result<String> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val code = rawCode.trim().uppercase().replace(Regex("[^A-Z0-9_\\-]"), "")
        if (code.length < 3) return Result.failure(IllegalArgumentException("Invalid referral code"))
        return try {
            val lookupSnap = db.collection("referral_codes").document(code).get().await()
            if (!lookupSnap.exists()) return Result.failure(IllegalArgumentException("Referral code not found"))
            val referrerUid = lookupSnap.getString("ownerUid").orEmpty()
            val referrerName = lookupSnap.getString("ownerName").orEmpty()
            if (referrerUid.isBlank() || referrerUid == uid) {
                return Result.failure(IllegalArgumentException("Cannot use your own referral code"))
            }

            val refConfigSnap = db.collection("app_config").document("referral").get().await()
            val refConfig = refConfigSnap.toObject(ReferralConfig::class.java) ?: ReferralConfig()
            val deviceFp = "${Build.BOARD}_${Build.MODEL}_${Build.BRAND}".take(60)

            db.runTransaction { tx ->
                val userRef = db.collection("users").document(uid)
                val userSnap = tx.get(userRef)
                val alreadyReferred = userSnap.getString("referredByUid").orEmpty()
                if (alreadyReferred.isNotBlank()) throw IllegalStateException("Referral code already applied")

                tx.update(
                    userRef,
                    mapOf(
                        "referredByCode" to code,
                        "referredByUid" to referrerUid,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }.await()

            // If triggerRule is signup and referral program is enabled, qualify immediately
            if (refConfig.enabled && refConfig.triggerRule == "signup") {
                qualifyReferralOnTrigger(uid, referrerUid, code, refConfig, deviceFp)
            }
            Result.success("Linked to $referrerName ($code)")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "referral_codes/$code")
            Result.failure(e)
        }
    }

    suspend fun checkPromoCouponExists(rawCode: String): Boolean {
        val code = rawCode.trim().uppercase().replace(Regex("[^A-Z0-9_\\-]"), "")
        if (code.length < 3) return false
        return runCatching {
            val snap = db.collection("coupons").document(code).get().await()
            snap.exists() && (snap.getBoolean("active") == true)
        }.getOrDefault(false)
    }

    suspend fun qualifyReferralOnTrigger(
        referredUid: String,
        referrerUid: String,
        referrerCode: String,
        refConfig: ReferralConfig,
        deviceFp: String,
        depositAmount: Double = 0.0
    ) {
        if (referrerUid.isBlank() || referrerUid == referredUid || !refConfig.enabled) return
        val reward = if (refConfig.referrerRewardType == "percent" && depositAmount > 0.0) {
            minOf((depositAmount * refConfig.referrerRewardValue / 100.0), 500.0)
        } else {
            minOf(refConfig.referrerRewardValue, 500.0)
        }
        val friendBonus = minOf(refConfig.friendSignupBonus, 200.0)
        runCatching {
            db.runTransaction { tx ->
                val referralDocRef = db.collection("referrals").document(referredUid)
                if (tx.get(referralDocRef).exists()) return@runTransaction
                val referrerRef = db.collection("users").document(referrerUid)
                val referrerSnap = tx.get(referrerRef)
                if (!referrerSnap.exists()) return@runTransaction

                val referredRef = db.collection("users").document(referredUid)
                val referredSnap = tx.get(referredRef)
                val referredName = referredSnap.getString("displayName").orEmpty().ifBlank { "Friend" }

                val currentBonus = referrerSnap.getDouble("bonusBalance") ?: 0.0
                val currentCount = referrerSnap.getLong("referralCount") ?: 0L
                val currentEarnings = referrerSnap.getDouble("referralEarnings") ?: 0.0

                tx.set(
                    referralDocRef,
                    mapOf(
                        "referrerUid" to referrerUid,
                        "referrerCode" to referrerCode,
                        "referredUid" to referredUid,
                        "referredName" to referredName,
                        "deviceFingerprint" to deviceFp,
                        "triggerType" to refConfig.triggerRule,
                        "rewardAmount" to reward,
                        "friendBonus" to friendBonus,
                        "status" to "qualified",
                        "flaggedFraud" to false,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                tx.update(
                    referrerRef,
                    mapOf(
                        "bonusBalance" to (currentBonus + reward),
                        "referralCount" to (currentCount + 1L),
                        "referralEarnings" to (currentEarnings + reward),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                if (friendBonus > 0.0) {
                    val myBonus = referredSnap.getDouble("bonusBalance") ?: 0.0
                    tx.update(
                        referredRef,
                        mapOf(
                            "bonusBalance" to (myBonus + friendBonus),
                            "referralQualified" to true,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
            }.await()
        }
    }

    suspend fun claimDailyCheckIn(config: GeneralConfig): Result<Long> {
        val uid = requireUserId()
        if (!config.checkinEnabled) return Result.failure(IllegalStateException("Daily check-in is disabled by Admin"))
        val today = ArenaFormatters.todayKey()
        return try {
            val reward = db.runTransaction { tx ->
                val userRef = db.collection("users").document(uid)
                val snap = tx.get(userRef)
                val lastDay = snap.getString("lastCheckInDay").orEmpty()
                if (lastDay == today) throw IllegalStateException("Already claimed today's streak bonus")
                val oldStreak = snap.getLong("streakDays") ?: 0L
                val newStreak = ((oldStreak % 7L) + 1L)
                val rewards = config.checkinRewards.ifEmpty { listOf(5L, 8L, 10L, 12L, 15L, 20L, 50L) }
                val amount = rewards[((newStreak - 1).toInt()).coerceIn(0, rewards.lastIndex)]
                val oldBonus = snap.getDouble("bonusBalance") ?: 0.0

                tx.update(
                    userRef,
                    mapOf(
                        "bonusBalance" to (oldBonus + amount.toDouble()),
                        "streakDays" to newStreak,
                        "lastCheckInDay" to today,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                val txRef = userRef.collection("transactions").document()
                tx.set(
                    txRef,
                    mapOf(
                        "userId" to uid,
                        "type" to "checkin",
                        "amount" to amount.toDouble(),
                        "direction" to "credit",
                        "walletBucket" to "bonus",
                        "title" to "Day $newStreak Check-In Streak",
                        "subtitle" to "Daily arena reward",
                        "referenceId" to today,
                        "status" to "completed",
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )
                amount
            }.await()
            Result.success(reward)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "users/$uid")
            Result.failure(e)
        }
    }

    suspend fun markNotificationRead(notifId: String) {
        val uid = auth.currentUser?.uid ?: return
        runCatching {
            db.collection("users").document(uid).collection("notifications").document(notifId)
                .update("read", true).await()
        }
    }

    suspend fun createSupportTicket(
        user: UserProfile,
        category: String,
        subject: String,
        message: String,
        matchId: String,
        reportedPlayerUid: String,
        screenshotBase64: String
    ): Result<String> {
        val uid = requireUserId()
        val ticketRef = db.collection("tickets").document()
        return try {
            require(subject.trim().length >= 2) { "Subject too short" }
            require(message.trim().isNotEmpty()) { "Message required" }
            val batch = db.batch()
            batch.set(
                ticketRef,
                mapOf(
                    "userId" to uid,
                    "userName" to user.displayName,
                    "userEmail" to user.email,
                    "category" to category.take(60),
                    "subject" to subject.trim().take(160),
                    "matchId" to matchId.trim().take(128),
                    "reportedPlayerUid" to reportedPlayerUid.trim().take(80),
                    "screenshotBase64" to screenshotBase64.take(440000),
                    "status" to "open",
                    "lastMessage" to message.trim().take(480),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                ).filterValues { it != "" }
            )
            val msgRef = ticketRef.collection("messages").document()
            batch.set(
                msgRef,
                mapOf(
                    "ticketId" to ticketRef.id,
                    "senderUid" to uid,
                    "senderName" to user.displayName,
                    "senderRole" to "user",
                    "text" to message.trim().take(2000),
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(ticketRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, ticketRef.path)
            Result.failure(e)
        }
    }

    suspend fun sendTicketReply(ticketId: String, senderName: String, text: String): Result<Unit> {
        val uid = requireUserId()
        return try {
            require(text.trim().isNotEmpty()) { "Reply cannot be empty" }
            val ticketRef = db.collection("tickets").document(ticketId)
            val msgRef = ticketRef.collection("messages").document()
            val batch = db.batch()
            batch.set(
                msgRef,
                mapOf(
                    "ticketId" to ticketId,
                    "senderUid" to uid,
                    "senderName" to senderName,
                    "senderRole" to "user",
                    "text" to text.trim().take(2000),
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            batch.update(
                ticketRef,
                mapOf(
                    "lastMessage" to text.trim().take(480),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "tickets/$ticketId")
            Result.failure(e)
        }
    }
}
