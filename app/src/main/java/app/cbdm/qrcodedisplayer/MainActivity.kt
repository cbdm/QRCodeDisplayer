/*
 * QR Code Displayer
 * Created by Caio (cbdm.app)
 *
 * Recreates cropped or poor quality QR codes using ML Kit and
 * generates a high-resolution version on a white background.
 */

package app.cbdm.qrcodedisplayer

import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import boofcv.android.ConvertBitmap
import boofcv.factory.fiducial.ConfigQrCode
import boofcv.factory.fiducial.FactoryFiducial
import boofcv.struct.image.GrayU8
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import zxingcpp.BarcodeReader

class MainActivity : ComponentActivity() {

    // Global state to trigger UI recomposition
    private val qrCodeData = mutableStateOf<String?>(null)
    private val statusMessage = mutableStateOf("Share an image with a QR code to this app.")
    private val decodedStage = mutableStateOf<String?>(null)

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

                val stage = decodedStage.value
                if (stage != null) {
                    // 1. State to track if the bug is open or closed
                    var isDebugExpanded by remember { mutableStateOf(false) }

                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(end = 16.dp, bottom = 64.dp)
                            // 2. Clip the shape FIRST so the clickable ripple stays inside the pill
                            .clip(RoundedCornerShape(50))
                            // 3. Toggle the state when clicked
                            .clickable { isDebugExpanded = !isDebugExpanded }
                            // 4. Subtle transparency when closed, solid when open so text is readable
                            .background(Color.LightGray.copy(alpha = if (isDebugExpanded) 0.9f else 0.4f))
                            // 5. This single line creates the smooth sliding animation!
                            .animateContentSize(
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioNoBouncy,
                                    stiffness = Spring.StiffnessLow
                                )
                            )
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.BugReport,
                                contentDescription = "Debug info",
                                tint = Color.DarkGray,
                                modifier = Modifier.size(18.dp)
                            )

                            // 6. Only draw the text if the user clicked to expand it
                            if (isDebugExpanded) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = stage,
                                    color = Color.DarkGray,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
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
                // Launch the heavy scanning process in the background
                lifecycleScope.launch {
                    decodeImageWithFallback(uri)
                }
            } else {
                statusMessage.value = "Error: No image attached."
            }
        }
    }

    private suspend fun decodeImageWithFallback(uri: Uri) {
        withContext(Dispatchers.Default) {
            val originalBitmap = try {
                contentResolver.openInputStream(uri)?.use {
                    android.graphics.BitmapFactory.decodeStream(it)
                }
            } catch (e: Exception) { null }

            if (originalBitmap == null) {
                statusMessage.value = "Could not read the image."
                return@withContext
            }

            // Create the 4 different image variations we'll try the models.
            // They only process the image if/when the variable is actually called.
            val normalImg by lazy { padBitmapWithWhite(originalBitmap) }
            val invertedImg by lazy { padBitmapWithWhite(invertBitmapColors(originalBitmap)) }
            val bwImg by lazy { padBitmapWithWhite(applyExtremeContrast(originalBitmap)) }
            val bwInvertedImg by lazy { padBitmapWithWhite(invertBitmapColors(applyExtremeContrast(originalBitmap))) }

            // ---------------------------------------------------------
            // 1: The quick checks (ZXing-C++)
            // ---------------------------------------------------------
            statusMessage.value = "Scanning..."

            val zxingReader = BarcodeReader().apply {
                options = BarcodeReader.Options(tryHarder = true)
            }

            // 1. Normal
            try {
                val res = zxingReader.read(normalImg).firstOrNull()
                if (res != null && !res.text.isNullOrEmpty()) {
                    qrCodeData.value = res.text
                    decodedStage.value = "Original QR code decoded on Stage 1 (ZXing Normal)"
                    return@withContext
                }
            } catch (e: Exception) {}

            // 2. Inverted
            try {
                val res = zxingReader.read(invertedImg).firstOrNull()
                if (res != null && !res.text.isNullOrEmpty()) {
                    qrCodeData.value = res.text
                    decodedStage.value = "Original QR code decoded on Stage 2 (ZXing Inverted)"
                    return@withContext
                }
            } catch (e: Exception) {}

            // 3. High Contrast B/W
            try {
                val res = zxingReader.read(bwImg).firstOrNull()
                if (res != null && !res.text.isNullOrEmpty()) {
                    qrCodeData.value = res.text
                    decodedStage.value = "Original QR code decoded on Stage 3 (ZXing B/W)"
                    return@withContext
                }
            } catch (e: Exception) {}

            // 4. High Contrast B/W Inverted
            try {
                val res = zxingReader.read(bwInvertedImg).firstOrNull()
                if (res != null && !res.text.isNullOrEmpty()) {
                    qrCodeData.value = res.text
                    decodedStage.value = "Original QR code decoded on Stage 4 (ZXing Inverted B/W)"
                    return@withContext
                }
            } catch (e: Exception) {}


            // ---------------------------------------------------------
            // 2: The heavy scan (BoofCV)
            // ---------------------------------------------------------
            statusMessage.value = "Trying harder..."

            val boofcvConfig = ConfigQrCode().apply {
                // 1. Checks if the QR code is mirrored/flipped
                considerTransposed = true
            }
            val boofcvDetector = FactoryFiducial.qrcode(boofcvConfig, GrayU8::class.java)

            // Helper function to keep the BoofCV boilerplate clean
            fun tryBoofCV(bitmapToTest: Bitmap): String? {
                val grayImage = GrayU8(bitmapToTest.width, bitmapToTest.height)
                ConvertBitmap.bitmapToGray(bitmapToTest, grayImage, null)
                boofcvDetector.process(grayImage)
                return if (boofcvDetector.detections.isNotEmpty()) boofcvDetector.detections[0].message else null
            }

            // 5. Normal Heavy
            try {
                val res = tryBoofCV(normalImg)
                if (!res.isNullOrEmpty()) {
                    qrCodeData.value = res
                    decodedStage.value = "Original QR code decoded on Stage 5 (BoofCV Normal)"
                    return@withContext
                }
            } catch (e: Exception) {}

            // 6. Inverted Heavy
            statusMessage.value = "Deep scanning inverted colors..."
            try {
                val res = tryBoofCV(invertedImg)
                if (!res.isNullOrEmpty()) {
                    qrCodeData.value = res
                    decodedStage.value = "Original QR code decoded on Stage 6 (BoofCV Inverted)"
                    return@withContext
                }
            } catch (e: Exception) {}

            // 7. B/W Heavy
            statusMessage.value = "Last resort: removing shadows..."
            try {
                val res = tryBoofCV(bwImg)
                if (!res.isNullOrEmpty()) {
                    qrCodeData.value = res
                    decodedStage.value = "Original QR code decoded on Stage 7 (BoofCV B/W)"
                    return@withContext
                }
            } catch (e: Exception) {}

            // 8. B/W Inverted Heavy
            try {
                val res = tryBoofCV(bwInvertedImg)
                if (!res.isNullOrEmpty()) {
                    qrCodeData.value = res
                    decodedStage.value = "Original QR code decoded on Stage 8 (BoofCV Inverted B/W)"
                    return@withContext
                }
            } catch (e: Exception) {}

            // COMPLETE FAILURE
            qrCodeData.value = null
            decodedStage.value = null
            statusMessage.value = "Unable to decode. The QR code may be too damaged."
        }
    }

    private fun padBitmapWithWhite(originalBitmap: Bitmap): Bitmap {
        // Calculate a 20% margin based on the image's original size
        val paddingX = (originalBitmap.width * 0.20).toInt()
        val paddingY = (originalBitmap.height * 0.20).toInt()
        val newWidth = originalBitmap.width + (paddingX * 2)
        val newHeight = originalBitmap.height + (paddingY * 2)

        val paddedBitmap = Bitmap.createBitmap(newWidth, newHeight, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(paddedBitmap)

        // Fill background with white, then draw the image in the center
        canvas.drawColor(android.graphics.Color.WHITE)
        canvas.drawBitmap(originalBitmap, paddingX.toFloat(), paddingY.toFloat(), null)

        return paddedBitmap
    }

    private fun invertBitmapColors(bitmap: Bitmap): Bitmap {
        val inverted = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(inverted)
        val paint = android.graphics.Paint()

        // Mathematical matrix to invert RGB values but leave Alpha (opacity) alone
        val colorMatrix = android.graphics.ColorMatrix(floatArrayOf(
            -1f,  0f,  0f,  0f, 255f, // Red
            0f, -1f,  0f,  0f, 255f, // Green
            0f,  0f, -1f,  0f, 255f, // Blue
            0f,  0f,  0f,  1f,   0f  // Alpha
        ))
        paint.colorFilter = android.graphics.ColorMatrixColorFilter(colorMatrix)
        canvas.drawBitmap(bitmap, 0f, 0f, paint)

        return inverted
    }

    private fun applyExtremeContrast(bitmap: Bitmap): Bitmap {
        val bwBitmap = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bwBitmap)
        val paint = android.graphics.Paint()

        val colorMatrix = android.graphics.ColorMatrix()
        // 1. Convert to grayscale
        colorMatrix.setSaturation(0f)

        // 2. Apply extreme contrast multiplier (pushes grays to hard black/white)
        val contrast = 10f
        val translate = -255f * (contrast - 1f) / 2f
        val contrastMatrix = android.graphics.ColorMatrix(floatArrayOf(
            contrast, 0f, 0f, 0f, translate,
            0f, contrast, 0f, 0f, translate,
            0f, 0f, contrast, 0f, translate,
            0f, 0f, 0f, 1f, 0f
        ))
        colorMatrix.postConcat(contrastMatrix)

        paint.colorFilter = android.graphics.ColorMatrixColorFilter(colorMatrix)
        canvas.drawBitmap(bitmap, 0f, 0f, paint)

        return bwBitmap
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
