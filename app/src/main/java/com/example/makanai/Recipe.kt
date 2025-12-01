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

    val imageUrl: String = "",
    val likes: Int = 0,
    val likedBy: List<String> = emptyList(),
    val ingredients: List<String> = emptyList(),
    val steps: List<Map<String, String>> = emptyList(),
    val createdAt: Long = 0
) : Parcelable