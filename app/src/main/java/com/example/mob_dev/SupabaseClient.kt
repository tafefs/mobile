package com.example.mob_dev
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.storage.Storage

object SupabaseClient {
    private const val SUPABASE_URL = "https://glwkkrakomaiygqotlem.supabase.co"
    private const val SUPABASE_KEY = "sb_publishable_1sNm2_G-o_miN3wMNUw78g_AYtZ9558"

    val client = createSupabaseClient(
        supabaseUrl = SUPABASE_URL,
        supabaseKey = SUPABASE_KEY
    ) {
        install(Auth) {
            autoSaveToStorage = true // разрешает сохранение в SharedPreferences
            alwaysAutoRefresh = true // автоматически обновляет токен если он просрочен
        }
        install(Postgrest)
        install(Storage)
    }
}