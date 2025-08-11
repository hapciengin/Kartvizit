package com.qrtasima.util

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

object FileStorageHelper {

    fun saveFile(context: Context, sourceUri: Uri): String? {
        return try {
            context.contentResolver.openInputStream(sourceUri)?.use { inputStream: InputStream ->
                val extension = context.contentResolver.getType(sourceUri)?.split('/')?.last() ?: "tmp"
                val fileName = "${UUID.randomUUID()}.$extension"
                val privateFile = File(context.filesDir, fileName)

                privateFile.outputStream().use { outputStream: OutputStream ->
                    inputStream.copyTo(outputStream)
                }
                fileName
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun deleteFile(context: Context, fileName: String?) {
        if (fileName.isNullOrEmpty()) return
        try {
            val file = File(context.filesDir, fileName)
            if (file.exists()) {
                file.delete()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}