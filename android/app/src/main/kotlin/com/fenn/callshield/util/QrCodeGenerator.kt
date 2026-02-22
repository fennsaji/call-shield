package com.fenn.callshield.util

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Generates QR code bitmaps from a string using ZXing.
 * No camera required — pure encoding only.
 */
@Singleton
class QrCodeGenerator @Inject constructor() {

    /**
     * Encodes [content] as a QR code bitmap of [sizePx] × [sizePx] pixels.
     */
    fun generate(content: String, sizePx: Int = 512): Bitmap {
        val hints = mapOf(EncodeHintType.MARGIN to 1)
        val matrix = MultiFormatWriter().encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx, hints)
        return Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888).apply {
            for (x in 0 until sizePx) {
                for (y in 0 until sizePx) {
                    setPixel(x, y, if (matrix[x, y]) Color.BLACK else Color.WHITE)
                }
            }
        }
    }
}
