package com.thoughtpearl.conveyance.crashlytics;

import android.os.Looper;
import android.widget.Toast;
import android.content.Context;
import android.os.Process;


public class MyExceptionHandler implements Thread.UncaughtExceptionHandler {
    private final Context context;

    public MyExceptionHandler(Context context) {
        this.context = context;
    }

    @Override
    public void uncaughtException(Thread thread, Throwable ex) {
        // Log the exception details
        ex.printStackTrace();

        // Show a toast message with crash details
        new Thread() {
            @Override
            public void run() {
                Looper.prepare();
                Toast.makeText(context, "Oops! Something went wrong. The app will now exit.", Toast.LENGTH_LONG).show();
                Looper.loop();
            }
        }.start();

        // Sleep for a while to allow the toast message to be displayed
        try {
            Thread.sleep(4000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Terminate the app
        Process.killProcess(Process.myPid());
        System.exit(1);
    }
}