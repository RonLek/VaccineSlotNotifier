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
import android.os.Process;
import android.util.Log;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;


public class SessionFinderService extends Service {
    private Looper serviceLooper;
    private ServiceHandler serviceHandler;
    private RequestQueue queue;
    private String getSessionsByDistrictURL;
    private StringRequest getSessionsByDistrict;
    boolean isFound = false;

    // Handler that receives messages from the thread
    private final class ServiceHandler extends Handler {
        public ServiceHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            try {
                while (!isFound) {
                    Thread.sleep(5000);
                    queue = Volley.newRequestQueue(getApplicationContext());
                    getSessionsByDistrict = new StringRequest(Request.Method.GET, getSessionsByDistrictURL,
                            response -> {
                                try {
                                    JSONObject obj = new JSONObject(response);
                                    JSONArray dataArray = obj.getJSONArray("sessions");

                                    for (int i = 0; i < dataArray.length(); i++) {
                                        JSONObject dataObj = dataArray.getJSONObject(i);
                                        if(dataObj.getInt("available_capacity") > 0) {
                                            startForegroundService();
                                            isFound = true;
                                            break;
                                        }
                                    }

                                } catch (Throwable t) {
                                    Log.e("My App", "Could not parse malformed JSON: \"" + response + "\"");
                                }
                            }, error -> Log.e("Error", "That didn't work!")) {
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
        }
    }

    public void startForegroundService(){
        final Intent intent1 = new Intent(this, ForegroundService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent1);
        } else {
            startService(intent1);
        }
    }

    @Override
    public void onCreate() {
        HandlerThread thread = new HandlerThread("ServiceStartArguments", Process.THREAD_PRIORITY_BACKGROUND);
        thread.start();

        serviceLooper = thread.getLooper();
        serviceHandler = new ServiceHandler(serviceLooper);

        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Toast.makeText(this, "service starting", Toast.LENGTH_SHORT).show();
        Message msg = serviceHandler.obtainMessage();
        msg.arg1 = startId;
        serviceHandler.sendMessage(msg);
        getSessionsByDistrictURL = intent.getStringExtra("urlString");

        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        Toast.makeText(this, "service done", Toast.LENGTH_SHORT).show();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "VaccineNotifier";
            String description = "Notifies about available vaccines";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel("vaccinefound", name, importance);
            channel.setDescription(description);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }
}