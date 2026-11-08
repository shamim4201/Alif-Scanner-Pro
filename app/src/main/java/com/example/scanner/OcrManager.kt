package com.example.scanner

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

object OcrManager {

    private val recognizer by lazy {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    suspend fun extractTextFromBitmap(bitmap: Bitmap): String {
        return suspendCancellableCoroutine { continuation ->
            try {
                val inputImage = InputImage.fromBitmap(bitmap, 0)
                recognizer.process(inputImage)
                    .addOnSuccessListener { visionText ->
                        val result = visionText.text
                        if (result.isNotBlank()) {
                            continuation.resume(result)
                        } else {
                            continuation.resume("No text could be detected in this image. Ensure the document is well-lit and clearly focused.")
                        }
                    }
                    .addOnFailureListener { error ->
                        // Fallback message
                        continuation.resume("Text extraction encountered an issue: ${error.localizedMessage ?: "Unknown error"}. Please ensure camera lens is clean and retry.")
                    }
            } catch (e: Exception) {
                continuation.resume("OCR processing error: ${e.message}")
            }
        }
    }
}
