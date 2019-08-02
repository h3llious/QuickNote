package com.blacksun.quicknote.adapters;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.RecyclerView;

import com.blacksun.quicknote.R;
import com.blacksun.quicknote.activities.DetailActivity;
import com.blacksun.quicknote.controllers.AttachManager;
import com.blacksun.quicknote.models.Attachment;
import com.blacksun.quicknote.utils.UtilHelper;

import java.io.File;
import java.util.ArrayList;

import static com.blacksun.quicknote.activities.DetailActivity.REQUEST_CHANGE;

public class FileRecyclerAdapter extends RecyclerView.Adapter<FileRecyclerAdapter.ViewHolder> {
    ArrayList<Attachment> files;
    Context context;

    public FileRecyclerAdapter(ArrayList<Attachment> files, Context context) {
        this.files = files;
        this.context = context;
    }

    @NonNull
    @Override
    public FileRecyclerAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View listView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_file, parent, false);
        ViewHolder viewHolder = new ViewHolder(listView);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull FileRecyclerAdapter.ViewHolder holder, final int position) {
        final String filePath = files.get(position).getPath();
        File newFile = new File(filePath);
        final String fileName = newFile.getName();

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

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                File file = new File(files.get(position).getPath());
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

        holder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                File file = new File(filePath);
                Uri fileUri = FileProvider.getUriForFile(v.getContext(),
                        "com.blacksun.quicknote.fileprovider",
                        file);
                String filename = file.getName();

                File storageLoc = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                File savedFile = new File(storageLoc, filename);

                UtilHelper.copy(fileUri, savedFile, context);

                Log.d("saveFile", ""+savedFile);

                Toast.makeText(context, "File saved", Toast.LENGTH_SHORT).show();
                return true;
            }
        });

        holder.deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Attachment curAttach = files.get(position);
                String curPath = curAttach.getPath();
                File curFile = new File(curPath);
                curFile.delete();

                long curId = curAttach.getId();

                if (curId != 0){
                    AttachManager.newInstance(context).delete(curAttach);
                }

                files.remove(position);
                notifyItemRemoved(position);
                notifyItemRangeChanged(position, files.size());


                //check changes in Notes
                Intent intent = new Intent(context, DetailActivity.class);
                intent.setAction(REQUEST_CHANGE);
                context.startActivity(intent);
            }
        });
    }

    @Override
    public int getItemCount() {
        return files.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView file;
        ImageButton deleteButton;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            file = itemView.findViewById(R.id.attach_file);
            deleteButton = itemView.findViewById(R.id.attach_file_close);
        }
    }
}
