package com.example.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit
import com.example.BuildConfig

@JsonClass(generateAdapter = true)
data class GeminiRequest(
    val contents: List<Content>,
    val generationConfig: GenerationConfig? = null
)

@JsonClass(generateAdapter = true)
data class Content(
    val parts: List<Part>
)

@JsonClass(generateAdapter = true)
data class Part(
    val text: String
)

@JsonClass(generateAdapter = true)
data class GenerationConfig(
    val responseMimeType: String? = null,
    val temperature: Float? = null
)

@JsonClass(generateAdapter = true)
data class GeminiResponse(
    val candidates: List<Candidate>?
)

@JsonClass(generateAdapter = true)
data class Candidate(
    val content: Content?
)

// The structured response we want to parse from Gemini
@JsonClass(generateAdapter = true)
data class PropertyMappingResult(
    val description: String,
    val estimatedLawnSizeSqFt: Int,
    val estimatedFencingLinearFt: Int,
    val estimatedDeckSizeSqFt: Int,
    val estimatedWindowsCount: Int,
    val estimatedDrivewaySizeSqFt: Int
)

interface GeminiApi {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GeminiRequest
    ): GeminiResponse
}

object GeminiClient {
    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://generativelanguage.googleapis.com/")
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    val api: GeminiApi = retrofit.create(GeminiApi::class.java)

    suspend fun mapPropertyAndEstimate(address: String, services: List<String>): PropertyMappingResult {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            // Fallback mock results if API key is not configured or placeholder
            return getFallbackResult(address, services)
        }

        val servicesStr = services.joinToString(", ")
        val prompt = """
            Act as a professional high-tech satellite property mapper.
            Analyze the property at address: "$address" for these requested services: [$servicesStr].
            Assume you are scanning satellite imagery of this property.
            
            Produce a highly realistic JSON object matching this schema:
            {
              "description": "A professional 2-3 sentence overview describing the property layout, visible obstacles, and mapping details as if observed from high-resolution satellite imagery.",
              "estimatedLawnSizeSqFt": 2500,
              "estimatedFencingLinearFt": 150,
              "estimatedDeckSizeSqFt": 300,
              "estimatedWindowsCount": 16,
              "estimatedDrivewaySizeSqFt": 800
            }
            
            Strictly return ONLY the JSON. No Markdown formatting or backticks.
        """.trimIndent()

        val request = GeminiRequest(
            contents = listOf(
                Content(parts = listOf(Part(text = prompt)))
            ),
            generationConfig = GenerationConfig(
                responseMimeType = "application/json",
                temperature = 0.2f
            )
        )

        return try {
            val response = api.generateContent(apiKey, request)
            val jsonText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?: throw Exception("Empty response from Gemini")
            
            val adapter = moshi.adapter(PropertyMappingResult::class.java)
            adapter.fromJson(jsonText) ?: throw Exception("Failed to parse JSON")
        } catch (e: Exception) {
            e.printStackTrace()
            getFallbackResult(address, services)
        }
    }

    private fun getFallbackResult(address: String, services: List<String>): PropertyMappingResult {
        // Deterministic simulation based on address hash
        val hash = address.hashCode()
        val baseLawn = 1500 + (Math.abs(hash) % 3000)
        val baseFence = 80 + (Math.abs(hash) % 120)
        val baseDeck = 100 + (Math.abs(hash) % 250)
        val baseWindows = 10 + (Math.abs(hash) % 15)
        val baseDriveway = 400 + (Math.abs(hash) % 600)

        val servicesStr = services.joinToString(", ")
        val desc = "Satellite scan for $address shows a beautifully balanced residential layout. " +
                "Visible turf area measures approx ${baseLawn} sq ft, bounded by clear boundary alignments. " +
                "The rear features a ${baseDeck} sq ft deck area. Structural perimeter scan indicates approx ${baseFence} linear feet of fencing boundary."

        return PropertyMappingResult(
            description = desc,
            estimatedLawnSizeSqFt = baseLawn,
            estimatedFencingLinearFt = baseFence,
            estimatedDeckSizeSqFt = baseDeck,
            estimatedWindowsCount = baseWindows,
            estimatedDrivewaySizeSqFt = baseDriveway
        )
    }
}
