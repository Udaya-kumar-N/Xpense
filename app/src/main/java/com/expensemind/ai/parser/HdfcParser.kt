package com.expensemind.ai.parser

import com.expensemind.ai.data.TransactionType
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * IMPORTANT: HDFC statement layouts vary slightly by account type (savings vs
 * credit card) and change over time. This regex is a starting template based
 * on the common savings-account layout:
 *
 *   01/06/26  UPI-AMAZON PAY-AMZN@ICICI-1234  0000123456  1,499.00   45,231.00
 *   (date)    (narration/description)          (ref no)   (amount)   (balance)
 *
 * You WILL need to calibrate this against your own real statement PDF -
 * paste 5-10 real (redact account numbers) lines to Claude Code and it can
 * adjust this regex precisely.
 */
class HdfcParser : BankParser {
    override val bankName = "HDFC"

    private val lineRegex = Regex(
        """(\d{2}/\d{2}/\d{2})\s+(.+?)\s+(\d{6,})\s+([\d,]+\.\d{2})\s*(Cr|Dr)?\s+([\d,]+\.\d{2})"""
    )
    private val inputFormat = SimpleDateFormat("dd/MM/yy", Locale.US)
    private val outputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    override fun parse(rawText: String): List<ParsedTransaction> {
        val results = mutableListOf<ParsedTransaction>()

        rawText.lines().forEach { line ->
            val match = lineRegex.find(line) ?: return@forEach
            val (dateStr, description, _, amountStr, crDrFlag, _) = match.destructured

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
