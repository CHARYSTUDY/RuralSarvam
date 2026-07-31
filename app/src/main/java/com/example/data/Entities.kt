package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "user_profile")
data class UserProfile(
    @PrimaryKey val id: Int = 1,
    val name: String = "Rajesh Kumar",
    val mobileNumber: String = "9876543210",
    val village: String = "Pipili",
    val district: String = "Puri",
    val state: String = "Odisha",
    val preferredLanguage: String = "Hindi",
    val occupation: String = "Farmer",
    val role: String = "Farmer", // Farmer, Laborer, Equipment Owner, Panchayat/Authority, Healthcare Worker, Admin
    val profilePhoto: String = "",
    val gpsLocation: String = "20.1165° N, 85.8340° E",
    val earnings: Double = 0.0
) : Serializable

@Entity(tableName = "labor_jobs")
data class LaborJob(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val farmerName: String,
    val workType: String, // Sowing, Harvesting, Weeding, Spraying, Packaging
    val date: String,
    val time: String,
    val numWorkersNeeded: Int,
    val dailyWage: Double,
    val location: String,
    val status: String = "OPEN", // OPEN, FILLED, COMPLETED
    val workersApplied: String = "" // Comma-separated names of workers who accepted
) : Serializable

@Entity(tableName = "equipment_items")
data class EquipmentItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val type: String, // Tractor, Drone, Harvester, Rotavator, Seed Drill, Sprayer, Water Tanker, Cultivator
    val ownerName: String,
    val dailyRate: Double,
    val availability: Boolean = true,
    val description: String = "",
    val imageRes: String = ""
) : Serializable

@Entity(tableName = "equipment_bookings")
data class EquipmentBooking(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val equipmentId: Int,
    val equipmentName: String,
    val farmerName: String,
    val startDate: String,
    val durationDays: Int,
    val totalCost: Double,
    val status: String = "PENDING" // PENDING, APPROVED, REJECTED, COMPLETED
) : Serializable

@Entity(tableName = "marketplace_products")
data class MarketplaceProduct(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val category: String, // Crops, Vegetables, Fruits, Dairy, Poultry, Organic
    val quantity: String,
    val price: Double,
    val sellerName: String,
    val sellerContact: String,
    val description: String = "",
    val imageRes: String = ""
) : Serializable

@Entity(tableName = "village_notices")
data class VillageNotice(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val category: String, // Water Supply, Electricity Outage, Village Meeting, Health Camp, School Announcement, Weather Alert
    val content: String,
    val publisher: String = "Gram Panchayat",
    val timestamp: Long = System.currentTimeMillis()
) : Serializable

@Entity(tableName = "chat_messages")
data class ChatMessage(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val sender: String, // "user" or "ai"
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val module: String // "agri", "health", "offline"
) : Serializable
