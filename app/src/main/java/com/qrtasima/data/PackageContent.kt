package com.qrtasima.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "package_contents",
    foreignKeys = [ForeignKey(
        entity = ProfilePackage::class,
        parentColumns = ["id"],
        childColumns = ["packageId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("packageId")]
)
data class PackageContent(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val packageId: Int,
    val type: String,
    val label: String,
    val value: String,
    val customColor: String? = null
)