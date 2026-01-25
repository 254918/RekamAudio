package com.example.rekamaudio.data.model

data class Recording(
    val id: Long,
    val fileName: String,
    val fileUri: String,
    val durationMs: Long,
    val createdAt: Long
)
