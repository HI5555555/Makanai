package com.example.makanai

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

// 1. Update the constructor to accept a click listener function
class PopularRecipeAdapter(
    private val recipes: List<Recipe>,
    private val onItemClicked: (Recipe) -> Unit
) : RecyclerView.Adapter<PopularRecipeAdapter.RecipeViewHolder>() {

    class RecipeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val image: ImageView = itemView.findViewById(R.id.recipe_image)
        val title: TextView = itemView.findViewById(R.id.recipe_title)
        val author: TextView = itemView.findViewById(R.id.recipe_author)
        val time: TextView = itemView.findViewById(R.id.recipe_time)
        val likes: TextView = itemView.findViewById(R.id.recipe_likes)
        val category: TextView = itemView.findViewById(R.id.recipe_category)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecipeViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_recipe_card, parent, false)
        return RecipeViewHolder(view)
    }

    override fun onBindViewHolder(holder: RecipeViewHolder, position: Int) {
        val recipe = recipes[position]

        holder.image.setImageResource(recipe.imageResId)
        holder.title.text = recipe.title
        holder.author.text = recipe.author
        holder.time.text = recipe.prepTime
        holder.likes.text = recipe.likes.toString()
        holder.category.text = recipe.category

        // 2. Set the click listener on the whole card
        holder.itemView.setOnClickListener {
            onItemClicked(recipe)
        }
    }

    override fun getItemCount() = recipes.size
}