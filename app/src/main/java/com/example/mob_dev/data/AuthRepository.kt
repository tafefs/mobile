package com.example.mob_dev.data

import com.example.mob_dev.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.builtin.Email
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject

class AuthRepository {

    // вход
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

    // регистрация
    suspend fun registerUser(email: String, pass: String, name: String, surname: String): Boolean {
        return try {
            SupabaseClient.client.auth.signUpWith(Email) {
                this.email = email
                this.password = pass
                data = buildJsonObject {
                    put("full_name", JsonPrimitive("$name $surname"))
                }
            }
            true
        } catch (e: Exception) {
            false
        }
    }
}