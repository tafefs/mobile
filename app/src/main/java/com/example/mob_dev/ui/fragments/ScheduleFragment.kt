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

    // интерфейс
    private lateinit var rvWorkouts: RecyclerView
    private lateinit var tvTotalBookings: TextView
    private lateinit var adapter: WorkoutAdapter

    // данные
    private var allWorkouts: List<Workout> = emptyList()
    private var myBookedIds: MutableList<String> = mutableListOf()

    // начальные состояния
    private var selectedDay = "ПН"
    private var selectedType = "Все"
    private var onlyWithSpots = false

    // списки кнопок фильтров
    private lateinit var dayTabs: List<TextView>
    private lateinit var typeTabs: List<TextView>

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_schedule, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tvTotalBookings = view.findViewById(R.id.tvTotalBookings)
        rvWorkouts = view.findViewById(R.id.rvWorkouts)
        val cbHasSpots = view.findViewById<CheckBox>(R.id.cbHasSpots)

        // инициализация списка
        rvWorkouts.layoutManager = LinearLayoutManager(requireContext())
        adapter = WorkoutAdapter(emptyList(), emptyList()) { workoutId, isBooked ->
            handleBookingClick(workoutId, isBooked)
        }
        rvWorkouts.adapter = adapter

        // инициализация фильтров
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

        loadDataFromServer()
    }

    private fun loadDataFromServer() {
        if (!NetworkUtils.isInternetAvailable(requireContext())) {
            Toast.makeText(requireContext(), "Нет интернета", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            // загрущка всех тренировок
            allWorkouts = bookingRepo.getWorkouts()

            // тренировки на которые записан
            val bookedIdsFromDb = bookingRepo.getMyBookedWorkoutIds()
            myBookedIds = bookedIdsFromDb.toMutableList() // Обновляем локальный список

            tvTotalBookings.text = "Запланировано: ${myBookedIds.size}"

            applyFilters()
        }
    }

    private fun setupFilters(cbHasSpots: CheckBox) {
        typeTabs[0].setOnClickListener { selectedType = "Все"; updateTabUI(typeTabs, it as TextView); applyFilters() }
        typeTabs[1].setOnClickListener { selectedType = "Групповые"; updateTabUI(typeTabs, it as TextView); applyFilters() }
        typeTabs[2].setOnClickListener { selectedType = "Персональные"; updateTabUI(typeTabs, it as TextView); applyFilters() }

        dayTabs[0].setOnClickListener { selectedDay = "ПН"; updateTabUI(dayTabs, it as TextView, true); applyFilters() }
        dayTabs[1].setOnClickListener { selectedDay = "ВТ"; updateTabUI(dayTabs, it as TextView, true); applyFilters() }
        dayTabs[2].setOnClickListener { selectedDay = "СР"; updateTabUI(dayTabs, it as TextView, true); applyFilters() }
        dayTabs[3].setOnClickListener { selectedDay = "ЧТ"; updateTabUI(dayTabs, it as TextView, true); applyFilters() }
        dayTabs[4].setOnClickListener { selectedDay = "ПТ"; updateTabUI(dayTabs, it as TextView, true); applyFilters() }
        dayTabs[5].setOnClickListener { selectedDay = "СБ"; updateTabUI(dayTabs, it as TextView, true); applyFilters() }
        dayTabs[6].setOnClickListener { selectedDay = "ВС"; updateTabUI(dayTabs, it as TextView, true); applyFilters() }


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

    // запись
    private fun handleBookingClick(workoutId: String, isBooked: Boolean) {
        lifecycleScope.launch {
            if (isBooked) {
                // отмена
                if (bookingRepo.cancelWorkout(workoutId)) {
                    myBookedIds.remove(workoutId)
                    Toast.makeText(requireContext(), "Запись отменена", Toast.LENGTH_SHORT).show()
                }
            } else {
                // запись
                if (bookingRepo.bookWorkout(workoutId)) {
                    myBookedIds.add(workoutId)
                    Toast.makeText(requireContext(), "Вы записаны!", Toast.LENGTH_SHORT).show()
                }
            }

            // счет записанных
            tvTotalBookings.text = "Запланировано: ${myBookedIds.size}"
            applyFilters()
        }
    }

    private fun updateTabUI(tabs: List<TextView>, activeTab: TextView, isDayTab: Boolean = false) {
        for (tab in tabs) {
            tab.setBackgroundResource(R.drawable.bg_filter_inactive)
            tab.text = tab.text.toString().replace("✓ ", "")
        }
        activeTab.setBackgroundResource(R.drawable.bg_filter_active)
        if (isDayTab) activeTab.text = "✓ ${activeTab.text}"
    }
}