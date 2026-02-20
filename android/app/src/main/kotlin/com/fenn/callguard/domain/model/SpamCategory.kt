package com.fenn.callguard.domain.model

/**
 * Canonical spam categories — must stay in sync with VALID_CATEGORIES in
 * supabase/functions/report/index.ts.
 *
 * PRD §2 Manual Report Flow categories:
 *   Telemarketing / Promotional, Loan or Financial Scam, Investment Scam,
 *   Impersonation (bank/government), Phishing, Job or Work From Home Scam, Other.
 */
enum class SpamCategory(val apiValue: String, val displayName: String) {
    TELEMARKETING("telemarketing", "Telemarketing / Promotional"),
    LOAN_SCAM("loan_scam", "Loan or Financial Scam"),
    INVESTMENT_SCAM("investment_scam", "Investment Scam"),
    IMPERSONATION("impersonation", "Impersonation (Bank / Govt)"),
    PHISHING("phishing", "Phishing"),
    JOB_SCAM("job_scam", "Job or Work From Home Scam"),
    OTHER("other", "Other"),
    ;

    companion object {
        fun fromApiValue(value: String): SpamCategory? =
            entries.firstOrNull { it.apiValue == value }
    }
}
