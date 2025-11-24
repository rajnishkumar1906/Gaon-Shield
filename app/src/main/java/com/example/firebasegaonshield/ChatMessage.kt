package com.example.firebasegaonshield

data class ChatMessage(
    val message: String,
    val isUser: Boolean,
    val timestamp: Long
)