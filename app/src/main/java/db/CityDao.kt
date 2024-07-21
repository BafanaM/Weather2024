package db

import androidx.lifecycle.LiveData
import androidx.room.*
import model.Cities
import model.CityUpdate

@Dao
interface CityDao {

    @Query("SELECT * FROM city_bd WHERE name LIKE :key || '%'")
    fun searchCity(key: String):List<Cities>

    @Update(entity = Cities::class)
    fun updateSavedCity(vararg obj:CityUpdate):Int

    @Query("SELECT * FROM city_bd WHERE saved= :key")
    fun getSavedCity(key:Int):LiveData<List<Cities>>

    @Delete
    fun deleteSavedCity(city: Cities)
}