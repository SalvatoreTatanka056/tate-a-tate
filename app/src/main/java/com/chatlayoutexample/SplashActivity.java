package com.chatlayoutexample;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {

    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable launchRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.splashfile);

        launchRunnable = () -> {
            if (!isFinishing()) {
                Intent intent = new Intent(SplashActivity.this, ChatActivity.class);
                startActivity(intent);
                finish();
            }
        };

        handler.postDelayed(launchRunnable, 2000);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (launchRunnable != null) {
            handler.removeCallbacks(launchRunnable);
        }
    }
}
