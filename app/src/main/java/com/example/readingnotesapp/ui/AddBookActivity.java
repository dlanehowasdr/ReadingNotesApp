package com.example.readingnotes.ui;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;
import com.example.readingnotes.R;
import com.example.readingnotes.data.AppDatabase;
import com.example.readingnotes.data.Book;

public class AddBookActivity extends AppCompatActivity {
    private static final int PICK_IMAGE = 100;
    private EditText etBookName, etPublisher;
    private ImageView ivCover;
    private String coverPath = null;
    private AppDatabase db;

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
            book.setCoverPath(coverPath);
            book.setStatus("在读");

            db.bookDao().insertBook(book);
            Toast.makeText(this, "添加成功", Toast.LENGTH_SHORT).show();
            finish();
        });
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