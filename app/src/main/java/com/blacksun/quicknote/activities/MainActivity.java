package com.blacksun.quicknote.activities;


import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.ProgressBar;

import androidx.appcompat.widget.SearchView;

import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.view.GravityCompat;
import androidx.core.view.MenuCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.blacksun.quicknote.R;
import com.blacksun.quicknote.adapters.NoteRecyclerAdapter;
import com.blacksun.quicknote.controllers.AttachManager;
import com.blacksun.quicknote.data.NoteContract;
import com.blacksun.quicknote.controllers.NoteManager;
import com.blacksun.quicknote.models.Attachment;
import com.blacksun.quicknote.models.Note;
import com.blacksun.quicknote.thread.SyncDownTask;
import com.blacksun.quicknote.thread.SyncManager;
import com.blacksun.quicknote.thread.SyncTask;
import com.blacksun.quicknote.thread.SyncUpTask;
import com.blacksun.quicknote.utils.DriveServiceHelper;
import com.blacksun.quicknote.utils.RecyclerItemTouchHelper;
import com.blacksun.quicknote.utils.UtilHelper;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.snackbar.Snackbar;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;

import java.io.File;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;

import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;

import static com.blacksun.quicknote.utils.DriveServiceHelper.DRIVE_TAG;
import static com.blacksun.quicknote.utils.UtilHelper.isInternetAvailable;
import static com.blacksun.quicknote.utils.UtilHelper.removeTempFiles;


public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener, RecyclerItemTouchHelper.RecyclerItemTouchHelperListener {

    public static final String REQUEST_RESTART = "restart";
    public static final String REQUEST_TAB = "home";
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

    TextView googleEmailText, googleNameText;
    ImageView googleAvatarImg;

    GoogleSignInAccount account;
    Button googleSignOutButton;
    Bitmap loadedAvatar;

    Drive googleServiceDrive;
    private static final String[] DRIVE_SCOPES = {DriveScopes.DRIVE_FILE, DriveScopes.DRIVE_APPDATA};

    DriveServiceHelper driveServiceHelper;

    public static String PACKAGE_NAME;
    public static File DIRECTORY;

    ProgressBar progressBar;
    View dimView;

    SharedPreferences sharedPreferences;

    boolean sortByTime, sortDescending;
    ConstraintLayout constraintLayout;

    SearchView searchView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_drawer);

        //package name
        PACKAGE_NAME = getPackageName();
        DIRECTORY = getFilesDir();

        //save reorder options
        setSharedPref();

        initializeView();

        //Remove temp files
        removeTempFiles();

        //sign in, sign out by google account
        setUpGoogleAccount();

        //set up "hamburger" button
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeButtonEnabled(true);
        }

        //new navigation drawer
        drawerToggle = new ActionBarDrawerToggle(this, drawerLayout, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(drawerToggle);
        drawerToggle.syncState();
        navigationView.setNavigationItemSelectedListener(this);

        //create new note activity
        newNoteFAB();

        retrieveNoteList();

        Log.d(MainActivity.class.getName(), "onCreate Activity");
    }

    private void newNoteFAB() {
        FloatingActionButton createNewNote = findViewById(R.id.fab);
        createNewNote.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, DetailActivity.class);
                startActivity(intent);
            }
        });
    }

    private void setSharedPref() {
        sharedPreferences = getPreferences(MODE_PRIVATE);
        sortByTime = sharedPreferences.getBoolean(getResources().getString(R.string.isSortedByTime), true);
        sortDescending = sharedPreferences.getBoolean(getResources().getString(R.string.isDescending), true);
    }

    private void retrieveNoteList() {
        notes = new ArrayList<>();

        getInfo();

        noteList = findViewById(R.id.note_list);

        //empty note list, show Empty View
        if (notes.size() == 0) {
            emptyView.setVisibility(View.VISIBLE);
        }

        noteRecyclerAdapter = new NoteRecyclerAdapter(notes);

        noteList.setHasFixedSize(true);
        noteList.setLayoutManager(new LinearLayoutManager(this));
        noteList.setAdapter(noteRecyclerAdapter);

        //swipe
        ItemTouchHelper.SimpleCallback itemTouchHelperCallback = new RecyclerItemTouchHelper(
                0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT, this);
        new ItemTouchHelper(itemTouchHelperCallback).attachToRecyclerView(noteList);
    }

    @Override
    public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction, int position) {
        if (viewHolder instanceof NoteRecyclerAdapter.ViewHolder) {

            // backup of removed item for undo purpose
            final int deletedIndex = viewHolder.getAdapterPosition();
            final Note deletedItem = notes.get(deletedIndex);

            // get the removed item name to display it in snack bar
            String name = deletedItem.getTitle();

            // remove the item from recycler view
            noteRecyclerAdapter.removeItem(deletedIndex);

            //delete note
            long noteId = deletedItem.getId();
            ArrayList<Attachment> currentAttachments = AttachManager.newInstance(this).getAttach(noteId, NoteContract.AttachEntry.ANY_TYPE);
            Log.d("attach", "sizeDel " + currentAttachments.size());

            for (int attachPos = 0; attachPos < currentAttachments.size(); attachPos++) {
                Attachment curAttach = currentAttachments.get(attachPos);
//                long curAttachId = curAttach.getId();
                String curPath = curAttach.getPath();

                File delFile = new File(curPath);

                //rename not delete
                File tempFile = new File(curPath + "(.temp)");

                boolean res = delFile.renameTo(tempFile);

                //boolean res = delFile.delete();
                if (res)
                    Log.d("attach", "file deleted");
                else
                    Log.d("attach", "file not deleted");

                AttachManager.newInstance(this).delete(curAttach);
            }

            //keeping for deleting later in Drive
            //NoteManager.newInstance(this).delete(currentNote);
            NoteManager.newInstance(this).disable(deletedItem);

            //empty view
            if (notes.size() == 0) {
                emptyView.setVisibility(View.VISIBLE);
            }

            // showing snack bar with Undo option
            Snackbar snackbar = Snackbar
                    .make(constraintLayout, name + " removed!", Snackbar.LENGTH_LONG);
            snackbar.setAction("UNDO", new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                    if (notes.size() == 0) {
                        emptyView.setVisibility(View.GONE);
                    }

                    // undo is selected, restore the deleted item
                    noteRecyclerAdapter.restoreItem(deletedItem, deletedIndex);

                    //add note again
                    NoteManager.newInstance(getApplication()).enable(deletedItem);
                    for (int attachPos = 0; attachPos < currentAttachments.size(); attachPos++) {
                        Attachment curAttach = currentAttachments.get(attachPos);
                        // long curAttachId = curAttach.getId();
                        String curPath = curAttach.getPath();

                        File delFile = new File(curPath + "(.temp)");
                        //rename not delete
                        File tempFile = new File(curPath);

                        boolean res = delFile.renameTo(tempFile);

                        //boolean res = delFile.delete();
                        if (res)
                            Log.d("attach", "file restored");
                        else
                            Log.d("attach", "file not restored");

                        AttachManager.newInstance(getApplication()).add(curAttach);
                    }
                }
            });
            snackbar.setActionTextColor(Color.YELLOW);
            snackbar.show();
        }
    }

    private void setUpGoogleAccount() {
        // Configure sign-in to request the user's ID, email address, and basic
        // profile. ID and basic profile are included in DEFAULT_SIGN_IN.
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestScopes(new Scope(DriveScopes.DRIVE_APPDATA), new Scope(DriveScopes.DRIVE_FILE))
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

    private void initializeView() {
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
        navigationView.setCheckedItem(R.id.nav_home);

        progressBar = findViewById(R.id.progress_icon);
        dimView = findViewById(R.id.dim);

        constraintLayout = findViewById(R.id.constraint_layout_main_activity);
    }

    private void onSignOut() {
        googleSignInClient.signOut();
        googleSignInButton.setVisibility(View.VISIBLE); //enable sign in button
        googleSignOutButton.setVisibility(View.GONE); //disable sign out button
        googleEmailText.setText(""); //clear user info
        googleNameText.setText(R.string.app_name);
        googleAvatarImg.setImageDrawable(getResources().getDrawable(R.mipmap.ic_avatar_round)); //default avatar
        loadedAvatar = null;
        googleServiceDrive = null;
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        if (!searchView.isIconified()) {
            searchView.setIconified(true);
        } else if (drawer.isDrawerOpen(GravityCompat.START)) {
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

    @Override
    protected void onNewIntent(Intent intent) {
        if (intent.getAction() != null && intent.getAction().equals(REQUEST_RESTART)) { //after syncing
            getInfo();
            noteRecyclerAdapter.notifyDataSetChanged();

            //enable screen interaction
            progressBar.setVisibility(View.GONE);
            dimView.setVisibility(View.GONE);
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
        }

        if (intent.getAction() != null && intent.getAction().equals(REQUEST_TAB)) {
            navigationView.setCheckedItem(R.id.nav_home);
        }

        super.onNewIntent(intent);
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
        String sortOrder;
        String sortType;

        if (sortByTime) {
            sortOrder = NoteContract.NoteEntry.COLUMN_NOTE_MODTIME;
        } else {
            sortOrder = NoteContract.NoteEntry.COLUMN_NOTE_TITLE;
        }

        if (sortDescending) {
            sortType = " DESC";
        } else {
            sortType = " ASC";
        }

        //get all note satisfied conditions above
        ArrayList<Note> newNotes = NoteManager.newInstance(this).getAllNotes(sortOrder + sortType);
        notes.clear();

        //note is not tagged as deleted
        for (Note note : newNotes) {
            if (note.getDeleted() == NoteContract.NoteEntry.NOT_DELETED) {
                notes.add(note);
            }
        }

        emptyView.setVisibility(View.GONE);
        if (notes.size() == 0) {
            emptyView.setVisibility(View.VISIBLE);
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);

        MenuItem search = menu.findItem(R.id.action_search);

        searchView = (SearchView) search.getActionView();
        search(searchView); //search note function

        return true;
    }

    private void search(SearchView searchView) {
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (newText.length() == 0) {
                    getInfo();
                    noteRecyclerAdapter.notifyDataSetChanged();
                } else {

                    newText = newText.toLowerCase();
                    ArrayList<Note> newList = new ArrayList<>();
                    for (Note note : notes) {
                        String title = note.getTitle().toLowerCase();
                        String content = note.getContent().toLowerCase();
                        String dateModified = NoteRecyclerAdapter.getDate(note.getDateModified(), "dd/MM/yyyy HH:mm");
                        //search by title, content and date modified
                        if (title.contains(newText) || content.contains(newText) || dateModified.contains(newText)) {
                            newList.add(note);
                        }
                    }
                    noteRecyclerAdapter.setFilter(newList);
                }
                return true;
            }
        });
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
        switch (id) {
            case R.id.action_settings:
                startActivity(new Intent(this, AboutActivity.class));
                return true;
            case R.id.action_sort:
                showSortPopup(id);
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void showSortPopup(int id) {
        View menuItemView = findViewById(id);
        PopupMenu popupMenu = new PopupMenu(this, menuItemView);
        popupMenu.inflate(R.menu.menu_sort);

        if (!sortByTime) {
            popupMenu.getMenu().findItem(R.id.sort_title).setChecked(true);
        }

        if (!sortDescending) {
            popupMenu.getMenu().findItem(R.id.sort_asc).setChecked(true);
        }

        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                item.setChecked(!item.isChecked());

                // Do other stuff
                switch (item.getItemId()) {
                    case R.id.sort_time:
                        if (!sortByTime) {
                            //sort ArrayList by date modified
                            Collections.sort(notes, new Comparator<Note>() {
                                @Override
                                public int compare(Note o1, Note o2) {
                                    return Long.compare(o1.getDateModified(), o2.getDateModified());
                                }
                            });

                            //check asc or desc
                            if (sortDescending)
                                Collections.reverse(notes);

                            sharedPreferences.edit().putBoolean(getResources().getString(R.string.isSortedByTime), true).apply();
                            sortByTime = true;
                        }
                        break;
                    case R.id.sort_title:
                        if (sortByTime) {
                            Collections.sort(notes, new Comparator<Note>() {
                                @Override
                                public int compare(Note o1, Note o2) {
                                    return o1.getTitle().toLowerCase().compareTo(o2.getTitle().toLowerCase());
                                }
                            });

                            //check asc or desc
                            if (sortDescending)
                                Collections.reverse(notes);

                            sharedPreferences.edit().putBoolean(getResources().getString(R.string.isSortedByTime), false).apply();
                            sortByTime = false;
                        }
                        break;
                    case R.id.sort_desc:
                        if (!sortDescending) {
                            Collections.reverse(notes);
                            sharedPreferences.edit().putBoolean(getResources().getString(R.string.isDescending), true).apply();
                            sortDescending = true;
                        }
                        break;
                    case R.id.sort_asc:
                        if (sortDescending) {
                            Collections.reverse(notes);
                            sharedPreferences.edit().putBoolean(getResources().getString(R.string.isDescending), false).apply();
                            sortDescending = false;
                        }
                        break;
                }
                noteRecyclerAdapter.notifyDataSetChanged();
                // Keep the popup menu open
                item.setShowAsAction(MenuItem.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW);
                item.setActionView(new View(getBaseContext()));
                item.setOnActionExpandListener(new MenuItem.OnActionExpandListener() {
                    @Override
                    public boolean onMenuItemActionExpand(MenuItem item) {
                        return false;
                    }

                    @Override
                    public boolean onMenuItemActionCollapse(MenuItem item) {
                        return false;
                    }
                });

                return false;
            }
        });
        popupMenu.show();

        MenuCompat.setGroupDividerEnabled(popupMenu.getMenu(), true);
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
                if (!isInternetAvailable())
                    new DownloadImgTask(this).execute(avatarUri.toString());
                else
                    googleAvatarImg.setImageDrawable(getResources().getDrawable(R.drawable.ic_user));
        }
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_home) {
            startActivity(new Intent(this, MainActivity.class));
        } else if (id == R.id.nav_about) {
            startActivity(new Intent(this, AboutActivity.class));
        } else if (id == R.id.nav_upload) {
            syncData(SyncManager.UP_DATA);
        } else if (id == R.id.nav_download) {
//            loadingIndicator();

            //download database into drive.
            syncData(SyncManager.DOWN_DATA);
        } else if (id == R.id.nav_sync) {
//            loadingIndicator();
            syncData(SyncManager.SYNC_DATA);
        }

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    private void loadingIndicator() {
        progressBar.setVisibility(View.VISIBLE);
        dimView.setVisibility(View.VISIBLE);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
    }

    private void syncData(String type) {
        if (googleServiceDrive == null) {
            Toast.makeText(this, "Please sign in with your Google account!", Toast.LENGTH_SHORT).show();
        } else {
//            if (!GoogleSignIn.hasPermissions(account, new Scope(DriveScopes.DRIVE_APPDATA), new Scope(DriveScopes.DRIVE_FILE))) {
//                GoogleSignIn.requestPermissions(this, 10, account, new Scope(DriveScopes.DRIVE_APPDATA), new Scope(DriveScopes.DRIVE_FILE));
//                Log.d(DRIVE_TAG, "Request Scope permission");
//            }
//            Log.d(DRIVE_TAG, "Has Scope permission");

            if (driveServiceHelper == null) {
                driveServiceHelper = new DriveServiceHelper(googleServiceDrive, this);
            }

            Thread checkInternetThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    if (!isInternetAvailable()) {
                        Log.d(DRIVE_TAG, "No internet connection");
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(getBaseContext(), "No internet connection", Toast.LENGTH_LONG).show();
                            }
                        });
                        return;
                    }
                    Log.d(DRIVE_TAG, "Internet connected");

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            loadingIndicator();

                            Toast.makeText(getBaseContext(), "Start syncing", Toast.LENGTH_LONG).show();
                        }
                    });
                }
            });
            checkInternetThread.start();

            Runnable syncTask = null;
            if (type.equals(SyncManager.DOWN_DATA)) {
                syncTask = new SyncDownTask(driveServiceHelper, getApplicationContext());
            } else if (type.equals(SyncManager.UP_DATA)) {
                syncTask = new SyncUpTask(driveServiceHelper, getApplicationContext());
            } else if (type.equals(SyncManager.SYNC_DATA)) {
                syncTask = new SyncTask(driveServiceHelper, getApplicationContext());
            }

            SyncManager.getSyncManager().runSync(syncTask);

        }
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

            return UtilHelper.getRoundedBitmap(bitmap);
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            // get a reference to the activity if it is still there
            MainActivity activity = activityReference.get();
            if (activity == null || activity.isFinishing()) return;

            ImageView avatar = activity.findViewById(R.id.google_avatar);

//            Bitmap rounded = UtilHelper.getRoundedBitmap(bitmap);
//            activity.loadedAvatar = rounded;
//
//            avatar.setImageBitmap(rounded);
            activity.loadedAvatar = bitmap;
            avatar.setImageBitmap(bitmap);
        }
    }
}