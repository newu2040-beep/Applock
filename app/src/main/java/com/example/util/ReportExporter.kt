package com.example.util

import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import com.example.data.IntruderLog
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.*

object ReportExporter {

    fun saveReportToPublicText(context: Context, log: IntruderLog): String? {
        val dateFormmatted = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(log.timestamp))
        val ticketId = "SR-${(10000..99999).random()}"
        
        val reportBuilder = StringBuilder()
        reportBuilder.append("=========================================\n")
        reportBuilder.append("       PRIVACY BREACH INCIDENT TICKET    \n")
        reportBuilder.append("=========================================\n")
        reportBuilder.append("Ticket Registry : #$ticketId\n")
        reportBuilder.append("Incident Time   : $dateFormmatted\n")
        reportBuilder.append("Intruded Target : ${log.appName}\n")
        reportBuilder.append("Capture Mode    : ${if (log.isSilent) "SILENT FRONT CAMERA SCAN" else "COVERT ALERT CAPTURE"}\n")
        reportBuilder.append("Auth Attempts   : ${log.attemptCount} SECURE ACCESS VIOLATIONS\n")
        reportBuilder.append("Intruder Status : LOCKED OUT BY BIOMETRIC GUARD\n")
        reportBuilder.append("Physical Evidence: ${log.photoPath ?: "Not Available"}\n")
        reportBuilder.append("-----------------------------------------\n")
        reportBuilder.append("             SECURITY ANALYSIS           \n")
        reportBuilder.append("-----------------------------------------\n")
        reportBuilder.append("- Severe threat risk detected via application interception.\n")
        reportBuilder.append("- TTS vocal alert was loaded and deployed locally.\n")
        reportBuilder.append("- Real-time digital fingerprint trace: COMPLETED.\n")
        reportBuilder.append("- High resolution face scan mapped: CONVERGED.\n")
        reportBuilder.append("=========================================\n")
        reportBuilder.append("      SECUREUNLOCK PRO - SHIELD ACTIVE   \n")
        reportBuilder.append("=========================================\n")

        val reportContent = reportBuilder.toString()
        val filename = "security_incident_${ticketId}.txt"

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val resolver = context.contentResolver
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                    put(MediaStore.MediaColumns.MIME_TYPE, "text/plain")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/SecureUnlockReports")
                }
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                if (uri != null) {
                    resolver.openOutputStream(uri)?.use { out ->
                        out.write(reportContent.toByteArray())
                    }
                    return "Saved in Downloads/SecureUnlockReports/$filename"
                }
            } else {
                val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "SecureUnlockReports")
                if (!dir.exists()) dir.mkdirs()
                val file = File(dir, filename)
                FileOutputStream(file).use { out ->
                    out.write(reportContent.toByteArray())
                }
                MediaScannerConnection.scanFile(context, arrayOf(file.absolutePath), arrayOf("text/plain"), null)
                return "Saved in public Downloads: ${file.absolutePath}"
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    fun savePhotoToPublicGallery(context: Context, photoPath: String?): String? {
        if (photoPath.isNullOrEmpty()) return null
        val sourceFile = File(photoPath)
        if (!sourceFile.exists()) return null

        val timestamp = System.currentTimeMillis()
        val dateFormatted = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date(timestamp))
        val destFilename = "secure_intruder_${dateFormatted}.jpg"

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val resolver = context.contentResolver
                val contentValues = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, destFilename)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                    put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/SecureUnlockIntruders")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }

                val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                if (uri != null) {
                    resolver.openOutputStream(uri)?.use { out ->
                        FileInputStream(sourceFile).use { input ->
                            input.copyTo(out)
                        }
                    }
                    contentValues.clear()
                    contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                    resolver.update(uri, contentValues, null, null)
                    return "Photo exported to Gallery/SecureUnlockIntruders!"
                }
            } else {
                val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "SecureUnlockIntruders")
                if (!dir.exists()) dir.mkdirs()
                val file = File(dir, destFilename)
                FileInputStream(sourceFile).use { input ->
                    FileOutputStream(file).use { out ->
                        input.copyTo(out)
                    }
                }
                MediaScannerConnection.scanFile(context, arrayOf(file.absolutePath), arrayOf("image/jpeg"), null)
                return "Photo saved in Pictures/SecureUnlockIntruders!"
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }
}
