package com.example.makanai

import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout // Import ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController

class SettingsFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // --- Find Top Level Views ---
        val backButton: ImageButton = view.findViewById(R.id.back_button_settings)
        val userImage: ImageView = view.findViewById(R.id.settings_user_image)
        val username: TextView = view.findViewById(R.id.settings_username)
        val userEmail: TextView = view.findViewById(R.id.settings_user_email)
        val logoutButtonLayout: LinearLayout = view.findViewById(R.id.logout_button_layout)
        val versionText: TextView = view.findViewById(R.id.version_text)

        // --- Set Placeholder User Data ---
        username.text = "Admin User"
        userEmail.text = "admin@example.com"
        try {
            val packageInfo = requireContext().packageManager.getPackageInfo(requireContext().packageName, 0)
            versionText.text = "Version ${packageInfo.versionName}"
        } catch (e: PackageManager.NameNotFoundException) {
            versionText.text = "Version ---"
        }

        // --- Setup Included Settings Items (using the helper function) ---
        setupSettingsItem(view.findViewById(R.id.item_edit_profile), R.drawable.ic_settings_profile, "プロフィールを編集") { navigateToEditProfile() }
        setupSettingsItem(view.findViewById(R.id.item_change_password), R.drawable.ic_settings_password, "パスワードを変更") { showToast("Change Password Clicked") }
        setupSettingsItem(view.findViewById(R.id.item_privacy), R.drawable.ic_settings_privacy, "プライバシー") { showToast("Privacy Clicked") }
        setupSettingsItem(view.findViewById(R.id.item_notifications), R.drawable.ic_settings_notifications, "通知設定") { showToast("Notifications Clicked") }
        setupSettingsItem(view.findViewById(R.id.item_language), R.drawable.ic_settings_language, "言語", "日本語") { showToast("Language Clicked") }
        setupSettingsItem(view.findViewById(R.id.item_theme), R.drawable.ic_settings_theme, "テーマ", "ライト") { showToast("Theme Clicked") }
        setupSettingsItem(view.findViewById(R.id.item_help), R.drawable.ic_settings_help, "ヘルプセンター") { showToast("Help Center Clicked") }
        setupSettingsItem(view.findViewById(R.id.item_terms), R.drawable.ic_settings_terms, "利用規約") { showToast("Terms Clicked") }
        setupSettingsItem(view.findViewById(R.id.item_policy), R.drawable.ic_settings_policy, "プライバシーポリシー") { showToast("Policy Clicked") }

        // --- Setup Other Click Listeners ---
        backButton.setOnClickListener { findNavController().popBackStack() }
        logoutButtonLayout.setOnClickListener {
            showToast("Logout Clicked")
            // TODO: Logout logic
        }
    }

    // Helper function (now works correctly because item_settings.xml root is ConstraintLayout)
    private fun setupSettingsItem(
        itemView: ConstraintLayout?, // Finding ConstraintLayout by include ID
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
                arrowView.visibility = View.VISIBLE
            } else {
                valueView.visibility = View.GONE
                arrowView.visibility = View.VISIBLE
            }

            layout.setOnClickListener { onClickAction?.invoke() }
        }
    }

    private fun navigateToEditProfile() {
        showToast("Navigate to Edit Profile (Screen not yet created)")
        // findNavController().navigate(R.id.action_settingsFragment_to_editProfileFragment)
    }

    private fun showToast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
}