package com.example.mob_dev.ui

import android.content.Intent
import android.os.Bundle
import android.widget.TextView // <-- Исправляет ошибку Unresolved reference 'TextView'
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.mob_dev.ui.MainActivity
import com.example.mob_dev.R
import com.example.mob_dev.SupabaseClient
import io.github.jan.supabase.gotrue.SessionStatus
import io.github.jan.supabase.gotrue.auth
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        val logo = findViewById<TextView>(R.id.tvSplashLogo)
        logo.alpha = 0f
        logo.animate().alpha(1f).setDuration(1500).start()

        lifecycleScope.launch {
            delay(2000)

            // Ждем загрузки сессии из памяти
            val status = SupabaseClient.client.auth.sessionStatus.first { it !is SessionStatus.LoadingFromStorage }


            if (status is SessionStatus.Authenticated) {
                // ИСПРАВЛЕНИЕ: Пересылаем метку "OPEN_FRAGMENT" дальше в MainActivity
                val nextIntent = Intent(this@SplashActivity, MainActivity::class.java).apply {
                    putExtra("OPEN_FRAGMENT", intent.getStringExtra("OPEN_FRAGMENT"))
                }
                startActivity(nextIntent)
                overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
            } else {
                startActivity(Intent(this@SplashActivity, LoginActivity::class.java))
                overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
            }

            finish()
        }
    }
}