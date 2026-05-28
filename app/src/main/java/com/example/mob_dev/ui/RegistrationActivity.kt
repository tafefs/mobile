package com.example.mob_dev.ui

import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.util.Patterns
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.mob_dev.R
import com.example.mob_dev.data.AuthRepository
import com.example.mob_dev.utils.NetworkUtils
import kotlinx.coroutines.launch
import java.util.Calendar

class RegistrationActivity : AppCompatActivity() {

    private var isPasswordVisible = false
    private val authRepository = AuthRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registration)

        val btnRegister = findViewById<Button>(R.id.btnRegister)
        val etName = findViewById<EditText>(R.id.etNameReg)
        val etSurname = findViewById<EditText>(R.id.etSurnameReg)
        val etDob = findViewById<EditText>(R.id.etDobReg)
        val etEmail = findViewById<EditText>(R.id.etEmailReg)
        val etPassword = findViewById<EditText>(R.id.etPassReg)

        val ivToggle = findViewById<ImageView>(R.id.ivTogglePasswordReg)
        val tvEmailError = findViewById<TextView>(R.id.tvEmailErrorReg)
        val tvPasswordError = findViewById<TextView>(R.id.tvPasswordErrorReg)

        setupDateMask(etDob)


        ivToggle.setOnClickListener {
            togglePasswordVisibility(etPassword, ivToggle)
        }


        btnRegister.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()
            val name = etName.text.toString().trim()
            val surname = etSurname.text.toString().trim()


            val isEmailValid = validateEmail(email, etEmail, tvEmailError)
            val isPassValid = validatePassword(password, etPassword, tvPasswordError)
            val isDateValid = validateDate(etDob.text.toString())


            if (isEmailValid && isPassValid && isDateValid) {

                if (!NetworkUtils.isInternetAvailable(this)) {

                    Toast.makeText(this, "Нет подключения к интернету", Toast.LENGTH_LONG).show()
                    return@setOnClickListener
                }

                lifecycleScope.launch {
                    val isSuccess = authRepository.registerUser(email, password, name, surname)

                    if (isSuccess) {
                        Toast.makeText(this@RegistrationActivity, "Регистрация успешна!", Toast.LENGTH_LONG).show()
                        finish()
                    } else {
                        Toast.makeText(this@RegistrationActivity, "Ошибка регистрации. Возможно, Email уже занят.", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }


    private fun setupDateMask(etDob: EditText) {
        etDob.addTextChangedListener(object : TextWatcher {
            private var current = ""
            private val ddmmyyyy = "DDMMYYYY"
            private val cal = Calendar.getInstance()

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (s.toString() != current) {
                    var clean = s.toString().replace("[^\\d.]|\\.".toRegex(), "")
                    val cleanC = current.replace("[^\\d.]|\\.".toRegex(), "")

                    val cl = clean.length
                    var sel = cl
                    var i = 2
                    while (i <= cl && i < 6) { sel++; i += 2 }
                    if (clean == cleanC) sel--

                    if (clean.length < 8) {
                        clean += ddmmyyyy.substring(clean.length)
                    } else {
                        var day = clean.substring(0, 2).toInt()
                        var mon = clean.substring(2, 4).toInt()
                        var year = clean.substring(4, 8).toInt()

                        mon = if (mon < 1) 1 else if (mon > 12) 12 else mon
                        cal.set(Calendar.MONTH, mon - 1)
                        year = if (year < 1900) 1900 else if (year > 2100) 2100 else year
                        cal.set(Calendar.YEAR, year)

                        day = if (day > cal.getActualMaximum(Calendar.DATE)) cal.getActualMaximum(Calendar.DATE) else day
                        clean = String.format("%02d%02d%02d", day, mon, year)
                    }

                    clean = String.format("%s.%s.%s", clean.substring(0, 2), clean.substring(2, 4), clean.substring(4, 8))
                    sel = if (sel < 0) 0 else sel
                    current = clean
                    etDob.setText(current)
                    etDob.setSelection(if (sel < current.length) sel else current.length)
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

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

    private fun validateDate(dateStr: String): Boolean {
        return if (dateStr.length != 10) {
            Toast.makeText(this, "Введите корректную дату", Toast.LENGTH_SHORT).show()
            false
        } else {
            true
        }
    }
}