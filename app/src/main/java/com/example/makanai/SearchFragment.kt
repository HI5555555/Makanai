package com.example.makanai

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class SearchFragment : Fragment(R.layout.fragment_search) {

    private lateinit var searchEditText: EditText
    private lateinit var searchRecyclerView: RecyclerView
    private lateinit var recipeAdapter: PopularRecipeAdapter

    // Master list of all recipes fetched from Firestore
    private var allRecipes = listOf<Recipe>()

    private val db = Firebase.firestore
    private val auth = Firebase.auth

    private var currentQuery = ""

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. Initialize Views
        val searchTitle = view.findViewById<TextView>(R.id.search_title)
        searchEditText = view.findViewById(R.id.search_edit_text)
        searchRecyclerView = view.findViewById(R.id.search_results_recycler_view)

        // 2. Setup Adapter
        recipeAdapter = PopularRecipeAdapter(
            recipes = emptyList(),
            onRecipeClicked = { recipe ->
                try {
                    val action = SearchFragmentDirections.actionSearchFragmentToRecipeDetailFragment(recipe.id)
                    findNavController().navigate(action)
                } catch (e: Exception) {
                    Log.e("SearchFragment", "Navigation Error", e)
                }
            },
            onLikeClicked = { recipe ->
                toggleLike(recipe)
            }
        )
        searchRecyclerView.adapter = recipeAdapter

        // 3. Fetch Data
        fetchRecipes()

        // 4. Search Text Listener
        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun afterTextChanged(s: Editable?) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                currentQuery = s.toString()
                performSearch()
            }
        })
    }

    // Fetch all recipes once
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
                performSearch() // Show initial list
            }
            .addOnFailureListener { exception ->
                Log.e("SearchFragment", "Error getting documents.", exception)
            }
    }

    // Simple Text Search Logic
    private fun performSearch() {
        val query = currentQuery.lowercase().trim()

        var filteredList = allRecipes

        if (query.isNotEmpty()) {
            filteredList = filteredList.filter { recipe ->
                val titleMatch = recipe.title.lowercase().contains(query)
                val descMatch = recipe.description.lowercase().contains(query)

                // Check Ingredients (handling the Map structure)
                val ingredientMatch = recipe.ingredients.any { ingredientMap ->
                    // Safely get the name from the map
                    val name = ingredientMap["name"] ?: ""
                    name.lowercase().contains(query)
                }

                titleMatch || descMatch || ingredientMatch
            }
        }

        recipeAdapter.updateData(filteredList)
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
            fetchRecipes()
        }
    }
}