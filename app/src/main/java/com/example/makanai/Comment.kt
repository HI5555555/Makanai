package com.example.makanai

data class Comment(
    val id: Int,
    val authorName: String,
    val text: String,
    val timestamp: String,
    val authorImageResId: Int
)