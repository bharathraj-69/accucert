package com.example.myrenamerexample

import android.content.Context
import android.graphics.*
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.util.Log
import java.io.File

class CertificateGenerator(
    private val context: Context,
    private val templateUri: Uri,
    private val selectedArea: RectF,
    private val outputDir: File
) {

    fun generateCertificates(names: List<String>, progressCallback: (Int) -> Unit): Boolean {
        Log.d("CertificateGenerator", "Generating certificates in folder: ${outputDir.absolutePath}")

        if (!outputDir.exists() && !outputDir.mkdirs()) {
            Log.e("CertificateGenerator", "Failed to create directory: ${outputDir.absolutePath}")
            return false
        }

        try {
            val fileDescriptor = context.contentResolver.openFileDescriptor(templateUri, "r")
            fileDescriptor?.use {
                val pdfRenderer = PdfRenderer(it)
                val page = pdfRenderer.openPage(0)

                names.forEachIndexed { index, name ->
                    val bitmap = Bitmap.createBitmap(page.width, page.height, Bitmap.Config.ARGB_8888)
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT)

                    val modifiedBitmap = addTextToBitmap(bitmap, name)
                    saveBitmapAsPdf(modifiedBitmap, File(outputDir, "$name.pdf"))

                    progressCallback(index + 1)
                }

                page.close()
                pdfRenderer.close()
            }
            return true
        } catch (e: Exception) {
            Log.e("CertificateGenerator", "Error generating certificates", e)
            return false
        }
    }

    private fun addTextToBitmap(bitmap: Bitmap, name: String): Bitmap {
        val resultBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(resultBitmap)
        val paint = Paint().apply {
            color = Color.BLACK
            textSize = selectedArea.height() / 1.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }

        val xPos = selectedArea.centerX()
        val yPos = selectedArea.centerY() - ((paint.descent() + paint.ascent()) / 2)
        canvas.drawText(name, xPos, yPos, paint)

        return resultBitmap
    }

    private fun saveBitmapAsPdf(bitmap: Bitmap, file: File) {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(bitmap.width, bitmap.height, 1).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas
        canvas.drawBitmap(bitmap, 0f, 0f, null)
        document.finishPage(page)

        file.outputStream().use {
            document.writeTo(it)
        }
        document.close()
    }
}
