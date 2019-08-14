package com.blacksun.quicknote.activities;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.text.Editable;
import android.text.Layout;
import android.text.Selection;
import android.text.Spannable;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.style.AlignmentSpan;
import android.text.style.ClickableSpan;
import android.text.style.ImageSpan;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
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
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
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

    boolean isChanged = false;

    public static final String SPANNABLE_TAG = "spannable";
    public static final String ATTACH_TAG = "attach";

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

    }

    private void setUpRecyclerView() {
        images = new ArrayList<>();
        imageRecyclerAdapter = new ImageRecyclerAdapter(images, this);

        imageList.setHasFixedSize(false);
        imageList.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        imageList.setAdapter(imageRecyclerAdapter);

        files = new ArrayList<>();
        fileRecyclerAdapter = new FileRecyclerAdapter(files, this);

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
        intent = Intent.createChooser(chooseGallery, "Choose an image");
        startActivityForResult(intent, REQUEST_IMAGE_CHOOSER);
    }

    private void dispatchChooseFileIntent() {
        Intent chooseFile;
        Intent intent;
        chooseFile = new Intent(Intent.ACTION_GET_CONTENT);
        chooseFile.addCategory(Intent.CATEGORY_OPENABLE);
        chooseFile.setType("*/*");
        intent = Intent.createChooser(chooseFile, "Choose a file");
        startActivityForResult(intent, REQUEST_FILE_CHOOSER);
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getFilesDir();
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
//            Bundle extras = data.getExtras();
//            Bitmap imageBitmap = (Bitmap) extras.get("data");
//            testImage.setImageBitmap(imageBitmap);
            if (resultCode == RESULT_OK) {
//                if (!TextUtils.isEmpty(oldPhotoPath)) {
//                    File tobedeleted = new File(oldPhotoPath);
//                    tobedeleted.delete();
//                }

                //new RecyclerView
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
                Uri uri = data.getData();
                //String path = getPath(uri);

                String id = DocumentsContract.getDocumentId(uri);

                String fileName = getFileName(uri);

                long timeStamp = (new Date()).getTime();

                File savedFile = new File(getFilesDir().getAbsolutePath() + "/" + timeStamp + "_" + fileName);
                UtilHelper.copy(uri, savedFile, this);
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

                //just test get and open file, not saved into database yet
//                testFile.setText(fileName);
//
//                testFile.setOnClickListener(new View.OnClickListener() {
//                    @Override
//                    public void onClick(View v) {
//                        File file = new File(filePath);
//                        Intent intent = new Intent();
//                        intent.setAction(android.content.Intent.ACTION_VIEW);
//                        Uri fileUri = FileProvider.getUriForFile(v.getContext(),
//                                "com.blacksun.quicknote.fileprovider",
//                                file);
//                        intent.setData(fileUri);
//                        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
//                        startActivity(intent);
//                    }
//                });

            }
        } else if (requestCode == REQUEST_IMAGE_CHOOSER) {
            if (resultCode == RESULT_OK) {
                Uri uri = data.getData();
                //String path = getPath(uri);

                String id = DocumentsContract.getDocumentId(uri);

                String fileName = getFileName(uri);
                try {
                    File savedFile = createImageFile();
                    UtilHelper.copy(uri, savedFile, this);
                    String filePath = savedFile.getAbsolutePath();

                    //new RecyclerView
//                    images.add(new Attachment(1, 1, "IMAGE", filePath));
//                    imageRecyclerAdapter.notifyDataSetChanged();

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
//                    createThumbnail(filePath);

                    //spannable test
                    spannableImage(newAttach);
                } catch (IOException e) {
                    Log.e("saveFile", "error saving file");
                }
            }
        }
    }

    private void spannableImage(Attachment newAttach) {
        int cursorLoc = detailContent.getSelectionStart();

        if (cursorLoc != -1) {
            Bitmap thumb = createThumbnail(currentPhotoPath);

            Log.d(SPANNABLE_TAG, "cursor location: " + cursorLoc);

            Editable content = detailContent.getText();


            String attachName = newAttach.getPath().substring(newAttach.getPath().lastIndexOf('/') + 1);
            //cursor +1 is the position of imageSpan
            content.insert(cursorLoc, "\n $" + attachName + "$ \n");

            //update position to change into image
            cursorLoc += 2;


            Log.d(SPANNABLE_TAG, "cursor name: " + content.subSequence(cursorLoc, cursorLoc + attachName.length() + 1));


            //+2 for cursor+1 and length+1
            content.setSpan(new ImageSpan(this, thumb), cursorLoc, cursorLoc + attachName.length() + 2, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            content.setSpan(new AlignmentSpan.Standard(Layout.Alignment.ALIGN_CENTER), cursorLoc, cursorLoc + attachName.length() + 2, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            content.setSpan(new ClickableSpan() {
                @Override
                public void onClick(@NonNull View widget) {
                    File file = new File(newAttach.getPath());
                    Intent intent = new Intent();
                    intent.setAction(Intent.ACTION_VIEW);
                    Uri fileUri = FileProvider.getUriForFile(widget.getContext(),
                            "com.blacksun.quicknote.fileprovider",
                            file);
                    intent.setData(fileUri);
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    widget.getContext().startActivity(intent);
                }
            }, cursorLoc, cursorLoc + attachName.length() + 2, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

            detailContent.setText(content);
        }
    }

    private Bitmap createThumbnail(final String path) {
        return ThumbnailUtils.extractThumbnail(BitmapFactory.decodeFile(path), 500, 500);
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
                detailTitle.setText(bundle.getString("title"));
                detailContent.setText(bundle.getString("content"));

                //test collapsing toolbar
                collapsingToolbar.setTitle(bundle.getString("title"));

                String title = bundle.getString("title");
                String content = bundle.getString("content");
                long dateCreated = bundle.getLong("dateCreated");
                long dateModified = bundle.getLong("dateModified");
                long id = bundle.getLong("noteID");
                String img = bundle.getString("imagePath");
                currentPhotoPath = img;
                if (!TextUtils.isEmpty(img)) {
                    File currentImg = new File(currentPhotoPath);
                    if (currentImg.exists())
                        createThumbnail(currentPhotoPath);
                }


                ArrayList<Attachment> currentImages = AttachManager.newInstance(this).getAttach(id, NoteContract.AttachEntry.IMAGE_TYPE);
                images.clear();
                images.addAll(currentImages);
                Log.d("attach", "id " + id + ", number " + images.size());
                imageRecyclerAdapter.notifyDataSetChanged();


                newImages = new ArrayList<>();
                newFiles = new ArrayList<>();

                ArrayList<Attachment> currentFiles = AttachManager.newInstance(this).getAttach(id, NoteContract.AttachEntry.FILE_TYPE);
                files.clear();
                files.addAll(currentFiles);
                Log.d("attach", "id " + id + ", number of files " + files.size());
                fileRecyclerAdapter.notifyDataSetChanged();


//                currentNote = new Note(title, content, id, dateCreated, dateModified, img);
                currentNote = new Note(title, content, id, dateCreated, dateModified);
//                isSaved = false;


                //spannable test
                Editable contentSpan = detailContent.getText();
                String contentString = contentSpan.toString();
                for (Attachment image : images) {
                    String attachName = image.getPath().substring(image.getPath().lastIndexOf('/') + 1);


                    if (contentString.contains("$" + attachName + "$")) {
                        int idxStart = contentString.indexOf("$" + attachName + "$");

                        Bitmap thumb = createThumbnail(image.getPath());

                        contentSpan.setSpan(new ImageSpan(this, thumb), idxStart, idxStart + attachName.length() + 2, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                        contentSpan.setSpan(new AlignmentSpan.Standard(Layout.Alignment.ALIGN_CENTER), idxStart, idxStart + attachName.length() + 2, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                        contentSpan.setSpan(new ClickableSpan() {
                            @Override
                            public void onClick(@NonNull View widget) {
                                File file = new File(image.getPath());
                                Intent intent = new Intent();
                                intent.setAction(android.content.Intent.ACTION_VIEW);
                                Uri fileUri = FileProvider.getUriForFile(widget.getContext(),
                                        "com.blacksun.quicknote.fileprovider",
                                        file);
                                intent.setData(fileUri);
                                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                                widget.getContext().startActivity(intent);
                            }
                        }, idxStart, idxStart + attachName.length() + 2, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    }
                }
                detailContent.setText(contentSpan);
            }
        } else {
            collapsingToolbar.setTitle("New note");
        }


        collapsingToolbar.setCollapsedTitleTextColor(ContextCompat.getColor(this, R.color.white));
        collapsingToolbar.setExpandedTitleColor(ContextCompat.getColor(this, R.color.transparent));
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
                Selection.removeSelection(buffer);
//                widget.setHighlightColor(Color.argb(50,100,0,0));
                return super.onTouchEvent(widget, buffer, event);
            }
        });
    }


    private boolean saveNote() {
        String title = detailTitle.getText().toString();
        String content = detailContent.getText().toString();

//        Log.d("save_state", "something title: " + TextUtils.isEmpty(title) + " content: \""+TextUtils.isEmpty(content)+"\""+(currentNote == null)+"   "+(images == null)+"  "+(files == null));

        if (TextUtils.isEmpty(title) && TextUtils.isEmpty(content) &&
                currentNote == null && images.size() == 0 && files.size() == 0) {
            Toast.makeText(this, "Note cannot be empty!", Toast.LENGTH_LONG).show();
            Log.d("saveState", "nothing");
            return false;
        }

//        if (TextUtils.isEmpty(title)) {
//            detailTitle.setError("Title is required");
//            return false;
//        }
//
//        if (TextUtils.isEmpty(content)) {
//            detailContent.setError("Content is required");
//            return false;
//        }
        if (TextUtils.isEmpty(title)) {
            title = "No title";
            //return false;
        }


        if (currentNote == null) {
            Note note = new Note();
            note.setTitle(title);

            if (!TextUtils.isEmpty(content))
                note.setContent(content);
            else
                note.setContent("");
            long newId = NoteManager.newInstance(this).create(note);

            if (newId == -1)
                Log.e("saveState", "error creating new note.");

            if (images != null) {
                for (int imagePos = 0; imagePos < images.size(); imagePos++) {
                    Attachment currentAttach = images.get(imagePos);
                    currentAttach.setNote_id(newId);
                    AttachManager.newInstance(this).create(currentAttach);
                }
                Log.d("saveState", "is adding images with size " + images.size());
            }

            if (files != null) {
                for (int filePos = 0; filePos < files.size(); filePos++) {
                    Attachment currentAttach = files.get(filePos);
                    currentAttach.setNote_id(newId);
                    AttachManager.newInstance(this).create(currentAttach);
                }
            }

            //update currentNote to show that a new note has been created
//            note.setId(newId);
//            currentNote = note;
//            newImages.addAll(images);
//            newFiles.addAll(files);

        } else {
            if (currentNote.getTitle().equals(title) && currentNote.getContent().equals(content)
                    && newImages.size() == 0 && newFiles.size() == 0 && !isChanged) {
                return false;
            }


            currentNote.setTitle(title);
            if (!TextUtils.isEmpty(content))
                currentNote.setContent(content);
//            currentNote.setImagePath(currentPhotoPath);
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
        collapsingToolbar.setTitle(detailTitle.getText());
        return true;

    }

    private boolean deleteNote() {

        if (currentNote == null)
            return false;
        else {
//            if (!TextUtils.isEmpty(currentPhotoPath)) {
//                File tobedeleted = new File(currentPhotoPath);
//                tobedeleted.delete();
//                Log.d("filepath", "Deleted");
//            } else
//                Log.d("filepath", "Not Deleted");

            long noteId = currentNote.getId();
            ArrayList<Attachment> currentAttachments = AttachManager.newInstance(this).getAttach(noteId, NoteContract.AttachEntry.ANY_TYPE);
            Log.d("attach", "sizeDel " + currentAttachments.size());

            for (int attachPos = 0; attachPos < currentAttachments.size(); attachPos++) {
                Attachment curAttach = currentAttachments.get(attachPos);
//                long curAttachId = curAttach.getId();
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
//                        Snackbar.make(getWindow().getDecorView(), "Save successfully", Snackbar.LENGTH_LONG)
//                                .setAction("Action", null).show();
                        Toast.makeText(this, "Save successfully", Toast.LENGTH_SHORT).show();
                        finish();
                    } else
                        finish();
//                        Snackbar.make(getWindow().getDecorView(), "Encounter error(s) when saving", Snackbar.LENGTH_LONG)
//                                .setAction("Action", null).show();
                    //Toast.makeText(this, "Encounter error(s) when saving", Toast.LENGTH_SHORT).show();
                } else {
                    if (saveCheck) {
//                        Snackbar.make(findViewById(R.id.detail_content), "Update successfully", Snackbar.LENGTH_LONG)
//                                .setAction("Action", null).show();
                        Toast.makeText(this, "Update successfully", Toast.LENGTH_SHORT).show();
//                        finish();
                    } else {
//                        finish();
                        Toast.makeText(this, "No change was made", Toast.LENGTH_SHORT).show();
                    }

//                        Snackbar.make(getWindow().getDecorView(), "Encounter error(s) when updating", Snackbar.LENGTH_LONG)
//                                .setAction("Action", null).show();
                    //Toast.makeText(this, "Encounter error(s) when updating", Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.action_delete:
                boolean deleteCheck = deleteNote();
                if (deleteCheck) {
//                    Snackbar.make(getWindow().getDecorView(), "Delete successfully", Snackbar.LENGTH_LONG)
//                            .setAction("Action", null).show();
                    Toast.makeText(this, "Delete successfully", Toast.LENGTH_SHORT).show();
                    finish();
                } else
//                    Snackbar.make(getWindow().getDecorView(), "Encounter error(s) when deleting", Snackbar.LENGTH_LONG)
//                            .setAction("Action", null).show();
                    Toast.makeText(this, "Encounter error(s) when deleting", Toast.LENGTH_SHORT).show();
                break;
            default:
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onPause() {
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
        if (intent.getAction().equals(REQUEST_CHANGE)) {
            isChanged = true;

            //delete attaches in content
            String attachName = intent.getStringExtra(NoteContract.AttachEntry.COLUMN_ATTACH_PATH);
            String contentString = detailContent.getText().toString();
            String attachFormat = "$" + attachName + "$";
            if (contentString.contains(attachFormat)) {
                int startIdx = contentString.indexOf(attachFormat);
                Editable changedContent = detailContent.getText().delete(startIdx, startIdx + 1 + attachFormat.length());
                detailContent.setText(changedContent);
            }
        }

        super.onNewIntent(intent);
    }
}
