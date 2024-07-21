package db
import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import db.CityDao
import model.Cities
import utils.DB_NAME

@Database(
    entities =[Cities::class],
    version = 1
)

abstract class CityDatabase: RoomDatabase() {

    abstract fun getCityDao(): CityDao

    companion object{

        private const val TAG = "CityDatabase"

        private val MIGRATION_1_2 = object : Migration(1,2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                //do update query here,
                // works like onUpgrade method of SQLite

                Log.d(TAG, "migrate: I am here")

                //adding save table
                database.execSQL("ALTER TABLE city_bd ADD COLUMN saved INTEGER DEFAULT NULL")
            }

        }

        @Volatile
        private var instance:CityDatabase?=null
        private val LOCK = Any()

        operator fun invoke(context: Context) = instance?: synchronized(LOCK){
            instance?:createDatabase(context).also{ instance=it}
        }

        private fun createDatabase(context: Context) = Room
//            .databaseBuilder(context.applicationContext,CityDatabase::class.java, DB_NAME)
//            .createFromAsset(DB_NAME)
            .databaseBuilder(context.applicationContext, CityDatabase::class.java, DB_NAME)
            .createFromAsset(DB_NAME)
            .build()
//            .addCallback(object : Callback(){
//                override fun onCreate(db: SupportSQLiteDatabase) {
//                    super.onCreate(db)
//                    Log.d(TAG, "onCreate: I am here")
//                    db.execSQL("ALTER TABLE city_bd ADD COLUMN isSaved INTEGER DEFAULT NULL")
//
//                }
//            })
//            .addMigrations(MIGRATION_1_2)
//            .build()
    }

}