package com.example.data

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

object FirebaseSyncManager {
    private const val TAG = "FirebaseSyncManager"
    var isFirebaseAvailable: Boolean = false
        private set

    fun initialize(context: Context) {
        try {
            val apps = FirebaseApp.getApps(context)
            isFirebaseAvailable = if (apps.isEmpty()) {
                val app = FirebaseApp.initializeApp(context)
                app != null
            } else {
                true
            }
            Log.d(TAG, "Firebase initialized. Available: $isFirebaseAvailable")
        } catch (e: Exception) {
            Log.e(TAG, "Firebase initialization failed. Falling back to sandbox mode.", e)
            isFirebaseAvailable = false
        }
    }

    fun syncCustomerProfile(profile: CustomerProfile) {
        if (!isFirebaseAvailable) return
        try {
            val db = FirebaseFirestore.getInstance()
            val data = mapOf(
                "id" to profile.id,
                "name" to profile.name,
                "email" to profile.email,
                "phone" to profile.phone,
                "address" to profile.address,
                "bookingDate" to profile.bookingDate,
                "mappingType" to profile.mappingType,
                "notes" to profile.notes,
                "timestamp" to profile.timestamp
            )
            db.collection("customers").document(profile.id.toString())
                .set(data, SetOptions.merge())
                .addOnSuccessListener { Log.d(TAG, "Customer ${profile.id} synced to Firestore") }
                .addOnFailureListener { e -> Log.e(TAG, "Error syncing customer ${profile.id}", e) }
        } catch (e: Exception) {
            Log.e(TAG, "Firestore sync customer profile failed", e)
        }
    }

    fun syncServiceQuote(quote: ServiceQuote) {
        if (!isFirebaseAvailable) return
        try {
            val db = FirebaseFirestore.getInstance()
            val data = mapOf(
                "id" to quote.id,
                "customerId" to quote.customerId,
                "propertySizeSqFt" to quote.propertySizeSqFt,
                "servicesSelected" to quote.servicesSelected,
                "isSubscription" to quote.isSubscription,
                "packageSelected" to quote.packageSelected,
                "quotedPrice" to quote.quotedPrice,
                "isContractSigned" to quote.isContractSigned,
                "contractSignature" to quote.contractSignature,
                "invoiceAmount" to quote.invoiceAmount,
                "invoiceStatus" to quote.invoiceStatus,
                "scheduledStartDate" to quote.scheduledStartDate,
                "timestamp" to quote.timestamp
            )
            db.collection("quotes").document(quote.id.toString())
                .set(data, SetOptions.merge())
                .addOnSuccessListener { Log.d(TAG, "Quote for customer ${quote.customerId} synced") }
                .addOnFailureListener { e -> Log.e(TAG, "Error syncing quote", e) }
        } catch (e: Exception) {
            Log.e(TAG, "Firestore sync service quote failed", e)
        }
    }

    fun syncLeadSequenceState(state: LeadSequenceState) {
        if (!isFirebaseAvailable) return
        try {
            val db = FirebaseFirestore.getInstance()
            val data = mapOf(
                "customerId" to state.customerId,
                "currentStage" to state.currentStage,
                "welcomeSentAt" to state.welcomeSentAt,
                "quoteSentAt" to state.quoteSentAt,
                "followUpSentAt" to state.followUpSentAt,
                "isReplied" to state.isReplied,
                "replyMessage" to state.replyMessage,
                "lastUpdated" to state.lastUpdated
            )
            db.collection("lead_states").document(state.customerId.toString())
                .set(data, SetOptions.merge())
                .addOnSuccessListener { Log.d(TAG, "Lead sequence for customer ${state.customerId} synced") }
                .addOnFailureListener { e -> Log.e(TAG, "Error syncing lead sequence", e) }
        } catch (e: Exception) {
            Log.e(TAG, "Firestore sync lead sequence failed", e)
        }
    }

    fun deleteCustomerFromFirestore(customerId: Int) {
        if (!isFirebaseAvailable) return
        try {
            val db = FirebaseFirestore.getInstance()
            db.collection("customers").document(customerId.toString()).delete()
            db.collection("quotes").document(customerId.toString()).delete()
            db.collection("lead_states").document(customerId.toString()).delete()
            Log.d(TAG, "Deleted customer $customerId data from Firestore")
        } catch (e: Exception) {
            Log.e(TAG, "Firestore delete failed", e)
        }
    }
}
