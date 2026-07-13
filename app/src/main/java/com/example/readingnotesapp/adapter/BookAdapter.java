package com.example.readingnotesapp.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.readingnotesapp.R;
import com.example.readingnotesapp.data.Book;
import java.io.File;
import java.util.List;

public class BookAdapter extends RecyclerView.Adapter<BookAdapter.BookViewHolder> {
    private List<Book> bookList;
    private OnBookClickListener listener;

    public interface OnBookClickListener {
        void onBookClick(Book book);
    }

    public BookAdapter(List<Book> bookList, OnBookClickListener listener) {
        this.bookList = bookList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public BookViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_book, parent, false);
        return new BookViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BookViewHolder holder, int position) {
        Book book = bookList.get(position);
        holder.bookName.setText(book.getName());

        // 设置状态和笔记条数
        String status = book.getStatus();
        holder.bookStatus.setText(status);

        // “已读”用粗体显示
        if ("已读".equals(status)) {
            holder.bookStatus.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
            holder.bookStatus.setTextColor(holder.itemView.getContext().getColor(android.R.color.holo_green_dark));
        } else {
            holder.bookStatus.setTypeface(android.graphics.Typeface.DEFAULT);
            holder.bookStatus.setTextColor(holder.itemView.getContext().getColor(android.R.color.holo_blue_dark));
        }

        // 显示笔记条数
        int noteCount = book.getNoteCount();
        holder.bookNoteCount.setText("(" + noteCount + "条)");

        // 显示封面
        if (book.getCoverPath() != null && !book.getCoverPath().isEmpty()) {
            File coverFile = new File(book.getCoverPath());
            if (coverFile.exists()) {
                Glide.with(holder.itemView.getContext())
                        .load(coverFile)
                        .centerCrop()
                        .placeholder(R.drawable.ic_book_placeholder)
                        .error(R.drawable.ic_book_placeholder)
                        .into(holder.bookCover);
            } else {
                holder.bookCover.setImageResource(R.drawable.ic_book_placeholder);
            }
        } else {
            holder.bookCover.setImageResource(R.drawable.ic_book_placeholder);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onBookClick(book);
            }
        });
    }

    @Override
    public int getItemCount() {
        return bookList.size();
    }

    static class BookViewHolder extends RecyclerView.ViewHolder {
        ImageView bookCover;
        TextView bookName;
        TextView bookStatus;
        TextView bookNoteCount;

        BookViewHolder(@NonNull View itemView) {
            super(itemView);
            bookCover = itemView.findViewById(R.id.book_cover);
            bookName = itemView.findViewById(R.id.book_name);
            bookStatus = itemView.findViewById(R.id.book_status);
            bookNoteCount = itemView.findViewById(R.id.book_note_count);
        }
    }

    public void updateBooks(List<Book> newBooks) {
        this.bookList = newBooks;
        notifyDataSetChanged();
    }
}