/*
 * QR Code Displayer
 * Created by Caio (cbdm.app)
 *
 * Recreates cropped or poor quality QR codes using ML Kit and
 * generates a high-resolution version on a white background.
 */

package app.cbdm.qrcodedisplayer

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import android.content.ContentValues
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

class MainActivity : ComponentActivity() {

    // Global state to trigger UI recomposition
    private val qrCodeData = mutableStateOf<String?>(null)
    private val statusMessage = mutableStateOf("Share an image with a QR code to this app.")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Handle intent if opened directly via Share menu
        handleIntent(intent)

        setContent {
            // Turn on full brightness
            LaunchedEffect(Unit) {
                window.attributes = window.attributes.apply { screenBrightness = 1.0f }
            }

            // Pure white background taking up the full screen
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                val data = qrCodeData.value
                if (data != null) {
                    // Generate and cache the Bitmap so it doesn't redraw constantly
                    val bitmap = remember(data) { generateCleanQrCode(data) }
                    if (bitmap != null) {
                        // 1. Add state variable for the title
                        var titleText by remember { mutableStateOf("") }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {

                            // 2. Add the invisible text field above the image
                            BasicTextField(
                                value = titleText,
                                onValueChange = { titleText = it },
                                textStyle = TextStyle(
                                    color = Color.Black,
                                    fontSize = 24.sp,
                                    textAlign = TextAlign.Center
                                ),
                                decorationBox = { innerTextField ->
                                    if (titleText.isEmpty()) {
                                        Text(
                                            text = "Click here to add a title",
                                            color = Color.LightGray,
                                            fontSize = 24.sp,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                    innerTextField()
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            )

                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = "Cleaned QR Code",
                                modifier = Modifier.fillMaxWidth(0.85f)
                            )

                            Spacer(modifier = Modifier.height(32.dp))

                            // 3. Update the button to apply the title and save
                            Button(onClick = {
                                val finalBitmap = addTitleToBitmap(bitmap, titleText)
                                saveBitmapToGallery(finalBitmap, titleText)
                            }) {
                                Text("Save QR Code")
                            }
                        }
                    } else {
                        StatusText("Failed to encode QR.")
                    }
                } else {
                    StatusText(statusMessage.value)
                }
            }
        }
    }

    // Handles new intents if the app is already open in the background
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent) {
        if (intent.action == Intent.ACTION_SEND && intent.type?.startsWith("image/") == true) {
            @Suppress("DEPRECATION")
            val uri = intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
            if (uri != null) {
                statusMessage.value = "Analyzing..."
                decodeImageWithMLKit(uri)
            } else {
                statusMessage.value = "Error: No image attached."
            }
        }
    }

    private fun decodeImageWithMLKit(uri: Uri) {
        try {
            // 1. Programmatically add a white border to the shared image
            val paddedBitmap = addWhiteMarginToUri(uri)
            if (paddedBitmap == null) {
                statusMessage.value = "Could not read the image."
                return
            }

            // 2. Feed the newly padded image to ML Kit instead of the original file
            val image = InputImage.fromBitmap(paddedBitmap, 0)

            val options = BarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS)
                .build()

            val scanner = BarcodeScanning.getClient(options)

            scanner.process(image)
                .addOnSuccessListener { barcodes ->
                    if (barcodes.isNotEmpty()) {
                        qrCodeData.value = barcodes.first().rawValue
                    } else {
                        qrCodeData.value = null
                        statusMessage.value = "No code detected, even with the added margin."
                    }
                }
                .addOnFailureListener {
                    qrCodeData.value = null
                    statusMessage.value = "Failed to scan: ${it.localizedMessage}"
                }
        } catch (e: Exception) {
            statusMessage.value = "Error loading image."
        }
    }

    private fun addWhiteMarginToUri(uri: Uri): Bitmap? {
        return try {
            // Read the original cropped image from the shared URI
            val inputStream = contentResolver.openInputStream(uri)
            val originalBitmap = android.graphics.BitmapFactory.decodeStream(inputStream)
            inputStream?.close()

            if (originalBitmap == null) return null

            // Calculate a 20% margin based on the image's original size
            val paddingX = (originalBitmap.width * 0.20).toInt()
            val paddingY = (originalBitmap.height * 0.20).toInt()

            val newWidth = originalBitmap.width + (paddingX * 2)
            val newHeight = originalBitmap.height + (paddingY * 2)

            // Create a new blank image
            val paddedBitmap = Bitmap.createBitmap(newWidth, newHeight, Bitmap.Config.ARGB_8888)
            val canvas = android.graphics.Canvas(paddedBitmap)

            // Fill the background entirely with pure white
            canvas.drawColor(android.graphics.Color.WHITE)

            // Draw the original tightly-cropped QR code directly in the center
            canvas.drawBitmap(originalBitmap, paddingX.toFloat(), paddingY.toFloat(), null)

            paddedBitmap
        } catch (e: Exception) {
            null
        }
    }

    private fun generateCleanQrCode(text: String): Bitmap? {
        return try {
            val size = 1024 // High resolution for crisp rendering
            val bitMatrix = QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, size, size)
            val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565)

            for (x in 0 until size) {
                for (y in 0 until size) {
                    bitmap.setPixel(x, y, if (bitMatrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
                }
            }
            bitmap
        } catch (e: Exception) {
            null
        }
    }

    private fun addTitleToBitmap(qrBitmap: Bitmap, title: String): Bitmap {
        if (title.isBlank()) return qrBitmap

        val paint = android.graphics.Paint().apply {
            color = android.graphics.Color.BLACK
            textSize = 80f // Large text to match the 1024x1024 QR code
            textAlign = android.graphics.Paint.Align.CENTER
            isAntiAlias = true
        }

        // Calculate layout spacing
        val textPadding = 60
        val textHeight = paint.descent() - paint.ascent()
        val extraHeight = (textHeight + textPadding * 2).toInt()

        // Create a taller bitmap to fit the text
        val newBitmap = Bitmap.createBitmap(
            qrBitmap.width,
            qrBitmap.height + extraHeight,
            Bitmap.Config.ARGB_8888
        )

        val canvas = android.graphics.Canvas(newBitmap)
        canvas.drawColor(android.graphics.Color.WHITE)

        // Draw the text centered at the top
        val xPos = canvas.width / 2f
        val yPos = textPadding - paint.ascent()
        canvas.drawText(title, xPos, yPos, paint)

        // Draw the QR code directly below the text
        canvas.drawBitmap(qrBitmap, 0f, extraHeight.toFloat(), null)

        return newBitmap
    }

    private fun saveBitmapToGallery(bitmap: Bitmap, title: String) {
        // Strip out weird characters to make a safe file name
        val safeTitle = title.replace(Regex("[^a-zA-Z0-9]"), "_").take(15)
        val filename = if (safeTitle.isNotBlank()) {
            "QRCode_${safeTitle}_${System.currentTimeMillis()}.png"
        } else {
            "QRCode_${System.currentTimeMillis()}.png"
        }

        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
        }

        val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

        try {
            if (uri != null) {
                contentResolver.openOutputStream(uri)?.use { outputStream ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                    Toast.makeText(this, "Saved to Gallery!", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Failed to create file.", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Error saving: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    @Composable
    private fun StatusText(text: String) {
        Text(
            text = text,
            color = Color.Black,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(32.dp)
        )
    }
}
