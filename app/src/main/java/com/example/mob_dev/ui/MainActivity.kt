package com.example.mob_dev.ui

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.mob_dev.R
import com.example.mob_dev.ui.fragments.HomeFragment
import com.example.mob_dev.ui.fragments.NotificationsFragment
import com.example.mob_dev.ui.fragments.ProfileViewFragment
import com.example.mob_dev.ui.fragments.ScheduleFragment
import com.example.mob_dev.ui.fragments.TrackingFragment
import com.yandex.mapkit.MapKitFactory

class MainActivity : AppCompatActivity() {

    enum class NavTab {
        HOME, TRACKING, SCHEDULE, NOTIFICATIONS, PROFILE
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        MapKitFactory.initialize(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Находим кнопки-контейнеры
        val btnHome = findViewById<LinearLayout>(R.id.btnNavHome)
        val btnTracking = findViewById<LinearLayout>(R.id.btnNavTracking)
        val btnSchedule = findViewById<LinearLayout>(R.id.btnNavSchedule)
        val btnNotifications = findViewById<LinearLayout>(R.id.btnNavNotifications)
        val btnProfile = findViewById<LinearLayout>(R.id.btnNavProfile)

        // Проверяем, откуда пришел запуск
        val openFragment = intent.getStringExtra("OPEN_FRAGMENT")

        if (openFragment == "notifications") {
            // Сценарий А: Переход из пуш-уведомления
            loadFragment(NotificationsFragment())
            updateNavUI(NavTab.NOTIFICATIONS)
        } else if (savedInstanceState == null) {
            // Сценарий Б: Обычный первый запуск приложения
            loadFragment(HomeFragment())
            updateNavUI(NavTab.HOME)
        }

        // ВТОРОЙ ДУБЛИРУЮЩИЙ БЛОК УДАЛЕН ОТСЮДА!

        // ОБРАБОТКА КЛИКОВ НИЖНЕГО МЕНЮ
        btnHome.setOnClickListener {
            loadFragment(HomeFragment())
            updateNavUI(NavTab.HOME)
        }

        btnTracking.setOnClickListener {
            loadFragment(TrackingFragment())
            updateNavUI(NavTab.TRACKING)
        }

        btnSchedule.setOnClickListener {
            loadFragment(ScheduleFragment())
            updateNavUI(NavTab.SCHEDULE)
        }

        btnNotifications.setOnClickListener {
            loadFragment(NotificationsFragment())
            updateNavUI(NavTab.NOTIFICATIONS)
        }

        btnProfile.setOnClickListener {
            loadFragment(ProfileViewFragment())
            updateNavUI(NavTab.PROFILE)
        }
    }

    // МЕТОД ЗАГРУЗКИ ФРАГМЕНТА
    fun loadFragment(fragment: Fragment, addToBackStack: Boolean = false) {
        val transaction = supportFragmentManager
            .beginTransaction()
            .setCustomAnimations(R.anim.fade_in, R.anim.fade_out)
            .replace(R.id.fragment_container, fragment)

        if (addToBackStack) {
            transaction.addToBackStack(null)
        }
        transaction.commit()
    }

    // МЕТОД ОБНОВЛЕНИЯ ИНТЕРФЕЙСА МЕНЮ
    private fun updateNavUI(activeTab: NavTab) {
        val colorActive = ContextCompat.getColor(this, R.color.accent_green)
        val colorInactive = ContextCompat.getColor(this, R.color.text_hint)

        val ivHome = findViewById<ImageView>(R.id.ivNavHome)
        val tvHome = findViewById<TextView>(R.id.tvNavHome)

        val ivTracking = findViewById<ImageView>(R.id.ivNavTracking)
        val tvTracking = findViewById<TextView>(R.id.tvNavTracking)

        val ivSchedule = findViewById<ImageView>(R.id.ivNavSchedule)
        val tvSchedule = findViewById<TextView>(R.id.tvNavSchedule)

        val ivNotif = findViewById<ImageView>(R.id.ivNavNotifications)
        val tvNotif = findViewById<TextView>(R.id.tvNavNotifications)

        val ivProfile = findViewById<ImageView>(R.id.ivNavProfile)
        val tvProfile = findViewById<TextView>(R.id.tvNavProfile)

        val allIcons = listOf(ivHome, ivTracking, ivSchedule, ivNotif, ivProfile)
        val allTexts = listOf(tvHome, tvTracking, tvSchedule, tvNotif, tvProfile)

        for (icon in allIcons) icon.setColorFilter(colorInactive)
        for (text in allTexts) text.setTextColor(colorInactive)

        when (activeTab) {
            NavTab.HOME -> {
                ivHome.setColorFilter(colorActive)
                tvHome.setTextColor(colorActive)
            }
            NavTab.TRACKING -> {
                ivTracking.setColorFilter(colorActive)
                tvTracking.setTextColor(colorActive)
            }
            NavTab.SCHEDULE -> {
                ivSchedule.setColorFilter(colorActive)
                tvSchedule.setTextColor(colorActive)
            }
            NavTab.NOTIFICATIONS -> {
                ivNotif.setColorFilter(colorActive)
                tvNotif.setTextColor(colorActive)
            }
            NavTab.PROFILE -> {
                ivProfile.setColorFilter(colorActive)
                tvProfile.setTextColor(colorActive)
            }
        }
    }
}