package com.example.data.api

import com.example.BuildConfig
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class GeminiRequest(
    @Json(name = "contents") val contents: List<Content>,
    @Json(name = "generationConfig") val generationConfig: GenerationConfig? = null,
    @Json(name = "systemInstruction") val systemInstruction: Content? = null
)

@JsonClass(generateAdapter = true)
data class Content(
    @Json(name = "parts") val parts: List<Part>
)

@JsonClass(generateAdapter = true)
data class Part(
    @Json(name = "text") val text: String? = null
)

@JsonClass(generateAdapter = true)
data class GenerationConfig(
    @Json(name = "temperature") val temperature: Float? = null,
    @Json(name = "topP") val topP: Float? = null,
    @Json(name = "topK") val topK: Int? = null
)

@JsonClass(generateAdapter = true)
data class GeminiResponse(
    @Json(name = "candidates") val candidates: List<Candidate>? = null
)

@JsonClass(generateAdapter = true)
data class Candidate(
    @Json(name = "content") val content: Content? = null
)

interface GeminiApiService {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GeminiRequest
    ): GeminiResponse
}

object RetrofitClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    val service: GeminiApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(GeminiApiService::class.java)
    }
}

object LocalRuralKnowledge {
    fun getLocalAnswer(query: String, category: String): String {
        val q = query.lowercase()
        return when (category.lowercase()) {
            "agri" -> {
                when {
                    q.contains("disease") || q.contains("leaf") || q.contains("spots") || q.contains("yellow") -> {
                        "🌾 *Crop Disease Diagnostic Support (Local Cache)*\n\n" +
                        "Based on symptoms like yellowing, spots, or wilting:\n" +
                        "1. **Paddy Blast**: Appears as spindle-shaped spots with grey centers. *Remedy*: Spray Neem seed kernel extract (5%) or use bio-pesticide Trichoderma viride.\n" +
                        "2. **Leaf Rust in Wheat**: Reddish-brown powder pustules. *Remedy*: Maintain proper spacing and avoid excess nitrogen fertilizer.\n" +
                        "3. **Prevention**: Avoid over-watering. Introduce crop rotation and plant resistant crop varieties."
                    }
                    q.contains("fertilizer") || q.contains("manure") || q.contains("urea") -> {
                        "🌱 *Subsidized Organic Fertilizer Recommendation*\n\n" +
                        "Recommended split dosage of nutrients for optimum soil health:\n" +
                        "- **Basal Dose**: Apply organic compost (5 tonnes/hectare) paired with Bio-NPK liquid during soil preparation.\n" +
                        "- **Top Dressing**: For nitrogen deficiency, top-dress with neem-coated urea at 21 and 42 days after transplanting.\n" +
                        "- **Soil Acidification**: Use Gypsum if your soil report shows pH > 8.5."
                    }
                    q.contains("pest") || q.contains("insect") || q.contains("bug") -> {
                        "🐛 *Eco-Friendly Pest Management (IPM)*\n\n" +
                        "1. **Sucking Pests (Aphids/Whiteflies)**: Spray natural Neem Oil solution (10,000 ppm) mixed with liquid soap.\n" +
                        "2. **Stem Borer**: Install Pheromone traps (4-5 per acre) to attract moths and interrupt the mating cycle.\n" +
                        "3. **Physical Protection**: Release Trichogramma egg parasitoids at weekly intervals."
                    }
                    else -> {
                        "🌾 *Smart Farming Advisory (Local Cache)*\n\n" +
                        "To maximize your agricultural yields this season:\n" +
                        "1. **Soil Testing**: Always test your soil pH and NPK levels before applying synthetic fertilizers.\n" +
                        "2. **Water Conservation**: Prefer Drip/Sprinkler irrigation to save up to 40% water.\n" +
                        "3. **Market Link**: Sell your surplus organic crops on RuralOS Marketplace for direct buyer pricing!"
                    }
                }
            }
            "health" -> {
                when {
                    q.contains("diabetes") || q.contains("sugar") -> {
                        "🩺 *Local Health Advisory: Diabetes & Blood Sugar*\n\n" +
                        "To manage diabetes risk effectively in rural settings:\n" +
                        "1. **Diet**: Reduce consumption of polished white rice and refined sugar. Substitute with millets (Ragi/Bajra) and high-fiber lentils.\n" +
                        "2. **Physical Activity**: Walking for 30 minutes daily after meals helps regulate insulin levels.\n" +
                        "3. **Regular Testing**: Check fasting blood sugar levels monthly at the nearby Primary Health Sub-Centre.\n\n" +
                        "⚠️ *Disclaimer: This is for educational purposes. Please consult your village healthcare worker or a licensed doctor.*"
                    }
                    q.contains("pressure") || q.contains("bp") || q.contains("tension") -> {
                        "❤️ *Local Health Advisory: Blood Pressure (Hypertension)*\n\n" +
                        "Awareness and monitoring guidelines:\n" +
                        "1. **Salt Intake**: Limit dietary salt intake to under 1 teaspoon (5g) per day. Avoid salted pickles and papad.\n" +
                        "2. **Stress Reduction**: Practice deep breathing or meditation for 10 minutes every morning.\n" +
                        "3. **Symptoms**: If you experience persistent headaches, dizziness, or neck pain, visit the local health camp immediately for a blood pressure reading."
                    }
                    q.contains("bmi") || q.contains("weight") || q.contains("height") -> {
                        "📊 *BMI (Body Mass Index) Calculation Guide*\n\n" +
                        "Calculate your BMI using the formula: weight (kg) / height² (m²)\n" +
                        "- **Under 18.5**: Underweight. Increase intake of protein-rich legumes, milk, eggs, and groundnuts.\n" +
                        "- **18.5 to 24.9**: Healthy range. Maintain balanced diet of green leafy vegetables and whole grains.\n" +
                        "- **25.0+**: Overweight. Restrict deep-fried snacks, sweet teas, and increase regular outdoor active physical labor."
                    }
                    else -> {
                        "🩺 *Primary Health Advisory (Local Cache)*\n\n" +
                        "Rural healthcare wellness tips:\n" +
                        "1. **Drinking Water**: Always boil drinking water or use chlorine tablets during monsoons to prevent diarrhea.\n" +
                        "2. **Child Nutrition**: Ensure exclusive breastfeeding for infants up to 6 months. Maintain immunizations as per the village health camp schedule.\n" +
                        "3. **First Aid**: Keep clean cotton, antiseptic liquid, and ORS packets handy at home."
                    }
                }
            }
            "offline" -> {
                when {
                    q.contains("scheme") || q.contains("government") || q.contains("pm") -> {
                        "🏛️ *Subsidized Government Schemes Summary*\n\n" +
                        "1. **PM-KISAN**: Cash benefit of ₹6,000/year in three equal installments directly into farmers' bank accounts.\n" +
                        "2. **Ayushman Bharat (PM-JAY)**: Cashless health insurance up to ₹5 Lakh/family per year for secondary and tertiary care.\n" +
                        "3. **PM Awas Yojana**: Subsidy assistance for building permanent, pucca houses in rural areas.\n" +
                        "4. **PM Garib Kalyan Anna Yojana**: Under this scheme, eligible rural households receive free food grains (rice/wheat)."
                    }
                    q.contains("education") || q.contains("school") || q.contains("learn") -> {
                        "📚 *Rural Learning & Skills Development*\n\n" +
                        "- **Modern Agriculture**: Free training materials available at RuralOS Learning Center. Learn crop rotation and greenhouse setups.\n" +
                        "- **Digital Payments**: Always confirm the recipient's name before entering your UPI PIN. Never share your OTP with anyone calling on behalf of bank officials.\n" +
                        "- **Entrepreneurship**: Self-Help Groups (SHGs) can register on RuralOS to list handicraft/organic foods in the marketplace."
                    }
                    else -> {
                        "🌾 *RuralOS Smart Village Assistant*\n\n" +
                        "I am your offline-ready village assistant. How can I assist you today?\n" +
                        "- **Agriculture**: Type 'fertilizer tips', 'crop disease', or 'pest control'.\n" +
                        "- **Health**: Type 'BP control', 'diabetes risk', or 'nutrition'.\n" +
                        "- **Schemes**: Type 'farmer schemes', 'health insurance', or 'housing subsidies'.\n" +
                        "- **Language**: You can query me in English, Hindi, Telugu, or any preferred language!"
                    }
                }
            }
            else -> "I'm here to support you in offline mode with pre-cached rural insights!"
        }
    }
}
