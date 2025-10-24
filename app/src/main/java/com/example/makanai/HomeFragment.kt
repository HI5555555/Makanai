package com.example.makanai

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView

import com.example.makanai.Recipe

class HomeFragment : Fragment(R.layout.fragment_home) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // --- 1. Setup Categories List (This code is fine) ---
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


        // --- 2. Setup Popular Recipes List (UPDATED) ---
        val popularRecyclerView: RecyclerView = view.findViewById(R.id.popular_recycler_view)

        // Get the recipes from our new repository
        val popularRecipes = RecipeRepository.getRecipes()

        val popularRecipeAdapter = PopularRecipeAdapter(popularRecipes) { recipe ->
            // This navigation code is already correct
            val action = HomeFragmentDirections.actionHomeFragmentToRecipeDetailFragment(recipe.id)
            findNavController().navigate(action)
        }

        popularRecyclerView.adapter = popularRecipeAdapter
    }
}