package com.learningdashboard.data.remote.mock

import android.content.res.AssetManager
import com.learningdashboard.core.network.ConnectivityObserver
import com.learningdashboard.data.remote.dto.ErrorDto
import com.learningdashboard.data.remote.dto.LoginRequestDto
import com.learningdashboard.data.remote.dto.LoginResponseDto
import com.learningdashboard.data.remote.dto.UserDto
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import java.io.FileNotFoundException
import java.io.IOException
import java.util.UUID

/**
 * Stands in for a backend: answers requests from JSON files in assets/mock.
 * Fails like a real HTTP call when the device is offline, so the offline path is exercised for real.
 * Remove it from the OkHttp client to point the app at a real server.
 */
class MockApiInterceptor(
    private val assets: AssetManager,
    private val connectivity: ConnectivityObserver,
    private val json: Json,
    private val latencyMillis: Long = 800,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        if (!connectivity.isCurrentlyOnline()) throw IOException("Unable to resolve host: no network")
        Thread.sleep(latencyMillis)

        val segments = request.url.pathSegments
        if (request.method == "POST" && segments == listOf("auth", "login")) return login(request)

        val userId = request.userId()
        if (userId == null || userId == EXPIRED_USER) {
            return request.respond(401, json.encodeToString(ErrorDto("Session expired")))
        }

        return when {
            request.method == "GET" && segments == listOf("courses") -> when (userId) {
                EMPTY_USER -> request.respond(200, "[]")
                ERROR_USER -> request.respond(500, json.encodeToString(ErrorDto("Internal server error")))
                else -> request.respond(200, readAsset("mock/courses.json"))
            }

            request.method == "GET" && segments.size == 2 && segments[0] == "courses" ->
                try {
                    request.respond(200, readAsset("mock/course_${segments[1]}.json"))
                } catch (e: FileNotFoundException) {
                    request.respond(404, json.encodeToString(ErrorDto("Course not found")))
                }

            request.method == "POST" && segments.size == 5 && segments[0] == "courses" &&
                segments[2] == "lessons" && segments[4] == "complete" -> request.respond(204, "")

            else -> request.respond(404, json.encodeToString(ErrorDto("Not found")))
        }
    }

    private fun login(request: Request): Response {
        val body = Buffer().also { request.body?.writeTo(it) }.readUtf8()
        val credentials = json.decodeFromString<LoginRequestDto>(body)
        val email = credentials.email.lowercase()
        val userId = DEMO_USERS[email]
        return if (userId != null && credentials.password == DEMO_PASSWORD) {
            val response = LoginResponseDto(token = "mock.$userId.${UUID.randomUUID()}", user = UserDto(userId, email))
            request.respond(200, json.encodeToString(response))
        } else {
            request.respond(401, json.encodeToString(ErrorDto("Invalid email or password")))
        }
    }

    /** Mock tokens look like "mock.<userId>.<random>"; anything else is rejected. */
    private fun Request.userId(): String? {
        val parts = header("Authorization")?.removePrefix("Bearer ")?.split('.') ?: return null
        if (parts.size != 3 || parts[0] != "mock") return null
        return parts[1].takeIf { it in DEMO_USERS.values }
    }

    private fun readAsset(path: String): String =
        assets.open(path).bufferedReader().use { it.readText() }

    private fun Request.respond(code: Int, body: String): Response =
        Response.Builder()
            .request(this)
            .protocol(Protocol.HTTP_1_1)
            .code(code)
            .message(if (code in 200..299) "OK" else "Error")
            .body(body.toResponseBody("application/json".toMediaType()))
            .build()

    private companion object {
        const val DEMO_PASSWORD = "password123"

        // Test accounts for showing the empty, server-error and session-expired states.
        const val EMPTY_USER = "u_empty"
        const val ERROR_USER = "u_error"
        const val EXPIRED_USER = "u_expired"

        val DEMO_USERS = mapOf(
            "demo@learn.com" to "u_1001",
            "alex@learn.com" to "u_1002",
            "empty@learn.com" to EMPTY_USER,
            "error@learn.com" to ERROR_USER,
            "expired@learn.com" to EXPIRED_USER,
        )
    }
}
