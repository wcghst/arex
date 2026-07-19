package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

class ServiceViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val repository = ServiceRepository(db.appDao())

    // UI Input States
    val nameInput = MutableStateFlow("")
    val emailInput = MutableStateFlow("")
    val phoneInput = MutableStateFlow("")
    val addressInput = MutableStateFlow("")
    val bookingDateInput = MutableStateFlow("")
    val selectedMappingMethod = MutableStateFlow("Auto Map") // "Auto Map", "Phone/Google Earth", "In-Person"
    val notesInput = MutableStateFlow("")

    val selectedServices = MutableStateFlow<Set<String>>(emptySet())
    val selectedPackage = MutableStateFlow("A la Carte") // "A la Carte", "Apex Standard Package", "Apex Elite Subscription"

    // UI Feedback States
    val isLoading = MutableStateFlow(false)
    val mappingResult = MutableStateFlow<PropertyMappingResult?>(null)
    val activeCustomerProfile = MutableStateFlow<CustomerProfile?>(null)
    val activeServiceQuote = MutableStateFlow<ServiceQuote?>(null)
    val activeLeadState = MutableStateFlow<LeadSequenceState?>(null)

    // Mode Toggle
    val isAdminMode = MutableStateFlow(false)

    // Admin Backend States
    val allCustomerProfiles = repository.allCustomers.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val selectedAdminCustomer = MutableStateFlow<CustomerProfile?>(null)
    val selectedAdminQuote = MutableStateFlow<ServiceQuote?>(null)
    val selectedAdminLeadState = MutableStateFlow<LeadSequenceState?>(null)

    // Simulated email logs displayed in UI
    private val _simulatedEmails = MutableStateFlow<List<SimulatedEmail>>(emptyList())
    val simulatedEmails: StateFlow<List<SimulatedEmail>> = _simulatedEmails.asStateFlow()

    val isFirebaseAvailable get() = FirebaseSyncManager.isFirebaseAvailable
    val currentUser = FirebaseAuthManager.currentUser

    init {
        FirebaseSyncManager.initialize(application)
        FirebaseAuthManager.checkStatus()

        // Observe active customer changes to update quote and lead state
        viewModelScope.launch {
            activeCustomerProfile.collect { profile ->
                if (profile != null) {
                    launch {
                        repository.getServiceQuoteForCustomer(profile.id).collect { quote ->
                            activeServiceQuote.value = quote
                        }
                    }
                    launch {
                        repository.getLeadSequenceStateForCustomer(profile.id).collect { state ->
                            activeLeadState.value = state
                            if (state != null) {
                                generateSimulatedEmails(profile, activeServiceQuote.value, state)
                            }
                        }
                    }
                } else {
                    activeServiceQuote.value = null
                    activeLeadState.value = null
                    _simulatedEmails.value = emptyList()
                }
            }
        }

        // Observe admin selected customer to load quote and state
        viewModelScope.launch {
            selectedAdminCustomer.collect { customer ->
                if (customer != null) {
                    selectedAdminQuote.value = repository.getServiceQuoteForCustomerSync(customer.id)
                    selectedAdminLeadState.value = repository.getLeadSequenceStateForCustomerSync(customer.id)
                } else {
                    selectedAdminQuote.value = null
                    selectedAdminLeadState.value = null
                }
            }
        }
    }

    // --- User Authentication Actions ---
    fun signInWithEmail(email: String, password: String, onResult: (Result<AuthUser>) -> Unit) {
        FirebaseAuthManager.signInWithEmail(email, password) { result ->
            if (result.isSuccess) {
                val user = result.getOrNull()
                if (user != null) {
                    viewModelScope.launch {
                        val profile = repository.getCustomerByEmail(user.email)
                        if (profile != null) {
                            activeCustomerProfile.value = profile
                        }
                    }
                }
            }
            onResult(result)
        }
    }

    fun signUpWithEmail(email: String, password: String, name: String, phone: String, isAdmin: Boolean, onResult: (Result<AuthUser>) -> Unit) {
        FirebaseAuthManager.signUpWithEmail(email, password, name, phone, isAdmin) { result ->
            if (result.isSuccess) {
                val user = result.getOrNull()
                if (user != null) {
                    if (nameInput.value.isBlank() && !isAdmin) {
                        nameInput.value = name
                        emailInput.value = email
                        phoneInput.value = phone
                    }
                }
            }
            onResult(result)
        }
    }

    fun signInWithGoogle(email: String, name: String) {
        FirebaseAuthManager.signInWithSandboxGoogle(email, name)
        viewModelScope.launch {
            val profile = repository.getCustomerByEmail(email)
            if (profile != null) {
                activeCustomerProfile.value = profile
            }
        }
    }

    fun signOut() {
        FirebaseAuthManager.signOut()
        resetForm()
    }

    // Toggle service selection
    fun toggleService(service: String) {
        val current = selectedServices.value
        if (current.contains(service)) {
            selectedServices.value = current - service
        } else {
            selectedServices.value = current + service
        }
    }

    // Set package option
    fun setPackage(packageName: String) {
        selectedPackage.value = packageName
        // Automatically select package-related services if selected
        when (packageName) {
            "Apex Standard Package" -> {
                selectedServices.value = setOf("Grass Cutting", "Window Washing", "Snow Removal")
            }
            "Apex Elite Subscription" -> {
                selectedServices.value = setOf(
                    "Grass Cutting", "Landscaping", "Fence Building",
                    "Deck Building", "Window Washing", "Pressure Washing", "Snow Removal"
                )
            }
        }
    }

    // Submit Quote (Generates Lead & starts Stage 1)
    fun submitQuoteRequest() {
        if (nameInput.value.isBlank() || emailInput.value.isBlank() || addressInput.value.isBlank()) {
            return
        }

        viewModelScope.launch {
            isLoading.value = true

            // 1. Map Property (using Gemini or fallback scanner)
            val servicesList = selectedServices.value.toList()
            val result = GeminiClient.mapPropertyAndEstimate(addressInput.value, servicesList)
            mappingResult.value = result

            // 2. Create customer profile
            val profile = CustomerProfile(
                name = nameInput.value,
                email = emailInput.value,
                phone = phoneInput.value,
                address = addressInput.value,
                bookingDate = bookingDateInput.value.ifBlank { "2026-07-02" }, // standard booking suggestion
                mappingType = selectedMappingMethod.value,
                notes = notesInput.value
            )

            // Calculate estimated quote price
            val quotePrice = calculateQuotePrice(result, servicesList, selectedPackage.value)

            val quote = ServiceQuote(
                customerId = 0, // Filled by repository
                propertySizeSqFt = result.estimatedLawnSizeSqFt,
                servicesSelected = servicesList.joinToString(", "),
                isSubscription = selectedPackage.value.contains("Subscription") || selectedPackage.value.contains("Package"),
                packageSelected = selectedPackage.value,
                quotedPrice = quotePrice,
                invoiceAmount = quotePrice,
                invoiceStatus = "Unpaid"
            )

            // 3. Save to Local Room DB and initialize lead sequence (Stage 1)
            val customerId = repository.createCustomerWithQuote(profile, quote)
            val savedProfile = profile.copy(id = customerId)
            activeCustomerProfile.value = savedProfile

            // Sync to Firebase Firestore in the background
            val savedQuote = quote.copy(customerId = customerId)
            val savedState = LeadSequenceState(
                customerId = customerId,
                currentStage = 1,
                welcomeSentAt = System.currentTimeMillis()
            )
            FirebaseSyncManager.syncCustomerProfile(savedProfile)
            FirebaseSyncManager.syncServiceQuote(savedQuote)
            FirebaseSyncManager.syncLeadSequenceState(savedState)

            isLoading.value = false
        }
    }

    // Reset workflow for a new quote
    fun resetForm() {
        nameInput.value = ""
        emailInput.value = ""
        phoneInput.value = ""
        addressInput.value = ""
        bookingDateInput.value = ""
        selectedMappingMethod.value = "Auto Map"
        notesInput.value = ""
        selectedServices.value = emptySet()
        selectedPackage.value = "A la Carte"
        mappingResult.value = null
        activeCustomerProfile.value = null
    }

    // Sign contract
    fun signContract(signature: String) {
        val currentQuote = activeServiceQuote.value ?: return
        val currentCustomer = activeCustomerProfile.value ?: return
        val currentState = activeLeadState.value ?: return

        viewModelScope.launch {
            val updatedQuote = currentQuote.copy(
                isContractSigned = true,
                contractSignature = signature,
                scheduledStartDate = currentCustomer.bookingDate.ifBlank { "2026-07-02" }
            )
            repository.updateServiceQuote(updatedQuote)
            activeServiceQuote.value = updatedQuote
            FirebaseSyncManager.syncServiceQuote(updatedQuote)

            // Move lead sequence stage to Booked/Replied (Stage 4)
            val updatedState = currentState.copy(
                currentStage = 4,
                isReplied = true,
                replyMessage = "Contract Signed by $signature",
                lastUpdated = System.currentTimeMillis()
            )
            repository.updateLeadSequenceState(updatedState)
            activeLeadState.value = updatedState
            FirebaseSyncManager.syncLeadSequenceState(updatedState)
        }
    }

    // Complete Payment
    fun payInvoice() {
        val currentQuote = activeServiceQuote.value ?: return
        viewModelScope.launch {
            val updatedQuote = currentQuote.copy(invoiceStatus = "Paid")
            repository.updateServiceQuote(updatedQuote)
            activeServiceQuote.value = updatedQuote
            FirebaseSyncManager.syncServiceQuote(updatedQuote)
        }
    }

    // Update scheduled start date
    fun updateSchedule(date: String) {
        val currentQuote = activeServiceQuote.value ?: return
        viewModelScope.launch {
            val updatedQuote = currentQuote.copy(scheduledStartDate = date)
            repository.updateServiceQuote(updatedQuote)
            activeServiceQuote.value = updatedQuote
            FirebaseSyncManager.syncServiceQuote(updatedQuote)
        }
    }

    // --- Fast Forward Simulation (Allows user/admin to see email sequence in action) ---
    fun fastForwardSimulation() {
        val currentState = activeLeadState.value ?: return
        val currentQuote = activeServiceQuote.value ?: return
        val currentCustomer = activeCustomerProfile.value ?: return

        // If customer has already signed contract, no need to progress follow-up emails
        if (currentQuote.isContractSigned) return

        viewModelScope.launch {
            val nextStage = when (currentState.currentStage) {
                1 -> 2 // Move from Stage 1 (Welcome) to Stage 2 (Quoted Price Email)
                2 -> 3 // Move from Stage 2 to Stage 3 (Follow-up after 2 days)
                else -> currentState.currentStage
            }

            if (nextStage != currentState.currentStage) {
                val updatedState = when (nextStage) {
                    2 -> currentState.copy(
                        currentStage = 2,
                        quoteSentAt = System.currentTimeMillis(),
                        lastUpdated = System.currentTimeMillis()
                    )
                    3 -> currentState.copy(
                        currentStage = 3,
                        followUpSentAt = System.currentTimeMillis(),
                        lastUpdated = System.currentTimeMillis()
                    )
                    else -> currentState
                }

                repository.updateLeadSequenceState(updatedState)
                activeLeadState.value = updatedState
                FirebaseSyncManager.syncLeadSequenceState(updatedState)
            }
        }
    }

    // Admin dashboard functions
    fun selectAdminCustomer(customer: CustomerProfile) {
        viewModelScope.launch {
            selectedAdminCustomer.value = customer
            selectedAdminQuote.value = repository.getServiceQuoteForCustomerSync(customer.id)
            selectedAdminLeadState.value = repository.getLeadSequenceStateForCustomerSync(customer.id)
        }
    }

    fun deleteCustomer(id: Int) {
        viewModelScope.launch {
            repository.deleteCustomer(id)
            FirebaseSyncManager.deleteCustomerFromFirestore(id)
            if (selectedAdminCustomer.value?.id == id) {
                selectedAdminCustomer.value = null
            }
            if (activeCustomerProfile.value?.id == id) {
                activeCustomerProfile.value = null
            }
        }
    }

    fun updateAdminQuotePrice(newPrice: Double) {
        val quote = selectedAdminQuote.value ?: return
        viewModelScope.launch {
            val updated = quote.copy(quotedPrice = newPrice, invoiceAmount = newPrice)
            repository.updateServiceQuote(updated)
            selectedAdminQuote.value = updated
            FirebaseSyncManager.syncServiceQuote(updated)
            
            // If active customer is also this one, sync
            if (activeCustomerProfile.value?.id == quote.customerId) {
                activeServiceQuote.value = updated
            }
        }
    }

    fun setLeadStageAdmin(stage: Int) {
        val state = selectedAdminLeadState.value ?: return
        viewModelScope.launch {
            val updated = state.copy(
                currentStage = stage,
                lastUpdated = System.currentTimeMillis()
            )
            repository.updateLeadSequenceState(updated)
            selectedAdminLeadState.value = updated
            FirebaseSyncManager.syncLeadSequenceState(updated)
            
            // Sync active customer
            if (activeCustomerProfile.value?.id == state.customerId) {
                activeLeadState.value = updated
            }
        }
    }

    // --- Price Calculation Engine ---
    fun calculateQuotePrice(
        scan: PropertyMappingResult,
        services: List<String>,
        pkg: String
    ): Double {
        var total = 0.0

        services.forEach { service ->
            total += when (service) {
                "Grass Cutting" -> {
                    // Base $45 + $0.015 per sq ft of lawn
                    45.0 + (scan.estimatedLawnSizeSqFt * 0.015)
                }
                "Landscaping" -> {
                    // Custom aesthetic setup base $150 + lawn layout factor
                    150.0 + (scan.estimatedLawnSizeSqFt * 0.02)
                }
                "Fence Building" -> {
                    // $30 per linear foot
                    scan.estimatedFencingLinearFt * 30.0
                }
                "Deck Building" -> {
                    // $40 per sq ft of deck
                    scan.estimatedDeckSizeSqFt * 40.0
                }
                "Window Washing" -> {
                    // $12 per window
                    scan.estimatedWindowsCount * 12.0
                }
                "Pressure Washing" -> {
                    // Base $80 + $0.12 per sq ft of driveway
                    80.0 + (scan.estimatedDrivewaySizeSqFt * 0.12)
                }
                "Snow Removal" -> {
                    // Standard seasonal premium base rate
                    120.0
                }
                else -> 0.0
            }
        }

        // Apply package offer discount structures
        return when (pkg) {
            "Apex Standard Package" -> total * 0.90 // 10% Discount
            "Apex Elite Subscription" -> total * 0.80 // 20% Discount
            else -> total
        }
    }

    // Format currency helper
    fun formatCurrency(amount: Double): String {
        val format = NumberFormat.getCurrencyInstance(Locale.US)
        return format.format(amount)
    }

    // --- Simulated Email Generation Logic ---
    private fun generateSimulatedEmails(
        customer: CustomerProfile,
        quote: ServiceQuote?,
        state: LeadSequenceState
    ) {
        val priceStr = formatCurrency(quote?.quotedPrice ?: 0.0)
        val servicesStr = quote?.servicesSelected?.ifBlank { "Apex Custom Selected Services" } ?: "All-In-One Landscaping"

        val list = mutableListOf<SimulatedEmail>()

        // Email 1: Welcome & Booking Date
        list.add(
            SimulatedEmail(
                id = 1,
                subject = "Welcome to Apex Services! Your Booking Date is Confirmed 📅",
                preheader = "We are preparing your virtual Google Earth property scan.",
                body = """
                    Hi ${customer.name},
                    
                    Thank you for choosing Apex Services! We are thrilled to help beautify and maintain your property at ${customer.address}.
                    
                    📅 YOUR PROVISIONAL START DATE: ${customer.bookingDate.ifBlank { "July 2nd, 2026" }}
                    
                    What happens next?
                    Our property mapping technicians are currently utilizing satellite maps to automatically outline your lawn layout, fence lines, and exterior cleaning surfaces. This keeps quotes 100% accurate without an intrusive in-person appointment.
                    
                    You will receive your custom quoted price and complete itemized invoice in your inbox shortly!
                    
                    Best Regards,
                    The Apex Landscaping & Construction Team
                """.trimIndent(),
                isSent = true,
                sentAtFormatted = "Sent immediately on signup"
            )
        )

        // Email 2: Quoted Price
        val isEmail2Sent = state.currentStage >= 2
        list.add(
            SimulatedEmail(
                id = 2,
                subject = "Your Custom Apex Property Service Quote is Ready! 🏷️",
                preheader = "View your detailed property mapping metrics and pricing breakdown.",
                body = """
                    Hi ${customer.name},
                    
                    We have completed our smart satellite scan of your property at ${customer.address}. Below is your personalized, high-precision price quote.
                    
                    📋 PROPERTY METRICS ACCUMULATED:
                    • Lawn Turf Area: ${quote?.propertySizeSqFt ?: 2500} sq ft
                    • Services Included: $servicesStr
                    • Pricing Structure: ${quote?.packageSelected ?: "A la Carte"}
                    
                    💰 YOUR TOTAL ESTIMATED QUOTE: $priceStr
                    
                    To start your service on ${customer.bookingDate.ifBlank { "July 2nd, 2026" }}, please review and sign your digital contract inside the Apex Portal and fulfill your initial invoice deposit.
                    
                    👉 Tap 'Review & Sign Contract' inside the Apex application to secure your scheduled slot.
                    
                    Best Regards,
                    The Apex Landscaping & Construction Team
                """.trimIndent(),
                isSent = isEmail2Sent,
                sentAtFormatted = if (isEmail2Sent) "Sent (Lead Day 1)" else "Scheduled for Day 1 (Pending Simulation)"
            )
        )

        // Email 3: Follow-Up
        val isEmail3Sent = state.currentStage >= 3
        val discountPriceStr = formatCurrency((quote?.quotedPrice ?: 0.0) * 0.90) // Special 10% off follow-up bribe
        list.add(
            SimulatedEmail(
                id = 3,
                subject = "Still Interested? Lock in a Special 10% Off Your Quote! 🎁",
                preheader = "We've reserved your service slot, but it expires soon.",
                body = """
                    Hi ${customer.name},
                    
                    We haven't heard back from you regarding your landscaping and exterior services quote for ${customer.address}. 
                    
                    We know that exterior projects require careful coordination. To make your decision easier, we are offering an exclusive 10% Follow-up Discount if you book within the next 48 hours!
                    
                    🔥 ADJUSTED QUOTE: $discountPriceStr (Save ${formatCurrency((quote?.quotedPrice ?: 0.0) * 0.10)}!)
                    
                    Please access your customer portal now to claim this adjusted price, sign your contract, and lock in your starting date of ${customer.bookingDate.ifBlank { "July 2nd, 2026" }}.
                    
                    We look forward to transforming your yard!
                    
                    Best Regards,
                    The Apex Landscaping & Construction Team
                """.trimIndent(),
                isSent = isEmail3Sent,
                sentAtFormatted = if (isEmail3Sent) "Sent (Follow-Up Day 3)" else "Scheduled for Day 3 if no reply (Pending Simulation)"
            )
        )

        _simulatedEmails.value = list
    }
}

data class SimulatedEmail(
    val id: Int,
    val subject: String,
    val preheader: String,
    val body: String,
    val isSent: Boolean,
    val sentAtFormatted: String
)
