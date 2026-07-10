package com.example.readingnotes.ui;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.readingnotes.R;
import com.example.readingnotes.adapter.NoteAdapter;
import com.example.readingnotes.data.AppDatabase;
import com.example.readingnotes.data.Book;
import com.example.readingnotes.data.Note;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class BookDetailActivity extends AppCompatActivity {
    private int bookId;
    private Book book;
    private AppDatabase db;
    private NoteAdapter noteAdapter;
    private RecyclerView recyclerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_book_detail);

        db = AppDatabase.getInstance(this);
        bookId = getIntent().getIntExtra("book_id", -1);

        if (bookId == -1) {
            finish();
            return;
        }

        book = db.bookDao().getBookById(bookId);
        if (book == null) {
            finish();
            return;
        }

        initViews();
        loadNotes();

        findViewById(R.id.btn_add_note).setOnClickListener(v -> {
            Intent intent = new Intent(this, AddNoteActivity.class);
            intent.putExtra("book_id", bookId);
            startActivity(intent);
        });

        findViewById(R.id.btn_mark_read).setOnClickListener(v -> {
            book.setStatus("已读");
            db.bookDao().updateBook(book);
            updateStatus();
            Toast.makeText(this, "已标记为已读", Toast.LENGTH_SHORT).show();
        });

        findViewById(R.id.btn_export_notes).setOnClickListener(v -> {
            exportNotes();
        });

        findViewById(R.id.btn_edit_book).setOnClickListener(v -> {
            Intent intent = new Intent(this, EditBookActivity.class);
            intent.putExtra("book_id", bookId);
            startActivity(intent);
        });
    }

    private void initViews() {
        TextView tvBookName = findViewById(R.id.tv_book_name_detail);
        TextView tvBookStatus = findViewById(R.id.tv_book_status_detail);
        TextView tvPublisher = findViewById(R.id.tv_publisher_detail);
        recyclerView = findViewById(R.id.recycler_notes);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        tvBookName.setText(book.getName());
        tvBookStatus.setText("状态: " + book.getStatus());
        tvPublisher.setText("出版社: " + (book.getPublisher() != null ? book.getPublisher() : "未知"));
    }

    private void loadNotes() {
        List<Note> notes = db.noteDao().getNotesByBookId(bookId);
        noteAdapter = new NoteAdapter(notes);
        recyclerView.setAdapter(noteAdapter);
    }

    private void updateStatus() {
        TextView tvStatus = findViewById(R.id.tv_book_status_detail);
        tvStatus.setText("状态: " + book.getStatus());
    }

    private void exportNotes() {
        List<Note> notes = db.noteDao().getNotesByBookId(bookId);
        if (notes.isEmpty()) {
            Toast.makeText(this, "没有笔记可导出", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            String filename = book.getName() + "_notes.txt";
            FileOutputStream fos = openFileOutput(filename, MODE_PRIVATE);
            OutputStreamWriter writer = new OutputStreamWriter(fos);

            writer.write("《" + book.getName() + "》读书笔记\n");
            writer.write("=".repeat(30) + "\n\n");

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
            for (Note note : notes) {
                writer.write(sdf.format(note.getCreateTime()) + "\n");
                writer.write(note.getContent() + "\n");
                writer.write("-".repeat(20) + "\n\n");
            }
            writer.close();

            Toast.makeText(this, "笔记已导出: " + filename, Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, "导出失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadNotes();
        book = db.bookDao().getBookById(bookId);
        updateStatus();
    }
}