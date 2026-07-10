package com.example.readingnotes.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import androidx.room.Delete;
import java.util.List;

@Dao
public interface BookDao {
    @Insert
    void insertBook(Book book);

    @Update
    void updateBook(Book book);

    @Delete
    void deleteBook(Book book);

    @Query("SELECT * FROM books ORDER BY createTime DESC")
    List<Book> getAllBooks();

    @Query("SELECT * FROM books WHERE id = :bookId")
    Book getBookById(int bookId);

    @Query("SELECT COUNT(*) FROM books")
    int getBookCount();

    @Query("SELECT COUNT(*) FROM books WHERE status = '已读'")
    int getReadCount();
}