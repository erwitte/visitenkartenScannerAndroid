import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import okhttp3.*
import com.google.gson.*
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

suspend fun extractData(textToExtractFrom: String): String = suspendCancellableCoroutine { continuation ->
    val apiKey = ""
    val url = "https://api.openai.com/v1/chat/completions"

    val jsonRequest = buildJsonObject {
        put("model", "gpt-4")
        putJsonArray("messages") {
            addJsonObject {
                put("role", "system")
                put("content", "Extrahiere den Namen des Unternehmens, die E-Mail, die Telefonnummer und die Anschrift. " +
                        "Antworte nur mit den Daten in der genannten Reihenfolge getrennt durch ;")
            }
            addJsonObject {
                put("role", "user")
                put("content", textToExtractFrom)
            }
        }
        put("max_tokens", 100)
    }

    val jsonString = Json.encodeToString(JsonElement.serializer(), jsonRequest)
    val mediaType = "application/json".toMediaType()
    val requestBody = jsonString.toRequestBody(mediaType)

    val client = OkHttpClient()
    val request = Request.Builder()
        .url(url)
        .addHeader("Authorization", "Bearer $apiKey")
        .addHeader("Content-Type", "application/json")
        .post(requestBody)
        .build()

    client.newCall(request).enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            continuation.resume("Failed to make API call: ${e.message}")
        }

        override fun onResponse(call: Call, response: Response) {
            val jsonResponse = response.body?.string()
            if (jsonResponse != null) {
                try {
                    val jsonObject = JsonParser.parseString(jsonResponse).asJsonObject
                    if (jsonObject.has("choices")) {
                        val content = jsonObject.getAsJsonArray("choices")
                            .get(0).asJsonObject
                            .getAsJsonObject("message")
                            .get("content").asString
                        continuation.resume(content)
                    } else if (jsonObject.has("error")) {
                        val error = jsonObject.getAsJsonObject("error")
                        continuation.resume("API Error: ${error.get("message").asString}")
                    } else {
                        continuation.resume("Unexpected response structure: $jsonResponse")
                    }
                } catch (e: Exception) {
                    continuation.resume("Failed to parse OpenAI response: ${e.message}")
                }
            } else {
                continuation.resume("Response body is null")
            }
        }
    })
}
