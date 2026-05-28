package com.example.mob_dev.data
import kotlinx.serialization.Serializable

@Serializable
data class UserProfile(
    val id: String? = null,
    val email: String? = null,
    val first_name: String? = null,
    val last_name: String? = null,
    val birth_date: String? = null,
    val phone: String? = null,
    val avatar_url: String? = null,
    val height: Int? = null
)