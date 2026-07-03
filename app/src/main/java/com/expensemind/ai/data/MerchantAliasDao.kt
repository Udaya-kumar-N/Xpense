package com.expensemind.ai.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface MerchantAliasDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(alias: MerchantAlias)

    @Query("SELECT * FROM merchant_aliases WHERE alias = :alias LIMIT 1")
    suspend fun findByAlias(alias: String): MerchantAlias?

    @Query("SELECT * FROM merchant_aliases WHERE isUserDefined = 1")
    suspend fun getUserDefinedAliases(): List<MerchantAlias>
}
