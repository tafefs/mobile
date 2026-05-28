package com.example.mob_dev.ui.fragments

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.mob_dev.R
import com.example.mob_dev.data.TrackingRepository
import com.example.mob_dev.ui.MainActivity
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

class TrackingFragment : Fragment() {

    private val trackingRepo = TrackingRepository()
    private lateinit var lineChart: LineChart
    private lateinit var tabWeek: TextView
    private lateinit var tabMonth: TextView
    private lateinit var tabYear: TextView
    private lateinit var tvWeightValue: TextView
    private lateinit var tvHeightValue: TextView

    private lateinit var progressBar: ProgressBar
    private lateinit var mainContent: ScrollView

    private var currentDaysPeriod = 7
    private var loadJob: Job? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_tracking, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        progressBar = view.findViewById(R.id.progressBarTracking)
        mainContent = view.findViewById(R.id.mainTrackingContent)

        lineChart = view.findViewById(R.id.lineChart)
        tvWeightValue = view.findViewById(R.id.tvWeightValue)
        tvHeightValue = view.findViewById(R.id.tvHeightValue)

        val btnEdit = view.findViewById<Button>(R.id.btnGoToTrackingEdit)

        tabWeek = view.findViewById(R.id.tabWeek)
        tabMonth = view.findViewById(R.id.tabMonth)
        tabYear = view.findViewById(R.id.tabYear)

        lineChart.setNoDataText("")

        lineChart.setOnChartValueSelectedListener(object : OnChartValueSelectedListener {
            override fun onValueSelected(e: Entry?, h: Highlight?) {
                if (e != null) {
                    Toast.makeText(requireContext(), "Вес: ${String.format(Locale.US, "%.1f", e.y)} кг", Toast.LENGTH_SHORT).show()
                }
            }
            override fun onNothingSelected() {}
        })

        // ИСПРАВЛЕНИЕ: updateTabUI вызывается МГНОВЕННО при клике, до запроса в сеть!
        tabWeek.setOnClickListener {
            currentDaysPeriod = 7
            updateTabUI(tabWeek)
            loadChartData(showFullScreenLoading = false)
        }
        tabMonth.setOnClickListener {
            currentDaysPeriod = 30
            updateTabUI(tabMonth)
            loadChartData(showFullScreenLoading = false)
        }
        tabYear.setOnClickListener {
            currentDaysPeriod = 365
            updateTabUI(tabYear)
            loadChartData(showFullScreenLoading = false)
        }

        btnEdit.setOnClickListener {
            (requireActivity() as MainActivity).loadFragment(TrackingEditFragment(), true)
        }

        updateTabUI(tabWeek)
    }

    override fun onResume() {
        super.onResume()
        loadChartData(showFullScreenLoading = true)
    }

    private fun loadChartData(showFullScreenLoading: Boolean) {
        loadJob?.cancel() // Отменяем старый запрос

        if (showFullScreenLoading) {
            progressBar.visibility = View.VISIBLE
            mainContent.visibility = View.GONE
        }

        loadJob = lifecycleScope.launch {
            try {
                val historyDeferred = async { trackingRepo.getWeightHistory(currentDaysPeriod) }
                val heightDeferred = async { trackingRepo.getCurrentHeight() }

                val history = historyDeferred.await()
                val currentHeight = heightDeferred.await()

                tvHeightValue.text = "$currentHeight см"
                if (history.isNotEmpty()) {
                    tvWeightValue.text = String.format(Locale.US, "%.1f кг", history.last().weight)
                } else {
                    tvWeightValue.text = "Нет данных"
                }

                val entries = ArrayList<Entry>()
                val datesList = ArrayList<String>()

                if (history.isEmpty()) {
                    entries.add(Entry(0f, 0f))
                    datesList.add("")
                } else {
                    history.forEachIndexed { index, item ->
                        entries.add(Entry(index.toFloat(), item.weight))
                        datesList.add(formatDate(item.recorded_at))
                    }
                }

                lineChart.clear()
                setupChart(entries, datesList)

            } catch (e: Exception) {
                // ИСПРАВЛЕНИЕ: Игнорируем ошибку отмены корутины в логах
                if (e is kotlinx.coroutines.CancellationException) throw e

                android.util.Log.e("TrackingError", "Ошибка при обновлении графика: ${e.message}")
            } finally {
                if (showFullScreenLoading) {
                    progressBar.visibility = View.GONE
                    mainContent.visibility = View.VISIBLE
                    mainContent.alpha = 0f
                    mainContent.animate().alpha(1f).setDuration(300).start()
                }
            }
        }
    }

    private fun formatDate(dateStr: String): String {
        return try {
            val inputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
            val outputFormatter = DateTimeFormatter.ofPattern("dd.MM")
            val date = LocalDate.parse(dateStr, inputFormatter)
            date.format(outputFormatter)
        } catch (e: Exception) {
            dateStr
        }
    }

    private fun setupChart(entries: ArrayList<Entry>, dates: List<String>) {
        val dataSet = LineDataSet(entries, "Изменение веса")
        dataSet.color = Color.parseColor("#2196F3")
        dataSet.valueTextColor = Color.TRANSPARENT
        dataSet.lineWidth = 3f
        dataSet.setCircleColor(Color.parseColor("#2196F3"))
        dataSet.circleRadius = 4f
        dataSet.setDrawCircleHole(false)
        dataSet.mode = LineDataSet.Mode.CUBIC_BEZIER
        dataSet.highLightColor = Color.parseColor("#66FF89")
        dataSet.highlightLineWidth = 1f

        val lineData = LineData(dataSet)
        lineChart.data = lineData

        lineChart.description.isEnabled = false
        lineChart.legend.isEnabled = false
        lineChart.setGridBackgroundColor(Color.TRANSPARENT)
        lineChart.setDrawGridBackground(false)

        val xAxis = lineChart.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.textColor = Color.WHITE
        xAxis.setDrawGridLines(true)
        xAxis.gridColor = Color.parseColor("#48484A")
        xAxis.granularity = 1f
        xAxis.valueFormatter = IndexAxisValueFormatter(dates)

        val yAxisLeft = lineChart.axisLeft
        yAxisLeft.textColor = Color.WHITE
        yAxisLeft.setDrawGridLines(true)
        yAxisLeft.gridColor = Color.parseColor("#48484A")
        lineChart.axisRight.isEnabled = false

        lineChart.invalidate()
        lineChart.animateX(500)
    }

    private fun updateTabUI(activeTab: TextView) {
        val allTabs = listOf(tabWeek, tabMonth, tabYear)
        for (tab in allTabs) {
            tab.setBackgroundColor(Color.TRANSPARENT)
            tab.text = tab.text.toString().replace("✓ ", "")
        }
        activeTab.setBackgroundResource(R.drawable.bg_tracking_tab_active)
        activeTab.text = "✓ ${activeTab.text}"
    }

    override fun onDestroyView() {
        loadJob?.cancel()
        super.onDestroyView()
    }
}