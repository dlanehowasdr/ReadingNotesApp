package com.example.readingnotesapp.data;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.Index;

@Entity(tableName = "books", indices = {@Index("userId")})
public class Book {
    @PrimaryKey(autoGenerate = true)
    private int id;
    private int userId;           // 新增：关联用户
    private String name;
    private String publisher;
    private String coverPath;
    private String status;        // "在读" or "已读"
    private long createTime;
    private long readTime;

    public Book() {
        this.createTime = System.currentTimeMillis();
        this.status = "在读";
        this.readTime = 0;
        this.userId = 0;
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPublisher() { return publisher; }
    public void setPublisher(String publisher) { this.publisher = publisher; }

    public String getCoverPath() { return coverPath; }
    public void setCoverPath(String coverPath) { this.coverPath = coverPath; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public long getCreateTime() { return createTime; }
    public void setCreateTime(long createTime) { this.createTime = createTime; }

    public long getReadTime() { return readTime; }
    public void setReadTime(long readTime) { this.readTime = readTime; }
}