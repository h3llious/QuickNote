package com.blacksun.quicknote.adapters;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.RecyclerView;

import com.blacksun.quicknote.R;
import com.blacksun.quicknote.activities.DetailActivity;
import com.blacksun.quicknote.controllers.AttachManager;
import com.blacksun.quicknote.data.NoteContract;
import com.blacksun.quicknote.models.Attachment;
import com.blacksun.quicknote.utils.UtilHelper;

import java.io.File;
import java.util.ArrayList;

import static com.blacksun.quicknote.activities.DetailActivity.REQUEST_CHANGE;

public class FileRecyclerAdapter extends RecyclerView.Adapter<FileRecyclerAdapter.ViewHolder> {
    ArrayList<Attachment> files;
    ArrayList<Attachment> newFiles;
    Context context;

    public FileRecyclerAdapter(ArrayList<Attachment> files, ArrayList<Attachment> newFiles, Context context) {
        this.files = files;
        this.newFiles = newFiles;
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


    }

    private void deleteAttach(int position) {
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

        for (int i = newFiles.size() - 1; i >= 0; i--) {
            if (newFiles.get(i).getPath().equals(curPath)) {
                newFiles.remove(i);
                break;
            }
        }


        //check changes in Notes
        Intent intent = new Intent(context, DetailActivity.class);
        intent.setAction(REQUEST_CHANGE);
        intent.putExtra(NoteContract.AttachEntry.COLUMN_ATTACH_TYPE, NoteContract.AttachEntry.IMAGE_TYPE);
        intent.putExtra(NoteContract.AttachEntry.COLUMN_ATTACH_PATH, curPath);
        context.startActivity(intent);
    }

    private void saveAttach(View v, int position) {
        File file = new File(files.get(position).getPath());
        Uri fileUri = FileProvider.getUriForFile(v.getContext(),
                "com.blacksun.quicknote.fileprovider",
                file);
        String filename = file.getName();

        File storageLoc = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        File savedFile = new File(storageLoc, filename);

        new Thread(new Runnable() {
            @Override
            public void run() {
                UtilHelper.copy(fileUri, savedFile, context);
            }
        }).start();

        Log.d("saveFile", ""+savedFile);

        Toast.makeText(context, "File saved into "+savedFile.getAbsolutePath(), Toast.LENGTH_LONG).show();
    }

    private void openAttach(View v, int position) {
        File file = new File(files.get(position).getPath());
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_VIEW);
        Uri fileUri = FileProvider.getUriForFile(v.getContext(),
                "com.blacksun.quicknote.fileprovider",
                file);
        intent.setData(fileUri);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        v.getContext().startActivity(intent);
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

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    openAttach(v, getAdapterPosition());
                }
            });

            itemView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    //creating a popup menu
                    PopupMenu popup = new PopupMenu(context, itemView);
                    //inflating menu from xml resource
                    popup.inflate(R.menu.menu_attach);
                    //adding click listener
                    popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                        @Override
                        public boolean onMenuItemClick(MenuItem item) {
                            switch (item.getItemId()) {
                                case R.id.action_open_attach:
                                    openAttach(v, getAdapterPosition());
                                    return true;
                                case R.id.action_save_attach:
                                    saveAttach(v, getAdapterPosition());
                                    return true;
                                case R.id.action_delete_attach:
                                    deleteAttach(getAdapterPosition());
                                    return true;
                                default:
                                    return false;
                            }
                        }
                    });
                    //displaying the popup
                    popup.show();


//                saveAttach(v, filePath);
                    return true;
                }
            });

            deleteButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    deleteAttach(getAdapterPosition());
                }
            });
        }
    }
}
