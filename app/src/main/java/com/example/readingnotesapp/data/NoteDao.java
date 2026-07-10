package com.example.readingnotes.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Delete;
import java.util.List;

@Dao
public interface NoteDao {
    @Insert
    void insertNote(Note note);

    @Delete
    void deleteNote(Note note);

    @Query("SELECT * FROM notes WHERE bookId = :bookId ORDER BY createTime DESC")
    List<Note> getNotesByBookId(int bookId);
}