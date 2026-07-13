package com.example.readingnotesapp.data;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "users")
public class User {
    @PrimaryKey(autoGenerate = true)
    private int id;
    private String username;      // 自动生成：年月日时分秒
    private String nickname;      // 用户昵称
    private String password;      // 用户密码
    private long createTime;
    private boolean isLastLogin;  // 是否最后一次登录

    public User() {
        this.createTime = System.currentTimeMillis();
        this.isLastLogin = false;
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public long getCreateTime() { return createTime; }
    public void setCreateTime(long createTime) { this.createTime = createTime; }

    public boolean isLastLogin() { return isLastLogin; }
    public void setLastLogin(boolean lastLogin) { isLastLogin = lastLogin; }
}