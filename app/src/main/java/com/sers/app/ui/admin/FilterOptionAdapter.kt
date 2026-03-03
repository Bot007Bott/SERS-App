package com.sers.app.ui.admin

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.sers.app.databinding.ItemFilterOptionBinding

class FilterOptionAdapter(
    private var options: MutableList<String>,
    private var currentSelected: String,
    private val onSelect: (String) -> Unit
) : RecyclerView.Adapter<FilterOptionAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemFilterOptionBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemFilterOptionBinding.inflate(
            LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val option = options[position]
        holder.binding.tvOption.text = option
        holder.binding.ivCheck.visibility =
            if (option == currentSelected) android.view.View.VISIBLE
            else android.view.View.GONE
        holder.binding.root.setOnClickListener {
            currentSelected = option
            onSelect(option)
            notifyDataSetChanged()
        }
    }

    override fun getItemCount() = options.size

    fun updateList(newList: MutableList<String>) {
        options = newList
        notifyDataSetChanged()
    }
}