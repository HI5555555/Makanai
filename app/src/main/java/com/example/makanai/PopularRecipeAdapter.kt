package com.example.makanai

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.transform.CircleCropTransformation
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

// Updated Constructor to take 2 functions
class PopularRecipeAdapter(
    private var recipes: List<Recipe>,
    private val onRecipeClicked: (Recipe) -> Unit,
    private val onLikeClicked: (Recipe) -> Unit
) : RecyclerView.Adapter<PopularRecipeAdapter.RecipeViewHolder>() {

    class RecipeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val image: ImageView = itemView.findViewById(R.id.recipe_image)
        val title: TextView = itemView.findViewById(R.id.recipe_title)
        val author: TextView = itemView.findViewById(R.id.recipe_author)
        val authorImage: ImageView? = itemView.findViewById(R.id.author_image_card)
        val time: TextView = itemView.findViewById(R.id.recipe_time)
        val likes: TextView = itemView.findViewById(R.id.recipe_likes)
        val category: TextView = itemView.findViewById(R.id.recipe_category)

        // Now this ID exists because we updated XML in Step 1
        val likeIcon: ImageView = itemView.findViewById(R.id.recipe_like_icon)
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
            error(R.drawable.img_granola)
        }

        holder.title.text = recipe.title
        holder.time.text = recipe.prepTime
        holder.likes.text = recipe.likes.toString()
        holder.category.text = recipe.category

        // Load Author Info
        holder.author.text = "Loading..."
        holder.authorImage?.setImageResource(R.drawable.ic_profile)

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

        // --- LIKE LOGIC ---
        val currentUser = Firebase.auth.currentUser
        val isLiked = if (currentUser != null) {
            recipe.likedBy.contains(currentUser.uid)
        } else {
            false
        }

        if (isLiked) {
            // Red for liked
            holder.likeIcon.setColorFilter(Color.parseColor("#FF6347"))
            holder.likeIcon.setImageResource(R.drawable.ic_heart_outline)
        } else {
            // Gray for unliked
            holder.likeIcon.setColorFilter(Color.parseColor("#808080"))
            holder.likeIcon.setImageResource(R.drawable.ic_heart_outline)
        }

        // Click Listeners
        holder.likeIcon.setOnClickListener { onLikeClicked(recipe) }
        holder.itemView.setOnClickListener { onRecipeClicked(recipe) }
    }

    override fun getItemCount() = recipes.size

    fun updateData(newRecipes: List<Recipe>) {
        recipes = newRecipes
        notifyDataSetChanged()
    }
}