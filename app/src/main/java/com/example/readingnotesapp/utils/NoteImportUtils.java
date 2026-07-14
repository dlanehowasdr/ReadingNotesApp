package com.example.readingnotesapp.utils;

import android.content.Context;
import android.net.Uri;
import com.example.readingnotesapp.data.Note;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class NoteImportUtils {

    // 两个汉字的空格（全角空格）
    private static final String INDENT = "　　";

    /**
     * 从导出的笔记文件中解析笔记列表（支持多段落，自动添加缩进）
     */
    public static List<Note> parseNotesFromFile(Context context, Uri fileUri) throws Exception {
        List<Note> notes = new ArrayList<>();

        // 读取文件内容
        StringBuilder content = new StringBuilder();
        InputStream inputStream = context.getContentResolver().openInputStream(fileUri);
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        String line;
        while ((line = reader.readLine()) != null) {
            content.append(line).append("\n");
        }
        reader.close();
        inputStream.close();

        String text = content.toString();
        String[] lines = text.split("\n");

        int i = 0;
        while (i < lines.length) {
            String currentLine = lines[i].trim();

            // 匹配笔记标题行：数字. 日期时间
            if (currentLine.matches("^\\d+\\.\\s+\\d{4}-\\d{2}-\\d{2}\\s+\\d{2}:\\d{2}$")) {
                // 提取时间
                String timeStr = currentLine.replaceFirst("^\\d+\\.\\s+", "").trim();
                long createTime = parseTimeToMillis(timeStr);

                // 收集笔记内容（支持多段落）
                StringBuilder noteContent = new StringBuilder();
                i++; // 移到下一行

                // 继续读取直到遇到分隔线或文件结束
                while (i < lines.length) {
                    String contentLine = lines[i];

                    // 检查是否是分隔线（以 --- 开头）
                    if (contentLine.trim().startsWith("-")) {
                        break;
                    }

                    // 检查是否遇到下一条笔记的标题
                    if (contentLine.trim().matches("^\\d+\\.\\s+\\d{4}-\\d{2}-\\d{2}\\s+\\d{2}:\\d{2}$")) {
                        break;
                    }

                    // ★★★ 为每个非空段落添加缩进（如果已有缩进则不重复添加） ★★★
                    String trimmedLine = contentLine.trim();
                    if (!trimmedLine.isEmpty()) {
                        if (noteContent.length() > 0) {
                            noteContent.append("\n");
                        }
                        // 检查是否已有缩进（以全角空格或两个全角空格开头）
                        String indentedLine = addIndentIfNeeded(trimmedLine);
                        noteContent.append(indentedLine);
                    }

                    i++;
                }

                // 如果笔记内容不为空，添加到列表
                String finalContent = noteContent.toString().trim();
                if (!finalContent.isEmpty()) {
                    Note note = new Note();
                    note.setContent(finalContent);
                    note.setCreateTime(createTime);
                    notes.add(note);
                }

                // 如果已经到达文件末尾，跳出循环
                if (i >= lines.length) {
                    break;
                }
            } else {
                i++;
            }
        }

        return notes;
    }

    /**
     * 从JSON格式的备份文件中解析笔记列表
     * JSON中的笔记也添加缩进
     */
    public static List<Note> parseNotesFromJson(Context context, Uri fileUri, String bookName) throws Exception {
        List<Note> notes = new ArrayList<>();

        InputStream inputStream = context.getContentResolver().openInputStream(fileUri);
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line);
        }
        reader.close();
        inputStream.close();

        JSONObject root = new JSONObject(sb.toString());
        JSONArray booksArray = root.getJSONArray("books");

        // 查找匹配的书籍
        for (int i = 0; i < booksArray.length(); i++) {
            JSONObject bookObj = booksArray.getJSONObject(i);
            String name = bookObj.getString("name");

            if (name.equals(bookName) && bookObj.has("notes")) {
                JSONArray notesArray = bookObj.getJSONArray("notes");
                for (int j = 0; j < notesArray.length(); j++) {
                    JSONObject noteObj = notesArray.getJSONObject(j);
                    String content = noteObj.getString("content");

                    // ★★★ 为JSON中的笔记内容添加缩进（如果已有则不重复添加） ★★★
                    String indentedContent = addIndentToContent(content);

                    Note note = new Note();
                    note.setContent(indentedContent);
                    note.setCreateTime(noteObj.getLong("createTime"));
                    notes.add(note);
                }
                break;
            }
        }

        return notes;
    }

    /**
     * ★★★ 判断是否需要添加缩进，如果需要则添加 ★★★
     */
    private static String addIndentIfNeeded(String line) {
        // 检查是否已有缩进（以全角空格开头）
        if (line.startsWith("　")) {
            return line; // 已有缩进，直接返回
        }
        // 检查是否已有缩进（以两个半角空格开头）
        if (line.startsWith("  ")) {
            return line; // 已有缩进，直接返回
        }
        // 没有缩进，添加两个全角空格
        return INDENT + line;
    }

    /**
     * 为笔记内容添加缩进（每行前加两个全角空格）
     * 如果已经有缩进则不重复添加
     */
    private static String addIndentToContent(String content) {
        if (content == null || content.isEmpty()) {
            return content;
        }

        String[] lines = content.split("\n");
        StringBuilder result = new StringBuilder();

        for (int i = 0; i < lines.length; i++) {
            String trimmedLine = lines[i].trim();
            if (!trimmedLine.isEmpty()) {
                if (result.length() > 0) {
                    result.append("\n");
                }
                // 判断是否需要添加缩进
                String indentedLine = addIndentIfNeeded(trimmedLine);
                result.append(indentedLine);
            }
        }

        return result.toString();
    }

    /**
     * 将时间字符串转换为毫秒时间戳
     */
    private static long parseTimeToMillis(String timeStr) {
        try {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault());
            java.util.Date date = sdf.parse(timeStr);
            return date != null ? date.getTime() : System.currentTimeMillis();
        } catch (Exception e) {
            return System.currentTimeMillis();
        }
    }

    /**
     * 过滤掉已存在的笔记（根据内容和时间去重）
     */
    public static List<Note> filterExistingNotes(List<Note> newNotes, List<Note> existingNotes) {
        if (newNotes == null || newNotes.isEmpty()) {
            return new ArrayList<>();
        }

        if (existingNotes == null || existingNotes.isEmpty()) {
            return newNotes;
        }

        // 使用Set记录已存在的笔记（内容+时间）
        Set<String> existingSet = new HashSet<>();
        for (Note note : existingNotes) {
            String key = note.getContent() + "_" + note.getCreateTime();
            existingSet.add(key);
        }

        // 过滤
        List<Note> result = new ArrayList<>();
        for (Note note : newNotes) {
            String key = note.getContent() + "_" + note.getCreateTime();
            if (!existingSet.contains(key)) {
                result.add(note);
            }
        }

        return result;
    }
}