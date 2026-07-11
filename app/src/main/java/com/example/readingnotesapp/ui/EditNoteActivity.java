package com.example.readingnotesapp.ui;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.example.readingnotesapp.R;
import com.example.readingnotesapp.data.AppDatabase;
import com.example.readingnotesapp.data.Note;

public class EditNoteActivity extends AppCompatActivity {
    private int noteId;
    private int bookId;
    private Note note;
    private EditText etNoteContent;
    private AppDatabase db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_note);

        db = AppDatabase.getInstance(this);

        // 获取传入的笔记ID
        noteId = getIntent().getIntExtra("note_id", -1);
        bookId = getIntent().getIntExtra("book_id", -1);

        if (noteId == -1) {
            Toast.makeText(this, "笔记不存在", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // 加载笔记
        note = db.noteDao().getNoteById(noteId);
        if (note == null) {
            Toast.makeText(this, "笔记不存在", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
    }

    private void initViews() {
        etNoteContent = findViewById(R.id.et_edit_note_content);
        Button btnSave = findViewById(R.id.btn_save_edit_note);
        Button btnDelete = findViewById(R.id.btn_delete_note);

        // 显示当前笔记内容
        etNoteContent.setText(note.getContent());
        etNoteContent.setSelection(note.getContent().length());

        // 保存修改
        btnSave.setOnClickListener(v -> {
            String content = etNoteContent.getText().toString().trim();
            if (content.isEmpty()) {
                Toast.makeText(this, "请输入笔记内容", Toast.LENGTH_SHORT).show();
                return;
            }

            note.setContent(content);
            db.noteDao().updateNote(note);
            Toast.makeText(this, "笔记已更新", Toast.LENGTH_SHORT).show();
            finish();
        });

        // 删除笔记
        btnDelete.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("删除笔记")
                    .setMessage("确定要删除这条笔记吗？")
                    .setPositiveButton("删除", (dialog, which) -> {
                        db.noteDao().deleteNote(note);
                        Toast.makeText(this, "笔记已删除", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .setNegativeButton("取消", null)
                    .show();
        });
    }
}