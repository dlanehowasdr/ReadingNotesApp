package com.example.readingnotesapp.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
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
    private TextView tvBookName, tvBookStatus, tvPublisher, tvStartTime, tvReadTime;
    private ImageView ivBookCover;

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
        displayBookInfo();
        loadNotes();
        setupButtons();
    }

    private void initViews() {
        tvBookName = findViewById(R.id.tv_book_name_detail);
        tvBookStatus = findViewById(R.id.tv_book_status_detail);
        tvPublisher = findViewById(R.id.tv_publisher_detail);
        tvStartTime = findViewById(R.id.tv_start_time);
        tvReadTime = findViewById(R.id.tv_read_time);
        ivBookCover = findViewById(R.id.iv_book_cover_detail);
        recyclerView = findViewById(R.id.recycler_notes);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // 添加间距装饰
        int spacingInPixels = getResources().getDimensionPixelSize(R.dimen.note_spacing);
        recyclerView.addItemDecoration(new SpacesItemDecoration(spacingInPixels));
    }

    private void displayBookInfo() {
        // 显示书籍信息
        tvBookName.setText(book.getName());
        tvPublisher.setText("出版社：" + (book.getPublisher() != null ? book.getPublisher() : "未知"));

        // 显示状态
        String status = book.getStatus();
        tvBookStatus.setText("状态：" + status);
        if ("已读".equals(status)) {
            tvBookStatus.setTextColor(getColor(android.R.color.holo_green_dark));
        } else {
            tvBookStatus.setTextColor(getColor(android.R.color.holo_blue_dark));
        }

        // 显示封面
        if (book.getCoverPath() != null && !book.getCoverPath().isEmpty()) {
            Glide.with(this)
                    .load(book.getCoverPath())
                    .placeholder(R.drawable.ic_book_placeholder)
                    .error(R.drawable.ic_book_placeholder)
                    .into(ivBookCover);
        } else {
            ivBookCover.setImageResource(R.drawable.ic_book_placeholder);
        }

        // 显示开始阅读时间（录入时间）
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
        String startTime = sdf.format(book.getCreateTime());
        tvStartTime.setText("开始阅读：" + startTime);

        // 显示完成阅读时间
        if (book.getReadTime() > 0) {
            String readTime = sdf.format(book.getReadTime());
            tvReadTime.setText("完成阅读：" + readTime);
        } else {
            tvReadTime.setText("完成阅读：--");
        }
    }

    private void loadNotes() {
        List<Note> notes = db.noteDao().getNotesByBookId(bookId);
        noteAdapter = new NoteAdapter(notes, note -> {
            Intent intent = new Intent(this, EditNoteActivity.class);
            intent.putExtra("note_id", note.getId());
            intent.putExtra("book_id", bookId);
            startActivity(intent);
        });
        recyclerView.setAdapter(noteAdapter);
    }

    private void setupButtons() {
        findViewById(R.id.btn_add_note).setOnClickListener(v -> {
            Intent intent = new Intent(this, AddNoteActivity.class);
            intent.putExtra("book_id", bookId);
            startActivity(intent);
        });

        findViewById(R.id.btn_mark_read).setOnClickListener(v -> {
            if ("已读".equals(book.getStatus())) {
                Toast.makeText(this, "本书已标记为已读", Toast.LENGTH_SHORT).show();
                return;
            }

            book.setStatus("已读");
            book.setReadTime(System.currentTimeMillis());  // 记录完成阅读时间
            db.bookDao().updateBook(book);
            displayBookInfo();  // 刷新显示
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

    private void exportAndShareNotes() {
        List<Note> notes = db.noteDao().getNotesByBookId(bookId);
        if (notes.isEmpty()) {
            Toast.makeText(this, "没有笔记可导出", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            String filename = book.getName() + "_笔记_" + System.currentTimeMillis() + ".txt";
            File file;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                file = new File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), filename);
            } else {
                file = new File(Environment.getExternalStorageDirectory(), filename);
            }

            if (!file.getParentFile().exists()) {
                file.getParentFile().mkdirs();
            }

            FileOutputStream fos = new FileOutputStream(file);
            OutputStreamWriter writer = new OutputStreamWriter(fos);

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            writer.write("《" + book.getName() + "》读书笔记\n");
            writer.write("导出时间：" + sdf.format(System.currentTimeMillis()) + "\n");
            writer.write("开始阅读：" + sdf.format(book.getCreateTime()) + "\n");
            if (book.getReadTime() > 0) {
                writer.write("完成阅读：" + sdf.format(book.getReadTime()) + "\n");
            } else {
                writer.write("完成阅读：未读完\n");
            }
            writer.write("状态：" + book.getStatus() + "\n");
            writer.write("=".repeat(40) + "\n\n");

            SimpleDateFormat sdf2 = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
            int index = 1;
            for (Note note : notes) {
                writer.write(index + ". " + sdf2.format(note.getCreateTime()) + "\n");
                writer.write("   " + note.getContent() + "\n");
                writer.write("-".repeat(30) + "\n\n");
                index++;
            }

            writer.write("\n总笔记数：" + notes.size() + " 条");
            writer.close();
            fos.close();

            Toast.makeText(this, "导出成功，正在打开分享...", Toast.LENGTH_SHORT).show();
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
        // 刷新数据
        book = db.bookDao().getBookById(bookId);
        if (book != null) {
            displayBookInfo();
            loadNotes();
        }
    }
}