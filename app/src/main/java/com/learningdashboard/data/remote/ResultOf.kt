package com.learningdashboard.data.remote

import com.learningdashboard.domain.model.AppError
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException
import retrofit2.HttpException
import java.io.IOException

/** Runs [block] and maps failures to [AppError]. Never swallows coroutine cancellation. */
internal suspend inline fun <T> resultOf(block: () -> T): Result<T> =
    try {
        Result.success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Throwable) {
        Result.failure(e.toAppError())
    }

internal fun Throwable.toAppError(): AppError = when (this) {
    is AppError -> this
    is IOException -> AppError.Network(this)
    is HttpException -> when (code()) {
        401 -> AppError.Unauthorized()
        404 -> AppError.NotFound()
        else -> AppError.Server(code())
    }
    is SerializationException -> AppError.Unknown(this)
    else -> AppError.Unknown(this)
}
