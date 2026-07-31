package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        UserProfile::class,
        LaborJob::class,
        EquipmentItem::class,
        EquipmentBooking::class,
        MarketplaceProduct::class,
        VillageNotice::class,
        ChatMessage::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userProfileDao(): UserProfileDao
    abstract fun laborJobDao(): LaborJobDao
    abstract fun equipmentDao(): EquipmentDao
    abstract fun equipmentBookingDao(): EquipmentBookingDao
    abstract fun marketplaceProductDao(): MarketplaceProductDao
    abstract fun villageNoticeDao(): VillageNoticeDao
    abstract fun chatMessageDao(): ChatMessageDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "ruralos_database"
                )
                    .addCallback(AppDatabaseCallback(scope))
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class AppDatabaseCallback(
        private val scope: CoroutineScope
    ) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                scope.launch(Dispatchers.IO) {
                    populateDatabase(database)
                }
            }
        }

        suspend fun populateDatabase(db: AppDatabase) {
            // Seed User Profile
            db.userProfileDao().insertOrUpdateUserProfile(
                UserProfile(
                    id = 1,
                    name = "Rajesh Kumar",
                    mobileNumber = "9876543210",
                    village = "Pipili",
                    district = "Puri",
                    state = "Odisha",
                    preferredLanguage = "English",
                    occupation = "Farmer",
                    role = "Farmer",
                    profilePhoto = "",
                    gpsLocation = "20.1165° N, 85.8340° E",
                    earnings = 12500.0
                )
            )

            // Seed Notice Board
            db.villageNoticeDao().insertNotice(
                VillageNotice(
                    title = "Scheduled Water Supply Maintenance",
                    category = "Water Supply",
                    content = "Water supply will be suspended tomorrow (Tuesday) from 9:00 AM to 12:00 PM for pipeline repairs near Panchayat Bhavan.",
                    publisher = "Water Department Authority"
                )
            )
            db.villageNoticeDao().insertNotice(
                VillageNotice(
                    title = "Free Vaccination & Health Camp",
                    category = "Health Camp",
                    content = "A free veterinary and child healthcare camp will be held at the Primary School campus this Saturday. Timings: 10:00 AM to 4:00 PM.",
                    publisher = "Primary Health Centre"
                )
            )
            db.villageNoticeDao().insertNotice(
                VillageNotice(
                    title = "High Winds & Rainfall Alert",
                    category = "Weather Alert",
                    content = "Local weather department predicts strong winds (up to 40 km/h) and moderate rainfall. Farmers are advised to secure harvested crops.",
                    publisher = "Indian Meteorological Dept"
                )
            )
            db.villageNoticeDao().insertNotice(
                VillageNotice(
                    title = "Subsidized Organic Fertilizer Distribution",
                    category = "Village Meeting",
                    content = "Panchayat will distribute subsidized organic bio-fertilizers. Registration starts tomorrow morning at the cooperative office. Please bring your Aadhaar.",
                    publisher = "Gram Panchayat Office"
                )
            )

            // Seed Labor Jobs
            db.laborJobDao().insertJob(
                LaborJob(
                    title = "Paddy Sowing Helpers Needed",
                    farmerName = "Suresh Mohanty",
                    workType = "Sowing",
                    date = "2026-07-15",
                    time = "08:00 AM - 05:00 PM",
                    numWorkersNeeded = 5,
                    dailyWage = 450.0,
                    location = "East Field, Pipili"
                )
            )
            db.laborJobDao().insertJob(
                LaborJob(
                    title = "Tomato Harvesting Team",
                    farmerName = "Ramesh Sahu",
                    workType = "Harvesting",
                    date = "2026-07-18",
                    time = "07:00 AM - 03:00 PM",
                    numWorkersNeeded = 3,
                    dailyWage = 400.0,
                    location = "Sahu Farms, Sector 3"
                )
            )

            // Seed Equipment Items
            db.equipmentDao().insertEquipment(
                EquipmentItem(
                    name = "Mahindra Yuvo Tractor",
                    type = "Tractor",
                    ownerName = "Jagannath Das",
                    dailyRate = 1200.0,
                    availability = true,
                    description = "Heavy duty 45 HP tractor with rotavator attachment. Perfect for deep tilling."
                )
            )
            db.equipmentDao().insertEquipment(
                EquipmentItem(
                    name = "AgriCopter Drone Sprayer",
                    type = "Drone",
                    ownerName = "HighTech Rural Co",
                    dailyRate = 2500.0,
                    availability = true,
                    description = "10L payload fertilizer and pesticide drone. Covers 1 acre in 15 minutes."
                )
            )
            db.equipmentDao().insertEquipment(
                EquipmentItem(
                    name = "Class Crop Harvester",
                    type = "Harvester",
                    ownerName = "Panchayat Cooperative",
                    dailyRate = 3500.0,
                    availability = true,
                    description = "Combine Harvester. Available for paddy and wheat harvesting under authority subsidy."
                )
            )
            db.equipmentDao().insertEquipment(
                EquipmentItem(
                    name = "Rotary Rotavator 6-feet",
                    type = "Rotavator",
                    ownerName = "Madan Mohan",
                    dailyRate = 600.0,
                    availability = true,
                    description = "6 feet rotavator suitable for wet and dry soil preparation."
                )
            )

            // Seed Marketplace Products
            db.marketplaceProductDao().insertProduct(
                MarketplaceProduct(
                    name = "Organic Basmati Paddy (Super Quality)",
                    category = "Crops",
                    quantity = "500 Kg",
                    price = 45.0, // per kg
                    sellerName = "Rajesh Kumar",
                    sellerContact = "9876543210",
                    description = "Cultivated using pure organic manures and bio-pesticides. High aroma, long grains."
                )
            )
            db.marketplaceProductDao().insertProduct(
                MarketplaceProduct(
                    name = "Fresh Country Eggs",
                    category = "Poultry",
                    quantity = "120 Units",
                    price = 6.5, // per piece
                    sellerName = "Gita Pradhan",
                    sellerContact = "9123456789",
                    description = "Pure free-range country chicken eggs. High in protein, delivered fresh daily."
                )
            )
            db.marketplaceProductDao().insertProduct(
                MarketplaceProduct(
                    name = "Pure Desi Cow Ghee",
                    category = "Dairy",
                    quantity = "25 Litres",
                    price = 650.0, // per litre
                    sellerName = "Hari Milk Dairy",
                    sellerContact = "9988776655",
                    description = "Made traditional Bilona method from fresh A2 cow milk. Nutritious and highly aromatic."
                )
            )
        }
    }
}
