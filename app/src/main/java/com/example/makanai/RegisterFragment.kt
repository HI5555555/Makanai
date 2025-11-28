package com.example.makanai

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController

class RegisterFragment : Fragment(R.layout.fragment_register) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<View>(R.id.register_back_button).setOnClickListener {
            findNavController().popBackStack()
        }

        view.findViewById<TextView>(R.id.login_link).setOnClickListener {
            findNavController().popBackStack() // Go back to login
        }

        view.findViewById<Button>(R.id.register_button).setOnClickListener {
            // Logic for registration will go here later
        }
    }
}