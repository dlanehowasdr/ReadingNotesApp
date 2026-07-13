package com.example.readingnotesapp.ui;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.readingnotesapp.R;
import com.example.readingnotesapp.MainActivity;
import com.example.readingnotesapp.data.AppDatabase;
import com.example.readingnotesapp.data.User;
import com.example.readingnotesapp.utils.UserManager;

public class LoginActivity extends AppCompatActivity {
    private EditText etUsername, etPassword;
    private Button btnLogin;
    private TextView tvRegister;
    private AppDatabase db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        db = AppDatabase.getInstance(this);

        // 检查是否已有登录用户
        UserManager userManager = UserManager.getInstance(this);
        if (userManager.hasLoggedInUser()) {
            goToMain();
            return;
        }

        initViews();
    }

    private void initViews() {
        etUsername = findViewById(R.id.et_login_username);
        etPassword = findViewById(R.id.et_login_password);
        btnLogin = findViewById(R.id.btn_login);
        tvRegister = findViewById(R.id.tv_register);

        btnLogin.setOnClickListener(v -> {
            String username = etUsername.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            if (username.isEmpty()) {
                Toast.makeText(this, "请输入用户名", Toast.LENGTH_SHORT).show();
                etUsername.requestFocus();
                return;
            }

            if (password.isEmpty()) {
                Toast.makeText(this, "请输入密码", Toast.LENGTH_SHORT).show();
                etPassword.requestFocus();
                return;
            }

            User user = db.userDao().getUserByUsername(username);
            if (user == null) {
                Toast.makeText(this, "用户不存在，请先注册", Toast.LENGTH_SHORT).show();
                etUsername.requestFocus();
                etUsername.selectAll();
                return;
            }

            if (!user.getPassword().equals(password)) {
                Toast.makeText(this, "密码错误", Toast.LENGTH_SHORT).show();
                etPassword.requestFocus();
                etPassword.selectAll();
                return;
            }

            // 登录成功
            UserManager.getInstance(this).setCurrentUser(user);
            Toast.makeText(this, "欢迎回来， " + user.getNickname(), Toast.LENGTH_SHORT).show();
            goToMain();
        });

        tvRegister.setOnClickListener(v -> {
            startActivity(new Intent(this, RegisterActivity.class));
        });
    }

    private void goToMain() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}