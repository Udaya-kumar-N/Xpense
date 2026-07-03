package com.expensemind.ai.parser

import com.expensemind.ai.data.TransactionType

/** Intermediate result before merchant-cleaning / categorization happens. */
data class ParsedTransaction(
    val date: String,          // normalized to ISO "yyyy-MM-dd"
    val amount: Double,
    val type: TransactionType,
    val rawDescription: String,
    val accountLast4: String,
    val paymentMode: String
)

interface BankParser {
    val bankName: String

    /** Parses raw PDF text into a flat list of transactions. */
    fun parse(rawText: String): List<ParsedTransaction>
}

object ParserFactory {
    fun getParser(bankType: BankType): BankParser? = when (bankType) {
        BankType.HDFC -> HdfcParser()
        BankType.SBI -> SbiParser()
        BankType.ICICI -> null // TODO: add IciciParser() once you have sample statements
        BankType.UNKNOWN -> null
    }
}
