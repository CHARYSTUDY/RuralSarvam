package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface UserProfileDao {
    @Query("SELECT * FROM user_profile WHERE id = 1 LIMIT 1")
    fun getUserProfile(): Flow<UserProfile?>

    @Query("SELECT * FROM user_profile WHERE id = 1 LIMIT 1")
    suspend fun getUserProfileOneShot(): UserProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateUserProfile(profile: UserProfile)
}

@Dao
interface LaborJobDao {
    @Query("SELECT * FROM labor_jobs ORDER BY id DESC")
    fun getAllJobs(): Flow<List<LaborJob>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertJob(job: LaborJob)

    @Update
    suspend fun updateJob(job: LaborJob)

    @Delete
    suspend fun deleteJob(job: LaborJob)
}

@Dao
interface EquipmentDao {
    @Query("SELECT * FROM equipment_items ORDER BY id DESC")
    fun getAllEquipment(): Flow<List<EquipmentItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEquipment(item: EquipmentItem)

    @Update
    suspend fun updateEquipment(item: EquipmentItem)
}

@Dao
interface EquipmentBookingDao {
    @Query("SELECT * FROM equipment_bookings ORDER BY id DESC")
    fun getAllBookings(): Flow<List<EquipmentBooking>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBooking(booking: EquipmentBooking)

    @Update
    suspend fun updateBooking(booking: EquipmentBooking)
}

@Dao
interface MarketplaceProductDao {
    @Query("SELECT * FROM marketplace_products ORDER BY id DESC")
    fun getAllProducts(): Flow<List<MarketplaceProduct>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: MarketplaceProduct)

    @Delete
    suspend fun deleteProduct(product: MarketplaceProduct)
}

@Dao
interface VillageNoticeDao {
    @Query("SELECT * FROM village_notices ORDER BY timestamp DESC")
    fun getAllNotices(): Flow<List<VillageNotice>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotice(notice: VillageNotice)

    @Delete
    suspend fun deleteNotice(notice: VillageNotice)
}

@Dao
interface ChatMessageDao {
    @Query("SELECT * FROM chat_messages WHERE module = :module ORDER BY timestamp ASC")
    fun getMessagesByModule(module: String): Flow<List<ChatMessage>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessage)

    @Query("DELETE FROM chat_messages WHERE module = :module")
    suspend fun clearMessagesByModule(module: String)
}
