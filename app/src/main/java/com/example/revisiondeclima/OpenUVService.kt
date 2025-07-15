package com.example.revisiondeclima

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

interface OpenUVService {

    @GET("uv")
    suspend fun getCurrentUV(
        @Query("lat") lat: Double,
        @Query("lng") lon: Double,
        @Header("x-access-token") token: String = BuildConfig.OPENUV_API_KEY
    ): OpenUVResponse

    @GET("uv/forecast")
    suspend fun getUVForecast(
        @Query("lat") lat: Double,
        @Query("lng") lon: Double,
        @Header("x-access-token") token: String = BuildConfig.OPENUV_API_KEY
    ): OpenUVResponse

    companion object {
        private const val BASE_URL = "https://api.openuv.io/api/v1/"

        fun create(): OpenUVService {
            val retrofit = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
            return retrofit.create(OpenUVService::class.java)
        }
    }
}