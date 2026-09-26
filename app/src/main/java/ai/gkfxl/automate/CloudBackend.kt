package ai.gkfxl.automate

import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/** Supabase REST/Auth helper. Never use a service-role key in Android. Run calls off the main thread. */
class CloudBackend(
    private val baseUrl: String = BuildConfig.SUPABASE_URL.trimEnd('/'),
    private val anonKey: String = BuildConfig.SUPABASE_ANON_KEY
) {
    data class Session(val accessToken: String, val userId: String, val email: String?)

    fun signIn(email: String, password: String): Session {
        val json = request("POST", "/auth/v1/token?grant_type=password",
            JSONObject().put("email", email.trim()).put("password", password), null)
        val user = json.getJSONObject("user")
        return Session(json.getString("access_token"), user.getString("id"), user.optString("email"))
    }

    fun signUp(email: String, password: String): JSONObject =
        request("POST", "/auth/v1/signup", JSONObject().put("email", email.trim()).put("password", password), null)

    fun assistant(prompt: String, accessToken: String): String =
        request("POST", "/functions/v1/gemini-assistant", JSONObject().put("prompt", prompt), accessToken).optString("text")

    fun syncRules(accessToken: String, userId: String, rulesJson: JSONArray) {
        for (i in 0 until rulesJson.length()) {
            val rule = rulesJson.getJSONObject(i)
            val row = JSONObject().put("id", rule.getLong("id").toString())
                .put("user_id", userId).put("payload", rule)
            request("POST", "/rest/v1/automation_rules?on_conflict=id", row, accessToken,
                mapOf("Prefer" to "resolution=merge-duplicates,return=minimal"))
        }
    }

    fun log(accessToken: String, userId: String, eventType: String, details: JSONObject = JSONObject()) {
        val row = JSONObject().put("user_id", userId).put("event_type", eventType).put("details", details)
        request("POST", "/rest/v1/automation_history", row, accessToken, mapOf("Prefer" to "return=minimal"))
    }

    private fun request(method: String, path: String, body: JSONObject?, token: String?,
                        extraHeaders: Map<String, String> = emptyMap()): JSONObject {
        require(baseUrl.startsWith("https://") && anonKey.isNotBlank()) {
            "Set SUPABASE_URL and SUPABASE_ANON_KEY in Gradle properties."
        }
        val connection = URL(baseUrl + path).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = method
            connection.connectTimeout = 15000
            connection.readTimeout = 20000
            connection.setRequestProperty("apikey", anonKey)
            connection.setRequestProperty("Authorization", "Bearer " + (token ?: anonKey))
            connection.setRequestProperty("Content-Type", "application/json")
            extraHeaders.forEach { (key, value) -> connection.setRequestProperty(key, value) }
            if (body != null) {
                connection.doOutput = true
                connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            }
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val response = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (code !in 200..299) throw IllegalStateException("Supabase request failed ($code): " + response.take(500))
            return if (response.isBlank()) JSONObject() else JSONObject(response)
        } finally {
            connection.disconnect()
        }
    }
}
