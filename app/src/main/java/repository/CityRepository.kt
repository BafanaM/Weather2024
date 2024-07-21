package repository

import db.CityDatabase
import model.CityUpdate

class CityRepository (private val database: CityDatabase) {
    fun updateSavedCities(obj: CityUpdate) = database.getCityDao().updateSavedCity(obj)
}