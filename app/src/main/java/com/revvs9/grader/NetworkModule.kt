package com.revvs9.grader

import okhttp3.JavaNetCookieJar
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.scalars.ScalarsConverterFactory
import java.net.CookieManager
import java.util.concurrent.TimeUnit // Import TimeUnit

object NetworkModule {

    // The base URL is a general prefix. Specific paths are defined in the ApiService.
    private const val BASE_URL = "https://hac.friscoisd.org/"

    // HttpLoggingInterceptor helps in debugging by logging network request and response data.
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY // Logs headers and body for both request and response.
                                                // Use Level.BASIC or Level.NONE for less verbose logging in production.
    }

    // OkHttpClient is the underlying HTTP client for Retrofit.
    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor) // Adds the logging interceptor to see network traffic.
        .cookieJar(JavaNetCookieJar(CookieManager())) // Manages cookies, essential for session handling after login.
        .connectTimeout(30, TimeUnit.SECONDS) // Set connect timeout to 30 seconds
        .readTimeout(30, TimeUnit.SECONDS)    // Set read timeout to 30 seconds
        .writeTimeout(30, TimeUnit.SECONDS)   // Set write timeout to 30 seconds
        .build()

    // Lazy-initialized Retrofit instance for the HacApiService.
    val hacApiService: HacApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient) // Uses the configured OkHttpClient.
            .addConverterFactory(ScalarsConverterFactory.create()) // Handles responses as plain Strings (for HTML).
            .build()
            .create(HacApiService::class.java) // Creates an implementation of the HacApiService interface.
    }
}
