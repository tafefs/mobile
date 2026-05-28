package com.example.mob_dev.data
import kotlinx.serialization.Serializable

@Serializable
data class WeightEntry(
    val weight: Float,
    val recorded_at: String
)