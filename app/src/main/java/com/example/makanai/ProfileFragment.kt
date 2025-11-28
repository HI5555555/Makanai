package com.example.makanai

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.tabs.TabLayout

class ProfileFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Find views
        val profileImage: ImageView = view.findViewById(R.id.profile_image)
        val usernameText: TextView = view.findViewById(R.id.username_text_new)
        val userDescText: TextView = view.findViewById(R.id.user_desc_text)
        val recipesCount: TextView = view.findViewById(R.id.recipes_count)
        val followersCount: TextView = view.findViewById(R.id.followers_count)
        val followingCount: TextView = view.findViewById(R.id.following_count)
        val settingsButton: ImageButton = view.findViewById(R.id.settings_button_new) // Correct ID
        val descLine1: TextView = view.findViewById(R.id.desc_line_1)
        val descLine2: TextView = view.findViewById(R.id.desc_line_2)
        val descLine3: TextView = view.findViewById(R.id.desc_line_3)
        val tabLayout: TabLayout = view.findViewById(R.id.tab_layout)
        val profileRecipesRecyclerView: RecyclerView = view.findViewById(R.id.profile_recipes_recycler_view)

        // --- Set placeholder data (replace with real data later) ---
        // profileImage.setImageResource(...)
        usernameText.text = "Exz"
        userDescText.text = "Home Chef Enthusiast"
        recipesCount.text = "2"
        followersCount.text = "1.2k"
        followingCount.text = "342"
        descLine1.text = "Passionate about cooking and sharing delicious recipes"
        descLine2.text = "Focus on healthy, wholesome meals"
        descLine3.text = "San Francisco, CA"


        // --- Setup RecyclerView ---
        val myRecipes = RecipeRepository.getRecipes() // Get all recipes for example
        val recipeAdapter = PopularRecipeAdapter(myRecipes) { recipe ->
            // val action = ProfileFragmentDirections.actionProfileFragmentToRecipeDetailFragment(recipe.id)
            // findNavController().navigate(action)
        }
        profileRecipesRecyclerView.adapter = recipeAdapter
        profileRecipesRecyclerView.isNestedScrollingEnabled = false


        // --- Setup Tab Layout Listener (Basic) ---
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> { // My Recipes tab
                        recipeAdapter.updateData(myRecipes)
                    }
                    1 -> { // Favorites tab
                        recipeAdapter.updateData(myRecipes.take(1))
                    }
                }
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        // --- Setup Settings Button ---
        settingsButton.setOnClickListener {
            // Add this line for debugging:
            android.widget.Toast.makeText(context, "Settings Clicked!", android.widget.Toast.LENGTH_SHORT).show()
            // Navigate to Settings Fragment using the action ID from nav_graph
            findNavController().navigate(R.id.action_profileFragment_to_settingsFragment)
        }
    }
}