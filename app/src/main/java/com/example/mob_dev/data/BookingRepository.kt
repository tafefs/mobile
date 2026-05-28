package com.example.mob_dev.data

import com.example.mob_dev.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.LocalTime import java.time.format.DateTimeFormatter

class BookingRepository {

    // ссылки на таблицы
    private val db = SupabaseClient.client.postgrest["bookings"]
    private val dbWorkouts = SupabaseClient.client.postgrest["workouts"]
    private val dbNews = SupabaseClient.client.postgrest["news"]
    private val dbEvents = SupabaseClient.client.postgrest["events"]
    private val dbNotifications = SupabaseClient.client.postgrest["notifications"]

    suspend fun getNews(): List<News> {
        return try {
            dbNews.select().decodeList<News>()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getLatestEvent(): Event? {
        return try {
            dbEvents.select {
                limit(1)
            }.decodeSingleOrNull<Event>()
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getWorkouts(): List<Workout> {
        return try {
            dbWorkouts.select().decodeList<Workout>()
        } catch (e: Exception) {
            emptyList()
        }
    }

    // записаться
    suspend fun bookWorkout(workoutId: String): Boolean {
        return try {
            db.insert(Booking(workout_id = workoutId))
            true
        } catch (e: Exception) {
            false
        }
    }

    // отменить запись
    suspend fun cancelWorkout(workoutId: String): Boolean {
        return try {
            db.delete {
                filter {
                    eq("workout_id", workoutId)
                }
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun getMyBookedWorkoutIds(): List<String> {
        return try {
            val result = db.select().decodeList<Booking>()
            result.map { it.workout_id }
        } catch (e: Exception) {
            emptyList()
        }
    }

    // уведомления
    suspend fun getNotifications(): List<AppNotification> {
        return try {
            dbNotifications.select().decodeList<AppNotification>()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun deleteNotification(id: String): Boolean {
        return try {
            dbNotifications.delete {
                filter { eq("id", id) }
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun addNotification(title: String, type: String): Boolean {
        return try {
            dbNotifications.insert(NewNotification(title, type))
            true
        } catch (e: Exception) {
            android.util.Log.e("NotifError", "Ошибка добавления: ${e.message}")
            false
        }
    }

    suspend fun getNearestBookedWorkout(): Workout? {
        return try {
            val bookedIds = getMyBookedWorkoutIds()
            if (bookedIds.isEmpty()) return null

            // все забронированные тренировки
            val dbWorkouts = SupabaseClient.client.postgrest["workouts"]
            val allBookedWorkouts = dbWorkouts.select {
                filter { isIn("id", bookedIds) }
            }.decodeList<Workout>()

            if (allBookedWorkouts.isEmpty()) return null

            // сортировка по дате
            val sortedWorkouts = allBookedWorkouts.sortedBy { workout ->
                val dayEnum = mapRussianDayToEnum(workout.day_of_week)
                getNextOccurrence(dayEnum, workout.time)
            }
            sortedWorkouts.firstOrNull()

        } catch (e: Exception) {
            android.util.Log.e("BookingError", "Ошибка поиска ближайшей тренировки: ${e.message}", e)
            null
        }
    }

    // вспомогательные функции
    private fun mapRussianDayToEnum(dayStr: String): DayOfWeek {
        return when (dayStr.uppercase()) {
            "ПН" -> DayOfWeek.MONDAY
            "ВТ" -> DayOfWeek.TUESDAY
            "СР" -> DayOfWeek.WEDNESDAY
            "ЧТ" -> DayOfWeek.THURSDAY
            "ПТ" -> DayOfWeek.FRIDAY
            "СБ" -> DayOfWeek.SATURDAY
            "ВС" -> DayOfWeek.SUNDAY
            else -> DayOfWeek.MONDAY
        }
    }
    private fun getNextOccurrence(targetDay: DayOfWeek, timeStr: String): LocalDateTime {
        val now = LocalDateTime.now()
        val time = LocalTime.parse(timeStr, DateTimeFormatter.ofPattern("HH:mm"))

        var targetDateTime = now.with(time)

        while (targetDateTime.dayOfWeek != targetDay || targetDateTime.isBefore(now)) {
            targetDateTime = targetDateTime.plusDays(1)
        }
        return targetDateTime
    }
}