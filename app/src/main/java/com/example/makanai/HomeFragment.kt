package com.example.makanai

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2 // Required for Slider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class HomeFragment : Fragment(R.layout.fragment_home) {

    private lateinit var popularRecipeAdapter: PopularRecipeAdapter

    // --- Slider Variables ---
    private lateinit var specialAdapter: SpecialRecipeAdapter
    private lateinit var specialViewPager: ViewPager2
    private lateinit var specialTitle: TextView

    private val db = Firebase.firestore
    private val auth = Firebase.auth

    // Store all recipes locally so we can filter without re-downloading
    private var allRecipes = listOf<Recipe>()

    // Auto-slide Handler
    private val sliderHandler = Handler(Looper.getMainLooper())
    private val sliderRunnable = Runnable {
        // Check if initialized to prevent crashes
        if (::specialViewPager.isInitialized && specialAdapter.itemCount > 0) {
            var nextItem = specialViewPager.currentItem + 1
            if (nextItem >= specialAdapter.itemCount) {
                nextItem = 0
            }
            specialViewPager.currentItem = nextItem
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. Setup Categories with Click Listener
        val categoriesRecyclerView: RecyclerView = view.findViewById(R.id.categories_recycler_view)
        val categories = listOf(
            Category("朝食", R.drawable.ic_breakfast),
            Category("昼食", R.drawable.ic_lunch),
            Category("夕食", R.drawable.ic_dinner),
            Category("軽食", R.drawable.ic_snack),
            Category("デザート", R.drawable.ic_dessert),
            Category("ヘルシー", R.drawable.ic_healthy)
        )

        val categoryAdapter = CategoryAdapter(categories) { selectedCategory ->
            filterRecipesByCategory(selectedCategory)
        }
        categoriesRecyclerView.adapter = categoryAdapter

        // 2. Setup Popular Recipes Recycler
        val popularRecyclerView: RecyclerView = view.findViewById(R.id.popular_recycler_view)

        popularRecipeAdapter = PopularRecipeAdapter(
            emptyList(),
            onRecipeClicked = { recipe ->
                navigateToDetail(recipe.id)
            },
            onLikeClicked = { recipe ->
                toggleLike(recipe)
            }
        )
        popularRecyclerView.adapter = popularRecipeAdapter

        // 3. Setup "Today's Special" Slider
        specialTitle = view.findViewById(R.id.special_title)
        specialViewPager = view.findViewById(R.id.special_view_pager)

        specialAdapter = SpecialRecipeAdapter(emptyList()) { recipe ->
            navigateToDetail(recipe.id)
        }
        specialViewPager.adapter = specialAdapter

        // Setup Auto-Slide Callback
        specialViewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                sliderHandler.removeCallbacks(sliderRunnable)
                sliderHandler.postDelayed(sliderRunnable, 4000) // Slide every 4 seconds
            }
        })

        // 4. Setup "See All" Link
        view.findViewById<View>(R.id.see_all_link).setOnClickListener {
            popularRecipeAdapter.updateData(allRecipes)
        }

        // 5. Fetch Data
        fetchRecipes()
    }

    private fun fetchRecipes() {
        db.collection("recipes")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { result, e ->
                if (e != null) {
                    Log.e("HomeFragment", "Listen failed.", e)
                    return@addSnapshotListener
                }

                val recipeList = result?.toObjects(Recipe::class.java) ?: emptyList()
                result?.forEachIndexed { index, document ->
                    recipeList[index].id = document.id
                }

                // Update local data
                allRecipes = recipeList

                // Update UI List
                popularRecipeAdapter.updateData(allRecipes)

                // Update "Today's Special" Slider
                updateFeaturedRecipe(allRecipes)
            }
    }

    // --- Slider Logic ---
    private fun updateFeaturedRecipe(recipes: List<Recipe>) {
        // Filter for recipes with MORE than 2 likes
        val specialRecipes = recipes.filter { it.likes > 2 }

        if (specialRecipes.isNotEmpty()) {
            // Show Title and Slider
            specialTitle.visibility = View.VISIBLE
            specialViewPager.visibility = View.VISIBLE

            // Update Adapter
            specialAdapter.updateData(specialRecipes)
        } else {
            // Hide if no popular recipes found
            specialTitle.visibility = View.GONE
            specialViewPager.visibility = View.GONE
        }
    }

    // --- Lifecycle Methods for Slider ---
    override fun onPause() {
        super.onPause()
        sliderHandler.removeCallbacks(sliderRunnable) // Stop sliding
    }

    override fun onResume() {
        super.onResume()
        sliderHandler.postDelayed(sliderRunnable, 4000) // Restart sliding
    }

    private fun filterRecipesByCategory(category: String) {
        val filteredList = allRecipes.filter { it.category == category }
        popularRecipeAdapter.updateData(filteredList)
        Toast.makeText(context, "$category を表示中", Toast.LENGTH_SHORT).show()
    }

    private fun navigateToDetail(recipeId: String) {
        try {
            val action = HomeFragmentDirections.actionHomeFragmentToRecipeDetailFragment(recipeId)
            findNavController().navigate(action)
        } catch (e: Exception) {
            Log.e("HomeFragment", "Navigation Error", e)
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
                val newLikes = if (currentLikes > 0) currentLikes - 1 else 0
                transaction.update(recipeRef, "likes", newLikes)
                transaction.update(recipeRef, "likedBy", FieldValue.arrayRemove(user.uid))
            } else {
                transaction.update(recipeRef, "likes", currentLikes + 1)
                transaction.update(recipeRef, "likedBy", FieldValue.arrayUnion(user.uid))
            }
        }
    }
}