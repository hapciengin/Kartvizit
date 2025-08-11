package com.qrtasima.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "profile_packages")
data class ProfilePackage(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val profileImageBase64: String? = null,
    val backgroundColor: String? = "#f0f2f5"
)