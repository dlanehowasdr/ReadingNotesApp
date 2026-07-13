package com.example.readingnotesapp.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import com.example.readingnotesapp.data.Book;
import com.example.readingnotesapp.data.Note;
import com.example.readingnotesapp.data.User;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class DataExportUtils {

    /**
     * 导出用户数据为JSON文件
     */
    public static File exportUserData(Context context, User user, List<Book> books,
                                      java.util.Map<Integer, List<Note>> notesMap) throws Exception {
        JSONObject root = new JSONObject();

        // 导出时间
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        root.put("exportTime", sdf.format(System.currentTimeMillis()));
        root.put("version", "1.0");

        // 用户信息
        JSONObject userInfo = new JSONObject();
        userInfo.put("username", user.getUsername());
        userInfo.put("nickname", user.getNickname());
        root.put("userInfo", userInfo);

        // 书籍列表
        JSONArray booksArray = new JSONArray();
        for (Book book : books) {
            JSONObject bookObj = new JSONObject();
            bookObj.put("name", book.getName());
            bookObj.put("publisher", book.getPublisher() != null ? book.getPublisher() : "");
            bookObj.put("status", book.getStatus());
            bookObj.put("createTime", book.getCreateTime());
            bookObj.put("readTime", book.getReadTime());

            // 封面图片（Base64编码）
            if (book.getCoverPath() != null && !book.getCoverPath().isEmpty()) {
                String coverBase64 = encodeImageToBase64(book.getCoverPath());
                bookObj.put("coverBase64", coverBase64);
            }

            // 该书的笔记列表
            List<Note> notes = notesMap.get(book.getId());
            if (notes != null && !notes.isEmpty()) {
                JSONArray notesArray = new JSONArray();
                for (Note note : notes) {
                    JSONObject noteObj = new JSONObject();
                    noteObj.put("content", note.getContent());
                    noteObj.put("createTime", note.getCreateTime());
                    notesArray.put(noteObj);
                }
                bookObj.put("notes", notesArray);
            }

            booksArray.put(bookObj);
        }
        root.put("books", booksArray);

        // 保存到文件
        String filename = "读书笔记备份_" + user.getUsername() + "_" +
                new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(System.currentTimeMillis()) + ".json";
        File file = new File(context.getExternalFilesDir(null), filename);

        FileWriter writer = new FileWriter(file);
        writer.write(root.toString(2));
        writer.close();

        return file;
    }

    /**
     * 将图片文件编码为Base64字符串
     */
    private static String encodeImageToBase64(String imagePath) {
        try {
            Bitmap bitmap = BitmapFactory.decodeFile(imagePath);
            if (bitmap == null) {
                return "";
            }
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos);
            byte[] imageBytes = baos.toByteArray();
            return Base64.encodeToString(imageBytes, Base64.DEFAULT);
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    /**
     * 从Base64字符串解码为图片文件
     */
    public static String decodeBase64ToImage(Context context, String base64, String filename) throws Exception {
        if (base64 == null || base64.isEmpty()) {
            return null;
        }

        byte[] imageBytes = Base64.decode(base64, Base64.DEFAULT);
        Bitmap bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);

        if (bitmap == null) {
            return null;
        }

        File dir = new File(context.getFilesDir(), "covers");
        if (!dir.exists()) {
            dir.mkdirs();
        }

        File file = new File(dir, filename);
        FileOutputStream fos = new FileOutputStream(file);
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, fos);
        fos.close();

        return file.getAbsolutePath();
    }
}