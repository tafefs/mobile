package com.example.mob_dev.ui.fragments

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.mob_dev.R
import com.example.mob_dev.SupabaseClient
import com.example.mob_dev.data.ProfileRepository
import com.example.mob_dev.ui.LoginActivity
import com.example.mob_dev.ui.MainActivity
import io.github.jan.supabase.gotrue.auth
import kotlinx.coroutines.launch
import android.widget.ProgressBar
import android.widget.LinearLayout
import android.widget.ImageView
import coil.load
import coil.transform.CircleCropTransformation

class ProfileViewFragment : Fragment() {

    private val profileRepository = ProfileRepository()

    private lateinit var progressBar: ProgressBar
    private lateinit var mainContent: LinearLayout

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_profile_view, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        progressBar = view.findViewById(R.id.progressBarProfile)
        mainContent = view.findViewById(R.id.mainProfileContent)

        val ivAvatar = view.findViewById<ImageView>(R.id.ivAvatarView)

        val tvUserName = view.findViewById<TextView>(R.id.tvUserNameView)
        val tvEmail = view.findViewById<TextView>(R.id.tvEmailView)
        val tvDob = view.findViewById<TextView>(R.id.tvDobView)
        val tvPhone = view.findViewById<TextView>(R.id.tvPhoneView)

        val btnGoToEdit = view.findViewById<Button>(R.id.btnGoToEdit)
        val btnLogout = view.findViewById<TextView>(R.id.btnLogoutView)
        val btnContactSupport = view.findViewById<TextView>(R.id.btnContactSupport)


        // загрузка данных

        loadProfileData(tvUserName, tvEmail, tvDob, tvPhone, ivAvatar)


        btnGoToEdit.setOnClickListener {
            (requireActivity() as MainActivity).loadFragment(ProfileEditFragment(), true)
        }

        btnContactSupport.setOnClickListener {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = android.net.Uri.parse("mailto:")
                putExtra(Intent.EXTRA_EMAIL, arrayOf("sasha.kalinin1@mail.ru"))
                putExtra(Intent.EXTRA_SUBJECT, "Вопрос по приложению")
            }

            if (intent.resolveActivity(requireActivity().packageManager) != null) {
                startActivity(intent)
            } else {
                Toast.makeText(requireContext(), "Нет приложения для отправки почты", Toast.LENGTH_SHORT).show()
            }
        }


        btnLogout.setOnClickListener {
            lifecycleScope.launch {
                try {
                    SupabaseClient.client.auth.signOut()
                    val intent = Intent(requireActivity(), LoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(requireContext(), "Ошибка при выходе", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun loadProfileData(tvName: TextView, tvEmail: TextView, tvDob: TextView, tvPhone: TextView?, ivAvatar: ImageView) {
        progressBar.visibility = View.VISIBLE
        mainContent.visibility = View.GONE

        lifecycleScope.launch {
            val profile = profileRepository.getProfile()

            if (profile != null) {
                val fullName = "${profile.first_name ?: ""} ${profile.last_name ?: ""}".trim()
                tvName.text = if (fullName.isNotEmpty()) fullName else "Имя не указано"

                tvEmail.text = profile.email ?: "Email не указан"
                tvDob.text = profile.birth_date ?: "Дата не указана"
                tvPhone?.text = profile.phone ?: "Телефон не указан"

                if (!profile.avatar_url.isNullOrEmpty()) {
                    ivAvatar.load(profile.avatar_url) {
                        crossfade(true)
                        transformations(CircleCropTransformation())
                    }
                }
            }

                progressBar.visibility = View.GONE
                mainContent.alpha = 0f
                mainContent.visibility = View.VISIBLE
                mainContent.animate().alpha(1f).setDuration(300).start()
        }
    }
}