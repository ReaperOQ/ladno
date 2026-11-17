package app.ladno.data.crypto

import dev.whyoleg.cryptography.BinarySize.Companion.bytes
import dev.whyoleg.cryptography.CryptographyProvider
import dev.whyoleg.cryptography.DelicateCryptographyApi
import dev.whyoleg.cryptography.algorithms.AES
import dev.whyoleg.cryptography.algorithms.HKDF
import dev.whyoleg.cryptography.algorithms.SHA256
import dev.whyoleg.cryptography.random.CryptographyRandom

enum class PayloadPurpose(internal val wireName: String) {
    RECORD("record"),
    ASSET("asset"),
}

data class PayloadContext(
    val userId: String,
    val vaultId: String,
    val entityId: String,
    val entityType: String,
    val schemaVersion: Int,
    val keyVersion: Int,
    val purpose: PayloadPurpose,
) {
    init {
        require(schemaVersion > 0 && keyVersion > 0)
    }

    internal fun aad(): ByteArray = canonicalFields(
        "ladno/payload-aad/v1", userId, vaultId, entityId, entityType,
        schemaVersion.toString(), keyVersion.toString(), purpose.wireName,
    )

    internal fun info(): ByteArray = canonicalFields(
        "ladno/payload-key/v1", vaultId, entityId, keyVersion.toString(), purpose.wireName,
    )
}

object VaultCrypto {
    fun generateKey(): ByteArray = CryptographyRandom.nextBytes(KEY_SIZE)

    suspend fun encrypt(vaultKey: ByteArray, context: PayloadContext, plaintext: ByteArray): ByteArray =
        encryptWithNonce(vaultKey, context, plaintext, CryptographyRandom.nextBytes(NONCE_SIZE))

    @OptIn(DelicateCryptographyApi::class)
    internal suspend fun encryptWithNonce(
        vaultKey: ByteArray,
        context: PayloadContext,
        plaintext: ByteArray,
        nonce: ByteArray,
    ): ByteArray {
        require(nonce.size == NONCE_SIZE)
        val aad = context.aad()
        return envelope(nonce, cipher(vaultKey, context).encryptWithIv(nonce, plaintext, aad))
    }

    @OptIn(DelicateCryptographyApi::class)
    suspend fun decrypt(vaultKey: ByteArray, context: PayloadContext, envelope: ByteArray): ByteArray {
        val (nonce, sealed) = splitEnvelope(envelope)
        val aad = context.aad()
        return cipher(vaultKey, context).decryptWithIv(nonce, sealed, aad)
    }

    private suspend fun cipher(vaultKey: ByteArray, context: PayloadContext) =
        CryptographyProvider.Default.run {
            require(vaultKey.size == KEY_SIZE)
            val key = get(HKDF).secretDerivation(
                SHA256, KEY_SIZE.bytes, "ladno/hkdf-sha256/v1".encodeToByteArray(), context.info(),
            ).deriveSecretToByteArray(vaultKey)
            try {
                get(AES.GCM).keyDecoder().decodeFromByteArray(AES.Key.Format.RAW, key).cipher()
            } finally {
                key.fill(0)
            }
        }
}

internal const val KEY_SIZE = 32
internal const val NONCE_SIZE = 12
private const val TAG_SIZE = 16
private const val FORMAT_VERSION: Byte = 1

internal fun envelope(nonce: ByteArray, sealed: ByteArray): ByteArray {
    require(nonce.size == NONCE_SIZE && sealed.size >= TAG_SIZE)
    return byteArrayOf(FORMAT_VERSION) + nonce + sealed
}

internal fun splitEnvelope(value: ByteArray): Pair<ByteArray, ByteArray> {
    require(value.size >= 1 + NONCE_SIZE + TAG_SIZE && value[0] == FORMAT_VERSION) {
        "Invalid crypto envelope"
    }
    return value.copyOfRange(1, 1 + NONCE_SIZE) to value.copyOfRange(1 + NONCE_SIZE, value.size)
}

internal fun wrapAad(userId: String, vaultId: String, keyVersion: Int): ByteArray {
    require(keyVersion > 0)
    return canonicalFields("ladno/local-wrap/v1", userId, vaultId, keyVersion.toString())
}

private fun canonicalFields(vararg fields: String): ByteArray {
    val encoded = fields.map {
        require(it.length in 1..1024)
        it.encodeToByteArray(throwOnInvalidSequence = true)
    }
    val result = ByteArray(encoded.sumOf { 4 + it.size })
    var offset = 0
    for (field in encoded) {
        for (shift in 24 downTo 0 step 8) result[offset++] = (field.size ushr shift).toByte()
        field.copyInto(result, offset)
        offset += field.size
    }
    return result
}
