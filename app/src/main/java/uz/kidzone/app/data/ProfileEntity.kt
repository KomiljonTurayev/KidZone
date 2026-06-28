package uz.kidzone.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "profiles")
data class ProfileEntity(
    @PrimaryKey val id: String,
    val name: String,
    val avatarPath: String?,
    val language: String,
    val timeLimitMinutes: Int,
    val pinHash: String?,
    val isDefault: Boolean,
    val createdAt: Long,
)
