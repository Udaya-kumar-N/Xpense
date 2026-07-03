package com.expensemind.ai.agent

import com.expensemind.ai.tools.FinanceTools
import java.text.NumberFormat
import java.time.YearMonth
import java.util.Locale

/**
 * MVP AGENT: rule-based intent matching + templated responses.
 * This works TODAY with zero AI model needed, so you have a usable app
 * immediately. It also gives Claude Code something correct to fall back
 * on if the LLM produces a bad tool call.
 *
 * UPGRADE PATH: once you plug in a local LLM (see README "Adding the local
 * LLM"), replace `route()` with a call that:
 *   1. Sends the user message + tool list to the LLM
 *   2. LLM returns which tool + params to call (function calling / JSON mode)
 *   3. You call the real FinanceTools function
 *   4. LLM turns the raw numbers into the natural-language reply
 * The FinanceTools class doesn't change either way - only how you decide
 * which function to call and how you phrase the answer.
 */
class AgentController(private val tools: FinanceTools) {

    private val inr = NumberFormat.getCurrencyInstance(Locale("en", "IN"))

    suspend fun handleMessage(userMessage: String): String {
        val msg = userMessage.lowercase().trim()

        return when {
            msg.contains("this month") && (msg.contains("spend") || msg.contains("spent")) ->
                monthlyExpenseReply(YearMonth.now().toString())

            msg.contains("category") || msg.contains("breakdown") ->
                categoryBreakdownReply(YearMonth.now().toString())

            msg.contains("compare") && msg.contains("month") ->
                compareMonthsReply()

            msg.contains("subscription") || msg.contains("recurring") ->
                recurringPaymentsReply()

            msg.contains("save") || msg.contains("savings") ->
                savingsReply(YearMonth.now().toString())

            msg.startsWith("remember that") || msg.startsWith("remember ") ->
                "Got it — alias-saving isn't wired up in this screen yet. " +
                "Hook this branch to MerchantAliasDao.upsert() to store: \"$userMessage\""

            else ->
                findMerchantOrKeywordReply(userMessage)
        }
    }

    private suspend fun monthlyExpenseReply(yearMonth: String): String {
        val total = tools.getMonthlyExpense(yearMonth)
        return "You've spent ${inr.format(total)} so far this month."
    }

    private suspend fun categoryBreakdownReply(yearMonth: String): String {
        val breakdown = tools.getCategorySummary(yearMonth)
        if (breakdown.isEmpty()) return "No transactions found for this month yet — import a statement first."
        val lines = breakdown.take(6).joinToString("\n") { "• ${it.category}: ${inr.format(it.total)}" }
        return "Here's your spending by category this month:\n$lines"
    }

    private suspend fun compareMonthsReply(): String {
        val current = YearMonth.now()
        val previous = current.minusMonths(1)
        val comparison = tools.compareMonths(previous.toString(), current.toString())
        val diff = comparison.totalB - comparison.totalA
        val direction = if (diff >= 0) "increased" else "decreased"
        val topMovers = comparison.categoryDeltas.take(3).joinToString("\n") {
            val d = if (it.diff >= 0) "+" else ""
            "• ${it.category}: $d${inr.format(it.diff)}"
        }
        return "Your spending $direction by ${inr.format(kotlin.math.abs(diff))} vs last month.\n" +
                "Biggest changes:\n$topMovers"
    }

    private suspend fun recurringPaymentsReply(): String {
        val recurring = tools.detectRecurringPayments()
        if (recurring.isEmpty()) return "No recurring payments detected yet — need a few months of data."
        val lines = recurring.take(10).joinToString("\n") {
            "• ${it.merchantClean}: ${inr.format(it.amount)} (seen in ${it.monthCount} months)"
        }
        return "These look like recurring payments/subscriptions:\n$lines"
    }

    private suspend fun savingsReply(yearMonth: String): String {
        val savings = tools.calculateSavings(yearMonth)
        return if (savings >= 0) "You've saved ${inr.format(savings)} this month (income minus spending)."
        else "You've spent ${inr.format(-savings)} more than you received this month."
    }

    private suspend fun findMerchantOrKeywordReply(userMessage: String): String {
        val results = tools.findTransaction(userMessage)
        if (results.isEmpty()) {
            return "I couldn't find anything matching \"$userMessage\". Try asking about a merchant name, " +
                    "\"this month\", \"category breakdown\", or \"compare months\"."
        }
        val total = results.sumOf { it.amount }
        val lines = results.take(5).joinToString("\n") { "• ${it.date}: ${inr.format(it.amount)} — ${it.merchantClean}" }
        return "Found ${results.size} matching transaction(s), totaling ${inr.format(total)}:\n$lines"
    }
}
