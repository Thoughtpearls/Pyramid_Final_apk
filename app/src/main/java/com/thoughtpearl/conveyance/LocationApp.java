package com.thoughtpearl.conveyance;

import android.app.Application;
import android.app.Dialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleEventObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ProcessLifecycleOwner;

import com.google.firebase.FirebaseApp;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.hypertrack.hyperlog.HyperLog;
import com.thoughtpearl.conveyance.api.LeavesDetails;
import com.thoughtpearl.conveyance.api.response.EmployeeProfile;
import com.thoughtpearl.conveyance.crashlytics.CrashlysticsCustomKey;
import com.thoughtpearl.conveyance.ui.customcomponent.MyProgressDialog;
import com.thoughtpearl.conveyance.utility.TrackerUtility;

public class LocationApp extends Application implements LifecycleEventObserver {

    public static final String APP_NAME = "TrackerApp";
    public static final float ZOOM_LEVEL = 17.5F;
    public static final float POLYLINE_WIDTH = 12F;
    public static final int POLYLINE_COLOR = Color.BLUE;
    public static final String NOTIFICATION_CHANNEL_ID = "tracking_channel";
    public static final String NOTIFICATION_CHANNEL_NAME = "Recording Ride";
    public static final int NOTIFICATION_ID = 1;
    public static final String CLOCK_IN = "CLOCKIN";
    public static final String CLOCK_OUT = "CLOCKOUT";
    public static final String ON_LEAVE = "ONLEAVE";
    public static String DEVICE_ID;
    public static String USER_NAME;
    public static MutableLiveData<EmployeeProfile> employeeProfileLiveData = new MutableLiveData<>();
    public static MutableLiveData<LeavesDetails> leavesDetailsMutableLiveData = new MutableLiveData<>();
    public static Dialog dialog;
    public static MyProgressDialog progressDialog;
    private static FirebaseCrashlytics mCrashlytics;
    private CrashlysticsCustomKey crashlysticsCustomKey;
    private static boolean isAppInBackground = false;
    public static void logs(String record_activity) {
        if (mCrashlytics != null) {
            mCrashlytics.setUserId(LocationApp.USER_NAME);
            mCrashlytics.log(record_activity);
            mCrashlytics.setCustomKey(LocationApp.USER_NAME, record_activity);
            mCrashlytics.sendUnsentReports();
            HyperLog.d("TRIP", "UserId :" + LocationApp.DEVICE_ID +"Username: " + LocationApp.USER_NAME + " Log: " + record_activity);
            //Timber.d("UserId :" + LocationApp.DEVICE_ID +"Username: " + LocationApp.USER_NAME + " Log: " + record_activity);
        }
    }

    public static void logs(String key, String message) {
        FirebaseCrashlytics crashlytics = FirebaseCrashlytics.getInstance();
        if (crashlytics != null) {
            crashlytics.setUserId(LocationApp.USER_NAME);
            crashlytics.setCustomKey(key, message);
            crashlytics.log(key + " : " + message);
        }
        HyperLog.d(key, message);
        //Timber.d(key  +" : " + message);
    }

    public static void logs(Throwable throwable) {
        if (mCrashlytics != null) {
            mCrashlytics.setUserId(LocationApp.USER_NAME);
            mCrashlytics.recordException(throwable);
        }
        //Timber.d(throwable);
        HyperLog.e("TRIP", "exception : ", throwable);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        ProcessLifecycleOwner.get().getLifecycle().addObserver(this);
        DEVICE_ID = TrackerUtility.getDeviceId(getApplicationContext());
        FirebaseApp.initializeApp(getApplicationContext());
        //Timber.plant(new Timber.DebugTree());
        HyperLog.initialize(this);
        //HyperLog.setURL("http://172.31.80.1:8080/uploadFile");
        HyperLog.setURL("http://43.204.194.87:8091/api/log/upload");
        //Timber.plant(new SentryBreadcrumbTree(Log.DEBUG));
        mCrashlytics = FirebaseCrashlytics.getInstance();
        mCrashlytics.setCrashlyticsCollectionEnabled(true);
        this.crashlysticsCustomKey = new CrashlysticsCustomKey(this.getApplicationContext());
        this.crashlysticsCustomKey.setCustomKeys();
        this.crashlysticsCustomKey.updateAndTrackNetworkState();
        //DEVICE_ID = "89ABCDEF-01234567-89ABCDEF";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel("location", "location", NotificationManager.IMPORTANCE_LOW);
            NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            notificationManager.createNotificationChannel(channel);
        }
        USER_NAME = getUserName(this);
        progressDialog = new MyProgressDialog(this);
    }

    public static Dialog showLoader(Context context) {
        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
        }
        dialog = progressDialog.show(context, "", "");
        return dialog;
    }

    public static Dialog showLoader() {
        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
        }
        dialog = progressDialog.show(progressDialog.getOwnerActivity(), "", "");
        return dialog;
    }

    public static void dismissLoader() {
        if (dialog == null) {
            return;
        }
        //Activity activity = dialog.getOwnerActivity();
        //if (activity != null && !activity.isFinishing()) {
        try {
            if (dialog != null && dialog.isShowing()) {
                dialog.dismiss();
            }
        } catch (Exception exception) {
            logs("TRIP", "enabled to close dialog :" + exception.getMessage());
        }
        //}
    }

    public static EmployeeProfile getEmployeeProfile() {
        if (employeeProfileLiveData.getValue() == null) {
            EmployeeProfile employeeProfile = new EmployeeProfile();
            return employeeProfile;
        }
        return employeeProfileLiveData.getValue();
    }

    public static String getUserName(Context context) {
        if (USER_NAME == null || USER_NAME.trim().length() == 0) {
            SharedPreferences sharedPreferences = context.getSharedPreferences(LocationApp.APP_NAME, MODE_PRIVATE);
            USER_NAME = sharedPreferences.getString("username", "");
        }
        return  USER_NAME;
    }

    @Override
    public void onStateChanged(@NonNull LifecycleOwner source, @NonNull Lifecycle.Event event) {
       if (event == Lifecycle.Event.ON_STOP) {
           isAppInBackground = true;
       } else if (event == Lifecycle.Event.ON_START) {
           isAppInBackground = false;
       }
    }

    public static boolean isAppInBackground() {
        return isAppInBackground;
    }
}