package com.example.readingnotesapp.ui;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.readingnotesapp.R;
import com.example.readingnotesapp.MainActivity;
import com.example.readingnotesapp.data.AppDatabase;
import com.example.readingnotesapp.data.User;
import com.example.readingnotesapp.utils.UserManager;

public class RegisterActivity extends AppCompatActivity {
    private EditText etUsername, etNickname, etPassword, etConfirmPassword;
    private Button btnRegister;
    private AppDatabase db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        db = AppDatabase.getInstance(this);
        initViews();
    }

    private void initViews() {
        etUsername = findViewById(R.id.et_register_username);
        etNickname = findViewById(R.id.et_register_nickname);
        etPassword = findViewById(R.id.et_register_password);
        etConfirmPassword = findViewById(R.id.et_register_confirm_password);
        btnRegister = findViewById(R.id.btn_register);

        btnRegister.setOnClickListener(v -> {
            String username = etUsername.getText().toString().trim();
            String nickname = etNickname.getText().toString().trim();
            String password = etPassword.getText().toString().trim();
            String confirmPassword = etConfirmPassword.getText().toString().trim();

            // 验证用户名
            if (username.isEmpty()) {
                Toast.makeText(this, "请输入用户名", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!username.matches("^[a-zA-Z0-9_]+$")) {
                Toast.makeText(this, "用户名只能包含字母、数字和下划线", Toast.LENGTH_SHORT).show();
                return;
            }

            if (username.length() < 3 || username.length() > 20) {
                Toast.makeText(this, "用户名长度应为3-20位", Toast.LENGTH_SHORT).show();
                return;
            }

            // 验证昵称
            if (nickname.isEmpty()) {
                Toast.makeText(this, "请输入昵称", Toast.LENGTH_SHORT).show();
                return;
            }

            // 验证密码
            if (password.isEmpty()) {
                Toast.makeText(this, "请输入密码", Toast.LENGTH_SHORT).show();
                return;
            }

            if (password.length() < 6) {
                Toast.makeText(this, "密码长度不能少于6位", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!password.equals(confirmPassword)) {
                Toast.makeText(this, "两次密码输入不一致", Toast.LENGTH_SHORT).show();
                return;
            }

            // 检查用户名是否已存在
            if (db.userDao().getUserByUsername(username) != null) {
                Toast.makeText(this, "用户名已存在，请换一个", Toast.LENGTH_SHORT).show();
                etUsername.requestFocus();
                etUsername.selectAll();
                return;
            }

            // 创建用户
            User user = new User();
            user.setUsername(username);
            user.setNickname(nickname);
            user.setPassword(password);
            user.setLastLogin(true);

            db.userDao().insertUser(user);

            // 注册成功后直接登录
            UserManager.getInstance(this).setCurrentUser(user);

            Toast.makeText(this, "注册成功，欢迎 " + nickname + "！", Toast.LENGTH_SHORT).show();

            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }
}