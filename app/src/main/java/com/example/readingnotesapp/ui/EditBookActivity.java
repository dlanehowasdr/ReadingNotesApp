package com.example.readingnotesapp.ui;

import android.content.ContentResolver;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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
import com.example.readingnotesapp.utils.UserManager;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class EditBookActivity extends AppCompatActivity {
    private static final int PICK_IMAGE = 100;
    private int bookId;
    private Book book;
    private EditText etBookName, etPublisher;
    private ImageView ivCover;
    private String coverPath;
    private AppDatabase db;
    private Uri selectedImageUri = null;

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

        book = db.bookDao().getBookById(bookId, UserManager.getInstance(this).getCurrentUserId());
        if (book == null) {
            Toast.makeText(this, "书籍不存在或无权访问", Toast.LENGTH_SHORT).show();  // ★★★ 修改这里 ★★★
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

            db.bookDao().updateBook(book);
            Toast.makeText(this, "修改成功", Toast.LENGTH_SHORT).show();
            finish();
        });

        btnDelete.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("删除书籍")
                    .setMessage("确定要删除《" + book.getName() + "》及其所有笔记吗？")
                    .setPositiveButton("删除", (dialog, which) -> {
                        // 删除封面图片文件
                        if (book.getCoverPath() != null) {
                            File coverFile = new File(book.getCoverPath());
                            if (coverFile.exists()) {
                                coverFile.delete();
                            }
                        }
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

        // 显示封面
        if (book.getCoverPath() != null && !book.getCoverPath().isEmpty()) {
            File coverFile = new File(book.getCoverPath());
            if (coverFile.exists()) {
                Glide.with(this)
                        .load(coverFile)
                        .centerCrop()
                        .placeholder(R.drawable.ic_book_placeholder)
                        .error(R.drawable.ic_book_placeholder)
                        .into(ivCover);
            } else {
                ivCover.setImageResource(R.drawable.ic_book_placeholder);
            }
        } else {
            ivCover.setImageResource(R.drawable.ic_book_placeholder);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE && resultCode == RESULT_OK && data != null) {
            selectedImageUri = data.getData();
            if (selectedImageUri != null) {
                // 显示选中的图片
                Glide.with(this)
                        .load(selectedImageUri)
                        .centerCrop()
                        .into(ivCover);

                // 保存图片到应用内部存储
                coverPath = saveImageToInternalStorage(selectedImageUri);
            }
        }
    }

    /**
     * 保存图片到应用内部存储
     */
    private String saveImageToInternalStorage(Uri imageUri) {
        try {
            // 创建封面存储目录
            File coverDir = new File(getFilesDir(), "covers");
            if (!coverDir.exists()) {
                coverDir.mkdirs();
            }

            // 生成唯一文件名
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            String fileName = "cover_" + timeStamp + ".jpg";
            File destFile = new File(coverDir, fileName);

            // 复制图片到内部存储
            ContentResolver resolver = getContentResolver();
            InputStream inputStream = resolver.openInputStream(imageUri);

            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
            inputStream.close();

            // 压缩并保存
            FileOutputStream outputStream = new FileOutputStream(destFile);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream);
            outputStream.close();
            bitmap.recycle();

            return destFile.getAbsolutePath();

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "保存封面失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            return null;
        }
    }
}