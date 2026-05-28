package com.example.mob_dev.ui.fragments

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import coil.load
import com.example.mob_dev.R
import com.example.mob_dev.SupabaseClient
import com.example.mob_dev.data.BookingRepository
import com.example.mob_dev.ui.MainActivity
import io.github.jan.supabase.gotrue.auth
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

class HomeFragment : Fragment() {

    private val bookingRepo = BookingRepository()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val btnMap = view.findViewById<Button>(R.id.btnOpenMap)
        val ivQrCode = view.findViewById<ImageView>(R.id.ivQrCode)
        val tvNearestWorkout = view.findViewById<TextView>(R.id.tvNearestWorkout)

        val tvNews1 = view.findViewById<TextView>(R.id.tvNewsTitle1)
        val tvNews2 = view.findViewById<TextView>(R.id.tvNewsTitle2)
        val ivEventImage = view.findViewById<ImageView>(R.id.ivEventImage)
        val tvEventDesc = view.findViewById<TextView>(R.id.tvEventDescription)

        btnMap.setOnClickListener {
            (requireActivity() as MainActivity).loadFragment(MapFragment(), true)
        }

        loadHomeData(ivQrCode, tvNearestWorkout, tvNews1, tvNews2, ivEventImage, tvEventDesc)
    }

    private fun loadHomeData(
        ivQr: ImageView,
        tvWorkout: TextView,
        tvNews1: TextView,
        tvNews2: TextView,
        ivEventImg: ImageView,
        tvEventText: TextView
    ) {
        lifecycleScope.launch {
            try {
                // кр код с айди пользователя
                val userId = SupabaseClient.client.auth.currentUserOrNull()?.id
                if (userId != null) {
                    val qrUrl = "https://api.qrserver.com/v1/create-qr-code/?size=150x150&data=$userId"

                    ivQr.load(qrUrl) {
                        crossfade(true)
                    }
                }

                // скачивание данных
                val workoutDeferred = async { bookingRepo.getNearestBookedWorkout() }
                val newsDeferred = async { bookingRepo.getNews() }
                val eventDeferred = async { bookingRepo.getLatestEvent() }

                val nearestWorkout = workoutDeferred.await()
                val newsList = newsDeferred.await()
                val latestEvent = eventDeferred.await()

                // отображение тренировки
                if (nearestWorkout != null) {
                    tvWorkout.text = "Ближайшая тренировка:\n${nearestWorkout.title}\n${nearestWorkout.day_of_week} в ${nearestWorkout.time}"
                } else {
                    tvWorkout.text = "Нет запланированных\nтренировок"
                }

                // отображение новостей
                if (newsList.size >= 2) {
                    tvNews1.text = newsList[0].title
                    tvNews2.text = newsList[1].title
                } else if (newsList.size == 1) {
                    tvNews1.text = newsList[0].title
                    tvNews2.text = "Скоро новые новости!"
                } else {
                    tvNews1.text = "Новостей пока нет"
                    tvNews2.text = "Новостей пока нет"
                }

                // отображение мероприятия
                if (latestEvent != null) {
                    tvEventText.text = latestEvent.title

                    ivEventImg.load(latestEvent.image_url) {
                        crossfade(true)
                        placeholder(android.R.drawable.ic_menu_rotate)
                    }
                }

            } catch (e: Exception) {
                Log.e("HomeError", "Ошибка загрузки данных: ${e.message}")
                tvWorkout.text = "Ошибка загрузки"
                tvNews1.text = "Ошибка"
                tvNews2.text = "Ошибка"
                tvEventText.text = "Не удалось загрузить мероприятие"
            }
        }
    }
}