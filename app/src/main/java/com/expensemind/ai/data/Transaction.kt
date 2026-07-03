package com.expensemind.ai.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * One row per bank transaction, already normalized:
 * - merchantRaw: exactly what the bank PDF said (e.g. "AMZN INDIA PVT")
 * - merchantClean: normalized name after MerchantNormalizer runs (e.g. "Amazon")
 * - category: assigned by CategoryClassifier (e.g. "Shopping", "Food", "Construction")
 * - amount: always positive; direction is in `type`
 */
@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val date: String,          // ISO format: "2026-06-15"
    val amount: Double,
    val type: TransactionType, // DEBIT or CREDIT
    val merchantRaw: String,
    val merchantClean: String,
    val category: String,
    val bankSource: String,    // "HDFC", "SBI", "ICICI", etc.
    val accountLast4: String,  // last 4 digits of account/card, for multi-account support
    val paymentMode: String,   // "UPI", "NEFT", "CARD", "CASH", "OTHER"
    val rawDescription: String, // original line from the statement, kept for auditing/debug
    val importBatchId: String  // groups transactions from the same statement import
)

enum class TransactionType { DEBIT, CREDIT }
