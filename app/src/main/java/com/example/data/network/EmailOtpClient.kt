package com.example.data.network

import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.security.SecureRandom
import java.util.concurrent.TimeUnit

data class EmailOtpSendResult(
    val sent: Boolean,
    val missingConfig: Boolean = false,
    val message: String = ""
)

class EmailOtpClient {
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    private val secureRandom = SecureRandom()
    private var activeEmail: String = ""
    private var activeCode: String = ""
    private var expiresAtMillis: Long = 0L

    fun resolveResendKey(adminOverrideKey: String): String {
        val override = adminOverrideKey.trim()
        if (override.isNotEmpty()) return override
        val buildKey = runCatching { BuildConfig.RESEND_API_KEY.trim() }.getOrDefault("")
        return if (buildKey == "MY_RESEND_API_KEY") "" else buildKey
    }

    suspend fun sendVerificationCode(
        email: String,
        username: String,
        adminOverrideKey: String = ""
    ): EmailOtpSendResult = withContext(Dispatchers.IO) {
        val cleanEmail = email.trim()
        if (!cleanEmail.contains("@") || !cleanEmail.contains(".")) {
            return@withContext EmailOtpSendResult(sent = false, message = "Enter a valid email address.")
        }

        val apiKey = resolveResendKey(adminOverrideKey)
        if (apiKey.isBlank()) {
            return@withContext EmailOtpSendResult(
                sent = false,
                missingConfig = true,
                message = "Email OTP service (RESEND_API_KEY) is not configured in AI Studio Secrets or Admin Settings. You can verify directly using Google Identity below."
            )
        }

        val code = (100000 + secureRandom.nextInt(900000)).toString()
        val html = """
            <div style="font-family:sans-serif;background:#07090E;color:#F3F7FF;padding:24px;border-radius:12px;">
              <h2 style="color:#00F0FF;margin:0 0 8px;">AHXAN ARENA VERIFICATION</h2>
              <p>Hello ${username.ifBlank { "Warrior" }},</p>
              <p>Use this 6-digit verification code to complete your Ahxan Arena registration:</p>
              <div style="font-size:28px;font-weight:bold;letter-spacing:6px;color:#00FF9D;padding:14px 20px;background:#151D2E;border:1px solid #00F0FF;border-radius:8px;display:inline-block;">$code</div>
              <p style="color:#7B89A6;font-size:12px;margin-top:16px;">This code expires in 10 minutes.</p>
            </div>
        """.trimIndent()

        val payload = JSONObject().apply {
            put("from", "Ahxan Arena <onboarding@resend.dev>")
            put("to", JSONArray().put(cleanEmail))
            put("subject", "Ahxan Arena Verification Code: $code")
            put("html", html)
        }

        try {
            val req = Request.Builder()
                .url("https://api.resend.com/emails")
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Content-Type", "application/json")
                .post(payload.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
                .build()

            httpClient.newCall(req).execute().use { resp ->
                val raw = resp.body?.string().orEmpty()
                if (resp.isSuccessful) {
                    activeEmail = cleanEmail.lowercase()
                    activeCode = code
                    expiresAtMillis = System.currentTimeMillis() + 10 * 60 * 1000L
                    EmailOtpSendResult(
                        sent = true,
                        message = "Verification code sent to $cleanEmail. Check your inbox or spam folder."
                    )
                } else {
                    val errMsg = runCatching { JSONObject(raw).optString("message") }.getOrDefault("")
                    EmailOtpSendResult(
                        sent = false,
                        message = errMsg.ifBlank { "Email service rejected request (HTTP ${resp.code})." }
                    )
                }
            }
        } catch (e: Exception) {
            EmailOtpSendResult(
                sent = false,
                message = e.localizedMessage ?: "Network error while sending email code."
            )
        }
    }

    fun verifyCode(email: String, enteredCode: String): Result<Unit> {
        val cleanEmail = email.trim().lowercase()
        val cleanCode = enteredCode.trim()
        if (activeEmail != cleanEmail || activeCode.isBlank()) {
            return Result.failure(IllegalStateException("Request a verification code for $email first."))
        }
        if (System.currentTimeMillis() > expiresAtMillis) {
            return Result.failure(IllegalStateException("Verification code expired. Please resend."))
        }
        if (cleanCode != activeCode) {
            return Result.failure(IllegalArgumentException("Incorrect verification code."))
        }
        return Result.success(Unit)
    }
}
