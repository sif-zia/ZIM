package com.example.zim.utils

import org.json.JSONObject
import android.util.Base64

data class Package(
    val sender: String,
    val receiver: String,
    val carrier: String,
    val type: Type
) {
    sealed class Type {
        data class Text(val msg: String) : Type()
        data class Protocol(val stepNumber: Int, val msg: String) : Type()
        data class TransferControl(
            val action: String,  // "START" or "END"
            val transferId: String,
            val totalChunks: Int,
            val contentType: String
        ) : Type()
        data class Image(
            val chunkNo: Int,
            val totalChunks: Int,
            val imageHash: String,
            val chunk: ByteArray,
            val imageType: String
        ) : Type() {
            // Override equals for proper ByteArray comparison
            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (javaClass != other?.javaClass) return false

                other as Image

                if (chunkNo != other.chunkNo) return false
                if (totalChunks != other.totalChunks) return false
                if (imageHash != other.imageHash) return false
                if (!chunk.contentEquals(other.chunk)) return false
                if (imageType != other.imageType) return false

                return true
            }

            // Override hashCode for proper ByteArray hashing
            override fun hashCode(): Int {
                var result = chunkNo
                result = 31 * result + totalChunks
                result = 31 * result + imageHash.hashCode()
                result = 31 * result + chunk.contentHashCode()
                result = 31 * result + imageType.hashCode()
                return result
            }
        }

        data object Other : Type()
    }

    // Serialization/Deserialization methods
    fun serializePackage(): String {
        val json = JSONObject().apply {
            put("sender", sender)
            put("receiver", receiver)
            put("carrier", carrier)

            when (val type = type) {
                is Type.Text -> {
                    put("type", "Text")
                    put("msg", type.msg)
                }
                is Type.Protocol -> {
                    put("type", "Protocol")
                    put("stepNumber", type.stepNumber)
                    put("msg", type.msg)
                }
                is Type.TransferControl -> {
                    put("type", "TransferControl")
                    put("action", type.action)
                    put("transferId", type.transferId)
                    put("totalChunks", type.totalChunks)
                    put("contentType", type.contentType)
                }
                is Type.Image -> {
                    put("type", "Image")
                    put("chunkNo", type.chunkNo)
                    put("totalChunks", type.totalChunks)
                    put("imageHash", type.imageHash)
                    // Convert ByteArray to Base64 string for JSON transmission
                    put("chunk", Base64.encodeToString(type.chunk, Base64.DEFAULT))
                    put("imageType", type.imageType)
                }
                is Type.Other -> {
                    put("type", "Other")
                }
            }
        }
        return json.toString()
    }

    companion object {
        fun deserializePackage(jsonStr: String): Package {
            val json = JSONObject(jsonStr)
            val sender = json.getString("sender")
            val receiver = json.getString("receiver")
            val carrier = json.getString("carrier")

            val type: Type = when (json.getString("type")) {
                "Text" -> Type.Text(json.getString("msg"))
                "Protocol" -> Type.Protocol(json.getInt("stepNumber"), json.getString("msg"))
                "TransferControl" -> Type.TransferControl(
                    json.getString("action"),
                    json.getString("transferId"),
                    json.getInt("totalChunks"),
                    json.getString("contentType")
                )
                "Image" -> {
                    // Convert Base64 string back to ByteArray
                    val base64Chunk = json.getString("chunk")
                    val byteArray = Base64.decode(base64Chunk, Base64.DEFAULT)

                    Type.Image(
                        json.getInt("chunkNo"),
                        json.getInt("totalChunks"),
                        json.getString("imageHash"),
                        byteArray,
                        json.getString("imageType")
                    )
                }
                "Other" -> Type.Other
                else -> throw IllegalArgumentException("Unknown package type: ${json.getString("type")}")
            }

            return Package(sender, receiver, carrier, type)
        }
    }
}