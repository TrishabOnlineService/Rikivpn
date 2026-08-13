package com.example.config

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class ServerEntry(
    val id: String,
    val name: String,
    val protocol: String,
    val host: String,
    val port: Int,
    val config: JsonElement? = null
)
