package com.example.makanai

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Recipe(
    val id: Int,
    val title: String,
    val imageResId: Int,
    val category: String,
    val author: String,
    val authorDesc: String = "フードブロガー", // New: Author description
    val mainDescription: String, // New: Main text description
    val prepTime: String, // New: e.g., "15分"
    val servings: String, // New: e.g., "2人分"
    val difficulty: String, // New: e.g., "簡単"
    val ingredients: List<String>, // New: A list of ingredients
    val steps: List<String>, // New: A list of steps
    val likes: Int
) : Parcelable