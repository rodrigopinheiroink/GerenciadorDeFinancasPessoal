package com.example.minhasfinancas.data.model

data class UserProfile(
    val id: String,
    val name: String,
    val photoUri: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
