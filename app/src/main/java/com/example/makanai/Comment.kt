package com.example.makanai

import java.util.Date

data class Comment(
    val id: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val authorImageUrl: String = "",
    val text: String = "",
    val commentImageUrl: String = "", // <--- NEW FIELD for the food photo
    val timestamp: Date = Date()
)