package com.example.mob_dev.data
import kotlinx.serialization.Serializable

@Serializable
data class NewNotification(
    val title: String,
    val type: String
)