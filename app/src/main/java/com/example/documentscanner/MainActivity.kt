package com.example.documentscanner

import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import coil.compose.AsyncImage
import com.example.documentscanner.ui.theme.DocumentScannerTheme
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.RESULT_FORMAT_JPEG
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.RESULT_FORMAT_PDF
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.SCANNER_MODE_FULL
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val options = GmsDocumentScannerOptions.Builder()
            .setGalleryImportAllowed(false)
            .setPageLimit(20)
            .setResultFormats(RESULT_FORMAT_JPEG, RESULT_FORMAT_PDF)
            .setScannerMode(SCANNER_MODE_FULL)
            .build()

        val scanner = GmsDocumentScanning.getClient(options)

        setContent {
            DocumentScannerTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {

                    var imgUri by remember {
                        mutableStateOf<List<Uri>>(emptyList())
                    }

                    val scannerLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.StartIntentSenderForResult(),
                        onResult = { result ->
                            run {
                                if (result.resultCode == RESULT_OK) {
                                    val res =
                                        GmsDocumentScanningResult.fromActivityResultIntent(result.data)
                                    res?.pages?.let { pages ->
                                        for (page in pages) {
                                            val imageUri = pages.map {
                                                it.imageUri
                                            }
                                            imgUri = imageUri

                                            //if you want you can save this imgUri in your photos as JPEG.
                                        }
                                    }
                                    res?.pdf?.let { pdf ->



                                        val pdfFile = FileOutputStream(makeAppFolder())
                                        val pdfUri = pdf.uri
                                        contentResolver.openInputStream(pdfUri)?.use {
                                            it.copyTo(pdfFile)
                                            Toast.makeText(
                                                baseContext,
                                                "File has been downloaded in your Downloads folder",
                                                Toast.LENGTH_LONG
                                            ).show()
                                        }

                                    }
                                }
                            }

                        })

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState(), enabled = true),

                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        imgUri.forEach { uri ->
                            AsyncImage(
                                model = uri,
                                contentDescription = null,
                                contentScale = ContentScale.FillWidth,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        Button(onClick = {
                            scanner.getStartScanIntent(this@MainActivity)
                                .addOnSuccessListener { intentSender ->
                                    scannerLauncher.launch(
                                        IntentSenderRequest.Builder(intentSender).build()
                                    )
                                }
                                .addOnFailureListener {
                                    Log.e("Scanner Error", it.message.toString())
                                    Toast.makeText(
                                        baseContext,
                                        "Something went wrong!!!",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                        }) {
                            Text(text = "Scan Document")
                        }
                    }


                }

            }
        }
    }

    private fun makeAppFolder(): File {
        val time = Calendar.getInstance().time
        val currentTimeFormat = SimpleDateFormat("LLLL_dd_KK_mm_aaa", Locale.getDefault())
        val fileName = currentTimeFormat.format(time)
        val folder: File =
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        return File(folder, "/$fileName.pdf")
    }
}


@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    DocumentScannerTheme {

    }
}