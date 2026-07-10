package com.example.readingnotes.ui;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.readingnotes.R;
import com.example.readingnotes.data.AppDatabase;
import com.example.readingnotes.data.Note;

public class AddNoteActivity extends AppCompatActivity {
    private int bookId;
    private EditText etNoteContent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_note);

        bookId = getIntent().getIntExtra("book_id", -1);
        if (bookId == -1) {
            finish();
            return;
        }

        etNoteContent = findViewById(R.id.et_note_content);
        Button btnSaveNote = findViewById(R.id.btn_save_note);

        btnSaveNote.setOnClickListener(v -> {
            String content = etNoteContent.getText().toString().trim();
            if (content.isEmpty()) {
                Toast.makeText(this, "请输入笔记内容", Toast.LENGTH_SHORT).show();
                return;
            }

            Note note = new Note();
            note.setBookId(bookId);
            note.setContent(content);

            AppDatabase.getInstance(this).noteDao().insertNote(note);
            Toast.makeText(this, "笔记添加成功", Toast.LENGTH_SHORT).show();
            finish();
        });
    }
}