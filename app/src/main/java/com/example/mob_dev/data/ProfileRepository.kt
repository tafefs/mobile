package com.example.mob_dev.data

import com.example.mob_dev.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.storage.storage
import android.util.Log


class ProfileRepository {

    private val db = SupabaseClient.client.postgrest["profiles"]

    suspend fun getProfile(): UserProfile? {
        return try {
            val userId = SupabaseClient.client.auth.currentUserOrNull()?.id ?: return null

            db.select {
                filter { eq("id", userId) }
            }.decodeSingleOrNull<UserProfile>()
        } catch (e: Exception) {
            null
        }
    }

    suspend fun uploadAvatar(imageBytes: ByteArray): String? {
        return try {
            val userId = SupabaseClient.client.auth.currentUserOrNull()?.id ?: return null

            val fileName = "${userId}_${System.currentTimeMillis()}.jpg"

            val bucket = SupabaseClient.client.storage.from("avatars")


            bucket.upload(path = fileName, data = imageBytes, upsert = false)

            bucket.publicUrl(fileName)
        } catch (e: Exception) {
            android.util.Log.e("UploadDebug", "Ошибка: ${e.message}")
            null
        }
    }

    suspend fun updateProfile(firstName: String, lastName: String, birthDate: String, phone: String, avatarUrl: String? = null): Boolean {
        return try {
            val userId = SupabaseClient.client.auth.currentUserOrNull()?.id ?: return false

            val updatedData = UserProfile(
                first_name = firstName,
                last_name = lastName,
                birth_date = birthDate,
                phone = phone,
                avatar_url = avatarUrl // Сохраняем ссылку в таблицу
            )

            db.update(updatedData) { filter { eq("id", userId) } }
            true
        } catch (e: Exception) {
            false
        }
    }
}