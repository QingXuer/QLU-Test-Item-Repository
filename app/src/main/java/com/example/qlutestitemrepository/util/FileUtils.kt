package com.example.qlutestitemrepository.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object FileUtils {

    fun getTestsRoot(): File {
        return File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "tests")
    }

    fun listFiles(subPath: String = ""): List<File> {
        val root = getTestsRoot()
        val targetDir = if (subPath.isEmpty()) root else File(root, subPath)
        
        if (!targetDir.exists()) {
            targetDir.mkdirs()
        }
        
        return targetDir.listFiles()?.toList()?.sortedWith(compareBy({ !it.isDirectory }, { it.name })) ?: emptyList()
    }

    fun deleteFile(file: File): Boolean {
        return try {
            if (file.isDirectory) {
                file.deleteRecursively()
            } else {
                file.delete()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun formatSize(size: Long): String {
        if (size <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (Math.log10(size.toDouble()) / Math.log10(1024.0)).toInt()
        return String.format("%.1f %s", size / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
    }

    fun formatDate(timestamp: Long): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    fun openWithOther(context: Context, file: File) {
        if (!file.exists()) {
            Toast.makeText(context, "文件不存在", Toast.LENGTH_SHORT).show()
            return
        }
        try {
            val uri = getFileUri(context, file)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, getMimeType(file))
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(intent, "用其他应用打开"))
        } catch (e: Exception) {
            Toast.makeText(context, "无法打开文件: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun exportFile(context: Context, file: File) {
        // Since the file is already in Downloads/tests, it is technically "exported" to a public directory.
        // However, "Export" might mean copying it to a specific location user chooses, or just sharing.
        // Given requirement "Export" vs "Share" usually means saving somewhere else.
        // But since we are on Android, "Export" usually implies System Picker or just Share intent.
        // The prompt also asks for "Share" in previous logic, but here asks for "Export".
        // Let's implement Export as a "Share/Send" intent but labeled generically, 
        // OR simply copy it to the root of Downloads if it's deep inside?
        // Let's use the Share intent as it's the most flexible "Export" mechanism without SAF complexity.
        // Wait, the requirement says "Option 3: Export".
        // Let's implement it as a Share intent for now, as it covers most "Export" use cases (email, drive, etc).
        shareFile(context, file, null)
    }
    
    // Modified shareFile to be more generic or support specific platforms
    fun shareFile(context: Context, file: File, platform: SharePlatform?) {
        if (!file.exists()) {
            Toast.makeText(context, "文件未找到", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val uri = getFileUri(context, file)
            val mimeType = getMimeType(file)

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            if (platform != null) {
                val packageName = when (platform) {
                    SharePlatform.QQ -> "com.tencent.mobileqq"
                    SharePlatform.WECHAT -> "com.tencent.mm"
                }
                intent.setPackage(packageName)
                try {
                    context.startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(context, "未安装该应用", Toast.LENGTH_SHORT).show()
                }
            } else {
                val chooser = Intent.createChooser(intent, "导出/分享文件")
                chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(chooser)
            }
        } catch (e: Exception) {
            Toast.makeText(context, "导出失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    // Existing helper
    fun shareFile(context: Context, fileName: String, platform: SharePlatform) {
         val file = getTestsFile(fileName)
         shareFile(context, file, platform)
    }

    fun openFile(context: Context, fileName: String, remoteUrl: String) {
        val file = getTestsFile(fileName)
        if (file.exists()) {
            try {
                val uri = getFileUri(context, file)
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, getMimeType(file))
                    flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(context, "无法打开本地文件，尝试在线预览", Toast.LENGTH_SHORT).show()
                openRemote(context, remoteUrl)
            }
        } else {
            openRemote(context, remoteUrl)
        }
    }

    private fun openRemote(context: Context, url: String) {
        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        context.startActivity(browserIntent)
    }

    suspend fun saveToUri(context: Context, url: String, uri: Uri): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    URL(url).openStream().use { inputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }

    suspend fun downloadToTests(context: Context, url: String, fileName: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val destFile = getTestsFile(fileName)
                // Ensure directory exists
                destFile.parentFile?.mkdirs()
                
                URL(url).openStream().use { input ->
                    destFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }

    fun isFileDownloaded(fileName: String): Boolean {
        return getTestsFile(fileName).exists()
    }

    fun getTestsFile(fileName: String): File {
        // Use Downloads/tests folder
        val testsDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "tests")
        return File(testsDir, fileName)
    }

    private fun getFileUri(context: Context, file: File): Uri {
        return FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
    }

    private fun getMimeType(file: File): String? {
        val extension = MimeTypeMap.getFileExtensionFromUrl(file.absolutePath) ?: "pdf"
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension) ?: "application/pdf"
    }
}

enum class SharePlatform {
    QQ, WECHAT
}
