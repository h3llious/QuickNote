package com.blacksun.quicknote.activities;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.blacksun.quicknote.R;
import com.blacksun.quicknote.data.NoteManager;
import com.blacksun.quicknote.models.Note;
import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

public class DetailActivity extends AppCompatActivity {


    EditText detailTitle, detailContent;
    CollapsingToolbarLayout collapsingToolbar;
    Note currentNote = null;
    boolean isNew = true;

    ImageButton detailCamera, detailImage, detailFile, detailCheckbox;

    PackageManager pm;

    static final int REQUEST_IMAGE_CAPTURE = 1;
    static final int REQUEST_FILE_CHOOSER = 2;
    static final int REQUEST_IMAGE_CHOOSER = 3;

    ImageView testImage;
    String currentPhotoPath;
    String currentPhotoName;
    String oldPhotoPath;

    TextView testFile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        initialize();

        pm = getPackageManager();

        detailCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!pm.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)) {
                    Toast.makeText(v.getContext(), "Device does not have camera", Toast.LENGTH_SHORT).show();
                } else {
                    dispatchTakePictureIntent();
                }
            }
        });

        detailFile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dispatchChooseFileIntent();

//                File file = new File("/data/user/0/com.blacksun.quicknote/files/NewTextFile.txt");
//                testFile.setText(file.getName());
//
//                testFile.setOnClickListener(new View.OnClickListener() {
//                    @Override
//                    public void onClick(View v) {
//                        File file = new File("/data/user/0/com.blacksun.quicknote/files/NewTextFile.txt");
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
        });

        detailImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent chooseGallery;
                Intent intent;
                chooseGallery = new Intent(Intent.ACTION_GET_CONTENT);
                chooseGallery.addCategory(Intent.CATEGORY_OPENABLE);
                chooseGallery.setType("image/*");
                intent = Intent.createChooser(chooseGallery, "Choose an image");
                startActivityForResult(intent, REQUEST_IMAGE_CHOOSER);
            }
        });


        setUpAppBar();

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

    //    public static void copy(File src, File dst) {
    public void copy(Uri uri, File dst) {
//        try (InputStream in = new FileInputStream(src);
        try (InputStream in = getContentResolver().openInputStream(uri);
             OutputStream out = new FileOutputStream(dst)) {
            // Transfer bytes from in to out
            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
        } catch (IOException e) {
            System.out.println(e);
        }
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
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
                if (!TextUtils.isEmpty(oldPhotoPath)) {
                    File tobedeleted = new File(oldPhotoPath);
                    tobedeleted.delete();
                }
                createThumbnail(currentPhotoPath);
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

                File savedFile = new File(getFilesDir().getAbsolutePath() + "/" + fileName);
                copy(uri, savedFile);
                final String filePath = savedFile.getAbsolutePath();

                Log.d("filepath", filePath + ": " + fileName);

                //just test get and open file, not saved into database yet
                testFile.setText(fileName);

                testFile.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        File file = new File(filePath);
                        Intent intent = new Intent();
                        intent.setAction(android.content.Intent.ACTION_VIEW);
                        Uri fileUri = FileProvider.getUriForFile(v.getContext(),
                                "com.blacksun.quicknote.fileprovider",
                                file);
                        intent.setData(fileUri);
                        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        startActivity(intent);
                    }
                });

            }
        } else if (requestCode == REQUEST_IMAGE_CHOOSER) {
            if (resultCode == RESULT_OK) {
                Uri uri = data.getData();
                //String path = getPath(uri);

                String id = DocumentsContract.getDocumentId(uri);

                String fileName = getFileName(uri);
                try {
                    File savedFile = createImageFile();
                    copy(uri, savedFile);
                    String filePath = savedFile.getAbsolutePath();

                    Log.d("filepath", filePath + ": " + fileName);
                    createThumbnail(filePath);
                } catch (IOException e) {
                    Log.e("saveFile", "error saving file");
                }
            }
        }
    }

    private void createThumbnail(final String path) {
        Bitmap thumbImage = ThumbnailUtils.extractThumbnail(BitmapFactory.decodeFile(path), 500, 500);
        testImage.setImageBitmap(thumbImage);

        testImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                File file = new File(path);
                Intent intent = new Intent();
                intent.setAction(android.content.Intent.ACTION_VIEW);
                Uri fileUri = FileProvider.getUriForFile(v.getContext(),
                        "com.blacksun.quicknote.fileprovider",
                        file);
                intent.setData(fileUri);
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                startActivity(intent);
            }
        });
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

    //Set title of the note and attributes of collapsing toolbar
    private void setUpAppBar() {
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

                currentNote = new Note(title, content, id, dateCreated, dateModified, img);
                isNew = false;
            }
        } else {
            collapsingToolbar.setTitle("New note");
        }

        collapsingToolbar.setCollapsedTitleTextColor(ContextCompat.getColor(this, R.color.white));
        collapsingToolbar.setExpandedTitleColor(ContextCompat.getColor(this, R.color.transparent));
    }

    private void initialize() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
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

        //just test TODO change into recyclerView
        testImage = findViewById(R.id.test_image);
        testFile = findViewById(R.id.test_file);
    }

    private boolean saveNote() {
        String title = detailTitle.getText().toString();
        if (TextUtils.isEmpty(title)) {
            detailTitle.setError("Title is required");
            return false;
        }

        String content = detailContent.getText().toString();
        if (TextUtils.isEmpty(content)) {
            detailContent.setError("Content is required");
            return false;
        }

        if (currentNote == null) {
            Note note = new Note();
            note.setTitle(title);
            note.setContent(content);
            note.setImagePath(currentPhotoPath);
            NoteManager.newInstance(this).create(note);
        } else {

            currentNote.setTitle(title);
            currentNote.setContent(content);
            currentNote.setImagePath(currentPhotoPath);
            NoteManager.newInstance(this).update(currentNote);
        }
        collapsingToolbar.setTitle(detailTitle.getText());
        return true;

    }

    private boolean deleteNote() {

        if (currentNote == null)
            return false;
        else {
            if (!TextUtils.isEmpty(currentPhotoPath)) {
                File tobedeleted = new File(currentPhotoPath);
                tobedeleted.delete();
                Log.d("filepath", "Deleted");
            }
            Log.d("filepath", "Not Deleted");
            NoteManager.newInstance(this).delete(currentNote);
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

                if (currentNote == null) {
                    if (saveCheck) {
//                        Snackbar.make(getWindow().getDecorView(), "Save successfully", Snackbar.LENGTH_LONG)
//                                .setAction("Action", null).show();
                        Toast.makeText(this, "Save successfully", Toast.LENGTH_SHORT).show();
                        finish();
                    } else
//                        Snackbar.make(getWindow().getDecorView(), "Encounter error(s) when saving", Snackbar.LENGTH_LONG)
//                                .setAction("Action", null).show();
                        Toast.makeText(this, "Encounter error(s) when saving", Toast.LENGTH_SHORT).show();
                } else {
                    if (saveCheck) {
//                        Snackbar.make(findViewById(R.id.detail_content), "Update successfully", Snackbar.LENGTH_LONG)
//                                .setAction("Action", null).show();
                        Toast.makeText(this, "Update successfully", Toast.LENGTH_SHORT).show();
                        //finish();
                    } else
//                        Snackbar.make(getWindow().getDecorView(), "Encounter error(s) when updating", Snackbar.LENGTH_LONG)
//                                .setAction("Action", null).show();
                        Toast.makeText(this, "Encounter error(s) when updating", Toast.LENGTH_SHORT).show();
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
        if (!isNew)
            saveNote();
    }
}
