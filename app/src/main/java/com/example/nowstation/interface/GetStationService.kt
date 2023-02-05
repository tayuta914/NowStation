package com.example.nowstation.`interface`

import com.example.nowstation.Station
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET

interface GetStationService {
    @GET("api")
    fun getStationList(): Call<Array<Station>>
}

    fun getStationApi(): GetStationService {
        val baseApiUrl = "http://express.heartrails.com/api/"
        val httpLogging = HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY)
        val httpClientBuilder = OkHttpClient.Builder().addInterceptor(httpLogging)

        val retrofit = Retrofit.Builder()
            .addConverterFactory(MoshiConverterFactory.create())
            .baseUrl(baseApiUrl)
            .client(httpClientBuilder.build())
            .build()

        return retrofit.create(GetStationService::class.java)
    }