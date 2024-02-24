package com.example.imagesgallery.Service;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.example.imagesgallery.MyChannel;
import com.example.imagesgallery.R;
import com.example.imagesgallery.Utils.Constants;

public class ServiceNotification extends Service {
    private final int MyServiceID = 300;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        int action = intent.getIntExtra("action", 0);
        sendNotification(action);
        return START_NOT_STICKY;
    }

    private void sendNotification(int action) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, MyChannel.MyChannelID);
        if (action == Constants.ACTION_UPLOADING) {
            builder.setSmallIcon(R.drawable.ic_launcher_foreground)
                    .setContentText("Uploading...")
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setProgress(0, 0, true)
                    .setOngoing(true);
        } else if (action == Constants.ACTION_UPLOAD_COMPLETE) {
            builder.setSmallIcon(R.drawable.ic_launcher_foreground)
                    .setContentText("Upload complete")
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setProgress(0, 0, false)
                    .setOngoing(false);
        } else if (action == Constants.ACTION_DOWNLOADING) {
            builder.setSmallIcon(R.drawable.ic_launcher_foreground)
                    .setContentText("Downloading...")
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setProgress(0, 0, true)
                    .setOngoing(true);
        } else if (action == Constants.ACTION_DOWNLOAD_COMPLETE) {
            builder.setSmallIcon(R.drawable.ic_launcher_foreground)
                    .setContentText("Download complete")
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setProgress(0, 0, false)
                    .setOngoing(false);
        }

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            builder.setForegroundServiceBehavior(Notification.FOREGROUND_SERVICE_IMMEDIATE);
        }

        startForeground(MyServiceID, builder.build());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopForeground(STOP_FOREGROUND_REMOVE);
    }
}
