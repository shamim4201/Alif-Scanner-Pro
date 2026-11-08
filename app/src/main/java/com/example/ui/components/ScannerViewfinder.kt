package com.example.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.scanner.OcrManager
import com.example.scanner.QrBarcodeImageAnalyzer
import com.example.ui.theme.ScannerCyanAccent
import com.example.viewmodel.ScannerMode
import java.io.File
import java.util.concurrent.Executors

@Composable
fun ScannerViewfinder(
    mode: ScannerMode,
    onCodeScanned: (rawValue: String, format: String) -> Unit,
    onDocumentCaptured: (Bitmap) -> Unit,
    onOcrCaptured: (Bitmap) -> Unit,
    onIdCardCaptured: (Bitmap) -> Unit = {},
    idCardFront: Bitmap? = null,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var camera by remember { mutableStateOf<Camera?>(null) }
    var isFlashOn by remember { mutableStateOf(false) }
    var isBackCamera by remember { mutableStateOf(true) }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var isCapturing by remember { mutableStateOf(false) }

    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    // Gallery picker launcher
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    val source = ImageDecoder.createSource(context.contentResolver, uri)
                    ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                        decoder.isMutableRequired = true
                    }
                } else {
                    @Suppress("DEPRECATION")
                    MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                }

                when (mode) {
                    ScannerMode.QR, ScannerMode.BARCODE, ScannerMode.BATCH -> {
                        // Extract barcode/QR from image
                        val inputImage = com.google.mlkit.vision.common.InputImage.fromBitmap(bitmap, 0)
                        val scanner = com.google.mlkit.vision.barcode.BarcodeScanning.getClient()
                        scanner.process(inputImage).addOnSuccessListener { barcodes ->
                            if (barcodes.isNotEmpty()) {
                                val b = barcodes.first()
                                onCodeScanned(b.rawValue ?: b.displayValue ?: "", "IMAGE_IMPORT")
                            } else {
                                android.widget.Toast.makeText(context, "No QR or barcode detected in image", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                    ScannerMode.DOCUMENT -> {
                        onDocumentCaptured(bitmap)
                    }
                    ScannerMode.ID_CARD -> {
                        onIdCardCaptured(bitmap)
                    }
                    ScannerMode.OCR -> {
                        onOcrCaptured(bitmap)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        val screenWidth = maxWidth
        val screenHeight = maxHeight

        // Camera Preview
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx).apply {
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                }
                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build().also {
                        it.surfaceProvider = previewView.surfaceProvider
                    }

                    val capture = ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .build()
                    imageCapture = capture

                    val analyzer = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                        .also {
                            it.setAnalyzer(
                                cameraExecutor,
                                QrBarcodeImageAnalyzer { raw, format ->
                                    onCodeScanned(raw, format)
                                }
                            )
                        }

                    val selector = if (isBackCamera) CameraSelector.DEFAULT_BACK_CAMERA else CameraSelector.DEFAULT_FRONT_CAMERA

                    try {
                        cameraProvider.unbindAll()
                        camera = cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            selector,
                            preview,
                            capture,
                            analyzer
                        )
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }, ContextCompat.getMainExecutor(ctx))

                previewView
            },
            modifier = Modifier.fillMaxSize()
        )

        // Overlay & Scanner Frame
        val isCard = mode == ScannerMode.ID_CARD
        val isDocOrOcr = mode == ScannerMode.DOCUMENT || mode == ScannerMode.OCR || isCard
        val frameWidth = if (isCard) screenWidth * 0.88f else if (isDocOrOcr) screenWidth * 0.85f else screenWidth * 0.75f
        val frameHeight = if (isCard) (screenWidth * 0.88f) * 0.63f else if (isDocOrOcr) screenHeight * 0.60f else screenWidth * 0.75f

        // Animated scan line
        val infiniteTransition = rememberInfiniteTransition(label = "laser")
        val laserProgress by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 2200, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "laser_pos"
        )

        Canvas(modifier = Modifier.fillMaxSize()) {
            val left = (size.width - frameWidth.toPx()) / 2f
            val top = (size.height - frameHeight.toPx()) / 2.3f
            val right = left + frameWidth.toPx()
            val bottom = top + frameHeight.toPx()

            // Draw dark vignette around the frame
            drawRect(
                color = Color(0x99000000),
                size = Size(size.width, top)
            )
            drawRect(
                color = Color(0x99000000),
                topLeft = Offset(0f, bottom),
                size = Size(size.width, size.height - bottom)
            )
            drawRect(
                color = Color(0x99000000),
                topLeft = Offset(0f, top),
                size = Size(left, frameHeight.toPx())
            )
            drawRect(
                color = Color(0x99000000),
                topLeft = Offset(right, top),
                size = Size(size.width - right, frameHeight.toPx())
            )

            // Draw targeting frame
            drawRoundRect(
                color = Color.White.copy(alpha = 0.4f),
                topLeft = Offset(left, top),
                size = Size(frameWidth.toPx(), frameHeight.toPx()),
                cornerRadius = CornerRadius(24f, 24f),
                style = Stroke(width = 3f)
            )

            // Draw bright corner accents
            val cornerLen = 36f
            val cornerStroke = 7f
            val accentColor = ScannerCyanAccent

            // Top Left
            drawLine(accentColor, Offset(left - 2, top + cornerLen), Offset(left - 2, top), cornerStroke)
            drawLine(accentColor, Offset(left - 2, top - 2), Offset(left + cornerLen, top - 2), cornerStroke)

            // Top Right
            drawLine(accentColor, Offset(right + 2, top + cornerLen), Offset(right + 2, top), cornerStroke)
            drawLine(accentColor, Offset(right + 2, top - 2), Offset(right - cornerLen, top - 2), cornerStroke)

            // Bottom Left
            drawLine(accentColor, Offset(left - 2, bottom - cornerLen), Offset(left - 2, bottom), cornerStroke)
            drawLine(accentColor, Offset(left - 2, bottom + 2), Offset(left + cornerLen, bottom + 2), cornerStroke)

            // Bottom Right
            drawLine(accentColor, Offset(right + 2, bottom - cornerLen), Offset(right + 2, bottom), cornerStroke)
            drawLine(accentColor, Offset(right + 2, bottom + 2), Offset(right - cornerLen, bottom + 2), cornerStroke)

            // Laser beam
            val laserY = top + (bottom - top) * laserProgress
            drawLine(
                brush = Brush.horizontalGradient(
                    listOf(
                        Color.Transparent,
                        ScannerCyanAccent.copy(alpha = 0.8f),
                        Color.White,
                        ScannerCyanAccent.copy(alpha = 0.8f),
                        Color.Transparent
                    )
                ),
                start = Offset(left + 8, laserY),
                end = Offset(right - 8, laserY),
                strokeWidth = 5f
            )
        }

        // Top Controls Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 40.dp, start = 16.dp, end = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onClose,
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                    .testTag("scanner_btn_back")
            ) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Close", tint = Color.White)
            }

            // Mode indicator pill
            Surface(
                color = Color.Black.copy(alpha = 0.6f),
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
            ) {
                Text(
                    text = when (mode) {
                        ScannerMode.QR -> "QR Code Scanner"
                        ScannerMode.BARCODE -> "Barcode Scanner"
                        ScannerMode.DOCUMENT -> "Document Scanner"
                        ScannerMode.ID_CARD -> if (idCardFront == null) "ID Card: Front Side" else "ID Card: Back Side"
                        ScannerMode.OCR -> "Text OCR Scanner"
                        ScannerMode.BATCH -> "Batch Mode Active"
                    },
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Flashlight toggle
                IconButton(
                    onClick = {
                        isFlashOn = !isFlashOn
                        camera?.cameraControl?.enableTorch(isFlashOn)
                    },
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                        .testTag("scanner_btn_flash")
                ) {
                    Icon(
                        imageVector = if (isFlashOn) Icons.Filled.FlashOn else Icons.Filled.FlashOff,
                        contentDescription = "Flashlight",
                        tint = if (isFlashOn) ScannerCyanAccent else Color.White
                    )
                }

                // Import from gallery
                IconButton(
                    onClick = {
                        galleryLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                        .testTag("scanner_btn_gallery")
                ) {
                    Icon(Icons.Filled.Image, contentDescription = "Import image", tint = Color.White)
                }
            }
        }

        // Shutter Button (For Document & OCR manual snap)
        if (isDocOrOcr) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 48.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = when (mode) {
                        ScannerMode.ID_CARD -> if (idCardFront == null) "Align FRONT of card in frame and snap" else "Front saved! Now align BACK of card"
                        ScannerMode.OCR -> "Align text inside frame and snap"
                        else -> "Align page inside frame and snap"
                    },
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(76.dp)
                ) {
                    if (isCapturing) {
                        CircularProgressIndicator(color = ScannerCyanAccent, strokeWidth = 3.dp)
                    } else {
                        IconButton(
                            onClick = {
                                val capture = imageCapture ?: return@IconButton
                                isCapturing = true
                                val photoFile = File.createTempFile("snap_", ".jpg", context.cacheDir)
                                val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

                                capture.takePicture(
                                    outputOptions,
                                    ContextCompat.getMainExecutor(context),
                                    object : ImageCapture.OnImageSavedCallback {
                                        override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                                            isCapturing = false
                                            val bitmap = BitmapFactory.decodeFile(photoFile.absolutePath)
                                            if (bitmap != null) {
                                                when (mode) {
                                                    ScannerMode.DOCUMENT -> onDocumentCaptured(bitmap)
                                                    ScannerMode.ID_CARD -> onIdCardCaptured(bitmap)
                                                    else -> onOcrCaptured(bitmap)
                                                }
                                            }
                                        }

                                        override fun onError(exception: ImageCaptureException) {
                                            isCapturing = false
                                            exception.printStackTrace()
                                        }
                                    }
                                )
                            },
                            modifier = Modifier
                                .size(72.dp)
                                .background(Color.White, CircleShape)
                                .border(4.dp, ScannerCyanAccent, CircleShape)
                                .testTag("scanner_shutter_btn")
                        ) {
                            Icon(
                                Icons.Filled.CameraAlt,
                                contentDescription = "Capture Document",
                                tint = Color.Black,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
