package com.qrtasima.ui.profiles.detail

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.graphics.toColorInt
import androidx.recyclerview.widget.RecyclerView
import com.qrtasima.R

class ColorPickerAdapter(
    private val colors: List<String>,
    private val onColorSelected: (String) -> Unit
) : RecyclerView.Adapter<ColorPickerAdapter.ColorViewHolder>() {

    inner class ColorViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val colorView: View = itemView.findViewById(R.id.color_view)
        fun bind(colorHex: String) {
            try {
                colorView.setBackgroundColor(colorHex.toColorInt())
            } catch (e: IllegalArgumentException) {
                colorView.setBackgroundColor(Color.GRAY)
            }
            itemView.setOnClickListener {
                onColorSelected(colorHex)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ColorViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_color_choice, parent, false)
        return ColorViewHolder(view)
    }

    override fun getItemCount(): Int {
        return colors.size
    }

    override fun onBindViewHolder(holder: ColorViewHolder, position: Int) {
        holder.bind(colors[position])
    }
}