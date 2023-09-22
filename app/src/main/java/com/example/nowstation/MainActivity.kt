package com.example.nowstation

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.ContentValues
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.nowstation.databinding.ActivityMainBinding
import com.example.nowstation.`interface`.GetStationService
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val REQUEST_CODE_LOCATION = 100
    private var requestingLocationUpdates = false
    private lateinit var retrofit: Retrofit
    private lateinit var getStationService: GetStationService

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(p0: LocationResult) {
            val location = p0.lastLocation
            if (location != null) {
                val latitude = location.latitude
                val longitude = location.longitude
                // 取得した緯度・経度を使って最寄り駅を検索する処理を呼び出す
                fetchNearbyStations(latitude, longitude)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        val moshi = Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()

        retrofit = Retrofit.Builder()
            .baseUrl("http://express.heartrails.com/")
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
        getStationService = retrofit.create(GetStationService::class.java)

        binding.serach.setOnClickListener {
            requestPermission()
        }
    }

    // 現在のパーミッションを要求
    private fun requestPermission() {
        val permissionStatusOfLocation = ContextCompat.checkSelfPermission(
            this@MainActivity,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
        if (permissionStatusOfLocation == PackageManager.PERMISSION_GRANTED) getCoordinates() else permissionRequest()
    }

    private fun permissionRequest() {
        val isNeedDialogOfLocation: Boolean =
            ActivityCompat.shouldShowRequestPermissionRationale(
                this@MainActivity,
                Manifest.permission.ACCESS_FINE_LOCATION
            )

        if (!isNeedDialogOfLocation) {
            ActivityCompat.requestPermissions(
                this@MainActivity,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                REQUEST_CODE_LOCATION
            )
            return
        }

        AlertDialog.Builder(this@MainActivity).apply {
            setTitle("位置情報取得には許可が必要になります。")
            setMessage("許可を得ないと位置情報を取得せずに終了することになります。ご注意下さい。")
            setPositiveButton("許可する") { _, _ ->
                ActivityCompat.requestPermissions(
                    this@MainActivity,
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    REQUEST_CODE_LOCATION
                )
            }
            setNegativeButton("許可しない") { _, _ ->
                Toast.makeText(
                    this@MainActivity,
                    "許可が得られなかったので中止しました。",
                    Toast.LENGTH_SHORT
                )
                    .show()
                finish()
            }
        }.show()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode != REQUEST_CODE_LOCATION) return
        if (grantResults.isEmpty() || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
            finish()
            return
        }
        getCoordinates()
    }

    private fun getCoordinates() {
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // 位置情報を取得する前にユーザから必要なパーミッションをチェックする
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        // 位置情報の取得
        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            if (location != null) {
                val latitude = location.latitude
                val longitude = location.longitude
                Log.d(ContentValues.TAG, "座標点を取得しました: ($latitude, $longitude)")

                // 取得した緯度・経度を使って最寄り駅を検索する処理を呼び出し
                fetchNearbyStations(latitude, longitude)
            }
        }
            .addOnFailureListener { e ->
                Log.e("MainActivity", "Error trying to get last GPS location", e)
            }
    }

    private fun fetchNearbyStations(latitude: Double, longitude: Double) {
        lifecycleScope.launch {
            try {
                val response = fetchStationsFromAPI(latitude, longitude)
                handleAPIResponse(response)
            } catch (t: Throwable) {
                Log.e("fetchItems", "Error in fetchItems", t)
            }
        }
    }

    private suspend fun fetchStationsFromAPI(
        latitude: Double,
        longitude: Double,
    ): Response<StationResponse>? {
        return withContext(Dispatchers.IO) {
            getStationService.getNearbyStations("getStations", latitude, longitude)
        }
    }

    private fun handleAPIResponse(response: Response<StationResponse>?) {
        if (response?.isSuccessful != true || response.body() == null) {
            Log.d("fetchItems", "response is not successful or body is null")
            return
        }
        val stations = response.body()?.response?.station ?: run {
            Log.d("fetchItems", "stations is null")
            return
        }
        val stationList = extractTopStations(stations, 3)
        getNearbyStations(stationList)
    }

    private fun extractTopStations(stations: List<StationData>, count: Int): List<StationData> {
        return stations.take(count).also { stationList ->
            stationList.forEach { station ->
                val stationInfo = "${station.name} (${station.distance}m) - ${station.line}"
                Log.d("fetchNearbyStations", stationInfo)
            }
        }
    }

    private fun getNearbyStations(stationList: List<StationData>) {
        lifecycleScope.launch {
            withContext(Dispatchers.Main) {
                // 取得した駅のリストを使って、何か処理を実行する
                val stationNames = stationList.take(3).map { it.name }
                binding.stationName1.text = stationNames.getOrNull(0) ?: ""
                binding.stationName2.text = stationNames.getOrNull(1) ?: ""
                binding.stationName3.text = stationNames.getOrNull(2) ?: ""
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (requestingLocationUpdates) {
            startLocationUpdates()
        }
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        locationCallback.also {
            if (it != null) {
                fusedLocationClient.requestLocationUpdates(
                    LocationRequest.create().apply {
                        interval = 5000
                        priority = Priority.PRIORITY_HIGH_ACCURACY
                    }, it, this.mainLooper
                )
            }
        }
    }

    override fun onPause() {
        super.onPause()
        stopLocationUpdates()
    }

    private fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }
}