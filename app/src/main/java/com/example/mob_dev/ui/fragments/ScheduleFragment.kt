package com.example.mob_dev.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mob_dev.R
import com.example.mob_dev.data.BookingRepository
import com.example.mob_dev.data.Workout
import com.example.mob_dev.ui.WorkoutAdapter
import com.example.mob_dev.utils.NetworkUtils
import kotlinx.coroutines.launch


class ScheduleFragment : Fragment() {

    private val bookingRepo = BookingRepository()

    // UI
    private lateinit var rvWorkouts: RecyclerView
    private lateinit var tvTotalBookings: TextView
    private lateinit var adapter: WorkoutAdapter

    // Данные
    private var allWorkouts: List<Workout> = emptyList()
    private var myBookedIds: MutableList<String> = mutableListOf()

    // Состояние фильтров (По умолчанию: ПН и Все)
    private var selectedDay = "ПН"
    private var selectedType = "Все"
    private var onlyWithSpots = false // Чекбокс "Есть места"

    // Списки кнопок фильтров
    private lateinit var dayTabs: List<TextView>
    private lateinit var typeTabs: List<TextView>

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_schedule, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tvTotalBookings = view.findViewById(R.id.tvTotalBookings)
        rvWorkouts = view.findViewById(R.id.rvWorkouts)
        val cbHasSpots = view.findViewById<CheckBox>(R.id.cbHasSpots) // Убедитесь, что чекбокс имеет этот ID в XML

        // 1. ИНИЦИАЛИЗАЦИЯ СПИСКА
        rvWorkouts.layoutManager = LinearLayoutManager(requireContext())
        adapter = WorkoutAdapter(emptyList(), emptyList()) { workoutId, isBooked ->
            handleBookingClick(workoutId, isBooked)
        }
        rvWorkouts.adapter = adapter

        // 2. ИНИЦИАЛИЗАЦИЯ ФИЛЬТРОВ
        // (Обязательно добавьте эти ID в fragment_schedule.xml)
        typeTabs = listOf(
            view.findViewById(R.id.tabAll),
            view.findViewById(R.id.tabGroup),
            view.findViewById(R.id.tabPersonal)
        )
        dayTabs = listOf(
            view.findViewById(R.id.tabMon), view.findViewById(R.id.tabTue),
            view.findViewById(R.id.tabWed), view.findViewById(R.id.tabThu),
            view.findViewById(R.id.tabFri), view.findViewById(R.id.tabSat),
            view.findViewById(R.id.tabSun)
        )

        setupFilters(cbHasSpots)

        // 3. ЗАГРУЗКА ДАННЫХ ИЗ БАЗЫ
        loadDataFromServer()
    }

    private fun loadDataFromServer() {
        if (!NetworkUtils.isInternetAvailable(requireContext())) {
            Toast.makeText(requireContext(), "Нет интернета", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            // 1. Скачиваем ВСЕ тренировки
            allWorkouts = bookingRepo.getWorkouts()

            // 2. ИСПРАВЛЕНИЕ: Скачиваем список ID, на которые мы реально записаны в БД
            val bookedIdsFromDb = bookingRepo.getMyBookedWorkoutIds()
            myBookedIds = bookedIdsFromDb.toMutableList() // Обновляем локальный список

            // 3. Обновляем текст
            tvTotalBookings.text = "Запланировано: ${myBookedIds.size}"

            // 4. Применяем фильтры (это обновит адаптер)
            applyFilters()
        }
    }

    // --- ЛОГИКА ФИЛЬТРАЦИИ ---
    private fun setupFilters(cbHasSpots: CheckBox) {
        // Клик по типу
        typeTabs[0].setOnClickListener { selectedType = "Все"; updateTabUI(typeTabs, it as TextView); applyFilters() }
        typeTabs[1].setOnClickListener { selectedType = "Групповые"; updateTabUI(typeTabs, it as TextView); applyFilters() }
        typeTabs[2].setOnClickListener { selectedType = "Персональные"; updateTabUI(typeTabs, it as TextView); applyFilters() }

        // Клик по дню
        dayTabs[0].setOnClickListener { selectedDay = "ПН"; updateTabUI(dayTabs, it as TextView, true); applyFilters() }
        dayTabs[1].setOnClickListener { selectedDay = "ВТ"; updateTabUI(dayTabs, it as TextView, true); applyFilters() }
        dayTabs[2].setOnClickListener { selectedDay = "СР"; updateTabUI(dayTabs, it as TextView, true); applyFilters() }
        dayTabs[3].setOnClickListener { selectedDay = "ЧТ"; updateTabUI(dayTabs, it as TextView, true); applyFilters() }
        dayTabs[4].setOnClickListener { selectedDay = "ПТ"; updateTabUI(dayTabs, it as TextView, true); applyFilters() }
        dayTabs[5].setOnClickListener { selectedDay = "СБ"; updateTabUI(dayTabs, it as TextView, true); applyFilters() }
        dayTabs[6].setOnClickListener { selectedDay = "ВС"; updateTabUI(dayTabs, it as TextView, true); applyFilters() }

        // Чекбокс
        cbHasSpots.setOnCheckedChangeListener { _, isChecked ->
            onlyWithSpots = isChecked
            applyFilters()
        }
    }

    private fun applyFilters() {
        val filteredList = allWorkouts.filter { workout ->
            val matchDay = workout.day_of_week == selectedDay
            val matchType = if (selectedType == "Все") true else workout.type == selectedType
            val matchSpots = if (onlyWithSpots) workout.has_spots else true

            matchDay && matchType && matchSpots
        }

        adapter.updateData(filteredList, myBookedIds)
    }

    // --- ЛОГИКА ЗАПИСИ (КОРЗИНА) ---
    private fun handleBookingClick(workoutId: String, isBooked: Boolean) {
        lifecycleScope.launch {
            if (isBooked) {
                // Отмена
                if (bookingRepo.cancelWorkout(workoutId)) {
                    myBookedIds.remove(workoutId)
                    Toast.makeText(requireContext(), "Запись отменена", Toast.LENGTH_SHORT).show()
                }
            } else {
                // Запись
                if (bookingRepo.bookWorkout(workoutId)) {
                    myBookedIds.add(workoutId)
                    Toast.makeText(requireContext(), "Вы записаны!", Toast.LENGTH_SHORT).show()
                }
            }

            // Пересчитываем Итого и обновляем список
            tvTotalBookings.text = "Запланировано: ${myBookedIds.size}" // Временно считаем локально
            applyFilters()
        }
    }

    // --- ВИЗУАЛ ФИЛЬТРОВ ---
    private fun updateTabUI(tabs: List<TextView>, activeTab: TextView, isDayTab: Boolean = false) {
        for (tab in tabs) {
            tab.setBackgroundResource(R.drawable.bg_filter_inactive)
            tab.text = tab.text.toString().replace("✓ ", "")
        }
        activeTab.setBackgroundResource(R.drawable.bg_filter_active)
        if (isDayTab) activeTab.text = "✓ ${activeTab.text}"
    }
}