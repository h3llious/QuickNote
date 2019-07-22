package com.blacksun.quicknote.activities;


import android.content.ContentResolver;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.blacksun.quicknote.R;
import com.blacksun.quicknote.adapters.NoteRecyclerAdapter;
import com.blacksun.quicknote.data.NoteManager;
import com.blacksun.quicknote.models.DriveFileHolder;
import com.blacksun.quicknote.models.Note;
import com.blacksun.quicknote.utils.DatabaseHelper;
import com.blacksun.quicknote.utils.DriveServiceHelper;
import com.blacksun.quicknote.utils.UtilHelper;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.drive.DriveFolder;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.http.FileContent;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;


public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    ArrayList<Note> notes;
    RecyclerView noteList;
    NoteRecyclerAdapter noteRecyclerAdapter;

    View emptyView;
    DrawerLayout drawerLayout;
    ActionBarDrawerToggle drawerToggle;
    NavigationView navigationView;

    SignInButton googleSignInButton;
    GoogleSignInClient googleSignInClient;

    static final int REQUEST_GOOGLE_SIGN_IN = 4;

    private static final String SIGN_IN_TAG = "SignIn";

    //TODO clean up
    private static final String DRIVE_TAG = "GDrive";

    TextView googleEmailText, googleNameText;
    ImageView googleAvatarImg;

    GoogleSignInAccount account;
    Button googleSignOutButton;
    Bitmap loadedAvatar;

    Drive googleServiceDrive;
    private static final String[] DRIVE_SCOPES = {DriveScopes.DRIVE_FILE, DriveScopes.DRIVE_APPDATA};

    DriveServiceHelper driveServiceHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_drawer);

        initialize();

        //sign in, sign out by google account
        setUpGoogleAccount();


        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeButtonEnabled(true);
        }

        //new navigation drawer
        drawerToggle = new ActionBarDrawerToggle(this, drawerLayout, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(drawerToggle);
        drawerToggle.syncState();
        navigationView.setNavigationItemSelectedListener(this);


        FloatingActionButton createNewNote = findViewById(R.id.fab);
        createNewNote.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
//                        .setAction("Action", null).show();
                Intent intent = new Intent(MainActivity.this, DetailActivity.class);
                startActivity(intent);
            }
        });

        retrieveNoteList();

    }

    private void retrieveNoteList() {
        notes = new ArrayList<>();

        getInfo();

        noteList = findViewById(R.id.note_list);

        if (notes.size() == 0) {
            emptyView.setVisibility(View.VISIBLE);
        }

        noteRecyclerAdapter = new NoteRecyclerAdapter(notes);

        noteList.setHasFixedSize(true);
        noteList.setLayoutManager(new LinearLayoutManager(this));
        noteList.setAdapter(noteRecyclerAdapter);
    }

    private void setUpGoogleAccount() {
        // Configure sign-in to request the user's ID, email address, and basic
        // profile. ID and basic profile are included in DEFAULT_SIGN_IN.
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .build();

        // Build a GoogleSignInClient with the options specified by gso.
        googleSignInClient = GoogleSignIn.getClient(this, gso);
        googleSignInButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent signInIntent = googleSignInClient.getSignInIntent();
                startActivityForResult(signInIntent, REQUEST_GOOGLE_SIGN_IN);
            }
        });

        googleSignOutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onSignOut();
            }
        });
    }

    private void initialize() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        drawerLayout = findViewById(R.id.drawer_layout);
        emptyView = findViewById(R.id.empty_view);
        navigationView = findViewById(R.id.nav_view);
        googleSignInButton = navigationView.getHeaderView(0).findViewById(R.id.google_sign_in);
        googleSignOutButton = navigationView.getHeaderView(0).findViewById(R.id.google_sign_out);
        googleAvatarImg = navigationView.getHeaderView(0).findViewById(R.id.google_avatar);
        googleEmailText = navigationView.getHeaderView(0).findViewById(R.id.google_email);
        googleNameText = navigationView.getHeaderView(0).findViewById(R.id.google_username);
    }

    private void onSignOut() {
        googleSignInClient.signOut();
        googleSignInButton.setVisibility(View.VISIBLE);
        googleSignOutButton.setVisibility(View.GONE);
        googleEmailText.setText("");
        googleNameText.setText(R.string.app_name);
        googleAvatarImg.setImageDrawable(getResources().getDrawable(R.mipmap.ic_launcher_round));
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        drawerToggle.syncState();
    }


    @Override
    protected void onStart() {
        super.onStart();
        //update after create new note or delete
        getInfo();
        noteRecyclerAdapter.notifyDataSetChanged();

        //check if already logged in
        checkLoggedIn();
    }

    private void checkLoggedIn() {
        account = GoogleSignIn.getLastSignedInAccount(this);
        if (account != null) {
            //Toast.makeText(this, "Already Logged In", Toast.LENGTH_SHORT).show();
            createDriveCredential(account);

            onLoggedIn(account);
            googleSignInButton.setVisibility(View.GONE);
            googleSignOutButton.setVisibility(View.VISIBLE);
        } else {
            Log.d(SIGN_IN_TAG, "Not logged in");
        }
    }

    private void getInfo() {
        ArrayList<Note> newNotes = NoteManager.newInstance(this).getAllNotes();
        notes.clear();
        notes.addAll(newNotes);
        emptyView.setVisibility(View.GONE);
        if (notes.size() == 0) {
            emptyView.setVisibility(View.VISIBLE);
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        if (drawerToggle.onOptionsItemSelected(item)) {
            return true;
        }

        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case REQUEST_GOOGLE_SIGN_IN:
                    try {
                        Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
                        account = task.getResult(ApiException.class);

                        if (account != null) {
                            createDriveCredential(account);
                            onLoggedIn(account);
                        }
                    } catch (ApiException e) {
                        Log.e(SIGN_IN_TAG, "SignInResult failed " + e.getStatusCode());
                    }
                    break;
            }
        }
    }

    private void createDriveCredential(GoogleSignInAccount account) {
        GoogleAccountCredential credential = GoogleAccountCredential.usingOAuth2(this,
                Arrays.asList(DRIVE_SCOPES));
        credential.setSelectedAccount(account.getAccount());
        googleServiceDrive = new Drive.Builder(AndroidHttp.newCompatibleTransport(), new GsonFactory(), credential)
                .setApplicationName(getResources().getString(R.string.app_name))
                .build();


    }

    private void onLoggedIn(GoogleSignInAccount account) {
        googleNameText.setText(account.getDisplayName());
        googleEmailText.setText(account.getEmail());

        googleSignInButton.setVisibility(View.GONE);
        googleSignOutButton.setVisibility(View.VISIBLE);

        Log.d(SIGN_IN_TAG, "" + account.getPhotoUrl());

        if (loadedAvatar != null)
            googleAvatarImg.setImageBitmap(loadedAvatar);
        else {
            Uri avatarUri = account.getPhotoUrl();

            if (avatarUri == null) //user doesn't have avatar
                googleAvatarImg.setImageDrawable(getResources().getDrawable(R.drawable.ic_user));
            else
                new DownloadImgTask(this).execute(avatarUri.toString());
        }
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_home) {

        } else if (id == R.id.nav_tools) {

        } else if (id == R.id.nav_upload) {

            //testing: upload database into drive.
            final String PACKAGE_NAME = getPackageName();
            final String DATABASE_NAME = DatabaseHelper.DATABASE_NAME;
//            final String DATABASE_PATH = "/data/data/" + PACKAGE_NAME + "/databases/" + DATABASE_NAME;
            final File FILE_DATABASE =
                    new File(Environment.getDataDirectory() + "/data/" + PACKAGE_NAME + "/databases/" + DATABASE_NAME);
            final String MIME_TYPE = "application/x-sqlite-3";
            final String FOLDER_NAME = "files";


            if (googleServiceDrive == null) {
                Toast.makeText(this, "Please sign in with your Google account!", Toast.LENGTH_SHORT).show();
            } else {

                if (!GoogleSignIn.hasPermissions(account, new Scope(DriveScopes.DRIVE_APPDATA), new Scope(DriveScopes.DRIVE_FILE))) {
                    GoogleSignIn.requestPermissions(this, 10, account, new Scope(DriveScopes.DRIVE_APPDATA), new Scope(DriveScopes.DRIVE_FILE));
                }

                if (driveServiceHelper == null) {
                    driveServiceHelper = new DriveServiceHelper(googleServiceDrive, this);
                }

                Thread testThread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            //upload db
                            driveServiceHelper.upload(FILE_DATABASE, MIME_TYPE, null);
//                            driveServiceHelper.upload(FILE_DATABASE, MIME_TYPE, "appDataFolder");

                            //create files folder on Drive
//                            String folderID = driveServiceHelper.createFolder(FOLDER_NAME, "appDataFolder");
                            String folderID = driveServiceHelper.createFolder(FOLDER_NAME, null);

                            //upload files
                            File directory = getFilesDir();
                            File[] files = directory.listFiles();
                            Log.d("Files", "Size: " + files.length);
//                            ArrayList<String> allFilesPath = new ArrayList<>();
                            for (File child : files) {
                                String name = child.getName();
                                if (!name.equals("instant-run")) {
//                                    allFilesPath.add(name);
                                    driveServiceHelper.upload(child, getMIMEType(child), folderID);
                                }
                                Log.d("Files", getMIMEType(child) + " FileName:" + child.getName());
                            }

                        } catch (UserRecoverableAuthIOException e) {
                            Log.e(DRIVE_TAG, "Error " + e.getMessage());
                            e.printStackTrace();
                            startActivityForResult(e.getIntent(), 6);

                        } catch (IOException e) {
                            Log.e(DRIVE_TAG, "Error " + e.getMessage());
                            e.printStackTrace();
                        }

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(getBaseContext(), "Finished uploading data into Drive", Toast.LENGTH_LONG).show();
                            }
                        });

                    }
                });
                testThread.start();


//                    System.out.println("File ID: " + file.getId());

            }
        } else if (id == R.id.nav_download) {


            //testing: download database into drive.
            final String PACKAGE_NAME = getPackageName();
            final String DATABASE_NAME = DatabaseHelper.DATABASE_NAME;
//            final String DATABASE_PATH = "/data/data/" + PACKAGE_NAME + "/databases/" + DATABASE_NAME;
            final String databasePath = Environment.getDataDirectory() + "/data/" + PACKAGE_NAME + "/databases/" + DATABASE_NAME;
            final File FILE_DATABASE = new File(databasePath);
            final String MIME_TYPE_DB = "application/x-sqlite-3";
            final String MIME_TYPE_FOLDER = "application/vnd.google-apps.folder";
            final String FOLDER_NAME = "files";


            if (googleServiceDrive == null) {
                Toast.makeText(this, "Please sign in with your Google account!", Toast.LENGTH_SHORT).show();
            } else {

                if (!GoogleSignIn.hasPermissions(account, new Scope(DriveScopes.DRIVE_APPDATA), new Scope(DriveScopes.DRIVE_FILE))) {
                    GoogleSignIn.requestPermissions(this, 10, account, new Scope(DriveScopes.DRIVE_APPDATA), new Scope(DriveScopes.DRIVE_FILE));
                }

                if (driveServiceHelper == null) {
                    driveServiceHelper = new DriveServiceHelper(googleServiceDrive, this);
                }

                Thread testThread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {


                            //database
                            ArrayList<DriveFileHolder> database = driveServiceHelper.search(MIME_TYPE_DB, DATABASE_NAME, null);
                            if (database.size() >= 1) {
                                String databaseId = database.get(0).getId();

                                driveServiceHelper.download(databasePath, databaseId);
                                Log.d(DRIVE_TAG, "Database downloaded " + databaseId);
                            } else {
                                Log.e(DRIVE_TAG, "Cannot download database");
                            }


                            //folder files
                            ArrayList<DriveFileHolder> filesFolder = driveServiceHelper.search(MIME_TYPE_FOLDER, FOLDER_NAME, null);
                            String folderId = null;
                            if (filesFolder.size() >= 1) {
                                folderId = filesFolder.get(0).getId();
                            } else {
                                Log.e(DRIVE_TAG, "Cannot get \"files\" folder");
                            }

                            //attachments
                            if (folderId != null) {
                                ArrayList<DriveFileHolder> attachments = driveServiceHelper.search(null, null, folderId);
                                String filesDir = getFilesDir().getAbsolutePath();
                                for (DriveFileHolder attach : attachments) {
                                    String attachId = attach.getId();

                                    driveServiceHelper.download(filesDir+"/"+attach.getName(), attachId);

                                }
                            } else {
                                Log.e(DRIVE_TAG, "No attachment");
                            }

                        } catch (UserRecoverableAuthIOException e) {
                            Log.e(DRIVE_TAG, "Error " + e.getMessage());
                            e.printStackTrace();
                            startActivityForResult(e.getIntent(), 6);

                        } catch (IOException e) {
                            Log.e(DRIVE_TAG, "Error " + e.getMessage());
                            e.printStackTrace();
                        }

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                //update after create new note or delete
                                getInfo();
                                noteRecyclerAdapter.notifyDataSetChanged();
                                Toast.makeText(getBaseContext(), "Finished synchronizing data into Drive", Toast.LENGTH_LONG).show();
                            }
                        });

                    }
                });
                testThread.start();


//                    System.out.println("File ID: " + file.getId());

            }

        }

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }


    //needed for Drive upload
    public String getMIMEType(File child) {
        String mimeType;
        Uri uri = Uri.fromFile(child);
        if (uri.getScheme().equals(ContentResolver.SCHEME_CONTENT)) {
            ContentResolver cr = this.getContentResolver();
            mimeType = cr.getType(uri);
        } else {
            String fileExtension = MimeTypeMap.getFileExtensionFromUrl(uri.toString());
            mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(
                    fileExtension.toLowerCase());
        }
        return mimeType;
    }

    private static class DownloadImgTask extends AsyncTask<String, Void, Bitmap> {
        private WeakReference<MainActivity> activityReference;

        DownloadImgTask(MainActivity context) {
            activityReference = new WeakReference<>(context);
        }

        @Override
        protected void onPreExecute() {
            MainActivity activity = activityReference.get();
            if (activity == null || activity.isFinishing()) return;

            NavigationView navigationView = activity.findViewById(R.id.nav_view);

            ImageView avatar = navigationView.getHeaderView(0).findViewById(R.id.google_avatar);
            avatar.setImageDrawable(activity.getResources().getDrawable(R.drawable.ic_user));
        }

        @Override
        protected Bitmap doInBackground(String... strings) {
            publishProgress();
            String url = strings[0];
            Bitmap bitmap = null;

            try {
                InputStream in = new java.net.URL(url).openStream();
                bitmap = BitmapFactory.decodeStream(in);
            } catch (Exception e) {
                Log.e(SIGN_IN_TAG, e.getMessage());
                e.printStackTrace();
            }

            return bitmap;
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {

            // get a reference to the activity if it is still there
            MainActivity activity = activityReference.get();
            if (activity == null || activity.isFinishing()) return;


            ImageView avatar = activity.findViewById(R.id.google_avatar);

            Bitmap rounded = UtilHelper.getRoundedCornerBitmap(bitmap);
            activity.loadedAvatar = rounded;

            avatar.setImageBitmap(rounded);
        }
    }
}
