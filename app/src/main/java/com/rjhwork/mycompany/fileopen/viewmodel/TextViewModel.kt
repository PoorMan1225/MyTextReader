package com.rjhwork.mycompany.fileopen.viewmodel

import android.graphics.Typeface
import android.net.Uri
import androidx.lifecycle.ViewModel

class TextViewModel : ViewModel() {

    // 텍스트 세로 열 개수
    var textCount = 0f

    // 텍스트 가로 행 개수
    var maxLine = 0

    // 화면 가로 넓이 픽셀
    var aWidth = 0

    // 화면의 세로 넓이 픽셀
    var aHeight = 0

    // 현재 페이지 위치
    var pagePosition = 0

    // 현재 페이지 데이터
    var currentPageData:String = ""

    // 검색전 페이지 데이터 사이즈
    var beforeDataSize = 0

    // 현재 text 이름
    var displayName:String = ""

    // 현재 text uri
    var contentUri: Uri? = null

    // 현재 text 의 크기 설정
    var textSize:Int = 1

    // 리소 현재 사이즈 sp
    var textSizeDimen:Float = 0f

    // 줄간격 설정
    var textLineSpacing:Float = 0f

    // 글자 색 설정
    var textColor:Int = 0

    // 배경 색 설정
    var backGroundColor: Int = 0

    // typface
    var typeFace: Typeface? = null

    var checkTextError: Int = 0
}