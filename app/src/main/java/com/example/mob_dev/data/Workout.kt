package com.example.mob_dev.data
import kotlinx.serialization.Serializable

@Serializable
data class Workout(
    val id: String,
    val title: String,
    val type: String,
    val day_of_week: String,
    val time: String,
    val has_spots: Boolean
)