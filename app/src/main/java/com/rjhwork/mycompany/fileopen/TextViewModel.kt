package com.rjhwork.mycompany.fileopen

import androidx.lifecycle.ViewModel

class TextViewModel : ViewModel() {

    var textCount = 0
    var maxLine = 0

    var aWidth = 0
        set(value) {
            field = value
            textCount = (aWidth * 0.028).toInt()
        }

    var aHeight = 0
        set(value) {
            field = value
            maxLine = (aHeight * 0.0065).toInt()
        }
}