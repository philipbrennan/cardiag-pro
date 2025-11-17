package com.cardiag.pro.utils

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Environment
import com.cardiag.pro.data.model.DiagnosticSession
import com.cardiag.pro.data.model.DtcCode
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Generates PDF and CSV reports for diagnostic sessions
 */
class ReportGenerator(private val context: Context) {

    companion object {
        private const val PDF_WIDTH = 612 // 8.5 inches at 72 DPI
        private const val PDF_HEIGHT = 792 // 11 inches at 72 DPI
        private const val MARGIN = 40f
        private const val LINE_HEIGHT = 20f
        private const val TITLE_SIZE = 20f
        private const val HEADER_SIZE = 16f
        private const val BODY_SIZE = 12f
        private const val SMALL_SIZE = 10f
    }

    /**
     * Generate PDF report for diagnostic session
     * @param session Diagnostic session with vehicle info
     * @param dtcCodes List of DTCs found during session
     * @return File object pointing to generated PDF, or null if generation failed
     */
    fun generatePdfReport(
        session: DiagnosticSession,
        dtcCodes: List<DtcCode>
    ): File? {
        return try {
            val timestamp = SimpleDateFormat("yyyy-MM-dd_HHmmss", Locale.US).format(Date())
            val fileName = "CarDiag_Report_${session.vin}_$timestamp.pdf"
            
            val reportsDir = File(
                context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS),
                "CarDiagReports"
            )
            if (!reportsDir.exists()) {
                reportsDir.mkdirs()
            }
            
            val file = File(reportsDir, fileName)
            
            val document = PdfDocument()
            var pageNumber = 1
            var yPosition = MARGIN
            
            // Create first page
            var pageInfo = PdfDocument.PageInfo.Builder(PDF_WIDTH.toInt(), PDF_HEIGHT.toInt(), pageNumber).create()
            var page = document.startPage(pageInfo)
            var canvas = page.canvas
            
            val paint = Paint().apply {
                color = Color.BLACK
                isAntiAlias = true
            }
            
            // Header
            paint.textSize = TITLE_SIZE
            paint.isFakeBoldText = true
            canvas.drawText("CarDiag Pro - Diagnostic Report", MARGIN, yPosition, paint)
            yPosition += LINE_HEIGHT * 2
            
            // Vehicle Information
            paint.textSize = HEADER_SIZE
            canvas.drawText("Vehicle Information", MARGIN, yPosition, paint)
            yPosition += LINE_HEIGHT
            
            paint.textSize = BODY_SIZE
            paint.isFakeBoldText = false
            
            val dateFormatter = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.US)
            val reportDate = dateFormatter.format(Date(session.timestamp))
            
            drawKeyValue(canvas, paint, "Report Date:", reportDate, MARGIN, yPosition)
            yPosition += LINE_HEIGHT
            
            drawKeyValue(canvas, paint, "VIN:", session.vin, MARGIN, yPosition)
            yPosition += LINE_HEIGHT
            
            if (!session.manufacturer.isNullOrBlank()) {
                drawKeyValue(canvas, paint, "Manufacturer:", session.manufacturer, MARGIN, yPosition)
                yPosition += LINE_HEIGHT
            }
            
            if (!session.model.isNullOrBlank()) {
                drawKeyValue(canvas, paint, "Model:", session.model, MARGIN, yPosition)
                yPosition += LINE_HEIGHT
            }
            
            drawKeyValue(canvas, paint, "Systems Scanned:", 
                if (session.systemScanned.isNullOrBlank()) "Engine" else session.systemScanned,
                MARGIN, yPosition)
            yPosition += LINE_HEIGHT * 2
            
            // DTCs Section
            paint.textSize = HEADER_SIZE
            paint.isFakeBoldText = true
            canvas.drawText("Diagnostic Trouble Codes", MARGIN, yPosition, paint)
            yPosition += LINE_HEIGHT
            
            paint.textSize = BODY_SIZE
            paint.isFakeBoldText = false
            
            if (dtcCodes.isEmpty()) {
                canvas.drawText("No trouble codes found - all systems OK", MARGIN, yPosition, paint)
                yPosition += LINE_HEIGHT
            } else {
                drawKeyValue(canvas, paint, "Total Codes Found:", dtcCodes.size.toString(), MARGIN, yPosition)
                yPosition += LINE_HEIGHT * 2
                
                // Draw each DTC
                dtcCodes.forEachIndexed { index, dtc ->
                    // Check if we need a new page
                    if (yPosition > PDF_HEIGHT - MARGIN - LINE_HEIGHT * 8) {
                        document.finishPage(page)
                        pageNumber++
                        pageInfo = PdfDocument.PageInfo.Builder(PDF_WIDTH.toInt(), PDF_HEIGHT.toInt(), pageNumber).create()
                        page = document.startPage(pageInfo)
                        canvas = page.canvas
                        yPosition = MARGIN
                    }
                    
                    // DTC Code with severity indicator
                    paint.isFakeBoldText = true
                    val codeColor = when {
                        dtc.code.startsWith("P0") -> Color.rgb(255, 152, 0) // Orange - generic
                        dtc.code.startsWith("P1") -> Color.rgb(156, 39, 176) // Purple - manufacturer
                        dtc.code.startsWith("C") -> Color.rgb(33, 150, 243) // Blue - chassis
                        dtc.code.startsWith("B") -> Color.rgb(244, 67, 54) // Red - body/airbag
                        else -> Color.BLACK
                    }
                    paint.color = codeColor
                    canvas.drawText("${index + 1}. ${dtc.code}", MARGIN, yPosition, paint)
                    yPosition += LINE_HEIGHT
                    
                    // Description
                    paint.color = Color.BLACK
                    paint.isFakeBoldText = false
                    val description = wrapText(dtc.description, PDF_WIDTH - MARGIN * 3, paint)
                    description.forEach { line ->
                        canvas.drawText(line, MARGIN + 20f, yPosition, paint)
                        yPosition += LINE_HEIGHT
                    }
                    
                    // System
                    paint.textSize = SMALL_SIZE
                    paint.color = Color.GRAY
                    canvas.drawText("System: ${dtc.system}", MARGIN + 20f, yPosition, paint)
                    yPosition += LINE_HEIGHT
                    
                    // Freeze Frame Data if available
                    val freezeFrame = session.freezeFrames?.get(dtc.code)
                    if (!freezeFrame.isNullOrBlank()) {
                        paint.color = Color.DKGRAY
                        canvas.drawText("Freeze Frame Data:", MARGIN + 20f, yPosition, paint)
                        yPosition += LINE_HEIGHT
                        
                        val freezeFrameLines = wrapText(freezeFrame, PDF_WIDTH - MARGIN * 4, paint)
                        freezeFrameLines.forEach { line ->
                            canvas.drawText(line, MARGIN + 40f, yPosition, paint)
                            yPosition += LINE_HEIGHT
                        }
                    }
                    
                    paint.textSize = BODY_SIZE
                    paint.color = Color.BLACK
                    yPosition += LINE_HEIGHT * 0.5f
                }
            }
            
            // Footer
            yPosition = PDF_HEIGHT - MARGIN
            paint.textSize = SMALL_SIZE
            paint.color = Color.GRAY
            canvas.drawText("Generated by CarDiag Pro", MARGIN, yPosition, paint)
            canvas.drawText("Page $pageNumber", PDF_WIDTH - MARGIN - 50f, yPosition, paint)
            
            document.finishPage(page)
            
            // Write to file
            FileOutputStream(file).use { outputStream ->
                document.writeTo(outputStream)
            }
            document.close()
            
            Timber.i("PDF report generated: ${file.absolutePath}")
            file
        } catch (e: Exception) {
            Timber.e(e, "Failed to generate PDF report")
            null
        }
    }

    /**
     * Generate CSV export for diagnostic session
     * @param session Diagnostic session with vehicle info
     * @param dtcCodes List of DTCs found during session
     * @return File object pointing to generated CSV, or null if generation failed
     */
    fun generateCsvExport(
        session: DiagnosticSession,
        dtcCodes: List<DtcCode>
    ): File? {
        return try {
            val timestamp = SimpleDateFormat("yyyy-MM-dd_HHmmss", Locale.US).format(Date())
            val fileName = "CarDiag_Export_${session.vin}_$timestamp.csv"
            
            val reportsDir = File(
                context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS),
                "CarDiagReports"
            )
            if (!reportsDir.exists()) {
                reportsDir.mkdirs()
            }
            
            val file = File(reportsDir, fileName)
            
            FileWriter(file).use { writer ->
                // Header row
                writer.append("VIN,Manufacturer,Model,Scan Date,System,DTC Code,Description,Freeze Frame\n")
                
                val dateFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
                val scanDate = dateFormatter.format(Date(session.timestamp))
                
                // Data rows
                dtcCodes.forEach { dtc ->
                    val freezeFrame = session.freezeFrames?.get(dtc.code) ?: ""
                    writer.append("\"${session.vin}\",")
                    writer.append("\"${session.manufacturer ?: ""}\",")
                    writer.append("\"${session.model ?: ""}\",")
                    writer.append("\"$scanDate\",")
                    writer.append("\"${dtc.system}\",")
                    writer.append("\"${dtc.code}\",")
                    writer.append("\"${dtc.description.replace("\"", "\"\"")}\",")
                    writer.append("\"${freezeFrame.replace("\"", "\"\"")}\"\n")
                }
            }
            
            Timber.i("CSV export generated: ${file.absolutePath}")
            file
        } catch (e: Exception) {
            Timber.e(e, "Failed to generate CSV export")
            null
        }
    }

    /**
     * Draw key-value pair with bold key
     */
    private fun drawKeyValue(canvas: android.graphics.Canvas, paint: Paint, key: String, value: String, x: Float, y: Float) {
        val originalBold = paint.isFakeBoldText
        paint.isFakeBoldText = true
        canvas.drawText(key, x, y, paint)
        
        paint.isFakeBoldText = false
        val keyWidth = paint.measureText(key)
        canvas.drawText(" $value", x + keyWidth, y, paint)
        
        paint.isFakeBoldText = originalBold
    }

    /**
     * Wrap text to fit within specified width
     */
    private fun wrapText(text: String, maxWidth: Float, paint: Paint): List<String> {
        val words = text.split(" ")
        val lines = mutableListOf<String>()
        var currentLine = ""
        
        words.forEach { word ->
            val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
            val width = paint.measureText(testLine)
            
            if (width > maxWidth && currentLine.isNotEmpty()) {
                lines.add(currentLine)
                currentLine = word
            } else {
                currentLine = testLine
            }
        }
        
        if (currentLine.isNotEmpty()) {
            lines.add(currentLine)
        }
        
        return lines
    }
}
