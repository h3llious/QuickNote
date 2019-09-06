package com.blacksun.quicknote.adapters;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
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
import static com.blacksun.quicknote.activities.DetailActivity.REQUEST_INSERT;

public class ImageRecyclerAdapter extends RecyclerView.Adapter<ImageRecyclerAdapter.ViewHolder> {
    private ArrayList<Attachment> images;
    private ArrayList<Attachment> newImages;
    private Context context;
    public MyAdapterListener onClickListener;

    public ImageRecyclerAdapter(ArrayList<Attachment> images, ArrayList<Attachment> newImages, Context context, MyAdapterListener listener) {
        this.images = images;
        this.newImages = newImages;
        this.context = context;
        this.onClickListener = listener;
    }

    public interface MyAdapterListener {
        //        void iconTextViewOnClick(View v, int position);
        void deleteButtonOnClick(View v, int position);
    }


    @NonNull
    @Override
    public ImageRecyclerAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View listView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_image, parent, false);
        return new ViewHolder(listView);
    }

    @Override
    public void onBindViewHolder(@NonNull ImageRecyclerAdapter.ViewHolder holder, final int position) {
        final Attachment attach = images.get(position);

        File attachFile = new File(attach.getPath());
        if (!attachFile.exists()){
            return;
        }

        holder.image.setImageDrawable(context.getResources().getDrawable(android.R.drawable.ic_menu_gallery));

        final Handler handler = new Handler();
        new Thread() {
            public void run() {
                // Do time-consuming initialization.
                Bitmap thumbImage = UtilHelper.getRoundedCornerBitmap(UtilHelper.createThumbnail(attach.getPath(), 300, 300), 20);
                // When done:
                handler.post(new Runnable() {
                    public void run() {
                        // set up the real UI
                        holder.image.setImageBitmap(thumbImage);
                    }
                });
            }
        }.start();
    }

    private void deleteAttach(int position) {
        Attachment curAttach = images.get(position);
        String curPath = curAttach.getPath();
        File curFile = new File(curPath);
        boolean isDel = curFile.delete();
        Log.d("imageAttach", "is Deleted Image: "+isDel);

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

    private void saveAttach(View v, int position) {
        File file = new File(images.get(position).getPath());
        Uri fileUri = FileProvider.getUriForFile(v.getContext(),
                "com.blacksun.quicknote.fileprovider",
                file);
        String filename = file.getName();

        File storageLoc = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        File savedFile = new File(storageLoc, filename);

        new Thread(new Runnable() {
            @Override
            public void run() {
                UtilHelper.copy(fileUri, savedFile, context);
            }
        }).start();

        Log.d("saveFile", "" + savedFile);

        Toast.makeText(context, context.getResources().getString(R.string.adapter_saved_path)+ storageLoc , Toast.LENGTH_SHORT).show();
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
                    popup.getMenu().findItem(R.id.action_insert_img).setVisible(true);
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
//                                    deleteAttach(getAdapterPosition());
                                    onClickListener.deleteButtonOnClick(v, getAdapterPosition());
                                    return true;
                                case R.id.action_insert_img:
                                    Intent intent = new Intent(context, DetailActivity.class);
                                    intent.setAction(REQUEST_INSERT);
                                    intent.putExtra("imgPos", getAdapterPosition());
                                    context.startActivity(intent);
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

            deleteButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
//                    deleteAttach(getAdapterPosition());
                    onClickListener.deleteButtonOnClick(v, getAdapterPosition());
                }
            });
        }
    }
}
