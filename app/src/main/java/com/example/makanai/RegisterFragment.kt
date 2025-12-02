package com.example.makanai

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.ktx.storage
import com.google.firebase.ktx.Firebase
import coil.load
import coil.transform.CircleCropTransformation

class RegisterFragment : Fragment(R.layout.fragment_register) {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var storage: FirebaseStorage
    private var selectedImageUri: Uri? = null
    private lateinit var profileImageView: ImageView

    // Launcher to pick image from gallery
    private val pickImage = registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            selectedImageUri = uri
            profileImageView.load(uri) {
                crossfade(true)
                // Make it round like the final result
                transformations(coil.transform.CircleCropTransformation())
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = Firebase.auth
        db = Firebase.firestore
        storage = Firebase.storage

        val nameInput = view.findViewById<TextInputEditText>(R.id.name_edit_text)
        val emailInput = view.findViewById<TextInputEditText>(R.id.reg_email_edit_text)
        val passwordInput = view.findViewById<TextInputEditText>(R.id.reg_password_edit_text)
        val confirmPasswordInput = view.findViewById<TextInputEditText>(R.id.reg_confirm_password_edit_text)
        val registerButton = view.findViewById<Button>(R.id.register_button)
        profileImageView = view.findViewById(R.id.register_profile_image)

        // Image Picker Click
        view.findViewById<View>(R.id.profile_image_container).setOnClickListener {
            pickImage.launch("image/*")
        }

        registerButton.setOnClickListener {
            val name = nameInput.text.toString().trim()
            val email = emailInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()
            val confirmPassword = confirmPasswordInput.text.toString().trim()

            // Validation
            if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Toast.makeText(context, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (password != confirmPassword) {
                Toast.makeText(context, "Passwords do not match", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Disable button to prevent double clicks
            registerButton.isEnabled = false
            registerButton.text = "Creating Account..."

            // 1. Create Auth User
            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val userId = auth.currentUser!!.uid

                        // 2. Upload Image (if selected) then Save Data
                        if (selectedImageUri != null) {
                            uploadImageAndSaveData(userId, name, email)
                        } else {
                            saveUserDataToFirestore(userId, name, email, "")
                        }
                    } else {
                        registerButton.isEnabled = true
                        registerButton.text = "登録する"
                        Toast.makeText(context, "Error: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                    }
                }
        }

        // Nav logic...
        view.findViewById<View>(R.id.register_back_button).setOnClickListener { findNavController().popBackStack() }
        view.findViewById<View>(R.id.login_link).setOnClickListener { findNavController().popBackStack() }
    }

    private fun uploadImageAndSaveData(userId: String, name: String, email: String) {
        // Create a reference to "profile_images/USER_ID.jpg"
        val ref = storage.reference.child("profile_images/$userId.jpg")

        ref.putFile(selectedImageUri!!)
            .addOnSuccessListener {
                // Get the download URL
                ref.downloadUrl.addOnSuccessListener { uri ->
                    saveUserDataToFirestore(userId, name, email, uri.toString())
                }
            }
            .addOnFailureListener {
                Toast.makeText(context, "Failed to upload image", Toast.LENGTH_SHORT).show()
                // Still save user data even if image fails
                saveUserDataToFirestore(userId, name, email, "")
            }
    }

    private fun saveUserDataToFirestore(userId: String, name: String, email: String, imageUrl: String) {
        val userMap = hashMapOf(
            "uid" to userId,
            "name" to name,
            "email" to email,
            "profileImageUrl" to imageUrl,
            "createdAt" to System.currentTimeMillis()
        )

        db.collection("users").document(userId).set(userMap)
            .addOnSuccessListener {
                Toast.makeText(context, "Registration successful!", Toast.LENGTH_LONG).show()
                auth.signOut()
                findNavController().popBackStack() // Go back to Login
            }
    }
}