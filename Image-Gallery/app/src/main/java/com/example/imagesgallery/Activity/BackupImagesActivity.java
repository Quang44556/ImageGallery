package com.example.imagesgallery.Activity;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import com.example.imagesgallery.Fragment.ImageFragment;
import com.example.imagesgallery.R;
import com.example.imagesgallery.Utils.NetworkUtils;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class BackupImagesActivity extends AppCompatActivity {

    ImageFragment imageFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_backup_images);

        if (!NetworkUtils.isNetworkAvailable(getApplication())) {
            // ask users to connect to internet if they haven't connected
            MaterialAlertDialogBuilder dialogBuilder = new MaterialAlertDialogBuilder(BackupImagesActivity.this);
            dialogBuilder.setTitle("Please connect to internet");

            dialogBuilder.setPositiveButton("Ok", (dialog, id) -> {
                dialog.dismiss();
            });

            dialogBuilder.show();
        }

        imageFragment = new ImageFragment();
        getSupportFragmentManager().beginTransaction().replace(R.id.container, imageFragment).commit();
    }
}