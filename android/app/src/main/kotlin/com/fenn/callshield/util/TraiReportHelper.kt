package com.fenn.callshield.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Prepares a TRAI DND complaint via SMS to 1909.
 *
 * TRAI DoT complaint format: COMPLAINT <calling number> <dd/mm/yyyy>
 * Since we only store the masked label (last 4 digits), the user fills the full number.
 */
object TraiReportHelper {

    private const val TRAI_NUMBER = "1909"
    private val DATE_FORMAT = SimpleDateFormat("dd/MM/yyyy", Locale("en", "IN"))

    /**
     * Creates an ACTION_SENDTO intent that opens the SMS app with 1909 pre-filled
     * and a partially populated complaint message.
     *
     * @param maskedLabel Display label like "****1234"
     */
    fun createSmsIntent(maskedLabel: String): Intent {
        val today = DATE_FORMAT.format(Date())
        // Template: user must replace the last-4-digit hint with the full number
        val body = "COMPLAINT $maskedLabel $today"
        return Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("smsto:$TRAI_NUMBER")
            putExtra("sms_body", body)
        }
    }

    /**
     * Creates a fallback intent to call 1909 (TRAI helpline) if SMS is unavailable.
     */
    fun createCallIntent(): Intent =
        Intent(Intent.ACTION_DIAL, Uri.parse("tel:$TRAI_NUMBER"))
}
