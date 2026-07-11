package com.example.readingnotesapp.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.readingnotesapp.R;
import com.example.readingnotesapp.adapter.NoteAdapter;
import com.example.readingnotesapp.data.AppDatabase;
import com.example.readingnotesapp.data.Book;
import com.example.readingnotesapp.data.Note;
import java.io.File;
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
            exportAndShareNotes();
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
        // 添加间距装饰
        int spacingInPixels = getResources().getDimensionPixelSize(R.dimen.note_spacing);
        recyclerView.addItemDecoration(new SpacesItemDecoration(spacingInPixels));
        tvBookName.setText(book.getName());
        tvBookStatus.setText("状态: " + book.getStatus());
        tvPublisher.setText("出版社: " + (book.getPublisher() != null ? book.getPublisher() : "未知"));
    }

    private void loadNotes() {
        List<Note> notes = db.noteDao().getNotesByBookId(bookId);
        noteAdapter = new NoteAdapter(notes, note -> {
            // 点击笔记进入编辑页面
            Intent intent = new Intent(this, EditNoteActivity.class);
            intent.putExtra("note_id", note.getId());
            intent.putExtra("book_id", bookId);
            startActivity(intent);
        });
        recyclerView.setAdapter(noteAdapter);
    }

    private void updateStatus() {
        TextView tvStatus = findViewById(R.id.tv_book_status_detail);
        tvStatus.setText("状态: " + book.getStatus());
    }

    private void exportAndShareNotes() {
        List<Note> notes = db.noteDao().getNotesByBookId(bookId);
        if (notes.isEmpty()) {
            Toast.makeText(this, "没有笔记可导出", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            // 创建导出文件
            String filename = book.getName() + "_笔记_" + System.currentTimeMillis() + ".txt";
            File file;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Android 10+ 使用外部存储目录
                file = new File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), filename);
            } else {
                file = new File(Environment.getExternalStorageDirectory(), filename);
            }

            // 创建父目录
            if (!file.getParentFile().exists()) {
                file.getParentFile().mkdirs();
            }

            // 写入内容
            FileOutputStream fos = new FileOutputStream(file);
            OutputStreamWriter writer = new OutputStreamWriter(fos);

            writer.write("《" + book.getName() + "》读书笔记\n");
            writer.write("导出时间：" + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(System.currentTimeMillis()) + "\n");
            writer.write("=".repeat(40) + "\n\n");

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
            int index = 1;
            for (Note note : notes) {
                writer.write(index + ". " + sdf.format(note.getCreateTime()) + "\n");
                writer.write("   " + note.getContent() + "\n");
                writer.write("-".repeat(30) + "\n\n");
                index++;
            }

            writer.write("\n总笔记数：" + notes.size() + " 条");
            writer.close();
            fos.close();

            Toast.makeText(this, "导出成功，正在打开分享...", Toast.LENGTH_SHORT).show();

            // 分享文件
            shareFile(file);

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "导出失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void shareFile(File file) {
        try {
            Uri fileUri;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                // Android 7.0+ 使用 FileProvider
                fileUri = FileProvider.getUriForFile(this,
                        getPackageName() + ".fileprovider",
                        file);
            } else {
                fileUri = Uri.fromFile(file);
            }

            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, book.getName() + " 读书笔记");
            shareIntent.putExtra(Intent.EXTRA_TEXT, "分享来自《" + book.getName() + "》的读书笔记");
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            startActivity(Intent.createChooser(shareIntent, "分享笔记"));

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "分享失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadNotes();
        book = db.bookDao().getBookById(bookId);
        if (book != null) {
            updateStatus();
        }
    }
}