package com.rjhwork.mycompany.fileopen

import java.util.concurrent.Executors

class ThreadPoolManager private constructor() {
    val executorService = Executors.newSingleThreadExecutor()

    companion object {
        private var instance:ThreadPoolManager? = null

        fun getInstance():ThreadPoolManager {
            if(instance == null) {
                instance = ThreadPoolManager()
            }
            return instance as ThreadPoolManager
        }
    }
}