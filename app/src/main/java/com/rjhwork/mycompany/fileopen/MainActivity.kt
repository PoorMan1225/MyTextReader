package com.rjhwork.mycompany.fileopen

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.*
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import android.view.*
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.viewpager2.widget.ViewPager2
import com.hbisoft.pickit.PickiT
import com.hbisoft.pickit.PickiTCallbacks
import com.rjhwork.mycompany.fileopen.databinding.ActivityMainBinding
import java.io.*
import java.lang.StringBuilder
import java.util.*

private const val TAG = "MainActivity"

class MainActivity : AppCompatActivity(), PickiTCallbacks {
    private lateinit var binding: ActivityMainBinding
    private var data = Collections.synchronizedList(mutableListOf<String>())
    private val searchIndexList = mutableListOf<Int>()
    private lateinit var threadPoolManager: ThreadPoolManager

    private lateinit var dialog: AlertDialog
    private lateinit var uri: Uri
    private lateinit var pickit: PickiT

    private var adapter: TextPageAdapter? = null

    private val textViewModel: TextViewModel by lazy {
        ViewModelProvider(this).get(TextViewModel::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        init()
        getActivitySize()
        initDialog()
    }

    private fun init() {
        threadPoolManager = ThreadPoolManager.getInstance()
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        pickit = PickiT(this, this, this)
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)
    }

    // 최초 퍼미션 요청
    private fun requestPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermission.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }

    // 권한 팝업.
    private fun showPermissionContextPopup() {
        AlertDialog.Builder(this)
            .setTitle("권한이 필요합니다.")
            .setMessage("파일에 접근하기 위한 권한이 필요합니다.")
            .setPositiveButton("동의하기") { _, _ ->
                requestPermission()
            }.setNegativeButton("취소하기") { _, _ -> }
    }

    private fun startSearchThread(length: Int, searchString: String, type: String) {
        val searchTask = SearchTask(textViewModel, data, searchIndexList)
        searchTask.apply {
            this.type = type
            this.searchLength = length
            this.searchString = searchString
        }
        val future = threadPoolManager.executorService.submit(searchTask)

        try {
            future.get()
            if(searchIndexList.isEmpty()) {
                Toast.makeText(this, "찾는 값이 없습니다. ", Toast.LENGTH_SHORT).show()
                return
            }
            updateAdapter(type)
        } catch (e: Exception) {
            Log.e(TAG, "future error : ${e.message}")
        }
    }

    override fun onStart() {
        super.onStart()
        binding.search.setOnClickListener {
            startSearching("search")
        }

        binding.forward.setOnClickListener {
            startSearching("forward")
        }

        binding.back.setOnClickListener {
            startSearching("back")
        }
        binding.viewPager.registerOnPageChangeCallback(pageChangeListener)
    }

    private fun startSearching(key: String) {
        if (binding.edtSearch.text.isNotEmpty()) {
            val searchText = binding.edtSearch.text
            startSearchThread(searchText.length, searchText.toString(), key)
        } else {
            Toast.makeText(this, "검색어를 입력해주세요.", Toast.LENGTH_SHORT).show()
        }
    }

    private val pageChangeListener = object : ViewPager2.OnPageChangeCallback() {
        override fun onPageSelected(position: Int) {
            textViewModel.pagePosition = position
        }
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
            when (msg.what) {
                TextSplitThread.MESSAGE_TEXT_TYPE -> {
                    displayPager()
                    Log.d(TAG, "data size: ${data.size}")
                    dialog.dismiss()
                }
            }
        }
    }

    private fun updateAdapter(type: String) {
        Log.d(TAG, "searchIndex : $searchIndexList")
        if(type == "search") {
            val pair = Pair(binding.edtSearch.text.length, searchIndexList)
            adapter?.apply {
                keywordListener = { pair }
                notifyItemChanged(textViewModel.pagePosition, "search")
            }
            return
        }

        val offset = if (type == "forward") 1 else -1

        binding.viewPager.currentItem = textViewModel.pagePosition - offset
        if (binding.viewPager.currentItem == textViewModel.pagePosition - offset) {
            val pair = Pair(binding.edtSearch.text.length, searchIndexList)
            adapter?.apply {
                keywordListener = { pair }
                notifyItemChanged(textViewModel.pagePosition - offset, "search")
                textViewModel.pagePosition += offset
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
                    requestPermission()
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

    private val requestPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { result ->
            if (result) {
                performFileSearch()
            } else {
                Toast.makeText(this, "권한이 거부 되었습니다.", Toast.LENGTH_SHORT).show()
            }
        }

    private fun startThread(encoding: String) {
        val thread =
            TextSplitThread(uri, encoding, applicationContext, textViewModel, data, mHandler)
        dialog.show()
        thread.start()
    }

    private fun displayPager() {
        adapter = TextPageAdapter(this@MainActivity, data)
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
        if (wasSuccessful) {
            path ?: return

            val file = File(path)
            val encoding = UniversalDetectorUtil.findFileEncoding(file)
            if (encoding.isNotEmpty()) {
                startThread(encoding)
            } else {
                Toast.makeText(this, "인코딩 실패!", Toast.LENGTH_SHORT).show()
                return
            }
        }
    }

    override fun onBackPressed() {
        pickit.deleteTemporaryFile(this)
        super.onBackPressed()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (!isChangingConfigurations) {
            pickit.deleteTemporaryFile(this)
        }
        threadPoolManager.executorService.isShutdown
    }
}