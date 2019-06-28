package com.blacksun.quicknote.activities;


import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.blacksun.quicknote.R;
import com.blacksun.quicknote.adapters.NoteRecyclerAdapter;
import com.blacksun.quicknote.data.NoteManager;
import com.blacksun.quicknote.models.Note;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    ArrayList<Note> notes;
    RecyclerView noteList;
    View emptyView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
                Intent intent = new Intent(MainActivity.this, DetailActivity.class);
                startActivity(intent);
            }
        });

        notes = new ArrayList<>();
        notes.add(new Note("Go home and travel to another planet need longerrrrrrrrrrrrrrr", "Now get up", 10, 10, 10));
        notes.add(new Note("Go home", "Now get up", 10, 10, 10));

        getInfo();

        noteList = (RecyclerView) findViewById(R.id.note_list);

        emptyView = findViewById(R.id.empty_view);
        if (notes.size() == 0) {
            emptyView.setVisibility(View.VISIBLE);
        }

        NoteRecyclerAdapter adapter = new NoteRecyclerAdapter(notes);

//        Log.d("TestRecyclerMain", "Not good 2");
        noteList.setHasFixedSize(true);
        noteList.setLayoutManager(new LinearLayoutManager(this));
        noteList.setAdapter(adapter);

    }

    @Override
    protected void onStart() {
        super.onStart();
        //update after create new note or delete
        getInfo();
        NoteRecyclerAdapter adapter = new NoteRecyclerAdapter(notes);

        noteList.setHasFixedSize(true);
        noteList.setLayoutManager(new LinearLayoutManager(this));
        noteList.setAdapter(adapter);
    }

    private void getInfo() {
        notes = NoteManager.newInstance(this).getAllNotes();
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
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
