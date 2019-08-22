package com.blacksun.quicknote.services;

import android.app.IntentService;
import android.content.Intent;

import androidx.annotation.Nullable;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;

public class SyncService extends IntentService {
    GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(getApplicationContext());

    public SyncService(String name) {
        super(name);
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {

    }
}
