package com.chatlayoutexample;

import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings;
import android.util.Log;

import androidx.annotation.RequiresApi;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.Scope;
import com.google.api.services.drive.DriveScopes;

public class MyServiceChat extends Service {
    public MyServiceChat() {
    }

    private DriveServiceHelper mDrive;
    private String IDCartelle;


    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }


    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override

    // execution of service will start
    // on calling this method
    public int onStartCommand(Intent intent, int flags, int startId) {

        requestSignIn();

        Bundle extras = intent.getExtras();
        IDCartelle = (String) extras.get("IDCartelle");

        return START_STICKY;
    }

    private void requestSignIn() {

        GoogleSignInOptions signInOptions =
                new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .requestEmail()
                        .requestScopes(new Scope(DriveScopes.DRIVE_FILE))
                        .build();
        GoogleSignInClient client = GoogleSignIn.getClient(this, signInOptions);

    }


    @Override

    // execution of the service will
    // stop on calling this method
    public void onDestroy() {
        super.onDestroy();


        // stopping the process

    }
    @Override
    public void onTaskRemoved(Intent rootIntent){
        String[] IdFolders = IDCartelle.split(" ");

        mDrive.DeleteFile(IdFolders[0]);
        mDrive.DeleteFile(IdFolders[1]);


        super.onTaskRemoved(rootIntent);
    }

}