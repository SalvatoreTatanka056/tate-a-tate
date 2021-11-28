package com.chatlayoutexample;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;

/**
 * Created by AbhiAndroid
 */

public class SplashActivity extends Activity {

    Handler handler;
    ImageView view_splash;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.splashfile);


        //view_tutorial.startAnimation(AnimationUtils.loadAnimation(getBaseContext(), R.id.imageViewMain));

        view_splash = (ImageView) findViewById(R.id.logo_id);

        ObjectAnimator fadeOut = ObjectAnimator.ofFloat(view_splash, "alpha",  1f, .3f);
        fadeOut.setDuration(2000);
        ObjectAnimator fadeIn = ObjectAnimator.ofFloat(view_splash, "alpha", .3f, 1f);
        fadeIn.setDuration(2000);

        final AnimatorSet mAnimationSet = new AnimatorSet();

        mAnimationSet.play(fadeIn).after(fadeOut);

        mAnimationSet.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                mAnimationSet.start();
            }
        });
        mAnimationSet.start();
        //view_splash.startAnimation(AnimationUtils.loadAnimation(getBaseContext(),));

        handler=new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {

               Intent intent=new Intent(getApplicationContext(),ListaUtenti.class);
               startActivity(intent);



                finish();
            }
        },3000);

        //Intent intent = new Intent(SplashActivity.this, tateatateService.class);
        //startService(intent);



    }



}