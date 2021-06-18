package com.rjhwork.mycompany.fileopen

import android.content.Context
import android.graphics.Color
import android.text.Spannable
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.rjhwork.mycompany.fileopen.databinding.ItemTextBinding

class TextPageAdapter(val context: Context, private val textData: MutableList<String>) :
    RecyclerView.Adapter<TextPageAdapter.ViewHolder>() {

    var keywordListener: (() -> Pair<Int, MutableList<Int>>)? = null

    class ViewHolder(private val binding: ItemTextBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(data: String) {
            binding.textView.text = data
        }

        fun bind(spannable: SpannableStringBuilder) {
            binding.textView.text = spannable
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = DataBindingUtil.inflate<ItemTextBinding>(
            LayoutInflater.from(context),
            R.layout.item_text,
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(textData[position])
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.isEmpty()) {
            super.onBindViewHolder(holder, position, payloads)
        } else {
            payloads.forEach { payload ->
                if (payload is String) {
                    if (payload == "search") {
                        val data = textData[position]
                        val pair = keywordListener?.invoke()

                        pair ?: return
                        val list = pair.second

                        val spannable = SpannableStringBuilder(data)
                        spannable.setSpan(
                            ForegroundColorSpan(Color.RED),
                            list[0],
                            list[0] + pair.first,
                            Spannable.SPAN_EXCLUSIVE_INCLUSIVE
                        )
                        holder.bind(spannable)
                    }
                }
            }
        }
    }

    override fun getItemCount() = textData.size
}