package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class ServiceRepository(private val appDao: AppDao) {

    val allCustomers: Flow<List<CustomerProfile>> = appDao.getAllCustomerProfiles()
    val allLeadStates: Flow<List<LeadSequenceState>> = appDao.getAllLeadSequenceStates()

    suspend fun getCustomerById(id: Int): CustomerProfile? {
        return appDao.getCustomerProfileById(id)
    }

    suspend fun getCustomerByEmail(email: String): CustomerProfile? {
        return appDao.getCustomerProfileByEmail(email)
    }

    fun getServiceQuoteForCustomer(customerId: Int): Flow<ServiceQuote?> {
        return appDao.getServiceQuoteForCustomer(customerId)
    }

    suspend fun getServiceQuoteForCustomerSync(customerId: Int): ServiceQuote? {
        return appDao.getServiceQuoteForCustomerSync(customerId)
    }

    fun getLeadSequenceStateForCustomer(customerId: Int): Flow<LeadSequenceState?> {
        return appDao.getLeadSequenceStateForCustomer(customerId)
    }

    suspend fun getLeadSequenceStateForCustomerSync(customerId: Int): LeadSequenceState? {
        return appDao.getLeadSequenceStateForCustomerSync(customerId)
    }

    suspend fun createCustomerWithQuote(
        profile: CustomerProfile,
        quote: ServiceQuote
    ): Int {
        // Insert profile
        val customerId = appDao.insertCustomerProfile(profile).toInt()
        
        // Insert quote with updated customer ID
        val finalQuote = quote.copy(customerId = customerId)
        appDao.insertServiceQuote(finalQuote)
        
        // Initialize lead sequence state (Stage 1: Welcome sent automatically)
        val leadState = LeadSequenceState(
            customerId = customerId,
            currentStage = 1,
            welcomeSentAt = System.currentTimeMillis()
        )
        appDao.insertLeadSequenceState(leadState)
        
        return customerId
    }

    suspend fun updateCustomer(profile: CustomerProfile) {
        appDao.updateCustomerProfile(profile)
    }

    suspend fun updateServiceQuote(quote: ServiceQuote) {
        appDao.updateServiceQuote(quote)
    }

    suspend fun updateLeadSequenceState(state: LeadSequenceState) {
        appDao.updateLeadSequenceState(state)
    }

    suspend fun deleteCustomer(id: Int) {
        appDao.deleteCustomerProfileById(id)
    }
}
