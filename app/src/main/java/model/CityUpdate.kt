package model

import androidx.room.ColumnInfo
import androidx.room.Entity

@Entity
data class CityUpdate (
    @ColumnInfo(name = "id")
    var id:Int?=null,

    @ColumnInfo(name = "saved")
    var saved:Int?=null
)