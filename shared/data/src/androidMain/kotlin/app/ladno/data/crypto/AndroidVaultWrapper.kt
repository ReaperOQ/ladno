package app.ladno.data.crypto

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class AndroidVaultWrapper(private val alias: String) {
    fun wrap(vaultKey: ByteArray, userId: String, vaultId: String, keyVersion: Int): ByteArray {
        require(vaultKey.size == KEY_SIZE)
        val aad = wrapAad(userId, vaultId, keyVersion)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, wrappingKey(create = true))
        cipher.updateAAD(aad)
        return envelope(cipher.iv, cipher.doFinal(vaultKey))
    }

    fun unwrap(wrapped: ByteArray, userId: String, vaultId: String, keyVersion: Int): ByteArray {
        val (nonce, sealed) = splitEnvelope(wrapped)
        require(sealed.size == KEY_SIZE + 16)
        val aad = wrapAad(userId, vaultId, keyVersion)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, wrappingKey(create = false), GCMParameterSpec(128, nonce))
        cipher.updateAAD(aad)
        return cipher.doFinal(sealed)
    }

    private fun wrappingKey(create: Boolean): SecretKey = synchronized(keyLock) {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        val key = store.getKey(alias, null)
        if (key != null) return@synchronized key as SecretKey
        check(create) { "Local wrapping key is missing" }
        KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").run {
            init(
                KeyGenParameterSpec.Builder(
                    alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
                )
                    .setKeySize(256)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setRandomizedEncryptionRequired(true)
                    .setUserAuthenticationRequired(false)
                    .build(),
            )
            generateKey()
        }
    }

    private companion object {
        val keyLock = Any()
    }
}
