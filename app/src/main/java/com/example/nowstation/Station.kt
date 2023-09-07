package com.example.nowstation

import com.squareup.moshi.Json

data class StationData(
    @Json(name = "name") val name: String,
    @Json(name = "都道府県") val prefecture: String,
    @Json(name = "line") val line: String,
    @Json(name = "x") val x: Double,
    @Json(name = "y") val y: Double,
    @Json(name = "郵便") val postalCode: String,
    @Json(name = "距離") val distance: String,
    @Json(name = "前") val prev: String?,
    @Json(name = "次") val next: String?,
)
data class StationResponse(
    @Json(name = "response") val response: Response
) {
    data class Response(
        @Json(name = "station") val station: List<StationData>
    )
}