package com.rjhwork.mycompany.fileopen

import android.Manifest
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.database.Cursor
import android.graphics.Typeface
import android.net.Uri
import android.os.*
import android.provider.OpenableColumns
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import android.view.*
import android.widget.PopupMenu
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat.getFont
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.viewpager2.widget.ViewPager2
import com.hbisoft.pickit.PickiT
import com.hbisoft.pickit.PickiTCallbacks
import com.rjhwork.mycompany.fileopen.adapter.TextPageAdapter
import com.rjhwork.mycompany.fileopen.databinding.ActivityMainBinding
import com.rjhwork.mycompany.fileopen.model.Data
import com.rjhwork.mycompany.fileopen.model.SaveData
import com.rjhwork.mycompany.fileopen.thread.*
import com.rjhwork.mycompany.fileopen.util.DisplayUtil
import com.rjhwork.mycompany.fileopen.util.PreferenceJsonUtil
import com.rjhwork.mycompany.fileopen.util.UniversalDetectorUtil
import com.rjhwork.mycompany.fileopen.viewmodel.TextViewModel
import java.io.*
import java.util.*
import java.util.concurrent.Callable

const val TAG = "MainActivity"
const val DATA = "saveData"

class MainActivity : AppCompatActivity(), PickiTCallbacks {
    private lateinit var binding: ActivityMainBinding
    private var data = mutableListOf<String>()
    private val searchIndexList = mutableListOf<Int>()
    private lateinit var threadPoolManager: ThreadPoolManager

    private lateinit var dialog: AlertDialog
    private lateinit var pickit: PickiT
    private lateinit var adapter: TextPageAdapter

    private lateinit var params: WindowManager.LayoutParams

    // 애니메이션 용도.
    private var settingLayoutTop: Float = 0.0f
    private var settingLayoutBottom: Float = 0.0f

    private var intentUri: String? = null

    private val textViewModel: TextViewModel by lazy {
        ViewModelProvider(this).get(TextViewModel::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        init()
        getActivitySize()
        getSettingLayoutSize()
        initDialog()
    }

    private fun saveDataUri() {
        // uri 가 없다면 최초로 앱을 깔고 파일을 연적이 없으므로
        // empty layout 을 보여준다.
        val data = PreferenceJsonUtil.getSaveObject(this, "data", PreferenceJsonUtil.SAVE_DATA)
        if (data == null) {
            textViewModel.apply {
                textLineSpacing = 2
                binding.lineSpacingCount.centerCount.text = 3.toString()
                binding.textBackColor.color1.isSelected = true
                binding.fontChangeLayout.serif.setTextColor(
                    ContextCompat.getColor(
                        this@MainActivity,
                        R.color.orange_color
                    )
                )
                backGroundColor = R.color.color1
                textColor = R.color.color4
                aWidth = binding.root.width
                aHeight = binding.root.height
                fontType = "serif"
                fontChangeColor(fontType)
                textSizeDimen = 0
                displaySizeGetRatio(aWidth, aHeight)
            }

            firstEnableButtonAndLayout(true)
            return
        }

        firstEnableButtonAndLayout(false)

        // data 를 한번 가져온적이 있을 때
        if (intentUri == null) {
            restoreData(data)
        } else {
            getBookMarkData()
        }
    }

    private fun getBookMarkData() {
        val future = threadPoolManager.executorService.submit(Callable<Data> {
            return@Callable textViewModel.getData(intentUri!!)
        })

        try {
            val data = future.get()
            val saveData = SaveData().apply {
                uri = data.uri
                page = data.page
                landData = data.landData
                textSize = data.textSize
                textDimension = data.textDimension
                backgroundColor = data.backgroundColor
                textColor = data.textColor
                lineSpace = data.lineSpace
                beforeDataSize = data.beforeDataSize
                fontType = data.fontType
            }
            restoreData(saveData)
        } catch (e: Exception) {
            Log.e(TAG, "get book mark data Error : ${e.message}")
        }
    }

    private fun displaySizeGetRatio(aWidth: Int, aHeight: Int) {
        textViewModel.textCount = DisplayUtil.getWidthDisplay(aWidth)
        textViewModel.maxLine = DisplayUtil.getHeightDisplay(aHeight)

        textSizeOffset()
        landOffset(aHeight, aWidth)
    }

    private fun textSizeOffset() {
        when (textViewModel.textSizeDimen) {
            0 -> {
                textViewModel.maxLine -= 0
                textViewModel.textCount -= 0
            }
            1 -> {
                textViewModel.maxLine -= 1
                textViewModel.textCount -= 4
            }
            2 -> {
                textViewModel.maxLine -= 2
                textViewModel.textCount -= 8
            }
        }
    }

    private fun landOffset(aHeight: Int, aWidth: Int) {
        val orientation = resources.configuration.orientation
        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            Log.d(TAG, "landOffSet: ${DisplayUtil.landLineOffset(aHeight, aWidth)}")
            textViewModel.maxLine -= DisplayUtil.landLineOffset(aHeight, aWidth)
        }
    }

    private fun restoreData(data: SaveData) {
        textViewModel.apply {
            if (data.landData.isNotBlank()) {
                currentPageData = data.landData // landscape 의 경우
                beforeDataSize = data.beforeDataSize
            }

            pagePosition = data.page
            textLineSpacing = data.lineSpace
            restoreLineSpacing()
            textSizeDimen = data.textDimension
            textSize = data.textSize
            aWidth = binding.root.width
            aHeight = binding.root.height
            backGroundColor = data.backgroundColor
            textColor = data.textColor
            fontType = data.fontType
            fontChangeColor(fontType)
            restoreFontColor()

            displaySizeGetRatio(aWidth, aHeight)
            binding.textSizeCount.centerCount.text = data.textSize.toString()
            restoreBackgroundColor()
            val uri = Uri.parse(data.uri)
            contentUri = uri
            getFileUriAndRender(uri)
        }
    }

    private fun TextViewModel.restoreBackgroundColor() {
        when (backGroundColor) {
            R.color.color1 -> colorSelected(1, true)
            R.color.color2 -> colorSelected(2, true)
            R.color.color3 -> colorSelected(3, true)
            R.color.color4 -> colorSelected(4, true)
        }
    }

    private fun TextViewModel.restoreLineSpacing() {
        val count = textLineSpacing + 1

        if (count > 0)
            binding.lineSpacingCount.centerCount.text = count.toString()
    }

    private fun TextViewModel.restoreFontColor() {
        when (fontType) {
            "serif" -> binding.fontChangeLayout.serif.setTextColor(
                ContextCompat.getColor(
                    this@MainActivity,
                    R.color.orange_color
                )
            )
            "gamja" -> binding.fontChangeLayout.gamja.setTextColor(
                ContextCompat.getColor(
                    this@MainActivity,
                    R.color.orange_color
                )
            )
            "gothic" -> binding.fontChangeLayout.gothic.setTextColor(
                ContextCompat.getColor(
                    this@MainActivity,
                    R.color.orange_color
                )
            )
            "myeungjo" -> binding.fontChangeLayout.myeungJo.setTextColor(
                ContextCompat.getColor(
                    this@MainActivity,
                    R.color.orange_color
                )
            )
        }
    }

    private fun getFileUriAndRender(uri: Uri) {
        getFileName(uri)
        dialog.show()
        pickit.getPath(uri, Build.VERSION.SDK_INT)
    }

    private fun firstEnableButtonAndLayout(init: Boolean) {
        binding.backButton.isEnabled = init.not()
        binding.forwardButton.isEnabled = init.not()
        binding.emptyLayout.isVisible = init
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
        params = window.attributes // 현재 밝기 정보 가져오기
        intentUri = intent.getStringExtra(URI_DATA) ?: return
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

        binding.viewPager.registerOnPageChangeCallback(pageChangeListener)

        searchLayoutEvent()
        centerLayoutEvent()
        toolbarEvent()
        settingLayoutEvent()
    }


    private fun centerLayoutEvent() {
        binding.emptyFileOpenButton.setOnClickListener {
            textReadProcess()
        }

        binding.backButton.setOnClickListener {
            forward()
        }

        binding.forwardButton.setOnClickListener {
            backward()
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun settingLayoutEvent() {

        binding.splitLayout.isClickable = true

        // 글자 크기 변경.
        binding.textSizeCount.plusCount.setOnTouchListener(changeTextSizeListener)
        binding.textSizeCount.minusCount.setOnTouchListener(changeTextSizeListener)

        // 배경, 글자색 변경.
        binding.textBackColor.color1.setOnClickListener {
            colorStateChange(true, 1, R.color.color1, R.color.black)
        }

        binding.textBackColor.color2.setOnClickListener {
            colorStateChange(true, 2, R.color.color2, R.color.black)
        }

        binding.textBackColor.color3.setOnClickListener {
            colorStateChange(true, 3, R.color.color3, R.color.black)
        }

        binding.textBackColor.color4.setOnClickListener {
            colorStateChange(true, 4, R.color.color4, R.color.white)
        }

        // 줄 간격
        binding.lineSpacingCount.plusCount.setOnTouchListener(changeLineSpacingListener)
        binding.lineSpacingCount.minusCount.setOnTouchListener(changeLineSpacingListener)

        // 화면 밝기
        binding.lightSeekbar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    Log.d(TAG, "progress : $progress")
                    params.screenBrightness = (progress / 100.0).toFloat()
                    window.attributes = params
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
            }
        })

        // 글자 폰트
        binding.fontChangeLayout.serif.setOnClickListener {
            fontChangeColor("serif")
            adapter.notifyDataSetChanged()
        }

        binding.fontChangeLayout.gamja.setOnClickListener {
            fontChangeColor("gamja")
            adapter.notifyDataSetChanged()
        }

        binding.fontChangeLayout.gothic.setOnClickListener {
            fontChangeColor("gothic")
            adapter.notifyDataSetChanged()
        }

        binding.fontChangeLayout.myeungJo.setOnClickListener {
            fontChangeColor("myeungjo")
            adapter.notifyDataSetChanged()
        }
    }

    private fun fontChangeColor(font: String) {
        var typeFace: Typeface? = null

        when (font) {
            "serif" -> {
                binding.fontChangeLayout.apply {
                    serif.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.orange_color))
                    gamja.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.white))
                    gothic.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.white))
                    myeungJo.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.white))
                }
                typeFace = getFont(this@MainActivity, R.font.noto_serif_semi_bold)
            }

            "gamja" -> {
                binding.fontChangeLayout.apply {
                    serif.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.white))
                    gamja.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.orange_color))
                    gothic.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.white))
                    myeungJo.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.white))
                }
                typeFace = getFont(this@MainActivity, R.font.gamja_flower_regular)
            }
            "gothic" -> {
                binding.fontChangeLayout.apply {
                    serif.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.white))
                    gamja.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.white))
                    gothic.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.orange_color))
                    myeungJo.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.white))
                }
                typeFace = getFont(this@MainActivity, R.font.gothic_a1_regular)
            }

            "myeungjo" -> {
                binding.fontChangeLayout.apply {
                    serif.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.white))
                    gamja.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.white))
                    gothic.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.white))
                    myeungJo.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.orange_color))
                }
                typeFace = getFont(this@MainActivity, R.font.nanum_myeungjo_regular)
            }
        }
        textViewModel.fontType = font

        typeFace?.let {
            textViewModel.typeFace = it
        }
    }

    private fun colorStateChange(flag: Boolean, check: Int, backColor: Int, textColor: Int) {
        colorSelected(check, flag)
        backTextColorChange(backColor, textColor)
    }

    private fun colorSelected(check: Int, flag: Boolean) {
        if (check == 1) {
            binding.textBackColor.color1.isSelected = flag
            binding.textBackColor.color2.isSelected = !flag
            binding.textBackColor.color3.isSelected = !flag
            binding.textBackColor.color4.isSelected = !flag
        }

        if (check == 2) {
            binding.textBackColor.color1.isSelected = !flag
            binding.textBackColor.color2.isSelected = flag
            binding.textBackColor.color3.isSelected = !flag
            binding.textBackColor.color4.isSelected = !flag
        }

        if (check == 3) {
            binding.textBackColor.color1.isSelected = !flag
            binding.textBackColor.color2.isSelected = !flag
            binding.textBackColor.color3.isSelected = flag
            binding.textBackColor.color4.isSelected = !flag
        }

        if (check == 4) {
            binding.textBackColor.color1.isSelected = !flag
            binding.textBackColor.color2.isSelected = !flag
            binding.textBackColor.color3.isSelected = !flag
            binding.textBackColor.color4.isSelected = flag
        }
    }

    private fun backTextColorChange(background: Int, textColor: Int) {
        textViewModel.backGroundColor = background
        textViewModel.textColor = textColor
        adapter.notifyDataSetChanged()
    }

    private fun searchLayoutEvent() {

        // 검색, 앞으로, 뒤로
        binding.search.setOnClickListener {
            startSearching("search")
        }

        binding.forward.setOnClickListener {
            startSearching("forward")
        }

        binding.back.setOnClickListener {
            startSearching("back")
        }

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

    private fun toolbarEvent() {
        binding.fileOpenButton.setOnClickListener {
            textReadProcess()
        }

        binding.switchNightMode.setOnCheckedChangeListener { _, isChecked ->
            binding.nightMode.isVisible = isChecked
        }

        binding.settingButton.setOnClickListener {
            if (binding.searchLayout.isVisible) {
                binding.searchLayout.isVisible = false
            }

            binding.settingLayout.isVisible = true

            val heightAnimator = ObjectAnimator.ofFloat(
                binding.settingLayout,
                "y",
                settingLayoutBottom,
                settingLayoutTop
            ).setDuration(200)
            heightAnimator.start()
        }

        binding.bookMarkButton.setOnClickListener { view ->
            PopupMenu(this, view).apply {
                setOnMenuItemClickListener(popupListener)
                inflate(R.menu.bookmark_menu)
                show()
            }
        }
    }

    private val popupListener = PopupMenu.OnMenuItemClickListener { menu ->
        when (menu.itemId) {
            R.id.go_bookmark -> {
                val intent = Intent(this@MainActivity, BookMarkActivity::class.java)
                startActivity(intent)
                true
            }
            R.id.add_bookmark -> {
                addBookmarkProcess()
                true
            }
            else -> false
        }
    }

    private fun addBookmarkProcess() {
        val saveData = Data()
        saveData.apply {
            uri = textViewModel.contentUri.toString()
            name = textViewModel.displayName
            page = textViewModel.pagePosition
            lineSpace = textViewModel.textLineSpacing
            landData = data[textViewModel.pagePosition]
            textSize = binding.textSizeCount.centerCount.text.sToInt()
            textDimension = textViewModel.textSizeDimen
            backgroundColor = textViewModel.backGroundColor
            textColor = textViewModel.textColor
            beforeDataSize = data.size
            fontType = textViewModel.fontType
        }

        Log.d(TAG, "background color : ${saveData.backgroundColor}")
        Log.d(TAG, "text color : ${saveData.textColor}")

        val future = threadPoolManager.executorService.submit {
            val check = textViewModel.getData(saveData.uri)
            if (check == null) {
                textViewModel.addData(saveData)
            } else {
                textViewModel.updateData(saveData)
            }
        }

        try {
            future.get()
            Toast.makeText(this, "북마크에 추가되었습니다.", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e(TAG, "${e.message}")
        }
    }

    private val changeLineSpacingListener = object : View.OnTouchListener {
        @SuppressLint("ClickableViewAccessibility")
        override fun onTouch(view: View, event: MotionEvent?): Boolean {
            event ?: return false

            when ((view as TextView).id) {
                R.id.plusCount -> changeUpDownLineSpacing(event, view, 1)
                R.id.minusCount -> changeUpDownLineSpacing(event, view, -1)
            }
            return true
        }
    }

    private fun changeUpDownLineSpacing(event: MotionEvent, view: TextView, check: Int) {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                if (check == -1) lineSizeDown() else lineSizeUp()
                view.setTextColor(ContextCompat.getColor(this, R.color.orange_color))
            }
            MotionEvent.ACTION_UP -> {
                mHandler.postDelayed({
                    view.setTextColor(ContextCompat.getColor(this, R.color.color1))
                }, 100)
            }
        }
    }

    private fun lineSizeDown() {
        val text = binding.lineSpacingCount.centerCount.text.toString()
        var count = text.toInt()

        if (count > 1) {
            count -= 1
            binding.lineSpacingCount.centerCount.text = count.toString()
            changeLineSpacingRender(count)
        }
    }

    private fun lineSizeUp() {
        val text = binding.lineSpacingCount.centerCount.text.toString()
        var count = text.toInt()

        if (count < 4) {
            count += 1
            binding.lineSpacingCount.centerCount.text = count.toString()
            changeLineSpacingRender(count)
        }
    }

    private fun changeLineSpacingRender(count: Int) {
        textViewModel.textLineSpacing = count - 1
        adapter.notifyDataSetChanged()
    }

    private val changeTextSizeListener = object : View.OnTouchListener {
        @SuppressLint("ClickableViewAccessibility")
        override fun onTouch(view: View, event: MotionEvent?): Boolean {
            event ?: return false

            when ((view as TextView).id) {
                R.id.plusCount -> changeUpDownColor(event, view, 1)
                R.id.minusCount -> changeUpDownColor(event, view, -1)
            }
            return true
        }
    }

    private fun changeUpDownColor(event: MotionEvent, view: View, check: Int) {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                if (check == -1) textSizeDown() else textSizeUp()
                (view as TextView).setTextColor(ContextCompat.getColor(this, R.color.orange_color))
            }
            MotionEvent.ACTION_UP -> {
                mHandler.postDelayed({
                    (view as TextView).setTextColor(ContextCompat.getColor(this, R.color.color1))
                }, 100)
            }
        }
    }

    private fun textSizeDown() {
        val text = binding.textSizeCount.centerCount.text.toString()
        textViewModel.textSize = text.toInt()

        var count = textViewModel.textSize
        if (count > 1) {
            count -= 1
            binding.textSizeCount.centerCount.text = count.toString()
            textViewModel.apply {
                checkTextError = -1
                currentPageData = data[textViewModel.pagePosition]
                textCount += 4
                maxLine += 1
            }
            changeCountRender(count)
        }
    }

    private fun textSizeUp() {
        val text = binding.textSizeCount.centerCount.text.toString()
        textViewModel.textSize = text.toInt()

        var count = textViewModel.textSize
        if (count < 3) {
            count += 1
            binding.textSizeCount.centerCount.text = count.toString()

            textViewModel.apply {
                checkTextError = 1
                currentPageData = data[textViewModel.pagePosition]
                textCount -= 4
                maxLine -= 1
            }
            changeCountRender(count)
        }
    }

    private fun backward() {
        textViewModel.pagePosition += 1
        if (textViewModel.pagePosition >= data.size) {
            textViewModel.pagePosition = data.size - 1
        }
        binding.viewPager.currentItem = textViewModel.pagePosition
    }

    private fun forward() {
        textViewModel.pagePosition -= 1
        if (textViewModel.pagePosition <= 0) {
            textViewModel.pagePosition = 0
        }
        binding.viewPager.currentItem = textViewModel.pagePosition
    }

    private fun changeCountRender(count: Int) {
        when (count) {
            1 -> changeRatio(0)
            2 -> changeRatio(1)
            3 -> changeRatio(2)
        }
    }

    private fun changeRatio(dimen: Int) {
        textViewModel.textSizeDimen = dimen
        textViewModel.beforeDataSize = data.size - 1

        textViewModel.contentUri ?: return
        dataRefresh(false)
        getFileUriAndRender(textViewModel.contentUri!!)
    }

    private fun startSearching(key: String) {
        if (binding.edtSearch.text.isNullOrEmpty().not()) {
            val searchText = binding.edtSearch.text
            startSearchThread(searchText!!.length, searchText.toString(), key)
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
                // landscape 일때 데이터 저장.
                saveDataUri()
                Log.d(TAG, "width : ${textViewModel.aWidth}")
                Log.d(TAG, "height : ${textViewModel.aHeight}")
            }
        })
    }

    private fun getSettingLayoutSize() {
        binding.settingLayout.viewTreeObserver.addOnGlobalLayoutListener(object :
            ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                binding.settingLayout.viewTreeObserver.removeOnGlobalLayoutListener(this)

                settingLayoutTop = binding.settingLayout.top.toFloat()
                settingLayoutBottom = binding.settingLayout.bottom.toFloat()

                Log.d(TAG, "top : $settingLayoutTop")
                Log.d(TAG, "bottom : $settingLayoutBottom")
            }
        })
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
                        firstPageOpen()
                    }
                }
            }
        }
    }

    private fun firstPageOpen() {
        initLightBar()
        initSeekBar()
        displayPager()
        dialog.dismiss()
    }

    private fun initLightBar() {
        params.screenBrightness = 0.5f
        binding.lightSeekbar.progress = 50
        window.attributes = params
    }

    private fun emptyLayoutInVisible() {
        firstEnableButtonAndLayout(false)
    }

    private fun rotatePageChange() {
        val rotateTask = RotatePageSearchTask(textViewModel, data)
        val future = threadPoolManager.executorService.submit(rotateTask)

        try {
            future.get()
            initLightBar()
            initSeekBar()
            displayPager()
            dialog.dismiss()
        } catch (e: Exception) {
            Log.e(TAG, "future error : ${e.message}")
            Toast.makeText(this, "검색 실패!", Toast.LENGTH_SHORT).show()
            errorFun()
        }
    }

    private fun errorFun() {
        val count = binding.textSizeCount.centerCount.text.sToInt()
        binding.textSizeCount.centerCount.text = if (textViewModel.checkTextError == -1) {
            (count + 1).toString()
        } else {
            (count - 1).toString()
        }
        textViewModel.pagePosition = binding.viewPager.currentItem
        dialog.dismiss()
    }

    private fun updateAdapter(type: String) {
        Log.d(TAG, "searchIndex : $searchIndexList")
        if (type == "search") {
            binding.edtSearch.text?.let {
                val pair = Pair(binding.edtSearch.text!!.length, searchIndexList)
                adapter.apply {
                    keywordListener = { pair }
                    notifyItemChanged(textViewModel.pagePosition, "search")
                }
            }
            return
        }

        // 페이지를 변경 많이 하게 되면 내부적으로 notifyChange 가 호출 되는 것 같음
        // 그래서 페이지 를 변경할때마다 검색어를 표시하는 것이 안됨. 다른방법이 있는지는 모르겠으나.
        // 콜백이 안먹히는 상황이 발생함.
        binding.viewPager.currentItem = textViewModel.pagePosition
    }

    private fun dataRefresh(pageRefresh: Boolean) {
        if (data.isNotEmpty()) {
            data.clear()
        }
        if (pageRefresh) {
            textViewModel.pagePosition = 0
            textViewModel.currentPageData = ""
        }
        textViewModel.displayName = ""
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
                        dataRefresh(true)
                        val uri = result.data?.data!!

                        contentResolver.takePersistableUriPermission(
                            uri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION
                        )
                        textViewModel.contentUri = uri
                        emptyLayoutInVisible()
                        getFileUriAndRender(uri)
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
        adapter = TextPageAdapter(
            this@MainActivity,
            data,
            searchViewVisibleListener = { setSearchVisible(it) },
            setTextSizeListener = { getTextDimen() },
            setColorChangeListener = { getBackTextColor() },
            setLineSpacingChangeListener = { getLineSpace() },
            setFontChangeListener = { getTypeFace() }
        )
        adapter.setHasStableIds(true)
        binding.viewPager.adapter = adapter
        binding.viewPager.setCurrentItem(textViewModel.pagePosition, false)
        binding.titleTextView.text = textViewModel.displayName
    }

    private fun getTextDimen(): Float {
        return when (textViewModel.textSizeDimen) {
            0 -> resources.getDimension(R.dimen.textSizeS)
            1 -> resources.getDimension(R.dimen.textSizeM)
            2 -> resources.getDimension(R.dimen.textSizeL)
            else -> throw IndexOutOfBoundsException("text size 범위 에러")
        }
    }

    private fun getLineSpace(): Float {
        return when (textViewModel.textLineSpacing) {
            0 -> resources.getDimension(R.dimen.lineSpacing1)
            1 -> resources.getDimension(R.dimen.lineSpacing2)
            2 -> resources.getDimension(R.dimen.lineSpacing3)
            3 -> resources.getDimension(R.dimen.lineSpacing4)
            else -> throw IndexOutOfBoundsException("line spacing 범위 에러")
        }
    }

    private fun getBackTextColor(): Pair<Int, Int> =
        Pair(textViewModel.backGroundColor, textViewModel.textColor)

    private fun getTypeFace(): Typeface {
        val typeface = textViewModel.typeFace
        return typeface!!
    }

    private fun setSearchVisible(it: Boolean) {
        if (binding.settingLayout.isVisible) {
            binding.settingLayout.isVisible = it.not()
            binding.toolBarLayout.isVisible = it.not()
            return
        }

        if (binding.searchLayout.isVisible) {
            adapter.notifyDataSetChanged()
            binding.searchLayout.isVisible = it.not()
            binding.toolBarLayout.isVisible = it.not()
        } else {
            binding.searchLayout.isVisible = it
            binding.toolBarLayout.isVisible = it
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
            if (uriPath.contains(textViewModel.displayName)) {
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
        if (binding.settingLayout.isVisible) {
            settingLayoutAnimation()

            mHandler.postDelayed({
                binding.settingLayout.isVisible = false
            }, 200)

            binding.searchLayout.isVisible = true
        } else {
            AlertDialog.Builder(this)
                .setMessage("종료 하시겠습니까?")
                .setPositiveButton("확인") { _, _ ->
                    super.onBackPressed()
                }.setNegativeButton("취소") { _, _ -> }
                .setCancelable(false)
                .show()
        }
    }

    private fun settingLayoutAnimation() {
        val heightAnimator = ObjectAnimator.ofFloat(
            binding.settingLayout,
            "y",
            settingLayoutTop,
            settingLayoutBottom
        ).setDuration(200)
        heightAnimator.start()
    }

    override fun onStop() {
        if (data.isNotEmpty()) {
            textViewModel.currentPageData = data[textViewModel.pagePosition]
            textViewModel.beforeDataSize = data.size - 1
            saveDataPreference(SaveData())
        }
        super.onStop()
    }

    private fun saveDataPreference(saveData: SaveData) {
        val orientation = resources.configuration.orientation
        if (orientation != Configuration.ORIENTATION_LANDSCAPE) {
            saveData.landData = ""
        } else {
            saveData.landData = data[textViewModel.pagePosition]
        }

        saveData.apply {
            page = textViewModel.pagePosition
            uri = textViewModel.contentUri.toString()
            textSize = binding.textSizeCount.centerCount.text.sToInt()
            textDimension = textViewModel.textSizeDimen
            backgroundColor = textViewModel.backGroundColor
            lineSpace = textViewModel.textLineSpacing
            textColor = textViewModel.textColor
            beforeDataSize = data.size - 1
            fontType = textViewModel.fontType
        }

        PreferenceJsonUtil.putSaveObject(this, "data", saveData, PreferenceJsonUtil.SAVE_DATA)
    }

    private fun CharSequence.sToInt() = this.toString().toInt()

    override fun onDestroy() {
        if (!isChangingConfigurations) {
            pickit.deleteTemporaryFile(this)
        }
        binding.viewPager.unregisterOnPageChangeCallback(pageChangeListener)
        threadPoolManager.executorService.isShutdown
        super.onDestroy()
    }

    companion object {
        fun newIntent(context: Context, uri: String): Intent {
            return Intent(context, MainActivity::class.java).putExtra(URI_DATA, uri)
        }

        private const val URI_DATA = "uri"
    }
}