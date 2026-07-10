package com.example.readingnotesapp.data;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.ForeignKey;
import androidx.room.Index;

@Entity(
        tableName = "notes",
        foreignKeys = @ForeignKey(
                entity = Book.class,
                parentColumns = "id",
                childColumns = "bookId",
                onDelete = ForeignKey.CASCADE,
                onUpdate = ForeignKey.CASCADE  // 可选：级联更新
        ),
        indices = {@Index(value = "bookId", name = "idx_note_book_id")}  // 命名索引
)
public class Note {
    @PrimaryKey(autoGenerate = true)
    private int id;
    private int bookId;
    private String content;
    private long createTime;

    public Note() {
        this.createTime = System.currentTimeMillis();
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getBookId() { return bookId; }
    public void setBookId(int bookId) { this.bookId = bookId; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public long getCreateTime() { return createTime; }
    public void setCreateTime(long createTime) { this.createTime = createTime; }
}