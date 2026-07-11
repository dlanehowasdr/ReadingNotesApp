package com.example.readingnotesapp.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import androidx.room.Delete;
import java.util.List;

@Dao
public interface NoteDao {
    @Insert
    void insertNote(Note note);

    @Update
    void updateNote(Note note);

    @Delete
    void deleteNote(Note note);

    @Query("SELECT * FROM notes WHERE bookId = :bookId ORDER BY createTime DESC")
    List<Note> getNotesByBookId(int bookId);

    @Query("SELECT * FROM notes WHERE id = :noteId")
    Note getNoteById(int noteId);
}