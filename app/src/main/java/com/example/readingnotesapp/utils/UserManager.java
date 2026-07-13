package com.example.readingnotesapp.utils;

import android.content.Context;
import android.content.SharedPreferences;
import com.example.readingnotesapp.data.AppDatabase;
import com.example.readingnotesapp.data.User;

public class UserManager {
    private static UserManager instance;
    private Context context;
    private User currentUser;
    private AppDatabase db;

    private UserManager(Context context) {
        this.context = context.getApplicationContext();
        this.db = AppDatabase.getInstance(context);
        loadCurrentUser();
    }

    public static synchronized UserManager getInstance(Context context) {
        if (instance == null) {
            instance = new UserManager(context);
        }
        return instance;
    }

    /**
     * 从 SharedPreferences 加载当前用户
     */
    private void loadCurrentUser() {
        SharedPreferences prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE);
        int userId = prefs.getInt("current_user_id", -1);
        if (userId != -1) {
            // 从数据库查询用户
            currentUser = db.userDao().getUserById(userId);
        }
    }

    /**
     * 获取当前用户
     */
    public User getCurrentUser() {
        if (currentUser == null) {
            loadCurrentUser();
        }
        return currentUser;
    }

    /**
     * 设置当前用户
     */
    public void setCurrentUser(User user) {
        this.currentUser = user;
        if (user != null) {
            SharedPreferences prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE);
            prefs.edit().putInt("current_user_id", user.getId()).apply();
        } else {
            SharedPreferences prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE);
            prefs.edit().remove("current_user_id").apply();
        }
    }

    /**
     * 获取当前用户ID
     */
    public int getCurrentUserId() {
        User user = getCurrentUser();
        return user != null ? user.getId() : -1;
    }

    /**
     * 获取当前用户名
     */
    public String getCurrentUsername() {
        User user = getCurrentUser();
        return user != null ? user.getUsername() : "";
    }

    /**
     * 获取当前用户昵称
     */
    public String getCurrentNickname() {
        User user = getCurrentUser();
        return user != null ? user.getNickname() : "";
    }

    /**
     * 退出登录
     */
    public void logout() {
        setCurrentUser(null);
        currentUser = null;
    }

    /**
     * 检查是否有已登录用户
     */
    public boolean hasLoggedInUser() {
        SharedPreferences prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE);
        int userId = prefs.getInt("current_user_id", -1);
        if (userId == -1) {
            return false;
        }
        // 验证用户是否还在数据库中
        User user = db.userDao().getUserById(userId);
        return user != null;
    }

    /**
     * 清除所有用户登录状态（一般不用）
     */
    public void clearAllLoginState() {
        SharedPreferences prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE);
        prefs.edit().clear().apply();
        currentUser = null;
    }
}