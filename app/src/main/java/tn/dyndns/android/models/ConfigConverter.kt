package tn.dyndns.android.models

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object ConfigConverter {
    val gson = Gson()

    inline fun <reified T> toJson(config: T): String {
        return gson.toJson(config)
    }

    inline fun <reified T> fromJson(jsonString: String): T {
        val type = object : TypeToken<T>() {}.type
        return gson.fromJson(jsonString, type)
    }
}