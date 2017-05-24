/**
 * Splash Activity
 *
 * @desc - opening page that has the PayOut logo
 * @authors - Tim Mahler
 * @version - 1.0.0
 */

package com.m87.sam.ui.activity;

import android.os.Bundle;


import android.app.Activity;
import android.content.Intent;
import android.os.Handler;

import com.m87.sam.BuildConfig;
import com.m87.sam.R;

public class SplashActivity extends Activity {

    // Splash screen timer
    private static int SPLASH_TIME_OUT = 10;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        new Handler().postDelayed(new Runnable() {

            /*
             * Showing splash screen with a timer. This will be useful when you
             * want to show case your app logo / company
             */

            @Override
            public void run() {
                // This method will be executed once the timer is over
                // Start your app main activity
                Intent i = new Intent(SplashActivity.this, HomeActivity.class);
                startActivity(i);

                // close this activity
                finish();
            }
        }, SPLASH_TIME_OUT);
    }

}
