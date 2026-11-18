package com.example.data.network

import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class PayZapiCreateResponse(
    val success: Boolean,
    val orderId: String = "",
    val qrCodeUrl: String = "",
    val upiIntentUrl: String = "",
    val checkoutUrl: String = "",
    val errorMessage: String = ""
)

data class PayZapiCheckResponse(
    val status: String, // "completed", "pending", "expired", "failed"
    val utr: String = "",
    val amount: Double = 0.0,
    val message: String = ""
)

class PayZapiClient {
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build()

    private val baseUrl = "https://payzapi.in/api/v1"

    fun resolveApiKey(adminOverrideKey: String): String {
        val trimmedOverride = adminOverrideKey.trim()
        if (trimmedOverride.isNotEmpty()) return trimmedOverride
        val buildKey = BuildConfig.PAYZAPI_API_KEY.trim()
        return if (buildKey == "MY_PAYZAPI_API_KEY") "" else buildKey
    }

    suspend fun createOrder(
        apiKey: String,
        amount: Double,
        provider: String,
        clientOrderId: String,
        customerName: String,
        customerEmail: String,
        description: String,
        redirectUrl: String
    ): PayZapiCreateResponse = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext PayZapiCreateResponse(
                success = false,
                errorMessage = "PayZapi API key not configured. Add PAYZAPI_API_KEY in AI Studio Secrets or Admin Gateway Settings."
            )
        }
        try {
            val payload = JSONObject().apply {
                put("amount", amount)
                put("payment_provider", provider)
                put("client_order_id", clientOrderId)
                put("customer_name", customerName.ifBlank { "Ahxan Player" })
                put("customer_email", customerEmail.ifBlank { "player@ahxanarena.app" })
                put("description", description)
                put("redirect_url", redirectUrl)
                put("expiry_minutes", 15)
            }
            val request = Request.Builder()
                .url("$baseUrl/create")
                .addHeader("Content-Type", "application/json")
                .addHeader("x-api-key", apiKey)
                .post(payload.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
                .build()

            httpClient.newCall(request).execute().use { response ->
                val raw = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    val errMsg = runCatching {
                        val j = JSONObject(raw)
                        j.optString("message").ifBlank { j.optString("error") }
                    }.getOrNull().orEmpty().ifBlank { "PayZapi HTTP ${response.code}" }
                    return@withContext PayZapiCreateResponse(success = false, errorMessage = errMsg)
                }

                val root = JSONObject(raw)
                val data = root.optJSONObject("data") ?: root
                val orderId = data.optString("order_id")
                    .ifBlank { data.optString("orderId") }
                    .ifBlank { root.optString("order_id") }
                if (orderId.isBlank()) {
                    val msg = root.optString("message").ifBlank { "Invalid PayZapi response: missing order_id" }
                    return@withContext PayZapiCreateResponse(success = false, errorMessage = msg)
                }

                val qr = data.optString("qr_code")
                    .ifBlank { data.optString("qr_code_url") }
                    .ifBlank { data.optString("qr_url") }
                    .ifBlank { data.optString("qrCode") }
                val upiIntent = data.optString("upi_intent")
                    .ifBlank { data.optString("upi_link") }
                    .ifBlank { data.optString("deep_link") }
                val checkoutUrl = data.optString("payment_url")
                    .ifBlank { data.optString("checkout_url") }
                    .ifBlank { "https://payzapi.in/pay/$orderId" }

                PayZapiCreateResponse(
                    success = true,
                    orderId = orderId,
                    qrCodeUrl = qr,
                    upiIntentUrl = upiIntent,
                    checkoutUrl = checkoutUrl
                )
            }
        } catch (e: Exception) {
            PayZapiCreateResponse(
                success = false,
                errorMessage = e.localizedMessage ?: "Network error contacting PayZapi"
            )
        }
    }

    suspend fun checkOrder(
        apiKey: String,
        payzapiOrderId: String
    ): PayZapiCheckResponse = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext PayZapiCheckResponse(
                status = "error",
                message = "PayZapi API key not configured."
            )
        }
        try {
            val request = Request.Builder()
                .url("$baseUrl/check?order_id=$payzapiOrderId")
                .addHeader("x-api-key", apiKey)
                .get()
                .build()

            httpClient.newCall(request).execute().use { response ->
                val raw = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    val msg = runCatching {
                        val j = JSONObject(raw)
                        j.optString("message").ifBlank { j.optString("error") }
                    }.getOrNull().orEmpty().ifBlank { "PayZapi check HTTP ${response.code}" }
                    return@withContext PayZapiCheckResponse(status = "error", message = msg)
                }

                val root = JSONObject(raw)
                val data = root.optJSONObject("data") ?: root
                val rawStatus = data.optString("status")
                    .ifBlank { root.optString("status") }
                    .lowercase()
                val normalized = when (rawStatus) {
                    "completed", "success", "paid" -> "completed"
                    "expired" -> "expired"
                    "failed", "cancelled" -> "failed"
                    else -> "pending"
                }
                val utr = data.optString("utr")
                    .ifBlank { data.optString("transaction_id") }
                    .ifBlank { data.optString("bank_ref") }
                val amount = data.optDouble("amount", 0.0)
                val msg = root.optString("message")

                PayZapiCheckResponse(
                    status = normalized,
                    utr = utr,
                    amount = amount,
                    message = msg
                )
            }
        } catch (e: Exception) {
            PayZapiCheckResponse(
                status = "error",
                message = e.localizedMessage ?: "Failed to check PayZapi status"
            )
        }
    }
}
