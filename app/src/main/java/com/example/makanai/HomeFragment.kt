package com.example.makanai

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class HomeFragment : Fragment(R.layout.fragment_home) {

    private lateinit var popularRecipeAdapter: PopularRecipeAdapter
    private val db = Firebase.firestore

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. Setup Categories (Keep this logic as is for now)
        val categoriesRecyclerView: RecyclerView = view.findViewById(R.id.categories_recycler_view)
        val categories = listOf(
            Category("朝食", R.drawable.ic_breakfast),
            Category("昼食", R.drawable.ic_lunch),
            Category("夕食", R.drawable.ic_dinner),
            Category("軽食", R.drawable.ic_snack),
            Category("デザート", R.drawable.ic_dessert)
        )
        val categoryAdapter = CategoryAdapter(categories)
        categoriesRecyclerView.adapter = categoryAdapter

        // 2. Setup Popular Recipes Recycler
        val popularRecyclerView: RecyclerView = view.findViewById(R.id.popular_recycler_view)

        // Initialize with empty list
        // Initialize adapter
        popularRecipeAdapter = PopularRecipeAdapter(emptyList()) { recipe ->
            // --- FIX IS HERE ---
            // Pass the ID directly (it is already a String)
            val action = HomeFragmentDirections.actionHomeFragmentToRecipeDetailFragment(recipe.id)
            findNavController().navigate(action)
        }
        popularRecyclerView.adapter = popularRecipeAdapter


        // 3. Fetch Data from Firestore
        fetchRecipes()
    }

    private fun fetchRecipes() {
        db.collection("recipes")
            .orderBy("createdAt", Query.Direction.DESCENDING) // Show newest first
            .get()
            .addOnSuccessListener { result ->
                val recipeList = mutableListOf<Recipe>()
                for (document in result) {
                    try {
                        // Convert Firestore document to Recipe object
                        val recipe = document.toObject(Recipe::class.java)
                        // Set the ID manually because it's the document ID, not inside the data fields
                        recipe.id = document.id
                        recipeList.add(recipe)
                    } catch (e: Exception) {
                        Log.e("HomeFragment", "Error parsing recipe: ${e.message}")
                    }
                }
                // Update Adapter
                popularRecipeAdapter.updateData(recipeList)
            }
            .addOnFailureListener { exception ->
                Log.e("HomeFragment", "Error getting documents.", exception)
            }
    }
}