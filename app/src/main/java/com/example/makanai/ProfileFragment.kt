package com.example.makanai

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.google.android.material.tabs.TabLayout
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class ProfileFragment : Fragment() {

    private lateinit var recipeAdapter: PopularRecipeAdapter
    private val db = Firebase.firestore
    private val auth = Firebase.auth
    private var myRecipesList = listOf<Recipe>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Find views (using nullable types '?' to be safe)
        val profileImage = view.findViewById<ImageView>(R.id.profile_image)
        val usernameText = view.findViewById<TextView>(R.id.username_text_new)
        val userDescText = view.findViewById<TextView>(R.id.user_desc_text)
        val recipesCount = view.findViewById<TextView>(R.id.recipes_count)
        val settingsButton = view.findViewById<ImageButton>(R.id.settings_button_new)
        val tabLayout = view.findViewById<TabLayout>(R.id.tab_layout)
        val profileRecipesRecyclerView = view.findViewById<RecyclerView>(R.id.profile_recipes_recycler_view)

        // Setup Settings Button
        settingsButton?.setOnClickListener {
            findNavController().navigate(R.id.action_profileFragment_to_settingsFragment)
        }

        // --- Setup RecyclerView ---
        recipeAdapter = PopularRecipeAdapter(emptyList()) { recipe ->
            try {
                val action = ProfileFragmentDirections.actionProfileFragmentToRecipeDetailFragment(recipe.id)
                findNavController().navigate(action)
            } catch (e: Exception) {
                // Fallback if Safe Args fails or action is missing
                Log.e("ProfileFragment", "Navigation failed", e)
            }
        }

        if (profileRecipesRecyclerView != null) {
            profileRecipesRecyclerView.adapter = recipeAdapter
            profileRecipesRecyclerView.isNestedScrollingEnabled = false
        }

        // --- Load User Data ---
        val currentUser = auth.currentUser

        if (currentUser != null) {
            // 1. Load Profile Info
            db.collection("users").document(currentUser.uid).get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        usernameText?.text = document.getString("name") ?: "User"
                        userDescText?.text = document.getString("bio") ?: "No bio yet"

                        val imgUrl = document.getString("profileImageUrl")
                        if (!imgUrl.isNullOrEmpty() && profileImage != null) {
                            profileImage.load(imgUrl)
                        }
                    }
                }

            // 2. Load User's Recipes
            db.collection("recipes")
                .whereEqualTo("authorId", currentUser.uid)
                .get()
                .addOnSuccessListener { result ->
                    val list = mutableListOf<Recipe>()
                    for (document in result) {
                        try {
                            val recipe = document.toObject(Recipe::class.java)
                            recipe.id = document.id
                            list.add(recipe)
                        } catch (e: Exception) {
                            Log.e("Profile", "Error parsing recipe", e)
                        }
                    }
                    myRecipesList = list
                    recipesCount?.text = list.size.toString()

                    // Update list if on the first tab
                    if (tabLayout?.selectedTabPosition == 0) {
                        recipeAdapter.updateData(myRecipesList)
                    }
                }
        } else {
            // Guest Mode
            usernameText?.text = "Guest"
            userDescText?.text = "Please login"
            recipesCount?.text = "0"
        }

        // --- Tab Listener ---
        tabLayout?.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> recipeAdapter.updateData(myRecipesList)
                    1 -> recipeAdapter.updateData(emptyList())
                }
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }
}