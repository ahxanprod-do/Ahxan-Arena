package com.example.data.repository

import com.example.data.model.Coupon
import com.example.data.model.MatchSlot
import com.example.data.model.OperationType
import com.example.data.model.RoomSecret
import com.example.data.model.Tournament
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
import kotlin.math.max
import kotlin.math.min

class ArenaTournamentRepository(private val db: FirebaseFirestore) {
    private val auth = Firebase.auth

    private fun requireUserId(): String =
        auth.currentUser?.uid ?: throw IllegalStateException("User must be signed in.")

    fun observeTournaments(): Flow<List<Tournament>> =
        db.collection("tournaments")
            .orderBy("scheduledAtMillis", Query.Direction.ASCENDING)
            .snapshots()
            .map { snap -> snap.toObjects(Tournament::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
            .catch { error ->
                val fe = generateSequence(error) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
                if (fe == null && error is CancellationException) throw error
                handleFirestoreError(fe ?: error, OperationType.LIST, "tournaments")
                throw error
            }

    fun observeMyJoinedMatches(uid: String): Flow<List<MatchSlot>> =
        db.collection("users").document(uid).collection("joined_matches")
            .snapshots()
            .map { snap ->
                snap.toObjects(MatchSlot::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt?.toDate()?.time ?: 0L }
            }
            .catch { error ->
                val fe = generateSequence(error) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
                if (fe == null && error is CancellationException) throw error
                if (auth.currentUser?.uid == uid) {
                    handleFirestoreError(fe ?: error, OperationType.LIST, "users/$uid/joined_matches")
                }
                throw error
            }

    suspend fun fetchRoomSecretIfJoined(matchId: String): Result<RoomSecret?> {
        val path = "tournaments/$matchId/secrets/room"
        return try {
            requireUserId()
            val snap = db.collection("tournaments").document(matchId)
                .collection("secrets").document("room").get().await()
            if (!snap.exists()) return Result.success(null)
            Result.success(snap.toObject(RoomSecret::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, path)
            Result.failure(e)
        }
    }

    suspend fun fetchMatchParticipants(matchId: String): Result<List<MatchSlot>> {
        return try {
            requireUserId()
            val snap = db.collection("tournaments").document(matchId)
                .collection("slots").get().await()
            val list = snap.toObjects(MatchSlot::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                .filter { it.status == "joined" || it.status == "settled" }
                .sortedBy { it.slotNumber }
            Result.success(list)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, "tournaments/$matchId/slots")
            Result.failure(e)
        }
    }

    suspend fun validateMatchCoupon(codeRaw: String, entryFee: Double): Result<Coupon> {
        val uid = requireUserId()
        val code = codeRaw.trim().uppercase()
        if (code.isBlank()) return Result.failure(IllegalArgumentException("Enter a coupon code"))
        return try {
            val snap = db.collection("coupons").document(code).get().await()
            if (!snap.exists()) return Result.failure(IllegalArgumentException("Coupon '$code' does not exist"))
            val coupon = snap.toObject(Coupon::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                ?: return Result.failure(IllegalArgumentException("Invalid coupon"))
            if (coupon.couponType != "match_discount") {
                return Result.failure(IllegalArgumentException("This code is for Deposit Bonus, not Match Entry"))
            }
            if (!coupon.active) return Result.failure(IllegalArgumentException("Coupon is paused"))
            if (coupon.usedCount >= coupon.maxGlobalUses) return Result.failure(IllegalArgumentException("Coupon usage limit reached"))
            if (coupon.expiresAtMillis > 0L && System.currentTimeMillis() > coupon.expiresAtMillis) {
                return Result.failure(IllegalArgumentException("Coupon has expired"))
            }
            if (entryFee < coupon.minAmount) {
                return Result.failure(IllegalArgumentException("Minimum entry fee ₹${coupon.minAmount.toInt()} required"))
            }
            val redemptions = db.collection("coupons").document(code).collection("redemptions")
                .whereEqualTo("userId", uid).get().await()
            if (redemptions.size() >= coupon.maxPerUserUses) {
                return Result.failure(IllegalArgumentException("You already used this coupon"))
            }
            Result.success(coupon)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, "coupons/$code")
            Result.failure(e)
        }
    }

    suspend fun atomicJoinMatch(
        match: Tournament,
        inGameName: String,
        gameUid: String,
        squadMembers: List<String>,
        couponCodeRaw: String,
        bonusUsablePercent: Long,
        preferredSlotNumber: Long? = null
    ): Result<MatchSlot> {
        val uid = requireUserId()
        val cleanIgn = inGameName.trim()
        val cleanGameUid = gameUid.trim()
        if (cleanIgn.isEmpty() || cleanGameUid.isEmpty()) {
            return Result.failure(IllegalArgumentException("In-Game Name and Game UID are required"))
        }

        val couponCode = couponCodeRaw.trim().uppercase()
        val validatedCoupon = if (couponCode.isNotEmpty() && match.entryFee > 0.0) {
            val res = validateMatchCoupon(couponCode, match.entryFee)
            if (res.isFailure) return Result.failure(res.exceptionOrNull()!!)
            res.getOrNull()
        } else null

        val discount = validatedCoupon?.computeBenefit(match.entryFee) ?: 0.0
        val payableFee = max(0.0, match.entryFee - discount)

        return try {
            val createdSlot = db.runTransaction { tx ->
                val matchRef = db.collection("tournaments").document(match.id)
                val matchSnap = tx.get(matchRef)
                val currentFilled = matchSnap.getLong("filledSlots") ?: 0L
                val maxSlots = matchSnap.getLong("maxSlots") ?: 0L
                val status = matchSnap.getString("status").orEmpty()
                if (status != "upcoming") throw IllegalStateException("Match is no longer open for registration")
                if (currentFilled >= maxSlots) throw IllegalStateException("All slots are filled!")

                val slotRef = matchRef.collection("slots").document(uid)
                if (tx.get(slotRef).exists()) throw IllegalStateException("You already joined this match")

                val userRef = db.collection("users").document(uid)
                val userSnap = tx.get(userRef)
                if (userSnap.getBoolean("banned") == true) throw IllegalStateException("Account suspended")

                val depositBal = userSnap.getDouble("depositBalance") ?: 0.0
                val winningBal = userSnap.getDouble("winningBalance") ?: 0.0
                val bonusBal = userSnap.getDouble("bonusBalance") ?: 0.0
                val displayName = userSnap.getString("displayName").orEmpty().ifBlank { "Player" }
                val matchesPlayed = userSnap.getLong("matchesPlayed") ?: 0L

                val maxBonusAllowed = if (bonusUsablePercent > 0L) (payableFee * bonusUsablePercent / 100.0) else 0.0
                val bonusUsed = min(bonusBal, maxBonusAllowed)
                val remainingAfterBonus = max(0.0, payableFee - bonusUsed)

                val depositUsed = min(depositBal, remainingAfterBonus)
                val remainingAfterDeposit = max(0.0, remainingAfterBonus - depositUsed)
                val winningUsed = min(winningBal, remainingAfterDeposit)

                if (depositUsed + winningUsed + bonusUsed + 0.001 < payableFee) {
                    throw IllegalStateException("Insufficient wallet balance. Need ₹${payableFee.toInt()}")
                }

                val couponRef = validatedCoupon?.let { db.collection("coupons").document(it.code) }
                val couponSnap = couponRef?.let { tx.get(it) }
                if (couponSnap != null && validatedCoupon != null) {
                    val usedCount = couponSnap.getLong("usedCount") ?: 0L
                    if (usedCount >= validatedCoupon.maxGlobalUses) {
                        throw IllegalStateException("Coupon max usage reached")
                    }
                }

                val newFilledCount = currentFilled + 1L
                val chosenSlot = preferredSlotNumber?.takeIf { it in 1L..maxSlots } ?: newFilledCount
                val slotPayload = mapOf(
                    "userId" to uid,
                    "matchId" to match.id,
                    "matchTitle" to match.title,
                    "displayName" to displayName,
                    "inGameName" to cleanIgn.take(60),
                    "gameUid" to cleanGameUid.take(60),
                    "squadMembers" to squadMembers.filter { it.isNotBlank() }.take(4),
                    "slotNumber" to chosenSlot,
                    "entryPaid" to payableFee,
                    "depositUsed" to depositUsed,
                    "winningUsed" to winningUsed,
                    "bonusUsed" to bonusUsed,
                    "couponCodeUsed" to (validatedCoupon?.code ?: ""),
                    "discountAmount" to discount,
                    "kills" to 0L,
                    "rank" to 0L,
                    "prizeWon" to 0.0,
                    "status" to "joined",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )

                tx.update(
                    matchRef,
                    mapOf(
                        "filledSlots" to newFilledCount,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                tx.set(slotRef, slotPayload)
                tx.set(userRef.collection("joined_matches").document(match.id), slotPayload)

                tx.update(
                    userRef,
                    mapOf(
                        "depositBalance" to (depositBal - depositUsed),
                        "winningBalance" to (winningBal - winningUsed),
                        "bonusBalance" to (bonusBal - bonusUsed),
                        "matchesPlayed" to (matchesPlayed + 1L),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )

                if (payableFee > 0.0) {
                    val txDoc = userRef.collection("transactions").document()
                    tx.set(
                        txDoc,
                        mapOf(
                            "userId" to uid,
                            "type" to "entry_fee",
                            "amount" to payableFee,
                            "direction" to "debit",
                            "walletBucket" to if (depositUsed > 0) "deposit" else "winning",
                            "title" to "Joined ${match.title}",
                            "subtitle" to "Slot #$chosenSlot • ${match.game}",
                            "referenceId" to match.id,
                            "status" to "completed",
                            "createdAt" to FieldValue.serverTimestamp()
                        )
                    )
                }

                if (couponRef != null && couponSnap != null && validatedCoupon != null) {
                    val prevUsed = couponSnap.getLong("usedCount") ?: 0L
                    val prevTotalDiscount = couponSnap.getDouble("totalDiscountGiven") ?: 0.0
                    tx.update(
                        couponRef,
                        mapOf(
                            "usedCount" to (prevUsed + 1L),
                            "totalDiscountGiven" to (prevTotalDiscount + discount),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                    val redRef = couponRef.collection("redemptions").document()
                    tx.set(
                        redRef,
                        mapOf(
                            "userId" to uid,
                            "userEmail" to auth.currentUser?.email.orEmpty(),
                            "code" to validatedCoupon.code,
                            "orderOrMatchId" to match.id,
                            "amount" to discount,
                            "createdAt" to FieldValue.serverTimestamp()
                        )
                    )
                }

                MatchSlot(
                    userId = uid,
                    matchId = match.id,
                    matchTitle = match.title,
                    displayName = displayName,
                    inGameName = cleanIgn,
                    gameUid = cleanGameUid,
                    squadMembers = squadMembers,
                    slotNumber = chosenSlot,
                    entryPaid = payableFee,
                    depositUsed = depositUsed,
                    winningUsed = winningUsed,
                    bonusUsed = bonusUsed,
                    couponCodeUsed = validatedCoupon?.code ?: "",
                    discountAmount = discount,
                    status = "joined"
                )
            }.await()

            Result.success(createdSlot)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "tournaments/${match.id}")
            Result.failure(e)
        }
    }
}
