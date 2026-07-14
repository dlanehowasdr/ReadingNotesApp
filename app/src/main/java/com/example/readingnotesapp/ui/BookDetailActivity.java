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
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
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
import com.example.readingnotesapp.utils.NoteImportUtils;
import com.example.readingnotesapp.utils.UserManager;
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

    private static final int REQUEST_ADD_NOTE = 1;
    private static final int REQUEST_EDIT_NOTE = 2;

    // ★★★ 文件选择器 ★★★
    private final ActivityResultLauncher<String> filePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    importNotesFromFile(uri);
                }
            });

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

        book = db.bookDao().getBookById(bookId, UserManager.getInstance(this).getCurrentUserId());
        if (book == null) {
            Toast.makeText(this, "书籍不存在或无权访问", Toast.LENGTH_SHORT).show();
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

        int spacingInPixels = getResources().getDimensionPixelSize(R.dimen.note_spacing);
        recyclerView.addItemDecoration(new SpacesItemDecoration(spacingInPixels));
    }

    private void displayBookInfo() {
        tvBookName.setText(book.getName());
        tvPublisher.setText("出版社：" + (book.getPublisher() != null ? book.getPublisher() : "未知"));

        String status = book.getStatus();
        tvBookStatus.setText("状态：" + status);
        if ("已读".equals(status)) {
            tvBookStatus.setTextColor(getColor(android.R.color.holo_green_dark));
        } else {
            tvBookStatus.setTextColor(getColor(android.R.color.holo_blue_dark));
        }

        if (book.getCoverPath() != null && !book.getCoverPath().isEmpty()) {
            File coverFile = new File(book.getCoverPath());
            if (coverFile.exists()) {
                Glide.with(this)
                        .load(coverFile)
                        .centerCrop()
                        .placeholder(R.drawable.ic_book_placeholder)
                        .error(R.drawable.ic_book_placeholder)
                        .into(ivBookCover);
            } else {
                ivBookCover.setImageResource(R.drawable.ic_book_placeholder);
            }
        } else {
            ivBookCover.setImageResource(R.drawable.ic_book_placeholder);
        }

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
        String startTime = sdf.format(book.getCreateTime());
        tvStartTime.setText("开始阅读：" + startTime);

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
            // ★★★ 改为 startActivityForResult ★★★
            Intent intent = new Intent(this, EditNoteActivity.class);
            intent.putExtra("note_id", note.getId());
            intent.putExtra("book_id", bookId);
            startActivityForResult(intent, REQUEST_EDIT_NOTE);
        });
        recyclerView.setAdapter(noteAdapter);
    }

    private void setupButtons() {
        // ★★★ 添加笔记改为 startActivityForResult ★★★
        findViewById(R.id.btn_add_note).setOnClickListener(v -> {
            Intent intent = new Intent(this, AddNoteActivity.class);
            intent.putExtra("book_id", bookId);
            startActivityForResult(intent, REQUEST_ADD_NOTE);
        });

        findViewById(R.id.btn_mark_read).setOnClickListener(v -> {
            if ("已读".equals(book.getStatus())) {
                Toast.makeText(this, "本书已标记为已读", Toast.LENGTH_SHORT).show();
                return;
            }

            book.setStatus("已读");
            book.setReadTime(System.currentTimeMillis());
            db.bookDao().updateBook(book);
            displayBookInfo();
            Toast.makeText(this, "已标记为已读", Toast.LENGTH_SHORT).show();
        });

        // ★★★ 导入笔记按钮 ★★★
        findViewById(R.id.btn_import_notes).setOnClickListener(v -> {
            showImportDialog();
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

    // ==================== 笔记导入功能 ====================

    private void showImportDialog() {
        new AlertDialog.Builder(this)
                .setTitle("📥 导入笔记")
                .setMessage("选择要导入的笔记文件\n\n支持格式：\n· 导出的笔记文件（.txt）\n· 数据备份文件（.json）")
                .setPositiveButton("选择文件", (dialog, which) -> {
                    filePickerLauncher.launch("*/*");
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void importNotesFromFile(Uri uri) {
        try {
            String fileName = getFileName(uri);
            List<Note> newNotes = null;

            // 根据文件扩展名选择解析方式
            if (fileName != null && fileName.endsWith(".json")) {
                // JSON格式（数据备份文件）
                newNotes = NoteImportUtils.parseNotesFromJson(this, uri, book.getName());
            } else {
                // TXT格式（导出的笔记文件）
                newNotes = NoteImportUtils.parseNotesFromFile(this, uri);
            }

            if (newNotes == null || newNotes.isEmpty()) {
                Toast.makeText(this, "文件中没有找到笔记", Toast.LENGTH_SHORT).show();
                return;
            }

            // 获取已存在的笔记
            List<Note> existingNotes = db.noteDao().getNotesByBookId(bookId);

            // 过滤掉已存在的笔记
            List<Note> notesToImport = NoteImportUtils.filterExistingNotes(newNotes, existingNotes);

            if (notesToImport.isEmpty()) {
                Toast.makeText(this, "所有笔记都已存在，无需导入", Toast.LENGTH_SHORT).show();
                return;
            }

            // 确认导入
            final List<Note> finalNotesToImport = notesToImport;
            new AlertDialog.Builder(this)
                    .setTitle("确认导入")
                    .setMessage("找到 " + newNotes.size() + " 条笔记\n" +
                            "其中 " + notesToImport.size() + " 条是新笔记\n\n" +
                            "确认导入吗？")
                    .setPositiveButton("导入", (dialog, which) -> {
                        performImportNotes(finalNotesToImport);
                    })
                    .setNegativeButton("取消", null)
                    .show();

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "导入失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void performImportNotes(List<Note> notesToImport) {
        try {
            for (Note note : notesToImport) {
                note.setBookId(bookId);
                db.noteDao().insertNote(note);
            }

            Toast.makeText(this, "成功导入 " + notesToImport.size() + " 条笔记", Toast.LENGTH_SHORT).show();

            // 刷新笔记列表
            loadNotes();

            // 返回结果，让首页刷新
            setResult(RESULT_OK);

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "导入失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private String getFileName(Uri uri) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                android.database.Cursor cursor = getContentResolver()
                        .query(uri, null, null, null, null);
                if (cursor != null && cursor.moveToFirst()) {
                    String name = cursor.getString(
                            cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME));
                    cursor.close();
                    return name;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return uri.getPath();
    }

    // ★★★ 添加 onActivityResult 处理返回结果 ★★★
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // 如果返回结果是 OK，刷新数据
        if (resultCode == RESULT_OK) {
            refreshData();
        }
    }

    private void refreshData() {
        book = db.bookDao().getBookById(bookId, UserManager.getInstance(this).getCurrentUserId());
        if (book != null) {
            displayBookInfo();
            loadNotes();
        }
    }

    // ★★★ 修改 onResume 为调用 refreshData ★★★
    @Override
    protected void onResume() {
        super.onResume();
        refreshData();
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
}