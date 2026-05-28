package com.example.mob_dev.data

import android.util.Log // ИМПОРТ ДЛЯ ЛОГОВ
import com.example.mob_dev.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import java.time.LocalDate

class TrackingRepository {

    private val dbHistory = SupabaseClient.client.postgrest["weight_history"]
    private val dbProfile = SupabaseClient.client.postgrest["profiles"]

    suspend fun getWeightHistory(daysLimit: Int): List<WeightEntry> {
        return try {
            val pastDate = LocalDate.now().minusDays(daysLimit.toLong()).toString()
            dbHistory.select {
                filter { gte("recorded_at", pastDate) }
                order("recorded_at", Order.ASCENDING)
            }.decodeList<WeightEntry>()
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e

            Log.e("TrackingError", "Ошибка загрузки истории веса: ${e.message}", e)
            emptyList()
        }
    }

    suspend fun getCurrentHeight(): Int {
        return try {
            val profile = dbProfile.select().decodeSingleOrNull<UserProfile>()
            profile?.height ?: 175
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e

            Log.e("TrackingError", "Ошибка загрузки роста: ${e.message}", e)
            175
        }
    }

    suspend fun saveTrackingData(weight: Float, height: Int): Boolean {
        return try {
            val userId = SupabaseClient.client.auth.currentUserOrNull()?.id ?: return false

            Log.d("TrackingError", "Начинаем сохранение. Вес: $weight, Рост: $height")

            // заапись веса в историю
            dbHistory.insert(WeightEntry(weight = weight, recorded_at = LocalDate.now().toString()))
            Log.d("TrackingError", "Вес успешно записан в историю")

            dbProfile.update(mapOf("height" to height)) {
                filter { eq("id", userId) }
            }
            Log.d("TrackingError", "Рост успешно обновлен в профиле")

            true
        } catch (e: Exception) {
            Log.e("TrackingError", "ОШИБКА СОХРАНЕНИЯ: ${e.message}", e)
            false
        }
    }
}