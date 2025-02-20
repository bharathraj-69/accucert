package com.example.myrenamerexample

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.util.Log
import android.view.View
import android.widget.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.*

class MainActivity : AppCompatActivity() {

    private lateinit var filePickerLauncher: ActivityResultLauncher<String>
    private lateinit var certificatePickerLauncher: ActivityResultLauncher<String>
    private lateinit var names: List<String>
    private var templateUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        window.statusBarColor = Color.TRANSPARENT

        val proceedToSelectionBtn: Button = findViewById(R.id.proceedToSelectionBtn)
        val uploadTextFileBtn: Button = findViewById(R.id.uploadTextFileBtn)
        val uploadCertificateBtn: Button = findViewById(R.id.uploadCertificateBtn)
        //val progressText: TextView = findViewById(R.id.progressText)
        //val progressBar: ProgressBar = findViewById(R.id.progressBar)

        checkAndRequestPermissions()

        filePickerLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) {
                readTextFile(uri)
            } else {
                Toast.makeText(this, "No file selected!", Toast.LENGTH_SHORT).show()
            }
        }

        certificatePickerLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) {
                templateUri = uri
                Toast.makeText(this, "Certificate template selected!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "No certificate file selected!", Toast.LENGTH_SHORT).show()
            }
        }

        uploadTextFileBtn.setOnClickListener { filePickerLauncher.launch("text/plain") }
        uploadCertificateBtn.setOnClickListener { certificatePickerLauncher.launch("application/pdf") }

        proceedToSelectionBtn.setOnClickListener {
            if (::names.isInitialized && templateUri != null) {
                val intent = Intent(this, SelectionActivity::class.java)
                intent.putStringArrayListExtra("names", ArrayList(names))
                intent.putExtra("templateUri", templateUri.toString())
                startActivity(intent)
            } else {
                Toast.makeText(this, "Please upload a text file and select a certificate template first.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun checkAndRequestPermissions() {
        val permissions = arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        val permissionsToRequest = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsToRequest.toTypedArray(), 1001)
        }
    }

    private fun readTextFile(uri: Uri) {
        try {
            val inputStream = contentResolver.openInputStream(uri)
            val reader = BufferedReader(InputStreamReader(inputStream))

            names = reader.useLines { lines ->
                lines.filter { it.isNotBlank() }.map { it.trim() }.toList()
            }
            Toast.makeText(this, "Loaded ${names.size} names!", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e("FileReadError", "Failed to read the file", e)
            Toast.makeText(this, "Failed to read the file!", Toast.LENGTH_SHORT).show()
        }
    }
}