package com.blacksun.quicknote.adapters;

import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.RecyclerView;

import com.blacksun.quicknote.R;
import com.blacksun.quicknote.models.Attachment;

import java.io.File;
import java.util.ArrayList;

public class FileRecyclerAdapter extends RecyclerView.Adapter<FileRecyclerAdapter.ViewHolder> {
    ArrayList<Attachment> files;

    public FileRecyclerAdapter(ArrayList<Attachment> files) {
        this.files = files;
    }

    @NonNull
    @Override
    public FileRecyclerAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View listView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_file, parent, false);
        ViewHolder viewHolder = new ViewHolder(listView);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull FileRecyclerAdapter.ViewHolder holder, int position) {
        String filePath = files.get(position).getPath();
        File newFile = new File(filePath);
        String fileName = newFile.getName();

        Log.d("filepath", "file recyclerView");

        String fileNameWithoutTime = fileName.substring(fileName.indexOf("_")+1);

        String ext = fileName.substring(fileName.lastIndexOf(".") + 1);
        holder.file.setText(fileNameWithoutTime);

        switch (ext) {
            case "pdf":
                holder.file.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_pdf, 0, 0, 0);
                break;
            case "mp3":
                holder.file.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_music_player, 0, 0, 0);
                break;
            default:
                holder.file.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_folder, 0,0,0);
        }
    }

    @Override
    public int getItemCount() {
        return files.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView file;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            file = itemView.findViewById(R.id.attach_file);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    File file = new File(files.get(getAdapterPosition()).getPath());
                    Intent intent = new Intent();
                    intent.setAction(android.content.Intent.ACTION_VIEW);
                    Uri fileUri = FileProvider.getUriForFile(v.getContext(),
                            "com.blacksun.quicknote.fileprovider",
                            file);
                    intent.setData(fileUri);
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    v.getContext().startActivity(intent);
                }
            });
        }
    }
}
