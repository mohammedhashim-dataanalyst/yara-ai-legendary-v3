package com.yara.ai.games
import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
class FaceEmotionHint{fun smiling(bitmap:Bitmap,onResult:(Float?)->Unit){try{val opts=FaceDetectorOptions.Builder().setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST).build();FaceDetection.getClient(opts).process(InputImage.fromBitmap(bitmap,0)).addOnSuccessListener{onResult(it.firstOrNull()?.smilingProbability)}.addOnFailureListener{onResult(null)}}catch(_:Throwable){onResult(null)}}}
