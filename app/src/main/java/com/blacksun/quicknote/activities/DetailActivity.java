package com.blacksun.quicknote.activities;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import com.blacksun.quicknote.R;
import com.blacksun.quicknote.data.NoteManager;
import com.blacksun.quicknote.models.Note;
import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

public class DetailActivity extends AppCompatActivity {

    EditText detailTitle, detailContent;
    CollapsingToolbarLayout collapsingToolbar;
    Note currentNote = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        collapsingToolbar = findViewById(R.id.toolbar_layout);

        detailContent = findViewById(R.id.detail_content);
        detailTitle = findViewById(R.id.detail_title);

        String title = null;
        if (getIntent() != null && getIntent().getExtras() != null) {
            Bundle bundle = getIntent().getExtras();
            if (bundle.getString("title") != null) {
                detailTitle.setText(bundle.getString("title"));
                detailContent.setText(bundle.getString("content"));

                //test collapsing toolbar
                collapsingToolbar.setTitle(bundle.getString("title"));

                title = bundle.getString("title");
                String content = bundle.getString("content");
                long dateCreated = bundle.getLong("dateCreated");
                long dateModified = bundle.getLong("dateModified");
                long id = bundle.getLong("noteID");

                currentNote = new Note(title, content, id, dateCreated, dateModified);


            }
        } else {
            title = "0";
            collapsingToolbar.setTitle("New note");
        }


        collapsingToolbar.setCollapsedTitleTextColor(ContextCompat.getColor(this, R.color.white));
        collapsingToolbar.setExpandedTitleColor(ContextCompat.getColor(this, R.color.transparent));

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);


        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                boolean saveCheck = saveNote();

                if (currentNote == null) {
                    if (saveCheck)
                        Snackbar.make(view, "Save successfully", Snackbar.LENGTH_LONG)
                                .setAction("Action", null).show();
                    else
                        Snackbar.make(view, "Encounter error(s) when saving", Snackbar.LENGTH_LONG)
                                .setAction("Action", null).show();
                }
                else {
                    if (saveCheck)
                        Snackbar.make(view, "Update successfully", Snackbar.LENGTH_LONG)
                                .setAction("Action", null).show();
                    else
                        Snackbar.make(view, "Encounter error(s) when updating", Snackbar.LENGTH_LONG)
                                .setAction("Action", null).show();
                }
                finish();
            }
        });
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
            NoteManager.newInstance(this).create(note);
        }
        else
        {
            currentNote.setTitle(title);
            currentNote.setContent(content);
            NoteManager.newInstance(this).update(currentNote);
        }
        return true;

    }

}
