package ui.activities

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.example.weather2024.R
import com.example.weather2024.databinding.ActivityMainBinding
import com.example.weather2024.databinding.LayoutWeatherMainBinding
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.Autocomplete
import com.google.android.libraries.places.widget.AutocompleteActivity
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode
import db.CityDatabase
import model.CityUpdate
import model.WeatherResponse
import repository.CityRepository
import repository.LocationProvider
import repository.WeatherRepository
import utils.GOOGLE_API_KEY
import utils.GpsUtils
import utils.Status
import utils.showToast
import utils.unixTimestampToTimeString
import viewmodel.WeatherViewModel
import viewmodel.WeatherViewModelFactory



class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var weatherBinding: LayoutWeatherMainBinding
    private lateinit var viewModel: WeatherViewModel
    private lateinit var model: LocationProvider
    private lateinit var weatherRepo: WeatherRepository
    private lateinit var cityRepo: CityRepository
    private var isGPSEnabled = false
    private var lat: String? = null
    private var lon: String? = null
    private var city: String? = null

    companion object {
        private const val AUTOCOMPLETE_REQUEST_CODE = 1
        private const val LOCATION_REQUEST = 2
        private const val TAG = "MainActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        weatherBinding = LayoutWeatherMainBinding.bind(binding.incInfoWeather.root)

        Places.initialize(applicationContext, GOOGLE_API_KEY)

        model = LocationProvider(this)
        weatherRepo = WeatherRepository()
        cityRepo = CityRepository(CityDatabase(this))

        viewModel = ViewModelProvider(
            this,
            WeatherViewModelFactory(weatherRepo, model, cityRepo)
        ).get(WeatherViewModel::class.java)

        GpsUtils(this).turnGPSOn(object : GpsUtils.OnGpsListener {
            override fun gpsStatus(isGPSEnable: Boolean) {
                this@MainActivity.isGPSEnabled = isGPSEnable
                if (isGPSEnable) {
                    invokeLocationAction()
                }
            }
        })

        setUpObservers()
    }

    private fun setUpObservers() {
        viewModel.locationLiveData.observe(this) { location ->
            viewModel.getWeatherByLocation(location.latitude.toString(), location.longitude.toString())
        }

        viewModel.weatherByLocation.observe(this) { resource ->
            resource?.let {
                when (it.status) {
                    Status.SUCCESS -> {
                        binding.incInfoWeather.root.visibility = View.VISIBLE
                        binding.progressBar.visibility = View.GONE
                        binding.animFailed.visibility = View.GONE
                        binding.animNetwork.visibility = View.GONE
                        setUpUI(it.data)
                        viewModel.updateSavedCities(CityUpdate(it.data?.id, 1))
                    }
                    Status.ERROR -> {
                        showFailedView(it.message)
                    }
                    Status.LOADING -> {
                        binding.progressBar.visibility = View.VISIBLE
                        binding.animFailed.visibility = View.GONE
                        binding.animNetwork.visibility = View.GONE
                    }
                }
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun setUpUI(data: WeatherResponse?) {
        data?.let { weatherData ->
            weatherBinding.tvTemp.text = weatherData.main?.temp.toString()
            weatherBinding.tvCityName.text = weatherData.name
            weatherBinding.tvWeatherCondition.text = weatherData.weather[0].main
            weatherBinding.inclSingleWeatherLayout.tvSunriseTime.text =
                weatherData.sys.sunrise.unixTimestampToTimeString()
            weatherBinding.inclSingleWeatherLayout.tvSunsetTime.text =
                weatherData.sys.sunset.unixTimestampToTimeString()
            weatherBinding.inclSingleWeatherLayout.tvRealFeelText.text =
                "${weatherData.main.feelsLike}${getString(R.string.degree_in_celsius)}"
            weatherBinding.inclSingleWeatherLayout.tvCloudinessText.text =
                "${weatherData.clouds.all}%"
            weatherBinding.inclSingleWeatherLayout.tvWindSpeedText.text =
                "${weatherData.wind.speed}m/s"
            weatherBinding.inclSingleWeatherLayout.tvHumidityText.text =
                "${weatherData.main.humidity}%"
            weatherBinding.inclSingleWeatherLayout.tvPressureText.text =
                "${weatherData.main.pressure}hPa"
            weatherBinding.inclSingleWeatherLayout.tvVisibilityText.text =
                "${weatherData.visibility}M"

            val backgroundResource = when (weatherData.weather[0].main) {
                "Clear" -> R.drawable.sea_sunnypng
                "Rain" -> R.drawable.sea_rainy
                "Clouds" -> R.drawable.sea_cloudy
                else -> R.drawable.sea_sunnypng
            }
            weatherBinding.inclSingleWeatherLayout.clWeatherSingle.setBackgroundResource(
                backgroundResource
            )

            lat = weatherData.coord.lat.toString()
            lon = weatherData.coord.lon.toString()
            city = weatherData.name
        }
    }

    private fun showFailedView(message: String?) {
        binding.progressBar.visibility = View.GONE
        binding.incInfoWeather.root.visibility = View.GONE

        when (message) {
            "Network Failure" -> {
                binding.animFailed.visibility = View.GONE
                binding.animNetwork.visibility = View.VISIBLE
            }
            else -> {
                binding.animNetwork.visibility = View.GONE
                binding.animFailed.visibility = View.VISIBLE
            }
        }
    }

    private fun invokeLocationAction() {
        Log.d(TAG, "invokeLocationAction called")
        when {
            !isGPSEnabled -> showToast(this, "Enable GPS", Toast.LENGTH_SHORT)
            isPermissionsGranted() -> {
                Log.d(TAG, "Permissions already granted")
                startLocationUpdate()
            }
            shouldShowRequestPermissionRationale() -> {
                Log.d(TAG, "Showing permission rationale")
                requestLocationPermission()
            }
            else -> {
                Log.d(TAG, "Requesting location permissions")
                requestLocationPermission()
            }
        }
    }

    private fun requestLocationPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ),
            LOCATION_REQUEST
        )
    }

    private fun startLocationUpdate() {
        viewModel.getCurrentLocation()
    }

    private fun isPermissionsGranted() =
        ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED

    private fun shouldShowRequestPermissionRationale() =
        ActivityCompat.shouldShowRequestPermissionRationale(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) || ActivityCompat.shouldShowRequestPermissionRationale(
            this,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )

    private fun showPermissionDeniedDialog() {
        AlertDialog.Builder(this)
            .setTitle("Location Permission Required")
            .setMessage("This app requires location permission to function. Please enable it in the app settings.")
            .setPositiveButton("Open Settings") { _, _ ->
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                val uri = Uri.fromParts("package", packageName, null)
                intent.data = uri
                startActivity(intent)
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_REQUEST) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startLocationUpdate()
            } else {
                showPermissionDeniedDialog()
            }
        }
    }

    fun onAddButtonClicked(view: View) {
        val fields = listOf(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG)
        val intent = Autocomplete.IntentBuilder(AutocompleteActivityMode.FULLSCREEN, fields)
            .build(this)
        startActivityForResult(intent, AUTOCOMPLETE_REQUEST_CODE)
    }

    fun onForecastButtonClicked(view: View) {
        startActivity(
            Intent(this@MainActivity, ForecastActivity::class.java)
                .putExtra(ForecastActivity.LATITUDE, lat)
                .putExtra(ForecastActivity.LONGITUDE, lon)
                .putExtra(ForecastActivity.CITY_NAME, city)
        )
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == AUTOCOMPLETE_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                val place = Autocomplete.getPlaceFromIntent(data!!)
                val placeName = place.name
                val latLng = place.latLng
                if (latLng != null) {
                    lat = latLng.latitude.toString()
                    lon = latLng.longitude.toString()
                    city = placeName
                    weatherBinding.tvCityName.text = placeName
                    viewModel.getWeatherByLocation(lat!!, lon!!)
                }
            } else if (resultCode == AutocompleteActivity.RESULT_ERROR) {
                val status = Autocomplete.getStatusFromIntent(data!!)
                status.statusMessage?.let { Log.i(TAG, it) }
            }
        }
    }
}