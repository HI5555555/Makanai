package com.example.makanai

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Recipe(
    val id: Int,
    val title: String,
    val description: String,
    val ingredients: String,
    val imageResId: Int // Add this line: R.drawable.img_granola
) : Parcelable