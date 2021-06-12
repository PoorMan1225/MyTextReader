package com.rjhwork.mycompany.fileopen

import android.app.Activity
import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.Intent
import android.graphics.Point
import android.net.Uri
import android.os.AsyncTask
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.util.DisplayMetrics
import android.util.Log
import android.view.Display
import android.view.Menu
import android.view.MenuItem
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import android.widget.Toolbar
import androidx.activity.result.contract.ActivityResultContracts
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.viewpager2.widget.ViewPager2
import com.rjhwork.mycompany.fileopen.databinding.ActivityMainBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.lang.StringBuilder
import java.nio.Buffer

const val TAG = "MainActivity"

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private var data = mutableListOf<String>()

    private val textViewModel:TextViewModel by lazy {
        ViewModelProvider(this).get(TextViewModel::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)

        getDisplaySize()
    }

    private fun getDisplaySize() {
        val outMetrics = DisplayMetrics()
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            val display = this.display
            display?.getRealMetrics(outMetrics)
            calculateWidthHeight(display)
        } else {
            @Suppress("DEPRECATION")
            val display = this.windowManager.defaultDisplay
            @Suppress("DEPRECATION")
            display.getMetrics(outMetrics)
            calculateWidthHeight(display)
        }
    }

    private fun calculateWidthHeight(display: Display?) {
        display ?: return

        val textPix = display.width.div(12)
        val maxLine = display.height.div(12+20)

        textViewModel.textPX = textPix
        textViewModel.maxLine = maxLine
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.sample_action) {
            performFileSearch()
        }
        return true
    }

    // 암시적 인텐트로 콘텐츠 프로바이더에 있는 파일의 액션과
    // 타입을 지정한 뒤에 request 보낸다.
    fun performFileSearch() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "text/*"
        }
        requestFileDocument.launch(intent)
    }

    // 여기서는 요청된 데이터가 길이가 길기 때문에 라인을 백그라운드 에서
    // 파싱해서 작동하도록 한다.
    val requestFileDocument =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                if (result.data == null) {
                    Toast.makeText(this, "데이터가 없습니다.", Toast.LENGTH_SHORT).show()
                } else {
                    val uri = result.data!!.data
//                    binding.textView.text = showText(uri!!)
                    TextAsyncTask().execute(uri!!)
                }
            }
        }

    inner class TextAsyncTask : AsyncTask<Uri, Int, String>() {
        val sb = StringBuilder()
        var dialog: AlertDialog = AlertDialog.Builder(this@MainActivity)
            .setView(R.layout.progress)
            .setCancelable(false)
            .create()

        override fun onPreExecute() {
            super.onPreExecute()
            dialog.show()
        }

        override fun doInBackground(vararg uri: Uri): String {
            applicationContext.contentResolver.openInputStream(uri[0]).use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    var line: String? = reader.readLine()
                    while (line != null) {
                        sb.append(line)
                        line = reader.readLine()
                    }
                }
            }
            return sb.toString()
        }

        override fun onPostExecute(result: String?) {

            dialog.dismiss()
            initData(result)
        }
    }

    private fun initData(result: String?) {
        result ?: return

        var i = 0
        var j = textViewModel.textPX*textViewModel.maxLine

        while (true) {
            if(i + j >= result.length-1) {
                data.add(result.slice(IntRange(i, result.length-1)))
                break
            }

            val slice = result.slice(IntRange(i, j+i)) // abc
            data.add(slice)
            i += j+1
        }
        displayPager()
    }

    private fun displayPager() {
        val adapter = TextPageAdapter(this@MainActivity, data)
        binding.viewPager.adapter = adapter
    }
}