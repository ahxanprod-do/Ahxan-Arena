package com.example.data.repository

import com.example.data.model.AuditLog
import com.example.data.model.BroadcastNotification
import com.example.data.model.CmsBanner
import com.example.data.model.Coupon
import com.example.data.model.DepositOrder
import com.example.data.model.GatewayConfig
import com.example.data.model.GeneralConfig
import com.example.data.model.MatchSlot
import com.example.data.model.OperationType
import com.example.data.model.PushDevice
import com.example.data.model.ReferralConfig
import com.example.data.model.ReferralRecord
import com.example.data.model.StaffRole
import com.example.data.model.SupportTicket
import com.example.data.model.Tournament
import com.example.data.model.UserProfile
import com.example.data.model.WithdrawalRequest
import com.example.data.model.handleFirestoreError
import com.google.firebase.Firebase
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

class ArenaAdminRepository(private val db: FirebaseFirestore) {
    private val auth = Firebase.auth

    private fun adminEmail(): String = auth.currentUser?.email.orEmpty().ifBlank { "admin" }
    private fun adminUid(): String = auth.currentUser?.uid ?: throw IllegalStateException("Admin not signed in")

    private suspend fun logAudit(actionType: String, targetId: String, summary: String, oldVal: String = "", newVal: String = "") {
        runCatching {
            db.collection("audit_logs").document().set(
                mapOf(
                    "adminUid" to adminUid(),
                    "adminEmail" to adminEmail(),
                    "actionType" to actionType.take(60),
                    "targetId" to targetId.take(128),
                    "summary" to summary.take(500),
                    "oldValue" to oldVal.take(500),
                    "newValue" to newVal.take(500),
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
        }
    }

    private suspend fun sendUserNotification(
        uid: String,
        title: String,
        body: String,
        route: String = "home",
        imageUrl: String = "",
        category: String = "system"
    ) {
        runCatching {
            db.collection("users").document(uid).collection("notifications").document().set(
                mapOf(
                    "userId" to uid,
                    "title" to title.take(140),
                    "body" to body.take(600),
                    "imageUrl" to imageUrl.take(1000),
                    "route" to route.take(120),
                    "category" to category.take(40),
                    "read" to false,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
        }
    }

    // --- ADMIN OBSERVERS ---
    fun observeAllUsers(): Flow<List<UserProfile>> =
        db.collection("users").snapshots()
            .map { it.toObjects(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
            .catch { handleFlowError(it, "users"); emit(emptyList()) }

    fun observeAllDeposits(): Flow<List<DepositOrder>> =
        db.collection("deposits").orderBy("createdAt", Query.Direction.DESCENDING).limit(150).snapshots()
            .map { it.toObjects(DepositOrder::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
            .catch { handleFlowError(it, "deposits"); emit(emptyList()) }

    fun observeAllWithdrawals(): Flow<List<WithdrawalRequest>> =
        db.collection("withdrawals").orderBy("createdAt", Query.Direction.DESCENDING).limit(150).snapshots()
            .map { it.toObjects(WithdrawalRequest::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
            .catch { handleFlowError(it, "withdrawals"); emit(emptyList()) }

    fun observeAllTickets(): Flow<List<SupportTicket>> =
        db.collection("tickets").orderBy("updatedAt", Query.Direction.DESCENDING).limit(150).snapshots()
            .map { it.toObjects(SupportTicket::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
            .catch { handleFlowError(it, "tickets"); emit(emptyList()) }

    fun observeAllCoupons(): Flow<List<Coupon>> =
        db.collection("coupons").snapshots()
            .map { it.toObjects(Coupon::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
            .catch { handleFlowError(it, "coupons"); emit(emptyList()) }

    fun observeAllReferrals(): Flow<List<ReferralRecord>> =
        db.collection("referrals").snapshots()
            .map { it.toObjects(ReferralRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
            .catch { handleFlowError(it, "referrals"); emit(emptyList()) }

    fun observeAllBanners(): Flow<List<CmsBanner>> =
        db.collection("cms_banners").snapshots()
            .map { it.toObjects(CmsBanner::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE).sortedBy { b -> b.sortOrder } }
            .catch { handleFlowError(it, "cms_banners"); emit(emptyList()) }

    fun observeStaffRoles(): Flow<List<StaffRole>> =
        db.collection("staff_roles").snapshots()
            .map { it.toObjects(StaffRole::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
            .catch { handleFlowError(it, "staff_roles"); emit(emptyList()) }

    fun observeAuditLogs(): Flow<List<AuditLog>> =
        db.collection("audit_logs").orderBy("createdAt", Query.Direction.DESCENDING).limit(150).snapshots()
            .map { it.toObjects(AuditLog::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
            .catch { handleFlowError(it, "audit_logs"); emit(emptyList()) }

    fun observePushDevices(): Flow<List<PushDevice>> =
        db.collection("push_devices").snapshots()
            .map { it.toObjects(PushDevice::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
            .catch { handleFlowError(it, "push_devices"); emit(emptyList()) }

    private fun handleFlowError(error: Throwable, path: String) {
        val fe = generateSequence(error) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
        if (fe == null && error is CancellationException) throw error
        handleFirestoreError(fe ?: error, OperationType.LIST, path)
    }

    // --- SEED INITIAL DATA (Admin first boot) ---
    suspend fun seedInitialDataIfEmpty(): Result<Boolean> {
        return try {
            adminUid()
            val existingMatches = db.collection("tournaments").limit(1).get().await()
            if (!existingMatches.isEmpty) return Result.success(false)

            val now = System.currentTimeMillis()
            val batch = db.batch()

            val sampleMatches = listOf(
                mapOf(
                    "title" to "BGMI Cyber Vanguard Scrims #1",
                    "game" to "BGMI",
                    "mode" to "Squad",
                    "mapName" to "Erangel",
                    "entryType" to "Paid",
                    "entryFee" to 40.0,
                    "prizePool" to 2500.0,
                    "perKillReward" to 25.0,
                    "maxSlots" to 25L,
                    "filledSlots" to 0L,
                    "status" to "upcoming",
                    "scheduledAtMillis" to (now + 3600_000L * 2),
                    "bannerKey" to "bgmi",
                    "rulesText" to "No emulators, no GFX hacks. Screenshot required for claims.",
                    "prizeBreakdown" to "1st: ₹1000 • 2nd: ₹600 • 3rd: ₹400 • Per Kill: ₹25",
                    "roomPublished" to false,
                    "createdBy" to adminEmail(),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                ),
                mapOf(
                    "title" to "Free Fire MAX Neon Clash",
                    "game" to "Free Fire MAX",
                    "mode" to "Solo",
                    "mapName" to "Bermuda",
                    "entryType" to "Paid",
                    "entryFee" to 25.0,
                    "prizePool" to 1200.0,
                    "perKillReward" to 15.0,
                    "maxSlots" to 48L,
                    "filledSlots" to 0L,
                    "status" to "upcoming",
                    "scheduledAtMillis" to (now + 3600_000L * 4),
                    "bannerKey" to "freefire",
                    "rulesText" to "Mobile devices only. Level 35+ ID required.",
                    "prizeBreakdown" to "Booyah: ₹500 • #2: ₹250 • Per Kill: ₹15",
                    "roomPublished" to false,
                    "createdBy" to adminEmail(),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                ),
                mapOf(
                    "title" to "COD Mobile Tactical Free Cup",
                    "game" to "COD Mobile",
                    "mode" to "Duo",
                    "mapName" to "Isolated",
                    "entryType" to "Free",
                    "entryFee" to 0.0,
                    "prizePool" to 500.0,
                    "perKillReward" to 10.0,
                    "maxSlots" to 50L,
                    "filledSlots" to 0L,
                    "status" to "upcoming",
                    "scheduledAtMillis" to (now + 3600_000L * 6),
                    "bannerKey" to "codm",
                    "rulesText" to "Free entry community tournament. Fair play enforced.",
                    "prizeBreakdown" to "1st Duo: ₹300 • 2nd Duo: ₹200",
                    "roomPublished" to false,
                    "createdBy" to adminEmail(),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            sampleMatches.forEach { batch.set(db.collection("tournaments").document(), it) }

            batch.set(
                db.collection("coupons").document("WELCOME20"),
                mapOf(
                    "code" to "WELCOME20",
                    "couponType" to "deposit_bonus",
                    "valueType" to "percentage",
                    "value" to 20.0,
                    "minAmount" to 50.0,
                    "maxCap" to 200.0,
                    "maxGlobalUses" to 500L,
                    "maxPerUserUses" to 1L,
                    "usedCount" to 0L,
                    "audience" to "all",
                    "active" to true,
                    "expiresAtMillis" to (now + 86400_000L * 90),
                    "totalDiscountGiven" to 0.0,
                    "description" to "Get 20% extra deposit bonus up to ₹200!",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.set(
                db.collection("coupons").document("FIRST50"),
                mapOf(
                    "code" to "FIRST50",
                    "couponType" to "match_discount",
                    "valueType" to "percentage",
                    "value" to 50.0,
                    "minAmount" to 10.0,
                    "maxCap" to 50.0,
                    "maxGlobalUses" to 500L,
                    "maxPerUserUses" to 1L,
                    "usedCount" to 0L,
                    "audience" to "all",
                    "active" to true,
                    "expiresAtMillis" to (now + 86400_000L * 90),
                    "totalDiscountGiven" to 0.0,
                    "description" to "50% off tournament entry fee (max ₹50)!",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )

            val banners = listOf(
                mapOf("title" to "BGMI MEGA CYBER SERIES", "subtitle" to "₹2,500 Prize Pool • Use FIRST50 for 50% OFF", "drawableKey" to "bgmi", "redirectRoute" to "tournaments", "active" to true, "sortOrder" to 1L, "createdAt" to FieldValue.serverTimestamp()),
                mapOf("title" to "PAYZAPI INSTANT BONUS", "subtitle" to "Apply WELCOME20 for +20% Extra Deposit Bonus", "drawableKey" to "freefire", "redirectRoute" to "wallet", "active" to true, "sortOrder" to 2L, "createdAt" to FieldValue.serverTimestamp()),
                mapOf("title" to "REFER & EARN ₹25/FRIEND", "subtitle" to "Instant bonus on friend's first verified deposit", "drawableKey" to "codm", "redirectRoute" to "refer", "active" to true, "sortOrder" to 3L, "createdAt" to FieldValue.serverTimestamp())
            )
            banners.forEach { batch.set(db.collection("cms_banners").document(), it) }

            batch.set(
                db.collection("app_config").document("general"),
                mapOf(
                    "tickerText" to "AHXAN ARENA LIVE • USE WELCOME20 FOR 20% DEPOSIT BONUS • USE FIRST50 ON MATCH ENTRY • INSTANT PAYZAPI UPI SETTLEMENT",
                    "maintenanceMode" to false,
                    "maintenanceMessage" to "Scheduled cyber-arena upgrade in progress.",
                    "maintenanceEta" to "15 mins",
                    "checkinEnabled" to true,
                    "checkinRewards" to listOf(5L, 8L, 10L, 12L, 15L, 20L, 50L),
                    "bonusUsablePercent" to 10L,
                    "matchesHeroBannerUrl" to "",
                    "matchesHeroBannerKey" to "bgmi",
                    "matchesHeroTitle" to "CYBER ESPORTS CHAMPIONSHIPS",
                    "matchesHeroSubtitle" to "Daily Competitive Scrims • Instant PayZapi Payouts",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.set(
                db.collection("app_config").document("gateway"),
                mapOf(
                    "payzapiApiKeyOverride" to "",
                    "resendApiKey" to "",
                    "fampayEnabled" to true,
                    "paytmEnabled" to true,
                    "bharatpeEnabled" to true,
                    "binanceEnabled" to true,
                    "minDeposit" to 10.0,
                    "maxDeposit" to 50000.0,
                    "depositMaintenance" to false,
                    "minWithdrawal" to 50.0,
                    "maxWithdrawal" to 25000.0,
                    "withdrawalFeePercent" to 2.0,
                    "dailyWithdrawalLimit" to 50000.0,
                    "payoutsPaused" to false,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.set(
                db.collection("app_config").document("referral"),
                mapOf(
                    "enabled" to true,
                    "referrerRewardType" to "flat",
                    "referrerRewardValue" to 25.0,
                    "friendSignupBonus" to 15.0,
                    "triggerRule" to "first_deposit",
                    "minDepositThreshold" to 50.0,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )

            batch.commit().await()
            logAudit("SEED_DATA", "system", "Auto-seeded initial tournaments, coupons, banners, and configs")
            Result.success(true)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "seed")
            Result.failure(e)
        }
    }

    // --- MODULE 2: NOTIFICATION STUDIO ---
    suspend fun sendBroadcastNotification(
        title: String,
        body: String,
        imageUrl: String,
        redirectRoute: String,
        audienceType: String,
        audienceTargetId: String,
        activeDevicesCount: Int
    ): Result<Unit> {
        return try {
            require(title.isNotBlank() && body.isNotBlank()) { "Title and body are required" }
            val broadcastRef = db.collection("broadcasts").document()
            broadcastRef.set(
                mapOf(
                    "title" to title.trim().take(140),
                    "body" to body.trim().take(600),
                    "imageUrl" to imageUrl.trim().take(1000),
                    "redirectRoute" to redirectRoute.trim().ifBlank { "home" },
                    "priority" to "high",
                    "audienceType" to audienceType,
                    "audienceTargetId" to audienceTargetId.trim(),
                    "sentByEmail" to adminEmail(),
                    "deliveredCount" to activeDevicesCount.toLong(),
                    "failedCount" to 0L,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()

            if (audienceType == "user" && audienceTargetId.isNotBlank()) {
                sendUserNotification(audienceTargetId.trim(), title, body, redirectRoute, imageUrl, "broadcast")
            } else if (audienceType == "match" && audienceTargetId.isNotBlank()) {
                val slots = db.collection("tournaments").document(audienceTargetId.trim()).collection("slots").get().await()
                slots.documents.forEach { doc ->
                    val uid = doc.getString("userId").orEmpty()
                    if (uid.isNotBlank()) sendUserNotification(uid, title, body, redirectRoute, imageUrl, "match")
                }
            }
            logAudit("SEND_BROADCAST", broadcastRef.id, "Sent push '$title' to $audienceType")
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "broadcasts")
            Result.failure(e)
        }
    }

    // --- MODULE 3: TOURNAMENT & MATCH ENGINE ---
    suspend fun saveTournament(tournament: Tournament): Result<Unit> {
        return try {
            val isNew = tournament.id.isBlank()
            val ref = if (isNew) db.collection("tournaments").document() else db.collection("tournaments").document(tournament.id)
            if (isNew) {
                ref.set(
                    mapOf(
                        "title" to tournament.title.trim().take(120),
                        "game" to tournament.game,
                        "mode" to tournament.mode,
                        "mapName" to tournament.mapName,
                        "entryType" to if (tournament.entryFee <= 0.0) "Free" else "Paid",
                        "entryFee" to tournament.entryFee,
                        "prizePool" to tournament.prizePool,
                        "perKillReward" to tournament.perKillReward,
                        "maxSlots" to tournament.maxSlots,
                        "filledSlots" to 0L,
                        "status" to tournament.status,
                        "scheduledAtMillis" to tournament.scheduledAtMillis,
                        "bannerKey" to tournament.bannerKey,
                        "rulesText" to tournament.rulesText,
                        "prizeBreakdown" to tournament.prizeBreakdown,
                        "roomPublished" to false,
                        "createdBy" to adminEmail(),
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
                logAudit("CREATE_MATCH", ref.id, "Created match ${tournament.title}")
            } else {
                ref.update(
                    mapOf(
                        "title" to tournament.title.trim().take(120),
                        "game" to tournament.game,
                        "mode" to tournament.mode,
                        "mapName" to tournament.mapName,
                        "entryType" to if (tournament.entryFee <= 0.0) "Free" else "Paid",
                        "entryFee" to tournament.entryFee,
                        "prizePool" to tournament.prizePool,
                        "perKillReward" to tournament.perKillReward,
                        "maxSlots" to tournament.maxSlots,
                        "status" to tournament.status,
                        "scheduledAtMillis" to tournament.scheduledAtMillis,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
                logAudit("UPDATE_MATCH", ref.id, "Updated match ${tournament.title}")
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "tournaments")
            Result.failure(e)
        }
    }

    suspend fun cloneTournament(source: Tournament): Result<Unit> {
        val cloned = source.copy(
            id = "",
            title = "${source.title} (Clone)".take(115),
            filledSlots = 0L,
            status = "upcoming",
            roomPublished = false,
            scheduledAtMillis = System.currentTimeMillis() + 3600_000L * 2
        )
        return saveTournament(cloned)
    }

    suspend fun publishRoomCredentials(
        match: Tournament,
        roomId: String,
        roomPassword: String,
        serverRegion: String,
        autoPush: Boolean
    ): Result<Unit> {
        return try {
            require(roomId.isNotBlank() && roomPassword.isNotBlank()) { "Room ID & Password required" }
            val matchRef = db.collection("tournaments").document(match.id)
            val batch = db.batch()
            batch.set(
                matchRef.collection("secrets").document("room"),
                mapOf(
                    "roomId" to roomId.trim(),
                    "roomPassword" to roomPassword.trim(),
                    "serverRegion" to serverRegion.trim().ifBlank { "Asia / India" },
                    "publishedAtMillis" to System.currentTimeMillis(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.update(
                matchRef,
                mapOf("roomPublished" to true, "status" to "live", "updatedAt" to FieldValue.serverTimestamp())
            )
            batch.commit().await()

            if (autoPush) {
                val slots = matchRef.collection("slots").get().await()
                slots.documents.forEach { slot ->
                    val uid = slot.getString("userId").orEmpty()
                    if (uid.isNotBlank()) {
                        sendUserNotification(
                            uid = uid,
                            title = "Room ID & Pass Published!",
                            body = "${match.title} • Room: $roomId | Pass: $roomPassword",
                            route = "my_matches",
                            category = "room"
                        )
                    }
                }
            }
            logAudit("PUBLISH_ROOM", match.id, "Published Room $roomId for ${match.title}")
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "tournaments/${match.id}/secrets/room")
            Result.failure(e)
        }
    }

    suspend fun fetchMatchRoster(matchId: String): Result<List<MatchSlot>> {
        return try {
            val snap = db.collection("tournaments").document(matchId).collection("slots").get().await()
            Result.success(snap.toObjects(MatchSlot::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                .sortedBy { it.slotNumber })
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, "tournaments/$matchId/slots")
            Result.failure(e)
        }
    }

    suspend fun kickAndRefundSlot(match: Tournament, slot: MatchSlot): Result<Unit> {
        return try {
            db.runTransaction { tx ->
                val matchRef = db.collection("tournaments").document(match.id)
                val slotRef = matchRef.collection("slots").document(slot.userId)
                val joinedRef = db.collection("users").document(slot.userId).collection("joined_matches").document(match.id)
                val userRef = db.collection("users").document(slot.userId)

                val matchSnap = tx.get(matchRef)
                val userSnap = tx.get(userRef)
                val filled = (matchSnap.getLong("filledSlots") ?: 1L).coerceAtLeast(1L)
                val oldDep = userSnap.getDouble("depositBalance") ?: 0.0
                val oldWin = userSnap.getDouble("winningBalance") ?: 0.0

                tx.update(matchRef, mapOf("filledSlots" to (filled - 1L), "updatedAt" to FieldValue.serverTimestamp()))
                tx.update(slotRef, mapOf("status" to "kicked", "updatedAt" to FieldValue.serverTimestamp()))
                if (tx.get(joinedRef).exists()) {
                    tx.update(joinedRef, mapOf("status" to "kicked", "updatedAt" to FieldValue.serverTimestamp()))
                }
                tx.update(
                    userRef,
                    mapOf(
                        "depositBalance" to (oldDep + slot.depositUsed),
                        "winningBalance" to (oldWin + slot.winningUsed),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                if (slot.entryPaid > 0.0) {
                    tx.set(
                        userRef.collection("transactions").document(),
                        mapOf(
                            "userId" to slot.userId,
                            "type" to "refund",
                            "amount" to slot.entryPaid,
                            "direction" to "credit",
                            "walletBucket" to "deposit",
                            "title" to "Slot Removed & Refunded",
                            "subtitle" to match.title,
                            "referenceId" to match.id,
                            "status" to "completed",
                            "createdAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
            }.await()
            sendUserNotification(slot.userId, "Match Slot Refunded", "Your slot in ${match.title} was refunded.", "wallet")
            logAudit("KICK_REFUND_SLOT", "${match.id}/${slot.userId}", "Kicked & refunded ${slot.inGameName}")
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "tournaments/${match.id}")
            Result.failure(e)
        }
    }

    suspend fun cancelMatchAndRefundAll(match: Tournament): Result<Int> {
        return try {
            val matchRef = db.collection("tournaments").document(match.id)
            val slots = matchRef.collection("slots").get().await()
                .toObjects(MatchSlot::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                .filter { it.status == "joined" }

            slots.forEach { slot ->
                db.runTransaction { tx ->
                    val userRef = db.collection("users").document(slot.userId)
                    val userSnap = tx.get(userRef)
                    val oldDep = userSnap.getDouble("depositBalance") ?: 0.0
                    val oldWin = userSnap.getDouble("winningBalance") ?: 0.0
                    tx.update(
                        userRef,
                        mapOf(
                            "depositBalance" to (oldDep + slot.depositUsed),
                            "winningBalance" to (oldWin + slot.winningUsed),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                    tx.update(matchRef.collection("slots").document(slot.userId), mapOf("status" to "refunded", "updatedAt" to FieldValue.serverTimestamp()))
                    tx.update(userRef.collection("joined_matches").document(match.id), mapOf("status" to "refunded", "updatedAt" to FieldValue.serverTimestamp()))
                    if (slot.entryPaid > 0.0) {
                        tx.set(
                            userRef.collection("transactions").document(),
                            mapOf(
                                "userId" to slot.userId,
                                "type" to "refund",
                                "amount" to slot.entryPaid,
                                "direction" to "credit",
                                "walletBucket" to "deposit",
                                "title" to "Match Cancelled Refund",
                                "subtitle" to match.title,
                                "referenceId" to match.id,
                                "status" to "completed",
                                "createdAt" to FieldValue.serverTimestamp()
                            )
                        )
                    }
                }.await()
                sendUserNotification(slot.userId, "Match Cancelled & Refunded", "${match.title} cancelled. Entry fee ₹${slot.entryPaid.toInt()} returned.", "wallet")
            }

            matchRef.update(mapOf("status" to "cancelled", "updatedAt" to FieldValue.serverTimestamp())).await()
            logAudit("CANCEL_MATCH", match.id, "Cancelled ${match.title} and refunded ${slots.size} players")
            Result.success(slots.size)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "tournaments/${match.id}")
            Result.failure(e)
        }
    }

    suspend fun distributeMatchResults(match: Tournament, results: List<MatchSlot>): Result<Double> {
        return try {
            var totalPaid = 0.0
            results.forEach { res ->
                val prize = res.prizeWon
                totalPaid += prize
                db.runTransaction { tx ->
                    val matchRef = db.collection("tournaments").document(match.id)
                    val slotRef = matchRef.collection("slots").document(res.userId)
                    val userRef = db.collection("users").document(res.userId)
                    val joinedRef = userRef.collection("joined_matches").document(match.id)
                    val lbRef = db.collection("leaderboard").document(res.userId)

                    val userSnap = tx.get(userRef)
                    val oldWin = userSnap.getDouble("winningBalance") ?: 0.0
                    val oldTotalWin = userSnap.getDouble("totalWinnings") ?: 0.0
                    val oldKills = userSnap.getLong("totalKills") ?: 0L
                    val lbSnap = tx.get(lbRef)
                    val lbWin = lbSnap.getDouble("totalWinnings") ?: 0.0
                    val lbKills = lbSnap.getLong("totalKills") ?: 0L
                    val lbMatches = lbSnap.getLong("matchesPlayed") ?: 0L

                    tx.update(slotRef, mapOf("kills" to res.kills, "rank" to res.rank, "prizeWon" to prize, "status" to "settled", "updatedAt" to FieldValue.serverTimestamp()))
                    if (tx.get(joinedRef).exists()) {
                        tx.update(joinedRef, mapOf("kills" to res.kills, "rank" to res.rank, "prizeWon" to prize, "status" to "settled", "updatedAt" to FieldValue.serverTimestamp()))
                    }
                    tx.update(
                        userRef,
                        mapOf(
                            "winningBalance" to (oldWin + prize),
                            "totalWinnings" to (oldTotalWin + prize),
                            "totalKills" to (oldKills + res.kills),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                    if (lbSnap.exists()) {
                        tx.update(
                            lbRef,
                            mapOf(
                                "totalWinnings" to (lbWin + prize),
                                "totalKills" to (lbKills + res.kills),
                                "matchesPlayed" to (lbMatches + 1L),
                                "updatedAt" to FieldValue.serverTimestamp()
                            )
                        )
                    }
                    if (prize > 0.0) {
                        tx.set(
                            userRef.collection("transactions").document(),
                            mapOf(
                                "userId" to res.userId,
                                "type" to "winning",
                                "amount" to prize,
                                "direction" to "credit",
                                "walletBucket" to "winning",
                                "title" to "Prize Won • Rank #${res.rank}",
                                "subtitle" to "${match.title} (${res.kills} Kills)",
                                "referenceId" to match.id,
                                "status" to "completed",
                                "createdAt" to FieldValue.serverTimestamp()
                            )
                        )
                    }
                }.await()
                if (prize > 0.0) {
                    sendUserNotification(res.userId, "You Won ₹${prize.toInt()}!", "Rank #${res.rank} • ${res.kills} kills in ${match.title}", "wallet")
                }
            }
            db.collection("tournaments").document(match.id)
                .update(mapOf("status" to "completed", "updatedAt" to FieldValue.serverTimestamp())).await()
            logAudit("DISTRIBUTE_PRIZES", match.id, "Settled ${results.size} players, paid ₹${totalPaid.toInt()}")
            Result.success(totalPaid)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "tournaments/${match.id}")
            Result.failure(e)
        }
    }

    // --- MODULE 4: COUPONS ---
    suspend fun saveCoupon(coupon: Coupon): Result<Unit> {
        val code = coupon.code.trim().uppercase().replace(Regex("[^A-Z0-9_\\-]"), "")
        if (code.length < 2) return Result.failure(IllegalArgumentException("Coupon code too short"))
        return try {
            val ref = db.collection("coupons").document(code)
            val exists = ref.get().await().exists()
            val payload = mutableMapOf<String, Any>(
                "code" to code,
                "couponType" to coupon.couponType,
                "valueType" to coupon.valueType,
                "value" to coupon.value,
                "minAmount" to coupon.minAmount,
                "maxCap" to coupon.maxCap,
                "maxGlobalUses" to coupon.maxGlobalUses,
                "maxPerUserUses" to coupon.maxPerUserUses,
                "audience" to coupon.audience,
                "active" to coupon.active,
                "expiresAtMillis" to coupon.expiresAtMillis,
                "description" to coupon.description,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (!exists) {
                payload["usedCount"] = 0L
                payload["totalDiscountGiven"] = 0.0
                payload["createdAt"] = FieldValue.serverTimestamp()
                ref.set(payload).await()
            } else {
                ref.update(payload).await()
            }
            logAudit("SAVE_COUPON", code, "Saved coupon $code (${coupon.couponType})")
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "coupons/$code")
            Result.failure(e)
        }
    }

    suspend fun toggleCouponActive(code: String, active: Boolean) {
        runCatching {
            db.collection("coupons").document(code)
                .update(mapOf("active" to active, "updatedAt" to FieldValue.serverTimestamp())).await()
            logAudit("TOGGLE_COUPON", code, "Set active=$active")
        }
    }

    suspend fun deleteCoupon(code: String) {
        runCatching {
            db.collection("coupons").document(code).delete().await()
            logAudit("DELETE_COUPON", code, "Deleted coupon $code")
        }
    }

    // --- MODULE 5: REFERRAL CONFIG & ANTI-FRAUD ---
    suspend fun saveReferralConfig(config: ReferralConfig): Result<Unit> {
        return try {
            db.collection("app_config").document("referral").set(
                mapOf(
                    "enabled" to config.enabled,
                    "referrerRewardType" to config.referrerRewardType,
                    "referrerRewardValue" to config.referrerRewardValue,
                    "friendSignupBonus" to config.friendSignupBonus,
                    "triggerRule" to config.triggerRule,
                    "minDepositThreshold" to config.minDepositThreshold,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            logAudit("UPDATE_REFERRAL_CFG", "referral", "Enabled=${config.enabled}, trigger=${config.triggerRule}")
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "app_config/referral")
            Result.failure(e)
        }
    }

    suspend fun toggleBlockReferral(referralId: String, block: Boolean): Result<Unit> {
        return try {
            db.collection("referrals").document(referralId).update(
                mapOf(
                    "status" to if (block) "blocked" else "qualified",
                    "flaggedFraud" to block,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            logAudit("REFERRAL_FRAUD_ACTION", referralId, "Blocked=$block")
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "referrals/$referralId")
            Result.failure(e)
        }
    }

    // --- MODULE 6: TICKETS ---
    suspend fun replyToTicketAsAdmin(ticket: SupportTicket, replyText: String, newStatus: String): Result<Unit> {
        return try {
            val ticketRef = db.collection("tickets").document(ticket.id)
            val batch = db.batch()
            if (replyText.isNotBlank()) {
                batch.set(
                    ticketRef.collection("messages").document(),
                    mapOf(
                        "ticketId" to ticket.id,
                        "senderUid" to adminUid(),
                        "senderName" to "Ahxan Support",
                        "senderRole" to "admin",
                        "text" to replyText.trim().take(2000),
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )
            }
            batch.update(
                ticketRef,
                mapOf(
                    "status" to newStatus,
                    "lastMessage" to (replyText.trim().ifBlank { "Status updated to $newStatus" }).take(480),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            sendUserNotification(
                uid = ticket.userId,
                title = "Support Reply • ${ticket.subject.take(36)}",
                body = replyText.trim().ifBlank { "Ticket status updated to ${newStatus.uppercase()}" },
                route = "support"
            )
            logAudit("TICKET_REPLY", ticket.id, "Status=$newStatus")
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "tickets/${ticket.id}")
            Result.failure(e)
        }
    }

    // --- MODULE 7 & 8: GATEWAY CONFIG & WITHDRAWAL QUEUE ---
    suspend fun saveGatewayConfig(config: GatewayConfig): Result<Unit> {
        return try {
            db.collection("app_config").document("gateway").set(
                mapOf(
                    "payzapiApiKeyOverride" to config.payzapiApiKeyOverride.trim(),
                    "resendApiKey" to config.resendApiKey.trim(),
                    "fampayEnabled" to config.fampayEnabled,
                    "paytmEnabled" to config.paytmEnabled,
                    "bharatpeEnabled" to config.bharatpeEnabled,
                    "binanceEnabled" to config.binanceEnabled,
                    "minDeposit" to config.minDeposit,
                    "maxDeposit" to config.maxDeposit,
                    "depositMaintenance" to config.depositMaintenance,
                    "minWithdrawal" to config.minWithdrawal,
                    "maxWithdrawal" to config.maxWithdrawal,
                    "withdrawalFeePercent" to config.withdrawalFeePercent,
                    "dailyWithdrawalLimit" to config.dailyWithdrawalLimit,
                    "payoutsPaused" to config.payoutsPaused,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            logAudit("UPDATE_GATEWAY_CFG", "gateway", "Updated PayZapi & Withdrawal rules")
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "app_config/gateway")
            Result.failure(e)
        }
    }

    suspend fun approveWithdrawal(wd: WithdrawalRequest, utrOrHash: String): Result<Unit> {
        return try {
            require(utrOrHash.trim().length >= 3) { "Enter UTR or Transaction Hash" }
            db.collection("withdrawals").document(wd.id).update(
                mapOf(
                    "status" to "approved",
                    "utrOrTxHash" to utrOrHash.trim(),
                    "processedBy" to adminEmail(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            sendUserNotification(wd.userId, "Withdrawal Approved!", "₹${wd.netAmount.toInt()} paid via ${wd.method}. Ref: ${utrOrHash.trim()}", "wallet")
            logAudit("APPROVE_WITHDRAWAL", wd.id, "Paid ₹${wd.netAmount.toInt()} to ${wd.destination}", "pending", "approved:$utrOrHash")
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "withdrawals/${wd.id}")
            Result.failure(e)
        }
    }

    suspend fun rejectWithdrawalAndRefund(wd: WithdrawalRequest, reason: String): Result<Unit> {
        return try {
            val cleanReason = reason.trim().ifBlank { "Rejected by Admin" }
            db.runTransaction { tx ->
                val wdRef = db.collection("withdrawals").document(wd.id)
                val wdSnap = tx.get(wdRef)
                if (wdSnap.getString("status") != "pending") throw IllegalStateException("Already processed")
                val userRef = db.collection("users").document(wd.userId)
                val userSnap = tx.get(userRef)
                val oldWin = userSnap.getDouble("winningBalance") ?: 0.0

                tx.update(
                    wdRef,
                    mapOf(
                        "status" to "rejected",
                        "rejectReason" to cleanReason,
                        "processedBy" to adminEmail(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                tx.update(
                    userRef,
                    mapOf(
                        "winningBalance" to (oldWin + wd.grossAmount),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                tx.set(
                    userRef.collection("transactions").document(),
                    mapOf(
                        "userId" to wd.userId,
                        "type" to "refund",
                        "amount" to wd.grossAmount,
                        "direction" to "credit",
                        "walletBucket" to "winning",
                        "title" to "Withdrawal Rejected Refund",
                        "subtitle" to cleanReason,
                        "referenceId" to wd.id,
                        "status" to "completed",
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )
            }.await()
            sendUserNotification(wd.userId, "Withdrawal Rejected", "₹${wd.grossAmount.toInt()} refunded to Winning Wallet. Reason: $cleanReason", "wallet")
            logAudit("REJECT_WITHDRAWAL", wd.id, "Rejected & refunded ₹${wd.grossAmount.toInt()}: $cleanReason")
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "withdrawals/${wd.id}")
            Result.failure(e)
        }
    }

    // --- MODULE 9: USER 360° CRM ---
    suspend fun adjustUserWallet(targetUid: String, bucket: String, isCredit: Boolean, amount: Double, reason: String): Result<Unit> {
        return try {
            require(amount > 0.0) { "Amount must be > 0" }
            require(reason.isNotBlank()) { "Audit reason is mandatory" }
            val field = when (bucket) {
                "winning" -> "winningBalance"
                "bonus" -> "bonusBalance"
                else -> "depositBalance"
            }
            var oldBal = 0.0
            var newBal = 0.0
            db.runTransaction { tx ->
                val userRef = db.collection("users").document(targetUid)
                val snap = tx.get(userRef)
                oldBal = snap.getDouble(field) ?: 0.0
                newBal = if (isCredit) oldBal + amount else (oldBal - amount).coerceAtLeast(0.0)
                tx.update(userRef, mapOf(field to newBal, "updatedAt" to FieldValue.serverTimestamp()))
                tx.set(
                    userRef.collection("transactions").document(),
                    mapOf(
                        "userId" to targetUid,
                        "type" to "admin_adjustment",
                        "amount" to amount,
                        "direction" to if (isCredit) "credit" else "debit",
                        "walletBucket" to bucket,
                        "title" to "Admin ${if (isCredit) "Credit" else "Debit"}",
                        "subtitle" to reason.take(180),
                        "referenceId" to adminUid(),
                        "status" to "completed",
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )
            }.await()
            logAudit("USER_WALLET_ADJUST", targetUid, "${if (isCredit) "Credited" else "Debited"} ₹$amount ($bucket): $reason", "$oldBal", "$newBal")
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "users/$targetUid")
            Result.failure(e)
        }
    }

    suspend fun toggleBanUser(targetUid: String, ban: Boolean, reason: String): Result<Unit> {
        return try {
            db.collection("users").document(targetUid).update(
                mapOf(
                    "banned" to ban,
                    "banReason" to if (ban) reason.trim().ifBlank { "Policy violation" } else "",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            logAudit(if (ban) "BAN_USER" else "UNBAN_USER", targetUid, reason.ifBlank { "Status updated" })
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "users/$targetUid")
            Result.failure(e)
        }
    }

    // --- MODULE 10: STAFF ROLES ---
    suspend fun saveStaffRole(role: StaffRole): Result<Unit> {
        val uid = role.userId.trim()
        if (uid.isBlank()) return Result.failure(IllegalArgumentException("Target User UID required"))
        return try {
            db.collection("staff_roles").document(uid).set(
                mapOf(
                    "userId" to uid,
                    "email" to role.email.trim(),
                    "name" to role.name.trim(),
                    "roleLabel" to role.roleLabel.trim().ifBlank { "Moderator" },
                    "manageMatches" to role.manageMatches,
                    "publishRooms" to role.publishRooms,
                    "verifyDeposits" to role.verifyDeposits,
                    "approveWithdrawals" to role.approveWithdrawals,
                    "manageCoupons" to role.manageCoupons,
                    "replyTickets" to role.replyTickets,
                    "sendPush" to role.sendPush,
                    "manageUsers" to role.manageUsers,
                    "systemSettings" to role.systemSettings,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            logAudit("SAVE_STAFF_ROLE", uid, "Assigned ${role.roleLabel} to ${role.email}")
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "staff_roles/$uid")
            Result.failure(e)
        }
    }

    suspend fun removeStaffRole(uid: String) {
        runCatching {
            db.collection("staff_roles").document(uid).delete().await()
            logAudit("REMOVE_STAFF_ROLE", uid, "Revoked staff permissions")
        }
    }

    // --- MODULE 11: CMS & GENERAL SETTINGS ---
    suspend fun saveGeneralConfig(config: GeneralConfig): Result<Unit> {
        return try {
            db.collection("app_config").document("general").set(
                mapOf(
                    "tickerText" to config.tickerText,
                    "maintenanceMode" to config.maintenanceMode,
                    "maintenanceMessage" to config.maintenanceMessage,
                    "maintenanceEta" to config.maintenanceEta,
                    "rulesText" to config.rulesText,
                    "termsText" to config.termsText,
                    "privacyText" to config.privacyText,
                    "whatsappUrl" to config.whatsappUrl,
                    "telegramUrl" to config.telegramUrl,
                    "discordUrl" to config.discordUrl,
                    "instagramUrl" to config.instagramUrl,
                    "checkinEnabled" to config.checkinEnabled,
                    "checkinRewards" to config.checkinRewards,
                    "bonusUsablePercent" to config.bonusUsablePercent,
                    "matchesHeroBannerUrl" to config.matchesHeroBannerUrl.trim(),
                    "matchesHeroBannerKey" to config.matchesHeroBannerKey,
                    "matchesHeroTitle" to config.matchesHeroTitle.trim().take(100),
                    "matchesHeroSubtitle" to config.matchesHeroSubtitle.trim().take(160),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            logAudit("UPDATE_CMS_GENERAL", "general", "Maintenance=${config.maintenanceMode}, CheckIn=${config.checkinEnabled}")
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "app_config/general")
            Result.failure(e)
        }
    }

    suspend fun saveBanner(banner: CmsBanner): Result<Unit> {
        return try {
            val ref = if (banner.id.isBlank()) db.collection("cms_banners").document() else db.collection("cms_banners").document(banner.id)
            ref.set(
                mapOf(
                    "title" to banner.title.trim().take(100),
                    "subtitle" to banner.subtitle.trim().take(160),
                    "imageUrl" to banner.imageUrl.trim().take(1000),
                    "drawableKey" to banner.drawableKey,
                    "redirectRoute" to banner.redirectRoute,
                    "active" to banner.active,
                    "sortOrder" to banner.sortOrder,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
            logAudit("SAVE_BANNER", ref.id, "Saved banner ${banner.title}")
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "cms_banners")
            Result.failure(e)
        }
    }

    suspend fun deleteBanner(bannerId: String) {
        runCatching {
            db.collection("cms_banners").document(bannerId).delete().await()
            logAudit("DELETE_BANNER", bannerId, "Deleted banner")
        }
    }
}
