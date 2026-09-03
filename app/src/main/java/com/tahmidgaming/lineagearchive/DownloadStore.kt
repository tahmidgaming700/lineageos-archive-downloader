package com.tahmidgaming.lineagearchive

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

object DownloadStore {
    data class Item(
        val id: String,
        val filename: String,
        val device: String,
        val version: String?,
        val expectedSha256: String?,
        val expectedSize: Long?,
        val url: String,
        val status: String = "Queued",
        val verified: Boolean? = null,
        val error: String? = null
    )

    private const val PREFS = "downloads"
    private const val KEY = "items"

    fun add(context: Context, item: Item) {
        val all = items(context).filterNot { it.id == item.id } + item
        save(context, all)
    }

    fun update(context: Context, id: String, transform: (Item) -> Item) {
        save(context, items(context).map { if (it.id == id) transform(it) else it })
    }

    fun items(context: Context): List<Item> = runCatching {
        val array = JSONArray(context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, "[]"))
        List(array.length()) { i ->
            val o = array.getJSONObject(i)
            Item(
                o.getString("id"), o.getString("filename"), o.getString("device"),
                o.optString("version").takeIf { it.isNotBlank() },
                o.optString("sha256").takeIf { it.isNotBlank() },
                if (o.has("size") && !o.isNull("size")) o.getLong("size") else null,
                o.getString("url"), o.optString("status", "Queued"),
                if (o.has("verified") && !o.isNull("verified")) o.getBoolean("verified") else null,
                o.optString("error").takeIf { it.isNotBlank() }
            )
        }
    }.getOrDefault(emptyList())

    private fun save(context: Context, items: List<Item>) {
        val array = JSONArray()
        items.takeLast(50).forEach { item ->
            array.put(JSONObject().apply {
                put("id", item.id); put("filename", item.filename); put("device", item.device)
                put("version", item.version); put("sha256", item.expectedSha256); put("size", item.expectedSize)
                put("url", item.url); put("status", item.status); put("verified", item.verified); put("error", item.error)
            })
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, array.toString()).apply()
    }

    fun newId(): String = UUID.randomUUID().toString()
}
