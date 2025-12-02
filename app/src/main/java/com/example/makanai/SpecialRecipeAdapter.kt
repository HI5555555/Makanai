package com.example.makanai

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import coil.load

class SpecialRecipeAdapter(
    private var recipes: List<Recipe>,
    private val onRecipeClicked: (Recipe) -> Unit
) : RecyclerView.Adapter<SpecialRecipeAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val image: ImageView = itemView.findViewById(R.id.special_image)
        val title: TextView = itemView.findViewById(R.id.special_card_subtitle)
        val desc: TextView = itemView.findViewById(R.id.special_card_desc)
        val button: View = itemView.findViewById(R.id.special_card_button)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_special_card, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val recipe = recipes[position]

        holder.title.text = recipe.title
        holder.desc.text = recipe.description

        if (recipe.imageUrl.isNotEmpty()) {
            holder.image.load(recipe.imageUrl) { crossfade(true) }
        }

        holder.button.setOnClickListener { onRecipeClicked(recipe) }
        holder.itemView.setOnClickListener { onRecipeClicked(recipe) }
    }

    override fun getItemCount() = recipes.size

    fun updateData(newRecipes: List<Recipe>) {
        recipes = newRecipes
        notifyDataSetChanged()
    }
}