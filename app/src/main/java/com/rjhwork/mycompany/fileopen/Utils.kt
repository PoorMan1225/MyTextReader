package com.rjhwork.mycompany.fileopen

import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import java.io.File

fun getRealPathFromURI(context: Context, uri: Uri): String? {
    if (DocumentsContract.isDocumentUri(context, uri)) {
        if (isExternalStorageDocument(uri)) {
            val docId = DocumentsContract.getDocumentId(uri)
            val split = docId.split(":")
            val type = split[0]
            if ("primary" == type) {
                return "${Environment.getExternalStorageDirectory()}/${split[1]}"
            } else {
                val sdCardPath = getRemovableSDCardPath(context).split("/Android")[0]
                return "${sdCardPath}/${split[1]}"
            }
        } else if (isDownloadsDocument(uri)) {
            val fileName = getFilePath(context, uri);
            if (fileName != null) {
                return Environment.getExternalStorageDirectory().toString() + "/Download/" + fileName;
            }

            var id = DocumentsContract.getDocumentId(uri);
            if (id.startsWith("raw:")) {
                id = id.replaceFirst("raw:", "");
                val file = File(id)
                if (file.exists())
                    return id
            }
            val contentUri = ContentUris.withAppendedId(Uri.parse("content://downloads/public_downloads"), id.toLong())
            return getDataColumn(context, contentUri)

        } else if(isMediaDocument(uri)) {
            val docId = DocumentsContract.getDocumentId(uri)
            val split = docId.split(":")
            val type = split[0]

            val contentUri:Uri = when (type) {
                "image" -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                "video" -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                "audio" -> MediaStore.Audio.Media.EXTERNAL_CONTENT_URI

                else -> MediaStore.Files.getContentUri("external")
            }

            val selection ="_id=?" // 행을 선택하는 기준.
            val selectionArgs:Array<String> = arrayOf(split[1]) // ? 에 들어가는 검색할 값.

            return getDataColumn(context, contentUri, selection, selectionArgs)
        } else if("content" == uri.scheme) {
            return getDataColumn(context, uri, null, null)
        }else if("file" == uri.scheme) {
            return uri.path
        }
    }
    return null
}

private fun getFilePath(context:Context, uri:Uri):String? {

    var cursor:Cursor? = null
    val projection:Array<String> = arrayOf(MediaStore.MediaColumns.DISPLAY_NAME)

    try {
        cursor = context.contentResolver.query(uri, projection, null, null,
            null);
        if (cursor != null && cursor.moveToFirst()) {
            val index = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME);
            return cursor.getString(index)
        }
    } finally {
        cursor?.close()
    }
    return null
}

fun getRemovableSDCardPath(context: Context): String {
    val storages = ContextCompat.getExternalFilesDirs(context, null)
    if (storages.size > 1 && storages[0] != null && storages[1] != null) {
        return storages[1].toString()
    } else {
        return ""
    }
}

fun getDataColumn(
    context: Context,
    contextUri: Uri,
    selection: String? = null,
    selectionArgs: Array<out String>? = null
): String? {
    var cursor: Cursor? = null
    val column = "_data"
    val projection: Array<String> = arrayOf(column)

    try {
        cursor =
            context.contentResolver.query(contextUri, projection, selection, selectionArgs, null)
        if (cursor != null && cursor.moveToFirst()) {
            val index = cursor.getColumnIndexOrThrow(column)
            return cursor.getString(index)
        }
    } finally {
        cursor?.close()
    }
    return null
}

fun isDownloadsDocument(uri: Uri) = "com.android.providers.downloads.documents" == uri.authority

fun isExternalStorageDocument(uri: Uri) = "com.android.externalstorage.documents" == uri.authority

fun isMediaDocument(uri:Uri) = "com.android.providers.media.documents" == uri.authority