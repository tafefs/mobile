package com.example.mob_dev.data
import kotlinx.serialization.Serializable

@Serializable
data class AppNotification(
    val id: String,
    val title: String,
    val type: String // 'reminder', 'warning', 'promo'
)