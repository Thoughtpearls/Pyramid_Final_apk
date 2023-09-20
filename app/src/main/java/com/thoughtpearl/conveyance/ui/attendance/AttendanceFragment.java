package com.thoughtpearl.conveyance.ui.attendance;

import static android.app.Activity.RESULT_OK;
import static com.thoughtpearl.conveyance.LocationApp.employeeProfileLiveData;
import static com.thoughtpearl.conveyance.utility.TrackerUtility.convertStringToDate;
import static java.io.File.createTempFile;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.roomorama.caldroid.CaldroidFragment;
import com.thoughtpearl.conveyance.LocationApp;
import com.thoughtpearl.conveyance.R;
import com.thoughtpearl.conveyance.api.ApiHandler;
import com.thoughtpearl.conveyance.api.LeavesDetails;
import com.thoughtpearl.conveyance.api.SearchRideFilter;
import com.thoughtpearl.conveyance.api.SearchRideResponse;
import com.thoughtpearl.conveyance.api.response.Attendance;
import com.thoughtpearl.conveyance.api.response.EmployeeProfile;
import com.thoughtpearl.conveyance.api.response.Ride;
import com.thoughtpearl.conveyance.databinding.FragmentAttendanceBinding;
import com.thoughtpearl.conveyance.respository.executers.AppExecutors;
import com.thoughtpearl.conveyance.services.MyService;
import com.thoughtpearl.conveyance.ui.recordride.RecordRideActivity;
import com.thoughtpearl.conveyance.utility.TrackerUtility;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AttendanceFragment extends Fragment {

    public static final int ATTENDANCE_CHECKIN_CAMERA_REQUEST = 1;
    public static final int ATTENDANCE_CHECKOUT_CAMERA_REQUEST = 2;
    private static final int REQUEST_CODE = 11;
    private FragmentAttendanceBinding binding;
    CaldroidFragment caldroidFragment;
    File checkInImageFile;
    File checkOutImageFile;
    private static boolean isClockedIn = false;
    private static boolean isClockedOut = false;
    private String customStartDate;
    private String customEndDate;
    Activity mActivity;

    ActivityResultLauncher<Uri> mCheckInResultLauncher;
    ActivityResultLauncher<Uri> mCheckOutResultLauncher;
    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mActivity = (Activity) context;

        mCheckInResultLauncher = registerForActivityResult(
                new ActivityResultContracts.TakePicture(),
                result -> {
                    Log.d("TRIP", "result" + result.booleanValue()
                            + "checkInImageFile :" +
                            checkInImageFile != null ? checkInImageFile.getAbsolutePath() : "null");
                    if (result.booleanValue()) {
                        checkInAttendance(null);
                    } else {
                        Toast.makeText(requireActivity(),"CheckIn having some issue please try after sometime.", Toast.LENGTH_LONG).show();
                    }
                }
        );

        mCheckOutResultLauncher = registerForActivityResult(
                new ActivityResultContracts.TakePicture(),
                result -> {
                    Log.d("TRIP", "result" + result.booleanValue()
                            + "checkOutImageFile :" +
                            checkOutImageFile != null ? checkOutImageFile.getAbsolutePath() : "null");
                    if (result.booleanValue()) {
                        checkOutAttendance(null);
                    } else {
                        Toast.makeText(requireActivity(),"CheckOut having some issue please try after sometime.", Toast.LENGTH_LONG).show();
                    }
                }
        );
    }

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        AttendanceViewModel attendanceViewModel =
                new ViewModelProvider(this).get(AttendanceViewModel.class);

        LocationApp.logs("Attendance onCreate");
        binding = FragmentAttendanceBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        binding.test.setOnScrollChangeListener(new View.OnScrollChangeListener() {
            @Override
            public void onScrollChange(View v, int scrollX, int scrollY, int oldScrollX, int oldScrollY) {
                if(scrollY > 25) {
                    binding.swipeRefreshLayout.setEnabled(false);
                } else {
                    binding.swipeRefreshLayout.setEnabled(true);
                }
            }
        });
         binding.swipeRefreshLayout.setNestedScrollingEnabled(true);
         binding.swipeRefreshLayout.setOnRefreshListener(() -> {
            LocationApp.logs("TRIP", "OnRefresh called from SwipeRefreshLayout");
             if (!TrackerUtility.checkConnection(mActivity)) {
                 Toast.makeText(mActivity, "Please check your network connection", Toast.LENGTH_LONG).show();
                 setSwipeLayoutIsRefreshing(false);
             } else {
                 calculateLeave(false);

             }
         });

        SharedPreferences sharedPreferences = mActivity.getSharedPreferences(LocationApp.APP_NAME, Context.MODE_PRIVATE);
        AtomicReference<String> checkInDate = new AtomicReference<>(sharedPreferences.getString(LocationApp.CLOCK_IN, ""));
        AtomicReference<String> checkOutDate = new AtomicReference<>(sharedPreferences.getString(LocationApp.CLOCK_OUT, ""));
        AtomicReference<Boolean> isRideDisabled = new AtomicReference<>(sharedPreferences.getBoolean("rideDisabled", false));
        if (checkInDate.get().trim().length() > 0 &&  checkInDate.get().equalsIgnoreCase(TrackerUtility.getDateString(new Date()))) {
            isClockedIn = true;
            binding.checkInBtn.setBackgroundColor(Color.GRAY);
        } else {
            isClockedIn = false;
            binding.checkInBtn.setBackgroundColor(Color.WHITE);
        }

        if (checkOutDate.get().equalsIgnoreCase(TrackerUtility.getDateString(new Date()))) {
            isClockedOut = true;
            binding.checkOutBtn.setBackgroundColor(Color.GRAY);
        } else {
            isClockedOut = false;
            binding.checkOutBtn.setBackgroundColor(Color.WHITE);
        }

        binding.checkInBtn.setOnClickListener(view -> {
            LocationManager locationManager = (LocationManager) mActivity.getSystemService(Context.LOCATION_SERVICE);
            if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                Toast.makeText(mActivity, "Please turn on device location.", Toast.LENGTH_LONG).show();
                return;
            }
            checkInDate.set(sharedPreferences.getString(LocationApp.CLOCK_IN, ""));
            checkOutDate.set(sharedPreferences.getString(LocationApp.CLOCK_OUT, ""));
            if (checkInDate.get().trim().length() > 0 &&  checkInDate.get().equalsIgnoreCase(TrackerUtility.getDateString(new Date()))) {
                isClockedIn = true;
                binding.checkInBtn.setBackgroundColor(Color.GRAY);
            } else {
                isClockedIn = false;
                binding.checkInBtn.setBackgroundColor(Color.WHITE);
            }

            if (isClockedIn) {
                Toast.makeText(mActivity, "You are already Check In", Toast.LENGTH_LONG).show();
                return;
            }

            if (checkPermissions()) {
                // Continue only if the File was successfully created
                Uri photoURI;
                if (checkInImageFile == null) {
                    String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
                    String imageFileName = "JPEG_" + timeStamp + "_" + System.currentTimeMillis() + ".jpeg";
                    checkInImageFile = new File(this.mActivity.getExternalFilesDir(Environment.DIRECTORY_PICTURES), imageFileName);
                }

                try {
                    if (checkInImageFile!= null && !checkInImageFile.exists()) {
                        try {
                            checkInImageFile.createNewFile();
                            checkInImageFile.setExecutable(true, false);
                         } catch (IOException ioException){
                           LocationApp.logs("TRIP", "checkInImage : faild to create file :" + ioException);
                         }
                    }
                    photoURI = FileProvider.getUriForFile(
                            mActivity,
                            "com.thoughtpearl.conveyance.fileprovider",
                            checkInImageFile
                    );

                    Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    if (photoURI != null) {
                        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                        cameraIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                        cameraIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    } else {
                        File directory = new File(getContext().getFilesDir(), "camera_images");
                        if(!directory.exists()) {
                            directory.mkdirs();
                        }
                        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
                        String imageFileName = "JPEG_" + timeStamp + "_" + System.currentTimeMillis() + ".jpeg";
                        File file = new File(directory, imageFileName);
                        photoURI = FileProvider.getUriForFile(requireActivity(), getActivity().getPackageName() + ".fileprovider", file);
                    }
                    mCheckInResultLauncher.launch(photoURI);

                } catch (Exception e) {
                    Toast.makeText(mActivity, "Error in creating file : " + e.getMessage(), Toast.LENGTH_LONG).show();
                }
            } else {
                requestPermissions();
            }

        });

        binding.checkOutBtn.setOnClickListener(view -> {
                LocationManager locationManager = (LocationManager) mActivity.getSystemService(Context.LOCATION_SERVICE);
                if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    Toast.makeText(mActivity, "Please turn on device location.", Toast.LENGTH_LONG).show();
                    return;
                }
                checkInDate.set(sharedPreferences.getString(LocationApp.CLOCK_IN, ""));
                checkOutDate.set(sharedPreferences.getString(LocationApp.CLOCK_OUT, ""));
                if (checkInDate.get().trim().length() > 0 &&  checkInDate.get().equalsIgnoreCase(TrackerUtility.getDateString(new Date()))) {
                    isClockedIn = true;
                    binding.checkInBtn.setBackgroundColor(Color.GRAY);
                } else {
                    isClockedIn = false;
                    binding.checkInBtn.setBackgroundColor(Color.WHITE);
                }

                if (checkOutDate.get().equalsIgnoreCase(TrackerUtility.getDateString(new Date()))) {
                    isClockedOut = true;
                    binding.checkOutBtn.setBackgroundColor(Color.GRAY);
                } else {
                    isClockedOut = false;
                    binding.checkOutBtn.setBackgroundColor(Color.WHITE);
                }

                if (!isClockedIn) {
                    Toast.makeText(mActivity, "You have not Check In today.", Toast.LENGTH_LONG).show();
                    return;
                }
                if (isClockedOut) {
                    Toast.makeText(mActivity, "You are already Check Out", Toast.LENGTH_LONG).show();
                    return;
                }

                if (checkPermissions()) {
                    if (isRideDisabled.get()) {
                        attendanceCheckOut();

                    } else {
                        fetchTodaysRides();
                    }
                } else {
                    requestPermissions();
                }
        });

        binding.applyLeaveBtn.setOnClickListener(view -> {
            showLeavesAlertDialog();
        });

        // If Activity is created after rotation
        if (savedInstanceState != null && caldroidFragment != null) {
            caldroidFragment.restoreStatesFromKey(savedInstanceState,
                    "CALDROID_SAVED_STATE");
        } else {
            //if (caldroidFragment == null) {
                caldroidFragment = new CaldroidFragment();
                Bundle args = new Bundle();
                Calendar cal = Calendar.getInstance();
                args.putInt(CaldroidFragment.MONTH, cal.get(Calendar.MONTH) + 1);
                args.putInt(CaldroidFragment.YEAR, cal.get(Calendar.YEAR));
                caldroidFragment.setArguments(args);
                FragmentTransaction t = getActivity().getSupportFragmentManager().beginTransaction();
                t.replace(R.id.calender_container, caldroidFragment);
                t.commit();
        }

        LocationApp.leavesDetailsMutableLiveData.observe(getViewLifecycleOwner(), getLeavesDetailsObserver());

        if (!TrackerUtility.checkConnection(mActivity)) {
            Toast.makeText(mActivity, "Please check your network connection", Toast.LENGTH_LONG).show();
            setSwipeLayoutIsRefreshing(false);
        } else {
            calculateLeave(true);
        }

        return root;
    }

    @NonNull
    private Observer<LeavesDetails> getLeavesDetailsObserver() {
        return leavesDetails -> {
            int month = 0;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                month = LocalDate.now().getMonth().getValue();
            }

            HashMap<Date, Integer> leavingsTaken = new HashMap<>();
            HashMap<Date, Drawable> leavingsTakenHashMap = new HashMap<>();
            ColorDrawable publicHolidayColor = new ColorDrawable(getResources().getColor(R.color.public_holiday_color));
            ColorDrawable leaveTakenBackgroundColor = new ColorDrawable(getResources().getColor(R.color.leave_color));
            ColorDrawable compOffBackgroundColor = new ColorDrawable(getResources().getColor(R.color.compoff_color));
            ColorDrawable workingBackgroundColor = new ColorDrawable(getResources().getColor(R.color.working_color));
            ColorDrawable weekEndBackgroundColor = new ColorDrawable(getResources().getColor(R.color.weekend_color));
            int leaveTakenTextColor = R.color.white;
            List<Date> weekendList = TrackerUtility.getAllWeekends();

            weekendList.forEach(date -> {
                leavingsTaken.put(date, leaveTakenTextColor);
                leavingsTakenHashMap.put(date, weekEndBackgroundColor);
            });

            binding.leaveTakenTextView.setText("" + leavesDetails.getLeavesTaken());
            binding.leaveRemainingTextView.setText("" + leavesDetails.getLeavesRemaining());
            binding.compoffTextView.setText("" + leavesDetails.getCompOffByMonth());

            if (leavesDetails.getHolidays() != null) {
                leavesDetails.getHolidays().forEach(attendanceDetails -> {
                    leavingsTaken.put(convertStringToDate(attendanceDetails.getDate()), leaveTakenTextColor);
                    leavingsTakenHashMap.put(convertStringToDate(attendanceDetails.getDate()), publicHolidayColor);
                });
            }

            if (leavesDetails.getAttendancesByYear() != null && leavesDetails.getAttendancesByYear().size() > 0) {
                leavesDetails.getAttendancesByYear().forEach(attendanceDetails -> {
                    if (attendanceDetails.getType().equalsIgnoreCase("ON-LEAVE")) {
                        leavingsTaken.put(convertStringToDate(attendanceDetails.getDate()), leaveTakenTextColor);
                        leavingsTakenHashMap.put(convertStringToDate(attendanceDetails.getDate()), leaveTakenBackgroundColor);
                    }
                });
            }

            if (leavesDetails.getCompOffByYear() != null) {
                leavesDetails.getCompOffByYear().forEach(attendanceDetails -> {
                    leavingsTaken.put(convertStringToDate(attendanceDetails.getDate()), leaveTakenTextColor);
                    leavingsTakenHashMap.put(convertStringToDate(attendanceDetails.getDate()), compOffBackgroundColor);
                });
            }

            if (leavesDetails.getWorkingDaysByYear() != null) {
                leavesDetails.getWorkingDaysByYear().forEach(attendanceDetails -> {
                    leavingsTaken.put(convertStringToDate(attendanceDetails.getDate()), leaveTakenTextColor);
                    leavingsTakenHashMap.put(convertStringToDate(attendanceDetails.getDate()), workingBackgroundColor);
                });
            }

            if (leavingsTaken.size() > 0 || leavingsTakenHashMap.size() > 0) {
                if (caldroidFragment != null) {
                    caldroidFragment.setTextColorForDates(leavingsTaken);
                    caldroidFragment.setBackgroundDrawableForDates(leavingsTakenHashMap);

                    caldroidFragment.refreshView();
                }
            }

        };
    }

    private void setupWorkingDaysBackground(int month, HashMap<Date, Integer> leavingsTaken, HashMap<Date, Drawable> leavingsTakenHashMap, ColorDrawable workingBackgroundColor, int leaveTakenTextColor) {
        LocalDate firstDateOfTheMonth = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            firstDateOfTheMonth = LocalDate.now().withMonth(month).with(TemporalAdjusters.firstDayOfMonth());
        }
        SharedPreferences sharedPreferences = mActivity.getSharedPreferences(LocationApp.APP_NAME, Context.MODE_PRIVATE);
        AtomicReference<String> checkInDate = new AtomicReference<>(sharedPreferences.getString(LocationApp.CLOCK_IN, ""));
        AtomicReference<String> checkOutDate = new AtomicReference<>(sharedPreferences.getString(LocationApp.CLOCK_OUT, ""));
        if (checkInDate.get().trim().length() > 0 &&  checkInDate.get().equalsIgnoreCase(TrackerUtility.getDateString(new Date()))) {
            isClockedIn = true;
        } else {
            isClockedIn = false;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            for (LocalDate date = firstDateOfTheMonth; !date
                    .isAfter(firstDateOfTheMonth.with(TemporalAdjusters.lastDayOfMonth())); date = date.plusDays(1))
                if (!leavingsTakenHashMap.containsKey(TrackerUtility.asDate(date)) && !date.isAfter(LocalDate.now())) {
                     boolean isUpdate = true;
                    if ((checkInDate.get().equals(date.toString()))) {
                        isUpdate = isClockedIn;
                    }
                    if (isUpdate) {
                        leavingsTaken.put(TrackerUtility.asDate(date), leaveTakenTextColor);
                        leavingsTakenHashMap.put(TrackerUtility.asDate(date), workingBackgroundColor);
                    }
                }
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        // TODO Auto-generated method stub
        super.onSaveInstanceState(outState);

        if (caldroidFragment != null) {
            caldroidFragment.saveStatesToKey(outState, "CALDROID_SAVED_STATE");
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    public void markAttendance(String imagePath, Attendance attendance, AlertDialog alertDialog) {
        if (!TrackerUtility.checkConnection(mActivity)) {
            Toast.makeText(mActivity, "Please check your network connection", Toast.LENGTH_LONG).show();
        } else {

            Dialog dailog = LocationApp.showLoader(mActivity);

            RequestBody type = RequestBody.create(MediaType.parse("text/plain"), attendance.getType());
            RequestBody date = RequestBody.create(MediaType.parse("text/plain"), attendance.getDate());
            RequestBody time = RequestBody.create(MediaType.parse("text/plain"), attendance.getTime());

            String username = LocationApp.getUserName(mActivity);
            String deviceId = LocationApp.DEVICE_ID;
            Map<String, RequestBody> bodyMap = new HashMap<>();
            bodyMap.put("date", date);
            bodyMap.put("time", time);
            bodyMap.put("type", type);
            if (attendance.getType().equalsIgnoreCase(LocationApp.ON_LEAVE)) {
                RequestBody reasonForLeave = RequestBody.create(MediaType.parse("text/plain"), attendance.getReasonForLeave() == null ? "" : attendance.getReasonForLeave());
                bodyMap.put("reasonForLeave", reasonForLeave);
            }

            if (attendance.getType() != LocationApp.ON_LEAVE) {
                File file = new File(imagePath);
                RequestBody filePart = RequestBody.create(MediaType.parse("image/jpeg"), file);
                bodyMap.put("file\"; filename=\"" + type + ".png\" ", filePart);
            }

            if (attendance.getLatitude() != null) {
                RequestBody latitude = RequestBody.create(MediaType.parse("text/plain"), attendance.getLatitude());
                bodyMap.put("latitude", latitude);
            }

            if (attendance.getLongitude() != null) {
                RequestBody longitude = RequestBody.create(MediaType.parse("text/plain"), attendance.getLongitude());
                bodyMap.put("longitude", longitude);
            }

            Call<ResponseBody> markAttendanceCall = ApiHandler.getClient().markAttendance(username, deviceId, bodyMap);
            AppExecutors.getInstance().getNetworkIO().execute(() -> {
                try {
                    Response<ResponseBody> response = markAttendanceCall.execute();
                    mActivity.runOnUiThread(() -> {
                        if (response.code() == 201 || response.code() == 200) {
                            LocationApp.logs("username :" + username + " attendance : " + deviceId + "response :" + response.code());
                            if (response.code() == 201 || response.code() == 200) {
                                if (attendance.getType() == LocationApp.ON_LEAVE) {
                                    if (alertDialog != null) {
                                        alertDialog.dismiss();
                                        //new Handler().postDelayed(() -> calculateLeave(true), 5000);
                                    }
                                    Toast.makeText(mActivity, "Leave Applied Successfully.", Toast.LENGTH_SHORT).show();
                                } else {
                                    LocationApp.logs("TRIP", "marked attendance type:" + attendance.getType() + " date:" + attendance.getDate() + " Time : " + attendance.getTime());
                                    Toast.makeText(mActivity, "Attendance marked successfully", Toast.LENGTH_SHORT).show();
                                    SharedPreferences sharedPreferences = mActivity.getSharedPreferences(LocationApp.APP_NAME, Context.MODE_PRIVATE);
                                    SharedPreferences.Editor editor = sharedPreferences.edit();
                                    if (attendance.getType().equalsIgnoreCase(LocationApp.CLOCK_IN)) {
                                        new Handler().postDelayed(() -> calculateLeave(true), 2000);
                                        editor.putString(LocationApp.CLOCK_IN, attendance.getDate());
                                        editor.commit();
                                        new Handler().postDelayed(() ->  {
                                                    if (binding!= null && binding.checkInBtn != null) {
                                                        binding.checkInBtn.setBackgroundColor(Color.GRAY);
                                                    } else {
                                                       MaterialButton checkInBtn =  mActivity.findViewById(R.id.checkInBtn);
                                                       if (checkInBtn != null) {
                                                         checkInBtn.setBackgroundColor(Color.GRAY);
                                                       }
                                                    }
                                        }, 1000);

                                        isClockedIn = true;
                                    } else if (attendance.getType().equalsIgnoreCase(LocationApp.CLOCK_OUT)) {
                                        editor.putString(LocationApp.CLOCK_OUT, attendance.getDate());
                                        editor.commit();
                                        new Handler().postDelayed(() -> {
                                            if (binding!= null && binding.checkOutBtn != null) {
                                                binding.checkOutBtn.setBackgroundColor(Color.GRAY);
                                            } else {
                                                MaterialButton checkOutBtn =  mActivity.findViewById(R.id.checkOutBtn);
                                                if (checkOutBtn != null) {
                                                    checkOutBtn.setBackgroundColor(Color.GRAY);
                                                }
                                            }
                                        }, 1000);
                                        isClockedOut = true;
                                    }
                                }
                            } else {
                                LocationApp.logs("username :" + username + " attendance : " + deviceId + "response : Else block attendance.getType()" + attendance.getType());
                                LocationApp.logs("markAttendance", "username :" + username + " attendance : " + deviceId + "response : Else block");
                                LocationApp.logs("TRIP", "Error :" + response.errorBody());
                                String message = "Attendance not marked";
                                if (attendance.getType() == LocationApp.ON_LEAVE) {
                                    message = "Leave not applied. Please try after sometime.";
                                }
                                Toast.makeText(mActivity, "else block : " + message, Toast.LENGTH_SHORT).show();
                            }
                        } else if (response.code() == 400) {
                            String errorMessage = "Something went wrong. Please try again";
                            if (attendance.getType().equalsIgnoreCase(LocationApp.CLOCK_IN)) {
                                errorMessage = "Its seems you have already checkIn for a day";
                            } else  if (attendance.getType().equalsIgnoreCase(LocationApp.CLOCK_OUT))  {
                                errorMessage = "Its seems you have already checkout for a day";
                            } else  if (attendance.getType().equalsIgnoreCase(LocationApp.ON_LEAVE))  {
                                errorMessage = "Its seems you have applied leaves on sunday which is not allowed";
                            }

                            try {
                                JSONObject jsonObject = new JSONObject(new String(response.errorBody().bytes()));
                                 if (jsonObject.has("message")) {
                                     errorMessage = jsonObject.getString("message");
                                 }
                            }  catch (JSONException | IOException e) {
                                e.printStackTrace();
                            }
                            Toast.makeText(mActivity, errorMessage, Toast.LENGTH_SHORT).show();
                            SharedPreferences sharedPreferences = mActivity.getSharedPreferences(LocationApp.APP_NAME, Context.MODE_PRIVATE);
                            SharedPreferences.Editor editor = sharedPreferences.edit();
                            if (attendance.getType().equalsIgnoreCase(LocationApp.CLOCK_IN)) {
                                new Handler().postDelayed(() -> calculateLeave(true), 2000);
                                editor.putString(LocationApp.CLOCK_IN, attendance.getDate());
                                editor.apply();
                                new Handler().postDelayed(() -> {
                                    if (binding!= null && binding.checkInBtn != null) {
                                        binding.checkInBtn.setBackgroundColor(Color.GRAY);
                                    } else {
                                        MaterialButton checkInBtn = mActivity.findViewById(R.id.checkInBtn);
                                        if (checkInBtn != null) {
                                            checkInBtn.setBackgroundColor(Color.GRAY);
                                        }
                                      }}, 1000);
                                isClockedIn = true;
                            } else if (attendance.getType().equalsIgnoreCase(LocationApp.CLOCK_OUT)) {
                                editor.putString(LocationApp.CLOCK_OUT, attendance.getDate());
                                editor.commit();
                                new Handler().postDelayed(() -> {
                                    if (binding!= null && binding.checkOutBtn != null) {
                                        binding.checkOutBtn.setBackgroundColor(Color.GRAY);
                                    } else {
                                        MaterialButton checkOutBtn = mActivity.findViewById(R.id.checkOutBtn);
                                        if (checkOutBtn != null) {
                                            checkOutBtn.setBackgroundColor(Color.GRAY);
                                        }
                                    }
                                }, 1000);
                                isClockedOut = true;
                            }
                        } else {
                            LocationApp.logs("markAttendance",  "onFailure username :" + username +" attendance : " + deviceId + " attendance.getType() " + attendance.getType());
                            LocationApp.logs("username :" + username +" attendance : " + deviceId + "response : Error block");
                            Toast.makeText(mActivity, "Attendance not marked", Toast.LENGTH_SHORT).show();
                        }
                        dailog.dismiss();
                    });
                } catch (Exception e) {
                    mActivity.runOnUiThread(()-> {
                        LocationApp.logs("markAttendance",  "onFailure username :" + username +" attendance : " + deviceId + " attendance.getType() " + attendance.getType());
                        LocationApp.logs("username :" + username +" attendance : " + deviceId + "response : Error block");
                        Toast.makeText(mActivity, "exception :" + e.getMessage(), Toast.LENGTH_SHORT).show();
                        dailog.dismiss();
                    });
                }
            });

        }
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        LocationApp.logs("Attendance : onActivityResult :");
        Date myDate = new Date();
        String date = TrackerUtility.getDateString(myDate);
        String time = TrackerUtility.getTimeString(myDate);

        LocationManager locationManager = (LocationManager) mActivity.getSystemService(Context.LOCATION_SERVICE);
        Criteria criteria = new Criteria();
        String provider = locationManager.getBestProvider(criteria, true);
        LocationApp.logs("Attendance : onActivityResult : provider :" + provider);
        if (ActivityCompat.checkSelfPermission(mActivity, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(mActivity, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        Location location = null;

        if (provider != null) {
            location = locationManager.getLastKnownLocation(provider);
        }

        if (location == null && locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        }

        if (location == null && locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        }

        Uri fileUri = null;
        if (data != null && data.getExtras() != null) {
            LocationApp.logs("Attendance : onActivityResult : checkInImageFile Path: data not null");
            Bitmap picture = (Bitmap) data.getExtras().get("data");
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            picture.compress(Bitmap.CompressFormat.JPEG, 100, stream);
            LocationApp.logs("Attendance : onActivityResult : checkInImageFile Path: data not null 1");
            String filename = "title"; //"pic_"+ System.currentTimeMillis();
            String path = MediaStore.Images.Media.insertImage(mActivity.getContentResolver(), picture,
                    filename , null);
            LocationApp.logs("Attendance : onActivityResult : checkInImageFile Path: data not null Path:" + path);
            fileUri = Uri.parse(path);
            LocationApp.logs("Attendance : onActivityResult Path :" + getImageFilePath(fileUri));
        }

        if (requestCode == AttendanceFragment.ATTENDANCE_CHECKIN_CAMERA_REQUEST && resultCode == RESULT_OK) {
            checkInAttendance(fileUri);
        } else if (requestCode == AttendanceFragment.ATTENDANCE_CHECKOUT_CAMERA_REQUEST && resultCode == RESULT_OK) {
            checkOutAttendance(fileUri);
        }
    }

    private void checkOutAttendance(Uri fileUri) {

        LocationApp.logs("Attendance : onActivityResult :");
        Date myDate = new Date();
        String date = TrackerUtility.getDateString(myDate);
        String time = TrackerUtility.getTimeString(myDate);

        LocationManager locationManager = (LocationManager) mActivity.getSystemService(Context.LOCATION_SERVICE);
        Criteria criteria = new Criteria();
        String provider = locationManager.getBestProvider(criteria, true);
        LocationApp.logs("Attendance : onActivityResult : provider :" + provider);
        if (ActivityCompat.checkSelfPermission(mActivity, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(mActivity, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        Location location = null;

        if (provider != null) {
            location = locationManager.getLastKnownLocation(provider);
        }

        if (location == null && locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        }

        if (location == null && locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        }

        if (checkOutImageFile == null) {
            LocationApp.logs("Attendance : onActivityResult : checkOutImageFile Path: null");
            if (fileUri == null) {
                Toast.makeText(mActivity, "There is some issue in storing capture image on device.", Toast.LENGTH_LONG).show();
                return;
            } else {
                String filePath = getImageFilePath(fileUri);
                if (filePath != null) {
                    checkOutImageFile = new File(filePath);
                }
            }

            if (checkOutImageFile == null) {
                Toast.makeText(mActivity, "There is some issue in storing capture image on device.", Toast.LENGTH_LONG).show();
                return;
            }
        }

        if (checkOutImageFile != null && !checkOutImageFile.exists() && fileUri != null) {
            LocationApp.logs("Attendance : check out attendance :" + "Checkout path : checkOutImageFile not exits");

            String filePath = getImageFilePath(fileUri);
            if (filePath != null) {
                File file = new File(filePath);
                if (file.exists()) {
                    LocationApp.logs("Attendance : check out attendance :" + "CheckOut path : fileUri exits" + fileUri);
                    checkOutImageFile = file;
                }
            }
        }

        LocationApp.logs("TRIP", "Checkout path :" + checkOutImageFile != null ? checkOutImageFile.getAbsoluteFile().getAbsolutePath() : "null");
        LocationApp.logs("Attendance : check out attendance :" + "Checkout path :" + checkOutImageFile != null ? checkOutImageFile.getAbsoluteFile().getAbsolutePath() : "null");
        Attendance attendance = new Attendance();
        attendance.setType(LocationApp.CLOCK_OUT);
        attendance.setTime(time);
        attendance.setDate(date);
        if (location != null) {
            attendance.setLatitude(String.valueOf(location.getLatitude()));
            attendance.setLongitude(String.valueOf(location.getLongitude()));
        }
        LocationApp.logs("Attendance : before check out attendance :");
        markAttendance(checkOutImageFile.getAbsolutePath(), attendance, null);
        LocationApp.logs("Attendance : after check out attendance :");
    }

    private void checkInAttendance(Uri fileUri) {
        LocationApp.logs("Attendance : onActivityResult :");
        Date myDate = new Date();
        String date = TrackerUtility.getDateString(myDate);
        String time = TrackerUtility.getTimeString(myDate);

        LocationManager locationManager = (LocationManager) mActivity.getSystemService(Context.LOCATION_SERVICE);
        Criteria criteria = new Criteria();
        String provider = locationManager.getBestProvider(criteria, true);
        LocationApp.logs("Attendance : onActivityResult : provider :" + provider);
        if (ActivityCompat.checkSelfPermission(mActivity, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(mActivity, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        Location location = null;

        if (provider != null) {
            location = locationManager.getLastKnownLocation(provider);
        }

        if (location == null && locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        }

        if (location == null && locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        }

        LocationApp.logs("Attendance : onActivityResult : checkInImageFile Path:" + checkInImageFile);
        if (checkInImageFile == null) {
            if (fileUri == null) {
                Toast.makeText(mActivity, "There is some issue in storing capture image on device.", Toast.LENGTH_LONG).show();
                return;
            } else {
                String filePath = getImageFilePath(fileUri);
                if (filePath != null) {
                    checkInImageFile = new File(filePath);
                }
            }

            if (checkInImageFile == null) {
                Toast.makeText(mActivity, "There is some issue in storing capture image on device.", Toast.LENGTH_LONG).show();
                return;
            }
        }

        if (checkInImageFile != null && !checkInImageFile.exists() && fileUri != null) {
            LocationApp.logs("Attendance : check in attendance :" + "CheckIn path : checkInImageFile not exits");

            String filePath = getImageFilePath(fileUri);
            if (filePath != null) {
                File file = new File(filePath);
                if (file.exists()) {
                    LocationApp.logs("Attendance : check in attendance :" + "CheckIn path : fileUri exits" + fileUri);
                    checkInImageFile = file;
                }
            }
        }

        LocationApp.logs("TRIP", "CheckIn path :" + checkInImageFile != null ? checkInImageFile.getAbsoluteFile().getAbsolutePath() : "null");
        LocationApp.logs("Attendance : onActivityResult :" + "CheckIn path :" + checkInImageFile != null ? checkInImageFile.getAbsoluteFile().getAbsolutePath() : "null");
        Attendance attendance = new Attendance();
        attendance.setType(LocationApp.CLOCK_IN);
        attendance.setTime(time);
        attendance.setDate(date);
        if (location != null) {
            attendance.setLatitude(String.valueOf(location.getLatitude()));
            attendance.setLongitude(String.valueOf(location.getLongitude()));
        }
        LocationApp.logs("Attendance : before check in attendance :");
        markAttendance(checkInImageFile.getAbsolutePath(), attendance, null);
        LocationApp.logs("Attendance : After check in attendance :");
    }

    @SuppressLint("Range")
    public String getImageFilePath(Uri uri) {
        String path = null;
        if (uri.getScheme().equals("content")) {
            try (Cursor cursor = mActivity.getContentResolver().query(uri, null, null, null, null)) {
                if (cursor.moveToFirst()) {
                    path = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA));
                }
            } catch (IllegalArgumentException e) {
                LocationApp.logs(e);
            }
        }
        return path;
    }

    /**
     * Return the current state of the permissions needed.
     */
    private boolean checkPermissions() {

        int permissionState = ActivityCompat.checkSelfPermission(getContext(),
                Manifest.permission.CAMERA);
        int permissionStateWriteFile = ActivityCompat.checkSelfPermission(getContext(),
                Manifest.permission.WRITE_EXTERNAL_STORAGE);
        int permissionStateFineLocation = ActivityCompat.checkSelfPermission(getContext(),
                Manifest.permission.ACCESS_FINE_LOCATION);
        int permissionStateCourseLocation = ActivityCompat.checkSelfPermission(getContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION);

        if (android.os.Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            return ((permissionState == PackageManager.PERMISSION_GRANTED) &&
                    (permissionStateWriteFile == PackageManager.PERMISSION_GRANTED) &&
                    (permissionStateFineLocation == PackageManager.PERMISSION_GRANTED) &&
                    (permissionStateCourseLocation == PackageManager.PERMISSION_GRANTED));
        } else {
            return ((permissionState == PackageManager.PERMISSION_GRANTED) &&
                    (permissionStateFineLocation == PackageManager.PERMISSION_GRANTED) &&
                    (permissionStateCourseLocation == PackageManager.PERMISSION_GRANTED));
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!checkPermissions()) {
            requestPermissions();
        }
    }

    private void takePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
             try {
                 Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                 intent.addCategory("android.intent.category.DEFAULT");
                 Uri uri = Uri.fromParts("package", mActivity.getPackageName(), null);
                 intent.setData(uri);
                 startActivityForResult(intent, 101);
             } catch (Exception exception) {
                 Intent intent = new Intent();
                 intent.setAction(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                 startActivityForResult(intent, 101);
             }
        } else {
            ActivityCompat.requestPermissions(mActivity, new String[] {Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE}, 101);
        }
    }

    private void requestPermissions() {

        boolean shouldProvideRationale = false;
        if (android.os.Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            shouldProvideRationale =
                    ActivityCompat.shouldShowRequestPermissionRationale(mActivity,
                            Manifest.permission.CAMERA) && ActivityCompat.shouldShowRequestPermissionRationale(mActivity,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE) && ActivityCompat.shouldShowRequestPermissionRationale(mActivity,
                            Manifest.permission.ACCESS_FINE_LOCATION) && ActivityCompat.shouldShowRequestPermissionRationale(mActivity,
                            Manifest.permission.ACCESS_COARSE_LOCATION);
        } else {
            shouldProvideRationale =
                    ActivityCompat.shouldShowRequestPermissionRationale(mActivity,
                            Manifest.permission.CAMERA) && ActivityCompat.shouldShowRequestPermissionRationale(mActivity,
                            Manifest.permission.ACCESS_FINE_LOCATION) && ActivityCompat.shouldShowRequestPermissionRationale(mActivity,
                            Manifest.permission.ACCESS_COARSE_LOCATION);
        }

        // Provide an additional rationale to the user. This would happen if the user denied the
        // request previously, but didn't check the "Don't ask again" checkbox.
        if (shouldProvideRationale) {
            Log.i("TRIP", "Displaying permission rationale to provide additional context.");

            ActivityCompat.requestPermissions(mActivity,
                    new String[]{Manifest.permission.CAMERA,Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    REQUEST_CODE);

        } else {
            Log.i("TRIP", "Requesting permission");
            // Request permission. It's possible this can be auto answered if device policy
            // sets the permission in a given state or the user denied the permission
            // previously and checked "Never ask again".
            String[] permissions = new String[]{Manifest.permission.CAMERA,Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.WRITE_EXTERNAL_STORAGE};

            if (android.os.Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                permissions = new String[]{Manifest.permission.CAMERA,Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION};
            }

            ActivityCompat.requestPermissions(mActivity,
                    permissions,
                    REQUEST_CODE);

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
        Snackbar.make(mActivity.findViewById(android.R.id.content),
                        getString(mainTextStringId),
                        Snackbar.LENGTH_INDEFINITE )
                .setAction(getString(actionStringId), listener).show();
    }

    /**
     * Callback received when a permissions request has been completed.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        Log.i("TRIP", "onRequestPermissionResult");
        if (requestCode == REQUEST_CODE) {
            if (grantResults.length <= 0) {
                // If user interaction was interrupted, the permission request is cancelled and you
                // receive empty arrays.
                Log.i("TRIP", "User interaction was cancelled.");
            } else if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                //Start Camera

            } else {
                // Permission denied.

                // Notify the user via a SnackBar that they have rejected a core permission for the
                // app, which makes the Activity useless. In a real app, core permissions would
                // typically be best requested during a welcome-screen flow.

                // Additionally, it is important to remember that a permission might have been
                // rejected without asking the user for permission (device policy or "Never ask
                // again" prompts). Therefore, a user interface affordance is typically implemented
                // when permissions are denied. Otherwise, your app could appear unresponsive to
                // touches or interactions which have required permissions.
                showSnackbar(R.string.permission_denied_explanation,
                        R.string.settings, view -> {
                            // Build intent that displays the App settings screen.
                            Intent intent = new Intent();
                            intent.setAction(
                                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                            Uri uri = Uri.fromParts("package",
                                    "com.thoughtpearl.conveyance", null);
                            intent.setData(uri);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(intent);
                        });
            }
        }
    }

    private File createImageFile() {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = new File(this.mActivity.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "RideRecord");

        // Create the storage directory if it does not exist
        if (! storageDir.exists()) {
            if (! storageDir.mkdirs()) {
                LocationApp.logs("TRIP", "storageDir : failed to create directory");
                File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_PICTURES), "RideRecord");

                if (!mediaStorageDir.exists()) {
                    if (! mediaStorageDir.mkdirs()) {
                        LocationApp.logs("TRIP", "mediaStorageDir : failed to create directory");
                    }
                }
                storageDir = mediaStorageDir;
            }
        }

        File image = null;
        try {
            image = createTempFile(
                    imageFileName, /* prefix */
                    ".jpg", /* suffix */
                    storageDir      /* directory */
            );
        } catch (IOException e) {
            e.printStackTrace();
           /*image = new File(String.valueOf(getActivity().getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    new ContentValues())));*/
        }

        // Save a file: path for use with ACTION_VIEW intents
        //mCurrentPhotoPath = image.getAbsolutePath();
        return image;
    }

    public void calculateLeave(boolean showLoader) {
        Dialog dialog = null;
        if (showLoader) {
            dialog = LocationApp.showLoader(mActivity);
        }
        setAttendanceLayoutVisibility(View.VISIBLE);
        setErrorLayoutVisibility(View.GONE);
        Call<LeavesDetails> leavesDetailsCAll= ApiHandler.getClient().getLeaveDetails(LocationApp.getUserName(mActivity), LocationApp.DEVICE_ID);
        Dialog finalDialog = dialog;
        leavesDetailsCAll.enqueue(new Callback<LeavesDetails>() {
            @Override
            public void onResponse(Call<LeavesDetails> call, Response<LeavesDetails> response) {
                if (response.code() == 200 || response.code() == 201) {
                   LeavesDetails leavesDetails = response.body();
                   LocationApp.leavesDetailsMutableLiveData.setValue(leavesDetails);
                   String username = LocationApp.getUserName(mActivity);
                   String deviceId = TrackerUtility.getDeviceId(mActivity);
                   getEmployeeProfile(username, deviceId);
                } else {
                    LocationApp.logs("TRIP", String.valueOf(response));
                    Toast.makeText(mActivity, "Leaves details not retried", Toast.LENGTH_SHORT).show();
                }
                //call employeeProfile

                new Handler().postDelayed(()-> {
                    if (showLoader) {
                        finalDialog.dismiss();
                    }
                    setSwipeLayoutIsRefreshing(false);
                }, 2000);
            }

            @Override
            public void onFailure(Call<LeavesDetails> call, Throwable t) {

                new Handler().postDelayed(()-> {
                    Toast.makeText(mActivity, "Leaves details not retried", Toast.LENGTH_SHORT).show();
                    if (showLoader) {
                        finalDialog.dismiss();
                    }

                    setAttendanceLayoutVisibility(View.GONE);
                    setErrorLayoutVisibility(View.VISIBLE);
                    setSwipeLayoutIsRefreshing(false);
                }, 2000);
            }
        });
    }

    private void setSwipeLayoutIsRefreshing(boolean isRefreshing) {
        if (binding != null && binding.swipeRefreshLayout != null) {
            binding.swipeRefreshLayout.setRefreshing(isRefreshing);
        } else {
            SwipeRefreshLayout swipeRefreshLayout = mActivity.findViewById(R.id.swipeRefreshLayout);
            if (swipeRefreshLayout != null) {
                swipeRefreshLayout.setRefreshing(isRefreshing);
            }
        }
    }

    private void setErrorLayoutVisibility(int visibility) {
        if (binding != null && binding.errorAnimation != null) {
            binding.errorAnimation.setVisibility(visibility);
        } else {
            View errorAnimationView = mActivity.findViewById(R.id.errorAnimation);
            if (errorAnimationView != null) {
                errorAnimationView.setVisibility(visibility);
            }
        }
    }

    private void setAttendanceLayoutVisibility(int visibility) {
        if (binding != null && binding.attendanceLayout != null) {
            binding.attendanceLayout.setVisibility(visibility);
        } else {
            View attendanceLayoutView = mActivity.findViewById(R.id.attendanceLayout);
            if (attendanceLayoutView != null) {
                attendanceLayoutView.setVisibility(visibility);
            }
        }
    }

    private void showLeavesAlertDialog() {
        customStartDate = TrackerUtility.getDateString(Calendar.getInstance().getTime());
        customEndDate = TrackerUtility.getDateString(Calendar.getInstance().getTime());

        MaterialAlertDialogBuilder alertDialogBuilder = new MaterialAlertDialogBuilder(mActivity);
        alertDialogBuilder.setCancelable(true);
        alertDialogBuilder.setView(R.layout.date_picker_layout);
        AlertDialog alertDialog = alertDialogBuilder.show();
        ((TextView)alertDialog.findViewById(R.id.popupTitle)).setText("Apply for leaves");
        ((TextView)alertDialog.findViewById(R.id.okAlertButton)).setText("Apply");
        ((TextView)alertDialog.findViewById(R.id.fromDateLabel)).setText("Leave Date");
        alertDialog.findViewById(R.id.leaveReason).setVisibility(View.VISIBLE);
        alertDialog.findViewById(R.id.leaveReasonEditText).setVisibility(View.VISIBLE);
        alertDialog.findViewById(R.id.toDateLabel).setVisibility(View.GONE);
        alertDialog.findViewById(R.id.toDateTextView).setVisibility(View.GONE);
        alertDialog.findViewById(R.id.okAlertButton).setOnClickListener(view-> {
            /*if (!TrackerUtility.convertStringToDate(customEndDate).after(TrackerUtility.convertStringToDate(customStartDate))) {
                Toast.makeText(mActivity, "From Date should not be less than To Date", Toast.LENGTH_LONG).show();
            } else*/
            {
                LocalDate localDate = null;
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    localDate = LocalDate.parse(customStartDate);
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    if (localDate.getDayOfWeek() == DayOfWeek.SUNDAY) {
                        Toast.makeText(mActivity, "Sorry, Cannot mark leave on sundays", Toast.LENGTH_LONG).show();
                        return;
                    }
                }

                String reasonForLeave = ((EditText) alertDialog.findViewById(R.id.leaveReasonEditText)).getText().toString();
                if (reasonForLeave.trim().length() == 0) {
                    Toast.makeText(mActivity, "Please enter reason for leave", Toast.LENGTH_LONG).show();
                    return;
                }

                Date myDate = convertStringToDate(customStartDate);
                if (LocationApp.leavesDetailsMutableLiveData != null &&
                        LocationApp.leavesDetailsMutableLiveData.getValue() != null) {
                   int count = (int) LocationApp.leavesDetailsMutableLiveData.getValue().getHolidays().stream().filter(attendanceDetails -> attendanceDetails.getDate().equalsIgnoreCase(customStartDate)).count();
                    if (count > 0) {
                        Toast.makeText(mActivity, "Can not apply leaves on public holidays", Toast.LENGTH_LONG).show();
                        return;
                    }

                    int alreadyLeaveCount = (int) LocationApp.leavesDetailsMutableLiveData.getValue().getAttendancesByYear().stream().filter(attendanceDetails -> attendanceDetails.getDate().equalsIgnoreCase(customStartDate)).count();
                    if (alreadyLeaveCount > 0) {
                        Toast.makeText(mActivity, "You have already applied for leave.", Toast.LENGTH_LONG).show();
                        return;
                    }
                }

                String date = TrackerUtility.getDateString(myDate);
                String time = TrackerUtility.getTimeString(myDate);
                LocationManager locationManager = (LocationManager) mActivity.getSystemService(Context.LOCATION_SERVICE);
                Criteria criteria = new Criteria();
                String provider = locationManager.getBestProvider(criteria, true);
                if (ActivityCompat.checkSelfPermission(mActivity, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(mActivity, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    // TODO: Consider calling
                    //    ActivityCompat#requestPermissions
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for ActivityCompat#requestPermissions for more details.
                    return;
                }

                Location location = null;
                if (provider != null) {
                    location = locationManager.getLastKnownLocation(provider);
                }
                if (location == null && locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                }
                if (location == null && locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                    location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                }

                Attendance attendance = new Attendance();
                attendance.setType(LocationApp.ON_LEAVE);
                attendance.setTime(time);
                attendance.setDate(date);
                attendance.setReasonForLeave(reasonForLeave);
                if (location != null) {
                    attendance.setLatitude(String.valueOf(location.getLatitude()));
                    attendance.setLongitude(String.valueOf(location.getLongitude()));
                }
                markAttendance("", attendance, alertDialog);
            }
        });

        alertDialog.findViewById(R.id.cancelAlertButton).setOnClickListener(view-> alertDialog.dismiss());

        DatePickerDialog.OnDateSetListener fromDateListener = (datePicker, i, i1, i2) -> {
            Calendar mCalendar = Calendar.getInstance();
            mCalendar.set(Calendar.YEAR, i);
            mCalendar.set(Calendar.MONTH, i1);
            mCalendar.set(Calendar.DAY_OF_MONTH, i2);
            customStartDate = TrackerUtility.getDateString(mCalendar.getTime());
            customEndDate = customStartDate;
            LocalDate localDate = null;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                localDate = LocalDate.parse(customStartDate);
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (localDate.getDayOfWeek() == DayOfWeek.SUNDAY) {
                    Toast.makeText(mActivity, "Sorry, Cannot mark leave on sundays", Toast.LENGTH_LONG).show();
                    return;
                }
            }
            ((TextView)alertDialog.findViewById(R.id.fromDateTextView)).setText(customStartDate);
            ((TextView)alertDialog.findViewById(R.id.toDateTextView)).setText(customEndDate);

        };

        DatePickerDialog.OnDateSetListener toDateListener = (datePicker, i, i1, i2) -> {
            Calendar mCalendar = Calendar.getInstance();
            mCalendar.set(Calendar.YEAR, i);
            mCalendar.set(Calendar.MONTH, i1);
            mCalendar.set(Calendar.DAY_OF_MONTH, i2);

            if (!convertStringToDate(customStartDate).before(mCalendar.getTime())) {
                Toast.makeText(mActivity, "ToDate can not be less than FromDate", Toast.LENGTH_SHORT).show();
                return;
            }
            customEndDate = TrackerUtility.getDateString(mCalendar.getTime());
            ((TextView)alertDialog.findViewById(R.id.toDateTextView)).setText(customEndDate);
        };

        String sToDate = TrackerUtility.getDateString(Calendar.getInstance().getTime());
        ((TextView)alertDialog.findViewById(R.id.toDateTextView)).setText(sToDate);
        ((TextView)alertDialog.findViewById(R.id.fromDateTextView)).setText(sToDate);

        alertDialog.findViewById(R.id.fromDateTextView).setOnClickListener(view -> {
            //Toast.makeText(mActivity, "From date clicked", Toast.LENGTH_SHORT).show();
            showCalender(fromDateListener);
        });

    }

    private void showCalender(DatePickerDialog.OnDateSetListener dateListener) {
        Calendar mCalendar = Calendar.getInstance();
        int year = mCalendar.get(Calendar.YEAR);
        int month = mCalendar.get(Calendar.MONTH);
        int dayOfMonth = mCalendar.get(Calendar.DAY_OF_MONTH);
        new DatePickerDialog(mActivity, dateListener, year, month, dayOfMonth).show();
    }

    private void fetchTodaysRides() {
        LocationApp.logs("Fetch Today's Rides");
        AppExecutors.getInstance().getNetworkIO().execute(() -> {
            Date today = Calendar.getInstance().getTime();
            //today = TrackerUtility.convertStringToDate("2022-12-27");
            SearchRideFilter filter = new SearchRideFilter(TrackerUtility.getDateString(today), TrackerUtility.getDateString(today));
            Call<SearchRideResponse> searchRideStatisticsCall = ApiHandler.getClient().searchRideStatistics(LocationApp.getUserName(mActivity), LocationApp.DEVICE_ID, filter);
            searchRideStatisticsCall.enqueue(new Callback<SearchRideResponse>() {
                @Override
                public void onResponse(Call<SearchRideResponse> call, Response<SearchRideResponse> response) {
                    LocationApp.logs("Fetch Today's Rides : response " + response.code());
                    if (response.code() == 200) {
                        List<Ride> tripRecordList = response.body().getRideDTOList();
                        if (tripRecordList != null) {
                            int incompleteRidesCount = (int) tripRecordList.stream().filter(ride -> ride.getRideEndTime() == null).count();
                            if (incompleteRidesCount > 0) {

                                MaterialAlertDialogBuilder alertDialogBuilder = new MaterialAlertDialogBuilder(mActivity);
                                alertDialogBuilder.setCancelable(true);
                                alertDialogBuilder.setTitle("Alert");
                                alertDialogBuilder.setMessage("Before checkout you need to finish your incomplete or running rides.");
                                alertDialogBuilder.setPositiveButton("Goto Rides", (dialogInterface, i) -> {

                                    if (MyService.isTrackingOn != null && MyService.isTrackingOn.getValue() != null && MyService.isTrackingOn.getValue()) {
                                        Intent intent = new Intent(getContext(), RecordRideActivity.class);
                                        startActivity(intent);
                                    } else {
                                        NavController navController = Navigation.findNavController(mActivity, R.id.nav_host_fragment_activity_bottom_navigation);
                                        navController.navigate(R.id.navigation_ridedetails);
                                    }
                                });
                                alertDialogBuilder.setNegativeButton("CANCEL", (dialogInterface, i) -> dialogInterface.dismiss());
                                alertDialogBuilder.show();
                                //Toast.makeText(getApplicationContext(), "", Toast.LENGTH_LONG).show();

                            } else {
                                attendanceCheckOut();
                            }
                        }
                    }
                }
                @Override
                public void onFailure(Call<SearchRideResponse> call, Throwable t) {
                    //Toast.makeText(context, "Failed to fetch rides at the moment.", Toast.LENGTH_LONG).show();
                }
            });
            //}
        });
    }

    private void attendanceCheckOut() {
        checkOutImageFile = createImageFile();
        if (checkOutImageFile == null) {
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            String imageFileName = "JPEG_" + timeStamp + "_" + System.currentTimeMillis() + ".jpeg";
            checkOutImageFile = new File(mActivity.getExternalFilesDir(Environment.DIRECTORY_PICTURES), imageFileName);
        }
        // Continue only if the File was successfully created
        Uri photoURI = null;
        if (checkOutImageFile != null) {
            try {
                if (!checkOutImageFile.exists()) {
                    try {
                        checkOutImageFile.createNewFile();
                        checkOutImageFile.setExecutable(true, false);
                    } catch (IOException ioException) {
                        LocationApp.logs("TRIP", "checkInImage : faild to create file :" + ioException);
                        //mActivity.getFileStreamPath("test");
                        //checkOutImageFile = new File(String.valueOf(mActivity.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, new ContentValues())));
                    }
                }
                photoURI = FileProvider.getUriForFile(
                            mActivity,
                            "com.thoughtpearl.conveyance.fileprovider",
                            checkOutImageFile
                    );
                Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                if (photoURI != null) {
                    cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                    cameraIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                    cameraIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                } else {
                        File directory = new File(getContext().getFilesDir(), "camera_images");
                        if(!directory.exists()) {
                            directory.mkdirs();
                        }
                        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
                        String imageFileName = "JPEG_" + timeStamp + "_" + System.currentTimeMillis() + ".jpeg";
                        File file = new File(directory, imageFileName);
                        photoURI = FileProvider.getUriForFile(requireActivity(), getActivity().getPackageName() + ".fileprovider", file);
                }
                //startActivityForResult(cameraIntent, ATTENDANCE_CHECKOUT_CAMERA_REQUEST);
                mCheckOutResultLauncher.launch(photoURI);
            } catch (Exception e) {
                Toast.makeText(mActivity, "Error in creating file :" + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        } else {
            Toast.makeText(mActivity, "It seems app does not have permission for storing file.", Toast.LENGTH_LONG).show();
        }
    }

    void getEmployeeProfile(String userName, String deviceId) {

        Call<EmployeeProfile> employeeProfileCall = ApiHandler.getClient().getEmployeeProfile(userName, deviceId);
        employeeProfileCall.enqueue(new Callback<EmployeeProfile>() {
            @Override
            public void onResponse(Call<EmployeeProfile> call, Response<EmployeeProfile> response) {
                if (response.code() == 200) {
                    employeeProfileLiveData.setValue(response.body());
                    mActivity.runOnUiThread(() -> {
                        SharedPreferences sharedPreferences = mActivity.getSharedPreferences(LocationApp.APP_NAME, Context.MODE_PRIVATE);
                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        editor.putString("employeeFullName", employeeProfileLiveData.getValue().getFullName());
                        editor.putString("employeeCode", employeeProfileLiveData.getValue().getEmployeeCode());
                        if (employeeProfileLiveData.getValue().isTodaysClockIn()) {
                            editor.putString(LocationApp.CLOCK_IN, TrackerUtility.getDateString(new Date()));
                            isClockedIn = true;
                            if (binding!= null && binding.checkInBtn != null) {
                                binding.checkInBtn.setBackgroundColor(Color.GRAY);
                            } else {
                                View view = mActivity.findViewById(R.id.checkInBtn);
                                if (view != null) {
                                    view.setBackgroundColor(Color.GRAY);
                                }
                            }
                        } else {
                            isClockedIn = false;
                            editor.putString(LocationApp.CLOCK_IN,"");
                            if (binding!= null && binding.checkInBtn != null) {
                                binding.checkInBtn.setBackgroundColor(Color.WHITE);
                            } else {
                                View view = mActivity.findViewById(R.id.checkInBtn);
                                if (view != null) {
                                    view.setBackgroundColor(Color.WHITE);
                                }
                            }
                        }

                        if (employeeProfileLiveData.getValue().isTodaysClockOut()) {
                            isClockedOut = true;
                            editor.putString(LocationApp.CLOCK_OUT, TrackerUtility.getDateString(new Date()));
                            if (binding!= null && binding.checkOutBtn != null) {
                                binding.checkOutBtn.setBackgroundColor(Color.GRAY);
                            } else {
                                View view = mActivity.findViewById(R.id.checkOutBtn);
                                if (view != null) {
                                    view.setBackgroundColor(Color.GRAY);
                                }
                            }
                        } else {
                            isClockedOut = false;
                            editor.putString(LocationApp.CLOCK_OUT, "");
                            if (binding!= null && binding.checkOutBtn != null) {
                                binding.checkOutBtn.setBackgroundColor(Color.WHITE);
                            } else {
                                View view = mActivity.findViewById(R.id.checkOutBtn);
                                if (view != null) {
                                    view.setBackgroundColor(Color.WHITE);
                                }
                            }
                        }
                        editor.apply();
                    });

                }
            }
            @Override
            public void onFailure(Call<EmployeeProfile> call, Throwable t) {

            }
        });
    }
}