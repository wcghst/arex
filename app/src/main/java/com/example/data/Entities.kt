package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "customer_profiles")
data class CustomerProfile(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val email: String,
    val phone: String,
    val address: String,
    val bookingDate: String = "",
    val mappingType: String = "Auto Map",
    val notes: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "service_quotes")
data class ServiceQuote(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val customerId: Int,
    val propertySizeSqFt: Int,
    val servicesSelected: String, // Comma-separated list: "Grass Cutting", "Deck Building", etc.
    val isSubscription: Boolean,
    val packageSelected: String,  // "A la Carte", "Apex Standard Package", "Apex Elite Subscription"
    val quotedPrice: Double,
    val isContractSigned: Boolean = false,
    val contractSignature: String? = null,
    val invoiceAmount: Double = 0.0,
    val invoiceStatus: String = "Unpaid", // "Unpaid", "Paid"
    val scheduledStartDate: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "lead_sequence_states")
data class LeadSequenceState(
    @PrimaryKey val customerId: Int,
    val currentStage: Int = 1, // 1: Email 1 Sent (Welcome + Booking), 2: Email 2 Sent (Quote), 3: Email 3 Sent (Follow-up), 4: Replied/Booked
    val welcomeSentAt: Long = 0,
    val quoteSentAt: Long = 0,
    val followUpSentAt: Long = 0,
    val isReplied: Boolean = false,
    val replyMessage: String? = null,
    val lastUpdated: Long = System.currentTimeMillis()
)
