package com.example.makanai

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.ktx.auth // IMPORT ADDED
import com.google.firebase.firestore.FieldValue // IMPORT ADDED
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class HomeFragment : Fragment(R.layout.fragment_home) {

    private lateinit var popularRecipeAdapter: PopularRecipeAdapter
    private val db = Firebase.firestore
    private val auth = Firebase.auth

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. Setup Categories
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

        // Initialize adapter with BOTH callbacks
        popularRecipeAdapter = PopularRecipeAdapter(
            emptyList(),
            onRecipeClicked = { recipe ->
                val action = HomeFragmentDirections.actionHomeFragmentToRecipeDetailFragment(recipe.id)
                findNavController().navigate(action)
            },
            onLikeClicked = { recipe ->
                toggleLike(recipe)
            }
        )
        popularRecyclerView.adapter = popularRecipeAdapter

        // 3. Fetch Data
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
                // Manually set ID because toObject doesn't do it for document ID
                result?.forEachIndexed { index, document ->
                    recipeList[index].id = document.id
                }
                popularRecipeAdapter.updateData(recipeList)
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
        }
    }
}