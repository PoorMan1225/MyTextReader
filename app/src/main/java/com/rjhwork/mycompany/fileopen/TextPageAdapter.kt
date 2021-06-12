package com.rjhwork.mycompany.fileopen

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.rjhwork.mycompany.fileopen.databinding.ItemTextBinding

class TextPageAdapter(private val context: Context, private val textData: MutableList<String>) :
    RecyclerView.Adapter<TextPageAdapter.ViewHolder>() {


    class ViewHolder(private val binding: ItemTextBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(data:String) {
            binding.textView.text = data
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = DataBindingUtil.inflate<ItemTextBinding>(
            LayoutInflater.from(context),
            R.layout.item_text,
            parent,
            false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(textData[position])
    }

    override fun getItemCount() = textData.size
}