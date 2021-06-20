package com.rjhwork.mycompany.fileopen.util

import org.mozilla.universalchardet.UniversalDetector
import java.io.File
import java.io.FileInputStream

class UniversalDetectorUtil {
    companion object {
        fun findFileEncoding(file: File):String {
            val buf:ByteArray = ByteArray(4096)
            val fis = FileInputStream(file)

            val detector = UniversalDetector(null)

            var nread = fis.read(buf)
            while (nread > 0 && !detector.isDone) {
                detector.handleData(buf, 0, nread)
                nread = fis.read(buf)
            }

            detector.dataEnd()

            val encoding = detector.detectedCharset
            detector.reset()
            return encoding
        }
    }
}