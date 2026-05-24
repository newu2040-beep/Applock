package com.example.util

import android.content.Context
import android.graphics.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

object IntruderImageUtils {

    fun generateProceduralIntruderSelfie(context: Context, appName: String, file: File) {
        val size = 400
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint()
        
        // 1. Dark atmospheric red gradient overlay
        val grad = LinearGradient(
            0f, 0f, size.toFloat(), size.toFloat(),
            Color.parseColor("#1C0303"),
            Color.parseColor("#090101"),
            Shader.TileMode.CLAMP
        )
        paint.shader = grad
        canvas.drawRect(0f, 0f, size.toFloat(), size.toFloat(), paint)
        paint.shader = null
        
        // 2. High-Tech Cyber Digital Matrix gridlines
        paint.color = Color.parseColor("#1BFF3333")
        paint.strokeWidth = 1f
        for (i in 0..size step 32) {
            canvas.drawLine(0f, i.toFloat(), size.toFloat(), i.toFloat(), paint)
            canvas.drawLine(i.toFloat(), 0f, i.toFloat(), size.toFloat(), paint)
        }
        
        // 3. Floating biometric scan coordinates / concentric circle rings
        paint.color = Color.parseColor("#DCFF4444")
        paint.strokeWidth = 2f
        paint.style = Paint.Style.STROKE
        val centerX = size / 2f
        val centerY = size / 2f
        
        // Draw cyber scanner locks
        canvas.drawCircle(centerX, centerY, 120f, paint)
        paint.color = Color.parseColor("#55FF3333")
        canvas.drawCircle(centerX, centerY, 140f, paint)
        
        // Red scanning brackets: Top-Left, Top-Right, Bottom-Left, Bottom-Right
        paint.color = Color.parseColor("#FFFF2222")
        paint.strokeWidth = 4f
        val padding = 60f
        // Top Left
        canvas.drawLine(padding, padding, padding + 30f, padding, paint)
        canvas.drawLine(padding, padding, padding, padding + 30f, paint)
        // Top Right
        canvas.drawLine(size - padding, padding, size - padding - 30f, padding, paint)
        canvas.drawLine(size - padding, padding, size - padding, padding + 30f, paint)
        // Bottom Left
        canvas.drawLine(padding, size - padding, padding + 30f, size - padding, paint)
        canvas.drawLine(padding, size - padding, padding, size - padding - 30f, paint)
        // Bottom Right
        canvas.drawLine(size - padding, size - padding, size - padding - 30f, size - padding, paint)
        canvas.drawLine(size - padding, size - padding, size - padding, size - padding - 30f, paint)

        // 4. Draw stylized neon Silhouette icon mimicking a real spy capture
        paint.style = Paint.Style.FILL
        // Intruder skin / head glow color
        paint.color = Color.parseColor("#CDFF3333")
        canvas.drawCircle(centerX, centerY - 24f, 48f, paint) // head
        
        // Shoulders path
        val shoulderPath = Path()
        shoulderPath.moveTo(centerX - 100f, size.toFloat() - 20f)
        shoulderPath.quadTo(centerX, centerY + 50f, centerX + 100f, size.toFloat() - 20f)
        shoulderPath.lineTo(centerX + 100f, size.toFloat())
        shoulderPath.lineTo(centerX - 100f, size.toFloat())
        shoulderPath.close()
        canvas.drawPath(shoulderPath, paint)

        // Draw critical crosshair horizontal / vertical target indicators
        paint.color = Color.parseColor("#FFCC0000")
        paint.strokeWidth = 1.5f
        paint.style = Paint.Style.STROKE
        canvas.drawLine(centerX - 160f, centerY, centerX + 160f, centerY, paint)
        canvas.drawLine(centerX, centerY - 160f, centerX, centerY + 160f, paint)

        // 5. Draw cyber security logs / HUD overlays
        paint.color = Color.parseColor("#FFFF5555")
        paint.style = Paint.Style.FILL
        paint.textSize = 15f
        paint.isAntiAlias = true
        paint.typeface = Typeface.MONOSPACE
        
        canvas.drawText("ALERT: BYPASS EXPLOIT DETECTED", 20f, 32f, paint)
        canvas.drawText("SECURE-LOGS ACTIVE", 20f, 52f, paint)
        
        val dateString = SimpleDateFormat("HH:mm:ss", Locale.US).format(Date())
        canvas.drawText("TIME: $dateString", size - 140f, 32f, paint)
        
        paint.textSize = 12f
        paint.color = Color.parseColor("#FFFFAA00")
        canvas.drawText("APP VIOLATED: $appName", 20f, size - 44f, paint)
        canvas.drawText("THIEF INTRUDER CONFIRMED [100%]", 20f, size - 24f, paint)
        
        // Red flashing alert banner
        paint.color = Color.parseColor("#FFFF1111")
        paint.textSize = 28f
        paint.typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        val textWidth = paint.measureText("THIEVES ALERT")
        canvas.drawText("THIEVES ALERT", (size - textWidth) / 2f, centerY + 80f, paint)

        // 6. Write compressed bitmap safely into local Storage of filesDir
        try {
            file.outputStream().use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
