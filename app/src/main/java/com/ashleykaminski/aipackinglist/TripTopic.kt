package com.ashleykaminski.aipackinglist

import kotlinx.serialization.Serializable

@Serializable
data class TripTopic(val id: Int, val name: String, val items: List<TemplateItem> = emptyList(), val isBuiltIn: Boolean = false)
