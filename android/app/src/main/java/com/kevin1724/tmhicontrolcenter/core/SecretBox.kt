package com.kevin1724.tmhicontrolcenter.core

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class SecretBox(private val keyAlias: String) {
    fun encrypt(plainText: String): String {
        if (plainText.isEmpty()) return ""
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        cipher.updateAAD(keyAlias.toByteArray(Charsets.UTF_8))
        val encrypted = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
        return listOf(
            VERSION,
            Base64.encodeToString(cipher.iv, Base64.NO_WRAP),
            Base64.encodeToString(encrypted, Base64.NO_WRAP),
        ).joinToString(":")
    }

    fun decrypt(value: String): String? {
        if (value.isEmpty()) return ""
        if (!isEncrypted(value)) return null
        return runCatching {
            val parts = value.split(":", limit = 3)
            val iv = Base64.decode(parts[1], Base64.NO_WRAP)
            val encrypted = Base64.decode(parts[2], Base64.NO_WRAP)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(128, iv))
            cipher.updateAAD(keyAlias.toByteArray(Charsets.UTF_8))
            String(cipher.doFinal(encrypted), Charsets.UTF_8)
        }.getOrNull()
    }

    fun isEncrypted(value: String): Boolean = value.startsWith("$VERSION:")

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(KEYSTORE).apply { load(null) }
        (keyStore.getKey(keyAlias, null) as? SecretKey)?.let { return it }

        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE)
        keyGenerator.init(
            KeyGenParameterSpec.Builder(
                keyAlias,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build(),
        )
        return keyGenerator.generateKey()
    }

    private companion object {
        const val VERSION = "v1"
        const val KEYSTORE = "AndroidKeyStore"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
    }
}
