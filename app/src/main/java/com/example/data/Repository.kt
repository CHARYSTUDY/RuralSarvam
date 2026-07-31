package com.example.data

import kotlinx.coroutines.flow.Flow

class RuralRepository(private val db: AppDatabase) {

    // User Profile
    val userProfile: Flow<UserProfile?> = db.userProfileDao().getUserProfile()
    
    suspend fun getUserProfileOneShot(): UserProfile? {
        return db.userProfileDao().getUserProfileOneShot()
    }

    suspend fun saveUserProfile(profile: UserProfile) {
        db.userProfileDao().insertOrUpdateUserProfile(profile)
    }

    // Labor Jobs
    val allJobs: Flow<List<LaborJob>> = db.laborJobDao().getAllJobs()

    suspend fun addJob(job: LaborJob) {
        db.laborJobDao().insertJob(job)
    }

    suspend fun updateJob(job: LaborJob) {
        db.laborJobDao().updateJob(job)
    }

    suspend fun deleteJob(job: LaborJob) {
        db.laborJobDao().deleteJob(job)
    }

    // Equipment Rental
    val allEquipment: Flow<List<EquipmentItem>> = db.equipmentDao().getAllEquipment()

    suspend fun addEquipment(item: EquipmentItem) {
        db.equipmentDao().insertEquipment(item)
    }

    suspend fun updateEquipment(item: EquipmentItem) {
        db.equipmentDao().updateEquipment(item)
    }

    // Bookings
    val allBookings: Flow<List<EquipmentBooking>> = db.equipmentBookingDao().getAllBookings()

    suspend fun addBooking(booking: EquipmentBooking) {
        db.equipmentBookingDao().insertBooking(booking)
    }

    suspend fun updateBooking(booking: EquipmentBooking) {
        db.equipmentBookingDao().updateBooking(booking)
    }

    // Marketplace Products
    val allProducts: Flow<List<MarketplaceProduct>> = db.marketplaceProductDao().getAllProducts()

    suspend fun addProduct(product: MarketplaceProduct) {
        db.marketplaceProductDao().insertProduct(product)
    }

    suspend fun deleteProduct(product: MarketplaceProduct) {
        db.marketplaceProductDao().deleteProduct(product)
    }

    // Notices
    val allNotices: Flow<List<VillageNotice>> = db.villageNoticeDao().getAllNotices()

    suspend fun addNotice(notice: VillageNotice) {
        db.villageNoticeDao().insertNotice(notice)
    }

    suspend fun deleteNotice(notice: VillageNotice) {
        db.villageNoticeDao().deleteNotice(notice)
    }

    // Chat
    fun getChatMessages(module: String): Flow<List<ChatMessage>> {
        return db.chatMessageDao().getMessagesByModule(module)
    }

    suspend fun addChatMessage(message: ChatMessage) {
        db.chatMessageDao().insertMessage(message)
    }

    suspend fun clearChatMessages(module: String) {
        db.chatMessageDao().clearMessagesByModule(module)
    }
}
