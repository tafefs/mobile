package com.example.mob_dev.ui.fragments

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import coil.load
import coil.transform.CircleCropTransformation
import com.example.mob_dev.R
import com.example.mob_dev.data.ProfileRepository
import com.example.mob_dev.utils.NetworkUtils
import kotlinx.coroutines.launch
import java.util.Calendar

class ProfileEditFragment : Fragment() {

    private val profileRepository = ProfileRepository()
    private var currentAvatarUrl: String? = null
    private var selectedImageBytes: ByteArray? = null
    private lateinit var ivAvatar: ImageView

    private val pickMedia = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            ivAvatar.load(uri) {
                transformations(CircleCropTransformation())
            }
            // Сжимаем фото перед отправкой
            selectedImageBytes = compressImage(uri)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_profile_edit, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val etName = view.findViewById<EditText>(R.id.etNameProfile)
        val etSurname = view.findViewById<EditText>(R.id.etSurnameProfile)
        val etDob = view.findViewById<EditText>(R.id.etDobProfile)
        val etPhone = view.findViewById<EditText>(R.id.etPhoneProfile)
        val etEmail = view.findViewById<EditText>(R.id.etEmailProfile)

        val btnSave = view.findViewById<Button>(R.id.btnSaveProfile)
        val btnCancel = view.findViewById<Button>(R.id.btnCancelProfile)
        ivAvatar = view.findViewById(R.id.ivAvatar)

        etEmail.isEnabled = false
        etEmail.alpha = 0.5f

        // --- ПОДКЛЮЧАЕМ МАСКИ ВВОДА ---
        setupDateMask(etDob)   // Маска даты
        setupPhoneMask(etPhone) // Маска телефона

        ivAvatar.setOnClickListener {
            pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }

        // Подгружаем данные
        lifecycleScope.launch {
            val profile = profileRepository.getProfile()
            if (profile != null) {
                etName.setText(profile.first_name)
                etSurname.setText(profile.last_name)
                etDob.setText(profile.birth_date)
                etPhone.setText(profile.phone)
                etEmail.setText(profile.email)
                currentAvatarUrl = profile.avatar_url

                if (!currentAvatarUrl.isNullOrEmpty()) {
                    ivAvatar.load(currentAvatarUrl) {
                        crossfade(true)
                        transformations(CircleCropTransformation())
                    }
                }
            }
        }

        btnSave.setOnClickListener {
            if (!NetworkUtils.isInternetAvailable(requireContext())) {
                Toast.makeText(requireContext(), "Нет интернета", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val newName = etName.text.toString().trim()
            val newSurname = etSurname.text.toString().trim()
            val newDob = etDob.text.toString().trim()
            val newPhone = etPhone.text.toString().trim()

            // Простая валидация перед сохранением
            if (newDob.isNotEmpty() && newDob.length != 10) {
                Toast.makeText(requireContext(), "Введите корректную дату", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (newPhone.isNotEmpty() && newPhone.length != 18) {
                Toast.makeText(requireContext(), "Введите корректный номер телефона", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            btnSave.isEnabled = false
            btnSave.text = "Загрузка..."

            lifecycleScope.launch {
                try {
                    var finalAvatarUrl = currentAvatarUrl

                    if (selectedImageBytes != null) {
                        val uploadedUrl = profileRepository.uploadAvatar(selectedImageBytes!!)
                        if (uploadedUrl != null) {
                            finalAvatarUrl = "$uploadedUrl?t=${System.currentTimeMillis()}"
                        }
                    }

                    val isSuccess = profileRepository.updateProfile(newName, newSurname, newDob, newPhone, finalAvatarUrl)

                    if (isSuccess) {
                        Toast.makeText(requireContext(), "Профиль обновлен!", Toast.LENGTH_SHORT).show()
                        requireActivity().supportFragmentManager.popBackStack()
                    } else {
                        Toast.makeText(requireContext(), "Ошибка при сохранении", Toast.LENGTH_SHORT).show()
                        btnSave.isEnabled = true
                        btnSave.text = "Сохранить изменения"
                    }
                } catch (e: Exception) {
                    btnSave.isEnabled = true
                    btnSave.text = "Сохранить изменения"
                }
            }
        }

        btnCancel.setOnClickListener {
            requireActivity().supportFragmentManager.popBackStack()
        }
    }

    // ==========================================
    // ВСПОМОГАТЕЛЬНЫЕ ФУНКЦИИ (МАСКИ И СЖАТИЕ)
    // ==========================================

    // 1. МАСКА ДАТЫ РОЖДЕНИЯ (ДД.ММ.ГГГГ)
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

    // 2. УМНАЯ МАСКА ТЕЛЕФОНА (+7 (XXX) XXX-XX-XX)
    private fun setupPhoneMask(etPhone: EditText) {
        etPhone.addTextChangedListener(object : TextWatcher {
            private var isUpdating = false

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (isUpdating) {
                    isUpdating = false
                    return
                }

                // Стираем всё кроме цифр
                var clean = s.toString().replace("[^\\d]".toRegex(), "")

                if (clean.isEmpty()) {
                    isUpdating = true
                    etPhone.setText("")
                    return
                }

                // Если пользователь случайно начал вводить 7 или 8 в начале, отрезаем её
                if (clean.startsWith("7") || clean.startsWith("8")) {
                    clean = clean.substring(1)
                }

                // Собираем маску по кусочкам
                val formatted = StringBuilder("+7 ")
                if (clean.isNotEmpty()) {
                    formatted.append("(")
                    val loc = Math.min(clean.length, 3)
                    formatted.append(clean.substring(0, loc))
                    if (clean.length > 3) {
                        formatted.append(") ")
                        val loc2 = Math.min(clean.length, 6)
                        formatted.append(clean.substring(3, loc2))
                        if (clean.length > 6) {
                            formatted.append("-")
                            val loc3 = Math.min(clean.length, 8)
                            formatted.append(clean.substring(6, loc3))
                            if (clean.length > 8) {
                                formatted.append("-")
                                val loc4 = Math.min(clean.length, 10)
                                formatted.append(clean.substring(8, loc4))
                            }
                        }
                    }
                }

                isUpdating = true
                etPhone.setText(formatted.toString())
                etPhone.setSelection(etPhone.text.length) // Курсор всегда в конец
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    // 3. СЖАТИЕ КАРТИНКИ
    private fun compressImage(uri: android.net.Uri): ByteArray? {
        return try {
            val inputStream = requireContext().contentResolver.openInputStream(uri) ?: return null
            val buffer = java.io.ByteArrayOutputStream()
            var nRead: Int
            val data = ByteArray(16384)
            while (inputStream.read(data, 0, data.size).also { nRead = it } != -1) {
                buffer.write(data, 0, nRead)
            }
            inputStream.close()

            val originalBytes = buffer.toByteArray()
            val originalBitmap = android.graphics.BitmapFactory.decodeByteArray(originalBytes, 0, originalBytes.size)

            val maxSize = 800
            val ratio = Math.min(maxSize.toFloat() / originalBitmap.width, maxSize.toFloat() / originalBitmap.height)
            val bitmap = if (ratio < 1.0f) {
                android.graphics.Bitmap.createScaledBitmap(originalBitmap, (originalBitmap.width * ratio).toInt(), (originalBitmap.height * ratio).toInt(), true)
            } else {
                originalBitmap
            }

            val outputStream = java.io.ByteArrayOutputStream()
            bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 70, outputStream)
            outputStream.toByteArray()
        } catch (e: Exception) {
            null
        }
    }
}