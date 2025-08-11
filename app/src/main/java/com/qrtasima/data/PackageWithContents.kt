package com.qrtasima.data

import androidx.room.Embedded
import androidx.room.Relation

data class PackageWithContents(
    @Embedded val profilePackage: ProfilePackage,
    @Relation(
        parentColumn = "id",
        entityColumn = "packageId"
    )
    val contents: List<PackageContent>
)