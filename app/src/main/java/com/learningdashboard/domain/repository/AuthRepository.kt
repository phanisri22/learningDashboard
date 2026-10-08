package com.learningdashboard.domain.repository

import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    fun isLoggedIn(): Boolean

    /** Turns false when the session ends, by logout or because the server rejected the token. */
    fun observeIsLoggedIn(): Flow<Boolean>

    /** True once after the session expired, so the login screen can tell the user. */
    fun consumeSessionExpired(): Boolean

    suspend fun login(email: String, password: String): Result<Unit>

    suspend fun logout()
}
