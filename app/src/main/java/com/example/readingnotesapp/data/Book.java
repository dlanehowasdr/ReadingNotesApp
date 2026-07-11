package com.example.readingnotesapp.data;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "books")
public class Book {
    @PrimaryKey(autoGenerate = true)
    private int id;
    private String name;
    private String publisher;
    private String coverPath;
    private String status; // "在读" or "已读"
    private long createTime;  // 录入时间（开始阅读时间）
    private long readTime;    // 阅读完成时间（点击已读的时间）

    public Book() {
        this.createTime = System.currentTimeMillis();
        this.status = "在读";
        this.readTime = 0;  // 0 表示未完成阅读
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

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