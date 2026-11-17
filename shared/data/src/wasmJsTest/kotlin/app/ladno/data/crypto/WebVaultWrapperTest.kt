@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package app.ladno.data.crypto

import kotlin.js.JsAny
import kotlin.js.Promise
import kotlinx.coroutines.await
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFails

class WebVaultWrapperTest {
    @Test
    fun reopenedIndexedDbKeyCanDecryptButCannotBeExported() = runTest {
        val id = randomId()
        val key = VaultCrypto.generateKey()
        try {
            assertFails { WebVaultWrapper(id).unwrap(ByteArray(61).apply { this[0] = 1 }, "u", "v", 1) }
            val wrapped = WebVaultWrapper(id).wrap(key, "u", "v", 1)
            val reopened = WebVaultWrapper(id)
            assertContentEquals(key, reopened.unwrap(wrapped, "u", "v", 1))
            assertFails { reopened.unwrap(wrapped, "u", "v", 2) }
            assertEquals("InvalidAccessError", exportResult(loadWrappingKey(id, create = false)).await().toString())
        } finally {
            deleteKey(id).await()
        }
    }
}

@JsFun("() => crypto.randomUUID()")
private external fun randomId(): String

@JsFun("key => crypto.subtle.exportKey('raw', key).then(() => 'exported', error => error.name)")
private external fun exportResult(key: JsAny): Promise<kotlin.js.JsString>

@JsFun("""
 id => new Promise((resolve, reject) => {
    const request = indexedDB.open('ladno-local-crypto', 1);
    request.onerror = () => reject(request.error);
    request.onsuccess = () => {
        const db = request.result;
        const tx = db.transaction('wrappingKeys', 'readwrite');
        tx.objectStore('wrappingKeys').delete(id);
        tx.onabort = () => { db.close(); reject(tx.error); };
        tx.oncomplete = () => { db.close(); resolve(null); };
    };
 })
""")
private external fun deleteKey(id: String): Promise<JsAny?>
