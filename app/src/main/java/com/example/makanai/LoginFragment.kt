package com.example.makanai // Make sure this matches your actual package

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
// IMPORTANT: This import allows you to use R.id
import com.example.makanai.R

class LoginFragment : Fragment(R.layout.fragment_login) {

    private lateinit var auth: FirebaseAuth

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize Firebase Auth
        auth = Firebase.auth

        val emailInput = view.findViewById<TextInputEditText>(R.id.email_edit_text)
        val passwordInput = view.findViewById<TextInputEditText>(R.id.password_edit_text)
        val loginButton = view.findViewById<Button>(R.id.login_button)
        val registerLink = view.findViewById<TextView>(R.id.register_link)

        // --- Handle Login Click ---
        loginButton.setOnClickListener {
            val email = emailInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(context, "Please enter email and password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Sign in with Firebase
            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(context, "Welcome back!", Toast.LENGTH_SHORT).show()

                        // FIX: Use R.id directly to navigate to Home
                        findNavController().navigate(R.id.action_loginFragment_to_homeFragment)
                    } else {
                        Toast.makeText(context, "Login failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                    }
                }
        }

        // --- Handle Register Link Click ---
        registerLink.setOnClickListener {
            // FIX: Use R.id directly to navigate to Register
            findNavController().navigate(R.id.action_loginFragment_to_registerFragment)
        }
    }
}