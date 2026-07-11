package com.example.readingnotesapp.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.readingnotesapp.R;
import com.example.readingnotesapp.data.Note;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class NoteAdapter extends RecyclerView.Adapter<NoteAdapter.NoteViewHolder> {
    private List<Note> noteList;
    private OnNoteClickListener listener;

    public interface OnNoteClickListener {
        void onNoteClick(Note note);
    }

    public NoteAdapter(List<Note> noteList, OnNoteClickListener listener) {
        this.noteList = noteList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public NoteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_note, parent, false);
        return new NoteViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NoteViewHolder holder, int position) {
        Note note = noteList.get(position);

        // 显示序号（从1开始）
        int noteNumber = position + 1;
        holder.noteNumber.setText(noteNumber + ".");
        holder.noteContent.setText(note.getContent());

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
        holder.noteTime.setText(sdf.format(note.getCreateTime()));

        // 点击笔记进入编辑
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onNoteClick(note);
            }
        });
    }

    @Override
    public int getItemCount() {
        return noteList.size();
    }

    static class NoteViewHolder extends RecyclerView.ViewHolder {
        TextView noteNumber;
        TextView noteContent;
        TextView noteTime;

        NoteViewHolder(@NonNull View itemView) {
            super(itemView);
            noteNumber = itemView.findViewById(R.id.note_number);
            noteContent = itemView.findViewById(R.id.note_content);
            noteTime = itemView.findViewById(R.id.note_time);
        }
    }

    public void updateNotes(List<Note> newNotes) {
        this.noteList = newNotes;
        notifyDataSetChanged();
    }
}