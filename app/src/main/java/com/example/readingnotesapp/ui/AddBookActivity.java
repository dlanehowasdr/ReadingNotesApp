package com.example.readingnotesapp.ui;

import android.content.ContentResolver;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
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

public class AddBookActivity extends AppCompatActivity {
    private static final int PICK_IMAGE = 100;
    private EditText etBookName, etPublisher;
    private ImageView ivCover;
    private String coverPath = null;
    private AppDatabase db;
    private Uri selectedImageUri = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_book);

        db = AppDatabase.getInstance(this);

        etBookName = findViewById(R.id.et_book_name);
        etPublisher = findViewById(R.id.et_publisher);
        ivCover = findViewById(R.id.iv_cover);
        Button btnSelectCover = findViewById(R.id.btn_select_cover);
        Button btnSave = findViewById(R.id.btn_save);

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

            Book book = new Book();
            book.setName(name);
            book.setPublisher(publisher);
            book.setCoverPath(coverPath);  // 保存封面路径
            book.setStatus("在读");
            book.setCreateTime(System.currentTimeMillis());
            book.setReadTime(0);
            book.setUserId(UserManager.getInstance(this).getCurrentUserId());

            db.bookDao().insertBook(book);
            Toast.makeText(this, "添加成功", Toast.LENGTH_SHORT).show();
            finish();
        });
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

            // 压缩图片（减少存储空间）
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
            inputStream.close();

            // 压缩并保存（质量80%，减少文件大小）
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