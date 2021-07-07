package com.rjhwork.mycompany.fileopen.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.*

@Entity
data class Data(
    @PrimaryKey var uri:String = "",
    var name:String = "",
    var page:Int = 0,
    var date: Date = Date(),
    var landData:String = "",
    var textSize:Int = 1,
    var textDimension:Int = 0,
    var backgroundColor:Int = 0,
    var textColor: Int = 0,
    var lineSpace: Int = 0,
    var beforeDataSize:Int = 0,
    var fontType: String = ""
)