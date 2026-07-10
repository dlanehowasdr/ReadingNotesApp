package com.example.readingnotesapp.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.readingnotesapp.R;
import com.example.readingnotesapp.adapter.BookAdapter;
import com.example.readingnotesapp.data.AppDatabase;
import com.example.readingnotesapp.data.Book;
import java.util.List;

public class HomeFragment extends Fragment {
    private RecyclerView recyclerView;
    private BookAdapter bookAdapter;
    private TextView tvTotalBooks, tvReadBooks, tvCompletionRate;
    private AppDatabase db;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        db = AppDatabase.getInstance(requireContext());

        initViews(view);
        loadBooks();
        updateStatistics();

        view.findViewById(R.id.btn_add_book).setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), AddBookActivity.class);
            startActivity(intent);
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
        List<Book> books = db.bookDao().getAllBooks();
        bookAdapter = new BookAdapter(books, book -> {
            Intent intent = new Intent(getActivity(), BookDetailActivity.class);
            intent.putExtra("book_id", book.getId());
            startActivity(intent);
        });
        recyclerView.setAdapter(bookAdapter);
    }

    private void updateStatistics() {
        int total = db.bookDao().getBookCount();
        int read = db.bookDao().getReadCount();
        double rate = total > 0 ? (read * 100.0 / total) : 0;

        tvTotalBooks.setText("总书籍: " + total);
        tvReadBooks.setText("已读: " + read);
        tvCompletionRate.setText(String.format("完成率: %.1f%%", rate));
    }

    @Override
    public void onResume() {
        super.onResume();
        // 刷新数据
        loadBooks();
        updateStatistics();
    }
}