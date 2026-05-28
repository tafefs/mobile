package com.example.mob_dev.ui

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.lifecycleScope
import com.example.mob_dev.R
import com.example.mob_dev.SupabaseClient
import com.example.mob_dev.utils.NetworkUtils
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.builtin.Email
import io.github.jan.supabase.gotrue.OtpType
import kotlinx.coroutines.launch

class ForgotPasswordActivity : AppCompatActivity() {

    private val CHANNEL_ID = "auth_notifications"
    private var savedEmail = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forgot_password)


        val blockEmail = findViewById<LinearLayout>(R.id.blockEmailInput)
        val blockCode = findViewById<LinearLayout>(R.id.blockCodeInput)
        val blockNewPassword = findViewById<LinearLayout>(R.id.blockNewPasswordInput)

        val etEmail = findViewById<EditText>(R.id.etEmailForgot)
        val etCode = findViewById<EditText>(R.id.etCodeForgot)
        val etNewPassword = findViewById<EditText>(R.id.etNewPassword)

        val btnSend = findViewById<Button>(R.id.btnSendCode)
        val btnVerify = findViewById<Button>(R.id.btnVerifyCode)
        val btnBack = findViewById<Button>(R.id.btnBackFromForgot)
        val btnSavePassword = findViewById<Button>(R.id.btnSaveNewPassword)

        val tvTitle = findViewById<TextView>(R.id.tvTitleForgot)
        val tvSubtitle = findViewById<TextView>(R.id.tvSubtitleForgot)

        createNotificationChannel()

        btnSend.setOnClickListener {
            val email = etEmail.text.toString().trim()

            if (email.isNotBlank()) {
                btnSend.isEnabled = false

                lifecycleScope.launch {
                    try {
                        SupabaseClient.client.auth.resetPasswordForEmail(email)

                        savedEmail = email

                        showNotification("Код отправлен!", "Проверьте почту $email")

                        blockEmail.visibility = View.GONE
                        blockCode.visibility = View.VISIBLE
                        tvSubtitle.text = "Введите 6-значный код для сброса пароля"

                    } catch (e: Exception) {
                        Toast.makeText(this@ForgotPasswordActivity, "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
                        btnSend.isEnabled = true
                    }
                }
            } else {
                Toast.makeText(this, "Введите Email", Toast.LENGTH_SHORT).show()
            }
        }

        btnVerify.setOnClickListener {
            val code = etCode.text.toString().trim()

            if (code.length == 8) {

                if (!NetworkUtils.isInternetAvailable(this)) {
                    Toast.makeText(this, "Нет подключения к интернету", Toast.LENGTH_LONG).show()
                    return@setOnClickListener
                }
                btnVerify.isEnabled = false
                lifecycleScope.launch {
                    try {
                        SupabaseClient.client.auth.verifyEmailOtp(
                            type = OtpType.Email.RECOVERY,
                            email = savedEmail,
                            token = code
                        )

                        Toast.makeText(this@ForgotPasswordActivity, "Код принят!", Toast.LENGTH_SHORT).show()

                        blockCode.visibility = View.GONE
                        blockNewPassword.visibility = View.VISIBLE

                        tvTitle.text = "Новый пароль"
                        tvSubtitle.text = "Придумайте надежный пароль для вашего аккаунта"

                    } catch (e: Exception) {
                        Toast.makeText(this@ForgotPasswordActivity, "Неверный код", Toast.LENGTH_SHORT).show()
                        btnVerify.isEnabled = true
                    }
                }
            } else {
                Toast.makeText(this, "Введите 8 цифр", Toast.LENGTH_SHORT).show()
            }
        }

        btnSavePassword.setOnClickListener {
            val newPass = etNewPassword.text.toString().trim()

            if (newPass.length >= 6) {
                btnSavePassword.isEnabled = false

                lifecycleScope.launch {
                    try {
                        SupabaseClient.client.auth.updateUser {
                            password = newPass
                        }

                        Toast.makeText(this@ForgotPasswordActivity, "Пароль успешно изменен!", Toast.LENGTH_LONG).show()

                        SupabaseClient.client.auth.signOut()

                        val intent = Intent(this@ForgotPasswordActivity, LoginActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)

                    } catch (e: Exception) {
                        Toast.makeText(this@ForgotPasswordActivity, "Ошибка смены пароля: ${e.message}", Toast.LENGTH_LONG).show()
                        btnSavePassword.isEnabled = true
                    }
                }
            } else {
                Toast.makeText(this, "Пароль должен содержать минимум 6 символов", Toast.LENGTH_SHORT).show()
            }
        }

        btnBack.setOnClickListener { finish() }
    }


    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Уведомления авторизации"
            val descriptionText = "Уведомления о кодах доступа"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun showNotification(title: String, text: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101)
                return
            }
        }

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        with(NotificationManagerCompat.from(this)) {
            notify(1, builder.build())
        }
    }
}