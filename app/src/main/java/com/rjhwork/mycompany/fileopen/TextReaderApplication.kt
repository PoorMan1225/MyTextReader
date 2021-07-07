package com.rjhwork.mycompany.fileopen

import android.app.Application
import com.rjhwork.mycompany.fileopen.database.TextRepository

class TextReaderApplication:Application() {

    override fun onCreate() {
        super.onCreate()
        TextRepository.initialize(this)
    }
}