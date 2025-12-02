package com.example.makanai

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Recipe(
    var id: String = "",
    val title: String = "",
    val description: String = "",
    val category: String = "",
    val prepTime: String = "",
    val servings: String = "",
    val difficulty: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val authorProfileImage: String = "",
    val imageUrl: String = "",
    val likes: Int = 0,

    // --- IMPORTANT CHANGE HERE ---
    // Ingredients are now Maps (Name, Qty, Unit), not Strings
    val ingredients: List<Map<String, String>> = emptyList(),

    // Steps are also Maps (Text, ImageUrl)
    val steps: List<Map<String, String>> = emptyList(),

    val likedBy: List<String> = emptyList(),
    val createdAt: Long = 0
) : Parcelable