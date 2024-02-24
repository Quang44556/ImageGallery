package com.example.imagesgallery.Activity;

import static com.example.imagesgallery.Database.SqliteDatabase.createDatabase;

import android.Manifest;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.example.imagesgallery.Fragment.AlbumFragment;
import com.example.imagesgallery.Fragment.ImageFragment;
import com.example.imagesgallery.Fragment.PersonalFragment;
import com.example.imagesgallery.R;
import com.example.imagesgallery.Utils.NetworkUtils;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.util.ArrayList;
import java.util.Objects;

import de.hdodenhof.circleimageview.CircleImageView;

public class MainActivity extends AppCompatActivity {
    BottomNavigationView bottomNavigationView;
    public static final String pathNoImage = "no_image";
    AlbumFragment albumFragment;
    ImageFragment imageFragment;
    PersonalFragment personalFragment;
    CircleImageView imgButtonUserProfile;
    LinearLayout linearLayoutTitle;
    GoogleSignInClient client;
    boolean isLoggedIn = false;
    public boolean isEnableBackup = false;
    Dialog dialog;
    TextView txtEmail, txtName, txtSignIn;
    Button btnSignOut, btnBackup;
    ImageView imgUserAvatar;
    GoogleSignInOptions gso;
    StorageReference storageRef;
    public FirebaseAuth mAuth;

    // when user return back after go to setting to accept permission to access storage
    ActivityResultLauncher<Intent> launchSettings = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> checkPermission());

    ActivityResultLauncher<Intent> launchSignIn = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    Intent data = result.getData();
                    if (data != null) {
                        Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
                        try {
                            GoogleSignInAccount account = task.getResult(ApiException.class);
                            firebaseAuth(account.getIdToken());
                        } catch (Exception e) {
                            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            });

    @Override
    protected void onStart() {
        super.onStart();

        if (mAuth != null) {
            FirebaseUser user = mAuth.getCurrentUser();
            if (user != null) {
                isLoggedIn = true;
            }
        }
    }

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
            permissions = new String[]{Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.POST_NOTIFICATIONS};
            for (String permission : permissions) {
                if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                    permissionsToRequest.add(permission);
                }
            }
        } else {
            permissions = new String[]{Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE};
            for (String permission : permissions) {
                if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                    permissionsToRequest.add(permission);
                }
            }
        }
        if (!permissionsToRequest.isEmpty()) {
            int REQUEST_CODE = 10;
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
        MaterialAlertDialogBuilder dialogBuilder = new MaterialAlertDialogBuilder(this);
        if (!showRationale) {
            // user checked "don't ask again"
            dialogBuilder.setMessage("Without permission to access your storage and camera, you are unable to use this app. " +
                    "You have previously declined these permissions. " +
                    "You must approve permissions in the app settings on your device.");

            // click yes
            dialogBuilder.setPositiveButton("Setting", (dialog, id) -> {
                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                Uri uri = Uri.fromParts("package", getPackageName(), null);
                intent.setData(uri);
                launchSettings.launch(intent);
            });
            // click no
            dialogBuilder.setNegativeButton("Quit", (dialog, id) -> finish());

            dialogBuilder.show();

        } else {
            // user don't checked "don't ask again"
            dialogBuilder.setMessage("Without this permission, you are unable to use this app. " +
                    "Are you sure you want to deny this permission ?");

            // click yes
            dialogBuilder.setPositiveButton("Retry", (dialog, id) -> checkPermission());
            // click no
            dialogBuilder.setNegativeButton("I'm sure", (dialog, id) -> finish());

            dialogBuilder.show();
        }
    }

    private void doNextTaskIfAllPermissionGranted() {
        bottomNavigationView = findViewById(R.id.bottom_navigation);
        imgButtonUserProfile = findViewById(R.id.imgButtonUserProfile);
        txtSignIn = findViewById(R.id.txtSignIn);
        linearLayoutTitle = findViewById(R.id.LinearLayoutTitle);

        SharedPreferences sharedPref = getSharedPreferences(getString(R.string.NamePreferences), Context.MODE_PRIVATE);
        isEnableBackup = sharedPref.getBoolean(getString(R.string.enable_backup_key), false);

        personalFragment = new PersonalFragment();
        imageFragment = new ImageFragment();
        albumFragment = new AlbumFragment();

        // Set default fragment when open app
        getSupportFragmentManager().beginTransaction().replace(R.id.container, imageFragment).commit();
        bottomNavigationView.setOnItemSelectedListener(item -> {
            if (item.getItemId() == R.id.bottom_album) {
                getSupportFragmentManager().beginTransaction().replace(R.id.container, albumFragment).commit();
                return true;
            } else if (item.getItemId() == R.id.bottom_image) {
                getSupportFragmentManager().beginTransaction().replace(R.id.container, imageFragment).commit();
                return true;
            } else if (item.getItemId() == R.id.bottom_personal) {
                getSupportFragmentManager().beginTransaction().replace(R.id.container, personalFragment).commit();
                return true;
            }
            return false;
        });

        // create database
        File storagePath = getApplication().getFilesDir();
        createDatabase(storagePath);

        // set up firebase auth and google sign-in
        mAuth = FirebaseAuth.getInstance();
        gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        client = GoogleSignIn.getClient(this, gso);

        // check if user already logged in
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            isLoggedIn = true;
        }

        txtSignIn.setOnClickListener(view -> {
            if (NetworkUtils.isNetworkAvailable(getApplication())) { // if user connected to internet
                // sign in google
                googleSignIn();
            } else {
                // ask user to connect to internet
                MaterialAlertDialogBuilder dialogBuilder = new MaterialAlertDialogBuilder(MainActivity.this);
                dialogBuilder.setTitle("Please connect to internet to sign in");

                // click ok to dismiss dialog
                dialogBuilder.setPositiveButton("Ok", (dialog, id) -> dialog.dismiss());

                dialogBuilder.show();
            }
        });

        // set up firebase storage
        storageRef = FirebaseStorage.getInstance().getReferenceFromUrl(getString(R.string.folder_path_storage));

        imgButtonUserProfile.setOnClickListener(view -> showUserProfile());
    }

    private void googleSignIn() {
        Intent intent = client.getSignInIntent();
        launchSignIn.launch(intent);
    }

    private void firebaseAuth(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                isLoggedIn = true;
                showLinearLayoutTitle();
            }
        });
    }

    private void showUserProfile() {
        dialog = new Dialog(MainActivity.this);
        dialog.setContentView(R.layout.dialog_user_profile);

        txtEmail = dialog.findViewById(R.id.txtEmailUser);
        txtName = dialog.findViewById(R.id.txtNameUser);
        btnSignOut = dialog.findViewById(R.id.btnSignOut);
        imgUserAvatar = dialog.findViewById(R.id.imgUserAvatar);
        btnBackup = dialog.findViewById(R.id.btnBackup);

        // display user information
        Uri photoUrl = null;
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            txtEmail.setText(user.getEmail());
            txtName.setText(user.getDisplayName());
            photoUrl = user.getPhotoUrl();
        }

        // display user avatar
        if (photoUrl != null) {
            Glide.with(MainActivity.this)
                    .load(photoUrl)
                    .error(R.drawable.user_profile)
                    .into(imgUserAvatar);
        }

        // display backup button
        if (isEnableBackup) {
            btnBackup.setText(R.string.DisableBackupTitle);
        } else {
            btnBackup.setText(R.string.EnableBackupTitle);
        }

        btnSignOut.setOnClickListener(view -> GoogleSignOut());

        btnBackup.setOnClickListener(view -> clickBackup());

        dialog.show();
        resizeDialog();
    }

    private void GoogleSignOut() {
        mAuth.signOut();
        isLoggedIn = false;
        dialog.dismiss();
        client.revokeAccess();
        showLinearLayoutTitle();
    }

    private void clickBackup() {
        isEnableBackup = !isEnableBackup;

        // save to SharedPreferences
        SharedPreferences sharedPref = getSharedPreferences(getString(R.string.NamePreferences), Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putBoolean(getString(R.string.enable_backup_key), isEnableBackup);
        editor.apply();

        // display backup button
        if (isEnableBackup) {
            btnBackup.setText(R.string.DisableBackupTitle);
        } else {
            btnBackup.setText(R.string.EnableBackupTitle);
        }
    }

    private void resizeDialog() {
        // resize dialog size
        int width = getApplicationContext().getResources().getDisplayMetrics().widthPixels;
        Objects.requireNonNull(dialog.getWindow()).setLayout((6 * width) / 7, ViewGroup.LayoutParams.WRAP_CONTENT);

        // get screen size
        DisplayMetrics displayMetrics = Resources.getSystem().getDisplayMetrics();
        float screenWidth = displayMetrics.widthPixels;

        // resize text size
        float newTextSize = screenWidth * 0.05f;
        txtName.setTextSize(TypedValue.COMPLEX_UNIT_PX, newTextSize);
        newTextSize = screenWidth * 0.04f;
        txtEmail.setTextSize(TypedValue.COMPLEX_UNIT_PX, newTextSize);

        // resize button
        newTextSize = screenWidth * 0.05f;
        btnSignOut.setTextSize(TypedValue.COMPLEX_UNIT_PX, newTextSize);

        // resize imageView
        newTextSize = screenWidth * 0.2f;
        ViewGroup.LayoutParams layoutParams = imgUserAvatar.getLayoutParams();
        layoutParams.width = (int) newTextSize;
        layoutParams.height = (int) newTextSize;
        imgUserAvatar.setLayoutParams(layoutParams);
    }

    public void hideBottomNavigationView() {
        bottomNavigationView.setVisibility(View.GONE);
    }

    public void showBottomNavigationView() {
        bottomNavigationView.setVisibility(View.VISIBLE);
    }

    public void hideLinearLayoutTitle() {
        linearLayoutTitle.setVisibility(View.GONE);
    }

    public void showLinearLayoutTitle() {
        linearLayoutTitle.setVisibility(View.VISIBLE);

        if (isLoggedIn) {
            imgButtonUserProfile.setVisibility(View.VISIBLE);
            txtSignIn.setVisibility(View.GONE);

            // display user avatar
            FirebaseUser user = mAuth.getCurrentUser();
            if (user != null) {
                Uri photoUrl = user.getPhotoUrl();
                if (photoUrl != null) {
                    Glide.with(MainActivity.this)
                            .load(photoUrl)
                            .error(R.drawable.user_profile)
                            .into(imgButtonUserProfile);
                }
            }

        } else {
            imgButtonUserProfile.setVisibility(View.GONE);
            txtSignIn.setVisibility(View.VISIBLE);
        }
    }
}


