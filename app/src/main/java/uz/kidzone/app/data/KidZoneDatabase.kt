package uz.kidzone.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [ProfileEntity::class, ProfileStatsEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class KidZoneDatabase : RoomDatabase() {
    abstract fun profileDao(): ProfileDao
    abstract fun profileStatsDao(): ProfileStatsDao

    companion object {
        @Volatile private var instance: KidZoneDatabase? = null

        fun getInstance(context: Context): KidZoneDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    KidZoneDatabase::class.java,
                    "kidzone.db",
                ).build().also { instance = it }
            }
    }
}
