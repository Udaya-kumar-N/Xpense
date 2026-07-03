package com.expensemind.ai.parser

import android.content.Context
import android.net.Uri
import com.expensemind.ai.data.AppDatabase
import com.expensemind.ai.data.Transaction
import com.expensemind.ai.normalize.CategoryClassifier
import com.expensemind.ai.normalize.MerchantNormalizer
import java.util.UUID

sealed class ImportResult {
    data class Success(val count: Int, val bank: String) : ImportResult()
    data class UnrecognizedBank(val message: String) : ImportResult()
    data class Failure(val error: Throwable) : ImportResult()
}

class ImportPipeline(private val context: Context) {

    private val db = AppDatabase.getInstance(context)
    private val normalizer = MerchantNormalizer(db.merchantAliasDao())

    suspend fun importStatement(uri: Uri, password: String? = null): ImportResult {
        return try {
            // 1. PDF -> raw text
            val rawText = PdfTextExtractor.extractText(context, uri, password)

            // 2. Detect which bank this is
            val bankType = BankDetector.detect(rawText)
            val parser = ParserFactory.getParser(bankType)
                ?: return ImportResult.UnrecognizedBank(
                    "Couldn't recognize this bank's format yet. Currently supported: HDFC, SBI. " +
                    "Send Claude Code a sample statement to add support for more banks."
                )

            // 3. Parse into raw transactions
            val parsed = parser.parse(rawText)
            if (parsed.isEmpty()) {
                return ImportResult.UnrecognizedBank(
                    "Detected ${parser.bankName} but couldn't extract any transaction lines. " +
                    "The statement layout may have changed - the parser regex needs recalibration."
                )
            }

            // 4. Normalize + categorize + store
            val batchId = UUID.randomUUID().toString()
            val transactions = parsed.map { p ->
                val cleanMerchant = normalizer.normalize(p.rawDescription)
                val category = CategoryClassifier.classify(cleanMerchant, p.rawDescription)
                Transaction(
                    date = p.date,
                    amount = p.amount,
                    type = p.type,
                    merchantRaw = p.rawDescription,
                    merchantClean = cleanMerchant,
                    category = category,
                    bankSource = parser.bankName,
                    accountLast4 = p.accountLast4,
                    paymentMode = p.paymentMode,
                    rawDescription = p.rawDescription,
                    importBatchId = batchId
                )
            }
            db.transactionDao().insertAll(transactions)

            ImportResult.Success(transactions.size, parser.bankName)
        } catch (e: Exception) {
            ImportResult.Failure(e)
        }
    }
}
