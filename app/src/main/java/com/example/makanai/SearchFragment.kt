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
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class SearchFragment : Fragment(R.layout.fragment_search) {

    private lateinit var searchTitle: TextView
    private lateinit var searchEditText: EditText
    private lateinit var filterButton: ImageButton
    private lateinit var searchRecyclerView: RecyclerView
    private lateinit var recipeAdapter: PopularRecipeAdapter

    // Use a local list to store the data fetched from Firestore
    private var allRecipes = listOf<Recipe>()
    private val db = Firebase.firestore

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        searchTitle = view.findViewById(R.id.search_title)
        searchEditText = view.findViewById(R.id.search_edit_text)
        filterButton = view.findViewById(R.id.filter_button)
        searchRecyclerView = view.findViewById(R.id.search_results_recycler_view)

        // 1. Setup Adapter (Initially empty)
        recipeAdapter = PopularRecipeAdapter(
            allRecipes,
            onRecipeClicked = { recipe ->
                val action = SearchFragmentDirections.actionSearchFragmentToRecipeDetailFragment(recipe.id)
                findNavController().navigate(action)
            },
            onLikeClicked = {
                // You can copy the toggleLike function here if you want searching to support liking
                // Or leave empty for now: {}
            }
        )
        searchRecyclerView.adapter = recipeAdapter

        // 2. Fetch Real Data from Firestore
        fetchRecipes()

        // 3. Search Listener
        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun afterTextChanged(s: Editable?) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterList(s.toString())
            }
        })

        // 4. Filter Button
        filterButton.setOnClickListener {
            val filterSheet = FilterBottomSheetFragment()
            filterSheet.show(childFragmentManager, filterSheet.tag)
        }
    }

    private fun fetchRecipes() {
        db.collection("recipes").get()
            .addOnSuccessListener { result ->
                val recipeList = mutableListOf<Recipe>()
                for (document in result) {
                    try {
                        // Convert Firestore doc to Recipe object
                        val recipe = document.toObject(Recipe::class.java)
                        // Ensure the ID is set from the document ID
                        recipe.id = document.id
                        recipeList.add(recipe)
                    } catch (e: Exception) {
                        Log.e("SearchFragment", "Error parsing recipe", e)
                    }
                }
                // Store in our local variable so we can filter it later
                allRecipes = recipeList
                // Update the UI
                recipeAdapter.updateData(allRecipes)
            }
            .addOnFailureListener { exception ->
                Log.e("SearchFragment", "Error getting documents.", exception)
            }
    }

    private fun filterList(query: String) {
        val normalizedQuery = query.lowercase().trim()

        val filteredList = if (normalizedQuery.isEmpty()) {
            allRecipes // Show all if search is empty
        } else {
            // Filter the local list
            allRecipes.filter { recipe ->
                recipe.title.lowercase().contains(normalizedQuery) ||
                        recipe.description.lowercase().contains(normalizedQuery) ||
                        recipe.ingredients.any { ingredient ->
                            ingredient.lowercase().contains(normalizedQuery)
                        }
            }
        }

        // Update the adapter
        recipeAdapter.updateData(filteredList)
    }
}