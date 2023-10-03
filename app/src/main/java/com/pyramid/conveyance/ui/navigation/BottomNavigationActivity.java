package com.pyramid.conveyance.ui.navigation;

import static com.pyramid.conveyance.LocationApp.dialog;
import static com.pyramid.conveyance.LocationApp.employeeProfileLiveData;
import static com.pyramid.conveyance.LocationApp.getUserName;
import static com.pyramid.conveyance.LocationApp.progressDialog;
import static java.io.File.createTempFile;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.PowerManager;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.Gravity;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.snackbar.Snackbar;
import com.pyramid.conveyance.respository.executers.AppExecutors;
import com.pyramid.conveyance.services.MyService;
import com.pyramid.conveyance.ui.customcomponent.MyProgressDialog;
import com.pyramid.conveyance.utility.TrackerUtility;
import com.pyramid.conveyance.LocationApp;
import com.pyramid.conveyance.R;
import com.pyramid.conveyance.api.ApiHandler;
import com.pyramid.conveyance.api.SearchRideFilter;
import com.pyramid.conveyance.api.SearchRideResponse;
import com.pyramid.conveyance.api.response.EmployeeProfile;
import com.pyramid.conveyance.api.response.Ride;
import com.pyramid.conveyance.respository.databaseclient.DatabaseClient;
import com.pyramid.conveyance.ui.login.LoginActivity;
import com.pyramid.conveyance.ui.recordride.RecordRideActivity;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class BottomNavigationActivity extends AppCompatActivity {

    private static final float END_SCALE = 0.85f;
    private static final String IMAGE_DIRECTORY_NAME = "images";
    private AppBarConfiguration appBarConfiguration;
    private NavController navController;
    private DrawerLayout drawer;
    private NavigationView navigationView;
    private BottomNavigationView bottomNavView;
    private LinearLayout contentView;
    private Button logoutBtn;
    private TextView employeeIdTextView;
    private TextView fatherNameTextView;
    private TextView uidTextView;
    private TextView fullNameTextView;
    private TextView vehicleNumberTextView;
    private TextView vehicleNameTextView;
    private TextView dateOfBirthTextView;
    private TextView addressTextView;
    private TextView branchTextView;
    private ShapeableImageView profileImageView;
    private ImageView logoutImageview;
    private final int TAKE_PICTURE = 1;
    private final int SELECT_PICTURE = 2;
    private boolean isCamera = false;
    private  Uri fileUri;
    private File profileImageFile = null;
    boolean isRideDisabled = false;

    @Override
    public Intent registerReceiver(@Nullable BroadcastReceiver receiver, IntentFilter filter) {
        return null;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bottom_navigation);

        setupViews();
        initializeData();
        navigateToSelectedFragment();
    }

    private void setupViews() {
        initToolbar();
        initNavigation();
    }

    private void initializeData() {
        String username = LocationApp.getUserName(this);
        String deviceId = TrackerUtility.getDeviceId(getApplicationContext());

        if (!TrackerUtility.checkConnection(getApplicationContext())) {
            runOnUiThread(() -> {
                Toast.makeText(getApplicationContext(), "Please check your network connection", Toast.LENGTH_LONG).show();
            });
        } else {
            // Perform network operations on a background thread
            AsyncTask.execute(() -> {
                getEmployeeProfile(username, deviceId);
            });
        }
    }

    private void navigateToSelectedFragment() {
        if (getIntent() != null && getIntent().getExtras() != null) {
            int navigationId = getIntent().getExtras().getInt("navigationId", R.id.navigation_home);
            navController.navigate(navigationId);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Check and request battery optimization exemption only if needed
        requestBatteryOptimizationExemptionIfNeeded();
    }

    private void requestBatteryOptimizationExemptionIfNeeded() {
        String packageName = getPackageName();
        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);

        if (pm != null && !pm.isIgnoringBatteryOptimizations(packageName)) {
            Intent intentBackground = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
            intentBackground.setData(Uri.parse("package:" + packageName));
            startActivity(intentBackground);
        }
//        Intent intent = new Intent();
//        intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
//        Uri uri = Uri.fromParts("package", getPackageName(), null);
//        intent.setData(uri);
//        startActivity(intent);
    }

    private void initToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
    }


    private void initNavigation() {
        drawer = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.navigation_view);
        bottomNavView = findViewById(R.id.nav_view);
        contentView = findViewById(R.id.content_view);

        SharedPreferences sharedPreferences = this.getSharedPreferences(LocationApp.APP_NAME, MODE_PRIVATE);
        isRideDisabled = sharedPreferences.getBoolean("rideDisabled", false);

        int bottomNavMenuResId = isRideDisabled ? R.menu.bottom_nav_menu_only_attendance : R.menu.bottom_nav_menu;
        int[] topLevelDestinations = isRideDisabled
                ? new int[]{R.id.navigation_home, R.id.navigation_attendance}
                : new int[]{R.id.navigation_home, R.id.navigation_attendance, R.id.navigation_statistics, R.id.navigation_ridedetails};

        bottomNavView.inflateMenu(bottomNavMenuResId);

        appBarConfiguration = new AppBarConfiguration.Builder(topLevelDestinations)
                .setDrawerLayout(drawer)
                .build();

        navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_bottom_navigation);
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);
        NavigationUI.setupWithNavController(bottomNavView, navController);

        animateNavigationDrawer();

        // Set the background color using app resources
        navigationView.setBackgroundColor(ContextCompat.getColor(this, R.color.white));

        // Setup the logout button
        setupLogoutButton();
    }

    private void setupLogoutButton() {
        logoutImageview = findViewById(R.id.logoutImage);
        logoutImageview.setOnClickListener(view -> {
            showLogoutAlertDialog();
        });
    }


    private void animateNavigationDrawer() {
        drawer.addDrawerListener(new DrawerLayout.SimpleDrawerListener() {
            @Override
            public void onDrawerSlide(View drawerView, float slideOffset) {
                runOnUiThread(() -> {
                    setupClickListeners();
                    updateUIWithEmployeeProfile();

                    // Scale the View based on current slide offset
                    final float diffScaledOffset = slideOffset * (1 - END_SCALE);
                    final float offsetScale = 1 - diffScaledOffset;
                    contentView.setScaleX(offsetScale);
                    contentView.setScaleY(offsetScale);

                    // Translate the View, accounting for the scaled width
                    final float xOffset = drawerView.getWidth() * slideOffset;
                    final float xOffsetDiff = contentView.getWidth() * diffScaledOffset / 2;
                    final float xTranslation = xOffset - xOffsetDiff;
                    contentView.setTranslationX(xTranslation);
                });
            }
        });
    }

    private void setupClickListeners() {
        logoutBtn = findViewById(R.id.logoutButton);
        if (logoutBtn != null) {
            logoutBtn.setOnClickListener(view -> {
                showLogoutAlertDialog();
            });
        }

        profileImageView = findViewById(R.id.profile_image);
        if (profileImageView != null) {
            profileImageView.setOnClickListener(view -> {
                selectImage();
            });
        }
    }

    private void updateUIWithEmployeeProfile() {
        employeeIdTextView = findViewById(R.id.employee_id);
        fatherNameTextView = findViewById(R.id.father_name);
        uidTextView = findViewById(R.id.uid);;
        fullNameTextView = findViewById(R.id.full_name);;
        vehicleNumberTextView = findViewById(R.id.vehicle_number);;
        vehicleNameTextView = findViewById(R.id.vehicle_name);;
        dateOfBirthTextView = findViewById(R.id.date_of_birth);;
        addressTextView = findViewById(R.id.address);;
        branchTextView = findViewById(R.id.branch_name);
        profileImageView = findViewById(R.id.profile_image);
        if (employeeProfileLiveData.getValue() != null) {
            // Update UI elements with LiveData values
            EmployeeProfile employeeProfile = employeeProfileLiveData.getValue();
            fullNameTextView.setText(employeeProfile.getFullName());
            employeeIdTextView.setText(employeeProfile.getEmployeeCode());
            fatherNameTextView.setText(employeeProfile.getFatherName());
            dateOfBirthTextView.setText(employeeProfile.getDob());
            uidTextView.setText(employeeProfile.getUid());
            addressTextView.setText(employeeProfile.getFullAddress());
            branchTextView.setText(employeeProfile.getBranch());
            vehicleNameTextView.setText(employeeProfile.getVehicleName());
            vehicleNumberTextView.setText(employeeProfile.getVehicleNo());

            Glide.with(getBaseContext()).load(employeeProfile.getProfile()).into(profileImageView);
        }
    }



    private void showLogoutAlertDialog() {
        runOnUiThread(() -> {
            MaterialAlertDialogBuilder alertDialogBuilder = new MaterialAlertDialogBuilder(BottomNavigationActivity.this);
            alertDialogBuilder.setCancelable(true);
            alertDialogBuilder.setTitle("Alert");
            alertDialogBuilder.setMessage("Do you want to logout?");
            alertDialogBuilder.setPositiveButton("OK", (dialogInterface, i) -> {
                if (isRideDisabled) {
                    logout();
                    Intent intent = new Intent(BottomNavigationActivity.this, LoginActivity.class);
                    startActivity(intent);
                    finish();
                } else {
                    fetchTodaysRides();
                }
            });
            alertDialogBuilder.setNegativeButton("CANCEL", (dialogInterface, i) -> dialogInterface.dismiss());
            alertDialogBuilder.show();
        });
    }

    private void logout() {
        SharedPreferences sharedPreferences = getSharedPreferences(LocationApp.APP_NAME, MODE_PRIVATE);
        sharedPreferences.edit().clear().commit();

        // Execute the database clearing operation on a background thread
        AppExecutors.getInstance().getDiskIO().execute(() -> {
            DatabaseClient.getInstance(this).getTripDatabase().clearAllTables();
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        // getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        LocationApp.dismissLoader();
        if (progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_bottom_navigation);
        return NavigationUI.navigateUp(navController, appBarConfiguration)
                || super.onSupportNavigateUp();
    }

    @Override
    public void onBackPressed() {
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    void getEmployeeProfile(String userName, String deviceId) {
        Call<EmployeeProfile> employeeProfileCall = ApiHandler.getClient().getEmployeeProfile(userName, deviceId);
        employeeProfileCall.enqueue(new Callback<EmployeeProfile>() {
            @Override
            public void onResponse(Call<EmployeeProfile> call, Response<EmployeeProfile> response) {
                if (response.isSuccessful()) {
                    EmployeeProfile profile = response.body();
                    if (profile != null) {
                        employeeProfileLiveData.setValue(profile);

                        runOnUiThread(() -> {
                            setupProfileImageView();
                            updateSharedPreferences(profile);
                        });
                    }
                }
            }

            @Override
            public void onFailure(Call<EmployeeProfile> call, Throwable t) {
                // Handle failure, if needed
            }
        });
    }

    private void setupProfileImageView() {
        if (profileImageFile != null && profileImageView != null) {
            Glide.with(BottomNavigationActivity.this).load(employeeProfileLiveData.getValue().getProfile()).into(profileImageView);
        }
    }

    private void updateSharedPreferences(EmployeeProfile profile) {
        SharedPreferences sharedPreferences = getSharedPreferences(LocationApp.APP_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("employeeFullName", profile.getFullName());
        editor.putString("employeeCode", profile.getEmployeeCode());

        if (profile.isTodaysClockIn()) {
            editor.putString(LocationApp.CLOCK_IN, TrackerUtility.getDateString(new Date()));
        } else {
            editor.putString(LocationApp.CLOCK_IN, "");
        }

        if (profile.isTodaysClockOut()) {
            editor.putString(LocationApp.CLOCK_OUT, TrackerUtility.getDateString(new Date()));
        } else {
            editor.putString(LocationApp.CLOCK_OUT, "");
        }
        editor.apply();
    }



    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent imageReturnedIntent) {
        super.onActivityResult(requestCode, resultCode, imageReturnedIntent);
        switch(requestCode) {
            case TAKE_PICTURE:
                if (resultCode == RESULT_OK) {
                    if (profileImageFile != null) {
                        if (!isFinishing()) {
                            LocationApp.showLoader(this);
                        }
                        AppExecutors.getInstance().getNetworkIO().execute(()->{
                            RequestBody filePart = RequestBody.create(MediaType.parse("image/jpeg"), profileImageFile);
                            Map<String, RequestBody> param = new HashMap<>();
                            param.put("file\"; filename=\"profile.png\" ", filePart);
                            Call<Void> uploadProfileCall = ApiHandler.getClient().uploadProfile(LocationApp.getUserName(this), LocationApp.DEVICE_ID, param);
                            uploadProfileCall.enqueue(new Callback<Void>() {
                                @Override
                                public void onResponse(Call<Void> call, Response<Void> response) {
                                    if (response.code() == 200 || response.code() == 201) {
                                        String username = getUserName(BottomNavigationActivity.this);
                                        String deviceId = TrackerUtility.getDeviceId(BottomNavigationActivity.this);
                                        getEmployeeProfile(username, deviceId);
                                        //Glide.with(getApplicationContext()).load(profileImageFile).into(profileImageView);
                                        new Handler().postDelayed(()-> {
                                            Toast.makeText(BottomNavigationActivity.this, "Profile Updated Successfully.", Toast.LENGTH_LONG).show();
                                        }, 2000);
                                    } else {
                                        LocationApp.logs("profileUpload", "response code :" + response.code());
                                        LocationApp.logs("profile upload failed due to response code :" + response.code());
                                        Toast.makeText(BottomNavigationActivity.this, "Upload Profile failed.", Toast.LENGTH_LONG).show();
                                    }
                                    LocationApp.dismissLoader();
                                }

                                @Override
                                public void onFailure(Call<Void> call, Throwable t) {
                                    LocationApp.logs("profileUpload", "onFailure :" + t.getMessage());
                                    LocationApp.logs("profile upload failed due onFailure : " + t.getMessage());
                                    LocationApp.logs(t);
                                    Toast.makeText(BottomNavigationActivity.this, "Upload Profile failed.", Toast.LENGTH_LONG).show();
                                    LocationApp.dismissLoader();
                                }

                            });
                        });

                    }
//                   profileImageView.setImageURI(selectedImage);
                }

                break;
            case SELECT_PICTURE:
                if (resultCode == RESULT_OK) {
                    byte[] imageData = null;
                    if (imageReturnedIntent != null) {
                        Uri selectedImage = imageReturnedIntent.getData();
                        String picturePath = getPicturePath(selectedImage);
                        profileImageFile = new File(picturePath);
                        profileImageFile.getAbsoluteFile();
                        try {
                            FileInputStream inputStream = new FileInputStream(profileImageFile);
                            int available = inputStream.available();
                            imageData = new byte[available];
                            inputStream.read(imageData);
                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }

                    //Glide.with(getApplicationContext()).load(selectedImage).into(profileImageView);
                    if (profileImageFile != null) {
                        byte[] finalImageData = imageData;
                        LocationApp.showLoader(this);
                        AppExecutors.getInstance().getNetworkIO().execute(()->{
                            RequestBody filePart = RequestBody.create(MediaType.parse("image/jpeg"), finalImageData);
                            Map<String, RequestBody> param = new HashMap<>();
                            param.put("file\"; filename=\"profile.png\" ", filePart);
                            Call<Void> uploadProfileCall = ApiHandler.getClient().uploadProfile(LocationApp.getUserName(this), LocationApp.DEVICE_ID, param);
                            uploadProfileCall.enqueue(new Callback<Void>() {
                                @Override
                                public void onResponse(Call<Void> call, Response<Void> response) {
                                    if (response.code() == 200 || response.code() == 201) {
                                        Glide.with(getApplicationContext()).load(profileImageFile).into(profileImageView);
                                        Toast.makeText(BottomNavigationActivity.this, "Profile Updated Successfully.", Toast.LENGTH_LONG).show();
                                    } else {
                                        LocationApp.logs("profileUpload", "response code :" + response.code());
                                        LocationApp.logs("profile upload failed due to response code :" + response.code());
                                        Toast.makeText(BottomNavigationActivity.this, "Upload Profile failed.", Toast.LENGTH_LONG).show();
                                    }
                                    LocationApp.dismissLoader();
                                }

                                @Override
                                public void onFailure(Call<Void> call, Throwable t) {
                                    LocationApp.logs("profileUpload", "onFailure :" + t.getMessage());
                                    LocationApp.logs("profile upload failed due onFailure : " + t.getMessage());
                                    LocationApp.logs(t);
                                    Toast.makeText(BottomNavigationActivity.this, "Upload Profile failed.", Toast.LENGTH_LONG).show();
                                    LocationApp.dismissLoader();
                                }
                            });
                        });
                    }
                }
                break;
        }
    }

    private String getPicturePath(Uri selectedImage) {
        String[] filePath = { MediaStore.Images.Media.DATA };
        Cursor c = null;
        try {
            c = getApplicationContext().getContentResolver().query(
                    selectedImage, filePath, null, null, null);
            if (c != null && c.moveToFirst()) {
                int columnIndex = c.getColumnIndex(filePath[0]);
                return c.getString(columnIndex);
            }
        } finally {
            if (c != null) {
                c.close();
            }
        }
        return null;
    }


    private void selectImage() {
        isCamera = true;
        final CharSequence[] items = { "Take Photo", "Choose from Library",
                "Cancel" };

        TextView title = new TextView(this);
        title.setText("Add Photo!");
        title.setBackgroundColor(Color.BLACK);
        title.setPadding(10, 15, 15, 10);
        title.setGravity(Gravity.CENTER);
        title.setTextColor(Color.WHITE);
        title.setTextSize(22);

        AlertDialog.Builder builder = new AlertDialog.Builder(
                BottomNavigationActivity.this);

        builder.setCustomTitle(title);

        // builder.setTitle("Add Photo!");
        builder.setItems(items, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int item) {
                if (items[item].equals("Take Photo")) {
                    if (checkPermissions()) {
                        takePhotoFromCamera();
                    } else {
                        requestPermissions();
                    }
                } else if (items[item].equals("Choose from Library")) {
                    if (checkPermissions()) {
                        selectImageFromGallery();
                    } else {
                        requestPermissions();
                    }

                } else if (items[item].equals("Cancel")) {
                    dialog.dismiss();
                }
            }
        });
        builder.show();
    }

    private void selectImageFromGallery() {
        Intent pickPhoto = new Intent(Intent.ACTION_PICK,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(pickPhoto , SELECT_PICTURE);
    }

    private void takePhotoFromCamera() {
        profileImageFile = createImageFile();
        // Continue only if the File was successfully created
        Uri photoURI = null;
        if (profileImageFile != null) {
            photoURI = FileProvider.getUriForFile(
                   getApplicationContext(),
                    "com.pyramid.conveyance.fileprovider",
                    profileImageFile
            );
        }

        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT,  photoURI);
        startActivityForResult(cameraIntent, TAKE_PICTURE);
    }


    private File createImageFile() {
        // Constants
        final String IMAGE_FILE_PREFIX = "JPEG_";
        final String IMAGE_FILE_EXTENSION = ".jpg";

        // Create an image file name with a timestamp
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = IMAGE_FILE_PREFIX + timeStamp;

        // Define storage directories
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        if (storageDir == null) {
            LocationApp.logs("TRIP", "Storage directory is null");
            return null;
        }

        File rideRecordDir = new File(storageDir, "RideRecord");
        if (!rideRecordDir.exists() && !rideRecordDir.mkdirs()) {
            LocationApp.logs("TRIP", "Failed to create directory: " + rideRecordDir.getAbsolutePath());

            // Fallback to the external public directory
            File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "RideRecord");
            if (!mediaStorageDir.exists() && !mediaStorageDir.mkdirs()) {
                LocationApp.logs("TRIP", "Failed to create directory: " + mediaStorageDir.getAbsolutePath());
                return null;
            }

            rideRecordDir = mediaStorageDir;
        }

        File image = new File(rideRecordDir, imageFileName + IMAGE_FILE_EXTENSION);

        try {
            if (image.createNewFile()) {
                return image;
            } else {
                LocationApp.logs("TRIP", "Failed to create image file");
            }
        } catch (IOException e) {
            e.printStackTrace();
            LocationApp.logs("TRIP", "Error creating image file: " + e.getMessage());
        }

        return null;
    }

    /**
     * Return the current state of the permissions needed.
     */
    private boolean checkPermissions() {
        int permissionState = ActivityCompat.checkSelfPermission(this,
                Manifest.permission.CAMERA);
        int permissionStateWriteFile = ActivityCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE);
        int permissionStateReadFile = ActivityCompat.checkSelfPermission(this,
                Manifest.permission.READ_EXTERNAL_STORAGE);

        if (android.os.Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            return ((permissionState == PackageManager.PERMISSION_GRANTED) &&
                    (permissionStateWriteFile == PackageManager.PERMISSION_GRANTED) &&
                    (permissionStateReadFile == PackageManager.PERMISSION_GRANTED));
        } else {
            return ((permissionState == PackageManager.PERMISSION_GRANTED) &&
                    (permissionStateReadFile == PackageManager.PERMISSION_GRANTED));
        }
    }

    private void requestPermissions() {
        boolean shouldProvideRationale = false;
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            shouldProvideRationale =
                    ActivityCompat.shouldShowRequestPermissionRationale(this,
                            Manifest.permission.CAMERA) && ActivityCompat.shouldShowRequestPermissionRationale(this,
                            Manifest.permission.READ_MEDIA_IMAGES)  && ActivityCompat.shouldShowRequestPermissionRationale(this,
                            Manifest.permission.READ_EXTERNAL_STORAGE) && ActivityCompat.shouldShowRequestPermissionRationale(this,
                            Manifest.permission.ACCESS_FINE_LOCATION) && ActivityCompat.shouldShowRequestPermissionRationale(this,
                            Manifest.permission.ACCESS_COARSE_LOCATION);
        } else if (android.os.Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            shouldProvideRationale = ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.CAMERA) &&
                    ActivityCompat.shouldShowRequestPermissionRationale(this,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE) &&
                    ActivityCompat.shouldShowRequestPermissionRationale(this,
                            Manifest.permission.READ_EXTERNAL_STORAGE);
        } else {
            shouldProvideRationale = ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.CAMERA) &&
                    ActivityCompat.shouldShowRequestPermissionRationale(this,
                            Manifest.permission.READ_EXTERNAL_STORAGE);;
        }

        // Provide an additional rationale to the user. This would happen if the user denied the
        // request previously, but didn't check the "Don't ask again" checkbox.
        if (shouldProvideRationale) {
            Log.i("TRIP", "Displaying permission rationale to provide additional context.");
            String[] permissions;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                permissions = new String[]{Manifest.permission.CAMERA,Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.READ_EXTERNAL_STORAGE,  Manifest.permission.READ_MEDIA_IMAGES};
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                permissions = new String[]{Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE};
            } else {
                permissions = new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE};
            }
            ActivityCompat.requestPermissions(this, permissions, TAKE_PICTURE);
        } else {
            Log.i("TRIP", "Requesting permission");
            String[] permissions;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                permissions = new String[]{Manifest.permission.CAMERA,Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.READ_EXTERNAL_STORAGE,  Manifest.permission.READ_MEDIA_IMAGES};
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                permissions = new String[]{Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE};
            } else {
                permissions = new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE};
            }
            ActivityCompat.requestPermissions(this, permissions, TAKE_PICTURE);
        }
    }

    /**
     * Callback received when a permissions request has been completed.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        Log.i("TRIP", "onRequestPermissionResult");
        if (requestCode == TAKE_PICTURE) {
            if (grantResults.length <= 0) {
                // If user interaction was interrupted, the permission request is cancelled and you
                // receive empty arrays.
                Log.i("TRIP", "User interaction was cancelled.");
            } else if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                //Start Camera
                takePhotoFromCamera();

            } else {
                showSnackbar(R.string.permission_denied_explanation,
                        R.string.settings, view -> {
                            // Build intent that displays the App settings screen.
                            Intent intent = new Intent();
                            intent.setAction(
                                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                            Uri uri = Uri.fromParts("package",
                                    "com.pyramid.conveyance", null);
                            intent.setData(uri);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(intent);
                        });
            }
        } else if (requestCode == SELECT_PICTURE) {
            if (grantResults.length <= 0) {
                // If user interaction was interrupted, the permission request is cancelled and you
                // receive empty arrays.
                Log.i("TRIP", "User interaction was cancelled.");
            } else if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                //Start Camera
                selectImageFromGallery();
            } else {
                showSnackbar(R.string.permission_denied_explanation,
                        R.string.settings, view -> {
                            // Build intent that displays the App settings screen.
                            Intent intent = new Intent();
                            intent.setAction(
                                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                            Uri uri = Uri.fromParts("package",
                                    "com.pyramid.conveyance", null);
                            intent.setData(uri);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(intent);
                        });
            }
        }
    }

    /**
     * Shows a {@link Snackbar}.
     *
     * @param mainTextStringId The id for the string resource for the Snackbar text.
     * @param actionStringId   The text of the action item.
     * @param listener         The listener associated with the Snackbar action.
     */
    private void showSnackbar(final int mainTextStringId, final int actionStringId,
                              View.OnClickListener listener) {
        Snackbar.make(findViewById(android.R.id.content),
                        getString(mainTextStringId),
                        Snackbar.LENGTH_INDEFINITE)
                .setAction(getString(actionStringId), listener).show();
    }

    private void fetchTodaysRides() {
        AppExecutors.getInstance().getNetworkIO().execute(() -> {
            Date today = Calendar.getInstance().getTime();
            SearchRideFilter filter = new SearchRideFilter(TrackerUtility.getDateString(today), TrackerUtility.getDateString(today));

            Call<SearchRideResponse> searchRideStatisticsCall = ApiHandler.getClient().searchRideStatistics(LocationApp.getUserName(this), LocationApp.DEVICE_ID, filter);

            searchRideStatisticsCall.enqueue(new Callback<SearchRideResponse>() {
                @Override
                public void onResponse(Call<SearchRideResponse> call, Response<SearchRideResponse> response) {
                    if (response.isSuccessful()) {
                        List<Ride> tripRecordList = response.body().getRideDTOList();
                        if (tripRecordList != null) {
                            int incompleteRidesCount = countIncompleteRides(tripRecordList);
                            if (incompleteRidesCount > 0) {
                                showIncompleteRidesAlert();
                            } else {
                                logoutAndNavigateToLogin();
                            }
                        }
                    } else {
                        // Handle unsuccessful response (e.g., network error)
                        handleLogoutError();
                    }
                }

                @Override
                public void onFailure(Call<SearchRideResponse> call, Throwable t) {
                    // Handle failure (e.g., network failure)
                    handleLogoutError();
                }
            });
        });
    }

    private int countIncompleteRides(List<Ride> rideList) {
        return (int) rideList.stream()
                .filter(ride -> ride.getRideEndTime() == null)
                .count();
    }

    private void showIncompleteRidesAlert() {
        runOnUiThread(() -> {
            MaterialAlertDialogBuilder alertDialogBuilder = new MaterialAlertDialogBuilder(BottomNavigationActivity.this);
            alertDialogBuilder.setCancelable(true);
            alertDialogBuilder.setTitle("Alert");
            alertDialogBuilder.setMessage("Before logout you need to finish your incomplete rides.");
            alertDialogBuilder.setPositiveButton("Go to Rides", (dialogInterface, i) -> {
                if (MyService.isTrackingOn != null && MyService.isTrackingOn.getValue() != null && MyService.isTrackingOn.getValue()) {
                    Intent intent = new Intent(BottomNavigationActivity.this, RecordRideActivity.class);
                    startActivity(intent);
                } else {
                    NavController navController = Navigation.findNavController(BottomNavigationActivity.this, R.id.nav_host_fragment_activity_bottom_navigation);
                    navController.navigate(R.id.navigation_ridedetails);
                }
            });
            alertDialogBuilder.setNegativeButton("CANCEL", (dialogInterface, i) -> dialogInterface.dismiss());
            alertDialogBuilder.show();
        });
    }

    private void logoutAndNavigateToLogin() {
        runOnUiThread(() -> {
            logout();
            Intent intent = new Intent(BottomNavigationActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        });
    }

    private void handleLogoutError() {
        runOnUiThread(() -> {
            Toast.makeText(BottomNavigationActivity.this, "Unable to logout. Please try after some time.", Toast.LENGTH_LONG).show();
            // Handle the error condition as needed
        });
    }

}