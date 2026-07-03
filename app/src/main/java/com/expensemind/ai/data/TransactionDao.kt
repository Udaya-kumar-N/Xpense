package com.expensemind.ai.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface TransactionDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(transactions: List<Transaction>)

    @Query("SELECT * FROM transactions ORDER BY date DESC")
    suspend fun getAll(): List<Transaction>

    @Query(
        """SELECT * FROM transactions
           WHERE date BETWEEN :startDate AND :endDate
           ORDER BY date DESC"""
    )
    suspend fun getBetweenDates(startDate: String, endDate: String): List<Transaction>

    @Query(
        """SELECT category, SUM(amount) as total, COUNT(*) as count
           FROM transactions
           WHERE type = 'DEBIT' AND date BETWEEN :startDate AND :endDate
           GROUP BY category
           ORDER BY total DESC"""
    )
    suspend fun getCategoryBreakdown(startDate: String, endDate: String): List<CategoryTotal>

    @Query(
        """SELECT merchantClean, SUM(amount) as total, COUNT(*) as count
           FROM transactions
           WHERE type = 'DEBIT' AND merchantClean LIKE '%' || :merchantQuery || '%'
           GROUP BY merchantClean
           ORDER BY total DESC"""
    )
    suspend fun getMerchantSpend(merchantQuery: String): List<MerchantTotal>

    @Query(
        """SELECT * FROM transactions
           WHERE merchantClean LIKE '%' || :query || '%'
              OR rawDescription LIKE '%' || :query || '%'
           ORDER BY date DESC"""
    )
    suspend fun searchTransactions(query: String): List<Transaction>

    @Query(
        """SELECT category, SUM(amount) as total, COUNT(*) as count
           FROM transactions
           WHERE type = 'DEBIT' AND category IN (:categories)
           GROUP BY category"""
    )
    suspend fun getSpendAcrossCategories(categories: List<String>): List<CategoryTotal>

    @Query(
        """SELECT SUM(amount) FROM transactions
           WHERE type = 'DEBIT' AND date BETWEEN :startDate AND :endDate"""
    )
    suspend fun getTotalDebitInRange(startDate: String, endDate: String): Double?

    @Query(
        """SELECT SUM(amount) FROM transactions
           WHERE type = 'CREDIT' AND date BETWEEN :startDate AND :endDate"""
    )
    suspend fun getTotalCreditInRange(startDate: String, endDate: String): Double?

    // Recurring payment detection: same merchant + similar amount appearing in 3+ different months
    @Query(
        """SELECT merchantClean, amount, COUNT(DISTINCT substr(date,1,7)) as monthCount
           FROM transactions
           WHERE type = 'DEBIT'
           GROUP BY merchantClean, amount
           HAVING monthCount >= 3
           ORDER BY monthCount DESC"""
    )
    suspend fun getPossibleRecurringPayments(): List<RecurringCandidate>
}

data class CategoryTotal(val category: String, val total: Double, val count: Int)
data class MerchantTotal(val merchantClean: String, val total: Double, val count: Int)
data class RecurringCandidate(val merchantClean: String, val amount: Double, val monthCount: Int)
