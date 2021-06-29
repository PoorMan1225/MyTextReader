package com.rjhwork.mycompany.fileopen.util

import android.content.Context
import android.util.Log
import androidx.core.content.edit
import com.google.gson.Gson
import com.rjhwork.mycompany.fileopen.TAG
import com.rjhwork.mycompany.fileopen.model.SaveData
import org.json.JSONArray
import org.json.JSONException

class PreferenceJsonUtil {

    companion object {
        const val PAGE_SAVE = "page"
        const val URI_SAVE = "uri"
        const val LAND_DATA = "landscape"
        const val SAVE_DATA = "data"

        fun putSaveObject(context:Context, key:String, value:SaveData, spKey: String) {
            val preference = context.getSharedPreferences(spKey, Context.MODE_PRIVATE)
            val gson = Gson()
            val json = gson.toJson(value)
            preference.edit {
                putString(key, json)
                apply()
            }
        }

        fun getSaveObject(context: Context, key: String, spKey: String): SaveData? {
            val gson = Gson()
            val json = context.getSharedPreferences(spKey, Context.MODE_PRIVATE).getString(key, null) ?: return null
            val data = gson.fromJson(json, SaveData::class.java)
            return data
        }

        fun putSavePreference(context: Context, key: String, value: Int, spKey: String) {
            val preference = context.getSharedPreferences(spKey, Context.MODE_PRIVATE)
            preference.edit {
                putInt(key, value)
                apply()
            }
        }

        fun putSavePreference(context: Context, key: String, value: String, spKey: String) {
            val preference = context.getSharedPreferences(spKey, Context.MODE_PRIVATE)
            preference.edit {
                putString(key, value)
                apply()
            }
        }

        fun getSavePreference(context: Context, key: String, spKey: String) =
            context.getSharedPreferences(spKey, Context.MODE_PRIVATE).getInt(key, 0)

        fun getSavePreference(context: Context, key: String, spKey: String, default:String = "") =
            context.getSharedPreferences(spKey, Context.MODE_PRIVATE).getString(key, null)

        fun putStringArrayPref(
            context: Context,
            key: String,
            value: MutableList<String>,
            spKey: String
        ) {
            val preference = context.getSharedPreferences(spKey, Context.MODE_PRIVATE)
            val jsonArray = JSONArray()

            value.forEach { data ->
                jsonArray.put(data)
            }

            preference.edit {
                if (value.isNotEmpty()) {
                    putString(key, jsonArray.toString())
                } else {
                    putString(key, null)
                }
                apply()
            }
        }

        fun getStringArrayPref(context: Context, key: String, spKey: String): MutableList<String>? {
            val preference = context.getSharedPreferences(spKey, Context.MODE_PRIVATE)
            val json = preference.getString(key, null)
            val dataList = mutableListOf<String>()

            if (json == null) {
                return null
            }

            try {
                val jsonArray = JSONArray(json)
                for (i in 0 until jsonArray.length()) {
                    val data = jsonArray.optString(i)
                    dataList.add(data)
                }
            } catch (e: JSONException) {
                Log.e(TAG, "GET JSON ARRAY ERROR : ${e.message}")
            }
            return dataList
        }
    }
}