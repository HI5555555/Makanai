package com.example.makanai

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
// Make sure this import matches your package name in build.gradle
import com.example.makanai.R

class LoginFragment : Fragment(R.layout.fragment_login) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. Navigate to Home
        view.findViewById<Button>(R.id.login_button).setOnClickListener {
            findNavController().navigate(R.id.action_loginFragment_to_homeFragment)
        }

        // 2. Navigate to Register
        view.findViewById<TextView>(R.id.register_link).setOnClickListener {
            findNavController().navigate(R.id.action_loginFragment_to_registerFragment)
        }
    }
}