package com.example.makanai

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import coil.load
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class EditProfileFragment : Fragment(R.layout.fragment_edit_profile) {

    private var selectedImageUri: Uri? = null
    private lateinit var profileImageView: ImageView
    private lateinit var saveButtonBottom: Button
    private lateinit var saveButtonTop: Button

    // Image Picker
    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            selectedImageUri = uri
            // Use Coil to load it safely in the background
            profileImageView.load(uri) {
                crossfade(true)
                placeholder(R.drawable.ic_profile) // Show this while loading
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // --- 1. Find Views ---
        val backButton = view.findViewById<ImageButton>(R.id.back_button_edit)
        saveButtonTop = view.findViewById<Button>(R.id.save_button_top)
        saveButtonBottom = view.findViewById<Button>(R.id.save_button_bottom)

        profileImageView = view.findViewById(R.id.edit_profile_image)
        val changePhotoButton = view.findViewById<View>(R.id.change_photo_button)

        val nameInput = view.findViewById<TextInputEditText>(R.id.edit_name_input)
        val emailInput = view.findViewById<TextInputEditText>(R.id.edit_email_input)
        val bioInput = view.findViewById<TextInputEditText>(R.id.edit_bio_input)
        val locationInput = view.findViewById<TextInputEditText>(R.id.edit_location_input)

        val dateText = view.findViewById<TextView>(R.id.info_created_date)

        val auth = Firebase.auth
        val db = Firebase.firestore
        val storage = Firebase.storage
        val currentUser = auth.currentUser

        // --- 2. Load Current Data ---
        if (currentUser != null) {
            emailInput.setText(currentUser.email)

            db.collection("users").document(currentUser.uid).get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        nameInput.setText(document.getString("name"))
                        bioInput.setText(document.getString("bio"))
                        locationInput.setText(document.getString("location"))

                        val imgUrl = document.getString("profileImageUrl")
                        if (!imgUrl.isNullOrEmpty()) {
                            // Use Coil to load existing image
                            profileImageView.load(imgUrl) {
                                crossfade(true)
                                placeholder(R.drawable.ic_profile)
                            }
                        }

                        val createdAt = document.getLong("createdAt")
                        if (createdAt != null) {
                            val sdf = SimpleDateFormat("yyyy年MM月dd日", Locale.JAPAN)
                            dateText.text = sdf.format(Date(createdAt))
                        }
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(context, "Error loading data: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }

        // --- 3. Handle Actions ---

        backButton.setOnClickListener { findNavController().popBackStack() }

        changePhotoButton.setOnClickListener {
            try {
                pickImage.launch("image/*")
            } catch (e: Exception) {
                Toast.makeText(context, "Cannot open gallery", Toast.LENGTH_SHORT).show()
            }
        }

        // --- SAVE LOGIC ---
        fun saveProfile() {
            if (currentUser == null) return

            val newName = nameInput.text.toString()
            val newBio = bioInput.text.toString()
            val newLocation = locationInput.text.toString()

            // Disable buttons to prevent crashes from double clicking
            setButtonsEnabled(false)
            Toast.makeText(context, "Saving...", Toast.LENGTH_SHORT).show()

            // A. If user picked a new image, Upload it first
            if (selectedImageUri != null) {
                val ref = storage.reference.child("profile_images/${currentUser.uid}.jpg")

                ref.putFile(selectedImageUri!!)
                    .addOnSuccessListener {
                        // Image uploaded, now get the URL
                        ref.downloadUrl.addOnSuccessListener { uri ->
                            // Now save text + new image URL
                            updateFirestore(currentUser.uid, newName, newBio, newLocation, uri.toString())
                        }
                    }
                    .addOnFailureListener { e ->
                        setButtonsEnabled(true)
                        Toast.makeText(context, "Image upload failed: ${e.message}", Toast.LENGTH_SHORT).show()
                        Log.e("EditProfile", "Upload Error", e)
                    }
            } else {
                // B. No new image, just save text
                updateFirestore(currentUser.uid, newName, newBio, newLocation, null)
            }
        }

        saveButtonTop.setOnClickListener { saveProfile() }
        saveButtonBottom.setOnClickListener { saveProfile() }
    }

    private fun updateFirestore(uid: String, name: String, bio: String, location: String, imageUrl: String?) {
        val db = Firebase.firestore

        val updates = hashMapOf<String, Any>(
            "name" to name,
            "bio" to bio,
            "location" to location
        )
        // Only update image URL if a new one exists
        if (imageUrl != null) {
            updates["profileImageUrl"] = imageUrl
        }

        db.collection("users").document(uid).update(updates)
            .addOnSuccessListener {
                Toast.makeText(context, "Profile Updated!", Toast.LENGTH_SHORT).show()
                findNavController().popBackStack()
            }
            .addOnFailureListener { e ->
                setButtonsEnabled(true)
                Toast.makeText(context, "Update failed: ${e.message}", Toast.LENGTH_SHORT).show()
                Log.e("EditProfile", "Firestore Error", e)
            }
    }

    private fun setButtonsEnabled(isEnabled: Boolean) {
        saveButtonTop.isEnabled = isEnabled
        saveButtonBottom.isEnabled = isEnabled
        saveButtonTop.text = if (isEnabled) "保存" else "..."
        saveButtonBottom.text = if (isEnabled) "変更を保存" else "保存中..."
    }
}