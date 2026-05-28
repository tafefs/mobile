package com.example.mob_dev.data

import com.example.mob_dev.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.builtin.Email
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject

class AuthRepository {

    // 1. Метод для входа (у вас уже есть)
    suspend fun loginUser(email: String, pass: String): Boolean {
        return try {
            SupabaseClient.client.auth.signInWith(Email) {
                this.email = email
                this.password = pass
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    // 2. НОВЫЙ Метод для регистрации
    suspend fun registerUser(email: String, pass: String, name: String, surname: String): Boolean {
        return try {
            SupabaseClient.client.auth.signUpWith(Email) {
                this.email = email
                this.password = pass
                data = buildJsonObject {
                    put("full_name", JsonPrimitive("$name $surname"))
                }
            }
            true // Успешная регистрация
        } catch (e: Exception) {
            false // Ошибка (например, email уже занят)
        }
    }
}