package com.expensemind.ai.parser

import com.expensemind.ai.data.TransactionType
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Template for SBI's typical statement format:
 *   1 Jun 2026   TRANSFER TO 1234567890 UPI/CR/123456/AMAZON   1,499.00 (Dr)   45,231.00
 *
 * Like HdfcParser, this needs calibration against a real SBI PDF sample.
 */
class SbiParser : BankParser {
    override val bankName = "SBI"

    private val lineRegex = Regex(
        """(\d{1,2}\s+\w{3}\s+\d{4})\s+(.+?)\s+([\d,]+\.\d{2})\s*\((Dr|Cr)\)\s+([\d,]+\.\d{2})"""
    )
    private val inputFormat = SimpleDateFormat("d MMM yyyy", Locale.US)
    private val outputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    override fun parse(rawText: String): List<ParsedTransaction> {
        val results = mutableListOf<ParsedTransaction>()

        rawText.lines().forEach { line ->
            val match = lineRegex.find(line) ?: return@forEach
            val (dateStr, description, amountStr, crDrFlag, _) = match.destructured

            val amount = amountStr.replace(",", "").toDoubleOrNull() ?: return@forEach
            val isCredit = crDrFlag.equals("Cr", ignoreCase = true)
            val isoDate = runCatching {
                outputFormat.format(inputFormat.parse(dateStr)!!)
            }.getOrNull() ?: return@forEach

            results.add(
                ParsedTransaction(
                    date = isoDate,
                    amount = amount,
                    type = if (isCredit) TransactionType.CREDIT else TransactionType.DEBIT,
                    rawDescription = description.trim(),
                    accountLast4 = extractLast4(description),
                    paymentMode = detectPaymentMode(description)
                )
            )
        }
        return results
    }

    private fun extractLast4(description: String): String {
        val digits = Regex("""\d{4}""").findAll(description).lastOrNull()?.value
        return digits ?: "0000"
    }

    private fun detectPaymentMode(description: String): String = when {
        description.contains("UPI", true) -> "UPI"
        description.contains("NEFT", true) -> "NEFT"
        description.contains("IMPS", true) -> "IMPS"
        description.contains("ATM", true) -> "ATM"
        description.contains("POS", true) || description.contains("CARD", true) -> "CARD"
        else -> "OTHER"
    }
}
