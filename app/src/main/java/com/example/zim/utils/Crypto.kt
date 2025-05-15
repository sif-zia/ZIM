package com.example.zim.utils

import android.util.Base64
import java.nio.charset.StandardCharsets
import java.security.*
import java.security.spec.ECGenParameterSpec
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher
import javax.crypto.KeyAgreement
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Utility class for secure communication using Elliptic Curve Diffie-Hellman (ECDH)
 * for key exchange and AES-GCM for symmetric encryption.
 */
class Crypto {

    companion object {
        private const val EC_ALGORITHM = "EC"
        private const val EC_CURVE = "secp256r1" // NIST P-256 curve
        private const val KEY_AGREEMENT_ALGORITHM = "ECDH"
        private const val CIPHER_ALGORITHM = "AES/GCM/NoPadding"
        private const val AES_KEY_SIZE = 256
        private const val GCM_TAG_LENGTH = 128
        private const val GCM_IV_LENGTH = 12 // 12 bytes for GCM
    }

    /**
     * Generates a random AES key for direct use (without ECDH key exchange)
     * @return SecretKeySpec for AES encryption
     */
    fun generateRandomAESKey(): SecretKeySpec {
        val keyBytes = ByteArray(AES_KEY_SIZE / 8) // 32 bytes for AES-256
        SecureRandom().nextBytes(keyBytes)
        return SecretKeySpec(keyBytes, "AES")
    }

    /**
     * Generates an EC key pair for ECDH key exchange
     * @return KeyPair containing public and private keys
     */
    fun generateECKeyPair(): KeyPair {
        val keyPairGenerator = KeyPairGenerator.getInstance(EC_ALGORITHM)
        val ecParameterSpec = ECGenParameterSpec(EC_CURVE)
        keyPairGenerator.initialize(ecParameterSpec, SecureRandom())
        return keyPairGenerator.generateKeyPair()
    }

    /**
     * Encodes public key to Base64 string for transmission
     * @param publicKey The public key to encode
     * @return Base64 encoded string representation of the public key
     */
    fun encodePublicKey(publicKey: PublicKey): String {
        return Base64.encodeToString(publicKey.encoded, Base64.NO_WRAP)
    }

    /**
     * Decodes Base64 string back to public key
     * @param encodedKey Base64 encoded string representation of the public key
     * @return Decoded PublicKey
     */
    fun decodePublicKey(encodedKey: String): PublicKey {
        val keyBytes = Base64.decode(encodedKey, Base64.NO_WRAP)
        val keySpec = X509EncodedKeySpec(keyBytes)
        val keyFactory = KeyFactory.getInstance(EC_ALGORITHM)
        return keyFactory.generatePublic(keySpec)
    }

    /**
     * Encodes private key to Base64 string (for storage purposes only, should not be transmitted)
     * @param privateKey The private key to encode
     * @return Base64 encoded string representation of the private key
     */
    fun encodePrivateKey(privateKey: PrivateKey): String {
        return Base64.encodeToString(privateKey.encoded, Base64.NO_WRAP)
    }

    /**
     * Decodes Base64 string back to private key
     * @param encodedKey Base64 encoded string representation of the private key
     * @return Decoded PrivateKey
     */
    fun decodePrivateKey(encodedKey: String): PrivateKey {
        val keyBytes = Base64.decode(encodedKey, Base64.NO_WRAP)
        val keySpec = PKCS8EncodedKeySpec(keyBytes)
        val keyFactory = KeyFactory.getInstance(EC_ALGORITHM)
        return keyFactory.generatePrivate(keySpec)
    }

    /**
     * Generates a shared secret using ECDH key agreement
     * @param privateKey Your private key
     * @param publicKey The public key of the other party
     * @return Byte array containing the shared secret
     */
    fun generateSharedSecret(privateKey: PrivateKey, publicKey: PublicKey): ByteArray {
        val keyAgreement = KeyAgreement.getInstance(KEY_AGREEMENT_ALGORITHM)
        keyAgreement.init(privateKey)
        keyAgreement.doPhase(publicKey, true)
        return keyAgreement.generateSecret()
    }

    /**
     * Derives an AES key from the shared secret
     * @param sharedSecret Shared secret from ECDH key agreement
     * @return SecretKeySpec for AES encryption
     */
    fun deriveAESKey(sharedSecret: ByteArray): SecretKeySpec {
        // Use the first 32 bytes (256 bits) of the shared secret as the AES key
        // In a production environment, you might want to use a key derivation function like HKDF
        val digest = MessageDigest.getInstance("SHA-256")
        val keyBytes = digest.digest(sharedSecret)
        return SecretKeySpec(keyBytes, "AES")
    }

    /**
     * Encrypts text using AES-GCM
     * @param plaintext Text to encrypt
     * @param secretKey AES key derived from shared secret
     * @return EncryptedData object containing ciphertext and IV
     */
    fun encryptText(plaintext: String, secretKey: SecretKeySpec): EncryptedData {
        val plainBytes = plaintext.toByteArray(StandardCharsets.UTF_8)
        return encryptData(plainBytes, secretKey)
    }

    /**
     * Decrypts text using AES-GCM
     * @param encryptedData EncryptedData object containing ciphertext and IV
     * @param secretKey AES key derived from shared secret
     * @return Decrypted text
     */
    fun decryptText(encryptedData: EncryptedData, secretKey: SecretKeySpec): String {
        val decryptedBytes = decryptData(encryptedData, secretKey)
        return String(decryptedBytes, StandardCharsets.UTF_8)
    }

    /**
     * Encrypts binary data using AES-GCM
     * @param data Data to encrypt
     * @param secretKey AES key derived from shared secret
     * @return EncryptedData object containing ciphertext and IV
     */
    fun encryptData(data: ByteArray, secretKey: SecretKeySpec): EncryptedData {
        // Generate a random IV (Initialization Vector)
        val iv = ByteArray(GCM_IV_LENGTH)
        SecureRandom().nextBytes(iv)

        // Initialize cipher for encryption
        val cipher = Cipher.getInstance(CIPHER_ALGORITHM)
        val gcmParameterSpec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmParameterSpec)

        // Encrypt the data
        val encryptedBytes = cipher.doFinal(data)

        return EncryptedData(encryptedBytes, iv)
    }

    /**
     * Decrypts binary data using AES-GCM
     * @param encryptedData EncryptedData object containing ciphertext and IV
     * @param secretKey AES key derived from shared secret
     * @return Decrypted data
     */
    fun decryptData(encryptedData: EncryptedData, secretKey: SecretKeySpec): ByteArray {
        // Initialize cipher for decryption
        val cipher = Cipher.getInstance(CIPHER_ALGORITHM)
        val gcmParameterSpec = GCMParameterSpec(GCM_TAG_LENGTH, encryptedData.iv)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmParameterSpec)

        // Decrypt the data
        return cipher.doFinal(encryptedData.ciphertext)
    }

    /**
     * Encodes EncryptedData for transmission
     * @param encryptedData EncryptedData object
     * @return Base64 encoded string representation of encrypted data and IV
     */
    fun encodeEncryptedData(encryptedData: EncryptedData): String {
        val encodedCiphertext = Base64.encodeToString(encryptedData.ciphertext, Base64.NO_WRAP)
        val encodedIv = Base64.encodeToString(encryptedData.iv, Base64.NO_WRAP)
        return "$encodedCiphertext:$encodedIv"
    }

    /**
     * Decodes Base64 string back to EncryptedData
     * @param encodedData Base64 encoded string representation of encrypted data and IV
     * @return EncryptedData object
     */
    fun decodeEncryptedData(encodedData: String): EncryptedData {
        val parts = encodedData.split(":")
        if (parts.size != 2) throw IllegalArgumentException("Invalid encoded data format")

        val ciphertext = Base64.decode(parts[0], Base64.NO_WRAP)
        val iv = Base64.decode(parts[1], Base64.NO_WRAP)

        return EncryptedData(ciphertext, iv)
    }

    /**
     * Encodes a SecretKeySpec to Base64 string for storage
     * @param secretKey The SecretKeySpec to encode
     * @return Base64 encoded string representation of the secret key
     */
    fun encodeSecretKey(secretKey: SecretKeySpec): String {
        return Base64.encodeToString(secretKey.encoded, Base64.NO_WRAP)
    }

    /**
     * Overloaded method to handle raw byte array input
     * @param secretKey The raw key bytes
     * @return Base64 encoded string representation of the secret key
     */
    fun encodeSecretKey(secretKey: ByteArray): String {
        return Base64.encodeToString(secretKey, Base64.NO_WRAP)
    }

    /**
     * Decodes Base64 string back to SecretKeySpec
     * @param encodedKey Base64 encoded string representation of the secret key
     * @return Decoded SecretKeySpec
     */
    fun decodeSecretKey(encodedKey: String): SecretKeySpec {
        val keyBytes = Base64.decode(encodedKey, Base64.NO_WRAP)
        return SecretKeySpec(keyBytes, "AES")
    }

    /**
     * Data class to hold encrypted data and its IV
     */
    data class EncryptedData(val ciphertext: ByteArray, val iv: ByteArray) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as EncryptedData

            if (!ciphertext.contentEquals(other.ciphertext)) return false
            if (!iv.contentEquals(other.iv)) return false

            return true
        }

        override fun hashCode(): Int {
            var result = ciphertext.contentHashCode()
            result = 31 * result + iv.contentHashCode()
            return result
        }
    }
}