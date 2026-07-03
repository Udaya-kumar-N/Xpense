package com.expensemind.ai.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Lets the user teach the agent nicknames, e.g.:
 *   alias = "Raja"        -> canonicalMerchant = "ABC Builders"
 * Populated either from the built-in merchant dictionary (MerchantNormalizer)
 * or from the user typing "Remember that X is Y" in chat.
 */
@Entity(tableName = "merchant_aliases")
data class MerchantAlias(
    @PrimaryKey
    val alias: String,          // stored lowercase, trimmed
    val canonicalMerchant: String,
    val isUserDefined: Boolean = false
)
