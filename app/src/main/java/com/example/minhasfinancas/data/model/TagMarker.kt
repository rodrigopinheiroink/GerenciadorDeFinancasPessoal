package com.example.minhasfinancas.data.model

data class TagMarker(
    val id: Long = 0,
    val name: String,
    val colorHex: String,
    val isDefault: Boolean = false,
    val displayOrder: Int = 0,
    val isVisibleOnHome: Boolean = true
)
