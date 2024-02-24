package com.example.imagesgallery;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;

public class MyChannel extends Application {

    public static String MyChannelID = "ChannelID";

    @Override
    public void onCreate() {
        super.onCreate();

        createNotificationChannel();
    }

    private void createNotificationChannel() {
        CharSequence name = "Channel_Images_Gallery";
        int importance = NotificationManager.IMPORTANCE_DEFAULT;
        NotificationChannel channel = new NotificationChannel(MyChannelID, name, importance);
        // Register the channel with the system; you can't change the importance
        // or other notification behaviors after this
        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        notificationManager.createNotificationChannel(channel);
    }
}
