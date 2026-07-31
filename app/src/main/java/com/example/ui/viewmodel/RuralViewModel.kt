package com.example.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.data.*
import com.example.data.api.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RuralViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application, viewModelScope)
    private val repository = RuralRepository(db)

    // UI States observed from Room DB
    val userProfile: StateFlow<UserProfile?> = repository.userProfile
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val jobs: StateFlow<List<LaborJob>> = repository.allJobs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val equipmentList: StateFlow<List<EquipmentItem>> = repository.allEquipment
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val bookings: StateFlow<List<EquipmentBooking>> = repository.allBookings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val products: StateFlow<List<MarketplaceProduct>> = repository.allProducts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val notices: StateFlow<List<VillageNotice>> = repository.allNotices
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Chat states
    val agriChatMessages: StateFlow<List<ChatMessage>> = repository.getChatMessages("agri")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val healthChatMessages: StateFlow<List<ChatMessage>> = repository.getChatMessages("health")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val offlineChatMessages: StateFlow<List<ChatMessage>> = repository.getChatMessages("offline")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Interactive Toggle to test offline/online behavior
    private val _isOnline = MutableStateFlow(true)
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    // Interactive AI loading indicators
    private val _isAgriLoading = MutableStateFlow(false)
    val isAgriLoading: StateFlow<Boolean> = _isAgriLoading.asStateFlow()

    private val _isHealthLoading = MutableStateFlow(false)
    val isHealthLoading: StateFlow<Boolean> = _isHealthLoading.asStateFlow()

    private val _isOfflineLoading = MutableStateFlow(false)
    val isOfflineLoading: StateFlow<Boolean> = _isOfflineLoading.asStateFlow()

    // Notification list (stored locally in ViewModel or in-memory for live alerts)
    private val _inAppNotifications = MutableStateFlow<List<String>>(
        listOf(
            "Welcome to RuralOS! Check out the notices page for scheduled maintenance updates.",
            "Soil Moisture alert: West Field soil moisture at 18%. Irrigation recommended soon."
        )
    )
    val inAppNotifications: StateFlow<List<String>> = _inAppNotifications.asStateFlow()

    // Soil moisture sensor simulation state
    private val _soilMoisture = MutableStateFlow(32) // %
    val soilMoisture: StateFlow<Int> = _soilMoisture.asStateFlow()

    // Weather temp simulation
    private val _weatherTemp = MutableStateFlow(34) // °C
    val weatherTemp: StateFlow<Int> = _weatherTemp.asStateFlow()

    // Carbon Footprint state
    private val _carbonSaved = MutableStateFlow(142.5) // kg CO2
    val carbonSaved: StateFlow<Double> = _carbonSaved.asStateFlow()

    init {
        // Ensure standard profile exists
        viewModelScope.launch(Dispatchers.IO) {
            val existing = repository.getUserProfileOneShot()
            if (existing == null) {
                repository.saveUserProfile(
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
            }
        }
    }

    // Toggle Online/Offline State
    fun toggleOnlineMode() {
        _isOnline.value = !_isOnline.value
        addInAppNotification("Device went " + (if (_isOnline.value) "Online" else "Offline") + " (Local Caching Active)")
    }

    fun addInAppNotification(message: String) {
        val updated = _inAppNotifications.value.toMutableList()
        updated.add(0, message)
        _inAppNotifications.value = updated
    }

    // Role switcher (immediate DB sync for seamless UI transformation)
    fun switchUserRole(role: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val current = repository.getUserProfileOneShot() ?: UserProfile()
            val updated = current.copy(role = role, occupation = role)
            repository.saveUserProfile(updated)
            withContext(Dispatchers.Main) {
                addInAppNotification("Logged in as $role. Dashboard configured.")
            }
        }
    }

    // Profile updates
    fun updateProfile(
        name: String,
        mobile: String,
        village: String,
        district: String,
        state: String,
        language: String,
        occupation: String
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val current = repository.getUserProfileOneShot() ?: UserProfile()
            val updated = current.copy(
                name = name,
                mobileNumber = mobile,
                village = village,
                district = district,
                state = state,
                preferredLanguage = language,
                occupation = occupation
            )
            repository.saveUserProfile(updated)
            withContext(Dispatchers.Main) {
                addInAppNotification("Profile updated successfully.")
            }
        }
    }

    // Feature 1: Labor Marketplace
    fun postLaborJob(title: String, workType: String, date: String, time: String, workers: Int, wage: Double, location: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val current = repository.getUserProfileOneShot()
            val job = LaborJob(
                title = title,
                farmerName = current?.name ?: "Unknown Farmer",
                workType = workType,
                date = date,
                time = time,
                numWorkersNeeded = workers,
                dailyWage = wage,
                location = location,
                status = "OPEN"
            )
            repository.addJob(job)
            withContext(Dispatchers.Main) {
                addInAppNotification("Job posted: '$title'. AI is matching nearby laborers.")
            }
        }
    }

    fun applyOrAcceptJob(job: LaborJob) {
        viewModelScope.launch(Dispatchers.IO) {
            val current = repository.getUserProfileOneShot()
            val workerName = current?.name ?: "Rajesh Kumar"
            val updatedWorkers = if (job.workersApplied.isEmpty()) workerName else "${job.workersApplied}, $workerName"
            val isFilled = updatedWorkers.split(",").size >= job.numWorkersNeeded
            val updatedJob = job.copy(
                workersApplied = updatedWorkers,
                status = if (isFilled) "FILLED" else "OPEN"
            )
            repository.updateJob(updatedJob)

            // Adjust earnings if Laborer
            if (current?.role == "Laborer") {
                val updatedProfile = current.copy(earnings = current.earnings + job.dailyWage)
                repository.saveUserProfile(updatedProfile)
            }

            withContext(Dispatchers.Main) {
                addInAppNotification("Accepted job: ${job.title}. Registered successfully.")
            }
        }
    }

    fun deleteJob(job: LaborJob) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteJob(job)
        }
    }

    // Feature 2: Equipment Rental
    fun registerEquipment(name: String, type: String, rate: Double, description: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val current = repository.getUserProfileOneShot()
            val item = EquipmentItem(
                name = name,
                type = type,
                ownerName = current?.name ?: "Owner",
                dailyRate = rate,
                availability = true,
                description = description
            )
            repository.addEquipment(item)
            withContext(Dispatchers.Main) {
                addInAppNotification("Registered equipment: $name for rental.")
            }
        }
    }

    fun bookEquipment(item: EquipmentItem, startDate: String, durationDays: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val current = repository.getUserProfileOneShot()
            val totalCost = item.dailyRate * durationDays
            val booking = EquipmentBooking(
                equipmentId = item.id,
                equipmentName = item.name,
                farmerName = current?.name ?: "Farmer",
                startDate = startDate,
                durationDays = durationDays,
                totalCost = totalCost,
                status = "PENDING"
            )
            repository.addBooking(booking)
            withContext(Dispatchers.Main) {
                addInAppNotification("Booking request submitted for ${item.name}. Pending authority approval.")
            }
        }
    }

    fun approveBooking(booking: EquipmentBooking, approve: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val updated = booking.copy(status = if (approve) "APPROVED" else "REJECTED")
            repository.updateBooking(updated)
            withContext(Dispatchers.Main) {
                addInAppNotification("Booking request for ${booking.equipmentName} was " + (if (approve) "APPROVED" else "REJECTED") + ".")
            }
        }
    }

    // Feature 6: Government Schemes Matcher
    fun getEligibleSchemes(profile: UserProfile?): List<Map<String, String>> {
        val allSchemes = listOf(
            mapOf(
                "title" to "PM Kisan Samman Nidhi (PM-KISAN)",
                "category" to "Agriculture",
                "eligibility" to "Farmer with cultivable land holding",
                "benefits" to "₹6,000 per year in 3 installments of ₹2,000 each.",
                "description" to "Direct income support to small and marginal farmer families."
            ),
            mapOf(
                "title" to "Ayushman Bharat National Health Protection",
                "category" to "Healthcare",
                "eligibility" to "Rural families identified by SEC-2011",
                "benefits" to "₹5 Lakh health cover per family per year for secondary/tertiary care.",
                "description" to "World's largest fully government-funded health insurance scheme."
            ),
            mapOf(
                "title" to "Pradhan Mantri Gramin Awas Yojana (PMAY-G)",
                "category" to "Housing",
                "eligibility" to "Rural households living in kutcha houses",
                "benefits" to "Financial assistance of ₹1.2 Lakh (plains) to ₹1.3 Lakh (hilly areas).",
                "description" to "Subsidies for constructing durable permanent pucca houses."
            ),
            mapOf(
                "title" to "MGNREGA Rural Employment Guarantee",
                "category" to "Employment",
                "eligibility" to "Adult members of rural households willing to do manual labor",
                "benefits" to "Guaranteed 100 days of manual wage employment per fiscal year.",
                "description" to "Secures livelihood and creates water conservation assets."
            ),
            mapOf(
                "title" to "Lakhpati Didi & Mahila Samridhi",
                "category" to "Women Empowerment",
                "eligibility" to "Rural women members of registered Self-Help Groups (SHGs)",
                "benefits" to "Interest-subsidized microloans up to ₹1 Lakh and digital tools training.",
                "description" to "Skill training for rural women to earn sustainable livelihoods."
            ),
            mapOf(
                "title" to "PM Vidya Lakshmi Scheme",
                "category" to "Education",
                "eligibility" to "Students pursuing higher education from villages",
                "benefits" to "Full interest subsidy on education loans up to ₹7.5 Lakhs.",
                "description" to "Ensures no rural youth is deprived of higher education due to financial issues."
            )
        )

        val role = profile?.role ?: "Farmer"
        return allSchemes.filter { scheme ->
            when (role) {
                "Farmer" -> scheme["category"] in listOf("Agriculture", "Healthcare", "Housing")
                "Laborer" -> scheme["category"] in listOf("Employment", "Healthcare", "Housing")
                "Healthcare Worker" -> scheme["category"] in listOf("Healthcare", "Women Empowerment", "Education")
                "Panchayat/Authority" -> true
                else -> true
            }
        }
    }

    // Feature 8: Marketplace
    fun addMarketplaceProduct(name: String, category: String, quantity: String, price: Double, description: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val current = repository.getUserProfileOneShot()
            val product = MarketplaceProduct(
                name = name,
                category = category,
                quantity = quantity,
                price = price,
                sellerName = current?.name ?: "Seller",
                sellerContact = current?.mobileNumber ?: "9876543210",
                description = description
            )
            repository.addProduct(product)
            withContext(Dispatchers.Main) {
                addInAppNotification("Listed product: '$name' in Marketplace category '$category'.")
            }
        }
    }

    fun buyMarketplaceProduct(product: MarketplaceProduct) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteProduct(product)
            withContext(Dispatchers.Main) {
                addInAppNotification("Order placed for ${product.name}! Seller ${product.sellerName} has been notified.")
            }
        }
    }

    // Feature 9: Notice Board
    fun addNotice(title: String, category: String, content: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val notice = VillageNotice(
                title = title,
                category = category,
                content = content,
                publisher = "Panchayat Head Desk"
            )
            repository.addNotice(notice)
            withContext(Dispatchers.Main) {
                addInAppNotification("Notice published: '$title'. Sent alerts to village users.")
            }
        }
    }

    fun deleteNotice(notice: VillageNotice) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteNotice(notice)
        }
    }

    // Feature 3, 4, 5: AI Assistants (Agriculture, Health, Offline)
    fun sendChatMessage(text: String, module: String) {
        if (text.isBlank()) return

        viewModelScope.launch(Dispatchers.IO) {
            // Save User message immediately
            repository.addChatMessage(ChatMessage(sender = "user", text = text, module = module))

            // Choose Loading state indicator
            val loadingFlow = when (module) {
                "agri" -> _isAgriLoading
                "health" -> _isHealthLoading
                else -> _isOfflineLoading
            }
            loadingFlow.value = true

            val isDeviceOnline = _isOnline.value
            val apiKey = BuildConfig.GEMINI_API_KEY
            val isKeyConfigured = apiKey.isNotBlank() && apiKey != "MY_GEMINI_API_KEY"

            var aiResponse = ""

            if (isDeviceOnline && isKeyConfigured) {
                // Perform real-time Gemini REST query
                try {
                    val systemPrompt = when (module) {
                        "agri" -> "You are RuralOS Agriculture Assistant. Give helpful advice on organic farming, crop diseases, bio-fertilizers, and water management in rural India. Be brief."
                        "health" -> "You are RuralOS Health Assistant. Give basic awareness about Blood Pressure, diabetes, BMI, nutrition, child vaccination, first aid. ALWAYS add a prominent disclaimer that you are not a replacement for a doctor."
                        else -> "You are RuralOS Offline-Ready Chatbot. Answer queries regarding government schemes, farming, basic literacy, digital payment safety. Be direct, simple and practical."
                    }

                    val request = GeminiRequest(
                        contents = listOf(Content(parts = listOf(Part(text = text)))),
                        systemInstruction = Content(parts = listOf(Part(text = systemPrompt))),
                        generationConfig = GenerationConfig(temperature = 0.7f)
                    )

                    val response = RetrofitClient.service.generateContent(apiKey, request)
                    aiResponse = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                        ?: "No text response generated by Gemini."
                } catch (e: Exception) {
                    Log.e("RuralOS_AI", "Gemini API call failed, falling back to cached answers", e)
                    aiResponse = "🔄 *Network error. Activated Local AI Cache:*\n\n" + LocalRuralKnowledge.getLocalAnswer(text, module)
                }
            } else {
                // Device is offline or key is missing -> use fast Local Knowledge Caching!
                aiResponse = if (!isDeviceOnline) {
                    "📶 *Offline Mode Enabled (Working fully cached)*\n\n" + LocalRuralKnowledge.getLocalAnswer(text, module)
                } else {
                    "🔑 *Demo Mode (Gemini API key not configured)*\n\n" + LocalRuralKnowledge.getLocalAnswer(text, module)
                }
            }

            repository.addChatMessage(ChatMessage(sender = "ai", text = aiResponse, module = module))
            loadingFlow.value = false
        }
    }

    fun clearChat(module: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.clearChatMessages(module)
        }
    }

    // IoT simulation controls
    fun triggerSoilSensorIrrigation() {
        _soilMoisture.value = 45
        _carbonSaved.value = _carbonSaved.value + 2.5
        addInAppNotification("Smart drip irrigation triggered via soil moisture telemetry.")
    }

    fun updateWeatherData() {
        _weatherTemp.value = (28..39).random()
        _soilMoisture.value = (15..65).random()
        addInAppNotification("Fetched latest climate sensors telemetry.")
    }

    fun recycleResidue(kilos: Int) {
        val co2Saved = kilos * 1.2
        _carbonSaved.value = _carbonSaved.value + co2Saved
        addInAppNotification("Recycled $kilos kg of Crop Residue into Organic Compost. Saved $co2Saved kg CO2 equivalents!")
    }
}
