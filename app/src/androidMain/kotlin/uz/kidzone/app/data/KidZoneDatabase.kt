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
        LevelUpEntity::class,
        ParentChallengeEntity::class,
    ],
    version = 7,
    exportSchema = false,
)
abstract class KidZoneDatabase : RoomDatabase() {
    abstract fun profileDao(): ProfileDao
    abstract fun profileStatsDao(): ProfileStatsDao
    abstract fun dailyChallengeDao(): DailyChallengeDao
    abstract fun streakDao(): StreakDao
    abstract fun levelUpDao(): LevelUpDao
    abstract fun parentChallengeDao(): ParentChallengeDao

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

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `level_ups` " +
                    "(`profileId` TEXT NOT NULL, `gameId` TEXT NOT NULL, `level` INTEGER NOT NULL, " +
                    "`date` TEXT NOT NULL, PRIMARY KEY(`profileId`, `gameId`, `level`))"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_level_ups_profileId_date` ON `level_ups` (`profileId`, `date`)"
                )
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `daily_challenge` ADD COLUMN `slot` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `daily_challenge` ADD COLUMN `target` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `daily_challenge` ADD COLUMN `progress` INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `parent_challenge` " +
                    "(`profileId` TEXT NOT NULL, `gameId` TEXT NOT NULL, `targetLevel` INTEGER NOT NULL, " +
                    "`createdAt` INTEGER NOT NULL, `completedAt` INTEGER, PRIMARY KEY(`profileId`))"
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
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7)
                .build().also { instance = it }
            }
    }
}
