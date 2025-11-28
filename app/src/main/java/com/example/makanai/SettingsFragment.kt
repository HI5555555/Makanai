package com.example.makanai

import android.content.pm.PackageManager
import android.graphics.Color // Make sure this is imported
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.example.makanai.R

class SettingsFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // --- Find Views ---
        val backButton: ImageButton = view.findViewById(R.id.back_button_settings)
        val username: TextView = view.findViewById(R.id.settings_username)
        val userEmail: TextView = view.findViewById(R.id.settings_user_email)
        val actionButtonLayout: LinearLayout = view.findViewById(R.id.logout_button_layout)
        val actionButtonText: TextView = actionButtonLayout.getChildAt(1) as TextView
        val actionButtonIcon: ImageView = actionButtonLayout.getChildAt(0) as ImageView
        val versionText: TextView = view.findViewById(R.id.version_text)

        // --- Check Login Status ---
        val currentUser = Firebase.auth.currentUser
        val db = Firebase.firestore

        if (currentUser != null) {
            // === LOGGED IN STATE ===
            userEmail.text = currentUser.email

            db.collection("users").document(currentUser.uid).get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        username.text = document.getString("name") ?: "User"
                    } else {
                        username.text = "User"
                    }
                }

            // Set Button to "Logout" (Red)
            actionButtonText.text = "ログアウト"
            actionButtonText.setTextColor(Color.parseColor("#B00020")) // Fixed: Use Hex Color
            actionButtonIcon.setImageResource(R.drawable.ic_settings_logout)
            actionButtonIcon.setColorFilter(Color.parseColor("#B00020")) // Fixed: Use Hex Color

            // Logout Logic
            actionButtonLayout.setOnClickListener {
                Firebase.auth.signOut()
                Toast.makeText(context, "Logged out", Toast.LENGTH_SHORT).show()
                findNavController().navigate(R.id.action_settingsFragment_to_loginFragment)
            }

        } else {
            // === GUEST / NOT LOGGED IN STATE ===
            username.text = "Guest"
            userEmail.text = "Not logged in"

            // Set Button to "Login" (Blue)
            actionButtonText.text = "ログイン"
            actionButtonText.setTextColor(Color.parseColor("#1976D2")) // Fixed: Use Hex Color
            actionButtonIcon.setImageResource(R.drawable.ic_settings_logout)
            actionButtonIcon.setColorFilter(Color.parseColor("#1976D2")) // Fixed: Use Hex Color

            // Login Logic
            actionButtonLayout.setOnClickListener {
                findNavController().navigate(R.id.action_settingsFragment_to_loginFragment)
            }
        }

        // --- Get App Version ---
        try {
            val packageInfo = requireContext().packageManager.getPackageInfo(requireContext().packageName, 0)
            versionText.text = "Version ${packageInfo.versionName}"
        } catch (e: Exception) {
            versionText.text = "Version 1.0"
        }

        // --- Setup Included Settings Items ---
        setupSettingsItem(view.findViewById(R.id.item_edit_profile), R.drawable.ic_settings_profile, "プロフィールを編集") { showToast("Edit Profile") }
        setupSettingsItem(view.findViewById(R.id.item_change_password), R.drawable.ic_settings_password, "パスワードを変更") { showToast("Change Password") }
        setupSettingsItem(view.findViewById(R.id.item_privacy), R.drawable.ic_settings_privacy, "プライバシー") { showToast("Privacy") }

        setupSettingsItem(view.findViewById(R.id.item_notifications), R.drawable.ic_settings_notifications, "通知設定") { showToast("Notifications") }
        setupSettingsItem(view.findViewById(R.id.item_language), R.drawable.ic_settings_language, "言語", "日本語") { showToast("Language") }
        setupSettingsItem(view.findViewById(R.id.item_theme), R.drawable.ic_settings_theme, "テーマ", "ライト") { showToast("Theme") }

        setupSettingsItem(view.findViewById(R.id.item_help), R.drawable.ic_settings_help, "ヘルプセンター") { showToast("Help") }
        setupSettingsItem(view.findViewById(R.id.item_terms), R.drawable.ic_settings_terms, "利用規約") { showToast("Terms") }
        setupSettingsItem(view.findViewById(R.id.item_policy), R.drawable.ic_settings_policy, "プライバシーポリシー") { showToast("Policy") }


        // --- Back Button Logic ---
        backButton.setOnClickListener {
            findNavController().popBackStack()
        }
    }

    private fun setupSettingsItem(
        itemView: ConstraintLayout?,
        iconResId: Int,
        title: String,
        value: String? = null,
        onClickAction: (() -> Unit)? = null
    ) {
        itemView?.let { layout ->
            val iconView: ImageView = layout.findViewById(R.id.settings_item_icon)
            val titleView: TextView = layout.findViewById(R.id.settings_item_title)
            val valueView: TextView = layout.findViewById(R.id.settings_item_value)
            val arrowView: ImageView = layout.findViewById(R.id.settings_item_arrow)

            iconView.setImageResource(iconResId)
            titleView.text = title

            if (value != null) {
                valueView.text = value
                valueView.visibility = View.VISIBLE
            } else {
                valueView.visibility = View.GONE
            }

            arrowView.visibility = View.VISIBLE
            layout.setOnClickListener { onClickAction?.invoke() }
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
}