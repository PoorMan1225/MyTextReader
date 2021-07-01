package com.rjhwork.mycompany.fileopen.model

data class SaveData(
    var uri: String = "",
    var page:Int = 0,
    var landData:String = "",
    var textSize:Int = 1,
    var textDimension:Float = 0f,
    var widthCountRatio:Int = 0,
    var heightLineRatio:Int = 0,
    var backgroundColor:Int = 0,
    var textColor: Int = 0,
    var lineSpace: Float = 0f
)