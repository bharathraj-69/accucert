package com.example.myrenamerexample

import android.app.ProgressDialog
import android.graphics.Bitmap
import android.graphics.RectF
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.*
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.io.File

class SelectionActivity : AppCompatActivity() {

    private lateinit var generateCertificatesBtn: Button
    private lateinit var selectionView: SelectionView
    private var templateUri: Uri? = null
    private var pdfRenderer: PdfRenderer? = null
    private var names: List<String> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_selection)

        generateCertificatesBtn = findViewById(R.id.generateCertificatesBtn)
        selectionView = findViewById(R.id.selectionView)

        templateUri = intent.getStringExtra("templateUri")?.let { Uri.parse(it) }
        names = intent.getStringArrayListExtra("names") ?: emptyList()

        loadPdfPreview(templateUri)

        generateCertificatesBtn.setOnClickListener {
            val selectedArea = selectionView.getSelectedArea()
            if (selectedArea != null && selectedArea.width() > 0 && selectedArea.height() > 0) {
                generateCertificates(selectedArea)
            } else {
                Toast.makeText(this, "Please select a valid area on the certificate.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadPdfPreview(uri: Uri?) {
        uri ?: return
        var fileDescriptor: ParcelFileDescriptor? = null
        try {
            fileDescriptor = contentResolver.openFileDescriptor(uri, "r")
            fileDescriptor?.let {
                pdfRenderer = PdfRenderer(it)
                if (pdfRenderer!!.pageCount > 0) {
                    renderPageToImage()
                } else {
                    Toast.makeText(this, "PDF has no pages", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Error loading PDF preview: ${e.message}", Toast.LENGTH_SHORT).show()
        } finally {
            fileDescriptor?.close()
        }
    }

    private fun renderPageToImage() {
        pdfRenderer?.let {
            val page = it.openPage(0)
            val bitmap = Bitmap.createBitmap(page.width, page.height, Bitmap.Config.ARGB_8888)
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

            selectionView.setBaseImage(bitmap)
            page.close()
        }
    }

    private fun generateCertificates(selectedArea: RectF) {
        if (templateUri == null) {
            Toast.makeText(this, "Template not selected!", Toast.LENGTH_SHORT).show()
            return
        }

        val rectF = RectF(
            selectedArea.left,
            selectedArea.top,
            selectedArea.right,
            selectedArea.bottom
        )

        // **Fix: Proper Folder Creation**
        val baseDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)

        val certificatesDir = File(baseDir, "My Certificates")
        if (!certificatesDir.exists()) certificatesDir.mkdirs()

        var folderNumber = 1
        var outputDir: File
        do {
            outputDir = File(certificatesDir, "Certificates_$folderNumber")
            folderNumber++
        } while (outputDir.exists())

        if (!outputDir.mkdirs()) {
            Toast.makeText(this, "Failed to create folder!", Toast.LENGTH_SHORT).show()
            return
        }

        val progressDialog = ProgressDialog(this).apply {
            setProgressStyle(ProgressDialog.STYLE_HORIZONTAL)
            setMessage("Generating certificates...")
            max = names.size
            progress = 0
            setCancelable(false)
            show()
        }

        Thread {
            val generator = CertificateGenerator(this, templateUri!!, rectF, outputDir)
            val success = generator.generateCertificates(names) { progress ->
                runOnUiThread {
                    progressDialog.progress = progress
                }
            }

            runOnUiThread {
                progressDialog.dismiss()
                Toast.makeText(
                    this,
                    if (success) "Certificates saved in: ${outputDir.absolutePath}" else "Error generating certificates.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }.start()
    }
}
