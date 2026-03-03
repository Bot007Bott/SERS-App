package com.sers.app.ui.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.sers.app.databinding.FragmentAdminProfileBinding

class AdminProfileFragment : Fragment() {

    private lateinit var binding: FragmentAdminProfileBinding
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentAdminProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").whereEqualTo("uid", uid).get()
            .addOnSuccessListener { docs ->
                if (docs.isEmpty) return@addOnSuccessListener
                val doc = docs.documents[0]
                val firstName = doc.getString("firstName") ?: ""
                val lastName = doc.getString("lastName") ?: ""
                val email = doc.getString("email") ?: ""
                val phone = doc.getString("phone") ?: ""
                binding.tvName.text = "$firstName $lastName"
                binding.tvRole.text = "Administrator"
                binding.tvAvatar.text = firstName.firstOrNull()?.uppercase() ?: "A"
                binding.tvEmail.text = email
                binding.tvPhone.text = phone.ifEmpty { "Not set" }
                binding.tvRoleInfo.text = "Administrator"
            }
    }
}