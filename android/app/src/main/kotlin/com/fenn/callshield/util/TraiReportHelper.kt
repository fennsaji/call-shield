package com.fenn.callshield.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Prepares TRAI DND and complaint SMS intents to 1909.
 *
 * Complaint format (confirmed from 1909 reply):
 *   "<description>, <phone number>, <dd/mm/yy>"
 *   e.g. "Received unsolicited spam call, 9876543210, 22/02/26"
 *
 * DND commands (confirmed from actual 1909 reply):
 *   Block all     : FULLY BLOCK  (or START 1,2,3... for all)
 *   Block promo   : BLOCK PROMO
 *   Block category: BLOCK 1,2,3  (or START 1,2,3)
 *   Unblock all   : UNBLOCK ALL
 *   Unblock category: UNBLOCK 91,92,93  (or STOP 91,92,93)
 *     — unblock codes are 9x: 91=Banking, 92=RealEstate, etc.
 *
 * Status check via SMS is NOT confirmed. Use 1909 IVR call instead.
 */
object TraiReportHelper {

    private const val TRAI_NUMBER = "1909"
    private val DATE_FORMAT = SimpleDateFormat("dd/MM/yy", Locale("en", "IN"))

    /**
     * Creates an SMS complaint to 1909.
     *
     * If [body] is a complete pre-built message, it is used as-is.
     * Otherwise the TRAI-confirmed format is applied:
     * "Received unsolicited spam call, <body>, <dd/mm/yy>"
     *
     * @param body Full SMS body OR caller label (auto-wrapped if it looks like a label).
     */
    fun createSmsIntent(body: String): Intent {
        val smsBody = if (body.contains(' ') || body.length > 30) {
            // Already a full description — use as-is
            body
        } else {
            // Short label (e.g. "+91****3210") — wrap in standard format
            val today = DATE_FORMAT.format(Date())
            "Received unsolicited spam call, $body, $today"
        }
        return smsIntent(smsBody)
    }

    /** SMS "FULLY BLOCK" — blocks all promotional calls and SMS. */
    fun createDndActivateIntent(): Intent = smsIntent("FULLY BLOCK")

    /** SMS "BLOCK PROMO" — blocks only promotional; OTPs/bank alerts still delivered. */
    fun createDndPromoIntent(): Intent = smsIntent("BLOCK PROMO")

    /**
     * SMS "BLOCK 1,4" — blocks all categories except the listed ones.
     * @param categories Non-empty list of TRAI category codes (1–7).
     */
    fun createDndPartialIntent(categories: List<Int>): Intent =
        smsIntent("BLOCK ${categories.sorted().joinToString(",")}")

    /** SMS "UNBLOCK ALL" — deactivates DND entirely (confirmed from 1909 reply). */
    fun createDndDeactivateIntent(): Intent = smsIntent("UNBLOCK ALL")

    /**
     * Creates an SMS DND intent with any command body.
     * Use this with operator-specific commands from [DndOperator].
     */
    fun createDndCommandIntent(command: String): Intent = smsIntent(command)

    /**
     * Dials 1909 (TRAI helpline, toll-free).
     * Use for status check — STATUS via SMS is not confirmed.
     */
    fun createCallIntent(): Intent =
        Intent(Intent.ACTION_DIAL).apply {
            data = Uri.fromParts("tel", TRAI_NUMBER, null)
        }

    private fun smsIntent(body: String): Intent =
        Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("smsto:$TRAI_NUMBER")
            putExtra("sms_body", body)
        }
}
