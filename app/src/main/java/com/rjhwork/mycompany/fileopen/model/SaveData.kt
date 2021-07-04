package com.rjhwork.mycompany.fileopen.model

import android.graphics.Typeface

data class SaveData(
    var uri: String = "",
    var page:Int = 0,
    var landData:String = "",
    var textSize:Int = 1,
    var textDimension:Float = 0f,
    var backgroundColor:Int = 0,
    var textColor: Int = 0,
    var lineSpace: Float = 0f,
    var beforeDataSize:Int = 0,
    var typeFace: Typeface? = null
)