@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package app.ladno.data.crypto

import kotlin.js.JsAny
import kotlin.js.Promise
import kotlinx.coroutines.await

class WebVaultWrapper(private val keyId: String) {
    suspend fun wrap(vaultKey: ByteArray, userId: String, vaultId: String, keyVersion: Int): ByteArray {
        require(vaultKey.size == KEY_SIZE)
        val aad = wrapAad(userId, vaultId, keyVersion).toBuffer()
        val nonce = randomNonce()
        return envelope(
            nonce.toBytes(NONCE_SIZE),
            seal(loadWrappingKey(keyId, create = true), nonce, aad, vaultKey.toBuffer())
                .await().toBytes(KEY_SIZE + 16),
        )
    }

    suspend fun unwrap(wrapped: ByteArray, userId: String, vaultId: String, keyVersion: Int): ByteArray {
        val (nonce, sealed) = splitEnvelope(wrapped)
        require(sealed.size == KEY_SIZE + 16)
        val aad = wrapAad(userId, vaultId, keyVersion).toBuffer()
        return open(loadWrappingKey(keyId, create = false), nonce.toBuffer(), aad, sealed.toBuffer())
            .await().toBytes(KEY_SIZE)
    }
}

internal suspend fun loadWrappingKey(keyId: String, create: Boolean): JsAny {
    val existing = accessKey(keyId, null).await()
    if (existing != null) return existing
    check(create) { "Local wrapping key is missing" }
    return checkNotNull(accessKey(keyId, generateWrappingKey().await()).await())
}

@JsFun("""
 (id, candidate) => new Promise((resolve, reject) => {
    const request = indexedDB.open('ladno-local-crypto', 1);
    request.onupgradeneeded = () => request.result.createObjectStore('wrappingKeys');
    request.onerror = () => reject(request.error);
    request.onsuccess = () => {
        const db = request.result;
        const tx = db.transaction('wrappingKeys', candidate ? 'readwrite' : 'readonly');
        const store = tx.objectStore('wrappingKeys');
        const read = store.get(id);
        let key = null;
        read.onsuccess = () => {
            key = read.result ?? candidate;
            if (!read.result && candidate) store.add(candidate, id);
        };
        tx.oncomplete = () => { db.close(); resolve(key); };
        tx.onabort = () => { db.close(); reject(tx.error); };
    };
 })
""")
private external fun accessKey(id: String, candidate: JsAny?): Promise<JsAny?>

@JsFun("() => crypto.subtle.generateKey({name: 'AES-GCM', length: 256}, false, ['encrypt', 'decrypt'])")
private external fun generateWrappingKey(): Promise<JsAny>

@JsFun("() => crypto.getRandomValues(new Uint8Array(12))")
private external fun randomNonce(): JsAny

@JsFun("(key, iv, additionalData, data) => crypto.subtle.encrypt({name: 'AES-GCM', iv, additionalData, tagLength: 128}, key, data).then(buffer => new Uint8Array(buffer))")
private external fun seal(key: JsAny, nonce: JsAny, aad: JsAny, data: JsAny): Promise<JsAny>

@JsFun("(key, iv, additionalData, data) => crypto.subtle.decrypt({name: 'AES-GCM', iv, additionalData, tagLength: 128}, key, data).then(buffer => new Uint8Array(buffer))")
private external fun open(key: JsAny, nonce: JsAny, aad: JsAny, data: JsAny): Promise<JsAny>

@JsFun("size => new Uint8Array(size)")
private external fun buffer(size: Int): JsAny

@JsFun("(bytes, index, value) => { bytes[index] = value; }")
private external fun setByte(bytes: JsAny, index: Int, value: Int)

@JsFun("(bytes, index) => bytes[index]")
private external fun getByte(bytes: JsAny, index: Int): Int

private fun ByteArray.toBuffer(): JsAny = buffer(size).also { bytes ->
    forEachIndexed { index, value -> setByte(bytes, index, value.toInt()) }
}

private fun JsAny.toBytes(size: Int): ByteArray = ByteArray(size) { getByte(this, it).toByte() }
