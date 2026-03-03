package com.sers.app.ui.teacher

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.sers.app.R
import com.sers.app.databinding.ActivityTeacherMainBinding
import com.sers.app.ui.auth.LoginActivity

class TeacherMainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTeacherMainBinding
    private lateinit var appBarConfiguration: AppBarConfiguration

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTeacherMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.navHostFragment) as NavHostFragment
        val navController = navHostFragment.navController

        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.teacherDashboardFragment,
                R.id.teacherCoursesFragment,
                R.id.teacherGradesFragment,
                R.id.teacherAttendanceFragment,
                R.id.teacherAnalyticsFragment,
                R.id.teacherReportFragment,
                R.id.teacherSettingsFragment,
                R.id.teacherProfileFragment
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
        tvUserRole.text = "Teacher"
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid != null) {
            FirebaseFirestore.getInstance().collection("users").whereEqualTo("uid", uid).get()
                .addOnSuccessListener { userDocs ->
                    val teacherId = userDocs.documents.firstOrNull()?.getString("teacherId")
                    if (teacherId != null) {
                        FirebaseFirestore.getInstance().collection("teachers")
                            .whereEqualTo("teacherId", teacherId).get()
                            .addOnSuccessListener { teacherDocs ->
                                val teacher = teacherDocs.documents.firstOrNull()
                                val fullName = "${teacher?.getString("firstName") ?: ""} ${teacher?.getString("lastName") ?: ""}".trim()
                                tvUserName.text = if (fullName.isNotEmpty()) fullName
                                else FirebaseAuth.getInstance().currentUser?.email ?: "Teacher"
                            }
                    } else {
                        tvUserName.text = FirebaseAuth.getInstance().currentUser?.email ?: "Teacher"
                    }
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