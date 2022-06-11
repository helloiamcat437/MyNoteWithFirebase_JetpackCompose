package io.zoemeow.diaryfirebase.data

import androidx.compose.runtime.mutableStateListOf
import io.zoemeow.diaryfirebase.model.Note

class NoteData {
    private val _noteList = mutableStateListOf<Note>()
    val noteList = _noteList

    var onDataChanged: () -> Unit = {}

    fun addNote(note: Note) {
        _noteList.add(note)
        onDataChanged()
    }

    fun deleteNote(note: Note) {
        _noteList.remove(note)
        onDataChanged()
    }

    fun deleteAllNotes() {
        _noteList.clear()
        onDataChanged()
    }
}
