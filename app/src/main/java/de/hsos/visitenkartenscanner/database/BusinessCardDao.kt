package de.hsos.visitenkartenscanner.database

import androidx.room.*

@Dao
interface BusinessCardDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(businessCard: BusinessCard)

    @Update
    suspend fun update(businessCard: BusinessCard)

    @Delete
    suspend fun delete(businessCard: BusinessCard)

    @Query("SELECT * FROM business_cards ORDER BY name ASC")
    suspend fun getAll(): List<BusinessCard>

    @Query("SELECT * FROM business_cards WHERE id = :id")
    suspend fun getById(id: Int): BusinessCard?
}
