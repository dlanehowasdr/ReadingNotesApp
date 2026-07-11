package com.example.readingnotesapp.ui;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;
import com.example.readingnotesapp.R;
import com.example.readingnotesapp.data.AppDatabase;
import com.example.readingnotesapp.data.Book;

public class EditBookActivity extends AppCompatActivity {
    private static final int PICK_IMAGE = 100;
    private int bookId;
    private Book book;
    private EditText etBookName, etPublisher;
    private ImageView ivCover;
    private String coverPath;
    private AppDatabase db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_book);

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
        loadBookInfo();

        Button btnSelectCover = findViewById(R.id.btn_select_cover_edit);
        Button btnSave = findViewById(R.id.btn_save_edit);
        Button btnDelete = findViewById(R.id.btn_delete_book);

        btnSelectCover.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK,
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            intent.setType("image/*");
            startActivityForResult(intent, PICK_IMAGE);
        });

        btnSave.setOnClickListener(v -> {
            String name = etBookName.getText().toString().trim();
            String publisher = etPublisher.getText().toString().trim();

            if (name.isEmpty()) {
                Toast.makeText(this, "请输入书名", Toast.LENGTH_SHORT).show();
                return;
            }

            book.setName(name);
            book.setPublisher(publisher);
            if (coverPath != null) {
                book.setCoverPath(coverPath);
            }
            // 不修改 createTime 和 readTime

            db.bookDao().updateBook(book);
            Toast.makeText(this, "修改成功", Toast.LENGTH_SHORT).show();
            finish();
        });

        btnDelete.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("删除书籍")
                    .setMessage("确定要删除《" + book.getName() + "》及其所有笔记吗？")
                    .setPositiveButton("删除", (dialog, which) -> {
                        db.bookDao().deleteBook(book);
                        Toast.makeText(this, "已删除", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .setNegativeButton("取消", null)
                    .show();
        });
    }

    private void initViews() {
        etBookName = findViewById(R.id.et_book_name_edit);
        etPublisher = findViewById(R.id.et_publisher_edit);
        ivCover = findViewById(R.id.iv_cover_edit);
    }

    private void loadBookInfo() {
        etBookName.setText(book.getName());
        etPublisher.setText(book.getPublisher());
        if (book.getCoverPath() != null && !book.getCoverPath().isEmpty()) {
            Glide.with(this).load(book.getCoverPath()).centerCrop().into(ivCover);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE && resultCode == RESULT_OK && data != null) {
            Uri imageUri = data.getData();
            coverPath = getRealPathFromURI(imageUri);
            Glide.with(this).load(imageUri).centerCrop().into(ivCover);
        }
    }

    private String getRealPathFromURI(Uri contentUri) {
        String[] projection = {MediaStore.Images.Media.DATA};
        Cursor cursor = getContentResolver().query(contentUri, projection, null, null, null);
        if (cursor == null) return null;
        int columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        cursor.moveToFirst();
        String path = cursor.getString(columnIndex);
        cursor.close();
        return path;
    }
}