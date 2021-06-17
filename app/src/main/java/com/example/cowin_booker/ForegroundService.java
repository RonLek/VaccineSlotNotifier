package com.example.cowin_booker;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class ForegroundService extends JobService {

    private static final String TAG = "ExampleJobService";
    private boolean jobCancelled = false;
    String urlString;
    private static ProgressDialog mProgressDialog;

    @Override
    public boolean onStartJob(JobParameters params) {
        Log.d(TAG, "Job started");
        urlString = params.getExtras().getString("urlString");
        doBackgroundWork(params);
        return true;
    }

    private void doBackgroundWork(JobParameters params) {
        if (jobCancelled) {
            return;
        }
        createNotificationChannel();
        Intent intent1 = new Intent(ForegroundService.this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent1, 0);
        retrieveJSON(urlString, new CallBack2() {
            @Override
            public void onSuccess(String str) {
                Notification notification = new NotificationCompat.Builder(ForegroundService.this, "ChannelId1")
                        .setContentText("Available doses : " + str).setSmallIcon(R.mipmap.ic_launcher)
                        .setContentIntent(pendingIntent).build();
                startForeground(1, notification);
//                removeSimpleProgressDialog();
            }

            @Override
            public void onFail(String msg) {

//                removeSimpleProgressDialog();
            }
        });

    }

    public void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel notificationChannel = new NotificationChannel(
                    "ChannelId1", "Foreground notification", NotificationManager.IMPORTANCE_DEFAULT
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(notificationChannel);
        }
    }

    private void retrieveJSON(String urlString, ForegroundService.CallBack2 callBack2) {
        showSimpleProgressDialog(ForegroundService.this, "Loading...", "Starting Service", false);
        StringRequest stringRequest = new StringRequest(Request.Method.GET, urlString,
                response -> {
                    try {
                        JSONObject obj = new JSONObject(response);
                        JSONArray dataArray = obj.getJSONArray("sessions");
                        int doses = 0;
                        for (int i = 0; i < dataArray.length(); i++) {
                            JSONObject dataObj = dataArray.getJSONObject(i);
                            doses += Integer.parseInt(dataObj.getString("available_capacity"));
                        }
                        callBack2.onSuccess(doses + "");
                        removeSimpleProgressDialog();
                    } catch (JSONException e) {
                        e.printStackTrace();
                        callBack2.onFail(e.getMessage());
                        removeSimpleProgressDialog();
                    }
                },
                error -> {
                    Toast.makeText(getApplicationContext(), error.getMessage(), Toast.LENGTH_SHORT).show();
                    removeSimpleProgressDialog();
                });

        RequestQueue requestQueue = Volley.newRequestQueue(this);
        requestQueue.add(stringRequest);
    }

    @Override
    public boolean onStopJob(JobParameters jobParameters) {
        Log.d(TAG, "Job cancelled before completion");
        jobCancelled = true;
        return true;
    }

    public static void removeSimpleProgressDialog() {
        try {
            if (mProgressDialog != null) {
                if (mProgressDialog.isShowing()) {
                    mProgressDialog.dismiss();
                    mProgressDialog = null;
                }
            }
        } catch (Exception ie) {
            ie.printStackTrace();
        }
    }

    public static void showSimpleProgressDialog(Context context, String title,
                                                String msg, boolean isCancelable) {
        try {
            if (mProgressDialog == null) {
                mProgressDialog = ProgressDialog.show(context, title, msg);
                mProgressDialog.setCancelable(isCancelable);
            }

            if (!mProgressDialog.isShowing()) {
                mProgressDialog.show();
            }

        } catch (Exception ie) {
            ie.printStackTrace();
        }
    }

    public interface CallBack2 {
        void onSuccess(String str);

        void onFail(String msg);
    }
}
