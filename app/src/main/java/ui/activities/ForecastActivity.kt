package ui.activities

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.weather2024.R
import db.CityDatabase
import kotlinx.android.synthetic.main.activity_forecast.anim_failed
import kotlinx.android.synthetic.main.activity_forecast.anim_network
import kotlinx.android.synthetic.main.activity_forecast.progressBar
import kotlinx.android.synthetic.main.activity_forecast.rv_forecast
import kotlinx.android.synthetic.main.activity_forecast.tv_error_msg
import kotlinx.android.synthetic.main.layout_toolbar.tv_tool_title
import repository.CityRepository
import repository.LocationProvider
import repository.WeatherRepository
import ui.adapters.ForecastAdapter
import utils.Status
import utils.lightStatusBar
import viewmodel.WeatherViewModel
import viewmodel.WeatherViewModelFactory


class ForecastActivity : AppCompatActivity() {

    private lateinit var viewModel: WeatherViewModel
    private lateinit var weatherRepository: WeatherRepository
    private lateinit var cityRepository: CityRepository
    private lateinit var locationProvider: LocationProvider
    private lateinit var mAdapter: ForecastAdapter
    private var lat: String? = null
    private var lon: String? = null
    private var city: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = resources.getColor(android.R.color.white)
        lightStatusBar(this, true)
        setContentView(R.layout.activity_forecast)

        weatherRepository = WeatherRepository()
        cityRepository = CityRepository(CityDatabase(this))
        locationProvider = LocationProvider(this)
        mAdapter = ForecastAdapter()

        val factory = WeatherViewModelFactory(weatherRepository, locationProvider, cityRepository)
        viewModel = ViewModelProvider(this, factory).get(WeatherViewModel::class.java)

        lat = intent.getStringExtra(LATITUDE)
        lon = intent.getStringExtra(LONGITUDE)
        city = intent.getStringExtra(CITY_NAME)

        tv_tool_title.text = city

        if (lat != null && lon != null) {
            Log.d("ForecastActivity", "Fetching weather forecast for lat: $lat, lon: $lon")
            viewModel.getWeatherForecast(lat!!, lon!!, EXCLUDE)
        }

        setUpRecyclerView()
        setUpObservers()
    }

    private fun setUpObservers() {
        viewModel.weatherForecast.observe(this, Observer { resource ->
            resource?.let {
                Log.d("ForecastActivity", "Weather forecast resource status: ${resource.status}")
                when (resource.status) {
                    Status.SUCCESS -> {
                        progressBar.visibility = View.GONE
                        tv_error_msg.visibility = View.GONE
                        anim_failed.visibility = View.GONE
                        anim_network.visibility = View.GONE
                        rv_forecast.visibility = View.VISIBLE
                        mAdapter.differ.submitList(it.data?.daily)
                        Log.d("ForecastActivity", "Weather forecast data: ${it.data?.daily}")
                    }
                    Status.ERROR -> {
                        Log.e("ForecastActivity", "Error fetching weather forecast: ${resource.message}")
                        showFailedView(it.message)
                    }
                    Status.LOADING -> {
                        progressBar.visibility = View.VISIBLE
                        tv_error_msg.visibility = View.GONE
                        rv_forecast.visibility = View.GONE
                        anim_failed.visibility = View.GONE
                        anim_network.visibility = View.GONE
                        Log.d("ForecastActivity", "Loading weather forecast")
                    }
                }
            }
        })
    }

    private fun showFailedView(message: String?) {
        progressBar.visibility = View.GONE
        tv_error_msg.visibility = View.GONE
        rv_forecast.visibility = View.GONE

        when (message) {
            "Network Failure" -> {
                anim_failed.visibility = View.GONE
                anim_network.visibility = View.VISIBLE
                Log.e("ForecastActivity", "Network failure")
            }
            else -> {
                anim_network.visibility = View.GONE
                anim_failed.visibility = View.VISIBLE
                Log.e("ForecastActivity", "Failed to fetch forecast: $message")
            }
        }
    }

    private fun setUpRecyclerView() {
        rv_forecast.apply {
            layoutManager = LinearLayoutManager(this@ForecastActivity)
            setHasFixedSize(true)
            adapter = mAdapter
        }
    }

    fun onBackButtonClicked(view: View) {
        onBackPressed()
        finish()
    }

    companion object {
        const val LATITUDE = "lat"
        const val LONGITUDE = "lon"
        const val CITY_NAME = "city"
        const val EXCLUDE = "current,minutely,hourly"
    }
}