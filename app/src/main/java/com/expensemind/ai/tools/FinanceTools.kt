package com.expensemind.ai.tools

import com.expensemind.ai.data.AppDatabase
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * These are the "tools" the agent can call. Each one is a plain Kotlin
 * function that queries SQLite and returns a plain-language-ready result.
 * Test these directly - they should work correctly with zero AI involved.
 */
class FinanceTools(private val db: AppDatabase) {

    private val monthFormatter = DateTimeFormatter.ofPattern("yyyy-MM")

    /** getMonthlyExpense(): total debit for a given "yyyy-MM", defaults to current month */
    suspend fun getMonthlyExpense(yearMonth: String = YearMonth.now().format(monthFormatter)): Double {
        val (start, end) = monthRange(yearMonth)
        return db.transactionDao().getTotalDebitInRange(start, end) ?: 0.0
    }

    /** getCategorySummary(): spend broken down by category for a given month */
    suspend fun getCategorySummary(yearMonth: String = YearMonth.now().format(monthFormatter)): List<com.expensemind.ai.data.CategoryTotal> {
        val (start, end) = monthRange(yearMonth)
        return db.transactionDao().getCategoryBreakdown(start, end)
    }

    /** getMerchantSpend(): total spend at a specific merchant (fuzzy match) */
    suspend fun getMerchantSpend(merchantQuery: String) =
        db.transactionDao().getMerchantSpend(merchantQuery)

    /** findTransaction(): search raw + clean descriptions for a keyword (e.g. contractor name) */
    suspend fun findTransaction(query: String) =
        db.transactionDao().searchTransactions(query)

    /** compareMonths(): month-over-month diff, overall and by category */
    suspend fun compareMonths(monthA: String, monthB: String): MonthComparison {
        val totalA = getMonthlyExpense(monthA)
        val totalB = getMonthlyExpense(monthB)
        val catA = getCategorySummary(monthA).associateBy { it.category }
        val catB = getCategorySummary(monthB).associateBy { it.category }

        val allCategories = (catA.keys + catB.keys).toSet()
        val categoryDeltas = allCategories.map { cat ->
            CategoryDelta(
                category = cat,
                monthA = catA[cat]?.total ?: 0.0,
                monthB = catB[cat]?.total ?: 0.0
            )
        }.sortedByDescending { kotlin.math.abs(it.monthB - it.monthA) }

        return MonthComparison(monthA, monthB, totalA, totalB, categoryDeltas)
    }

    /** detectRecurringPayments(): merchants/amounts repeating across 3+ months = likely subscriptions */
    suspend fun detectRecurringPayments() = db.transactionDao().getPossibleRecurringPayments()

    /** calculateSavings(): credits minus debits for a given month */
    suspend fun calculateSavings(yearMonth: String = YearMonth.now().format(monthFormatter)): Double {
        val (start, end) = monthRange(yearMonth)
        val credit = db.transactionDao().getTotalCreditInRange(start, end) ?: 0.0
        val debit = db.transactionDao().getTotalDebitInRange(start, end) ?: 0.0
        return credit - debit
    }

    /** sumAcrossCategories(): e.g. total "house building" cost across Construction, Steel, Tiles, etc. */
    suspend fun sumAcrossCategories(categories: List<String>) =
        db.transactionDao().getSpendAcrossCategories(categories)

    private fun monthRange(yearMonth: String): Array<String> {
        val ym = YearMonth.parse(yearMonth, monthFormatter)
        val start = ym.atDay(1).toString()
        val end = ym.atEndOfMonth().toString()
        return arrayOf(start, end)
    }
}

data class MonthComparison(
    val monthA: String,
    val monthB: String,
    val totalA: Double,
    val totalB: Double,
    val categoryDeltas: List<CategoryDelta>
)

data class CategoryDelta(val category: String, val monthA: Double, val monthB: Double) {
    val diff: Double get() = monthB - monthA
}
