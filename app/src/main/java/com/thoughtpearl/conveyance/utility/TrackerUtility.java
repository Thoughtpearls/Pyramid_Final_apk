package com.thoughtpearl.conveyance.utility;

import static java.io.File.createTempFile;

import android.Manifest;
import android.app.ActivityManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.icu.text.DateFormat;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Environment;
import android.provider.Settings;
import android.util.Base64;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;

import androidx.core.content.ContentResolverCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.thoughtpearl.conveyance.LocationApp;
import com.thoughtpearl.conveyance.api.response.LocationRequest;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.math.RoundingMode;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.ByteBuffer;
import java.security.Permission;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import pub.devrel.easypermissions.EasyPermissions;

public class TrackerUtility {
    public static boolean hasLocationPermissions(Context context) {
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
           return EasyPermissions.hasPermissions(
                    context,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            );
        } else {
          return EasyPermissions.hasPermissions(
                    context,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION
            );
        }
    }

    public static boolean isPermissionGranted(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return Environment.isExternalStorageManager();
        } else {
            int readStorage = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE);
           return  readStorage == PackageManager.PERMISSION_GRANTED;
        }
    }

   /**
     * Convert a millisecond duration to a string format
     *
     * @param millis A duration to convert to a string form
     * @return A string of the form "X Days Y Hours Z Minutes A Seconds".
     */
    public static String getDurationBreakdown(long millis) {
        if(millis < 0) {
            throw new IllegalArgumentException("Duration must be greater than zero!");
        }

        long hours = TimeUnit.MILLISECONDS.toHours(millis);
        millis -= TimeUnit.HOURS.toMillis(hours);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis);
        millis -= TimeUnit.MINUTES.toMillis(minutes);
        long seconds = TimeUnit.MILLISECONDS.toSeconds(millis);

        StringBuilder sb = new StringBuilder(64);
        if (hours > 0) {
            sb.append(hours);
            sb.append("hr");
        }

        if (minutes > 0) {
            sb.append(minutes);
            sb.append("min ");
        }

        if (seconds > 0) {
            sb.append(seconds);
            sb.append("sec");
        }

        return(sb.toString());
    }
 
    public static String getDeviceId(Context context) {
        if (LocationApp.DEVICE_ID == null) {
            String android_id = Settings.Secure.getString(context.getContentResolver(),
                    Settings.Secure.ANDROID_ID);
            LocationApp.logs("TRIP", "Before Android ID : " + android_id);
            //android_id = "16fb058fe8efb57d";
            //android_id = "89ABCDEF-01234567-89ABCDEF";
            android_id = "555113d4af5795a2";
            //android_id = "08753888962421ba";
            //android_id = "8bbc400b0285eaad";
            //android_id = "927f80c1e257cf9f";
            LocationApp.logs("TRIP", "After Android ID : " + android_id);
            LocationApp.DEVICE_ID = android_id;
            return android_id;
        } else {
            return LocationApp.DEVICE_ID;
        }
    }

    public static boolean checkPlayServicesAvailable(Context context) {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        int resultCode = apiAvailability.isGooglePlayServicesAvailable(context);
        if (resultCode != ConnectionResult.SUCCESS) {
            /*if (apiAvailability.isUserResolvableError(resultCode)) {
                apiAvailability.getErrorDialog(this, resultCode, PLAY_SERVICES_RESOLUTION_REQUEST)
                        .show();
            } else {
                Log.i(TAG, "This device is not supported.");
                finish();
            }*/
            return false;
        }
        return true;
    }

    public static Bitmap getBitmapFromURL(String strURL) {
        try {
            URL url = new URL(strURL);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoInput(true);
            connection.connect();
            InputStream input = connection.getInputStream();
            Bitmap myBitmap = BitmapFactory.decodeStream(input);
            return myBitmap;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * CHECK WHETHER INTERNET CONNECTION IS AVAILABLE OR NOT
     */
    public static boolean checkConnection(Context context) {
        final ConnectivityManager connMgr = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        if (connMgr != null) {
            NetworkInfo activeNetworkInfo = connMgr.getActiveNetworkInfo();

            if (activeNetworkInfo != null) { // connected to the internet
                // connected to the mobile provider's data plan
                if (activeNetworkInfo.getType() == ConnectivityManager.TYPE_WIFI) {
                    // connected to wifi
                    return true;
                } else return activeNetworkInfo.getType() == ConnectivityManager.TYPE_MOBILE;
            }
        }
        return false;
    }

    public static String convertToObject(InputStream inputStream) {
        String objectString = null;
        try {
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(
                            inputStream, "iso-8859-1"), 8);
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
            inputStream.close();
            objectString = sb.toString();
        } catch (Exception e) {
            Log.e("Buffer Error", "Error converting result " + e.toString());
        }
        return objectString;
    }

    public static String getDateString(Date myDate) {
        String datePattern = "yyyy-MM-dd";
        return new SimpleDateFormat(datePattern).format(myDate);
    }

    public static String getDateString(Date myDate, String datePattern) {
        return new SimpleDateFormat(datePattern).format(myDate);

    }

    public static String getTimeString(Date myDate) {
        String timePattern = "HH:mm:ss";
        return new SimpleDateFormat(timePattern).format(myDate);
    }

    public static Date convertStringToDate(String dateInString) {
        return convertStringToDate(dateInString, "yyyy-M-d");
    }

    public static Date convertStringToDate(String dateInString, String pattern) {
        SimpleDateFormat formatter = new SimpleDateFormat(pattern, Locale.ENGLISH);
        try {
            return formatter.parse(dateInString);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return new Date();
    }

    public static Bitmap loadBitmapFromView(Context context, View v) {
        DisplayMetrics dm = context.getResources().getDisplayMetrics();
        v.measure(View.MeasureSpec.makeMeasureSpec(dm.widthPixels, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(dm.heightPixels, View.MeasureSpec.EXACTLY));
        v.layout(0, 0, v.getMeasuredWidth(), v.getMeasuredHeight());
        Bitmap returnedBitmap = Bitmap.createBitmap(v.getMeasuredWidth(),
                v.getMeasuredHeight(), Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(returnedBitmap);
        v.draw(c);

        return returnedBitmap;
    }

    public static File takeScreen(Context context, View view, Bitmap bitmap) {
        //Bitmap bitmap = loadBitmapFromView(context, view);
        //String mPath = Environment.getExternalStorageDirectory() + File.separator + "" + System.currentTimeMillis() + ".jpeg";
        File imageFile = createImageFile(context);
        OutputStream fout = null;
        try {
            fout = new FileOutputStream(imageFile);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fout);
            fout.flush();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (fout != null) {
                try {
                    fout.close();
                } catch (IOException exception){
                    exception.printStackTrace();
                }
            }
        }
        return imageFile;
    }

    public static File createImageFile(Context context) {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "screen_" + timeStamp + "_";
        File storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = null;
        try {
            image = createTempFile(
                    imageFileName, /* prefix */
                    ".jpg", /* suffix */
                    storageDir      /* directory */
            );
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Save a file: path for use with ACTION_VIEW intents
        //mCurrentPhotoPath = image.getAbsolutePath();
        return image;
    }

    public static String roundOffDoubleToString(Double totalDistance) {
        try {
            NumberFormat formatter = NumberFormat.getInstance(Locale.US);
            formatter.setMaximumFractionDigits(2);
            formatter.setMinimumFractionDigits(2);
            formatter.setRoundingMode(RoundingMode.HALF_UP);
            return formatter.format(totalDistance);
        } catch (Exception e) {
            LocationApp.logs("TRIP", "error :" + e.getMessage());
        }
        return "0";
    }

    public static Double roundOffDouble(Double totalDistance) {
        return Double.valueOf(roundOffDoubleToString(totalDistance).replaceAll("[^0-9.]+",""));
    }

    public static Double calculateDistanceInMeter(List<LocationRequest> locationRequestList) {
        Double distance = 0d;
        if (locationRequestList == null || locationRequestList.size() == 0) {
            return distance;
        }

        if (locationRequestList.size() == 2) {
            LocationRequest pos1 = locationRequestList.get(0);
            LocationRequest pos2 = locationRequestList.get(1);
            //distance += calculateDistance(pos1.getLatitude(), pos1.getLongitude(), pos2.getLatitude(), pos2.getLongitude(), "M");
            float[] result = new float[1];
            Location.distanceBetween(pos1.getLatitude(), pos1.getLongitude(), pos2.getLatitude(), pos2.getLongitude(), result);
            long l = convertStringToDate(pos2.getTimeStamp(), "HH:mm:ss").getTime() - convertStringToDate(pos1.getTimeStamp(), "HH:mm:ss").getTime();
            LocationApp.logs("TRIP", "location1(" + pos1.getLatitude() + "," + pos1.getLongitude() + ") and location2(" + pos2.getLatitude() + "," + pos2.getLongitude() + ") between : 0 and  1" +  " is -> " + result[0] + " loc1 time" + pos1.getTimeStamp() + "loc2 time:" + pos2.getTimeStamp() +" diff is (seconds) :" + TimeUnit.MILLISECONDS.toSeconds(l));
            if (result[0] > 2000 && TimeUnit.MILLISECONDS.toMinutes(l) < 2) {
                LocationApp.logs("TRIP", "Rejected records :  i -> 0  i+1-> 1");
            } else {
                distance += result[0];
            }
        }

        for(int i = 0; i < locationRequestList.size() - 2; i++) {
            LocationRequest pos1 = locationRequestList.get(i);
            LocationRequest pos2 = locationRequestList.get(i + 1);
            //distance += calculateDistance(pos1.getLatitude(), pos1.getLongitude(), pos2.getLatitude(), pos2.getLongitude(), "M");
            float[] result = new float[1];
            Location.distanceBetween(pos1.getLatitude(), pos1.getLongitude(), pos2.getLatitude(), pos2.getLongitude(), result);
            long l = convertStringToDate(pos2.getTimeStamp(), "HH:mm:ss").getTime() - convertStringToDate(pos1.getTimeStamp(), "HH:mm:ss").getTime();
            LocationApp.logs("TRIP", "location1(" + pos1.getLatitude() + "," + pos1.getLongitude() + ") and location2(" + pos2.getLatitude() + "," + pos2.getLongitude() + ") between :" + i + " and " + (i + 1) + " is -> " + result[0] + " loc1 time" + pos1.getTimeStamp() + "loc2 time:" + pos2.getTimeStamp() +" diff is (seconds) :" + TimeUnit.MILLISECONDS.toSeconds(l));
            if (result[0] > 2000 && TimeUnit.MILLISECONDS.toMinutes(l) < 2) {
                LocationApp.logs("TRIP", "Rejected records :  i -> " + i + " i+1-> " + (i+1));
            } else {
                distance += result[0];
            }
        }
        return distance;
    }

    public static Double calculateDistanceInKilometer(List<com.thoughtpearl.conveyance.respository.entity.Location> locationList) {
        Double distance = 0d;
        if (locationList == null || locationList.size() == 0) {
            return distance;
        }
        for(int i = 0; i < locationList.size() - 2; i++) {
            com.thoughtpearl.conveyance.respository.entity.Location pos1 = locationList.get(i);
            com.thoughtpearl.conveyance.respository.entity.Location pos2 = locationList.get(i + 1);
            //distance += calculateDistance(pos1.getLatitude(), pos1.getLongitude(), pos2.getLatitude(), pos2.getLongitude(), "M");
            float[] result = new float[1];
            Location.distanceBetween(pos1.getLatitude(), pos1.getLongitude(), pos2.getLatitude(), pos2.getLongitude(), result);
            distance += result[0];
        }
        return distance / 1000d;
    }

    public static boolean isMyServiceRunning(Class<?> serviceClass, Context context) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    public static double calculateDistance(double lat1, double lon1, double lat2, double lon2, String unit)
    {
        double theta = lon1 - lon2;
        double dist = Math.sin(deg2rad(lat1)) * Math.sin(deg2rad(lat2)) + Math.cos(deg2rad(lat1)) * Math.cos(deg2rad(lat2)) * Math.cos(deg2rad(theta));
        dist = Math.acos(dist);
        dist = rad2deg(dist);
        dist = dist * 60 * 1.1515;
        if (unit == "K") {
            dist = dist * 1.609344;
        } else if (unit == "N") {
            dist = dist * 0.8684;
        }
        return (dist);
    }

    /*:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::*/
    /*::  This function converts decimal degrees to radians             :*/
    /*:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::*/
    private static double deg2rad(double deg)
    {
        return (deg * Math.PI / 180.0);
    }

    /*:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::*/
    /*::  This function converts radians to decimal degrees             :*/
    /*:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::*/
    private static double rad2deg(double rad)
    {
        return (rad * 180.0 / Math.PI);
    }

     //System.out.println(getWeekends(2));// All weekends of Feb in the current year
     //System.out.println(getWeekends(2020, 2));// All weekends of Feb 2020

    /*
     * All weekends (Sat & Sun) of the given month in the current year
     */
    public static List<Date> getWeekends(int month) {
        LocalDate firstDateOfTheMonth = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            firstDateOfTheMonth = LocalDate.now().withMonth(month).with(TemporalAdjusters.firstDayOfMonth());
        }
        List<Date> list = new ArrayList<>();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            for (LocalDate date = firstDateOfTheMonth; !date
                    .isAfter(firstDateOfTheMonth.with(TemporalAdjusters.lastDayOfMonth())); date = date.plusDays(1))
                if (date.getDayOfWeek() == DayOfWeek.SUNDAY)
                    list.add(Date.from(date.atStartOfDay(ZoneId.systemDefault()).toInstant()));
        }

        return list;
    }

    public static List<Date> getAllWeekends() {
        List<Date> list = new ArrayList<>();

        LocalDate firstDateOfTheMonth = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            firstDateOfTheMonth = LocalDate.now().withMonth(1).with(TemporalAdjusters.firstDayOfMonth());
        }

        Date firstWeekendOfTheMonth = null;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            for (LocalDate date = firstDateOfTheMonth; !date
                    .isAfter(firstDateOfTheMonth.with(TemporalAdjusters.lastDayOfMonth())); date = date.plusDays(1))
                if (date.getDayOfWeek() == DayOfWeek.SUNDAY) {
                    firstWeekendOfTheMonth = Date.from(date.atStartOfDay(ZoneId.systemDefault()).toInstant());
                    break;
                }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            for (LocalDate date = TrackerUtility.asLocalDate(firstWeekendOfTheMonth); date.getYear() == LocalDate.now().getYear(); date = date.plusDays(7))
                if (date.getDayOfWeek() == DayOfWeek.SUNDAY) {
                    list.add(Date.from(date.atStartOfDay(ZoneId.systemDefault()).toInstant()));
                }
        }

        return list;
    }

    /*
     * All weekends (Sat & Sun) of the given year and the month
     */
    public static List<Date> getWeekends(int year, int month) {
        LocalDate firstDateOfTheMonth = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            firstDateOfTheMonth = YearMonth.of(year, month).atDay(1);
        }
        List<Date> list = new ArrayList<>();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            for (LocalDate date = firstDateOfTheMonth; !date
                    .isAfter(firstDateOfTheMonth.with(TemporalAdjusters.lastDayOfMonth())); date = date.plusDays(1))
                if (date.getDayOfWeek() == DayOfWeek.SUNDAY)
                    list.add(Date.from(date.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant()));
        }

        return list;
    }

    public static Date asDate(LocalDate localDate) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return Date.from(localDate.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant());
        }
        return new Date();
    }

    public static Date asDate(LocalDateTime localDateTime) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant());
        }
        return new Date();
    }

    public static LocalDate asLocalDate(Date date) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return Instant.ofEpochMilli(date.getTime()).atZone(ZoneId.systemDefault()).toLocalDate();
        }
        return null;
    }

    public static LocalDateTime asLocalDateTime(Date date) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return Instant.ofEpochMilli(date.getTime()).atZone(ZoneId.systemDefault()).toLocalDateTime();
        }
        return null;
    }

    public static boolean isDeveloperModeEnabled(Context context) {
         /*if (Integer.valueOf(Build.VERSION.SDK_INT) >= 17) {
            return android.provider.Settings.Secure.getInt(context.getContentResolver(),
                    android.provider.Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 0) != 0;
         }*/
        return false;
    }

    /**
     * @return Application's version code from the PackageManager.
     */
    public static int getAppVersionCode(Context context) {
        try {
            PackageInfo packageInfo = context.getPackageManager()
                    .getPackageInfo(context.getPackageName(), 0);
            return packageInfo.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            // should never happen
            //throw new RuntimeException("Could not get package name: " + e);
        }
        return 0;
    }

    public static  boolean isGpsEnabled(Context context) {
        ContentResolver contentResolver = context.getContentResolver();
        // Find out what the settings say about which providers are enabled
        //  String locationMode = "Settings.Secure.LOCATION_MODE_OFF";
        int mode = Settings.Secure.getInt(
                contentResolver, Settings.Secure.LOCATION_MODE, Settings.Secure.LOCATION_MODE_OFF);
        if (mode != Settings.Secure.LOCATION_MODE_OFF) {
            return true;
               /* if (mode == Settings.Secure.LOCATION_MODE_HIGH_ACCURACY) {
                    locationMode = "High accuracy. Uses GPS, Wi-Fi, and mobile networks to determine location";
                } else if (mode == Settings.Secure.LOCATION_MODE_SENSORS_ONLY) {
                    locationMode = "Device only. Uses GPS to determine location";
                } else if (mode == Settings.Secure.LOCATION_MODE_BATTERY_SAVING) {
                    locationMode = "Battery saving. Uses Wi-Fi and mobile networks to determine location";
                }*/
        } else {
            return false;
        }
    }

    public static byte[] convertBitmapToByteArray(Bitmap bitmap){
        ByteBuffer byteBuffer = ByteBuffer.allocate(bitmap.getByteCount());
        bitmap.copyPixelsToBuffer(byteBuffer);
        byteBuffer.rewind();
        return byteBuffer.array();
    }

    /**
     * @param bitmap
     * @return converting bitmap and return a string
     */
    public static String BitMapToString(Bitmap bitmap) {
        ByteArrayOutputStream baos=new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG,100, baos);
        byte [] b=baos.toByteArray();
        String temp= Base64.encodeToString(b, Base64.DEFAULT);
        return temp;
    }

    /**
     * @param encodedString
     * @return bitmap (from given string)
     */
    public static Bitmap StringToBitMap(String encodedString){
        try{
            byte [] encodeByte=Base64.decode(encodedString,Base64.DEFAULT);
            Bitmap bitmap= BitmapFactory.decodeByteArray(encodeByte, 0, encodeByte.length);
            return bitmap;
        }catch(Exception e){
            e.getMessage();
            return null;
        }
    }
}
