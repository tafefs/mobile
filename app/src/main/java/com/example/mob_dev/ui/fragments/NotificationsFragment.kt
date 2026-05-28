package com.example.mob_dev.ui.fragments

import android.Manifest
import android.app.AlertDialog
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mob_dev.R
import com.example.mob_dev.data.BookingRepository
import com.example.mob_dev.ui.NotificationAdapter
import com.example.mob_dev.utils.NetworkUtils
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.launch
import android.app.PendingIntent
import android.content.Intent
import com.example.mob_dev.ui.SplashActivity

class NotificationsFragment : Fragment() {

    private val bookingRepo = BookingRepository()
    private lateinit var rvNotifications: RecyclerView
    private lateinit var adapter: NotificationAdapter
    private lateinit var progressBar: ProgressBar
    private lateinit var mainContent: ScrollView

    private val CHANNEL_ID = "app_internal_notifications"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_notifications, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        progressBar = view.findViewById(R.id.progressBarNotifications)
        mainContent = view.findViewById(R.id.mainNotificationsContent)
        rvNotifications = view.findViewById(R.id.rvNotifications)
        val btnAdd = view.findViewById<FloatingActionButton>(R.id.btnAddNotification)

        createNotificationChannel()

        rvNotifications.layoutManager = LinearLayoutManager(requireContext())
        adapter = NotificationAdapter(emptyList()) { id, position ->
            deleteNotification(id, position)
        }
        rvNotifications.adapter = adapter

        btnAdd.setOnClickListener {
            showCreateNotificationDialog()
        }

        loadNotifications()
    }

    private fun loadNotifications() {
        progressBar.visibility = View.VISIBLE
        mainContent.visibility = View.GONE

        lifecycleScope.launch {
            try {
                val notifications = bookingRepo.getNotifications()
                adapter.updateData(notifications)
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Ошибка загрузки", Toast.LENGTH_SHORT).show()
            } finally {
                progressBar.visibility = View.GONE
                mainContent.visibility = View.VISIBLE
            }
        }
    }

    private fun showCreateNotificationDialog() {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Создать уведомление")

        val input = EditText(requireContext())
        input.hint = "Введите текст уведомления"
        builder.setView(input)

        val types = arrayOf("Напоминание (Белый)", "Предупреждение (Оранжевый)", "Акция (Зеленый)")
        val typeKeys = arrayOf("reminder", "warning", "promo")

        builder.setItems(types) { dialog, which ->
            val text = input.text.toString().trim()
            val selectedType = typeKeys[which]

            if (text.isNotEmpty()) {
                lifecycleScope.launch {
                    val success = bookingRepo.addNotification(text, selectedType)
                    if (success) {

                        // === НАША МАГИЯ: ВЫЗЫВАЕМ НАСТОЯЩЕЕ СИСТЕМНОЕ УВЕДОМЛЕНИЕ В ШТОРКЕ ===
                        val pushTitle = when(selectedType) {
                            "reminder" -> "Напоминание о занятии"
                            "warning" -> "Внимание! Важное сообщение"
                            else -> "Акция от фитнес-центра"
                        }
                        showSystemNotification(pushTitle, text)
                        // ===================================================================

                        Toast.makeText(requireContext(), "Уведомление создано!", Toast.LENGTH_SHORT).show()
                        loadNotifications() // Перезагружаем список
                    } else {
                        Toast.makeText(requireContext(), "Ошибка создания", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                Toast.makeText(requireContext(), "Текст не может быть пустым", Toast.LENGTH_SHORT).show()
            }
        }

        builder.setNegativeButton("Отмена") { dialog, _ -> dialog.cancel() }
        builder.show()
    }

    private fun deleteNotification(id: String, position: Int) {
        if (!NetworkUtils.isInternetAvailable(requireContext())) {
            Toast.makeText(requireContext(), "Нет интернета", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            val success = bookingRepo.deleteNotification(id)
            if (success) {
                Toast.makeText(requireContext(), "Уведомление прочитано", Toast.LENGTH_SHORT).show()
                val updatedList = bookingRepo.getNotifications()
                adapter.updateData(updatedList)
            } else {
                Toast.makeText(requireContext(), "Ошибка", Toast.LENGTH_SHORT).show()
            }
        }
    }



    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Внутренние уведомления"
            val descriptionText = "Уведомления от фитнес-центра"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager = requireContext().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun showSystemNotification(title: String, text: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 102)
                return
            }
        }

        val intent = Intent(requireContext(), SplashActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("OPEN_FRAGMENT", "notifications")
        }

        val pendingIntent = PendingIntent.getActivity(
            requireContext(),
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(requireContext(), CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        with(NotificationManagerCompat.from(requireContext())) {
            val uniqueId = System.currentTimeMillis().toInt()
            notify(uniqueId, builder.build())
        }
    }
}