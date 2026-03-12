package com.ashleykaminski.aipackinglist

import kotlinx.serialization.Serializable

@Serializable
data class TemplateItem(val id: Int, val text: String)
