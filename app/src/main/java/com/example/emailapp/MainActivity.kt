package com.example.emailapp

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    // 1. Create the fragment instances once so they stay "alive"
    private val senderFragment = SenderFragment()
    private val receiverFragment = ReceiverFragment()
    private var activeFragment: Fragment = senderFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)

        // 2. Add both fragments to the container, but hide the Receiver at first
        supportFragmentManager.beginTransaction().apply {
            add(R.id.fragment_container, receiverFragment, "RECEIVER").hide(receiverFragment)
            add(R.id.fragment_container, senderFragment, "SENDER")
            commit()
        }

        // 3. Switch between them without destroying them
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_sender -> {
                    showFragment(senderFragment)
                    true
                }
                R.id.nav_receiver -> {
                    showFragment(receiverFragment)
                    true
                }
                else -> false
            }
        }
    }

    private fun showFragment(fragment: Fragment) {
        if (fragment != activeFragment) {
            supportFragmentManager.beginTransaction()
                .hide(activeFragment) // Hide the current one
                .show(fragment)       // Show the new one
                .commit()
            activeFragment = fragment
        }
    }
}