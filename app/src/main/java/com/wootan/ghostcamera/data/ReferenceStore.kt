package com.wootan.ghostcamera.data

import android.content.Context
import android.net.Uri
import org.json.JSONArray
import java.io.File
import java.util.UUID

data class ReferencePhoto(
    val id: String,
    val file: File,
)

class ReferenceStore(private val context: Context) {
    private val directory = File(context.filesDir, "reference_photos")
    private val preferences = context.getSharedPreferences("reference_store", Context.MODE_PRIVATE)

    fun load(): List<ReferencePhoto> {
        directory.mkdirs()
        val order = readOrder().filter { File(directory, it).isFile }
        if (order.isNotEmpty()) {
            if (order != readOrder()) saveOrder(order)
            return order.map(::toPhoto)
        }

        val recovered = directory.listFiles()
            ?.filter(File::isFile)
            ?.sortedBy(File::lastModified)
            ?.map { it.name }
            .orEmpty()
        if (recovered.isNotEmpty()) saveOrder(recovered)
        return recovered.map(::toPhoto)
    }

    fun add(uris: List<Uri>): List<ReferencePhoto> {
        directory.mkdirs()
        val order = readOrder().toMutableList()

        uris.forEach { uri ->
            val fileName = "reference_${UUID.randomUUID()}.img"
            val target = File(directory, fileName)
            runCatching {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    target.outputStream().buffered().use { output -> input.copyTo(output) }
                } ?: error("Unable to open selected image")
                order += fileName
            }.onFailure {
                target.delete()
            }
        }

        saveOrder(order)
        return order.filter { File(directory, it).isFile }.map(::toPhoto)
    }

    fun delete(photo: ReferencePhoto): List<ReferencePhoto> {
        photo.file.delete()
        val order = readOrder().filterNot { it == photo.file.name }
        saveOrder(order)
        return order.filter { File(directory, it).isFile }.map(::toPhoto)
    }

    fun move(fromIndex: Int, toIndex: Int): List<ReferencePhoto> {
        val order = readOrder().toMutableList()
        if (fromIndex !in order.indices || toIndex !in order.indices || fromIndex == toIndex) {
            return load()
        }
        val moved = order.removeAt(fromIndex)
        order.add(toIndex, moved)
        saveOrder(order)
        return order.map(::toPhoto)
    }

    private fun toPhoto(fileName: String) = ReferencePhoto(
        id = fileName,
        file = File(directory, fileName),
    )

    private fun readOrder(): List<String> = runCatching {
        val json = JSONArray(preferences.getString(ORDER_KEY, "[]"))
        buildList {
            repeat(json.length()) { index -> add(json.getString(index)) }
        }
    }.getOrDefault(emptyList())

    private fun saveOrder(order: List<String>) {
        preferences.edit().putString(ORDER_KEY, JSONArray(order).toString()).apply()
    }

    private companion object {
        const val ORDER_KEY = "photo_order"
    }
}
