package com.example.util

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File

/** Writes user-approved academic exports to cache and shares a temporary read-only content URI. */
object AcademicFileSharing {
    fun shareFile(context: Context, file: File, mimeType: String, chooserTitle: String) {
        require(file.exists() && file.isFile) { "Export file is not available" }
        val exportRoot = File(context.cacheDir, "academic_exports").canonicalFile
        val canonicalFile = file.canonicalFile
        require(canonicalFile.path.startsWith(exportRoot.path + File.separator)) { "Only academic export files may be shared" }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            clipData = android.content.ClipData.newUri(context.contentResolver, file.name, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, chooserTitle).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    fun shareTextFile(context: Context, fileName: String, mimeType: String, content: String, chooserTitle: String) {
        require(fileName.matches(Regex("[A-Za-z0-9._-]{1,100}"))) { "Invalid export filename" }
        val directory = File(context.cacheDir, "academic_exports").apply { mkdirs() }
        val output = File(directory, fileName)
        output.writeText(content, Charsets.UTF_8)
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", output)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, fileName)
            clipData = android.content.ClipData.newUri(context.contentResolver, fileName, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, chooserTitle).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    fun saveTextFileToDownloads(context: Context, fileName: String, mimeType: String, content: String): Result<String> = runCatching {
        require(fileName.matches(Regex("[A-Za-z0-9._-]{1,100}"))) { "Invalid export filename" }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            val values = android.content.ContentValues().apply {
                put(android.provider.MediaStore.Downloads.DISPLAY_NAME, fileName)
                put(android.provider.MediaStore.Downloads.MIME_TYPE, mimeType)
                put(android.provider.MediaStore.Downloads.IS_PENDING, 1)
            }
            val resolver = context.contentResolver
            val uri = resolver.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                ?: error("Unable to create file in Downloads")
            resolver.openOutputStream(uri)?.use { it.write(content.toByteArray(Charsets.UTF_8)) }
                ?: error("Unable to write to Downloads")
            values.clear()
            values.put(android.provider.MediaStore.Downloads.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
            "Saved $fileName to Downloads"
        } else {
            @Suppress("DEPRECATION")
            val downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
            downloadsDir.mkdirs()
            val outFile = File(downloadsDir, fileName)
            outFile.writeText(content, Charsets.UTF_8)
            "Saved $fileName to Downloads"
        }
    }
}
