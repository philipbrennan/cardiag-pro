package com.cardiag.pro.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.cardiag.pro.R
import com.cardiag.pro.databinding.ActivityMainBinding
import com.cardiag.pro.ui.connection.ConnectionFragment
import com.cardiag.pro.ui.diagnostic.DiagnosticFragment
import com.cardiag.pro.ui.logs.LogsFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        // Set up bottom navigation
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_connection -> {
                    loadFragment(ConnectionFragment())
                    true
                }
                R.id.navigation_diagnostic -> {
                    loadFragment(DiagnosticFragment())
                    true
                }
                R.id.navigation_logs -> {
                    loadFragment(LogsFragment())
                    true
                }
                else -> false
            }
        }

        // Load default fragment
        if (savedInstanceState == null) {
            loadFragment(ConnectionFragment())
            binding.bottomNavigation.selectedItemId = R.id.navigation_connection
        }
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }
}
