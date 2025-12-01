package com.example.makanai

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.transform.CircleCropTransformation
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class PopularRecipeAdapter(
    private var recipes: List<Recipe>,
    private val onItemClicked: (Recipe) -> Unit
) : RecyclerView.Adapter<PopularRecipeAdapter.RecipeViewHolder>() {

    class RecipeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val image: ImageView = itemView.findViewById(R.id.recipe_image)
        val title: TextView = itemView.findViewById(R.id.recipe_title)
        val author: TextView = itemView.findViewById(R.id.recipe_author)
        // Ensure you have an author_image_card or author_image in your item_recipe_card.xml
        // Let's assume the TextView 'recipe_author' is next to an ImageView
        val authorImage: ImageView? = itemView.findViewById(R.id.author_image_card) // Add ID to XML if needed

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

        // Load Recipe Image
        holder.image.load(recipe.imageUrl) {
            crossfade(true)
            placeholder(R.drawable.img_granola)
        }

        holder.title.text = recipe.title
        holder.time.text = recipe.prepTime
        holder.likes.text = recipe.likes.toString()
        holder.category.text = recipe.category

        // Placeholder while loading author
        holder.author.text = "Loading..."
        holder.authorImage?.setImageResource(R.drawable.ic_profile)

        // --- THE BETTER WAY: Fetch Author Info for this Card ---
        if (recipe.authorId.isNotEmpty()) {
            Firebase.firestore.collection("users").document(recipe.authorId).get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val name = document.getString("name")
                        val photoUrl = document.getString("profileImageUrl")

                        holder.author.text = name ?: "Unknown"

                        if (!photoUrl.isNullOrEmpty()) {
                            holder.authorImage?.load(photoUrl) {
                                transformations(CircleCropTransformation())
                            }
                        }
                    }
                }
        } else {
            holder.author.text = "Unknown"
        }

        holder.itemView.setOnClickListener { onItemClicked(recipe) }
    }

    override fun getItemCount() = recipes.size

    fun updateData(newRecipes: List<Recipe>) {
        recipes = newRecipes
        notifyDataSetChanged()
    }
}