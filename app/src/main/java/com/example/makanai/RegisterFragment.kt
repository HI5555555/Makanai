package com.example.makanai

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView // Import TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class RegisterFragment : Fragment(R.layout.fragment_register) {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize Firebase
        auth = Firebase.auth
        db = Firebase.firestore

        val nameInput = view.findViewById<TextInputEditText>(R.id.name_edit_text)
        val emailInput = view.findViewById<TextInputEditText>(R.id.reg_email_edit_text)
        val passwordInput = view.findViewById<TextInputEditText>(R.id.reg_password_edit_text)
        val registerButton = view.findViewById<Button>(R.id.register_button)

        // Find links and back buttons
        val loginLink = view.findViewById<TextView>(R.id.login_link)
        val backButton = view.findViewById<View>(R.id.register_back_button)

        // Handle Register Click
        registerButton.setOnClickListener {
            val name = nameInput.text.toString().trim()
            val email = emailInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()

            if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Toast.makeText(context, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 1. Create User in Firebase Auth
            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        // 2. Save User Name to Firestore
                        val userId = auth.currentUser!!.uid
                        val userMap = hashMapOf(
                            "uid" to userId,
                            "name" to name,
                            "email" to email,
                            "createdAt" to System.currentTimeMillis()
                        )

                        db.collection("users").document(userId).set(userMap)
                            .addOnSuccessListener {
                                Toast.makeText(context, "Account created!", Toast.LENGTH_SHORT).show()
                                // Navigate to Home
                                findNavController().navigate(R.id.action_loginFragment_to_homeFragment)
                                // Note: We navigate to Home, effectively logging them in
                            }
                            .addOnFailureListener {
                                Toast.makeText(context, "Failed to save user data", Toast.LENGTH_SHORT).show()
                            }
                    } else {
                        Toast.makeText(context, "Registration failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                    }
                }
        }

        // Navigation
        loginLink.setOnClickListener {
            findNavController().popBackStack()
        }
        backButton.setOnClickListener {
            findNavController().popBackStack()
        }
    }
}