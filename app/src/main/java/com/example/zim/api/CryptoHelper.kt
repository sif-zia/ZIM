package com.example.zim.api

import com.example.zim.data.room.Dao.UserDao
import com.example.zim.utils.Crypto
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject


class CryptoHelper @Inject constructor(
    private val userDao: UserDao
) {
    private val crypto = Crypto()

    suspend fun encryptMessage(message: String, publicKey: String): String {
        val secretKey = getSecretKey(publicKey)
        return encrypt(message, secretKey)
    }

    suspend fun decryptMessage(message: String, publicKey: String): String {
        val secretKey = getSecretKey(publicKey)
        return decrypt(message, secretKey)
    }

    private suspend fun getSecretKey(publicKey: String): SecretKeySpec {
        val privateKeyStr = userDao.getCurrentUser().currentUser.prKey ?: ""
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