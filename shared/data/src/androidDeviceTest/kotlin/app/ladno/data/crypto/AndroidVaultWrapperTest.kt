package app.ladno.data.crypto

import java.security.KeyStore
import java.util.UUID
import javax.crypto.AEADBadTagException
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class AndroidVaultWrapperTest {
    @Test
    fun reopenedKeystoreKeyCanDecryptButCannotBeExported() {
        val alias = "ladno-t03-test-" + UUID.randomUUID()
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        val key = VaultCrypto.generateKey()
        try {
            assertFailsWith<IllegalStateException> {
                AndroidVaultWrapper(alias).unwrap(ByteArray(61).apply { this[0] = 1 }, "u", "v", 1)
            }
            val wrapped = AndroidVaultWrapper(alias).wrap(key, "u", "v", 1)
            val reopened = AndroidVaultWrapper(alias)
            assertContentEquals(key, reopened.unwrap(wrapped, "u", "v", 1))
            assertFailsWith<AEADBadTagException> { reopened.unwrap(wrapped, "u", "v", 2) }
            assertNull(store.getKey(alias, null).encoded)
        } finally {
            store.deleteEntry(alias)
        }
    }
}
