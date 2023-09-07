package com.example.nowstation.`interface`
import com.example.nowstation.StationResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query


interface GetStationService {
    @GET("api/json")
    suspend fun getNearbyStations(
        @Query("method") method: String,
        @Query("x") longitude: Double,
        @Query("y") latitude: Double
    ): Response<StationResponse>
}