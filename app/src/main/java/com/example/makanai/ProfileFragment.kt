package com.example.makanai

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.google.android.material.tabs.TabLayout
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class ProfileFragment : Fragment() {

    private lateinit var recipeAdapter: PopularRecipeAdapter
    private val db = Firebase.firestore
    private val auth = Firebase.auth

    // Store lists locally to switch tabs quickly
    private var myRecipesList = listOf<Recipe>()
    private var favoriteRecipesList = listOf<Recipe>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Find views
        val profileImage = view.findViewById<ImageView>(R.id.profile_image)
        val usernameText = view.findViewById<TextView>(R.id.username_text_new)
        val userDescText = view.findViewById<TextView>(R.id.user_desc_text)
        val recipesCount = view.findViewById<TextView>(R.id.recipes_count)
        val settingsButton = view.findViewById<ImageButton>(R.id.settings_button_new)
        val tabLayout = view.findViewById<TabLayout>(R.id.tab_layout)
        val profileRecipesRecyclerView = view.findViewById<RecyclerView>(R.id.profile_recipes_recycler_view)

        // --- Setup Settings Button ---
        settingsButton?.setOnClickListener {
            findNavController().navigate(R.id.action_profileFragment_to_settingsFragment)
        }

        // --- Setup RecyclerView ---
        // Initialize adapter with BOTH callbacks (Click & Like)
        recipeAdapter = PopularRecipeAdapter(
            emptyList(),
            onRecipeClicked = { recipe ->
                try {
                    // Navigate to Detail Page
                    val action = ProfileFragmentDirections.actionProfileFragmentToRecipeDetailFragment(recipe.id)
                    findNavController().navigate(action)
                } catch (e: Exception) {
                    Log.e("ProfileFragment", "Navigation failed", e)
                }
            },
            onLikeClicked = { recipe ->
                // Handle Like (Now active!)
                toggleLike(recipe)
            }
        )

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

                        // Check if URL exists AND is not empty
                        if (!imgUrl.isNullOrEmpty()) {
                            // Load real image
                            if (profileImage != null) {
                                profileImage.load(imgUrl) {
                                    crossfade(true)
                                    transformations(coil.transform.CircleCropTransformation())
                                }
                            }
                        } else {
                            // FORCE load the default icon if no URL exists
                            profileImage?.setImageResource(R.drawable.ic_profile)
                        }
                    }
                }

            // 2. Load User's Own Recipes
            loadUserRecipes(currentUser.uid)

            // 3. Pre-load Favorites
            loadFavorites()

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
                    0 -> {
                        // My Recipes Tab
                        recipeAdapter.updateData(myRecipesList)
                    }
                    1 -> {
                        // Favorites Tab
                        loadFavorites()
                    }
                }
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun loadFavorites() {
        val currentUser = auth.currentUser ?: return

        // Query recipes where 'likedBy' array contains current user ID
        db.collection("recipes")
            .whereArrayContains("likedBy", currentUser.uid)
            .get()
            .addOnSuccessListener { result ->
                val list = mutableListOf<Recipe>()
                for (document in result) {
                    val recipe = document.toObject(Recipe::class.java)
                    recipe.id = document.id
                    list.add(recipe)
                }
                favoriteRecipesList = list

                // Only update adapter if the "Favorites" tab is currently selected (index 1)
                val tabLayout = view?.findViewById<TabLayout>(R.id.tab_layout)
                if (tabLayout?.selectedTabPosition == 1) {
                    recipeAdapter.updateData(favoriteRecipesList)
                }
            }
            .addOnFailureListener { e ->
                Log.e("ProfileFragment", "Error loading favorites", e)
            }
    }

    private fun toggleLike(recipe: Recipe) {
        val user = auth.currentUser ?: return
        val recipeRef = db.collection("recipes").document(recipe.id)

        db.runTransaction { transaction ->
            val snapshot = transaction.get(recipeRef)
            val currentLikes = snapshot.getLong("likes") ?: 0
            val likedBy = snapshot.get("likedBy") as? List<String> ?: emptyList()

            if (likedBy.contains(user.uid)) {
                // Unlike
                val newLikes = if (currentLikes > 0) currentLikes - 1 else 0
                transaction.update(recipeRef, "likes", newLikes)
                transaction.update(recipeRef, "likedBy", FieldValue.arrayRemove(user.uid))
            } else {
                // Like
                transaction.update(recipeRef, "likes", currentLikes + 1)
                transaction.update(recipeRef, "likedBy", FieldValue.arrayUnion(user.uid))
            }
        }.addOnSuccessListener {
            // Refresh the lists to reflect changes
            val tabLayout = view?.findViewById<TabLayout>(R.id.tab_layout)

            if (tabLayout?.selectedTabPosition == 1) {
                // If on Favorites tab, reload (unliked item will disappear)
                loadFavorites()
            } else {
                // If on My Recipes tab, reload to update like count UI
                loadUserRecipes(user.uid)
            }
        }
    }

    private fun loadUserRecipes(userId: String) {
        db.collection("recipes")
            .whereEqualTo("authorId", userId)
            .get()
            .addOnSuccessListener { result ->
                val list = mutableListOf<Recipe>()
                for (document in result) {
                    val recipe = document.toObject(Recipe::class.java)
                    recipe.id = document.id
                    list.add(recipe)
                }
                myRecipesList = list

                // Update count
                view?.findViewById<TextView>(R.id.recipes_count)?.text = list.size.toString()

                // Update adapter if on tab 0
                val tabLayout = view?.findViewById<TabLayout>(R.id.tab_layout)
                if (tabLayout?.selectedTabPosition == 0) {
                    recipeAdapter.updateData(myRecipesList)
                }
            }
    }
}