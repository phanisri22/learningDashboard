package com.learningdashboard.data.auth

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.security.GeneralSecurityException
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

data class Session(val token: String, val userId: String)

interface SessionStore {
    /** Id of the logged-in user, or null when logged out. Cached data is scoped to it. */
    val userId: StateFlow<String?>

    fun getToken(): String?
    fun save(session: Session)
    fun clear()

    /** Ends the session if [token] is still the current one, so a late 401 can't end a newer session. */
    fun expire(token: String)

    /** True once after [expire], so the login screen can say why the user was logged out. */
    fun consumeExpired(): Boolean
}

/**
 * Encrypts the token with an AES-GCM key held in the Android Keystore; only the
 * ciphertext is written to SharedPreferences. The user id is not a secret.
 */
@Singleton
class KeystoreSessionStore @Inject constructor(
    @ApplicationContext context: Context,
) : SessionStore {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    @Volatile
    private var cachedToken: String? = null

    @Volatile
    private var expired = false

    private val _userId = MutableStateFlow(prefs.getString(KEY_USER_ID, null))
    override val userId: StateFlow<String?> = _userId.asStateFlow()

    override fun getToken(): String? {
        cachedToken?.let { return it }
        if (_userId.value == null) return null
        val iv = prefs.getString(KEY_IV, null) ?: return null
        val cipherText = prefs.getString(KEY_TOKEN, null) ?: return null
        return try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(128, decode(iv)))
            String(cipher.doFinal(decode(cipherText)), Charsets.UTF_8).also { cachedToken = it }
        } catch (e: GeneralSecurityException) {
            // Key was invalidated (e.g. device credentials reset): force a fresh login.
            clear()
            null
        }
    }

    override fun save(session: Session) {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val cipherText = cipher.doFinal(session.token.toByteArray(Charsets.UTF_8))
        prefs.edit {
            putString(KEY_IV, encode(cipher.iv))
            putString(KEY_TOKEN, encode(cipherText))
            putString(KEY_USER_ID, session.userId)
        }
        cachedToken = session.token
        expired = false
        _userId.value = session.userId
    }

    override fun clear() {
        cachedToken = null
        prefs.edit { clear() }
        _userId.value = null
    }

    @Synchronized
    override fun expire(token: String) {
        if (getToken() != token) return
        expired = true
        clear()
    }

    override fun consumeExpired(): Boolean = expired.also { expired = false }

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }

        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build(),
        )
        return generator.generateKey()
    }

    private fun encode(bytes: ByteArray) = Base64.encodeToString(bytes, Base64.NO_WRAP)
    private fun decode(value: String) = Base64.decode(value, Base64.NO_WRAP)

    private companion object {
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val KEY_ALIAS = "session_token_key"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val PREFS_NAME = "secure_session"
        const val KEY_TOKEN = "token"
        const val KEY_IV = "iv"
        const val KEY_USER_ID = "user_id"
    }
}
