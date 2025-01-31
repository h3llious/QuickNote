package com.blacksun.quicknote.adapters;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.RoundedBitmapDrawable;
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory;
import androidx.recyclerview.widget.RecyclerView;

import com.blacksun.quicknote.R;
import com.blacksun.quicknote.activities.DetailActivity;
import com.blacksun.quicknote.controllers.AttachManager;
import com.blacksun.quicknote.data.NoteContract;
import com.blacksun.quicknote.models.Attachment;
import com.blacksun.quicknote.models.Note;
import com.blacksun.quicknote.utils.UtilHelper;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;

public class NoteRecyclerAdapter extends RecyclerView.Adapter<NoteRecyclerAdapter.ViewHolder> {
    private ArrayList<Note> notes;

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

        String tempContent = note.getContent().replaceAll("\\$.*?\\$", "<IMG>");
        holder.textContent.setText(tempContent);

        ArrayList<Attachment> curAttaches = AttachManager.newInstance(holder.itemView.getContext())
                .getAttach(note.getId(), NoteContract.AttachEntry.ANY_TYPE);

        if (curAttaches.size() != 0) {
            Attachment curAttach = curAttaches.get(curAttaches.size() - 1);
            if (curAttach.getType().equals(NoteContract.AttachEntry.FILE_TYPE)) {
                holder.img.setImageDrawable(ContextCompat.getDrawable(holder.itemView.getContext(), R.drawable.ic_attach));
            } else {
                File checkedImg = new File(curAttach.getPath());
                if (checkedImg.exists()) {

                    int height = holder.itemView.getContext().getResources().getDimensionPixelSize(R.dimen.listPreferredItemHeightLarge);

                    final Handler handler = new Handler();
                    new Thread() {
                        public void run() {
                            // Do time-consuming initialization.
                            Bitmap thumbImage = UtilHelper.createThumbnail(curAttach.getPath(), height, height);

                            RoundedBitmapDrawable roundedBitmapDrawable = RoundedBitmapDrawableFactory
                                    .create(holder.itemView.getContext().getResources(), thumbImage);
                            final float roundPx = (float) thumbImage.getWidth() * 0.1f;
                            roundedBitmapDrawable.setCornerRadius(roundPx);
                            // When done:
                            handler.post(new Runnable() {
                                public void run() {
                                    // set up the real UI
                                    holder.img.setImageDrawable(roundedBitmapDrawable);
                                }
                            });
                        }
                    }.start();
                } else {
                    Log.e("Note Adapter", "Error getting image");
                }
            }
        } else {
            holder.img.setImageDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        //change date time to x mins ago if possible
        long moddedTime = note.getDateModified();
        long currentTime = System.currentTimeMillis();

        long timeDiff = currentTime - moddedTime;

        if (timeDiff< 60000) {
            holder.textTime.setText(holder.itemView.getResources().getString(R.string.note_time_recently));
        } else if (timeDiff < 3600000) {
            long minute = timeDiff / 60000;
            String timeModified = minute + holder.itemView.getResources().getString(R.string.note_time_past);
            holder.textTime.setText(timeModified);
        } else {
            holder.textTime.setText(getDate(note.getDateModified(), "dd/MM/yyyy HH:mm"));
        }

        holder.itemView.setTag(position);
    }

    @Override
    public int getItemCount() {
        return notes.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView textTitle, textContent, textTime;
        ImageView img;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            textTitle = itemView.findViewById(R.id.note_title);
            textTitle.setSelected(true);

            textContent = itemView.findViewById(R.id.note_content);
            textTime = itemView.findViewById(R.id.note_time);
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

                    v.getContext().startActivity(intent);
                }
            });
        }
    }

    public static String getDate(long milliSeconds, String dateFormat) {
        // Create a DateFormatter object for displaying date in specified format.
        SimpleDateFormat formatter = new SimpleDateFormat(dateFormat);

        // Create a calendar object that will convert the date and time value in milliseconds to date.
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(milliSeconds);
        return formatter.format(calendar.getTime());
    }

    @Override
    public long getItemId(int position) {
        return notes.get(position).getId();
    }

    public void removeItem(int position) {
        notes.remove(position);
        // notify the item removed by position
        // to perform recycler view delete animations
        notifyItemRemoved(position);
    }

    public void restoreItem(Note item, int position) {
        notes.add(position, item);
        // notify item added by position
        notifyItemInserted(position);
    }

    public void setFilter(ArrayList<Note> newList){
        notes.clear();
        notes.addAll(newList);
        notifyDataSetChanged();
    }
}
