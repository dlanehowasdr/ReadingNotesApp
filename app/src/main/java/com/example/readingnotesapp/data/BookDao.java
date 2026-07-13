package com.example.readingnotesapp.data;

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

    @Query("SELECT * FROM books WHERE userId = :userId ORDER BY createTime DESC")
    List<Book> getBooksByUserId(int userId);

    @Query("SELECT * FROM books WHERE id = :bookId AND userId = :userId")
    Book getBookById(int bookId, int userId);

    @Query("SELECT COUNT(*) FROM books WHERE userId = :userId")
    int getBookCountByUserId(int userId);

    @Query("SELECT COUNT(*) FROM books WHERE userId = :userId AND status = '已读'")
    int getReadCountByUserId(int userId);

    @Query("DELETE FROM books WHERE userId = :userId")
    void deleteBooksByUserId(int userId);
}