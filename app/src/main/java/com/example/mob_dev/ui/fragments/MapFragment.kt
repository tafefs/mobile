package com.example.mob_dev.ui.fragments

import android.Manifest
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.mob_dev.R
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.yandex.mapkit.Animation
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.map.CameraPosition
import com.yandex.mapkit.mapview.MapView
import com.yandex.mapkit.user_location.UserLocationLayer
import com.yandex.mapkit.layers.ObjectEvent
import com.yandex.mapkit.user_location.UserLocationObjectListener
import com.yandex.mapkit.user_location.UserLocationView
import com.yandex.runtime.image.ImageProvider

class MapFragment : Fragment(), UserLocationObjectListener {

    private lateinit var mapView: MapView
    private lateinit var userLocationLayer: UserLocationLayer

    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false)) {
            enableUserLocation()
        } else {
            Toast.makeText(requireContext(), "Разрешение отклонено", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_map, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mapView = view.findViewById(R.id.mapview)
        val btnBack = view.findViewById<TextView>(R.id.btnBackFromMap)
        val btnMyLocation = view.findViewById<FloatingActionButton>(R.id.btnMyLocation)

        // 1. Кнопка НАЗАД
        btnBack.setOnClickListener {
            requireActivity().supportFragmentManager.popBackStack()
        }

        // 2. СТАВИМ МЕТКУ КЛУБА (С конвертацией XML в Bitmap)
        val fitnessClubPoint = Point(53.397213, 58.984059) // Координаты клуба

        val clubBitmap = getBitmapFromVectorDrawable(requireContext(), R.drawable.ic_pin_club)
        if (clubBitmap != null) {
            val clubIcon = ImageProvider.fromBitmap(clubBitmap)
            mapView.map.mapObjects.addPlacemark(fitnessClubPoint, clubIcon)
        }

        moveToPosition(fitnessClubPoint)

        // 3. Запрос геолокации
        locationPermissionRequest.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION))

        // 4. КНОПКА "ГДЕ Я"
        btnMyLocation.setOnClickListener {
            if (::userLocationLayer.isInitialized && userLocationLayer.cameraPosition() != null) {
                val userPoint = userLocationLayer.cameraPosition()!!.target
                moveToPosition(userPoint)
            } else {
                Toast.makeText(requireContext(), "Ищем спутники...", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun moveToPosition(point: Point) {
        mapView.map.move(
            CameraPosition(point, 15.0f, 0.0f, 0.0f),
            Animation(Animation.Type.SMOOTH, 1.5f),
            null
        )
    }

    private fun enableUserLocation() {
        val mapKit = MapKitFactory.getInstance()
        userLocationLayer = mapKit.createUserLocationLayer(mapView.mapWindow)
        userLocationLayer.isVisible = true
        userLocationLayer.isHeadingEnabled = true
        userLocationLayer.setObjectListener(this)
    }

    // --- ИЗМЕНЕНИЕ ИКОНКИ ВАШЕГО МЕСТОПОЛОЖЕНИЯ ---
    override fun onObjectAdded(userLocationView: UserLocationView) {
        // Меняем цвет радиуса (полупрозрачный зеленый)
        userLocationView.accuracyCircle.fillColor = android.graphics.Color.parseColor("#4466FF89")

        // Меняем саму иконку на вашу кастомную
        val userBitmap = getBitmapFromVectorDrawable(requireContext(), R.drawable.ic_user_location) // Убедитесь, что этот файл есть
        if (userBitmap != null) {
            val userIcon = ImageProvider.fromBitmap(userBitmap)
            userLocationView.pin.setIcon(userIcon)    // Иконка когда вы стоите
            userLocationView.arrow.setIcon(userIcon)  // Иконка когда вы идете
        }
    }

    override fun onObjectRemoved(view: UserLocationView) {}
    override fun onObjectUpdated(view: UserLocationView, event: ObjectEvent) {}

    // --- ВСПОМОГАТЕЛЬНАЯ ФУНКЦИЯ ДЛЯ КОНВЕРТАЦИИ ВЕКТОРОВ ---
    // Яндекс карты не понимают .xml файлы, эта функция рисует их в растровую картинку
    private fun getBitmapFromVectorDrawable(context: Context, drawableId: Int): Bitmap? {
        val drawable = ContextCompat.getDrawable(context, drawableId) ?: return null
        val bitmap = Bitmap.createBitmap(
            drawable.intrinsicWidth,
            drawable.intrinsicHeight,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }

    // --- ЖИЗНЕННЫЙ ЦИКЛ ---
    override fun onStart() {
        super.onStart()
        MapKitFactory.getInstance().onStart()
        mapView.onStart()
    }

    override fun onStop() {
        mapView.onStop()
        MapKitFactory.getInstance().onStop()
        super.onStop()
    }
}