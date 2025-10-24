package com.example.makanai

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView

class HomeFragment : Fragment(R.layout.fragment_home) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // --- 1. Setup Categories List ---

        // Find the RecyclerView for categories
        val categoriesRecyclerView: RecyclerView = view.findViewById(R.id.categories_recycler_view)

        // Create dummy data for categories
        val categories = listOf(
            Category("朝食", R.drawable.ic_breakfast),
            Category("昼食", R.drawable.ic_lunch),
            Category("夕食", R.drawable.ic_dinner),
            Category("軽食", R.drawable.ic_snack),
            Category("デザート", R.drawable.ic_dessert)
        )

        // Create and set the adapter
        val categoryAdapter = CategoryAdapter(categories)
        categoriesRecyclerView.adapter = categoryAdapter


        // --- 2. Setup Popular Recipes List ---

        // Find the RecyclerView for popular recipes
        val popularRecyclerView: RecyclerView = view.findViewById(R.id.popular_recycler_view)

        // Create dummy data for popular recipes
        val popularRecipes = listOf(
            Recipe(1, "Classic Granola Bowl", "A perfect breakfast", "", R.drawable.img_granola),
            Recipe(2, "Mediterranean Bowl", "Healthy and colorful", "", R.drawable.img_buddha_bowl),
            Recipe(3, "Fruit Salad", "Refreshing and delicious", "", R.drawable.img_fruit_salad),
            // Add more recipes as needed
        )

        // Create and set the adapter
        val popularRecipeAdapter = PopularRecipeAdapter(popularRecipes)
        popularRecyclerView.adapter = popularRecipeAdapter
    }
}
