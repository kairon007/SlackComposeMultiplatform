package dev.baseio.security

const val TRANSFORMATION_ASYMMETRIC = "RSA/ECB/PKCS1Padding"
const val KEY_SIZE: Int = 2048

typealias EncryptedData = Pair<String, String>

expect object CapillaryEncryption {

    fun encrypt(
        plaintext: ByteArray,
        publicKey: PublicKey,
    ): EncryptedData

    fun decrypt(
        encryptedData: EncryptedData,
        privateKey: PrivateKey,
    ): ByteArray
}
