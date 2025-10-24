package com.example.makanai

import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.RecyclerView

class RecipeDetailFragment : Fragment(R.layout.fragment_recipe_detail) {

    private val args: RecipeDetailFragmentArgs by navArgs()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Find all the UI elements
        val recipeImage: ImageView = view.findViewById(R.id.detail_recipe_image)
        val title: TextView = view.findViewById(R.id.detail_recipe_title)
        val authorName: TextView = view.findViewById(R.id.author_name)
        val authorDesc: TextView = view.findViewById(R.id.author_desc)
        val description: TextView = view.findViewById(R.id.detail_description)
        val prepTime: TextView = view.findViewById(R.id.detail_prep_time)
        val servings: TextView = view.findViewById(R.id.detail_servings)
        val difficulty: TextView = view.findViewById(R.id.detail_difficulty)
        val ingredientsList: TextView = view.findViewById(R.id.detail_ingredients_list)
        val stepsList: TextView = view.findViewById(R.id.detail_steps_list)
        val backButton: ImageButton = view.findViewById(R.id.back_button)

        // Get the passed recipe ID
        val recipeId = args.recipeId

        // Find the recipe from our repository
        val recipe = RecipeRepository.getRecipeById(recipeId)

        // Fill the UI with the recipe data
        recipe?.let {
            recipeImage.setImageResource(it.imageResId)
            title.text = it.title
            authorName.text = it.author
            authorDesc.text = it.authorDesc
            description.text = it.mainDescription
            prepTime.text = it.prepTime
            servings.text = it.servings
            difficulty.text = it.difficulty

            // Format and display the lists
            ingredientsList.text = it.ingredients.joinToString(separator = "\n") { "・ $it" }
            stepsList.text = it.steps.mapIndexed { index, step ->
                "${index + 1}. $step"
            }.joinToString(separator = "\n\n")
        }

        // Set up the back button
        backButton.setOnClickListener {
            findNavController().popBackStack()
        }

        // --- ADD THIS NEW CODE ---

        // 1. Find the comments RecyclerView
        val commentsRecyclerView: RecyclerView = view.findViewById(R.id.comments_recycler_view)

        // 2. Create dummy data for comments
        val comments = listOf(
            Comment(
                id = 1,
                authorName = "佐藤 美咲",
                text = "今日の朝食に作りました！とても美味しかったです😋",
                timestamp = "2時間前",
                authorImageResId = R.drawable.ic_profile
            ),
            Comment(
                id = 2,
                authorName = "田中 太郎",
                text = "シンプルで美味しい。チェリートマトも添えました！",
                timestamp = "5時間前",
                authorImageResId = R.drawable.ic_profile
            )
        )

        // 3. Create and set the adapter
        val commentAdapter = CommentAdapter(comments)
        commentsRecyclerView.adapter = commentAdapter

        // You can also update the comment title
        val commentsTitle: TextView = view.findViewById(R.id.comments_title)
        commentsTitle.text = "コメント (${comments.size})"




    }
}