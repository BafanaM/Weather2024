package viewmodel

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import model.CityUpdate
import model.LocationData
import model.WeatherForecastResponse
import model.WeatherResponse
import repository.CityRepository
import repository.LocationProvider
import repository.WeatherRepository
import retrofit2.Response
import utils.RequestCompleteListener
import utils.Resource
import java.io.IOException

class WeatherViewModel(
    private val weatherRepository: WeatherRepository,
    private val locationProvider: LocationProvider,
    private val cityRepository: CityRepository
) : ViewModel() {

    private val tag = "ViewModel"
    val locationLiveData = MutableLiveData<LocationData>()
    val locationLiveDataFailure = MutableLiveData<String>()
    val weatherByLocation = MutableLiveData<Resource<WeatherResponse>>()
    val weatherForecast = MutableLiveData<Resource<WeatherForecastResponse>>()
    private val _weatherForecast = MutableLiveData<Resource<WeatherForecastResponse>>()

    fun getCurrentLocation() {
        locationProvider.getCurrentUserLocation(object : RequestCompleteListener<LocationData> {
            override fun onRequestCompleted(data: LocationData) {
                locationLiveData.postValue(data)
            }

            override fun onRequestFailed(errorMessage: String?) {
                locationLiveDataFailure.postValue(errorMessage)
            }
        })
    }

    fun getWeatherByLocation(lat: String, lon: String) {
        viewModelScope.launch { safeWeatherByLocationFetch(lat, lon) }
    }

    private suspend fun safeWeatherByLocationFetch(lat: String, lon: String) {
        weatherByLocation.postValue(Resource.loading(null))
        try {
            val response = weatherRepository.getWeatherByLocation(lat, lon)
            weatherByLocation.postValue(handleWeatherResponse(response))
        } catch (t: Throwable) {
            handleError(t, weatherByLocation)
        }
    }

    private fun handleWeatherResponse(response: Response<WeatherResponse>): Resource<WeatherResponse>? {
        return if (response.isSuccessful) Resource.success(response.body()) else Resource.error(
            null,
            "Error: ${response.errorBody()}"
        )
    }

    fun getWeatherForecast(lat: String, lon: String, exclude: String) {
        _weatherForecast.value = Resource.loading(null)
        viewModelScope.launch { safeWeatherForecastFetch(lat, lon, exclude) }
    }

    private suspend fun safeWeatherForecastFetch(lat: String, lon: String, exclude: String) {
        weatherForecast.postValue(Resource.loading(null))
        try {
            val response = weatherRepository.getWeatherForecast(lat, lon, exclude)
            weatherForecast.postValue(handleWeatherForecast(response))
        } catch (t: Throwable) {
            handleError(t, weatherForecast)
        }
    }

    private fun handleWeatherForecast(response: Response<WeatherForecastResponse>): Resource<WeatherForecastResponse>? {
        return if (response.isSuccessful) {
            Log.d(tag, "Weather forecast fetched successfully")
            Resource.success(response.body())
        } else {
            Log.e(tag, "Error fetching weather forecast: ${response.errorBody()}")
            Resource.error(null, "Error: ${response.errorBody()}")
        }
    }

    fun updateSavedCities(obj: CityUpdate) = viewModelScope.launch {
        try {
            withContext(Dispatchers.IO) {
                cityRepository.updateSavedCities(obj)
            }
            info(tag, "Success: Updating City DB")
        } catch (e: Exception) {
            e.printStackTrace()
            error(tag, e.message ?: "Error updating city DB")
        }
    }

    private fun <T> handleError(t: Throwable, liveData: MutableLiveData<Resource<T>>) {
        val errorMessage = when (t) {
            is IOException -> "Network Failure"
            else -> t.localizedMessage ?: "Unknown Error"
        }
        Log.e(tag, "Handling error: $errorMessage")
        liveData.postValue(Resource.error(null, errorMessage))
    }

    private fun info(tag: String, message: String) {
        // Do nothing for now
    }

    private fun error(tag: String, message: String) {
        // Do nothing for now
    }
}