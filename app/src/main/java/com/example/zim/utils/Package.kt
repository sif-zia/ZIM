package com.example.zim.utils

data class Package(
    val sender: String,
    val receiver: String,
    val carrier: String,
    val type: Type
) {
    sealed class Type {
        data class Text(val msg: String) : Type()
        data class Protocol(val stepNumber: Int, val msg: String) : Type()
        data class Image(
            val chunkNo: Int,
            val totalChunks: Int,
            val imageHash: String,
            val chunk: ByteArray,
            val imageType: String
        ) : Type()

        data object Other : Type()
    }
}
