package com.qrtasima.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface PackageDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertPackage(profilePackage: ProfilePackage): Long

    @Update
    fun updatePackage(profilePackage: ProfilePackage)

    @Query("SELECT * FROM profile_packages ORDER BY id DESC")
    fun getAllPackages(): Flow<List<ProfilePackage>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertContent(content: PackageContent)

    @Delete
    fun deleteContent(content: PackageContent)

    @Transaction
    fun saveAllContents(packageId: Int, contents: List<PackageContent>) {
        clearContentsForPackage(packageId)
        contents.forEach { content ->
            insertContent(content.copy(packageId = packageId))
        }
    }

    @Query("DELETE FROM package_contents WHERE packageId = :packageId")
    fun clearContentsForPackage(packageId: Int)

    @Transaction
    @Query("SELECT * FROM profile_packages WHERE id = :packageId")
    fun getPackageWithContents(packageId: Int): Flow<PackageWithContents>

    @Delete
    fun deletePackage(profilePackage: ProfilePackage)
}