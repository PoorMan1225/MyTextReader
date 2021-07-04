package com.rjhwork.mycompany.fileopen.adapter

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.rjhwork.mycompany.fileopen.R
import com.rjhwork.mycompany.fileopen.TAG
import com.rjhwork.mycompany.fileopen.databinding.ItemTextBinding

class TextPageAdapter(
    val context: Context,
    private val textData: MutableList<String>,
    private val searchViewVisibleListener: (Boolean) -> Unit,
    private val setTextSizeListener: () -> Float,
    private val setColorChangeListener: () -> Pair<Int, Int>,
    private val setLineSpacingChangeListener: () -> Float,
    private val setFontChangeListener: () -> Typeface
) :
    RecyclerView.Adapter<TextPageAdapter.ViewHolder>() {

    // 내가 값을 받을 때.(TextPageAdapter 가 값을 받을때)
    var keywordListener: (() -> Pair<Int, MutableList<Int>>)? = null

    inner class ViewHolder(private val binding: ItemTextBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(data: String) {

            val colorPair = setColorChangeListener.invoke()
            if(colorPair.first == 0 && colorPair.second == 0) {
                binding.itemBackground.setBackgroundColor(ContextCompat.getColor(context, R.color.color1))
                binding.textView.setTextColor(ContextCompat.getColor(context, R.color.color4))
            }else {
                binding.itemBackground.setBackgroundColor(ContextCompat.getColor(context, colorPair.first))
                binding.textView.setTextColor(ContextCompat.getColor(context, colorPair.second))
            }

            val size = setTextSizeListener.invoke()

            // 동적으로 text size 변경.
            val lineSpace = setLineSpacingChangeListener.invoke()

            // dp 를 px 로 변경
            binding.textView.setLineSpacing(lineSpace, 1.0f)

            // sp 를 px 로 변경
            binding.textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, size)

            // font 변경
            val typface = setFontChangeListener.invoke()
            binding.textView.typeface = typface
            binding.textView.text = data
            binding.root.setOnClickListener {
                searchViewVisibleListener(true)
            }
        }

        fun bind(spannable: SpannableStringBuilder) {
            binding.textView.text = spannable
            binding.root.setOnClickListener {
                searchViewVisibleListener(true)
            }
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
                        pair.second.forEachIndexed { i, _ ->
                            spannable.setSpan(
                                ForegroundColorSpan(Color.RED),
                                list[i],
                                list[i] + pair.first,
                                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                            )
                        }
                        holder.bind(spannable)
                    }
                }
            }
        }
    }

    override fun getItemId(position: Int): Long {
        return textData[position].hashCode().toLong()
    }

    override fun getItemCount() = textData.size
}