package com.expensemind.ai.parser

enum class BankType { HDFC, SBI, ICICI, UNKNOWN }

object BankDetector {

    /**
     * Looks for distinctive header/footer strings that each bank always includes
     * in their statement PDFs. Extend this list as you add more banks/samples.
     */
    fun detect(rawText: String): BankType {
        val text = rawText.uppercase()
        return when {
            text.contains("HDFC BANK") -> BankType.HDFC
            text.contains("STATE BANK OF INDIA") || text.contains("SBI ") -> BankType.SBI
            text.contains("ICICI BANK") -> BankType.ICICI
            else -> BankType.UNKNOWN
        }
    }
}
