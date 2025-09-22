package com.ashleykaminski.aipackinglist

import kotlinx.serialization.Serializable

// Originally from MainActivity.kt
@Serializable
data class PackingList(
    val id: Int,
    val name: String,
    val items: List<SelectableItem> = emptyList() // Each packing list has its own items
)
