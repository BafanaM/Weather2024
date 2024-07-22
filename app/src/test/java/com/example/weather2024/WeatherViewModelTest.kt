package com.example.weather2024

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import io.mockk.MockKAnnotations
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.impl.annotations.MockK
import io.mockk.verify
import io.mockk.verifyOrder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.TestCoroutineScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runBlockingTest
import kotlinx.coroutines.test.setMain
import model.Clouds
import model.Coord
import model.Daily
import model.FeelsLike
import model.LocationData
import model.Main
import model.Sys
import model.Temp
import model.Weather
import model.WeatherF
import model.WeatherForecastResponse
import model.WeatherResponse
import model.Wind
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import repository.CityRepository
import repository.LocationProvider
import repository.WeatherRepository
import retrofit2.Response
import utils.RequestCompleteListener
import utils.Resource
import viewmodel.WeatherViewModel
import java.io.IOException


@ExperimentalCoroutinesApi
class WeatherViewModelTest {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = TestCoroutineDispatcher()
    private val testScope = TestCoroutineScope(testDispatcher)

    private lateinit var weatherViewModel: WeatherViewModel

    @MockK
    private lateinit var weatherRepository: WeatherRepository

    @MockK
    private lateinit var locationProvider: LocationProvider

    @MockK
    private lateinit var cityRepository: CityRepository

    @MockK
    private lateinit var locationLiveDataObserver: Observer<LocationData>

    @MockK
    private lateinit var locationLiveDataFailureObserver: Observer<String>

    @MockK
    private lateinit var weatherByLocationObserver: Observer<Resource<WeatherResponse>>

    @MockK
    private lateinit var weatherForecastObserver: Observer<Resource<WeatherForecastResponse>>

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        Dispatchers.setMain(testDispatcher)

        weatherViewModel = WeatherViewModel(
            weatherRepository,
            locationProvider,
            cityRepository
        )

        weatherViewModel.locationLiveData.observeForever(locationLiveDataObserver)
        weatherViewModel.locationLiveDataFailure.observeForever(locationLiveDataFailureObserver)
        weatherViewModel.weatherByLocation.observeForever(weatherByLocationObserver)
        weatherViewModel.weatherForecast.observeForever(weatherForecastObserver)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        testDispatcher.cleanupTestCoroutines()
        clearAllMocks()
    }

    @Test
    fun testGetCurrentLocationSuccess() {
        val locationData = LocationData(10.0, 20.0)

        coEvery {
            locationProvider.getCurrentUserLocation(any())
        } answers {
            val callback = firstArg<RequestCompleteListener<LocationData>>()
            callback.onRequestCompleted(locationData)
        }

        weatherViewModel.getCurrentLocation()

        verify { locationLiveDataObserver.onChanged(locationData) }
        verify(exactly = 0) { locationLiveDataFailureObserver.onChanged(any()) }
    }

    @Test
    fun testGetCurrentLocationFailure() {
        val errorMessage = "Location error"

        coEvery {
            locationProvider.getCurrentUserLocation(any())
        } answers {
            val callback = firstArg<RequestCompleteListener<LocationData>>()
            callback.onRequestFailed(errorMessage)
        }

        weatherViewModel.getCurrentLocation()

        verify { locationLiveDataFailureObserver.onChanged(errorMessage) }
        verify(exactly = 0) { locationLiveDataObserver.onChanged(any()) }

    }

    @Test
    fun testGetWeatherByLocationSuccess() = testScope.runBlockingTest {
        val lat = "10.0"
        val lon = "20.0"
        val weatherResponse = WeatherResponse(
            base = "stations",
            clouds = Clouds(all = 0),
            cod = 200,
            coord = Coord(lat.toDouble(), lon.toDouble()),
            dt = 1234567890,
            id = 1,
            main = Main(
                feelsLike = 25.0,
                grndLevel = 0,
                humidity = 50,
                pressure = 1013,
                seaLevel = 0,
                temp = 25.0,
                tempMax = 30.0,
                tempMin = 20.0
            ),
            name = "Test City",
            sys = Sys(
                country = "TC",
                sunrise = 0,
                sunset = 0
            ),
            timezone = 3600,
            visibility = 10000,
            weather = listOf(
                Weather(
                    description = "clear sky",
                    icon = "01d",
                    id = 800,
                    main = "Clear"
                )
            ),
            wind = Wind(deg = 0, speed = 0.0)
        )
        val response = Response.success(weatherResponse)

        coEvery { weatherRepository.getWeatherByLocation(lat, lon) } returns response

        weatherViewModel.getWeatherByLocation(lat, lon)

        verifyOrder {
            weatherByLocationObserver.onChanged(Resource.loading(null))
            weatherByLocationObserver.onChanged(Resource.success(weatherResponse))
        }
    }

    @Test
    fun testGetWeatherByLocationError() = testScope.runBlockingTest {
        val lat = "10.0"
        val lon = "20.0"
        val errorMessage = "Network Failure"
        coEvery { weatherRepository.getWeatherByLocation(lat, lon) } throws IOException()

        weatherViewModel.getWeatherByLocation(lat, lon)

        verifyOrder {
            weatherByLocationObserver.onChanged(Resource.loading(null))
            weatherByLocationObserver.onChanged(Resource.error(null, errorMessage))
        }
    }

    @Test
    fun testGetWeatherForecastSuccess() = testScope.runBlockingTest {
        val lat = "10.0"
        val lon = "20.0"
        val exclude = "hourly,daily"

        val forecastResponse = WeatherForecastResponse(
            daily = listOf(
                Daily(
                    clouds = 10,
                    dewPoint = 15.5,
                    dt = 1638422400,
                    feelsLike = FeelsLike(day = 20.0, eve = 18.0, morn = 16.0, night = 14.0),
                    humidity = 75,
                    pop = 0.1,
                    pressure = 1012,
                    rain = 0.0,
                    sunrise = 1638402000,
                    sunset = 1638445200,
                    temp = Temp(
                        day = 21.0,
                        eve = 19.0,
                        max = 22.0,
                        min = 15.0,
                        morn = 16.0,
                        night = 14.0
                    ),
                    uvi = 3.0,
                    weather = listOf(
                        WeatherF(description = "Clear sky", icon = "01d", id = 800, main = "Clear")
                    ),
                    windDeg = 180,
                    windSpeed = 5.0
                )
            ),
            lat = lat.toDouble(),
            lon = lon.toDouble(),
            timezone = "Africa/Johannesburg",
            timezoneOffset = 0
        )

        val response = Response.success(forecastResponse)

        coEvery { weatherRepository.getWeatherForecast(lat, lon, exclude) } returns response

        weatherViewModel.getWeatherForecast(lat, lon, exclude)

        verifyOrder {
            weatherForecastObserver.onChanged(Resource.loading(null))
            weatherForecastObserver.onChanged(Resource.success(forecastResponse))
        }
    }

    @Test
    fun testGetWeatherForecastError() = testScope.runBlockingTest {
        val lat = "10.0"
        val lon = "20.0"
        val exclude = "hourly,daily"
        val errorMessage = "Network Failure"
        coEvery { weatherRepository.getWeatherForecast(lat, lon, exclude) } throws IOException()

        weatherViewModel.getWeatherForecast(lat, lon, exclude)

        verifyOrder {
            weatherForecastObserver.onChanged(Resource.loading(null))
            weatherForecastObserver.onChanged(Resource.error(null, errorMessage))
        }
    }
}