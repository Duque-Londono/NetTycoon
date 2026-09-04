package com.ejemplo.nettycoon.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * Respuesta de ipwho.is para `GET https://ipwho.is/{ip}`.
 *
 * Solo se mapean los campos que el juego usa. **ipwho.is responde HTTP 200 incluso ante un
 * error lógico**, indicándolo con [success] = `false` y un [message]; ese caso debe tratarse
 * como error, no como éxito.
 *
 * Los `@SerializedName` son explícitos para resistir la minificación/ofuscación en release.
 */
data class IpWhoIsResponseDto(
    @SerializedName("success") val success: Boolean,
    @SerializedName("country") val country: String? = null,
    @SerializedName("message") val message: String? = null,
    @SerializedName("connection") val connection: ConnectionDto? = null,
)

/** Bloque `connection` del JSON; de aquí tomamos el ISP. */
data class ConnectionDto(
    @SerializedName("isp") val isp: String? = null,
)
