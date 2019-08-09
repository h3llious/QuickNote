package com.blacksun.quicknote.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;

import com.blacksun.quicknote.R;

import mehdi.sakout.aboutpage.AboutPage;
import mehdi.sakout.aboutpage.Element;

import static com.blacksun.quicknote.activities.MainActivity.REQUEST_TAB;

public class AboutActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_about);
        setTitle("About");

        if (getSupportActionBar() != null)
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        Element versionElement = new Element();
        versionElement.setTitle("Version " + getResources().getString(R.string.version));

        View aboutPage = new AboutPage(this)
                .isRTL(false)
                .setImage(R.mipmap.ic_launcher_round)
                .addItem(versionElement)
                .addGroup("Connect with us")
                .addEmail("anhnhatbuiit@gmail.com")
                .addFacebook("h3llious")
                .addPlayStore("com.blacksun.quicknote")
                .addGitHub("H3llious")
                .setDescription("QuickNote\nby Hellious")
                .create();

        setContentView(aboutPage);
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setAction(REQUEST_TAB);
        startActivity(intent);
        finish();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
