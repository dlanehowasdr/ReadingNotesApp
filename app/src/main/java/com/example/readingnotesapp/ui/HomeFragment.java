package com.example.readingnotesapp.ui;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.readingnotesapp.R;
import com.example.readingnotesapp.adapter.BookAdapter;
import com.example.readingnotesapp.data.AppDatabase;
import com.example.readingnotesapp.data.Book;
import com.example.readingnotesapp.data.Note;
import com.example.readingnotesapp.data.User;
import com.example.readingnotesapp.utils.DataExportUtils;
import com.example.readingnotesapp.utils.DataImportUtils;
import com.example.readingnotesapp.utils.UserManager;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class HomeFragment extends Fragment {
    private RecyclerView recyclerView;
    private BookAdapter bookAdapter;
    private TextView tvTotalBooks, tvReadBooks, tvCompletionRate;
    private AppDatabase db;
    private UserManager userManager;
    private int currentUserId;

    // 文件选择器（用于数据恢复）
    private final ActivityResultLauncher<String> filePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    importDataFromFile(uri);
                }
            });

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        db = AppDatabase.getInstance(getContext());
        userManager = UserManager.getInstance(getContext());
        currentUserId = userManager.getCurrentUserId();

        initViews(view);
        loadBooks();
        updateStatistics();

        view.findViewById(R.id.btn_add_book).setOnClickListener(v -> {
            startActivity(new Intent(getContext(), AddBookActivity.class));
        });

        return view;
    }

    private void initViews(View view) {
        recyclerView = view.findViewById(R.id.recycler_books);
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 3));

        tvTotalBooks = view.findViewById(R.id.tv_total_books);
        tvReadBooks = view.findViewById(R.id.tv_read_books);
        tvCompletionRate = view.findViewById(R.id.tv_completion_rate);

    }

    private void loadBooks() {
        List<Book> books = db.bookDao().getBooksByUserId(currentUserId);

        // 为每本书加载笔记条数
        for (Book book : books) {
            List<Note> notes = db.noteDao().getNotesByBookId(book.getId());
            book.setNoteCount(notes != null ? notes.size() : 0);
        }

        bookAdapter = new BookAdapter(books, book -> {
            Intent intent = new Intent(getContext(), BookDetailActivity.class);
            intent.putExtra("book_id", book.getId());
            startActivity(intent);
        });
        recyclerView.setAdapter(bookAdapter);
    }

    private void updateStatistics() {
        int total = db.bookDao().getBookCountByUserId(currentUserId);
        int read = db.bookDao().getReadCountByUserId(currentUserId);
        double rate = total > 0 ? (read * 100.0 / total) : 0;

        tvTotalBooks.setText("总书籍: " + total);
        tvReadBooks.setText("已读: " + read);
        tvCompletionRate.setText(String.format("完成率: %.1f%%", rate));
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.menu_main, menu);

        // 动态设置菜单标题：用户名 + ::
        User user = userManager.getCurrentUser();
        if (user != null) {
            MenuItem menuUser = menu.findItem(R.id.menu_user);
            if (menuUser != null) {
                menuUser.setTitle("👤 " + user.getNickname() + " ::");
            }
        }

        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.menu_register) {
            startActivity(new Intent(getContext(), RegisterActivity.class));
            return true;
        } else if (id == R.id.menu_switch_user) {
            showSwitchUserDialog();
            return true;
        } else if (id == R.id.menu_statistics) {
            showStatisticsDialog();
            return true;
        } else if (id == R.id.menu_data_backup) {
            exportData();
            return true;
        } else if (id == R.id.menu_data_restore) {
            showRestoreDialog();
            return true;
        } else if (id == R.id.menu_logout) {
            logoutUser();
            return true;
        } else if (id == R.id.menu_about) {
            showAboutDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // ==================== 数据备份 ====================
    private void exportData() {
        try {
            User user = userManager.getCurrentUser();
            if (user == null) {
                Toast.makeText(getContext(), "请先登录", Toast.LENGTH_SHORT).show();
                return;
            }

            List<Book> books = db.bookDao().getBooksByUserId(currentUserId);
            if (books.isEmpty()) {
                Toast.makeText(getContext(), "没有数据可以备份", Toast.LENGTH_SHORT).show();
                return;
            }

            // 获取所有书籍的笔记
            Map<Integer, List<Note>> notesMap = new HashMap<>();
            for (Book book : books) {
                List<Note> notes = db.noteDao().getNotesByBookId(book.getId());
                notesMap.put(book.getId(), notes);
            }

            // 导出数据
            File exportFile = DataExportUtils.exportUserData(getContext(), user, books, notesMap);

            // 分享文件
            shareFile(exportFile);

            Toast.makeText(getContext(), "数据备份成功！", Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "备份失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void shareFile(File file) {
        try {
            Uri fileUri;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                fileUri = FileProvider.getUriForFile(getContext(),
                        getContext().getPackageName() + ".fileprovider",
                        file);
            } else {
                fileUri = Uri.fromFile(file);
            }

            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("application/json");
            shareIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, "读书笔记数据备份");
            shareIntent.putExtra(Intent.EXTRA_TEXT, "这是《读书笔记》应用的数据备份文件，请妥善保管！");
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            startActivity(Intent.createChooser(shareIntent, "保存备份文件"));

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "分享失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    // ==================== 数据恢复 ====================
    private void showRestoreDialog() {
        new AlertDialog.Builder(getContext())
                .setTitle("⚠️ 数据恢复")
                .setMessage("恢复数据将清除当前用户的所有书籍和笔记数据，\n"
                        + "并替换为备份文件中的数据。\n\n"
                        + "请确认您已备份当前数据！")
                .setPositiveButton("选择文件", (dialog, which) -> {
                    filePickerLauncher.launch("application/json");
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void importDataFromFile(Uri uri) {
        try {
            String filePath = getRealPathFromUri(uri);
            if (filePath == null) {
                Toast.makeText(getContext(), "无法读取文件", Toast.LENGTH_SHORT).show();
                return;
            }

            File file = new File(filePath);

            DataImportUtils.ImportResult result = DataImportUtils.importData(getContext(), file, currentUserId);

            if (result.bookInfos.isEmpty()) {
                Toast.makeText(getContext(), "备份文件中没有书籍数据", Toast.LENGTH_SHORT).show();
                return;
            }

            int totalNotes = 0;
            for (DataImportUtils.BookInfo info : result.bookInfos) {
                totalNotes += info.notes.size();
            }
            final int finalTotalNotes = totalNotes;

            new AlertDialog.Builder(getContext())
                    .setTitle("确认恢复")
                    .setMessage("将恢复 " + result.bookInfos.size() + " 本书籍和 " +
                            finalTotalNotes + " 条笔记，\n"
                            + "当前用户的所有数据将被清除。\n\n"
                            + "确认继续？")
                    .setPositiveButton("确认恢复", (dialog, which) -> {
                        performDataRestore(result);
                    })
                    .setNegativeButton("取消", null)
                    .show();

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "恢复失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void performDataRestore(DataImportUtils.ImportResult result) {
        try {
            // 1. 清除当前用户的所有数据
            List<Book> oldBooks = db.bookDao().getBooksByUserId(currentUserId);
            for (Book book : oldBooks) {
                db.noteDao().deleteNotesByBookId(book.getId());
                db.bookDao().deleteBook(book);
            }

            // 2. 插入新数据
            for (DataImportUtils.BookInfo bookInfo : result.bookInfos) {
                Book book = bookInfo.book;
                db.bookDao().insertBook(book);
            }

            // 3. 获取新插入的书籍列表
            List<Book> newBooks = db.bookDao().getBooksByUserId(currentUserId);

            // 4. 插入笔记
            for (int i = 0; i < result.bookInfos.size() && i < newBooks.size(); i++) {
                DataImportUtils.BookInfo bookInfo = result.bookInfos.get(i);
                Book newBook = newBooks.get(i);

                List<Note> notes = bookInfo.notes;
                if (notes != null && !notes.isEmpty()) {
                    for (Note note : notes) {
                        note.setBookId(newBook.getId());
                        db.noteDao().insertNote(note);
                    }
                }
            }

            Toast.makeText(getContext(), "数据恢复成功！共恢复 " +
                    result.bookInfos.size() + " 本书籍", Toast.LENGTH_SHORT).show();
            loadBooks();
            updateStatistics();

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "恢复失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private String getRealPathFromUri(Uri uri) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                android.database.Cursor cursor = getContext().getContentResolver()
                        .query(uri, null, null, null, null);
                if (cursor != null && cursor.moveToFirst()) {
                    String displayName = cursor.getString(
                            cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME));
                    cursor.close();

                    java.io.InputStream is = getContext().getContentResolver().openInputStream(uri);
                    File tempFile = new File(getContext().getFilesDir(), displayName);
                    java.io.FileOutputStream fos = new java.io.FileOutputStream(tempFile);
                    byte[] buffer = new byte[1024];
                    int len;
                    while ((len = is.read(buffer)) != -1) {
                        fos.write(buffer, 0, len);
                    }
                    fos.close();
                    is.close();
                    return tempFile.getAbsolutePath();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        String[] projection = {android.provider.MediaStore.MediaColumns.DATA};
        android.database.Cursor cursor = getContext().getContentResolver()
                .query(uri, projection, null, null, null);
        if (cursor != null) {
            int columnIndex = cursor.getColumnIndexOrThrow(android.provider.MediaStore.MediaColumns.DATA);
            cursor.moveToFirst();
            String path = cursor.getString(columnIndex);
            cursor.close();
            return path;
        }
        return null;
    }

    // ==================== 其他菜单功能 ====================

    private void showSwitchUserDialog() {
        List<User> users = db.userDao().getAllUsers();
        if (users.isEmpty()) {
            Toast.makeText(getContext(), "没有其他用户", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] userNames = new String[users.size()];
        for (int i = 0; i < users.size(); i++) {
            User u = users.get(i);
            userNames[i] = u.getNickname() + " (" + u.getUsername() + ")";
        }

        new AlertDialog.Builder(getContext())
                .setTitle("切换用户")
                .setItems(userNames, (dialog, which) -> {
                    User selectedUser = users.get(which);
                    userManager.setCurrentUser(selectedUser);
                    currentUserId = selectedUser.getId();
                    Toast.makeText(getContext(), "切换到: " + selectedUser.getNickname(), Toast.LENGTH_SHORT).show();
                    loadBooks();
                    updateStatistics();

                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void showStatisticsDialog() {
        User user = userManager.getCurrentUser();
        int total = db.bookDao().getBookCountByUserId(currentUserId);
        int read = db.bookDao().getReadCountByUserId(currentUserId);
        double rate = total > 0 ? (read * 100.0 / total) : 0;

        // 统计笔记总数
        int totalNotes = 0;
        List<Book> books = db.bookDao().getBooksByUserId(currentUserId);
        for (Book book : books) {
            List<Note> notes = db.noteDao().getNotesByBookId(book.getId());
            totalNotes += notes != null ? notes.size() : 0;
        }

        // 获取当前时间
        long now = System.currentTimeMillis();
        java.util.Calendar cal = java.util.Calendar.getInstance();

        // 本周已读
        java.util.Calendar weekStart = java.util.Calendar.getInstance();
        weekStart.set(java.util.Calendar.DAY_OF_WEEK, java.util.Calendar.MONDAY);
        weekStart.set(java.util.Calendar.HOUR_OF_DAY, 0);
        weekStart.set(java.util.Calendar.MINUTE, 0);
        weekStart.set(java.util.Calendar.SECOND, 0);
        long weekStartTime = weekStart.getTimeInMillis();

        int weekRead = 0;
        for (Book b : books) {
            if ("已读".equals(b.getStatus()) && b.getReadTime() >= weekStartTime) {
                weekRead++;
            }
        }

        // 本月已读
        java.util.Calendar monthStart = java.util.Calendar.getInstance();
        monthStart.set(java.util.Calendar.DAY_OF_MONTH, 1);
        monthStart.set(java.util.Calendar.HOUR_OF_DAY, 0);
        monthStart.set(java.util.Calendar.MINUTE, 0);
        monthStart.set(java.util.Calendar.SECOND, 0);
        long monthStartTime = monthStart.getTimeInMillis();

        int monthRead = 0;
        for (Book b : books) {
            if ("已读".equals(b.getStatus()) && b.getReadTime() >= monthStartTime) {
                monthRead++;
            }
        }

        // 本年已读
        java.util.Calendar yearStart = java.util.Calendar.getInstance();
        yearStart.set(java.util.Calendar.DAY_OF_YEAR, 1);
        yearStart.set(java.util.Calendar.HOUR_OF_DAY, 0);
        yearStart.set(java.util.Calendar.MINUTE, 0);
        yearStart.set(java.util.Calendar.SECOND, 0);
        long yearStartTime = yearStart.getTimeInMillis();

        int yearRead = 0;
        for (Book b : books) {
            if ("已读".equals(b.getStatus()) && b.getReadTime() >= yearStartTime) {
                yearRead++;
            }
        }

        // 计算周数、月数
        java.util.Calendar nowCal = java.util.Calendar.getInstance();
        int dayOfYear = nowCal.get(java.util.Calendar.DAY_OF_YEAR);
        int weeksSinceYearStart = (int) Math.ceil(dayOfYear / 7.0);
        int monthsSinceYearStart = nowCal.get(java.util.Calendar.MONTH) + 1;

        double weekAvg = weeksSinceYearStart > 0 ? (double) yearRead / weeksSinceYearStart : 0;
        double monthAvg = monthsSinceYearStart > 0 ? (double) yearRead / monthsSinceYearStart : 0;

        String message = "📊 数据统计\n\n" +
                "👤 用户: " + (user != null ? user.getNickname() : "") + "\n" +
                "📚 总书籍: " + total + " 本\n" +
                "✅ 已读: " + read + " 本\n" +
                "📝 笔记总数: " + totalNotes + " 条\n" +
                "📈 完成率: " + String.format("%.1f%%", rate) + "\n\n" +
                "📅 本周已读: " + weekRead + " 本\n" +
                "📊 周平均阅读: " + String.format("%.1f", weekAvg) + " 本\n\n" +
                "📅 本月已读: " + monthRead + " 本\n" +
                "📊 月平均阅读: " + String.format("%.1f", monthAvg) + " 本\n\n" +
                "📅 本年已读: " + yearRead + " 本";

        new AlertDialog.Builder(getContext())
                .setTitle("📊 数据统计")
                .setMessage(message)
                .setPositiveButton("确定", null)
                .show();
    }

    private void logoutUser() {
        new AlertDialog.Builder(getContext())
                .setTitle("注销用户")
                .setMessage("确定要注销当前用户吗？该用户的所有数据将被清除！")
                .setPositiveButton("确定", (dialog, which) -> {
                    db.bookDao().deleteBooksByUserId(currentUserId);
                    db.userDao().deleteUser(userManager.getCurrentUser());
                    userManager.logout();

                    Intent intent = new Intent(getContext(), LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    getActivity().finish();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void showAboutDialog() {
        String message = "📚 读书笔记 v1.0\n\n" +
                "功能：\n" +
                "· 记录和管理读书笔记\n" +
                "· 支持多用户切换\n" +
                "· 数据备份与恢复\n" +
                "· 数据统计\n\n" +
                "❤️ 感谢使用！";

        new AlertDialog.Builder(getContext())
                .setTitle("关于")
                .setMessage(message)
                .setPositiveButton("确定", null)
                .show();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadBooks();
        updateStatistics();
        // 刷新菜单
        getActivity().invalidateOptionsMenu();
    }
}