package com.example.mob_dev.ui

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.mob_dev.R
import com.example.mob_dev.data.Workout

class WorkoutAdapter(
    private var workouts: List<Workout>,
    private var bookedIds: List<String>, // на которые записаны
    private val onBookClick: (String, Boolean) -> Unit // записан ли
) : RecyclerView.Adapter<WorkoutAdapter.WorkoutViewHolder>() {

    class WorkoutViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvTitle: TextView = view.findViewById(R.id.tvWorkoutTitle)
        val tvTime: TextView = view.findViewById(R.id.tvWorkoutTime)
        val btnBook: Button = view.findViewById(R.id.btnBookWorkout)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WorkoutViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_workout, parent, false)
        return WorkoutViewHolder(view)
    }

    override fun onBindViewHolder(holder: WorkoutViewHolder, position: Int) {
        val workout = workouts[position]
        val isBooked = bookedIds.contains(workout.id)

        holder.tvTitle.text = workout.title
        holder.tvTime.text = "${workout.time} • ${workout.type}"

        if (!workout.has_spots && !isBooked) {
            holder.btnBook.text = "Нет мест"
            holder.btnBook.setBackgroundResource(R.drawable.bg_btn_orange)
            holder.btnBook.isEnabled = false
        } else if (isBooked) {
            holder.btnBook.text = "Отменить"
            holder.btnBook.setBackgroundResource(R.drawable.bg_btn_gray)
            holder.btnBook.isEnabled = true
        } else {
            holder.btnBook.text = "Записаться"
            holder.btnBook.setBackgroundResource(R.drawable.bg_btn_blue)
            holder.btnBook.isEnabled = true
        }

        holder.btnBook.setOnClickListener {
            onBookClick(workout.id, isBooked)
        }
    }

    override fun getItemCount() = workouts.size


    fun updateData(newWorkouts: List<Workout>, newBookedIds: List<String>) {
        workouts = newWorkouts
        bookedIds = newBookedIds
        notifyDataSetChanged()
    }
}