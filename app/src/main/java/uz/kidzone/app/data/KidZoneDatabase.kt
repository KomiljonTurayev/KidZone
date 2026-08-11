package uz.kidzone.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        ProfileEntity::class,
        ProfileStatsEntity::class,
        DailyChallengeEntity::class,
        StreakEntity::class,
    ],
    version = 3,
    exportSchema = false,
)
abstract class KidZoneDatabase : RoomDatabase() {
    abstract fun profileDao(): ProfileDao
    abstract fun profileStatsDao(): ProfileStatsDao
    abstract fun dailyChallengeDao(): DailyChallengeDao
    abstract fun streakDao(): StreakDao

    companion object {
        @Volatile private var instance: KidZoneDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `daily_challenge` " +
                    "(`id` TEXT NOT NULL, `profileId` TEXT NOT NULL, `date` TEXT NOT NULL, " +
                    "`gameId` TEXT NOT NULL, `gameTitle` TEXT NOT NULL, " +
                    "`completed` INTEGER NOT NULL DEFAULT 0, PRIMARY KEY(`id`))"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `streak` " +
                    "(`profileId` TEXT NOT NULL, `count` INTEGER NOT NULL DEFAULT 0, " +
                    "`lastCompletedDate` TEXT NOT NULL DEFAULT '', PRIMARY KEY(`profileId`))"
                )
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE `streak` ADD COLUMN `lastCelebratedMilestone` INTEGER NOT NULL DEFAULT 0"
                )
            }
        }

        fun getInstance(context: Context): KidZoneDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    KidZoneDatabase::class.java,
                    "kidzone.db",
                )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .build().also { instance = it }
            }
    }
}
