package com.example.makanai

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class SearchFragment : Fragment(R.layout.fragment_search) {

    private lateinit var searchTitle: TextView
    private lateinit var searchEditText: EditText
    private lateinit var filterButton: ImageButton
    private lateinit var searchRecyclerView: RecyclerView
    private lateinit var recipeAdapter: PopularRecipeAdapter

    private var allRecipes = listOf<Recipe>()
    private val db = Firebase.firestore
    private val auth = Firebase.auth

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        searchTitle = view.findViewById(R.id.search_title)
        searchEditText = view.findViewById(R.id.search_edit_text)
        filterButton = view.findViewById(R.id.filter_button)
        searchRecyclerView = view.findViewById(R.id.search_results_recycler_view)

        // 1. Setup Adapter with BOTH callbacks
        recipeAdapter = PopularRecipeAdapter(
            emptyList(),
            onRecipeClicked = { recipe ->
                try {
                    val action = SearchFragmentDirections.actionSearchFragmentToRecipeDetailFragment(recipe.id)
                    findNavController().navigate(action)
                } catch (e: Exception) {
                    Log.e("SearchFragment", "Navigation Error", e)
                }
            },
            onLikeClicked = { recipe ->
                // --- NEW: Call the like logic ---
                toggleLike(recipe)
            }
        )
        searchRecyclerView.adapter = recipeAdapter

        // ... (Fetch and Search logic remains the same) ...
        fetchRecipes()

        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun afterTextChanged(s: Editable?) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterList(s.toString())
            }
        })

        filterButton.setOnClickListener {
            val filterSheet = FilterBottomSheetFragment()
            filterSheet.show(childFragmentManager, filterSheet.tag)
        }
    }

    // ... (fetchRecipes and filterList remain the same) ...
    private fun fetchRecipes() {
        db.collection("recipes").get()
            .addOnSuccessListener { result ->
                val recipeList = mutableListOf<Recipe>()
                for (document in result) {
                    try {
                        val recipe = document.toObject(Recipe::class.java)
                        recipe.id = document.id
                        recipeList.add(recipe)
                    } catch (e: Exception) {
                        Log.e("SearchFragment", "Error parsing recipe", e)
                    }
                }
                allRecipes = recipeList
                recipeAdapter.updateData(allRecipes)
            }
    }

    private fun filterList(query: String) {
        val normalizedQuery = query.lowercase().trim()
        val filteredList = if (normalizedQuery.isEmpty()) {
            allRecipes
        } else {
            allRecipes.filter { recipe ->
                recipe.title.lowercase().contains(normalizedQuery) ||
                        recipe.description.lowercase().contains(normalizedQuery) ||
                        recipe.ingredients.any { ingredient ->
                            // Handle Map vs String (from previous fixes)
                            if (ingredient is Map<*, *>) {
                                (ingredient["name"] as? String)?.lowercase()?.contains(normalizedQuery) == true
                            } else {
                                ingredient.toString().lowercase().contains(normalizedQuery)
                            }
                        }
            }
        }
        recipeAdapter.updateData(filteredList)
    }

    // --- NEW: Add this function ---
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
            // Ideally refresh the list to show updated icon state immediately
            // Since Search doesn't have a realtime listener like Home, we might need to manually update the local list
            // But transactions usually trigger a refresh if you re-fetch or update local data.
            // For simplicity, let's just let the next search/refresh handle it or re-fetch here:
            fetchRecipes()
        }
    }
}