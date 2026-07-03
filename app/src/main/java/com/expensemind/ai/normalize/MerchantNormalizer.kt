package com.expensemind.ai.normalize

import com.expensemind.ai.data.MerchantAliasDao

/**
 * Turns a messy raw description like:
 *   "UPI-AMZN INDIA PVT-AMZN@ICICI-1234-PAYMENT"
 * into a clean merchant name:
 *   "Amazon"
 *
 * Two layers:
 *  1. Built-in dictionary (common Indian merchants/UPI handles)
 *  2. User-defined aliases from MerchantAliasDao ("Raja" -> "ABC Builders")
 */
class MerchantNormalizer(private val aliasDao: MerchantAliasDao) {

    // Starter dictionary - extend this over time as you see more raw descriptions.
    // Key: substring to match (uppercase). Value: clean merchant name.
    private val builtInDictionary = linkedMapOf(
        "AMZN" to "Amazon",
        "AMAZON" to "Amazon",
        "FLIPKART" to "Flipkart",
        "SWIGGY" to "Swiggy",
        "ZOMATO" to "Zomato",
        "NETFLIX" to "Netflix",
        "UBER" to "Uber",
        "OLA" to "Ola",
        "MYNTRA" to "Myntra",
        "BIGBASKET" to "BigBasket",
        "IRCTC" to "IRCTC",
        "PAYTM" to "Paytm",
        "PHONEPE" to "PhonePe",
        "GOOGLEPAY" to "Google Pay",
        "AIRTEL" to "Airtel",
        "JIO" to "Jio",
        "HOTSTAR" to "Disney+ Hotstar",
        "SPOTIFY" to "Spotify"
    )

    suspend fun normalize(rawDescription: String): String {
        val upper = rawDescription.uppercase()

        // 1. Check user-defined aliases first (highest priority - user explicitly taught this)
        val words = upper.split(Regex("[\\s\\-/@]+"))
        for (word in words) {
            val alias = aliasDao.findByAlias(word.lowercase())
            if (alias != null) return alias.canonicalMerchant
        }

        // 2. Check built-in dictionary
        for ((key, cleanName) in builtInDictionary) {
            if (upper.contains(key)) return cleanName
        }

        // 3. Fallback: best-effort cleanup of the raw text
        return fallbackClean(rawDescription)
    }

    private fun fallbackClean(raw: String): String {
        return raw
            .replace(Regex("""UPI[-/]?"""), "")
            .replace(Regex("""\d{6,}"""), "") // strip long reference numbers
            .replace(Regex("""[-/@]+"""), " ")
            .trim()
            .split(" ")
            .filter { it.isNotBlank() }
            .take(3) // keep it short
            .joinToString(" ")
            .ifBlank { "Unknown Merchant" }
    }
}
