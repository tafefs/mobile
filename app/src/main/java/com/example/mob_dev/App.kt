package com.example.mob_dev

import android.app.Application
import com.yandex.mapkit.MapKitFactory

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        // ВАЖНО: Инициализация должна быть ЗДЕСЬ, один раз на всё приложение
        MapKitFactory.setApiKey("54868aa9-f115-4365-830c-baab56c994a0") // Вставьте ваш реальный ключ
    }
}