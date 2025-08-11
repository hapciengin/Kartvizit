package com.qrtasima
import android.app.Application
import com.qrtasima.data.AppDatabase
import com.qrtasima.util.ThemeManager

class QrTasimaApplication : Application() {
    val database: AppDatabase by lazy { AppDatabase.getDatabase(this) }

    override fun onCreate() {
        super.onCreate()
        ThemeManager.applyTheme(this)
    }
}