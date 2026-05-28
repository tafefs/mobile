package com.example.mob_dev.ui

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.util.Patterns
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.mob_dev.ui.MainActivity
import com.example.mob_dev.R
import com.example.mob_dev.data.AuthRepository // Импорт вашего репозитория
import com.example.mob_dev.utils.NetworkUtils
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private var isPasswordVisible = false
    private val authRepository = AuthRepository() // Инициализация слоя данных

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Инициализация UI элементов
        val etEmail = findViewById<EditText>(R.id.etEmailLogin)
        val etPassword = findViewById<EditText>(R.id.etPasswordLogin)
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val tvRegister = findViewById<TextView>(R.id.tvGoToRegister)
        val tvForgot = findViewById<TextView>(R.id.tvForgotPassword)
        val ivToggle = findViewById<ImageView>(R.id.ivTogglePasswordLogin)
        val tvEmailError = findViewById<TextView>(R.id.tvEmailError)
        val tvPasswordError = findViewById<TextView>(R.id.tvPasswordError)

        // 1. Логика "Глазика"
        ivToggle.setOnClickListener {
            togglePasswordVisibility(etPassword, ivToggle)
        }

        // 2. Логика Входа
        btnLogin.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            // Проверяем локально (Валидация)
            val isEmailValid = validateEmail(email, etEmail, tvEmailError)
            val isPassValid = validatePassword(password, etPassword, tvPasswordError)

            // Если всё верно - обращаемся к Репозиторию
            if (isEmailValid && isPassValid) {

                if (!NetworkUtils.isInternetAvailable(this)) {
                    // Если интернета нет - показываем ошибку и останавливаем выполнение (return)
                    Toast.makeText(this, "Нет подключения к интернету", Toast.LENGTH_LONG).show()
                    return@setOnClickListener
                }

                lifecycleScope.launch {
                    val isSuccess = authRepository.loginUser(email, password)

                    if (isSuccess) {
                        Toast.makeText(this@LoginActivity, "Вход выполнен!", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                        overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
                        finish() // Раскомментировал, чтобы нельзя было вернуться назад кнопкой "Back"
                    } else {
                        Toast.makeText(this@LoginActivity, "Неверный Email или Пароль", Toast.LENGTH_SHORT).show()
                        etEmail.setBackgroundResource(R.drawable.bg_input_error)
                        etPassword.setBackgroundResource(R.drawable.bg_input_error)
                    }
                }
            }
        }

        // 3. Переходы на другие экраны
        tvRegister.setOnClickListener {
            startActivity(Intent(this, RegistrationActivity::class.java))
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
        }

        tvForgot.setOnClickListener {
            startActivity(Intent(this, ForgotPasswordActivity::class.java))
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
        }
    }

    // --- ВСПОМОГАТЕЛЬНЫЕ ФУНКЦИИ (СОБЛЮДЕНИЕ SOLID) ---

    private fun togglePasswordVisibility(etPassword: EditText, ivToggle: ImageView) {
        isPasswordVisible = !isPasswordVisible
        if (isPasswordVisible) {
            etPassword.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            ivToggle.setImageResource(R.drawable.ic_eye_visible)
        } else {
            etPassword.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            ivToggle.setImageResource(R.drawable.ic_eye_hidden)
        }
        etPassword.setSelection(etPassword.text.length)
    }

    private fun validateEmail(email: String, etEmail: EditText, tvError: TextView): Boolean {
        return if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setBackgroundResource(R.drawable.bg_input_error)
            tvError.visibility = View.VISIBLE
            false
        } else {
            etEmail.setBackgroundResource(R.drawable.bg_input_field)
            tvError.visibility = View.GONE
            true
        }
    }

    private fun validatePassword(password: String, etPassword: EditText, tvError: TextView): Boolean {
        return if (password.length < 6) {
            etPassword.setBackgroundResource(R.drawable.bg_input_error)
            tvError.visibility = View.VISIBLE
            false
        } else {
            etPassword.setBackgroundResource(R.drawable.bg_input_field)
            tvError.visibility = View.GONE
            true
        }
    }
}