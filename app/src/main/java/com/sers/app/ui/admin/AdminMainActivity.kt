package com.sers.app.ui.admin

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.sers.app.utils.ThemeHelper
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.sers.app.databinding.ActivityAdminMainBinding
import com.sers.app.R
import android.content.Intent
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.sers.app.ui.auth.LoginActivity

class AdminMainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAdminMainBinding
    private lateinit var appBarConfiguration: AppBarConfiguration

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeHelper.applySavedTheme(this)
        binding = ActivityAdminMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.navHostFragment) as NavHostFragment
        val navController = navHostFragment.navController

        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.dashboardFragment,
                R.id.userManagementFragment,
                R.id.studentManagementFragment,
                R.id.teacherManagementFragment,
                R.id.enrollmentFragment,
                R.id.coursesFragment,
                R.id.gradesFragment,
                R.id.attendanceFragment,
                R.id.analyticsFragment,
                R.id.adminReportFragment,
                R.id.adminSettingsFragment,
                R.id.adminProfileFragment
            ),
            binding.drawerLayout
        )

        setupActionBarWithNavController(navController, appBarConfiguration)
        binding.navigationView.setupWithNavController(navController)
        binding.navigationView.setNavigationItemSelectedListener { menuItem ->
            menuItem.isChecked = true
            binding.drawerLayout.closeDrawers()
            navController.navigate(menuItem.itemId)
            true
        }

        // Load nav header
        val headerView = binding.navigationView.getHeaderView(0)
        val tvUserName = headerView.findViewById<android.widget.TextView>(R.id.tvUserName)
        val tvUserRole = headerView.findViewById<android.widget.TextView>(R.id.tvUserRole)
        tvUserName.text = "Administrator"
        tvUserRole.text = "Admin"
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid != null) {
            FirebaseFirestore.getInstance().collection("users").whereEqualTo("uid", uid).get()
                .addOnSuccessListener { docs ->
                    val doc = docs.documents.firstOrNull()
                    tvUserName.text = doc?.getString("username") ?: "Administrator"
                    tvUserRole.text = "Admin"
                }
        }

        binding.btnLogout.setOnClickListener {
            MaterialAlertDialogBuilder(this)
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
                .setPositiveButton("Logout") { _, _ ->
                    FirebaseAuth.getInstance().signOut()
                    startActivity(Intent(this, LoginActivity::class.java))
                    finish()
                }
                .show()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.navHostFragment) as NavHostFragment
        val navController = navHostFragment.navController
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }
}