package app.ladno.data.crypto

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertFails
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse

class VaultCryptoTest {
    private val vaultKey = ByteArray(32) { it.toByte() }
    private val context = PayloadContext("u", "v", "a:b", "Покупка", 3, 2, PayloadPurpose.RECORD)
    private val plaintext = "Ладно\u0000".encodeToByteArray()
    private val jvmVector = "01000102030405060708090a0be74ae8900fdc3ae9873aa4cdcec00d9a24dffc8511f0a22530bc68".hexToByteArray()
    private val wasmVector = "01101112131415161718191a1b5607fecce4f692229baebe6fcc714fe425b4fd8cf97e09989b094f".hexToByteArray()

    @Test
    fun canonicalFieldsAndBothGoldenVectors() = runTest {
        assertContentEquals(
            ("000000146c61646e6f2f7061796c6f61642d6161642f76310000000175000000017600000003613a62" +
                "0000000ed09fd0bed0bad183d0bfd0bad0b000000001330000000132000000067265636f7264").hexToByteArray(),
            context.aad(),
        )
        assertContentEquals(
            "000000146c61646e6f2f7061796c6f61642d6b65792f7631000000017600000003613a620000000132000000067265636f7264"
                .hexToByteArray(),
            context.info(),
        )
        for (vector in listOf(jvmVector, wasmVector)) {
            val (nonce, _) = splitEnvelope(vector)
            assertContentEquals(vector, VaultCrypto.encryptWithNonce(vaultKey, context, plaintext, nonce))
            assertContentEquals(plaintext, VaultCrypto.decrypt(vaultKey, context, vector))
        }
    }

    @Test
    fun authenticationRejectsWrongKeyTagAndContext() = runTest {
        val badTag = jvmVector.copyOf().apply { this[lastIndex] = (last().toInt() xor 1).toByte() }
        assertFails { VaultCrypto.decrypt(ByteArray(32), context, jvmVector) }
        assertFails { VaultCrypto.decrypt(vaultKey, context, badTag) }
        for (changed in listOf(
            context.copy(userId = "other"), context.copy(vaultId = "other"),
            context.copy(entityId = "other"), context.copy(entityType = "other"),
            context.copy(schemaVersion = 4), context.copy(keyVersion = 3),
            context.copy(purpose = PayloadPurpose.ASSET),
        )) {
            assertFails { VaultCrypto.decrypt(vaultKey, changed, jvmVector) }
        }
    }

    @Test
    fun malformedEnvelopeFailsBeforeDecrypting() = runTest {
        assertFailsWith<IllegalArgumentException> { VaultCrypto.decrypt(vaultKey, context, ByteArray(0)) }
        val unknownVersion = jvmVector.copyOf().apply { this[0] = 2 }
        assertFailsWith<IllegalArgumentException> { VaultCrypto.decrypt(vaultKey, context, unknownVersion) }
    }

    @Test
    fun freshEncryptionsUseDifferentNonces() = runTest {
        val first = VaultCrypto.encrypt(vaultKey, context, plaintext)
        val second = VaultCrypto.encrypt(vaultKey, context, plaintext)
        assertFalse(splitEnvelope(first).first.contentEquals(splitEnvelope(second).first))
        assertContentEquals(plaintext, VaultCrypto.decrypt(vaultKey, context, second))
    }
}
