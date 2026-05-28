package com.example.mob_dev.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.ScrollView
import android.widget.ProgressBar
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.mob_dev.R
import com.example.mob_dev.data.TrackingRepository
import com.example.mob_dev.ui.MainActivity
import com.example.mob_dev.utils.NetworkUtils
import kotlinx.coroutines.launch
import java.util.Locale
import kotlinx.coroutines.Job

class TrackingEditFragment : Fragment() {

    private val trackingRepo = TrackingRepository()
    private var currentHeight: Int = 175
    private var currentWeight: Float = 60.0f
    // для контоля коррутин
    private var loadJob: Job? = null
    private var saveJob: Job? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_tracking_edit, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val progressBar = view.findViewById<ProgressBar>(R.id.progressBarTrackingEdit)
        val mainContent = view.findViewById<ScrollView>(R.id.mainTrackingEditContent)

        val tvHeight = view.findViewById<TextView>(R.id.tvEditHeight)
        val tvWeight = view.findViewById<TextView>(R.id.tvEditWeight)

        val btnPlusHeight = view.findViewById<TextView>(R.id.btnPlusHeight)
        val btnMinusHeight = view.findViewById<TextView>(R.id.btnMinusHeight)
        val btnPlusWeight = view.findViewById<TextView>(R.id.btnPlusWeight)
        val btnMinusWeight = view.findViewById<TextView>(R.id.btnMinusWeight)

        val btnSave = view.findViewById<Button>(R.id.btnSaveTracking)
        val btnCancel = view.findViewById<Button>(R.id.btnCancelTracking)

        progressBar.visibility = View.VISIBLE
        mainContent.visibility = View.GONE

        // загрузка данных
        loadJob = lifecycleScope.launch {
            val history = trackingRepo.getWeightHistory(1)
            if (history.isNotEmpty()) {
                currentWeight = history.last().weight
            }
            currentHeight = trackingRepo.getCurrentHeight()
            updateDisplay(tvHeight, tvWeight)

            progressBar.visibility = View.GONE
            mainContent.visibility = View.VISIBLE
        }

        btnPlusHeight.setOnClickListener {
            if (currentHeight < 250) currentHeight += 1
            updateDisplay(tvHeight, tvWeight)
        }
        btnMinusHeight.setOnClickListener {
            if (currentHeight > 100) currentHeight -= 1
            updateDisplay(tvHeight, tvWeight)
        }


        btnPlusWeight.setOnClickListener {
            if (currentWeight < 300.0f) currentWeight += 0.5f
            updateDisplay(tvHeight, tvWeight)
        }
        btnMinusWeight.setOnClickListener {
            if (currentWeight > 30.0f) currentWeight -= 0.5f
            updateDisplay(tvHeight, tvWeight)
        }

        btnSave.setOnClickListener {
            if (!NetworkUtils.isInternetAvailable(requireContext())) {
                Toast.makeText(requireContext(), "Нет интернета", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            saveJob?.cancel()

            btnSave.isEnabled = false
            btnSave.text = "Сохранение..."

            saveJob = lifecycleScope.launch {
                val isSuccess = trackingRepo.saveTrackingData(currentWeight, currentHeight)

                if (isSuccess) {
                    Toast.makeText(requireContext(), "Данные сохранены", Toast.LENGTH_SHORT).show()
                    requireActivity().supportFragmentManager.popBackStack()
                } else {
                    Toast.makeText(requireContext(), "Ошибка при сохранении", Toast.LENGTH_SHORT).show()
                    btnSave.isEnabled = true
                    btnSave.text = "Сохранить изменения"
                }
            }
        }

        btnCancel.setOnClickListener {
            (requireActivity() as MainActivity).loadFragment(TrackingFragment(), true)
        }
    }

    override fun onDestroyView() {
        loadJob?.cancel()
        saveJob?.cancel()
        super.onDestroyView()
    }

    private fun updateDisplay(tvHeight: TextView, tvWeight: TextView) {
        tvHeight.text = "$currentHeight см"
        // Жестко задаем Locale.US, чтобы была ТОЧКА!
        tvWeight.text = String.format(Locale.US, "%.1f кг", currentWeight)
    }


}