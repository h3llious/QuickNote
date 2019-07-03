package com.blacksun.quicknote.adapters;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ThumbnailUtils;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
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
        holder.textTime.setText(getDate(note.getDateModified(),"dd/MM/yyyy HH:mm"));
        holder.textTimeCreated.setText(getDate(note.getDateCreated(),"dd/MM/yyyy HH:mm"));

        if (!TextUtils.isEmpty(note.getImagePath()))
        {
            int height = holder.itemView.getContext().getResources().getDimensionPixelSize(R.dimen.listPreferredItemHeightLarge);

            Bitmap thumbImage = ThumbnailUtils.extractThumbnail(BitmapFactory.decodeFile(note.getImagePath()), height, height);
            holder.img.setImageBitmap(thumbImage);
        }

        //TODO: change date time to x mins ago if possible

        holder.itemView.setTag(position);
    }

    @Override
    public int getItemCount() {
        return notes.size();
    }


    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView textTitle, textContent, textTime, textTimeCreated;
        ImageView img;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            textTitle = itemView.findViewById(R.id.note_title);
            textTitle.setSelected(true);

            textContent = itemView.findViewById(R.id.note_content);
            textTime = itemView.findViewById(R.id.note_time);
            textTimeCreated = itemView.findViewById(R.id.note_time_created);
            img = itemView.findViewById(R.id.note_img);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(v.getContext(), DetailActivity.class);
                    intent.putExtra("noteID", notes.get(getAdapterPosition()).getId());
                    intent.putExtra("title", notes.get(getAdapterPosition()).getTitle());
                    intent.putExtra("content", notes.get(getAdapterPosition()).getContent());
                    intent.putExtra("dateModified", notes.get(getAdapterPosition()).getDateModified());
                    intent.putExtra("dateCreated", notes.get(getAdapterPosition()).getDateCreated());
                    intent.putExtra("imagePath", notes.get(getAdapterPosition()).getImagePath());

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
