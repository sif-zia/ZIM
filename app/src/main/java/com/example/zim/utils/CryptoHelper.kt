package com.example.zim.utils

import com.example.zim.data.room.Dao.UserDao
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject


class CryptoHelper @Inject constructor(
    private val userDao: UserDao
) {
    private val crypto = Crypto()

    /**
     * Generates a random secret key and returns it as a Base64 encoded string
     * @return Base64 encoded random secret key
     */
    fun generateRandomSecretKey(): String {
        val secretKey = crypto.generateRandomAESKey()
        return crypto.encodeSecretKey(secretKey)
    }

    /**
     * Encrypts a message using the provided secret key string
     * @param message The message to encrypt
     * @param secretKeyString Base64 encoded secret key
     * @return Encrypted message as a Base64 encoded string
     */
    fun encryptGroupMessage(message: String, secretKeyString: String): String {
        val secretKey = crypto.decodeSecretKey(secretKeyString)
        return encrypt(message, secretKey)
    }

    /**
     * Decrypts a message using the provided secret key string
     * @param message The encrypted message
     * @param secretKeyString Base64 encoded secret key
     * @return Decrypted message
     */
    fun decryptGroupMessage(message: String, secretKeyString: String): String {
        val secretKey = crypto.decodeSecretKey(secretKeyString)
        return decrypt(message, secretKey)
    }

    suspend fun encryptMessage(message: String, publicKey: String): String {
        val secretKey = getSecretKey(publicKey)
        return encrypt(message, secretKey)
    }

    suspend fun decryptMessage(message: String, publicKey: String): String {
        val secretKey = getSecretKey(publicKey)
        return decrypt(message, secretKey)
    }

    private suspend fun getSecretKey(publicKey: String): SecretKeySpec {
        val privateKeyStr = userDao.getCurrentUser()?.currentUser?.prKey ?: ""
        val privateKey = crypto.decodePrivateKey(privateKeyStr)
        val decodedPublicKey = crypto.decodePublicKey(publicKey)
        val sharedSecret = crypto.generateSharedSecret(privateKey, decodedPublicKey)
        val secretKey = crypto.deriveAESKey(sharedSecret)
        return secretKey
    }

    private fun encrypt(message: String, secretKey: SecretKeySpec): String {
        val encryptedMessage = crypto.encryptText(message, secretKey)
        val encodedMessage = crypto.encodeEncryptedData(encryptedMessage)
        return encodedMessage
    }

    private fun decrypt(message: String, secretKey: SecretKeySpec): String {
        val decodedMessage = crypto.decodeEncryptedData(message)
        val decryptedMessages = crypto.decryptText(decodedMessage, secretKey)
        return decryptedMessages
    }
}