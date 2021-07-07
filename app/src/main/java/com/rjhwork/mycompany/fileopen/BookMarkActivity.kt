package com.rjhwork.mycompany.fileopen

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import androidx.appcompat.app.AlertDialog
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.rjhwork.mycompany.fileopen.adapter.BookMarkAdapter
import com.rjhwork.mycompany.fileopen.databinding.ActivityBookMarkBinding
import com.rjhwork.mycompany.fileopen.model.Data
import com.rjhwork.mycompany.fileopen.thread.ThreadPoolManager
import com.rjhwork.mycompany.fileopen.viewmodel.BookMarkViewModel
import java.lang.Exception
import kotlin.concurrent.thread

class BookMarkActivity : AppCompatActivity() {

    private val bookMarkViewModel: BookMarkViewModel by lazy {
        ViewModelProvider(this).get(BookMarkViewModel::class.java)
    }

    private var adapter: BookMarkAdapter? = BookMarkAdapter(this, emptyList(), ::goToMainActivity, ::bookMarkDelete)
    private lateinit var binding: ActivityBookMarkBinding
    private lateinit var threadPoolManager: ThreadPoolManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        init()
    }

    private fun init() {
        binding = DataBindingUtil.setContentView(this, R.layout.activity_book_mark)
        setSupportActionBar(binding.bookMarkToolBar)
        title = "북마크"
        supportActionBar?.apply {
            this.setDisplayHomeAsUpEnabled(true)
        }

        threadPoolManager = ThreadPoolManager.getInstance()
        binding.bookMarkRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.bookMarkRecyclerView.adapter = adapter

        // 액티비티의 뷰 생명주기.
        bookMarkViewModel.dataList.observe(this@BookMarkActivity, { dataList ->
            updateUI(dataList)
        })
    }

    private fun updateUI(dataList: List<Data>?) {
        dataList?.let {
            adapter = BookMarkAdapter(this@BookMarkActivity, it, ::goToMainActivity, ::bookMarkDelete)
            binding.bookMarkRecyclerView.adapter = adapter
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun goToMainActivity(uri: String) {
        val intent = MainActivity.newIntent(this, uri)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
        finish()
    }

    private fun bookMarkDelete(data:Data) {
       AlertDialog.Builder(this)
            .setMessage("삭제 하시겠습니까?")
            .setPositiveButton("삭제") { _, _ ->
                deleteProcess(data)
            }.setNegativeButton("취소") { _, _ -> }
            .show()
    }

    private fun deleteProcess(data:Data) {
        val future = threadPoolManager.executorService.submit {
            bookMarkViewModel.deleteData(data)
        }

        try {
            future.get()
        } catch (e: Exception) {
            Log.e(TAG, "delete query Error : ${e.message}")
        }
    }
}
