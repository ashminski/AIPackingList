package com.ashleykaminski.aipackinglist

import kotlinx.serialization.Serializable

@Serializable
data class PackingListTemplate(
    val id: Int,
    val name: String,
    val items: List<TemplateItem> = emptyList()
)
