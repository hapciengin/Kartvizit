package com.qrtasima.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.qrtasima.data.PackageDao
import com.qrtasima.ui.profiles.detail.DetailViewModel

@Suppress("UNCHECKED_CAST")
class ViewModelFactory(private val dao: PackageDao) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(MainViewModel::class.java) -> MainViewModel(dao) as T
            modelClass.isAssignableFrom(DetailViewModel::class.java) -> DetailViewModel(dao) as T
            else -> throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}