package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {
    // --- CustomerProfile queries ---
    @Query("SELECT * FROM customer_profiles ORDER BY timestamp DESC")
    fun getAllCustomerProfiles(): Flow<List<CustomerProfile>>

    @Query("SELECT * FROM customer_profiles WHERE id = :id LIMIT 1")
    suspend fun getCustomerProfileById(id: Int): CustomerProfile?

    @Query("SELECT * FROM customer_profiles WHERE email = :email LIMIT 1")
    suspend fun getCustomerProfileByEmail(email: String): CustomerProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomerProfile(profile: CustomerProfile): Long

    @Update
    suspend fun updateCustomerProfile(profile: CustomerProfile)

    @Query("DELETE FROM customer_profiles WHERE id = :id")
    suspend fun deleteCustomerProfileById(id: Int)

    // --- ServiceQuote queries ---
    @Query("SELECT * FROM service_quotes WHERE customerId = :customerId LIMIT 1")
    fun getServiceQuoteForCustomer(customerId: Int): Flow<ServiceQuote?>

    @Query("SELECT * FROM service_quotes WHERE customerId = :customerId LIMIT 1")
    suspend fun getServiceQuoteForCustomerSync(customerId: Int): ServiceQuote?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertServiceQuote(quote: ServiceQuote): Long

    @Update
    suspend fun updateServiceQuote(quote: ServiceQuote)

    // --- LeadSequenceState queries ---
    @Query("SELECT * FROM lead_sequence_states WHERE customerId = :customerId LIMIT 1")
    fun getLeadSequenceStateForCustomer(customerId: Int): Flow<LeadSequenceState?>

    @Query("SELECT * FROM lead_sequence_states WHERE customerId = :customerId LIMIT 1")
    suspend fun getLeadSequenceStateForCustomerSync(customerId: Int): LeadSequenceState?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLeadSequenceState(state: LeadSequenceState)

    @Update
    suspend fun updateLeadSequenceState(state: LeadSequenceState)

    @Query("SELECT * FROM lead_sequence_states")
    fun getAllLeadSequenceStates(): Flow<List<LeadSequenceState>>
}
