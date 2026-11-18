package com.example.data.repository

import android.os.Build
import com.example.data.model.Coupon
import com.example.data.model.DepositOrder
import com.example.data.model.GatewayConfig
import com.example.data.model.OperationType
import com.example.data.model.ReferralConfig
import com.example.data.model.UserProfile
import com.example.data.model.WalletTransaction
import com.example.data.model.WithdrawalRequest
import com.example.data.model.handleFirestoreError
import com.example.data.network.PayZapiClient
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
import kotlin.random.Random

class ArenaWalletRepository(
    private val db: FirebaseFirestore,
    private val payZapiClient: PayZapiClient,
    private val userRepo: ArenaUserRepository
) {
    private val auth = Firebase.auth

    private fun requireUserId(): String =
        auth.currentUser?.uid ?: throw IllegalStateException("User must be signed in.")

    fun observeTransactions(uid: String): Flow<List<WalletTransaction>> =
        db.collection("users").document(uid).collection("transactions")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(80)
            .snapshots()
            .map { snap -> snap.toObjects(WalletTransaction::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
            .catch { error ->
                val fe = generateSequence(error) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
                if (fe == null && error is CancellationException) throw error
                if (auth.currentUser?.uid == uid) {
                    handleFirestoreError(fe ?: error, OperationType.LIST, "users/$uid/transactions")
                }
                emit(emptyList())
            }

    fun observeMyDeposits(uid: String): Flow<List<DepositOrder>> =
        db.collection("deposits")
            .whereEqualTo("userId", uid)
            .snapshots()
            .map { snap ->
                snap.toObjects(DepositOrder::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt?.toDate()?.time ?: 0L }
            }
            .catch { error ->
                val fe = generateSequence(error) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
                if (fe == null && error is CancellationException) throw error
                if (auth.currentUser?.uid == uid) {
                    handleFirestoreError(fe ?: error, OperationType.LIST, "deposits")
                }
                emit(emptyList())
            }

    fun observeMyWithdrawals(uid: String): Flow<List<WithdrawalRequest>> =
        db.collection("withdrawals")
            .whereEqualTo("userId", uid)
            .snapshots()
            .map { snap ->
                snap.toObjects(WithdrawalRequest::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt?.toDate()?.time ?: 0L }
            }
            .catch { error ->
                val fe = generateSequence(error) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
                if (fe == null && error is CancellationException) throw error
                if (auth.currentUser?.uid == uid) {
                    handleFirestoreError(fe ?: error, OperationType.LIST, "withdrawals")
                }
                emit(emptyList())
            }

    fun observeActiveCoupons(): Flow<List<Coupon>> =
        db.collection("coupons")
            .snapshots()
            .map { snap ->
                snap.toObjects(Coupon::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .filter { it.active }
            }
            .catch { error ->
                val fe = generateSequence(error) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
                if (fe == null && error is CancellationException) throw error
                handleFirestoreError(fe ?: error, OperationType.LIST, "coupons")
                emit(emptyList())
            }

    suspend fun validateDepositPromo(codeRaw: String, amount: Double): Result<Coupon> {
        val uid = requireUserId()
        val code = codeRaw.trim().uppercase()
        if (code.isBlank()) return Result.failure(IllegalArgumentException("Enter a promo code"))
        return try {
            val snap = db.collection("coupons").document(code).get().await()
            if (!snap.exists()) return Result.failure(IllegalArgumentException("Promo code '$code' not found"))
            val coupon = snap.toObject(Coupon::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                ?: return Result.failure(IllegalArgumentException("Invalid promo code"))
            if (coupon.couponType != "deposit_bonus") {
                return Result.failure(IllegalArgumentException("This coupon is for Match Entry Discount only"))
            }
            if (!coupon.active) return Result.failure(IllegalArgumentException("Promo code is currently paused"))
            if (coupon.usedCount >= coupon.maxGlobalUses) return Result.failure(IllegalArgumentException("Promo limit reached"))
            if (coupon.expiresAtMillis > 0L && System.currentTimeMillis() > coupon.expiresAtMillis) {
                return Result.failure(IllegalArgumentException("Promo code expired"))
            }
            if (amount < coupon.minAmount) {
                return Result.failure(IllegalArgumentException("Minimum deposit ₹${coupon.minAmount.toInt()} required"))
            }
            val redemptions = db.collection("coupons").document(code).collection("redemptions")
                .whereEqualTo("userId", uid).get().await()
            if (redemptions.size() >= coupon.maxPerUserUses) {
                return Result.failure(IllegalArgumentException("You already redeemed '$code'"))
            }
            Result.success(coupon)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, "coupons/$code")
            Result.failure(e)
        }
    }

    suspend fun createPayZapiDeposit(
        user: UserProfile,
        amount: Double,
        provider: String,
        promoCodeRaw: String,
        gatewayConfig: GatewayConfig
    ): Result<DepositOrder> {
        val uid = requireUserId()
        if (gatewayConfig.depositMaintenance) {
            return Result.failure(IllegalStateException("Deposits are temporarily paused for maintenance"))
        }
        if (amount < gatewayConfig.minDeposit || amount > gatewayConfig.maxDeposit) {
            return Result.failure(
                IllegalArgumentException("Deposit must be between ₹${gatewayConfig.minDeposit.toInt()} and ₹${gatewayConfig.maxDeposit.toInt()}")
            )
        }

        val promoCode = promoCodeRaw.trim().uppercase()
        var expectedBonus = 0.0
        if (promoCode.isNotEmpty()) {
            val promoRes = validateDepositPromo(promoCode, amount)
            if (promoRes.isFailure) return Result.failure(promoRes.exceptionOrNull()!!)
            expectedBonus = promoRes.getOrNull()?.computeBenefit(amount) ?: 0.0
        }

        val clientOrderId = "AHXAN_${System.currentTimeMillis()}_${Random.nextInt(1000, 9999)}"
        val apiKey = payZapiClient.resolveApiKey(gatewayConfig.payzapiApiKeyOverride)
        val createRes = payZapiClient.createOrder(
            apiKey = apiKey,
            amount = amount,
            provider = provider,
            clientOrderId = clientOrderId,
            customerName = user.displayName,
            customerEmail = user.email,
            description = "Ahxan Arena Wallet Deposit",
            redirectUrl = "ahxanarena://wallet?check_order=$clientOrderId"
        )
        if (!createRes.success) {
            return Result.failure(IllegalStateException(createRes.errorMessage))
        }

        val depositRef = db.collection("deposits").document(clientOrderId)
        val expiryMillis = System.currentTimeMillis() + 15 * 60 * 1000L
        return try {
            val payload = mapOf(
                "userId" to uid,
                "userEmail" to user.email.take(200),
                "userName" to user.displayName.take(80),
                "clientOrderId" to clientOrderId,
                "payzapiOrderId" to createRes.orderId,
                "amount" to amount,
                "provider" to provider,
                "promoCode" to promoCode,
                "expectedPromoBonus" to expectedBonus,
                "promoBonusCredited" to 0.0,
                "status" to "pending",
                "utr" to "",
                "qrCodeUrl" to createRes.qrCodeUrl,
                "upiIntentUrl" to createRes.upiIntentUrl,
                "checkoutUrl" to createRes.checkoutUrl,
                "expiryMillis" to expiryMillis,
                "paidAtMillis" to 0L,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            depositRef.set(payload).await()
            Result.success(
                DepositOrder(
                    id = clientOrderId,
                    userId = uid,
                    userEmail = user.email,
                    userName = user.displayName,
                    clientOrderId = clientOrderId,
                    payzapiOrderId = createRes.orderId,
                    amount = amount,
                    provider = provider,
                    promoCode = promoCode,
                    expectedPromoBonus = expectedBonus,
                    status = "pending",
                    qrCodeUrl = createRes.qrCodeUrl,
                    upiIntentUrl = createRes.upiIntentUrl,
                    checkoutUrl = createRes.checkoutUrl,
                    expiryMillis = expiryMillis
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, depositRef.path)
            Result.failure(e)
        }
    }

    suspend fun verifyPayZapiDeposit(
        deposit: DepositOrder,
        gatewayConfig: GatewayConfig,
        referralConfig: ReferralConfig
    ): Result<String> {
        val uid = requireUserId()
        val apiKey = payZapiClient.resolveApiKey(gatewayConfig.payzapiApiKeyOverride)
        val checkRes = payZapiClient.checkOrder(apiKey, deposit.payzapiOrderId)

        return when (checkRes.status) {
            "completed" -> {
                try {
                    val utr = checkRes.utr.ifBlank { "PZ_${deposit.payzapiOrderId.takeLast(8)}" }
                    val nowMillis = System.currentTimeMillis()
                    var creditedBonus = 0.0
                    var shouldTriggerReferral = false
                    var referrerUid = ""
                    var referrerCode = ""

                    db.runTransaction { tx ->
                        val depRef = db.collection("deposits").document(deposit.id)
                        val depSnap = tx.get(depRef)
                        if (depSnap.getString("status") != "pending") {
                            throw IllegalStateException("ALREADY_CREDITED")
                        }
                        val depUserId = depSnap.getString("userId") ?: uid
                        val amount = depSnap.getDouble("amount") ?: deposit.amount
                        val promoCode = depSnap.getString("promoCode").orEmpty()
                        val expectedBonus = depSnap.getDouble("expectedPromoBonus") ?: 0.0

                        val userRef = db.collection("users").document(depUserId)
                        val userSnap = tx.get(userRef)
                        val oldDeposit = userSnap.getDouble("depositBalance") ?: 0.0
                        val oldBonus = userSnap.getDouble("bonusBalance") ?: 0.0
                        val alreadyQualified = userSnap.getBoolean("referralQualified") ?: false
                        referrerUid = userSnap.getString("referredByUid").orEmpty()
                        referrerCode = userSnap.getString("referredByCode").orEmpty()

                        var promoBonusToCredit = 0.0
                        val couponRef = if (promoCode.isNotBlank() && expectedBonus > 0.0) {
                            db.collection("coupons").document(promoCode)
                        } else null
                        val couponSnap = couponRef?.let { tx.get(it) }
                        if (couponSnap != null && couponSnap.exists()) {
                            val active = couponSnap.getBoolean("active") ?: false
                            val used = couponSnap.getLong("usedCount") ?: 0L
                            val maxUses = couponSnap.getLong("maxGlobalUses") ?: 0L
                            if (active && used < maxUses) {
                                promoBonusToCredit = expectedBonus
                            }
                        }
                        creditedBonus = promoBonusToCredit

                        shouldTriggerReferral = !alreadyQualified &&
                            referrerUid.isNotBlank() &&
                            referralConfig.enabled &&
                            referralConfig.triggerRule == "first_deposit" &&
                            amount >= referralConfig.minDepositThreshold

                        tx.update(
                            depRef,
                            mapOf(
                                "status" to "completed",
                                "utr" to utr,
                                "promoBonusCredited" to promoBonusToCredit,
                                "paidAtMillis" to nowMillis,
                                "updatedAt" to FieldValue.serverTimestamp()
                            )
                        )

                        tx.update(
                            userRef,
                            mapOf(
                                "depositBalance" to (oldDeposit + amount),
                                "bonusBalance" to (oldBonus + promoBonusToCredit),
                                "referralQualified" to (alreadyQualified || shouldTriggerReferral),
                                "updatedAt" to FieldValue.serverTimestamp()
                            )
                        )

                        val depTxRef = userRef.collection("transactions").document()
                        tx.set(
                            depTxRef,
                            mapOf(
                                "userId" to depUserId,
                                "type" to "deposit",
                                "amount" to amount,
                                "direction" to "credit",
                                "walletBucket" to "deposit",
                                "title" to "PayZapi Deposit (${deposit.provider.uppercase()})",
                                "subtitle" to "UTR: $utr",
                                "referenceId" to deposit.payzapiOrderId,
                                "status" to "completed",
                                "createdAt" to FieldValue.serverTimestamp()
                            )
                        )

                        if (promoBonusToCredit > 0.0 && couponRef != null && couponSnap != null) {
                            val prevUsed = couponSnap.getLong("usedCount") ?: 0L
                            val prevTotal = couponSnap.getDouble("totalDiscountGiven") ?: 0.0
                            tx.update(
                                couponRef,
                                mapOf(
                                    "usedCount" to (prevUsed + 1L),
                                    "totalDiscountGiven" to (prevTotal + promoBonusToCredit),
                                    "updatedAt" to FieldValue.serverTimestamp()
                                )
                            )
                            val redRef = couponRef.collection("redemptions").document()
                            tx.set(
                                redRef,
                                mapOf(
                                    "userId" to depUserId,
                                    "userEmail" to (depSnap.getString("userEmail") ?: ""),
                                    "code" to promoCode,
                                    "orderOrMatchId" to deposit.id,
                                    "amount" to promoBonusToCredit,
                                    "createdAt" to FieldValue.serverTimestamp()
                                )
                            )
                            val bonusTxRef = userRef.collection("transactions").document()
                            tx.set(
                                bonusTxRef,
                                mapOf(
                                    "userId" to depUserId,
                                    "type" to "promo_bonus",
                                    "amount" to promoBonusToCredit,
                                    "direction" to "credit",
                                    "walletBucket" to "bonus",
                                    "title" to "Promo Bonus ($promoCode)",
                                    "subtitle" to "Deposit offer credited",
                                    "referenceId" to deposit.id,
                                    "status" to "completed",
                                    "createdAt" to FieldValue.serverTimestamp()
                                )
                            )
                        }
                    }.await()

                    if (shouldTriggerReferral) {
                        val deviceFp = "${Build.BOARD}_${Build.MODEL}_${Build.BRAND}".take(60)
                        userRepo.qualifyReferralOnTrigger(
                            referredUid = deposit.userId,
                            referrerUid = referrerUid,
                            referrerCode = referrerCode,
                            refConfig = referralConfig,
                            deviceFp = deviceFp,
                            depositAmount = deposit.amount
                        )
                    }

                    val bonusMsg = if (creditedBonus > 0.0) " + ₹${creditedBonus.toInt()} Bonus" else ""
                    Result.success("Payment Confirmed! UTR: $utr • ₹${deposit.amount.toInt()}$bonusMsg credited")
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    if (e.message == "ALREADY_CREDITED") {
                        Result.success("Deposit already verified and credited!")
                    } else {
                        handleFirestoreError(e, OperationType.UPDATE, "deposits/${deposit.id}")
                        Result.failure(e)
                    }
                }
            }
            "expired", "failed" -> {
                runCatching {
                    db.collection("deposits").document(deposit.id).update(
                        mapOf(
                            "status" to checkRes.status,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                }
                Result.failure(IllegalStateException("Order ${checkRes.status}. Please create a new deposit order."))
            }
            "pending" -> Result.failure(
                IllegalStateException("Payment not confirmed yet. Wait a few seconds and click Check Payment again.")
            )
            else -> Result.failure(
                IllegalStateException(checkRes.message.ifBlank { "Unable to verify payment right now" })
            )
        }
    }

    suspend fun requestWithdrawal(
        user: UserProfile,
        method: String,
        destination: String,
        grossAmount: Double,
        gatewayConfig: GatewayConfig
    ): Result<Unit> {
        val uid = requireUserId()
        if (gatewayConfig.payoutsPaused) {
            return Result.failure(IllegalStateException("Withdrawals are temporarily paused by Admin"))
        }
        if (grossAmount < gatewayConfig.minWithdrawal || grossAmount > gatewayConfig.maxWithdrawal) {
            return Result.failure(
                IllegalArgumentException("Withdrawal must be between ₹${gatewayConfig.minWithdrawal.toInt()} and ₹${gatewayConfig.maxWithdrawal.toInt()}")
            )
        }
        val cleanDest = destination.trim()
        if (cleanDest.length < 4) {
            return Result.failure(IllegalArgumentException("Enter a valid UPI ID or Binance BEP-20 address"))
        }

        val fee = (grossAmount * gatewayConfig.withdrawalFeePercent / 100.0)
        val net = (grossAmount - fee).coerceAtLeast(0.0)
        val wdRef = db.collection("withdrawals").document()

        return try {
            db.runTransaction { tx ->
                val userRef = db.collection("users").document(uid)
                val snap = tx.get(userRef)
                val winningBal = snap.getDouble("winningBalance") ?: 0.0
                if (winningBal + 0.001 < grossAmount) {
                    throw IllegalStateException("Insufficient Winning Balance (Available: ₹${winningBal.toInt()})")
                }
                tx.update(
                    userRef,
                    mapOf(
                        "winningBalance" to (winningBal - grossAmount).coerceAtLeast(0.0),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                tx.set(
                    wdRef,
                    mapOf(
                        "userId" to uid,
                        "userName" to user.displayName.take(80),
                        "userEmail" to user.email.take(200),
                        "method" to method,
                        "destination" to cleanDest.take(160),
                        "grossAmount" to grossAmount,
                        "feeAmount" to fee,
                        "netAmount" to net,
                        "status" to "pending",
                        "utrOrTxHash" to "",
                        "rejectReason" to "",
                        "processedBy" to "",
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                val txRef = userRef.collection("transactions").document()
                tx.set(
                    txRef,
                    mapOf(
                        "userId" to uid,
                        "type" to "withdrawal",
                        "amount" to grossAmount,
                        "direction" to "debit",
                        "walletBucket" to "winning",
                        "title" to "Withdrawal Request ($method)",
                        "subtitle" to "To $cleanDest • Net ₹${net.toInt()}",
                        "referenceId" to wdRef.id,
                        "status" to "pending",
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )
            }.await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, wdRef.path)
            Result.failure(e)
        }
    }
}
