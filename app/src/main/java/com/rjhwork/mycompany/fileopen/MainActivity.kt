package com.rjhwork.mycompany.fileopen

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.*
import androidx.appcompat.app.AppCompatActivity
import android.view.*
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.hbisoft.pickit.PickiT
import com.hbisoft.pickit.PickiTCallbacks
import com.rjhwork.mycompany.fileopen.databinding.ActivityMainBinding
import java.io.*
import java.lang.StringBuilder
import java.util.*

private const val TAG = "MainActivity"
private const val MESSAGE_TEXT_TYPE = 1001

class MainActivity : AppCompatActivity(), PickiTCallbacks {
    private lateinit var binding: ActivityMainBinding
    private var data = mutableListOf<String>()
    private lateinit var dialog: AlertDialog
    private lateinit var uri: Uri
    private lateinit var pickit:PickiT

    private val textViewModel: TextViewModel by lazy {
        ViewModelProvider(this).get(TextViewModel::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        pickit = PickiT(this, this, this)
        getActivitySize()
        initDialog()
        firstRequestPermission()
    }

    // 최초 퍼미션 요청
    private fun firstRequestPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestMultiPermission.launch(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE))
        }
    }

    private fun showPermissionContextPopup() {
        AlertDialog.Builder(this)
            .setTitle("권한이 필요합니다.")
            .setMessage("파일에 접근하기 위한 권한이 필요합니다.")
            .setPositiveButton("동의하기") { _, _ ->
                requestMultiPermission.launch(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE))
            }.setNegativeButton("취소하기") { _, _ -> }
    }

    private fun getActivitySize() {
        binding.root.viewTreeObserver.addOnGlobalLayoutListener(object :
            ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                binding.root.viewTreeObserver.removeOnGlobalLayoutListener(this)
                val heightOffset = convertDPtoPixel(90)
                val widthOffset = convertDPtoPixel(15)

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
            textReadProcess()
        }
        return true
    }

    private fun textReadProcess() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            when {
                ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                        == PackageManager.PERMISSION_GRANTED -> performFileSearch()

                ContextCompat.checkSelfPermission(
                    this, Manifest.permission.READ_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_DENIED == shouldShowRequestPermissionRationale(
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) -> showPermissionContextPopup()
                else -> {
                    requestMultiPermission.launch(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE))
                }
            }
        }
    }

    // 암시적 인텐트로 콘텐츠 프로바이더에 있는 파일의 액션과
    // 타입을 지정한 뒤에 request 보낸다.
    private fun performFileSearch() {
        if (data.isNotEmpty()) {
            data.clear()
        }
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
                    if (result.data!!.data == null) {
                        return@registerForActivityResult
                    } else {
                        uri = result.data?.data!!
                        dialog.show()
                        pickit.getPath(uri, Build.VERSION.SDK_INT)
                    }
                }
            }
        }

    private val requestMultiPermission =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            permissions.entries.forEach {
                if(it.value.not()) {
                    Toast.makeText(this, "권한이 부여되지 않았습니다.", Toast.LENGTH_SHORT).show()
                    return@registerForActivityResult
                }
            }
            performFileSearch()
        }

    private fun startThread(encoding: String) {
        val thread = TextSplitThread(uri, encoding)
        thread.start()
    }

    inner class TextSplitThread(private val uri: Uri, private val encoding: String) : Thread() {

        override fun run() {
            Looper.prepare()
            dialog.show()

            val resultList = mutableListOf<String>()
            val saveList = mutableListOf<String>()
            val sb = StringBuilder()

            applicationContext.contentResolver.openInputStream(uri).use { inputStream ->

                BufferedReader(InputStreamReader(inputStream, encoding)).use { reader ->
                    var line: String? = reader.readLine()
                    var count: Int

                    while (line != null) {
                        addSaveListToResultList(saveList, resultList)
                        // text 문서의 한 라인을 화면의 넓이에 맞기 파싱한 라인 list 를
                        // 결과 리스트에 더한다.
                        addLineListToResultList(line, resultList)

                        count = resultList.size
                        // 결과 리스트의 사이즈가 화면 높이의 maxLine 보다 더 작은 경우
                        if (count < textViewModel.maxLine) {
                            line = reader.readLine()
                            continue
                        } else {
                            // 결과 리스트의 사이즈가 화면 높이의 maxLine 보다 더 큰 경우
                            // maxLine 에 맞게 잘라준다.
                            splitHeightLine(sb, resultList, count, saveList)
                        }
                        line = reader.readLine()
                    }
                }
            }
            val message = mHandler.obtainMessage(MESSAGE_TEXT_TYPE)
            mHandler.sendMessage(message)
        }


        private fun splitHeightLine(
            sb: StringBuilder,
            resultList: MutableList<String>,
            count: Int,
            saveList: MutableList<String>
        ) {
            var count1 = count
            var i = 0
            val j = textViewModel.maxLine - 1

            while (true) {
                (i..(j + i)).forEach {
                    sb.append(resultList[it])
                }
                data.add(sb.toString())
                sb.clear()

                count1 -= textViewModel.maxLine
                i += j + 1
                if (count1 < textViewModel.maxLine) {
                    break
                }
            }
            (i until resultList.size).forEach {
                saveList.add(resultList[it])
            }
            resultList.clear()
        }

        private fun addLineListToResultList(line: String, resultList: MutableList<String>) {
            val lineList = checkTextCountAddLineList(line)
            resultList += lineList
        }

        private fun addSaveListToResultList(saveList: MutableList<String>, resultList: MutableList<String>) {
            if (saveList.size > 0) {
                resultList += saveList
                saveList.clear()
            }
        }
    }

    // 문자를 한글자 씩 확인해서 width 에 맞게 textCount 센후에 해당 문자 한줄씩
    // lineList 에 넣어주는 함수
    private fun checkTextCountAddLineList(line: String): MutableList<String> {
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
                if (i == line.length - 1) {
                    sb.append(c).append("\n")
                    lineList.add(sb.toString())
                    return@forEachIndexed
                }
                sb.append(c)
            }

            if (count == textViewModel.textCount) {
                count = 0
                sb.append("\n")
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

    override fun PickiTonUriReturned() {}

    override fun PickiTonStartListener() {}

    override fun PickiTonProgressUpdate(progress: Int) {}

    override fun PickiTonCompleteListener(
        path: String?,
        wasDriveFile: Boolean,
        wasUnknownProvider: Boolean,
        wasSuccessful: Boolean,
        Reason: String?
    ) {
        if(wasSuccessful) {
            val file = File(path)
            val encoding = UniversalDetectorUtil.findFileEncoding(file)
            if(encoding.isNotEmpty()) {
                startThread(encoding)
            }
        }
    }

    override fun onBackPressed() {
        pickit.deleteTemporaryFile(this)
        super.onBackPressed()
    }

    override fun onDestroy() {
        super.onDestroy()
        if(!isChangingConfigurations) {
            pickit.deleteTemporaryFile(this)
        }
    }

}