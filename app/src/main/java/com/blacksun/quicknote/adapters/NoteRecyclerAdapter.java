package com.blacksun.quicknote.adapters;

import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.blacksun.quicknote.R;
import com.blacksun.quicknote.activities.DetailActivity;
import com.blacksun.quicknote.activities.MainActivity;
import com.blacksun.quicknote.models.Note;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;

public class NoteRecyclerAdapter extends RecyclerView.Adapter<NoteRecyclerAdapter.ViewHolder> {
    ArrayList<Note> notes;

    public NoteRecyclerAdapter(ArrayList<Note> notes) {
        this.notes = notes;
    }

    @NonNull
    @Override
    public NoteRecyclerAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View listItem = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_note, parent, false);
        ViewHolder viewHolder = new ViewHolder(listItem);

        Log.d("TestRecycler", "Not good");
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull NoteRecyclerAdapter.ViewHolder holder, int position) {
        final Note note = notes.get(position);
        holder.textTitle.setText(note.getTitle());
        holder.textContent.setText(note.getContent());
        holder.textTime.setText(getDate(note.getDateModified(),"dd/MM/yyyy hh:mm"));
        //TODO: change date time to x mins ago if possible

        holder.itemView.setTag(position);
    }

    @Override
    public int getItemCount() {
        return notes.size();
    }


    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView textTitle, textContent, textTime;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            textTitle = itemView.findViewById(R.id.note_title);
            textTitle.setSelected(true);

            textContent = itemView.findViewById(R.id.note_content);
            textTime = itemView.findViewById(R.id.note_time);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(v.getContext(), DetailActivity.class);
                    intent.putExtra("noteID", getAdapterPosition());
                    intent.putExtra("title", notes.get(getAdapterPosition()).getTitle());
                    intent.putExtra("content", notes.get(getAdapterPosition()).getContent());
                    intent.putExtra("dateModified", notes.get(getAdapterPosition()).getDateModified());
                    intent.putExtra("dateCreated", notes.get(getAdapterPosition()).getDateCreated());

                    v.getContext().startActivity(intent);
                }
            });
        }
    }

    public static String getDate(long milliSeconds, String dateFormat)
    {
        // Create a DateFormatter object for displaying date in specified format.
        SimpleDateFormat formatter = new SimpleDateFormat(dateFormat);

        // Create a calendar object that will convert the date and time value in milliseconds to date.
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(milliSeconds);
        return formatter.format(calendar.getTime());
    }
}
