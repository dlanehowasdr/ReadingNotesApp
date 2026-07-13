package com.example.readingnotesapp.utils;

import android.content.Context;
import com.example.readingnotesapp.data.Book;
import com.example.readingnotesapp.data.Note;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

public class DataImportUtils {

    /**
     * 从JSON文件导入数据
     */
    public static ImportResult importData(Context context, File file, int userId) throws Exception {
        // 读取JSON文件
        StringBuilder sb = new StringBuilder();
        BufferedReader reader = new BufferedReader(new FileReader(file));
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line);
        }
        reader.close();

        JSONObject root = new JSONObject(sb.toString());

        // 解析用户信息
        JSONObject userInfo = root.getJSONObject("userInfo");
        String username = userInfo.getString("username");
        String nickname = userInfo.getString("nickname");

        // 解析书籍列表
        JSONArray booksArray = root.getJSONArray("books");
        List<BookInfo> bookInfos = new ArrayList<>();

        for (int i = 0; i < booksArray.length(); i++) {
            JSONObject bookObj = booksArray.getJSONObject(i);

            BookInfo bookInfo = new BookInfo();

            Book book = new Book();
            book.setName(bookObj.getString("name"));
            book.setPublisher(bookObj.optString("publisher", ""));
            book.setStatus(bookObj.getString("status"));
            book.setCreateTime(bookObj.getLong("createTime"));
            book.setReadTime(bookObj.optLong("readTime", 0));
            book.setUserId(userId);

            // 恢复封面图片
            String coverBase64 = bookObj.optString("coverBase64", "");
            if (!coverBase64.isEmpty()) {
                String coverPath = DataExportUtils.decodeBase64ToImage(context, coverBase64,
                        "cover_" + System.currentTimeMillis() + "_" + i + ".jpg");
                book.setCoverPath(coverPath);
            }

            bookInfo.book = book;

            // 解析笔记
            if (bookObj.has("notes")) {
                JSONArray notesArray = bookObj.getJSONArray("notes");
                List<Note> notes = new ArrayList<>();
                for (int j = 0; j < notesArray.length(); j++) {
                    JSONObject noteObj = notesArray.getJSONObject(j);
                    Note note = new Note();
                    note.setContent(noteObj.getString("content"));
                    note.setCreateTime(noteObj.getLong("createTime"));
                    notes.add(note);
                }
                bookInfo.notes = notes;
            }

            bookInfos.add(bookInfo);
        }

        return new ImportResult(bookInfos);
    }

    public static class BookInfo {
        public Book book;
        public List<Note> notes = new ArrayList<>();
    }

    public static class ImportResult {
        public List<BookInfo> bookInfos;

        public ImportResult(List<BookInfo> bookInfos) {
            this.bookInfos = bookInfos;
        }
    }
}