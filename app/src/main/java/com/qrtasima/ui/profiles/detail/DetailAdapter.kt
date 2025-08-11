package com.qrtasima.ui.profiles.detail

import android.annotation.SuppressLint
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.graphics.toColorInt
import androidx.core.widget.ImageViewCompat
import androidx.recyclerview.widget.RecyclerView
import com.qrtasima.data.PackageContent
import com.qrtasima.databinding.ItemDetailButtonBinding
import com.qrtasima.databinding.ItemDetailCustomBinding


class DetailAdapter(
    private var contentList: MutableList<PackageContent>,
    private val onContentChanged: (position: Int, newContent: PackageContent) -> Unit,
    private val onDeleteClicked: (PackageContent) -> Unit,
    private val onColorPickerClicked: (item: PackageContent, position: Int) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val VIEW_TYPE_BUTTON = 1
        private const val VIEW_TYPE_CUSTOM = 2
        private const val VIEW_TYPE_NAME = 3
    }

    @SuppressLint("NotifyDataSetChanged")
    fun updateData(newContentList: List<PackageContent>) {
        this.contentList.clear()
        this.contentList.addAll(newContentList)
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        val itemType = contentList[position].type
        return when {
            itemType.equals("İSİM", ignoreCase = true) -> VIEW_TYPE_NAME
            itemType.equals("CUSTOM_TEXT", ignoreCase = true) -> VIEW_TYPE_CUSTOM
            else -> VIEW_TYPE_BUTTON
        }
    }

    override fun getItemCount(): Int = contentList.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEW_TYPE_NAME, VIEW_TYPE_BUTTON -> {
                val binding = ItemDetailButtonBinding.inflate(inflater, parent, false)
                ButtonViewHolder(binding)
            }
            VIEW_TYPE_CUSTOM -> {
                val binding = ItemDetailCustomBinding.inflate(inflater, parent, false)
                CustomViewHolder(binding)
            }
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = contentList[position]
        when (holder) {
            is ButtonViewHolder -> holder.bind(item, getItemViewType(position))
            is CustomViewHolder -> holder.bind(item)
        }
    }

    inner class ButtonViewHolder(
        private val binding: ItemDetailButtonBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        private val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    val item = contentList[adapterPosition]
                    val updatedItem = item.copy(value = s.toString())
                    onContentChanged(adapterPosition, updatedItem)
                }
            }
        }

        fun bind(item: PackageContent, viewType: Int) {
            binding.editButtonTitle.removeTextChangedListener(textWatcher)
            binding.labelButtonTitle.text = item.label
            binding.editButtonTitle.setText(item.value)
            binding.editButtonTitle.addTextChangedListener(textWatcher)
            binding.buttonDeleteButton.setOnClickListener { onDeleteClicked(item) }

            if (viewType == VIEW_TYPE_NAME) {
                binding.buttonPickColor.visibility = View.GONE
                // Name field's color is directly from PackageContent customColor for name item itself
                item.customColor?.let { colorString ->
                    try {
                        binding.editButtonTitle.setTextColor(colorString.toColorInt())
                    } catch (e: Exception) {
                        // Fallback to default if color is invalid
                        binding.editButtonTitle.setTextColor(binding.labelButtonTitle.textColors)
                    }
                } ?: run {
                    binding.editButtonTitle.setTextColor(binding.labelButtonTitle.textColors)
                }
            } else {
                binding.buttonPickColor.visibility = View.VISIBLE
                binding.buttonPickColor.setOnClickListener {
                    onColorPickerClicked(item, adapterPosition)
                }
                item.customColor?.let {
                    try {
                        ImageViewCompat.setImageTintList(binding.buttonPickColor, android.content.res.ColorStateList.valueOf(it.toColorInt()))
                    } catch (e: Exception) {
                        ImageViewCompat.setImageTintList(binding.buttonPickColor, null)
                    }
                } ?: run {
                    ImageViewCompat.setImageTintList(binding.buttonPickColor, null)
                }
            }
        }
    }

    inner class CustomViewHolder(
        private val binding: ItemDetailCustomBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        private val titleTextWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    val item = contentList[adapterPosition]
                    val updatedItem = item.copy(label = s.toString())
                    onContentChanged(adapterPosition, updatedItem)
                }
            }
        }

        private val valueTextWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    val item = contentList[adapterPosition]
                    val updatedItem = item.copy(value = s.toString())
                    onContentChanged(adapterPosition, updatedItem)
                }
            }
        }

        fun bind(item: PackageContent) {
            binding.editCustomTitle.removeTextChangedListener(titleTextWatcher)
            binding.editCustomValue.removeTextChangedListener(valueTextWatcher)
            binding.editCustomTitle.setText(item.label)
            binding.editCustomValue.setText(item.value)
            binding.editCustomTitle.addTextChangedListener(titleTextWatcher)
            binding.editCustomValue.addTextChangedListener(valueTextWatcher)
            binding.buttonDeleteCustom.setOnClickListener {
                onDeleteClicked(item)
            }
        }
    }
}