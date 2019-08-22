package com.blacksun.quicknote.adapters;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
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

public class ImageRecyclerAdapter extends RecyclerView.Adapter<ImageRecyclerAdapter.ViewHolder> {
    ArrayList<Attachment> images;
    ArrayList<Attachment> newImages;
    Context context;

    public ImageRecyclerAdapter(ArrayList<Attachment> images, ArrayList<Attachment> newImages, Context context) {
        this.images = images;
        this.newImages = newImages;
        this.context = context;
    }

    @NonNull
    @Override
    public ImageRecyclerAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View listView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_image, parent, false);
        ViewHolder viewHolder = new ViewHolder(listView);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull ImageRecyclerAdapter.ViewHolder holder, final int position) {
        final Attachment attach = images.get(position);

//        Bitmap imageFile = UtilHelper.decodeSampledBitmapFromFile(attach.getPath(), 300, 300);

        Bitmap thumbImage = UtilHelper.createThumbnail(attach.getPath(), 300, 300);
        holder.image.setImageBitmap(thumbImage);

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openAttach(v, position);
            }
        });

        holder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {

                //creating a popup menu
                PopupMenu popup = new PopupMenu(context, holder.itemView);
                //inflating menu from xml resource
                popup.inflate(R.menu.menu_attach);
                //adding click listener
                popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        switch (item.getItemId()) {
                            case R.id.action_open_attach:
                                openAttach(v, position);
                                return true;
                            case R.id.action_save_attach:
                                saveAttach(v, attach);
                                return true;
                            case R.id.action_delete_attach:
                                deleteAttach(position);
                                return true;
                            default:
                                return false;
                        }
                    }
                });
                //displaying the popup
                popup.show();


//                saveAttach(v, attach);
                return true;
            }
        });

        holder.deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                deleteAttach(position);
            }
        });
    }

    private void deleteAttach(int position) {
        Attachment curAttach = images.get(position);
        String curPath = curAttach.getPath();
        File curFile = new File(curPath);
        curFile.delete();

        long curId = curAttach.getId();

        if (curId != 0) {
            AttachManager.newInstance(context).delete(curAttach);
        }

        images.remove(position);
        notifyItemRemoved(position);
        notifyItemRangeChanged(position, images.size());

        for (int i = newImages.size() - 1; i >= 0; i--) {
            if (newImages.get(i).getPath().equals(curPath)) {
                newImages.remove(i);
                break;
            }
        }


        //check changes in Notes
        Intent intent = new Intent(context, DetailActivity.class);
        intent.setAction(REQUEST_CHANGE);
        intent.putExtra(NoteContract.AttachEntry.COLUMN_ATTACH_PATH, curPath);
        intent.putExtra(NoteContract.AttachEntry.COLUMN_ATTACH_TYPE, NoteContract.AttachEntry.IMAGE_TYPE);
        context.startActivity(intent);
    }

    private void saveAttach(View v, Attachment attach) {
        File file = new File(attach.getPath());
        Uri fileUri = FileProvider.getUriForFile(v.getContext(),
                "com.blacksun.quicknote.fileprovider",
                file);
        String filename = file.getName();

        File storageLoc = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        File savedFile = new File(storageLoc, filename);

        UtilHelper.copy(fileUri, savedFile, context);

        Log.d("saveFile", "" + savedFile);

        Toast.makeText(context, "Image saved " + storageLoc , Toast.LENGTH_SHORT).show();
    }

    private void openAttach(View v, int position) {
        File file = new File(images.get(position).getPath());
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
        return images.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        ImageView image;
        ImageButton deleteButton;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.attach_image);
            deleteButton = itemView.findViewById(R.id.attach_close);


        }
    }
}
