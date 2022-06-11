package io.zoemeow.diaryfirebase.model

data class Note(
    var date: Long = 0,
    var title: String = String(),
    var content: String = String()
)