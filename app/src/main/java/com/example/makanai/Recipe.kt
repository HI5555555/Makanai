package com.example.makanai

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Recipe(
    var id: String = "", // Ensure ID is a String
    val title: String = "",
    val description: String = "",
    val category: String = "",
    val prepTime: String = "",
    val servings: String = "",
    val difficulty: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val imageUrl: String = "",
    val likes: Int = 0,
    // Updated ingredients to List of Maps
    val ingredients: List<Map<String, String>> = emptyList(),
    // Updated steps to List of Maps
    val steps: List<Map<String, String>> = emptyList(),
    val likedBy: List<String> = emptyList(),
    val createdAt: Long = 0
) : Parcelable