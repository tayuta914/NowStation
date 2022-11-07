package com.example.nowstation

import android.Manifest
import android.annotation.SuppressLint import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.nowstation.databinding.ActivityMainBinding
import com.google.android.gms.location.*


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val REQUEST_CODE_LOCATION = 100
    private val locationCallback: LocationCallback? = null
    private var requestingLocationUpdates = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.reqBtn.setOnClickListener {
            checkPermission()
        }
    }

    private fun checkPermission() {
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
                Toast.makeText(this@MainActivity, "許可が得られなかったので中止しました。", Toast.LENGTH_SHORT).show()
                finish()
            }
        }.show()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode != REQUEST_CODE_LOCATION) return

        if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
            finish()
            return
        }
        getCoordinates()
    }

    private fun getCoordinates() {
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
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
        fusedLocationClient.lastLocation.addOnSuccessListener {
            Toast.makeText(this, "座標点を取得しました。", Toast.LENGTH_SHORT).show()
            val latitude: String = it.latitude.toString()
            val longitude: String = it.longitude.toString()
            binding.latitude.text = "緯度:$latitude"
            binding.longitude.text = "経度:$longitude"
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
        locationCallback.also {
            if (it != null) {
                fusedLocationClient.removeLocationUpdates(it)
            }
            stopLocationUpdates()
        }
    }

    private fun stopLocationUpdates() {
        if (locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }
    }
}

