package com.blacksun.quicknote.activities;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.text.Editable;
import android.text.Layout;
import android.text.Spannable;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.method.LinkMovementMethod;
import android.text.style.AlignmentSpan;
import android.text.style.ClickableSpan;
import android.text.style.ImageSpan;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.blacksun.quicknote.R;
import com.blacksun.quicknote.adapters.FileRecyclerAdapter;
import com.blacksun.quicknote.adapters.ImageRecyclerAdapter;
import com.blacksun.quicknote.controllers.AttachManager;
import com.blacksun.quicknote.data.NoteContract;
import com.blacksun.quicknote.controllers.NoteManager;
import com.blacksun.quicknote.models.Attachment;
import com.blacksun.quicknote.models.Note;
import com.blacksun.quicknote.utils.UtilHelper;
import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.android.material.snackbar.Snackbar;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class DetailActivity extends AppCompatActivity {


    EditText detailTitle, detailContent;
    CollapsingToolbarLayout collapsingToolbar;
    Note currentNote = null;
    boolean isSaved = false;

    ImageButton detailCamera, detailImage, detailFile, detailCheckbox;

    PackageManager pm;

    static final int REQUEST_IMAGE_CAPTURE = 1;
    static final int REQUEST_FILE_CHOOSER = 2;
    static final int REQUEST_IMAGE_CHOOSER = 3;

    String currentPhotoPath;
    String currentPhotoName;
    String oldPhotoPath;

    RecyclerView imageList, fileList;

    ArrayList<Attachment> images, files, newImages, newFiles;
    ImageRecyclerAdapter imageRecyclerAdapter;
    FileRecyclerAdapter fileRecyclerAdapter;

    boolean isAttaching = false;

    public final static String REQUEST_CHANGE = "changed";
    public final static String REQUEST_INSERT = "insert";

    boolean isChanged = false;

    public static final String SPANNABLE_TAG = "spannable";
    public static final String ATTACH_TAG = "attach";

    DisplayMetrics displayMetrics = new DisplayMetrics();

    ImageHandler mEmoticonHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        //findViewById
        initialize();

        //toolbar for insert media
        setOnClickButton();

        //list of images and files
        setUpRecyclerView();

        //existed note content
        setUpCurrentNote();

        Log.d(DetailActivity.class.getName(), "onCreate Activity");
    }

    private void setUpRecyclerView() {
        newImages = new ArrayList<>();
        newFiles = new ArrayList<>();

        images = new ArrayList<>();
        imageRecyclerAdapter = new ImageRecyclerAdapter(images, newImages, this, new ImageRecyclerAdapter.MyAdapterListener() {
            @Override
            public void deleteButtonOnClick(View v, int position) {
                Attachment curAttach = images.get(position);
                String curPath = curAttach.getPath();
                File curFile = new File(curPath);
                boolean isDel = curFile.delete();
                Log.d("imageAttach", "is Deleted Image: " + isDel);

                long curId = curAttach.getId();

                if (curId != 0) {
                    AttachManager.newInstance(getApplicationContext()).delete(curAttach);
                }

                images.remove(position);
                imageRecyclerAdapter.notifyItemRemoved(position);
                imageRecyclerAdapter.notifyItemRangeChanged(position, images.size());

                for (int i = newImages.size() - 1; i >= 0; i--) {
                    if (newImages.get(i).getPath().equals(curPath)) {
                        newImages.remove(i);
                        break;
                    }
                }

                isChanged = true;
//                String attachType = intent.getStringExtra(NoteContract.AttachEntry.COLUMN_ATTACH_TYPE);

                //delete attaches in content
//                String attachPath = intent.getStringExtra(NoteContract.AttachEntry.COLUMN_ATTACH_PATH);

                //when attach type is image, delete corresponding image in EditText if existed
//                if (attachType.equals(NoteContract.AttachEntry.IMAGE_TYPE)) {
//                    File curFile = new File(attachPath);
                String attachName = curFile.getName();
                String contentString = detailContent.getText().toString();
                String attachFormat = "$" + attachName + "$";
                if (contentString.contains(attachFormat)) {
                    int startIdx = contentString.indexOf(attachFormat);
                    Editable changedContent = detailContent.getText().delete(startIdx, startIdx + 1 + attachFormat.length());
                    detailContent.setText(changedContent);
                }
//                }
            }
        });

        imageList.setHasFixedSize(false);
        imageList.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        imageList.setAdapter(imageRecyclerAdapter);

        files = new ArrayList<>();
        fileRecyclerAdapter = new FileRecyclerAdapter(files, newFiles, this, new FileRecyclerAdapter.MyAdapterListener() {
            @Override
            public void deleteButtonOnClick(View v, int position) {
                Attachment curAttach = files.get(position);
                String curPath = curAttach.getPath();
                File curFile = new File(curPath);
                curFile.delete();

                long curId = curAttach.getId();

                if (curId != 0) {
                    AttachManager.newInstance(getApplicationContext()).delete(curAttach);
                }

                files.remove(position);
                fileRecyclerAdapter.notifyItemRemoved(position);
                fileRecyclerAdapter.notifyItemRangeChanged(position, files.size());

                for (int i = newFiles.size() - 1; i >= 0; i--) {
                    if (newFiles.get(i).getPath().equals(curPath)) {
                        newFiles.remove(i);
                        break;
                    }
                }

                isChanged = true;
            }
        });

        fileList.setHasFixedSize(false); //size change with content
        fileList.setLayoutManager(new LinearLayoutManager(this));
        fileList.setAdapter(fileRecyclerAdapter);
    }

    private void setOnClickButton() {
        pm = getPackageManager();

        detailCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!pm.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)) {
                    Toast.makeText(v.getContext(), "Device does not have camera", Toast.LENGTH_SHORT).show();
                } else {
                    isAttaching = true;
                    dispatchTakePictureIntent();
                }
            }
        });

        detailFile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isAttaching = true;
                dispatchChooseFileIntent();
            }
        });

        detailImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isAttaching = true;
                dispatchChooseGalleryIntent();
            }
        });

        detailCheckbox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

//                int cursorLoc = detailContent.getSelectionStart();
//
//                if (cursorLoc != -1) {
//
//
//                    Editable content = detailContent.getText();
//
//
//                    //cursor is the position of imageSpan
//                    String textCheckbox = "$checked$";
//                    String textNoCheck = "$notChecked$";
//                    content.insert(cursorLoc, textCheckbox);
//
//
//                    //+2 for cursor+1 and length+1
//                    content.setSpan(new ImageSpan(getApplicationContext(), android.R.drawable.checkbox_off_background),
//                            cursorLoc, cursorLoc + textCheckbox.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
//                    content.setSpan(new ClickableSpan() {
//                        @Override
//                        public void onClick(@NonNull View widget) {
//                            ImageSpan[] toBeDeleted = content.getSpans(cursorLoc, cursorLoc + textCheckbox.length(), ImageSpan.class);
//                            for (ImageSpan delete: toBeDeleted) {
//                                content.removeSpan(delete);
//                            }
//                            ClickableSpan[] toBeDeletedClick = content.getSpans(cursorLoc, cursorLoc + textCheckbox.length(), ClickableSpan.class);
//                            for (ClickableSpan delete: toBeDeletedClick) {
//                                content.removeSpan(delete);
//                            }
//                            content.replace(cursorLoc, cursorLoc + textCheckbox.length(), textNoCheck);
//                            content.setSpan(new ImageSpan(getApplicationContext(), android.R.drawable.checkbox_on_background),
//                                    cursorLoc, cursorLoc + textNoCheck.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
//                            content.setSpan(new ClickableSpan() {
//                                @Override
//                                public void onClick(@NonNull View widget) {
//                                    ImageSpan[] toBeDelted = content.getSpans(cursorLoc, cursorLoc + textNoCheck.length(), ImageSpan.class);
//                                    for (ImageSpan delete: toBeDelted) {
//                                        content.removeSpan(delete);
//                                    }
//                                    ClickableSpan[] toBeDeletedClick = content.getSpans(cursorLoc, cursorLoc + textNoCheck.length(), ClickableSpan.class);
//                                    for (ClickableSpan delete: toBeDeletedClick) {
//                                        content.removeSpan(delete);
//                                    }
//                                    content.replace(cursorLoc, cursorLoc + textNoCheck.length(), textCheckbox);
//                                    content.setSpan(new ImageSpan(getApplicationContext(), android.R.drawable.checkbox_off_background),
//                                            cursorLoc, cursorLoc + textCheckbox.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
//                                    detailContent.setText(content);
//                                }
//                            }, cursorLoc, cursorLoc + textCheckbox.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
//                            detailContent.setText(content);
//
//                        }
//                    }, cursorLoc, cursorLoc + textCheckbox.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
//
//                    detailContent.setText(content);
//                } else {
////                    Toast.makeText(getBaseContext(), "Please choose a position in content box", Toast.LENGTH_SHORT).show();
//                }

                Snackbar.make(v, "Not implemented yet", Snackbar.LENGTH_SHORT)
                        .setAction("Dismiss", new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                            }
                        })
                        .show();
            }
        });
    }

    private void dispatchChooseGalleryIntent() {
        Intent chooseGallery;
        Intent intent;
        chooseGallery = new Intent(Intent.ACTION_GET_CONTENT);
        chooseGallery.addCategory(Intent.CATEGORY_OPENABLE);
        chooseGallery.setType("image/*");
        chooseGallery.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        intent = Intent.createChooser(chooseGallery, "Choose an image");
        startActivityForResult(intent, REQUEST_IMAGE_CHOOSER);
    }

    private void dispatchChooseFileIntent() {
        Intent chooseFile;
        Intent intent;
        chooseFile = new Intent(Intent.ACTION_GET_CONTENT);
        chooseFile.addCategory(Intent.CATEGORY_OPENABLE);
        chooseFile.setType("*/*");
        chooseFile.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        intent = Intent.createChooser(chooseFile, "Choose a file");
        startActivityForResult(intent, REQUEST_FILE_CHOOSER);
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getFilesDir();

        //if saved in external storage
//        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);

        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        oldPhotoPath = currentPhotoPath;
        currentPhotoPath = image.getAbsolutePath();
        currentPhotoName = image.getName();
        Log.d("filePath", "" + currentPhotoPath);
        Log.d("filePath", "" + currentPhotoName);
        return image;
    }

    public String getFileName(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            Log.d("filepath", "getFileNameContent");
            Cursor cursor = getContentResolver().query(uri, null, null, null, null);
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                }
            } finally {
                cursor.close();
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        return result;
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_IMAGE_CAPTURE) {
            if (resultCode == RESULT_OK) {

                compressImage();

                //new RecyclerView Item
                Attachment newAttach = new Attachment();
                if (currentNote == null) {
                    newAttach.setType(NoteContract.AttachEntry.IMAGE_TYPE);
                    newAttach.setPath(currentPhotoPath);
                    images.add(newAttach);
                } else {
                    newAttach.setNote_id(currentNote.getId());
                    newAttach.setType(NoteContract.AttachEntry.IMAGE_TYPE);
                    newAttach.setPath(currentPhotoPath);
                    newImages.add(newAttach);
                    images.add(newAttach);
                }
                imageRecyclerAdapter.notifyItemInserted(images.size() - 1);

                //spannable test
                spannableImage(newAttach);

            } else if (resultCode == RESULT_CANCELED) {
                if (!TextUtils.isEmpty(currentPhotoPath)) {
                    File tobedeleted = new File(currentPhotoPath);
                    tobedeleted.delete();
                    Log.d("filePath", "cancel ok");
                }
            }
        } else if (requestCode == REQUEST_FILE_CHOOSER) {
            if (resultCode == RESULT_OK) {
                if (data.getClipData() != null) {

                    int count = data.getClipData().getItemCount();
                    int currentItem = 0;
                    while (currentItem < count) {

                        Uri uri = data.getClipData().getItemAt(currentItem).getUri();

//                        String id = DocumentsContract.getDocumentId(uri);

                        saveFileAttach(uri);

                        currentItem++;
                    }
                } else {
                    Uri uri = data.getData();

//                    String id = DocumentsContract.getDocumentId(uri);

                    saveFileAttach(uri);
                }

            }
        } else if (requestCode == REQUEST_IMAGE_CHOOSER) {
            if (resultCode == RESULT_OK) {

                if (data.getClipData() != null) {

                    int count = data.getClipData().getItemCount();
                    int currentItem = 0;
                    while (currentItem < count) {

                        Uri uri = data.getClipData().getItemAt(currentItem).getUri();
                        //String path = getPath(uri);

//                        String id = DocumentsContract.getDocumentId(uri);

                        saveImageAttach(uri);
                        currentItem++;
                    }

                } else {
                    Uri uri = data.getData();

//                    String id = DocumentsContract.getDocumentId(uri);

                    saveImageAttach(uri);
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void saveImageAttach(Uri uri) {
        String fileName = getFileName(uri);
        try {
            File savedFile = createImageFile();
//                            UtilHelper.copy(uri, savedFile, this);
            String filePath = savedFile.getAbsolutePath();

            //smaller image size
            minimizeImageSize(uri, savedFile, filePath);

            Attachment newAttach = new Attachment();
            if (currentNote == null) {
                newAttach.setType(NoteContract.AttachEntry.IMAGE_TYPE);
                newAttach.setPath(currentPhotoPath);
                images.add(newAttach);
            } else {
                newAttach.setNote_id(currentNote.getId());
                newAttach.setType(NoteContract.AttachEntry.IMAGE_TYPE);
                newAttach.setPath(currentPhotoPath);
                newImages.add(newAttach);
                images.add(newAttach);
            }
            imageRecyclerAdapter.notifyItemInserted(images.size() - 1);
//                    imageRecyclerAdapter.notifyDataSetChanged(); //BUG can't solve, change to this

            Log.d("filepath", filePath + ": " + fileName);
            //spannable test
            spannableImage(newAttach);

        } catch (IOException e) {
            Log.e("saveFile", "error saving file");
        }
    }

    private void minimizeImageSize(Uri uri, File savedFile, String filePath) throws IOException {
        InputStream in = this.getContentResolver().openInputStream(uri);
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeStream(in, null, options);
        in.close();
        // Calculate inSampleSize
        options.inSampleSize = UtilHelper.calculateInSampleSize(options, 960, 960);
        int quality = 100 / options.inSampleSize;

        if (quality < 100) {
            // Decode bitmap with inSampleSize set
            options.inJustDecodeBounds = false;
            in = this.getContentResolver().openInputStream(uri);
            Bitmap capturedImg = BitmapFactory.decodeStream(in, null, options);

            try (FileOutputStream out = new FileOutputStream(filePath)) {
                capturedImg.compress(Bitmap.CompressFormat.JPEG, quality, out); // bmp is your Bitmap instance
                // PNG is a lossless format, the compression factor (100) is ignored
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            UtilHelper.copy(uri, savedFile, this);
        }
        in.close();
    }

    private void saveFileAttach(Uri uri) {
        String fileName = getFileName(uri);

        long timeStamp = (new Date()).getTime();

        File savedFile = new File(getFilesDir().getAbsolutePath() + "/" + timeStamp + "_" + fileName);

        long timeStart = System.currentTimeMillis();
        //thread implementation
//                    new Thread(new Runnable() {
//                        @Override
//                        public void run() {
//                            UtilHelper.copy(uri, savedFile, getApplicationContext());
//                        }
//                    }).start();

        UtilHelper.copy(uri, savedFile, this);
        Log.d("file", "execution time: " + (System.currentTimeMillis() - timeStart));

        final String filePath = savedFile.getAbsolutePath();

        Log.d("filepath", filePath + ": " + fileName);

        //files.add(new Attachment(1, 1, "FILE", filePath));
        Attachment newAttach = new Attachment();
        if (currentNote == null) {
            newAttach.setType(NoteContract.AttachEntry.FILE_TYPE);
            newAttach.setPath(filePath);
            files.add(newAttach);
        } else {
            newAttach.setNote_id(currentNote.getId());
            newAttach.setType(NoteContract.AttachEntry.FILE_TYPE);
            newAttach.setPath(filePath);
            newFiles.add(newAttach);
            files.add(newAttach);
        }

//                fileRecyclerAdapter.notifyDataSetChanged();
        fileRecyclerAdapter.notifyItemInserted(files.size() - 1);
    }

    private void compressImage() {
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(currentPhotoPath, options);

        // Calculate inSampleSize
        options.inSampleSize = UtilHelper.calculateInSampleSize(options, 960, 960);
        int quality = 100 / options.inSampleSize;

//                Bitmap capturedImg = UtilHelper.decodeSampledBitmapFromFile(currentPhotoPath, 720, 720);

        if (quality < 100) {
            // Decode bitmap with inSampleSize set
            options.inJustDecodeBounds = false;
            Bitmap capturedImg = BitmapFactory.decodeFile(currentPhotoPath, options);

            try (FileOutputStream out = new FileOutputStream(currentPhotoPath)) {
                capturedImg.compress(Bitmap.CompressFormat.JPEG, quality, out); // bmp is your Bitmap instance
                // PNG is a lossless format, the compression factor (100) is ignored
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void spannableImage(Attachment newAttach) {
        final int cursorLoc = detailContent.getSelectionStart();

        if (cursorLoc != -1) {
//            Bitmap thumb = UtilHelper.createThumbnail(currentPhotoPath, 500, 500);

            new Thread(new Runnable() {
                @Override
                public void run() {
                    Bitmap thumb = UtilHelper.getRoundedCornerBitmap(
                            UtilHelper.createThumbnail(currentPhotoPath, displayMetrics.widthPixels / 2, displayMetrics.widthPixels / 2), 20);

                    Log.d(SPANNABLE_TAG, "cursor location: " + cursorLoc);

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {

                            Editable content = detailContent.getText();

                            String attachName = newAttach.getPath().substring(newAttach.getPath().lastIndexOf('/') + 1);
                            //cursor onPause+1 is the position of imageSpan
                            content.insert(cursorLoc, "\n$" + attachName + "$ \n");

                            //update position to change into image
                            int newCursorLoc = cursorLoc + 1;

                            Log.d(SPANNABLE_TAG, "cursor name: " + content.subSequence(newCursorLoc, newCursorLoc + attachName.length() + 1));

                            //+2 for cursor+1 and length+1
                            setSpans(thumb, content, attachName, newCursorLoc, newAttach);

                            detailContent.setText(content);
                        }
                    });
                }
            }).start();

        }
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            File photoFile = null;

            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                Log.e("saveFile", "error saving file");
            }

            if (photoFile != null) {
                Uri photoUri = FileProvider.getUriForFile(this,
                        "com.blacksun.quicknote.fileprovider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            }

        } else {
            Toast.makeText(getBaseContext(), "No camera app installed", Toast.LENGTH_SHORT).show();
        }
    }

    //Set title of the note and attributes of collapsing toolbar with corresponding attachments
    private void setUpCurrentNote() {
        if (getIntent() != null && getIntent().getExtras() != null) {
            Bundle bundle = getIntent().getExtras();
            if (bundle.getString("title") != null) {

                String title = bundle.getString("title");
                String content = bundle.getString("content");
                long dateCreated = bundle.getLong("dateCreated");
                long dateModified = bundle.getLong("dateModified");
                long id = bundle.getLong("noteID");

                detailTitle.setText(title);
                detailContent.setText(content);

                //test collapsing toolbar
                collapsingToolbar.setTitle(title);

//                String img = bundle.getString("imagePath");
//                currentPhotoPath = img;

                ArrayList<Attachment> currentImages = AttachManager.newInstance(this).getAttach(id, NoteContract.AttachEntry.IMAGE_TYPE);
                images.clear();
                images.addAll(currentImages);
                Log.d("attach", "id " + id + ", number " + images.size());
                imageRecyclerAdapter.notifyDataSetChanged();

                //change header image
                changeHeaderImage();

                ArrayList<Attachment> currentFiles = AttachManager.newInstance(this).getAttach(id, NoteContract.AttachEntry.FILE_TYPE);
                files.clear();
                files.addAll(currentFiles);
                Log.d("attach", "id " + id + ", number of files " + files.size());
                fileRecyclerAdapter.notifyDataSetChanged();

//                currentNote = new Note(title, content, id, dateCreated, dateModified, img);
                currentNote = new Note(title, content, id, dateCreated, dateModified);
//                isSaved = false;

                setUpImageSpan();

            }
        } else {
            collapsingToolbar.setTitle("New note");
            changeHeaderImageDefault();
        }

        //handle remove image when backspacing
        mEmoticonHandler = new ImageHandler(detailContent);

        collapsingToolbar.setCollapsedTitleTextColor(ContextCompat.getColor(this, R.color.white));
        collapsingToolbar.setExpandedTitleColor(ContextCompat.getColor(this, R.color.transparent));
    }

    private void setUpImageSpan() {
        if (images.size() > 0) {
            //spannable test
            //Image holder
            Editable contentSpan = detailContent.getText();
            String contentString = contentSpan.toString();
            for (Attachment image : images) {
                String attachName = image.getPath().substring(image.getPath().lastIndexOf('/') + 1);

                if (contentString.contains("$" + attachName + "$")) {
                    int idxStart = contentString.indexOf("$" + attachName + "$");

                    contentSpan.setSpan(new ImageSpan(getResources().getDrawable(android.R.drawable.ic_menu_gallery)), idxStart, idxStart + attachName.length() + 2, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    contentSpan.setSpan(new AlignmentSpan.Standard(Layout.Alignment.ALIGN_CENTER), idxStart, idxStart + attachName.length() + 2, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
            }
            detailContent.setText(contentSpan);
            //thread implementation
            setSpanEditText.start();
        }
    }


    Thread setSpanEditText = new Thread(new Runnable() {
        @Override
        public void run() {
            Editable contentSpan = detailContent.getText();
            contentSpan.clearSpans();
            String contentString = contentSpan.toString();
            for (Attachment image : images) {
                String attachName = image.getPath().substring(image.getPath().lastIndexOf('/') + 1);

                if (contentString.contains("$" + attachName + "$")) {
                    int idxStart = contentString.indexOf("$" + attachName + "$");

                    Bitmap thumb = UtilHelper.getRoundedCornerBitmap(
                            UtilHelper.createThumbnail(image.getPath(), displayMetrics.widthPixels / 2, displayMetrics.widthPixels / 2), 20);

                    setSpans(thumb, contentSpan, attachName, idxStart, image);
                }
            }

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    detailContent.setText(contentSpan);
                }
            });
        }
    });

    private void changeHeaderImage() {
        if (images.size() == 0) {
            changeHeaderImageDefault();
        } else {
            float scale = getResources().getDisplayMetrics().density;
            int dpAsPixels = (int) (180 * scale + 0.5f);

            ImageView toolbarImage = findViewById(R.id.toolbar_image);

            //handle loading bitmap
            new Thread(new Runnable() {
                @Override
                public void run() {
                    Bitmap imgHeader = UtilHelper.createThumbnail(images.get(0).getPath(), displayMetrics.widthPixels, dpAsPixels);
                    Log.d("bitmap", "display width " + displayMetrics.widthPixels);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            toolbarImage.setImageBitmap(imgHeader);
                        }
                    });
                }
            }).start();
        }
    }

    private void initialize() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null)
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        collapsingToolbar = findViewById(R.id.toolbar_layout);

        detailContent = findViewById(R.id.detail_content);
        detailTitle = findViewById(R.id.detail_title);

        detailCamera = findViewById(R.id.detail_camera);
        detailImage = findViewById(R.id.detail_image);
        detailFile = findViewById(R.id.detail_file);
        detailCheckbox = findViewById(R.id.detail_checkbox);

        imageList = findViewById(R.id.detail_images);
        fileList = findViewById(R.id.detail_files);

        //spannable test
        detailContent.setMovementMethod(new LinkMovementMethod() {
            @Override
            public boolean onTouchEvent(TextView widget, Spannable buffer, MotionEvent event) {
                detailContent.setSelection(0);
//                Selection.removeSelection(buffer);
//                widget.setHighlightColor(Color.argb(50,100,0,0));
                return super.onTouchEvent(widget, buffer, event);
            }
        });

        //size pixel
        WindowManager windowmanager = (WindowManager) getApplicationContext().getSystemService(Context.WINDOW_SERVICE);
        windowmanager.getDefaultDisplay().getMetrics(displayMetrics);
    }


    private boolean saveNote() {
        String title = detailTitle.getText().toString();
        String content = detailContent.getText().toString();

        if (TextUtils.isEmpty(title) && TextUtils.isEmpty(content) &&
                currentNote == null && images.size() == 0 && files.size() == 0) {
            Toast.makeText(this, "Note cannot be empty!", Toast.LENGTH_LONG).show();
            Log.d("saveState", "nothing");
            return false;
        }

        if (TextUtils.isEmpty(title)) {
            title = "No title";
            //return false;
        }

        if (currentNote == null) {
            saveNewNote(title, content);

        } else {
            //check if note is changed
            if (currentNote.getTitle().equals(title) && currentNote.getContent().equals(content)
                    && newImages.size() == 0 && newFiles.size() == 0 && !isChanged) {
                return false;
            }

            updateNote(title, content);
        }
        collapsingToolbar.setTitle(detailTitle.getText());
        return true;
    }

    private void updateNote(String title, String content) {
        currentNote.setTitle(title);
        if (!TextUtils.isEmpty(content))
            currentNote.setContent(content);
        else
            currentNote.setContent("");

        //update record in db
        NoteManager.newInstance(this).update(currentNote);

        if (newImages != null) {
            for (int imagePos = 0; imagePos < newImages.size(); imagePos++) {
                Attachment currentAttach = newImages.get(imagePos);
                AttachManager.newInstance(this).create(currentAttach);
            }
            newImages.clear();
        }

        if (newFiles != null) {
            for (int filePos = 0; filePos < newFiles.size(); filePos++) {
                Attachment currentAttach = newFiles.get(filePos);
                AttachManager.newInstance(this).create(currentAttach);
            }
            newFiles.clear();
        }
    }

    private void saveNewNote(String title, String content) {
        Note note = new Note();
        note.setTitle(title);

        if (!TextUtils.isEmpty(content))
            note.setContent(content);
        else
            note.setContent("");

        //create new record in db
        long newId = NoteManager.newInstance(this).create(note);

        if (newId == -1)
            Log.e("saveState", "error creating new note.");

        //save images
        if (images != null) {
            for (int imagePos = 0; imagePos < images.size(); imagePos++) {
                Attachment currentAttach = images.get(imagePos);
                currentAttach.setNote_id(newId);
                AttachManager.newInstance(this).create(currentAttach);
            }
            Log.d("saveState", "is adding images with size " + images.size());
        }

        //save files
        if (files != null) {
            for (int filePos = 0; filePos < files.size(); filePos++) {
                Attachment currentAttach = files.get(filePos);
                currentAttach.setNote_id(newId);
                AttachManager.newInstance(this).create(currentAttach);
            }
        }
    }

    private boolean deleteNote() {
        if (currentNote == null)
            return false;
        else {
            long noteId = currentNote.getId();
            ArrayList<Attachment> currentAttachments = AttachManager.newInstance(this).getAttach(noteId, NoteContract.AttachEntry.ANY_TYPE);
            Log.d("attach", "sizeDel " + currentAttachments.size());

            for (int attachPos = 0; attachPos < currentAttachments.size(); attachPos++) {
                Attachment curAttach = currentAttachments.get(attachPos);
                String curPath = curAttach.getPath();

                File delFile = new File(curPath);
                boolean res = delFile.delete();
                if (res)
                    Log.d("attach", "file deleted");
                else
                    Log.d("attach", "file not deleted");

                AttachManager.newInstance(this).delete(curAttach);
            }
            //keeping for deleting later in Drive
            //NoteManager.newInstance(this).delete(currentNote);
            NoteManager.newInstance(this).disable(currentNote);
        }
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_detail, menu);
        if (currentNote == null)
            menu.findItem(R.id.action_delete).setVisible(false);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        switch (id) {
            case android.R.id.home:
                finish();
                break;
            case R.id.action_save:
                boolean saveCheck = saveNote();
                if (saveCheck)
                    isSaved = true;

                if (currentNote == null) {
                    if (saveCheck) {
                        Toast.makeText(this, "Save successfully", Toast.LENGTH_SHORT).show();
                        finish();
                    } else
                        finish();
                } else {
                    if (saveCheck) {
                        Toast.makeText(this, "Update successfully", Toast.LENGTH_SHORT).show();
//                        finish();
                    } else {
//                        finish();
                        Toast.makeText(this, "No change was made", Toast.LENGTH_SHORT).show();
                    }
                    //Toast.makeText(this, "Encounter error(s) when updating", Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.action_delete:

                new AlertDialog.Builder(this)
                        .setTitle("Delete note")
                        .setMessage("Are you sure you want to delete this note?")

                        // Specifying a listener allows you to take an action before dismissing the dialog.
                        // The dialog is automatically dismissed when a dialog button is clicked.
                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                // Continue with delete operation
                                boolean deleteCheck = deleteNote();
                                if (deleteCheck) {
                                    Toast.makeText(getBaseContext(), "Delete successfully", Toast.LENGTH_SHORT).show();
                                    finish();
                                } else
                                    Toast.makeText(getBaseContext(), "Encounter error(s) when deleting", Toast.LENGTH_SHORT).show();
                            }
                        })
                        // A null listener allows the button to dismiss the dialog and take no further action.
                        .setNegativeButton(android.R.string.no, null)
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .show();
                break;
            default:
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onPause() {
        Log.d(DetailActivity.class.getName(), "onPause");
        super.onPause();
        if (!isAttaching) {
            if (!isSaved) { //fix no duplication caused by calling saveNote() twice in a row
                saveNote();
//            isSaved = true;
                Log.d("SaveState", "save with " + isSaved);
            }
        } else
            isAttaching = false;
        //just implement create new note or something

    }

    @Override
    protected void onNewIntent(Intent intent) {
        if (intent.getAction() != null) {
            if (intent.getAction().equals(REQUEST_CHANGE)) {
                changeInAttach(intent);
            } else if (intent.getAction().equals(REQUEST_INSERT)) {
                //insert images from RecyclerView to EditText
                insertImageEditText(intent);
            }
        }
        super.onNewIntent(intent);
    }

    private void insertImageEditText(Intent intent) {
        int imgPos = intent.getIntExtra("imgPos", -1);
        if (imgPos != -1) {
            final int cursorLoc = detailContent.getSelectionStart();
            if (cursorLoc >= 0) {
                final Attachment inserted = images.get(imgPos);

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        Bitmap thumb = UtilHelper.getRoundedCornerBitmap(
                                UtilHelper.createThumbnail(inserted.getPath(), displayMetrics.widthPixels / 2, displayMetrics.widthPixels / 2), 20);

                        Log.d(SPANNABLE_TAG, "cursor location: " + cursorLoc);

                        Editable content = detailContent.getText();

                        String attachName = inserted.getPath().substring(inserted.getPath().lastIndexOf('/') + 1);
                        //cursor onPause+1 is the position of imageSpan
                        content.insert(cursorLoc, "\n$" + attachName + "$ \n");

                        //update position to change into image
                        int newCursorLoc = cursorLoc + 1;

                        Log.d(SPANNABLE_TAG, "cursor name: " + content.subSequence(newCursorLoc, newCursorLoc + attachName.length() + 1));

                        setSpans(thumb, content, attachName, newCursorLoc, inserted);

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                detailContent.setText(content);
                            }
                        });
                    }
                }).start();

            } else {
                Toast.makeText(this, "Please select a position in the content textbox!", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void setSpans(Bitmap thumb, Editable content, String attachName, int newCursorLoc, Attachment inserted) {
        //+2 for cursor+1 and length+1
        content.setSpan(new ImageSpan(getApplicationContext(), thumb), newCursorLoc, newCursorLoc + attachName.length() + 2, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        content.setSpan(new AlignmentSpan.Standard(Layout.Alignment.ALIGN_CENTER), newCursorLoc, newCursorLoc + attachName.length() + 2, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        content.setSpan(new ClickableSpan() {
            @Override
            public void onClick(@NonNull View widget) {
                File file = new File(inserted.getPath());
                Intent intent = new Intent();
                intent.setAction(Intent.ACTION_VIEW);
                Uri fileUri = FileProvider.getUriForFile(widget.getContext(),
                        "com.blacksun.quicknote.fileprovider",
                        file);
                intent.setData(fileUri);
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                widget.getContext().startActivity(intent);
            }
        }, newCursorLoc, newCursorLoc + attachName.length() + 2, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    }

    private void changeInAttach(Intent intent) {
        isChanged = true;
        String attachType = intent.getStringExtra(NoteContract.AttachEntry.COLUMN_ATTACH_TYPE);

        //delete attaches in content
        String attachPath = intent.getStringExtra(NoteContract.AttachEntry.COLUMN_ATTACH_PATH);

        //when attach type is image, delete corresponding image in EditText if existed
        if (attachType.equals(NoteContract.AttachEntry.IMAGE_TYPE)) {
            File curFile = new File(attachPath);
            String attachName = curFile.getName();
            String contentString = detailContent.getText().toString();
            String attachFormat = "$" + attachName + "$";
            if (contentString.contains(attachFormat)) {
                int startIdx = contentString.indexOf(attachFormat);
                Editable changedContent = detailContent.getText().delete(startIdx, startIdx + 1 + attachFormat.length());
                detailContent.setText(changedContent);
            }
        }
    }

    private void changeHeaderImageDefault() {
        ImageView toolbarImage = findViewById(R.id.toolbar_image);
        Calendar rightNow = Calendar.getInstance();
        int currentHourIn24Format = rightNow.get(Calendar.HOUR_OF_DAY);
        if (currentHourIn24Format >= 6 && currentHourIn24Format < 18) {
            toolbarImage.setImageDrawable(getResources().getDrawable(R.drawable.day));
        } else {
            toolbarImage.setImageDrawable(getResources().getDrawable(R.drawable.night));
        }
    }

    private static class ImageHandler implements TextWatcher {

        private final EditText mEditor;
        private final ArrayList<ImageSpan> mEmoticonsToRemove = new ArrayList<ImageSpan>();

        public ImageHandler(EditText editor) {
            // Attach the handler to listen for text changes.
            mEditor = editor;
            mEditor.addTextChangedListener(this);
        }

        @Override
        public void beforeTextChanged(CharSequence text, int start, int count, int after) {
            // Check if some text will be removed.
            if (count > after) {
                int end = start + count;
                Editable message = mEditor.getEditableText();
                ImageSpan[] list = message.getSpans(start, end, ImageSpan.class);

                for (ImageSpan span : list) {
                    // Get only the emoticons that are inside of the changed
                    // region.
                    int spanStart = message.getSpanStart(span);
                    int spanEnd = message.getSpanEnd(span);
                    if ((spanStart < end) && (spanEnd > start)) {
                        // Add to remove list
                        mEmoticonsToRemove.add(span);
                    }
                }
            }
        }

        @Override
        public void afterTextChanged(Editable text) {
            Editable message = mEditor.getEditableText();

            // Commit the emoticons to be removed.
            for (ImageSpan span : mEmoticonsToRemove) {
                int start = message.getSpanStart(span);
                int end = message.getSpanEnd(span);

                // Remove the span
                message.removeSpan(span);

                // Remove the remaining emoticon text.
                if (start != end) {
                    message.delete(start, end);
                }
            }
            mEmoticonsToRemove.clear();
        }

        @Override
        public void onTextChanged(CharSequence text, int start, int before, int count) {
        }
    }
}
