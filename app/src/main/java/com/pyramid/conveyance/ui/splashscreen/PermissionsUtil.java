//package com.pyramid.conveyance.ui.splashscreen;
//
//import android.Manifest;
//import android.app.Activity;
//import android.content.Context;
//import android.content.Intent;
//import android.content.SharedPreferences;
//import android.content.pm.PackageManager;
//import android.net.Uri;
//import android.os.Build;
//import android.os.Handler;
//import android.provider.Settings;
//import android.util.Log;
//import android.widget.Toast;
//
//import androidx.annotation.NonNull;
//import androidx.core.app.ActivityCompat;
//import com.google.android.material.snackbar.Snackbar;
//import com.karumi.dexter.Dexter;
//import com.karumi.dexter.MultiplePermissionsReport;
//import com.karumi.dexter.PermissionToken;
//import com.karumi.dexter.listener.PermissionDeniedResponse;
//import com.karumi.dexter.listener.PermissionRequest;
//import com.karumi.dexter.listener.multi.MultiplePermissionsListener;
//import com.pyramid.conveyance.LocationApp;
//import com.pyramid.conveyance.R;
//import com.pyramid.conveyance.login.ui.conveyance.LoginActivity;
//import com.pyramid.conveyance.navigation.ui.conveyance.BottomNavigationActivity;
//
//import java.util.List;
//
//public class PermissionsUtil {
//    private static final int REQUEST_CODE = 364;
//    private final Activity mActivity;
//
//
//    private void requestPermissions(Context context, String[] permissions) {
//        Dexter.withContext(context)
//                .withPermissions(permissions)
//                .withListener(new MultiplePermissionsListener() {
//                    @Override
//                    public void onPermissionsChecked(MultiplePermissionsReport report) {
//                        Log.d("PermissionDebug", "onPermissionsChecked called");
//                        if (report.areAllPermissionsGranted()) {
//                            // All permissions are granted
//                            Log.d("PermissionDebug", "All permissions granted");
//                            SharedPreferences sharedPreferences = getSharedPreferences(LocationApp.APP_NAME, MODE_PRIVATE);
//                            String username = sharedPreferences.getString("username","");
//                            LocationApp.logs("TRIP", "username :" + username);
//                            if (username.trim().length() == 0) {
//                                Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
//                                startActivity(intent);
//                                finish();
//                            } else {
//                                Intent intent = new Intent(getApplicationContext(), BottomNavigationActivity.class);
//                                startActivity(intent);
//                                finish();
//                            }
//                        } else {
//                            // At least one permission is denied
//                            Log.d("PermissionDebug", "Some permissions denied");
//                            for (PermissionDeniedResponse response : report.getDeniedPermissionResponses()) {
//                                String deniedPermission = response.getPermissionName();
//                                deniedPermissions.append(deniedPermission).append("\n"); // Append denied permissions
//                            }
//
//                            // Show denied permissions using a Toast
//                            Toast.makeText(getApplicationContext(), "Permission(s) denied:\n" + deniedPermissions, Toast.LENGTH_LONG).show();
////                        Log.d("PermissionDebug", "Some permissions denied");
////
////                        boolean shouldRequestAgain = false;
////
////                        for (PermissionDeniedResponse response : report.getDeniedPermissionResponses()) {
////                            String deniedPermission = response.getPermissionName();
////                            shouldRequestAgain = true; // Set to true if any permission is denied
////                        }
////
////                        if (shouldRequestAgain) {
////                            // Request permissions again after a delay
////                            new Handler().postDelayed(new Runnable() {
////                                @Override
////                                public void run() {
////                                    requestPermissionsAgain(permissions);
////                                }
////                            }, 2000);
////                        }
//
//                            new Handler().postDelayed(new Runnable() {
//                                @Override
//                                public void run() {
//                                    Toast.makeText(getApplicationContext(), "Restart App after manually giving denied permissions", Toast.LENGTH_LONG).show();
//                                    finish();
//                                }
//                            }, 5000);
//                        }
//                    }
//
//                    @Override
//                    public void onPermissionRationaleShouldBeShown(List<PermissionRequest> permissions, PermissionToken token) {
//                        Log.d("PermissionDebug", "onPermissionRationaleShouldBeShown called");
//                        // Show permission rationale if needed
//                        token.continuePermissionRequest();
//                    }
//
//                })
//                .check();
//    }
//
//}
