package com.example.zim.api

import android.adservices.adid.AdId
import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import kotlinx.serialization.modules.*
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

// Custom serializer for LocalDateTime
object LocalDateTimeSerializer : KSerializer<LocalDateTime> {
    private val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("LocalDateTime", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: LocalDateTime) {
        encoder.encodeString(formatter.format(value))
    }

    override fun deserialize(decoder: Decoder): LocalDateTime {
        return LocalDateTime.parse(decoder.decodeString(), formatter)
    }
}

@Serializable
class AlertData {
    var alertType: String = ""
    var alertDescription: String? = null
    var alertId : Int
    @Serializable(with = LocalDateTimeSerializer::class)
    var alertTime: LocalDateTime = LocalDateTime.now()

    var alertSenderFName: String = ""
    var alertSenderLName: String = ""
    var alertSenderPuKey: String = ""
    var alertHops: Int = 0

    constructor(
        alertType: String,
        alertDescription: String?,
        alertTime: LocalDateTime,
        alertSenderFName: String,
        alertSenderLName: String,
        alertSenderPuKey: String,
        alertHops: Int,
        alertId: Int
    ) {
        this.alertType = alertType
        this.alertDescription = alertDescription
        this.alertTime = alertTime
        this.alertSenderFName = alertSenderFName
        this.alertSenderLName = alertSenderLName
        this.alertSenderPuKey = alertSenderPuKey
        this.alertHops = alertHops
        this.alertId=alertId
    }
}