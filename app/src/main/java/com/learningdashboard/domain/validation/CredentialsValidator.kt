package com.learningdashboard.domain.validation

enum class EmailError { Blank, Invalid }
enum class PasswordError { Blank, TooShort }

object CredentialsValidator {
    const val MIN_PASSWORD_LENGTH = 6

    // android.util.Patterns is unavailable in local unit tests, so keep a plain regex.
    private val emailRegex = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

    fun validateEmail(email: String): EmailError? = when {
        email.isBlank() -> EmailError.Blank
        !emailRegex.matches(email.trim()) -> EmailError.Invalid
        else -> null
    }

    fun validatePassword(password: String): PasswordError? = when {
        password.isEmpty() -> PasswordError.Blank
        password.length < MIN_PASSWORD_LENGTH -> PasswordError.TooShort
        else -> null
    }
}
