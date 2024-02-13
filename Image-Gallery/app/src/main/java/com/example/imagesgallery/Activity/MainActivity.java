package com.example.imagesgallery.Activity;

import static com.example.imagesgallery.Database.SqliteDatabase.createDatabase;

import android.Manifest;
import android.app.Dialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.imagesgallery.Fragment.AlbumFragment;
import com.example.imagesgallery.Fragment.ImageFragment;
import com.example.imagesgallery.Fragment.PersonalFragment;
import com.example.imagesgallery.Model.Image;
import com.example.imagesgallery.R;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.io.File;
import java.util.ArrayList;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {
    BottomNavigationView bottomNavigationView;
    public static final String pathNoImage = "no_image";
    AlbumFragment albumFragment;
    ImageFragment imageFragment;
    PersonalFragment personalFragment;
    Dialog dialogNavBottom;
    Button btnFavoriteAlbums;
    Button btnFavoriteImages;
    private final int REQUEST_CODE = 10;

    // when user return back after go to setting to accept permission to access storage
    ActivityResultLauncher<Intent> launchSettings = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> checkPermission());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        checkPermission();
    }

    private void checkPermission() {
        ArrayList<String> permissionsToRequest = new ArrayList<>();
        String[] permissions;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
//            permissions = new String[]{Manifest.permission.READ_MEDIA_IMAGES,
//                    Manifest.permission.CAMERA};
            permissions = new String[]{Manifest.permission.READ_MEDIA_IMAGES};
            for (String permission : permissions) {
                if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                    permissionsToRequest.add(permission);
                }
            }
        } else {
//            permissions = new String[]{Manifest.permission.READ_EXTERNAL_STORAGE,
//                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
//                    Manifest.permission.CAMERA};
            permissions = new String[]{Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE};
            for (String permission : permissions) {
                if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                    permissionsToRequest.add(permission);
                }
            }
        }
        if (!permissionsToRequest.isEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsToRequest.toArray(new String[0]), REQUEST_CODE);
        } else {
            doNextTaskIfAllPermissionGranted();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        int totalPermissionGranted = 0;
        for (int i = 0; i < permissions.length; i++) {
            if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                boolean showRationale = shouldShowRequestPermissionRationale(permissions[i]);
                handlePermissionNotGranted(showRationale);
                break;
            } else {
                totalPermissionGranted++;
            }
        }
        if (totalPermissionGranted == permissions.length) {
            doNextTaskIfAllPermissionGranted();
        }
    }

    private void handlePermissionNotGranted(boolean showRationale) {
        // user decline to grant permission
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        if (!showRationale) {
            // user checked "don't ask again"
            builder.setMessage("Without permission to access your storage and camera, you are unable to use this app. " +
                    "You have previously declined these permissions. " +
                    "You must approve permissions in the app settings on your device.");

            // click yes
            builder.setPositiveButton("Setting", (dialog, id) -> {
                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                Uri uri = Uri.fromParts("package", getPackageName(), null);
                intent.setData(uri);
                launchSettings.launch(intent);
            });
            // click no
            builder.setNegativeButton("Quit", (dialog, id) -> finish());

            AlertDialog dialog = builder.create();
            dialog.show();

        } else {
            // user don't checked "don't ask again"
            builder.setMessage("Without this permission, you are unable to use this app. " +
                    "Are you sure you want to deny this permission ?");

            // click yes
            builder.setPositiveButton("Retry", (dialog, id) -> checkPermission());
            // click no
            builder.setNegativeButton("I'm sure", (dialog, id) -> finish());

            AlertDialog dialog = builder.create();
            dialog.show();
        }
    }

    private void doNextTaskIfAllPermissionGranted() {
        personalFragment = new PersonalFragment();
        imageFragment = new ImageFragment();
        albumFragment = new AlbumFragment();

        bottomNavigationView = findViewById(R.id.bottom_navigation);

        // create database
        File storagePath = getApplication().getFilesDir();
        createDatabase(storagePath);

        // Set default fragment when open app
        getSupportFragmentManager().beginTransaction().replace(R.id.container, imageFragment).commit();
        bottomNavigationView.setOnItemSelectedListener(item -> {
            if (item.getItemId() == R.id.album) {
                getSupportFragmentManager().beginTransaction().replace(R.id.container, albumFragment).commit();
                return true;
            } else if (item.getItemId() == R.id.image) {
                getSupportFragmentManager().beginTransaction().replace(R.id.container, imageFragment).commit();
                return true;
            } else if (item.getItemId() == R.id.personal) {
                getSupportFragmentManager().beginTransaction().replace(R.id.container, personalFragment).commit();
                return true;
            }
            return false;
        });
    }

    public void hideBottomNavigationView() {
        bottomNavigationView.setVisibility(View.GONE);
    }

    public void showBottomNavigationView() {
        bottomNavigationView.setVisibility(View.VISIBLE);
    }
}


