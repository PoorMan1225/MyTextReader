package com.rjhwork.mycompany.fileopen

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.ContentProvider
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.database.Cursor
import android.net.Uri
import android.os.*
import android.provider.OpenableColumns
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import android.view.*
import android.widget.SeekBar
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.viewpager2.widget.ViewPager2
import com.hbisoft.pickit.PickiT
import com.hbisoft.pickit.PickiTCallbacks
import com.rjhwork.mycompany.fileopen.adapter.TextPageAdapter
import com.rjhwork.mycompany.fileopen.databinding.ActivityMainBinding
import com.rjhwork.mycompany.fileopen.thread.RotatePageSearchTask
import com.rjhwork.mycompany.fileopen.thread.SearchTask
import com.rjhwork.mycompany.fileopen.thread.TextSplitThread
import com.rjhwork.mycompany.fileopen.thread.ThreadPoolManager
import com.rjhwork.mycompany.fileopen.util.PreferenceJsonUtil
import com.rjhwork.mycompany.fileopen.util.UniversalDetectorUtil
import com.rjhwork.mycompany.fileopen.viewmodel.TextViewModel
import java.io.*
import java.util.*

const val TAG = "MainActivity"

class MainActivity : AppCompatActivity(), PickiTCallbacks {
    private lateinit var binding: ActivityMainBinding
    private var data = mutableListOf<String>()
    private val searchIndexList = mutableListOf<Int>()
    private lateinit var threadPoolManager: ThreadPoolManager

    private lateinit var dialog: AlertDialog
    private lateinit var pickit: PickiT
    private lateinit var adapter: TextPageAdapter

    private val textViewModel: TextViewModel by lazy {
        ViewModelProvider(this).get(TextViewModel::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        init()
        getActivitySize()
        initDialog()
    }

    private fun saveDataUri() {
        val saveUri =
            PreferenceJsonUtil.getSavePreference(this, "uri", PreferenceJsonUtil.URI_SAVE, "") ?: return

        Log.d(TAG, "saveUri : $saveUri")
        val savePage =
            PreferenceJsonUtil.getSavePreference(this, "page", PreferenceJsonUtil.PAGE_SAVE)
        val landData =
            PreferenceJsonUtil.getSavePreference(this, "landscape", PreferenceJsonUtil.LAND_DATA, "")

        if (landData != null) {
            if(savePage != -1 && landData.isBlank()) {
                textViewModel.pagePosition = savePage  // portrait 경우
            }else {
                textViewModel.currentPageData = landData // landscape 의 경우
            }
        }
        val uri = Uri.parse(saveUri)
        textViewModel.contentUri = uri

        getFileName(uri)
        dialog.show()
        pickit.getPath(uri, Build.VERSION.SDK_INT)
    }

    private fun initSeekBar() {
        if (data.isNotEmpty()) {
            binding.seekBar.max = data.size - 1
        }
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
            .setCancelable(false)
            .show()
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
            if (searchIndexList.isEmpty()) {
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

        binding.seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                seekBar ?: return

                if (fromUser) {
                    binding.viewPager.currentItem = seekBar.progress
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                seekBar ?: return
                binding.viewPager.currentItem = seekBar.progress
            }
        })
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
            binding.seekBar.progress = position
            binding.pageTextView.text =
                getString(R.string.page_text, position.toString(), (data.size - 1).toString())
        }
    }

    private fun getActivitySize() {
        binding.root.viewTreeObserver.addOnGlobalLayoutListener(object :
            ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                binding.root.viewTreeObserver.removeOnGlobalLayoutListener(this)
                val orientation = resources.configuration.orientation
                // landscape 일때 데이터 저장.
                textViewModel.aWidth = binding.root.width
                textViewModel.aHeight = binding.root.height
                if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    textViewModel.maxLine -= 1
                }
                Log.d(TAG, "width : ${textViewModel.aWidth}")
                Log.d(TAG, "height : ${textViewModel.aHeight}")

                saveDataUri()
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
                    Log.d(TAG, "textViewModel currentPageData : ${textViewModel.currentPageData}")
                    if (textViewModel.currentPageData.isNotBlank()) {
                        rotatePageChange()
                    } else {
                        initSeekBar()
                        displayPager()
                        dialog.dismiss()
                    }
                }
            }
        }
    }

    private fun rotatePageChange() {
        val rotateTask = RotatePageSearchTask(textViewModel, data)
        val future = threadPoolManager.executorService.submit(rotateTask)

        try {
            future.get()
            initSeekBar()
            displayPager()
            dialog.dismiss()
        } catch (e: Exception) {
            Log.e(TAG, "future error : ${e.message}")
        }
    }

    private fun updateAdapter(type: String) {
        Log.d(TAG, "searchIndex : $searchIndexList")
        if (type == "search") {
            val pair = Pair(binding.edtSearch.text.length, searchIndexList)
            adapter.apply {
                keywordListener = { pair }
                notifyItemChanged(textViewModel.pagePosition, "search")
            }
            return
        }

        // 페이지를 변경 많이 하게 되면 내부적으로 notifyChange 가 호출 되는 것 같음
        // 그래서 페이지 를 변경할때마다 검색어를 표시하는 것이 안됨. 다른방법이 있는지는 모르겠으나.
        // 콜백이 안먹히는 상황이 발생함.
        binding.viewPager.currentItem = textViewModel.pagePosition
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

    private fun dataRefresh() {
        if (data.isNotEmpty()) {
            data.clear()
        }
        textViewModel.pagePosition = 0
        textViewModel.displayName = ""
        textViewModel.currentPageData = ""
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
            Log.d(TAG, "resultCode: ${result.resultCode}")
            if (result.resultCode == Activity.RESULT_OK) {
                if (result.data == null) {
                    Toast.makeText(this, "데이터가 없습니다.", Toast.LENGTH_SHORT).show()
                } else {
                    if (result.data!!.data == null) {
                        return@registerForActivityResult
                    } else {
                        // 제대로 uri 가 왔을 경우에 데이터 초기화.
                        dataRefresh()
                        val uri = result.data?.data!!
                        Log.d(TAG, "uri : $uri")
                        val contentUri = uri.path
                        Log.d(TAG, "contentUri : $contentUri")

                        contentResolver.takePersistableUriPermission(
                            uri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION
                        )
                        textViewModel.contentUri = uri
                        getFileName(uri)
                        dialog.show()
                        pickit.getPath(uri, Build.VERSION.SDK_INT)
                    }
                }
            }
        }

    // 파일 이름 얻기.
    private fun getFileName(uri: Uri) {
        val contentResolver = applicationContext.contentResolver

        val cursor: Cursor? = contentResolver.query(uri, null, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val displayName: String =
                    it.getString(it.getColumnIndex(OpenableColumns.DISPLAY_NAME))

                Log.d(TAG, "display Name : $displayName")
                textViewModel.displayName = displayName.substringBefore(".")
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
        textViewModel.contentUri ?: return

        val thread =
            TextSplitThread(
                textViewModel.contentUri!!,
                encoding,
                applicationContext,
                textViewModel,
                data,
                mHandler
            )
        thread.start()
    }

    private fun displayPager() {
        adapter = TextPageAdapter(this@MainActivity,
            data,
            searchViewVisibleListener = { setSearchVisible(it) })
        adapter.setHasStableIds(true)
        binding.viewPager.adapter = adapter
        binding.viewPager.setCurrentItem(textViewModel.pagePosition, false)
        binding.titleTextView.text = textViewModel.displayName
    }

    private fun setSearchVisible(it: Boolean) {
        if (binding.searchLayout.isVisible) {
            adapter.notifyDataSetChanged()
            binding.searchLayout.isVisible = it.not()
        } else {
            binding.searchLayout.isVisible = it
        }
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

            var realPath = path
            Log.d(TAG, "realPath : $realPath")
            val uriPath = textViewModel.contentUri?.path!!
            Log.d(TAG, "uriPath : $uriPath")
            if(uriPath.contains(textViewModel.displayName)) {
                realPath = uriPath.substringAfter(":")
            }

            val file = File(realPath)
            val encoding = UniversalDetectorUtil.findFileEncoding(file)
            if (encoding.isNotEmpty()) {
                Log.d(TAG, "encoding : $encoding")
                startThread(encoding)
            } else {
                Toast.makeText(this, "인코딩 실패!", Toast.LENGTH_SHORT).show()
                return
            }
        }
    }

    override fun onBackPressed() {
        AlertDialog.Builder(this)
            .setMessage("종료 하시겠습니까?")
            .setPositiveButton("확인") { _, _ ->
                PreferenceJsonUtil.putSavePreference(
                    this,
                    "uri",
                    textViewModel.contentUri.toString(),
                    PreferenceJsonUtil.URI_SAVE
                )
                binding.viewPager.unregisterOnPageChangeCallback(pageChangeListener)
                super.onBackPressed()
            }.setNegativeButton("취소") { _, _ -> }
            .setCancelable(false)
            .show()
    }

    override fun onStop() {
        if (data.isNotEmpty()) {
            textViewModel.currentPageData = data[textViewModel.pagePosition]
            textViewModel.dataSize = data.size - 1
            PreferenceJsonUtil.putSavePreference(
                this,
                "uri",
                textViewModel.contentUri.toString(),
                PreferenceJsonUtil.URI_SAVE
            )
            val orientation = resources.configuration.orientation
            // landscape 일때 데이터 저장.
            if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                landScapeExit()
            } else {
                // portrait 일때 데이터 저장.
                portraitExit()
            }
        }
        super.onStop()
    }

    private fun portraitExit() {
        PreferenceJsonUtil.putSavePreference(
            this,
            "page",
            textViewModel.pagePosition,
            PreferenceJsonUtil.PAGE_SAVE
        )
        PreferenceJsonUtil.putSavePreference(
            this,
            "landscape",
            "",
            PreferenceJsonUtil.LAND_DATA
        )
    }

    private fun landScapeExit() {
        PreferenceJsonUtil.putSavePreference(
            this,
            "landscape",
            data[textViewModel.pagePosition],
            PreferenceJsonUtil.LAND_DATA
        )
        PreferenceJsonUtil.putSavePreference(
            this,
            "page",
            -1,
            PreferenceJsonUtil.PAGE_SAVE
        )
    }

    override fun onDestroy() {
        if (!isChangingConfigurations) {
            pickit.deleteTemporaryFile(this)
        }
        binding.viewPager.unregisterOnPageChangeCallback(pageChangeListener)
        threadPoolManager.executorService.isShutdown
        super.onDestroy()
    }
}