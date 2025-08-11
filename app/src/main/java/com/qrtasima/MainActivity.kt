package com.qrtasima

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.qrtasima.ui.contacts.ContactsFragment
import com.qrtasima.ui.profiles.ProfilesFragment
import com.qrtasima.ui.scanner.ScannerFragment

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val toolbar: MaterialToolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        val bottomNavigation: BottomNavigationView = findViewById(R.id.bottom_navigation)

        val profilesFragment = ProfilesFragment()
        val scannerFragment = ScannerFragment()
        val contactsFragment = ContactsFragment()

        setCurrentFragment(profilesFragment)

        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_profiles -> setCurrentFragment(profilesFragment)
                R.id.navigation_scanner -> setCurrentFragment(scannerFragment)
                R.id.navigation_contacts -> setCurrentFragment(contactsFragment)
            }
            true
        }
    }

    private fun setCurrentFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction().apply {
            replace(R.id.fragment_container, fragment)
            commit()
        }
    }
}