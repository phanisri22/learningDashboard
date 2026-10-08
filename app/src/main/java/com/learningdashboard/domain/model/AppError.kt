package com.learningdashboard.domain.model

/** Errors the UI knows how to present. Data-layer exceptions are mapped into these. */
sealed class AppError(cause: Throwable? = null) : Exception(cause) {
    class Network(cause: Throwable? = null) : AppError(cause)
    class InvalidCredentials : AppError()
    class Unauthorized : AppError()
    class NotFound : AppError()
    class Server(val code: Int) : AppError()
    class Unknown(cause: Throwable? = null) : AppError(cause)
}

fun Throwable.asAppError(): AppError = this as? AppError ?: AppError.Unknown(this)
