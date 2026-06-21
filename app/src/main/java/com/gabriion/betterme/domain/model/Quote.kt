package com.gabriion.betterme.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Quote(
    val text: String,
    val author: String? = null,
    val theme: String? = null
)
