package com.example.cowin_booker;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

public class ForegroundService extends Service {
    //to show notification
    public void createNotificationChannel(){
        //we have to check if os is oreo or above
         // then we have to create notification channel
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.O){
            //this is how we create notification
            NotificationChannel notificationChannel = new NotificationChannel(
                    "ChannelId1","Foreground notification", NotificationManager.IMPORTANCE_DEFAULT
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(notificationChannel);


        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        createNotificationChannel();

        Intent intent1 = new Intent(this,MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this,0,intent1,0);

        Notification notification = new NotificationCompat.Builder(this,"ChannelId1").setContentText("COWIN-Booker is Running").setSmallIcon(R.mipmap.ic_launcher).setContentIntent(pendingIntent).build();

        startForeground(1,notification);
        Intent intents = new Intent(getBaseContext(), SessionFinderService.class);
        startService(intents);
        return START_STICKY;
    }
    //this function starts in the foreground service

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        stopForeground(true);
        stopSelf();
        super.onDestroy();
    }
}
