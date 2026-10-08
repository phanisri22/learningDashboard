package com.learningdashboard.data.remote

import com.learningdashboard.data.auth.SessionStore
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(private val sessionStore: SessionStore) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val token = sessionStore.getToken() ?: return chain.proceed(chain.request())
        val request = chain.request().newBuilder()
            .header("Authorization", "Bearer $token")
            .build()
        val response = chain.proceed(request)
        // The server rejected the token: end the session so the app returns to login.
        if (response.code == 401) sessionStore.expire(token)
        return response
    }
}
