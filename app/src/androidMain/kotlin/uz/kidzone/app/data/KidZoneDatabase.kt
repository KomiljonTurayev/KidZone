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
    version = 4,
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

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `profiles` ADD COLUMN `updatedAt` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `profiles` ADD COLUMN `deleted` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("UPDATE `profiles` SET `updatedAt` = `createdAt`")
            }
        }

        fun getInstance(context: Context): KidZoneDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    KidZoneDatabase::class.java,
                    "kidzone.db",
                )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                .build().also { instance = it }
            }
    }
}
