package com.fenn.callshield.util

/**
 * Indian mobile operator enum for DND SMS command selection.
 *
 * Operators use different command syntax to interact with TRAI's 1909 DND service.
 *
 * Key differences (confirmed by user testing on Jio + Airtel):
 *   - Jio full block: START 0 (FULLY BLOCK does NOT work on Jio)
 *   - Jio partial block: BLOCK 1,4 or START 1,4 both work; BLOCK PROMO works
 *   - Jio deactivate: UNBLOCK ALL confirmed working
 *   - Airtel full block: FULLY BLOCK confirmed working
 *   - Vi full block: FULLY BLOCK confirmed working
 *   - BSNL: legacy START syntax; STOP DND to deactivate
 */
enum class DndOperator(val displayName: String) {
    JIO("Jio"),
    AIRTEL("Airtel"),
    VI("Vi"),
    BSNL("BSNL"),
    OTHER("Other");

    /** SMS command to activate Full DND (blocks all promotional). */
    fun fullBlockCommand(): String = when (this) {
        AIRTEL, VI, OTHER -> "FULLY BLOCK"
        JIO, BSNL -> "START 0"
    }

    /**
     * SMS command to block specific TRAI categories.
     * @param categories Non-empty sorted list of TRAI category codes (1–8).
     */
    fun partialBlockCommand(categories: List<Int>): String {
        val sorted = categories.sorted().joinToString(",")
        return when (this) {
            JIO, AIRTEL, VI, OTHER -> "BLOCK $sorted"
            BSNL -> "START $sorted"
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
