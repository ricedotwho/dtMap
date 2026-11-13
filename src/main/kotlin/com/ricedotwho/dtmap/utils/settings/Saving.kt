package com.ricedotwho.dtmap.utils.settings

import com.google.gson.Gson
import com.google.gson.JsonElement

/**
 * zzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzz
 */
interface Saving {
    fun read(element: JsonElement, gson: Gson)
    fun write(gson: Gson): JsonElement
}