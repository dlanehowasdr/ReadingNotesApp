package com.example.readingnotesapp.ui;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import com.example.readingnotesapp.R;
import com.example.readingnotesapp.data.AppDatabase;
import com.example.readingnotesapp.data.Note;
import java.text.SimpleDateFormat;
import java.util.Locale;

public class EditNoteActivity extends AppCompatActivity {
    private int noteId;
    private int bookId;
    private Note note;
    private EditText etNoteContent;
    private TextView tvNoteInfo;
    private SwitchCompat swEditMode;
    private Button btnSave, btnDelete;
    private AppDatabase db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_note);

        db = AppDatabase.getInstance(this);

        noteId = getIntent().getIntExtra("note_id", -1);
        bookId = getIntent().getIntExtra("book_id", -1);

        if (noteId == -1) {
            Toast.makeText(this, "笔记不存在", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        note = db.noteDao().getNoteById(noteId);
        if (note == null) {
            Toast.makeText(this, "笔记不存在", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        loadNoteInfo();
        setupListeners();
    }

    private void initViews() {
        etNoteContent = findViewById(R.id.et_edit_note_content);
        tvNoteInfo = findViewById(R.id.tv_note_info);
        swEditMode = findViewById(R.id.sw_edit_mode);
        btnSave = findViewById(R.id.btn_save_edit_note);
        btnDelete = findViewById(R.id.btn_delete_note);
    }

    private void loadNoteInfo() {
        // 显示笔记内容
        etNoteContent.setText(note.getContent());
        // 将光标移到开头，避免滚动到末尾
        etNoteContent.setSelection(0);

        // 显示笔记时间信息
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        String info = "创建时间：" + sdf.format(note.getCreateTime());
        tvNoteInfo.setText(info);

        // 默认只能浏览（只读模式）
        swEditMode.setChecked(false);
        etNoteContent.setEnabled(false);
        etNoteContent.setTextColor(getColor(android.R.color.black));
        btnSave.setEnabled(false);
        btnSave.setAlpha(0.5f);
    }

    private void setupListeners() {
        // 编辑开关监听
        swEditMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                // 开启编辑模式
                etNoteContent.setEnabled(true);
                etNoteContent.setTextColor(getColor(android.R.color.black));
                etNoteContent.requestFocus();
                // 光标移到末尾，方便接着输入
                etNoteContent.setSelection(etNoteContent.getText().length());
                btnSave.setEnabled(true);
                btnSave.setAlpha(1.0f);
                Toast.makeText(this, "已进入编辑模式", Toast.LENGTH_SHORT).show();
            } else {
                // 关闭编辑模式（只读模式）
                etNoteContent.setEnabled(false);
                etNoteContent.setTextColor(getColor(android.R.color.darker_gray));
                btnSave.setEnabled(false);
                btnSave.setAlpha(0.5f);
                Toast.makeText(this, "已退出编辑模式", Toast.LENGTH_SHORT).show();
            }
        });

        // 保存修改
        btnSave.setOnClickListener(v -> {
            String content = etNoteContent.getText().toString().trim();
            if (content.isEmpty()) {
                Toast.makeText(this, "请输入笔记内容", Toast.LENGTH_SHORT).show();
                return;
            }

            note.setContent(content);
            db.noteDao().updateNote(note);
            Toast.makeText(this, "✅ 笔记已更新", Toast.LENGTH_SHORT).show();
            setResult(RESULT_OK);
            finish();
        });

        // 删除笔记
        btnDelete.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("⚠️ 删除笔记")
                    .setMessage("确定要删除这条笔记吗？")
                    .setPositiveButton("删除", (dialog, which) -> {
                        db.noteDao().deleteNote(note);
                        Toast.makeText(this, "🗑️ 笔记已删除", Toast.LENGTH_SHORT).show();
                        setResult(RESULT_OK);
                        finish();
                    })
                    .setNegativeButton("取消", null)
                    .show();
        });
    }
}