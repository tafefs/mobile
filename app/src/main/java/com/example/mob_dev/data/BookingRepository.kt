package com.example.mob_dev.data

import com.example.mob_dev.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.LocalTime import java.time.format.DateTimeFormatter

class BookingRepository {

    // Ссылка на нашу таблицу в БД
    private val db = SupabaseClient.client.postgrest["bookings"]
    // Внутри класса BookingRepository добавьте:
    private val dbWorkouts = SupabaseClient.client.postgrest["workouts"]
    private val dbNews = SupabaseClient.client.postgrest["news"]
    private val dbEvents = SupabaseClient.client.postgrest["events"]

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
                limit(1) // Берем только одно мероприятие для главного баннера
            }.decodeSingleOrNull<Event>()
        } catch (e: Exception) {
            null
        }
    }

    // Получить все тренировки из базы
    suspend fun getWorkouts(): List<Workout> {
        return try {
            dbWorkouts.select().decodeList<Workout>()
        } catch (e: Exception) {
            emptyList()
        }
    }

    // 1. ДОБАВИТЬ В КОРЗИНУ (Записаться)
    suspend fun bookWorkout(workoutId: String): Boolean {
        return try {
            // Отправляем JSON { "workout_id": "..." } в таблицу
            db.insert(Booking(workout_id = workoutId))
            true
        } catch (e: Exception) {
            false
        }
    }

    // 2. УДАЛИТЬ ИЗ КОРЗИНЫ (Отменить запись)
    suspend fun cancelWorkout(workoutId: String): Boolean {
        return try {
            // Удаляем запись, где workout_id совпадает
            // (Чужую запись он не удалит благодаря RLS политике в БД)
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

    // 3. ПРОВЕРКА (Записан ли уже?)
    suspend fun isBooked(workoutId: String): Boolean {
        return try {
            // Ищем запись с таким workout_id. Если находим - значит записан.
            val result = db.select {
                filter {
                    eq("workout_id", workoutId)
                }
            }.decodeList<Booking>()

            result.isNotEmpty() // Возвращает true, если список не пуст
        } catch (e: Exception) {
            false
        }
    }

    // 4. ПОДСЧЕТ ИТОГО (Сумма корзины)
    suspend fun getTotalBookings(): Int {
        return try {
            // Скачиваем все записи текущего пользователя и считаем их количество
            val result = db.select().decodeList<Booking>()
            result.size
        } catch (e: Exception) {
            0
        }
    }

    suspend fun getMyBookedWorkoutIds(): List<String> {
        return try {
            // Скачиваем ваши записи. Благодаря RLS скачаются только ваши.
            val result = db.select().decodeList<Booking>()
            // Превращаем список объектов Booking в список строк (ID тренировок)
            result.map { it.workout_id }
        } catch (e: Exception) {
            emptyList()
        }
    }

    // Внутри класса BookingRepository:
    private val dbNotifications = SupabaseClient.client.postgrest["notifications"]

    // Скачать уведомления текущего пользователя
    suspend fun getNotifications(): List<AppNotification> {
        return try {
            dbNotifications.select().decodeList<AppNotification>()
        } catch (e: Exception) {
            emptyList()
        }
    }

    // Удалить уведомление (прочитано)
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

            // Скачиваем детальную информацию обо ВСЕХ забронированных тренировках
            val dbWorkouts = SupabaseClient.client.postgrest["workouts"]
            val allBookedWorkouts = dbWorkouts.select {
                filter { isIn("id", bookedIds) } // Скачиваем только забронированные
            }.decodeList<Workout>()

            if (allBookedWorkouts.isEmpty()) return null

            // Сортируем тренировки по дате и времени их следующего наступления
            val sortedWorkouts = allBookedWorkouts.sortedBy { workout ->
                val dayEnum = mapRussianDayToEnum(workout.day_of_week)
                // Вычисляем точную дату и время следующего занятия
                getNextOccurrence(dayEnum, workout.time)
            }

            // Возвращаем ту тренировку, которая наступит раньше остальных
            sortedWorkouts.firstOrNull()

        } catch (e: Exception) {
            android.util.Log.e("BookingError", "Ошибка поиска ближайшей тренировки: ${e.message}", e)
            null
        }
    }

    // --- ВСПОМОГАТЕЛЬНЫЕ ФУНКЦИИ ДЛЯ РАБОТЫ С ДАТАМИ (Clean Code) ---

    // Превращает русский день недели (ПН, ВТ...) в стандартный системный формат DayOfWeek
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

    // Вычисляет точную дату и время следующего сеанса тренировки от текущего момента
    private fun getNextOccurrence(targetDay: DayOfWeek, timeStr: String): LocalDateTime {
        val now = LocalDateTime.now()
        val time = LocalTime.parse(timeStr, DateTimeFormatter.ofPattern("HH:mm"))

        // Начинаем расчет, предполагая, что тренировка сегодня
        var targetDateTime = now.with(time)

        // Цикл крутит дни вперед, пока день недели не совпадет,
        // и пока время тренировки не окажется в будущем от текущего момента
        while (targetDateTime.dayOfWeek != targetDay || targetDateTime.isBefore(now)) {
            targetDateTime = targetDateTime.plusDays(1)
        }
        return targetDateTime
    }
}