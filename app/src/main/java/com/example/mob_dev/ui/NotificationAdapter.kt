package com.example.mob_dev.ui

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.mob_dev.R
import com.example.mob_dev.data.AppNotification

class NotificationAdapter(
    private var list: List<AppNotification>,
    private val onDeleteClick: (String, Int) -> Unit // Клик для удаления (ID, позиция в списке)
) : RecyclerView.Adapter<NotificationAdapter.NotifViewHolder>() {

    class NotifViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvTitle: TextView = view.findViewById(R.id.tvNotifTitle)
        val ivIcon: ImageView = view.findViewById(R.id.ivNotifIcon)
        val blockWarning: FrameLayout = view.findViewById(R.id.blockWarningIcon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotifViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_notification, parent, false)
        return NotifViewHolder(view)
    }

    override fun onBindViewHolder(holder: NotifViewHolder, position: Int) {
        val item = list[position]
        holder.tvTitle.text = item.title

        // ДИНАМИЧЕСКИ НАСТРАИВАЕМ ИКОНКИ И ЦВЕТА
        when (item.type) {
            "reminder" -> {
                holder.ivIcon.visibility = View.VISIBLE
                holder.blockWarning.visibility = View.GONE
                holder.ivIcon.setImageResource(android.graphics.drawable.Icon.createWithResource(holder.itemView.context, android.R.drawable.ic_popup_reminder).resId)
                holder.ivIcon.setColorFilter(Color.WHITE)
            }
            "warning" -> {
                // Для предупреждения прячем обычную иконку и показываем оранжевый восклицательный знак
                holder.ivIcon.visibility = View.GONE
                holder.blockWarning.visibility = View.VISIBLE
            }
            "promo" -> {
                holder.ivIcon.visibility = View.VISIBLE
                holder.blockWarning.visibility = View.GONE
                holder.ivIcon.setImageResource(android.graphics.drawable.Icon.createWithResource(holder.itemView.context, android.R.drawable.ic_menu_agenda).resId)
                // Зеленый цвет для подарка/промо
                holder.ivIcon.setColorFilter(ContextCompat.getColor(holder.itemView.context, R.color.accent_green))
            }
        }

        // Клик по всей карточке удаляет её (помечает прочитанной)
        holder.itemView.setOnClickListener {
            onDeleteClick(item.id, position)
        }
    }

    override fun getItemCount() = list.size

    fun updateData(newList: List<AppNotification>) {
        list = newList
        notifyDataSetChanged()
    }
}