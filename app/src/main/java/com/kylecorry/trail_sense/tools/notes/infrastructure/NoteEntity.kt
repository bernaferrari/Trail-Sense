package com.kylecorry.trail_sense.tools.notes.infrastructure

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.kylecorry.trail_sense.tools.notes.domain.Note

@Entity(
    tableName = "notes"
)
data class NoteEntity(
    @ColumnInfo(name = "title") val title: String?,
    @ColumnInfo(name = "contents") val contents: String?,
    @ColumnInfo(name = "created") val createdOn: Long
) {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "_id")
    var id: Long = 0

    fun toNote(): Note = Note(title, contents, createdOn, id)

    companion object {
        fun from(note: Note): NoteEntity {
            return NoteEntity(note.title, note.contents, note.createdOn).also { it.id = note.id }
        }
    }
}
