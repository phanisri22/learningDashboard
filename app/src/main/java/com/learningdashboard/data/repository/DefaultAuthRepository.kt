package com.learningdashboard.data.repository

import com.learningdashboard.data.auth.Session
import com.learningdashboard.data.auth.SessionStore
import com.learningdashboard.data.remote.LearningApi
import com.learningdashboard.data.remote.dto.LoginRequestDto
import com.learningdashboard.data.remote.resultOf
import com.learningdashboard.domain.model.AppError
import com.learningdashboard.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import retrofit2.HttpException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultAuthRepository @Inject constructor(
    private val api: LearningApi,
    private val sessionStore: SessionStore,
) : AuthRepository {

    override fun isLoggedIn(): Boolean = sessionStore.getToken() != null

    override fun observeIsLoggedIn(): Flow<Boolean> =
        sessionStore.userId.map { it != null }.distinctUntilChanged()

    override fun consumeSessionExpired(): Boolean = sessionStore.consumeExpired()

    override suspend fun login(email: String, password: String): Result<Unit> = resultOf {
        val response = try {
            api.login(LoginRequestDto(email = email, password = password))
        } catch (e: HttpException) {
            throw if (e.code() == 401) AppError.InvalidCredentials() else e
        }
        sessionStore.save(Session(token = response.token, userId = response.user.id))
    }

    // Cached data is kept: it is scoped to the user and reappears on their next login.
    override suspend fun logout() {
        sessionStore.clear()
    }
}
