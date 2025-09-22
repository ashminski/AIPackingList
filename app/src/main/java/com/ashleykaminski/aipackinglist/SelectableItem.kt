package com.ashleykaminski.aipackinglist

import kotlinx.serialization.Serializable

// Data class to represent an item in the list
@Serializable
data class SelectableItem(
    val id: Int,
    val text: String,
    var isSelected: Boolean = false // For the checkbox
)
