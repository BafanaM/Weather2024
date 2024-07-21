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

class WeatherViewModel : ViewModel() {

    private val tag = "ViewModel"
    val locationLiveData = MutableLiveData<LocationData>()
    val locationLiveDataFailure = MutableLiveData<String>()
    val weatherByLocation = MutableLiveData<Resource<WeatherResponse>>()
    val weatherForecast = MutableLiveData<Resource<WeatherForecastResponse>>()

    fun getCurrentLocation(model: LocationProvider) {
        model.getCurrentUserLocation(object : RequestCompleteListener<LocationData> {
            override fun onRequestCompleted(data: LocationData) {
                locationLiveData.postValue(data)
            }

            override fun onRequestFailed(errorMessage: String?) {
                locationLiveDataFailure.postValue(errorMessage)
            }
        })
    }

    fun getWeatherByLocation(model: WeatherRepository, lat: String, lon: String) {
        viewModelScope.launch { safeWeatherByLocationFetch(model, lat, lon) }
    }


    private suspend fun safeWeatherByLocationFetch(
        model: WeatherRepository,
        lat: String,
        lon: String
    ) {
        weatherByLocation.postValue(Resource.loading(null))
        try {
            val response = model.getWeatherByLocation(lat, lon)
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

    fun getWeatherForecast(model: WeatherRepository, lat: String, lon: String, exclude: String) {
        viewModelScope.launch { safeWeatherForecastFetch(model, lat, lon, exclude) }
    }

    private suspend fun safeWeatherForecastFetch(
        model: WeatherRepository,
        lat: String,
        lon: String,
        exclude: String
    ) {
        weatherForecast.postValue(Resource.loading(null))
        try {
            val response = model.getWeatherForecast(lat, lon, exclude)
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

    fun updateSavedCities(model: CityRepository, obj: CityUpdate) = viewModelScope.launch {
        try {
            withContext(Dispatchers.IO) {
                model.updateSavedCities(obj)
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
        // Implement error logging info
    }

    private fun error(tag: String, message: String) {
        // Implement error logging info
    }
}