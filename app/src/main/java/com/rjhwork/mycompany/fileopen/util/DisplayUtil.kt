package com.rjhwork.mycompany.fileopen.util

class DisplayUtil {
    companion object {
        fun getWidthDisplay(aWidth:Int):Float {
           return when(aWidth) {
                in 0 until 200 -> 10f
                in 200 until 300 -> 15f
                in 300 until 400 -> 17f
                in 400 until 500 -> 18f
                in 500 until 600 -> 19f
                in 600 until 700 -> 20f
                in 700 until 800 -> 21f
                in 800 until 900 -> 22f
                in 900 until 1100 -> 23f
                in 1100 until 1300 -> 25f
                in 1300 until 1500 -> 27f
                in 1500 until 1600 -> 33f
                in 1700 until 1800 -> 37f
                else -> 40f
            }
        }

        fun getHeightDisplay(aHeight:Int): Int {
           return when(aHeight) {
                in 0 until 100 -> 4
                in 100 until 200 -> 5
                in 200 until 300 -> 7
                in 300 until 400 -> 8
                in 400 until 600 -> 9
                in 600 until 700 -> 10
                in 700 until 1000 -> 11
                in 1000 until 1900 -> 12
                in 1900 until 2000 -> 15
                in 2000 until 2100 -> 16
                in 2100 until 2750 -> 17
                else -> 18
            }
        }

        fun landLineOffset(aHeight:Int, aWidth: Int):Int {
            if(aHeight.toFloat() / aWidth >= 0.8) {
                return 0
            }

            return  when(aHeight) {
                in 0 until 450 -> 3
                in 450 until 800 -> 4
                in 800 until 1400 -> 5
                else -> 5
            }
        }
    }
}