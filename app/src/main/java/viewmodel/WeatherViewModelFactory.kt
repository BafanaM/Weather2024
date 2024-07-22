package viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import repository.CityRepository
import repository.LocationProvider
import repository.WeatherRepository

class WeatherViewModelFactory(
    private val weatherRepo: WeatherRepository,
    private val locationProvider: LocationProvider,
    private val cityRepo: CityRepository
) : ViewModelProvider.Factory {

    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(WeatherViewModel::class.java)) {
            return WeatherViewModel(weatherRepo, locationProvider, cityRepo) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}