package com.pyramid.conveyance.ui.splashscreen;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowInsets;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;
import com.pyramid.conveyance.ui.navigation.BottomNavigationActivity;
import com.pyramid.conveyance.LocationApp;
import com.pyramid.conveyance.databinding.ActivitySplashScreenBinding;
import com.pyramid.conveyance.ui.login.LoginActivity;

import java.util.List;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class SplashScreenActivity extends AppCompatActivity {
    /**
     * Whether or not the system UI should be auto-hidden after
     * {@link #AUTO_HIDE_DELAY_MILLIS} milliseconds.
     */
    private static final boolean AUTO_HIDE = true;

    /**
     * If {@link #AUTO_HIDE} is set, the number of milliseconds to wait after
     * user interaction before hiding the system UI.
     */
    private static final int AUTO_HIDE_DELAY_MILLIS = 2000;

    /**
     * Some older devices needs a small delay between UI widget updates
     * and a change of the status and navigation bar.
     */
    private static final int UI_ANIMATION_DELAY = 3000;
    private final Handler mHideHandler = new Handler(Looper.myLooper());
    private View mContentView;
    private final Runnable mHidePart2Runnable = new Runnable() {
        @SuppressLint("InlinedApi")
        @Override
        public void run() {
            String[] permissions;
            if (android.os.Build.VERSION.SDK_INT <= Build.VERSION_CODES.R  || android.os.Build.VERSION.SDK_INT == Build.VERSION_CODES.S_V2 || android.os.Build.VERSION.SDK_INT == Build.VERSION_CODES.S) {
                permissions =
                        new String[]{
                                Manifest.permission.CAMERA,
                                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION,
                                Manifest.permission.ACCESS_NOTIFICATION_POLICY,
                                Manifest.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS};
            } else {
                permissions =
                        new String[]{
                                Manifest.permission.CAMERA,
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION,
                                Manifest.permission.POST_NOTIFICATIONS,
                                Manifest.permission.READ_MEDIA_IMAGES,
                        Manifest.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS};
            }
            final StringBuilder deniedPermissions = new StringBuilder();
            Dexter.withContext(getApplicationContext())
                    .withPermissions(permissions)
                    .withListener(new MultiplePermissionsListener() {
                @Override
                public void onPermissionsChecked(MultiplePermissionsReport report) {
                    Log.d("PermissionDebug", "onPermissionsChecked called");
                    if (report.areAllPermissionsGranted()) {
                        // All permissions are granted
                        Log.d("PermissionDebug", "All permissions granted");
                        SharedPreferences sharedPreferences = getSharedPreferences(LocationApp.APP_NAME, MODE_PRIVATE);
                        String username = sharedPreferences.getString("username","");
                        LocationApp.logs("TRIP", "username :" + username);
                        if (username.trim().length() == 0) {
                            Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
                            startActivity(intent);
                            finish();
                        } else {
                            Intent intent = new Intent(getApplicationContext(), BottomNavigationActivity.class);
                            startActivity(intent);
                            finish();
                        }
                    } else {
                        // At least one permission is denied
                        Log.d("PermissionDebug", "Some permissions denied");
                        for (PermissionDeniedResponse response : report.getDeniedPermissionResponses()) {
                            String deniedPermission = response.getPermissionName();
                            deniedPermissions.append(deniedPermission).append("\n"); // Append denied permissions
                        }

                        // Show denied permissions using a Toast
                        Toast.makeText(getApplicationContext(), "Permission(s) denied:\n" + deniedPermissions, Toast.LENGTH_LONG).show();
//                        Log.d("PermissionDebug", "Some permissions denied");
//
//                        boolean shouldRequestAgain = false;
//
//                        for (PermissionDeniedResponse response : report.getDeniedPermissionResponses()) {
//                            String deniedPermission = response.getPermissionName();
//                            shouldRequestAgain = true; // Set to true if any permission is denied
//                        }
//
//                        if (shouldRequestAgain) {
//                            // Request permissions again after a delay
//                            new Handler().postDelayed(new Runnable() {
//                                @Override
//                                public void run() {
//                                    requestPermissionsAgain(permissions);
//                                }
//                            }, 2000);
//                        }

                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(getApplicationContext(), "Restart App after manually giving denied permissions", Toast.LENGTH_LONG).show();
                                finish();
                            }
                        }, 5000);
                    }
                }

                @Override
                public void onPermissionRationaleShouldBeShown(List<PermissionRequest> permissions, PermissionToken token) {
                    Log.d("PermissionDebug", "onPermissionRationaleShouldBeShown called");
                    // Show permission rationale if needed
                    token.continuePermissionRequest();
                }

            })
                    .check();

        }
    };

    private final Runnable mShowPart2Runnable = () -> {
        // Delayed display of UI elements
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.show();
        }
    };
    private boolean mVisible;
    private final Runnable mHideRunnable = new Runnable() {
        @Override
        public void run() {
            hide();
        }
    };
    /**
     * Touch listener to use for in-layout UI controls to delay hiding the
     * system UI. This is to prevent the jarring behavior of controls going away
     * while interacting with activity UI.
     */
    private final View.OnTouchListener mDelayHideTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            switch (motionEvent.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    if (AUTO_HIDE) {
                        delayedHide(AUTO_HIDE_DELAY_MILLIS);
                    }
                    break;
                case MotionEvent.ACTION_UP:
                    view.performClick();
                    break;
                default:
                    break;
            }
            return false;
        }
    };
    private ActivitySplashScreenBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivitySplashScreenBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        mVisible = true;

    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        // Trigger the initial hide() shortly after the activity has been
        // created, to briefly hint to the user that UI controls
        // are available.
        delayedHide(100);
    }

    private void hide() {
        // Hide UI first
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }
        mVisible = false;

        // Schedule a runnable to remove the status and navigation bar after a delay
        mHideHandler.removeCallbacks(mShowPart2Runnable);
        mHideHandler.postDelayed(mHidePart2Runnable, UI_ANIMATION_DELAY);
    }

    private void show() {
        // Show the system bar
        if (Build.VERSION.SDK_INT >= 30) {
            mContentView.getWindowInsetsController().show(
                    WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
        } else {
            mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        }
        mVisible = true;

        // Schedule a runnable to display UI elements after a delay
        mHideHandler.removeCallbacks(mHidePart2Runnable);
        mHideHandler.postDelayed(mShowPart2Runnable, UI_ANIMATION_DELAY);
    }

    /**
     * Schedules a call to hide() in delay milliseconds, canceling any
     * previously scheduled calls.
     */
    private void delayedHide(int delayMillis) {
        mHideHandler.removeCallbacks(mHideRunnable);
        mHideHandler.postDelayed(mHideRunnable, delayMillis);
    }
}