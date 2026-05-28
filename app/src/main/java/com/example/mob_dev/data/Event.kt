package com.example.mob_dev.data
import kotlinx.serialization.Serializable

@Serializable
data class Event(val title: String, val image_url: String)