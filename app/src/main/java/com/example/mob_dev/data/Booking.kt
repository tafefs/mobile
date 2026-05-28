package com.example.mob_dev.data

import kotlinx.serialization.Serializable

// Эта аннотация позволяет конвертировать класс в JSON для отправки в БД
@Serializable
data class Booking(
    val workout_id: String
    // user_id мы не передаем, так как БД (благодаря default auth.uid() в SQL) сама подставит ID текущего пользователя
)