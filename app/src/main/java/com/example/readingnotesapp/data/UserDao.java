package com.example.readingnotesapp.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import androidx.room.Delete;
import java.util.List;

@Dao
public interface UserDao {
    @Insert
    void insertUser(User user);

    @Update
    void updateUser(User user);

    @Delete
    void deleteUser(User user);

    // 按用户名查询
    @Query("SELECT * FROM users WHERE username = :username")
    User getUserByUsername(String username);

    // ★★★ 按ID查询（新增） ★★★
    @Query("SELECT * FROM users WHERE id = :userId")
    User getUserById(int userId);

    // 按昵称查询（用于搜索）
    @Query("SELECT * FROM users WHERE nickname LIKE '%' || :nickname || '%'")
    List<User> getUsersByNickname(String nickname);

    // 获取最后登录用户
    @Query("SELECT * FROM users WHERE isLastLogin = 1 LIMIT 1")
    User getLastLoginUser();

    // 获取所有用户
    @Query("SELECT * FROM users")
    List<User> getAllUsers();

    // 清除所有用户的最后登录状态
    @Query("UPDATE users SET isLastLogin = 0")
    void clearLastLogin();

    // 获取用户总数
    @Query("SELECT COUNT(*) FROM users")
    int getUserCount();
}