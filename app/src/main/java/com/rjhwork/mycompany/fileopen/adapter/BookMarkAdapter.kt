package com.rjhwork.mycompany.fileopen.adapter

import android.content.Context
import android.text.format.DateFormat
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.rjhwork.mycompany.fileopen.R
import com.rjhwork.mycompany.fileopen.TAG
import com.rjhwork.mycompany.fileopen.databinding.BookMarkItemBinding
import com.rjhwork.mycompany.fileopen.model.Data
import java.lang.StringBuilder

class BookMarkAdapter(
    private val context: Context,
    private val bookMarkDataList: List<Data>,
    private val goToMainActivity:(uri:String) -> Unit,
    private val bookMarkDeleteListener: (Data) -> Unit
) : RecyclerView.Adapter<BookMarkAdapter.ViewHolder>(
) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = DataBindingUtil.inflate<BookMarkItemBinding>(
            LayoutInflater.from(context),
            R.layout.book_mark_item,
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(bookMarkDataList[position])
    }

    override fun getItemCount(): Int = bookMarkDataList.size

    inner class ViewHolder(private val binding: BookMarkItemBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(data:Data) {
            binding.bookMarkName.text = data.name

            val splitData = data.landData.split("\n")
            val list = splitData.filter { (it.isNotBlank() && it.isNotEmpty()) }
            val sb = StringBuilder().append(list[0]).append(list[1])

            binding.bookMarkText.text = sb.toString()
            binding.bookMarkDate.text = DateFormat.format("yyyy-MM-dd", data.date)

            binding.root.setOnClickListener {
                goToMainActivity.invoke(data.uri)
            }

            binding.deleteBookMark.setOnClickListener {
                bookMarkDeleteListener.invoke(data)
            }
        }
    }
}