package com.expensemind.ai.normalize

/**
 * Rule-based categorization (fast, transparent, no AI needed for the MVP).
 * Matches against BOTH the clean merchant name and the raw description,
 * so "Steel Traders Pvt Ltd" still lands in Construction even though it's
 * not in the merchant dictionary.
 */
object CategoryClassifier {

    private val rules: List<Pair<String, List<String>>> = listOf(
        "Food & Dining" to listOf("SWIGGY", "ZOMATO", "RESTAURANT", "CAFE", "FOOD", "DOMINO", "MCDONALD", "KFC", "PIZZA"),
        "Groceries" to listOf("BIGBASKET", "GROFERS", "BLINKIT", "ZEPTO", "DMART", "GROCERY", "SUPERMARKET"),
        "Shopping" to listOf("AMAZON", "FLIPKART", "MYNTRA", "AJIO", "MALL", "RETAIL"),
        "Transport" to listOf("UBER", "OLA", "RAPIDO", "PETROL", "FUEL", "IRCTC", "METRO", "PARKING", "TOLL"),
        "Utilities" to listOf("ELECTRICITY", "BESCOM", "TNEB", "WATER BOARD", "GAS", "BROADBAND", "AIRTEL", "JIO", "WIFI"),
        "Subscriptions" to listOf("NETFLIX", "SPOTIFY", "HOTSTAR", "PRIME", "YOUTUBE PREMIUM", "APPLE.COM", "GOOGLE PLAY"),
        "Medical" to listOf("HOSPITAL", "PHARMACY", "APOLLO", "MEDPLUS", "CLINIC", "DIAGNOSTIC", "MEDICAL"),
        "Construction" to listOf("CEMENT", "STEEL", "TILES", "GRANITE", "HARDWARE", "BUILDERS", "CONTRACTOR", "PAINT", "PLUMB", "ELECTRICIAN", "SAND", "BRICK"),
        "Education" to listOf("SCHOOL", "COLLEGE", "TUITION", "COURSE", "UDEMY", "COACHING"),
        "Travel" to listOf("MAKEMYTRIP", "GOIBIBO", "AIRLINE", "INDIGO", "HOTEL", "AIRBNB", "OYO"),
        "Loan/EMI" to listOf("EMI", "LOAN", "NBFC"),
        "Insurance" to listOf("INSURANCE", "LIC", "POLICY PREMIUM"),
        "Investments" to listOf("MUTUAL FUND", "ZERODHA", "GROWW", "SIP", "STOCK", "PPF", "NPS")
    )

    fun classify(merchantClean: String, rawDescription: String): String {
        val haystack = (merchantClean + " " + rawDescription).uppercase()
        for ((category, keywords) in rules) {
            if (keywords.any { haystack.contains(it) }) return category
        }
        return "Uncategorized"
    }
}
