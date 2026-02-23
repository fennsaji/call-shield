package com.fenn.callshield.domain.model

enum class BlockingPreset { BALANCED, AGGRESSIVE, CONTACTS_ONLY, NIGHT_GUARD, INTERNATIONAL_LOCK, CUSTOM }

enum class UnknownCallAction { ALLOW, SILENCE, REJECT }

data class AdvancedBlockingPolicy(
    val preset: BlockingPreset = BlockingPreset.BALANCED,
    // Contact policies
    val allowContactsOnly: Boolean = false,
    val silenceUnknownNumbers: Boolean = false,
    // Night Guard (Free: fixed 10PM–7AM silence only)
    val nightGuardEnabled: Boolean = false,
    val nightGuardStartHour: Int = 22,
    val nightGuardEndHour: Int = 7,
    val nightGuardAction: UnknownCallAction = UnknownCallAction.SILENCE, // Pro can set REJECT
    // Region policy
    val blockInternational: Boolean = false,
    // Escalation
    val autoEscalateEnabled: Boolean = false,
    val autoEscalateThreshold: Int = 3,
) {
    /** True if any non-default option is active beyond just the preset field. */
    fun isCustomized(): Boolean =
        allowContactsOnly || silenceUnknownNumbers || nightGuardEnabled ||
                blockInternational || autoEscalateEnabled
}

fun BlockingPreset.toDefaultPolicy(): AdvancedBlockingPolicy = when (this) {
    BlockingPreset.BALANCED -> AdvancedBlockingPolicy(preset = this)
    BlockingPreset.AGGRESSIVE -> AdvancedBlockingPolicy(
        preset = this,
        silenceUnknownNumbers = true,
        autoEscalateEnabled = true,
        autoEscalateThreshold = 2,
    )
    BlockingPreset.CONTACTS_ONLY -> AdvancedBlockingPolicy(
        preset = this,
        allowContactsOnly = true,
    )
    BlockingPreset.NIGHT_GUARD -> AdvancedBlockingPolicy(
        preset = this,
        nightGuardEnabled = true,
    )
    BlockingPreset.INTERNATIONAL_LOCK -> AdvancedBlockingPolicy(
        preset = this,
        blockInternational = true,
    )
    BlockingPreset.CUSTOM -> AdvancedBlockingPolicy(preset = this)
}
