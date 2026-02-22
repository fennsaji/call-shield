package com.fenn.callshield.util

/**
 * Indian mobile operator enum for DND SMS command selection.
 *
 * Operators use different command syntax to interact with TRAI's 1909 DND service.
 *
 * Key differences (confirmed by user testing on Jio + Airtel, research for others):
 *   - Jio uses 2025 TRAI amendment syntax: FULLY BLOCK / BLOCK 1,4 / UNBLOCK ALL
 *   - Airtel uses legacy TRAI syntax: START 0 / START 1,4 / UNBLOCK ALL
 *   - Vi appears to follow Jio/2025 syntax for full block, legacy START for categories
 *   - BSNL uses legacy START syntax and has its own STOP DND deactivation command
 *
 * BLOCK PROMO is a 2025 TRAI standard command and is used universally (not operator-specific).
 */
enum class DndOperator(val displayName: String) {
    JIO("Jio"),
    AIRTEL("Airtel"),
    VI("Vi"),
    BSNL("BSNL"),
    OTHER("Other");

    /** SMS command to activate Full DND (blocks all promotional). */
    fun fullBlockCommand(): String = when (this) {
        JIO, VI, OTHER -> "FULLY BLOCK"
        AIRTEL, BSNL -> "START 0"
    }

    /**
     * SMS command to block specific TRAI categories.
     * @param categories Non-empty sorted list of TRAI category codes (1–8).
     */
    fun partialBlockCommand(categories: List<Int>): String {
        val sorted = categories.sorted().joinToString(",")
        return when (this) {
            JIO, OTHER -> "BLOCK $sorted"
            AIRTEL, VI, BSNL -> "START $sorted"
        }
    }

    /**
     * SMS command to deactivate DND entirely.
     * UNBLOCK ALL is confirmed by actual 1909 reply for most operators.
     * BSNL documents STOP DND as its deactivation command.
     */
    fun deactivateCommand(): String = when (this) {
        JIO, AIRTEL, VI, OTHER -> "UNBLOCK ALL"
        BSNL -> "STOP DND"
    }

    companion object {
        fun fromName(name: String): DndOperator =
            entries.firstOrNull { it.name == name } ?: OTHER
    }
}
