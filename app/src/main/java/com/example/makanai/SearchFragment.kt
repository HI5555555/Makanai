package com.example.makanai

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.ImageButton // Import ImageButton
import android.widget.TextView // Import TextView if not already present
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView

class SearchFragment : Fragment(R.layout.fragment_search) {

    private lateinit var searchTitle: TextView // New: for the title
    private lateinit var searchEditText: EditText
    private lateinit var filterButton: ImageButton // New: for the filter button
    private lateinit var searchRecyclerView: RecyclerView
    private lateinit var recipeAdapter: PopularRecipeAdapter

    // Get all recipes from our single source of truth
    private val allRecipes = RecipeRepository.getRecipes()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        searchTitle = view.findViewById(R.id.search_title) // Initialize title
        searchEditText = view.findViewById(R.id.search_edit_text)
        filterButton = view.findViewById(R.id.filter_button) // Initialize filter button
        searchRecyclerView = view.findViewById(R.id.search_results_recycler_view)

        // Setup the adapter. It will navigate to the detail screen when clicked.
        recipeAdapter = PopularRecipeAdapter(allRecipes) { recipe ->
            // Use the action we created in the nav_graph.xml
            val action = SearchFragmentDirections.actionSearchFragmentToRecipeDetailFragment(recipe.id)
            findNavController().navigate(action)
        }
        searchRecyclerView.adapter = recipeAdapter

        // Add a listener to the search bar to filter the list
        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun afterTextChanged(s: Editable?) {} // Correct signature

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterList(s.toString())
            }
        })

        // (Optional) Set up a click listener for the filter button
        filterButton.setOnClickListener {
            // For now, it does nothing. Later, you could open a filter dialog.
            // Toast.makeText(context, "Filter button clicked!", Toast.LENGTH_SHORT).show()
            val filterSheet = FilterBottomSheetFragment()
            filterSheet.show(childFragmentManager, filterSheet.tag)
            // To match your example image, you'll open a filter bottom sheet here later.
            // For now, we can just leave it empty or show a Toast.
        }

        // Show all recipes initially when the fragment loads
        filterList("")
    }

    private fun filterList(query: String) {
        val normalizedQuery = query.lowercase().trim()

        val filteredList = if (normalizedQuery.isEmpty()) {
            allRecipes // Show all if search is empty or if no query is entered
        } else {
            // Search logic: check title, description, and ingredients
            allRecipes.filter { recipe ->
                recipe.title.lowercase().contains(normalizedQuery) ||
                        recipe.mainDescription.lowercase().contains(normalizedQuery) ||
                        recipe.ingredients.any { ingredient ->
                            ingredient.lowercase().contains(normalizedQuery)
                        }
            }
        }

        // Use the adapter method to update the list
        recipeAdapter.updateData(filteredList)
    }
}