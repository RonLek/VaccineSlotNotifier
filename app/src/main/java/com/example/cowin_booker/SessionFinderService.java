package com.example.cowin_booker;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import android.os.Process;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import static android.content.Intent.getIntent;


public class SessionFinderService extends Service {
    private Looper serviceLooper;
    private ServiceHandler serviceHandler;
    private RequestQueue queue;
    private String getSessionsByDistrictURL;
    private StringRequest getSessionsByDistrict;
    int i = 0;


    // Handler that receives messages from the thread
    private final class ServiceHandler extends Handler {
        public ServiceHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            try {
                while (true) {
                    i++;
                    Thread.sleep(5000);
                    Log.d("Message", "Within service message");
                    // Instantiate the RequestQueue.
                    queue = Volley.newRequestQueue(getApplicationContext());
                    getSessionsByDistrictURL = "https://cdn-api.co-vin.in/api/v2/appointment/sessions/public/findByDistrict?district_id=363&date=12-05-2021";

                    // Request a string response from the provided URL.
                    getSessionsByDistrict = new StringRequest(Request.Method.GET, getSessionsByDistrictURL,
                            new Response.Listener<String>() {
                                @Override
                                public void onResponse(String response) {
                                    try {

                                        JSONArray sessionsResult = (new JSONObject(response)).getJSONArray("sessions");

                                        Log.d("Results", sessionsResult.toString());
                                        if (sessionsResult.length() > 0) {
                                            String centers = "";
                                            for (int j = 0; j < sessionsResult.length(); j++) {
                                                centers.concat(sessionsResult.getJSONObject(j).get("name").toString() + "");
                                            }
                                            NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(), "vaccinefound")
                                                    .setSmallIcon(R.drawable.ic_launcher_foreground)
                                                    .setContentTitle("Session Found")
                                                    .setPriority(NotificationCompat.PRIORITY_MAX)
                                                    .setStyle(new NotificationCompat.BigTextStyle()
                                                            .bigText(centers));

                                            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(getApplicationContext());

                                            // notificationId is a unique int for each notification that you must define
                                            notificationManager.notify(12, builder.build());
                                        }

                                    } catch (Throwable t) {
                                        Log.e("My App", "Could not parse malformed JSON: \"" + response + "\"");
                                    }
                                }
                            }, new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            Log.e("Error", "That didn't work!");
                        }

                    }) {
                        @Override
                        public Map<String, String> getHeaders() throws AuthFailureError {
                            Map<String, String> params = new HashMap<String, String>();
                            params.put("User-Agent", "Nintendo Gameboy");
                            params.put("Accept-Language", "en_US");
                            params.put("Host", "cdn-api.co-vin.in");
                            params.put("Accept", "application/json");

                            return params;
                        }
                    };

                    queue.add(getSessionsByDistrict);
                }
            } catch (InterruptedException e) {
                // Restore interrupt status.
                Thread.currentThread().interrupt();
            }
            // Stop the service using the startId, so that we don't stop
            // the service in the middle of handling another job
//            stopSelf(msg.arg1);
        }
    }

    @Override
    public void onCreate() {

        // Start up the thread running the service. Note that we create a
        // separate thread because the service normally runs in the process's
        // main thread, which we don't want to block. We also make it
        // background priority so CPU-intensive work doesn't disrupt our UI.
        HandlerThread thread = new HandlerThread("ServiceStartArguments",
                Process.THREAD_PRIORITY_BACKGROUND);
        thread.start();

        // Get the HandlerThread's Looper and use it for our Handler
        serviceLooper = thread.getLooper();
        serviceHandler = new ServiceHandler(serviceLooper);

        createNotificationChannel();

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Toast.makeText(this, "service starting", Toast.LENGTH_SHORT).show();

        // For each start request, send a message to start a job and deliver the
        // start ID so we know which request we're stopping when we finish the job
        Message msg = serviceHandler.obtainMessage();
        msg.arg1 = startId;
        serviceHandler.sendMessage(msg);

        // If we get killed, after returning from here, restart
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        // We don't provide binding, so return null
        return null;
    }

    @Override
    public void onDestroy() {
        Toast.makeText(this, "service done", Toast.LENGTH_SHORT).show();
    }

    private void createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "VaccineNotifier";
            String description = "Notifies about available vaccines";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel("vaccinefound", name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }
}