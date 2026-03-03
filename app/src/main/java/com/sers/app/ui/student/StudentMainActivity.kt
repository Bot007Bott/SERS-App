package com.sers.app.ui.student

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.sers.app.databinding.ActivityStudentMainBinding
import com.sers.app.R
import android.content.Intent
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.sers.app.ui.auth.LoginActivity

class StudentMainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityStudentMainBinding
    private lateinit var appBarConfiguration: AppBarConfiguration

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStudentMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.studentNavHostFragment) as NavHostFragment
        val navController = navHostFragment.navController

        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.studentDashboardFragment,
                R.id.studentGradesFragment,
                R.id.studentAttendanceFragment,
                R.id.studentProfileFragment,
                R.id.studentReportFragment,
                R.id.studentSettingsFragment
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
        tvUserRole.text = "Student"
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid != null) {
            FirebaseFirestore.getInstance().collection("users").whereEqualTo("uid", uid).get()
                .addOnSuccessListener { userDocs ->
                    val studentId = userDocs.documents.firstOrNull()?.getString("studentId")
                    if (studentId != null) {
                        FirebaseFirestore.getInstance().collection("students")
                            .whereEqualTo("studentId", studentId).get()
                            .addOnSuccessListener { studentDocs ->
                                val student = studentDocs.documents.firstOrNull()
                                val fullName = "${student?.getString("firstName") ?: ""} ${student?.getString("lastName") ?: ""}".trim()
                                tvUserName.text = if (fullName.isNotEmpty()) fullName
                                else FirebaseAuth.getInstance().currentUser?.email ?: "Student"
                            }
                    } else {
                        tvUserName.text = FirebaseAuth.getInstance().currentUser?.email ?: "Student"
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
            .findFragmentById(R.id.studentNavHostFragment) as NavHostFragment
        return navHostFragment.navController.navigateUp(appBarConfiguration)
                || super.onSupportNavigateUp()
    }
}