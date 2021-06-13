package com.rjhwork.mycompany.fileopen

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.*
import androidx.appcompat.app.AppCompatActivity
import android.view.*
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.rjhwork.mycompany.fileopen.databinding.ActivityMainBinding
import java.io.BufferedReader
import java.io.InputStreamReader
import java.lang.StringBuilder
import java.util.*

private const val TAG = "MainActivity"
private const val MESSAGE_TEXT_TYPE = 1001

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private var data = Collections.synchronizedList(mutableListOf<String>())
    private lateinit var dialog: AlertDialog

    private val textViewModel: TextViewModel by lazy {
        ViewModelProvider(this).get(TextViewModel::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)

        getActivitySize()
        initDialog()
    }

    private fun getActivitySize() {
        binding.root.viewTreeObserver.addOnGlobalLayoutListener(object :
            ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                binding.root.viewTreeObserver.removeOnGlobalLayoutListener(this)
                val heightOffset = convertDPtoPixel(90)
                val widthOffset = convertDPtoPixel(18)

                textViewModel.aWidth = binding.root.width - widthOffset
                textViewModel.aHeight = binding.root.height - heightOffset
            }
        })
    }

    private fun convertDPtoPixel(dp: Int): Int {
        val scale = this.resources.displayMetrics.density
        return (dp * scale + 0.5f).toInt()
    }

    private fun initDialog() {
        dialog = AlertDialog.Builder(this@MainActivity)
            .setView(R.layout.progress)
            .setCancelable(false)
            .create()
    }

    private val mHandler = object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            if (msg.what == MESSAGE_TEXT_TYPE) {
                displayPager()
                dialog.dismiss()
            }
        }
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
    private fun performFileSearch() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "text/*"
        }
        requestFileDocument.launch(intent)
    }

    // 여기서는 요청된 데이터가 길이가 길기 때문에 라인을 백그라운드 에서
    // 파싱해서 작동하도록 한다.
    private val requestFileDocument =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                if (result.data == null) {
                    Toast.makeText(this, "데이터가 없습니다.", Toast.LENGTH_SHORT).show()
                } else {
                    val uri = result.data!!.data
                    dialog.show()

                    val thread = TextSplitThread()
                    thread.apply {
                        this.uri = uri!!
                        start()
                    }
                }
            }
        }

    inner class TextSplitThread : Thread() {
        lateinit var uri: Uri

        override fun run() {
            Looper.prepare()
            dialog.show()

            val resultList = mutableListOf<String>()
            val saveList = mutableListOf<String>()
            val sb = StringBuilder()

            applicationContext.contentResolver.openInputStream(uri).use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    var line: String? = reader.readLine()
                    var count: Int

                    while (line != null) {
                        if (saveList.size > 0) {
                            resultList += saveList
                            saveList.clear()
                        }
                        val lineList = checkAddLine(line)
                        resultList += lineList

                        count = resultList.size
                        if (count < textViewModel.maxLine) {
                            line = reader.readLine()
                            continue
                        } else {
                            var i = 0
                            val j = textViewModel.maxLine - 1

                            while (true) {
                                (i..(j + i)).forEach {
                                    sb.append(resultList[it])
                                }
                                data.add(sb.toString())
                                sb.clear()

                                count -= textViewModel.maxLine
                                i += j + 1
                                if (count < textViewModel.maxLine) {
                                    break
                                }
                            }
                            (i until resultList.size).forEach {
                                saveList.add(resultList[it])
                            }
                            resultList.clear()
                        }
                        line = reader.readLine()
                    }
                }
            }
            val message = mHandler.obtainMessage(MESSAGE_TEXT_TYPE)
            mHandler.sendMessage(message)
        }
    }

    private fun checkAddLine(line: String): MutableList<String> {
        val sb = StringBuilder()
        val lineList = mutableListOf<String>()
        var count = 0

        line.forEachIndexed { i, c ->
            count++
            if (c == '\n') {
                sb.append(c)
                lineList.add(sb.toString())
                count = 0
                sb.clear()
            } else {
                if(i == line.length-1) {
                    sb.append(c).append("\n")
                    lineList.add(sb.toString())
                    return@forEachIndexed
                }
                sb.append(c)
            }

            if (count == textViewModel.textCount) {
                count = 0
                lineList.add(sb.toString())
                sb.clear()
            }
        }
        return lineList
    }

    private fun displayPager() {
        val adapter = TextPageAdapter(this@MainActivity, data)
        binding.viewPager.adapter = adapter
    }
}