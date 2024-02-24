package com.example.imagesgallery.Activity;

import static com.example.imagesgallery.Database.SqliteDatabase.delete;
import static com.example.imagesgallery.Database.SqliteDatabase.removeImageFromAlbum;
import static com.example.imagesgallery.Database.SqliteDatabase.update;
import static com.example.imagesgallery.Utils.FileHelper.copyFileToExternalStorage;
import static com.example.imagesgallery.Utils.PathUtils.getUriFromPath;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.RecoverableSecurityException;
import android.app.WallpaperManager;
import android.content.Intent;
import android.content.IntentSender;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.IntentSenderRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.NotificationCompat;
import androidx.core.content.FileProvider;
import androidx.viewpager2.widget.ViewPager2;

import com.example.imagesgallery.Adapter.ImageViewPager2Adapter;
import com.example.imagesgallery.Model.Image;
import com.example.imagesgallery.MyChannel;
import com.example.imagesgallery.R;
import com.example.imagesgallery.DepthPageTransformer;
import com.example.imagesgallery.Service.ServiceNotification;
import com.example.imagesgallery.Utils.Constants;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Objects;

public class ImageInfoActivity extends AppCompatActivity {
    ArrayList<Image> imageArrayList;
    int currentIndex;
    Image image;
    private ActivityResultLauncher<Intent> startIntentSeeDescription;
    private ActivityResultLauncher<IntentSenderRequest> startIntentDeleteImage;
    ViewPager2 viewPager2;
    FirebaseAuth mAuth;
    FirebaseUser user;
    int action;
    final int NOTIFICATION_ID = 123;

    @Override
    @SuppressLint("ClickableViewAccessibility")
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_see_full_image);

        initActivityResultLauncher();

        // init firebase
        mAuth = FirebaseAuth.getInstance();
        user = mAuth.getCurrentUser();

        // Get the path to the image from the intent
        currentIndex = getIntent().getIntExtra("index", 0);
        imageArrayList = getIntent().getParcelableArrayListExtra("imageArraylist");
        if (imageArrayList != null) {
            image = imageArrayList.get(currentIndex);
        }

        // get action
        action = getIntent().getIntExtra("action", 0);

        // init viewpager2
        viewPager2 = findViewById(R.id.viewpager);
        ImageViewPager2Adapter adapter = new ImageViewPager2Adapter(imageArrayList, getApplicationContext(), viewPager2);
        viewPager2.setAdapter(adapter);
        viewPager2.setCurrentItem(currentIndex, false);

        viewPager2.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                currentIndex = position;
                image = imageArrayList.get(currentIndex);
                invalidateOptionsMenu();
            }
        });

        viewPager2.setPageTransformer(new DepthPageTransformer());

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);

        toolbar.setNavigationOnClickListener(v -> finishActivityOnBackPress());

        // set back press in device
        getOnBackPressedDispatcher().addCallback(new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                finishActivityOnBackPress();
            }
        });
    }

    private void initActivityResultLauncher() {
        // when click button back in toolbar or in smartphone to finish DescriptionActivity
        startIntentSeeDescription = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        Intent data = result.getData();
                        if (data != null) {
                            // get result from DescriptionActivity and change description
                            String description = data.getStringExtra("description");
                            image.setDescription(description);
                        }
                    }
                }
        );

        // after delete image and return back to MainActivity
        startIntentDeleteImage = registerForActivityResult(
                new ActivityResultContracts.StartIntentSenderForResult(),
                result -> {
                    // Handle the result in onActivityResult
                    if (result.getResultCode() == RESULT_OK) {
                        // update to database
                        delete(image);
                        imageArrayList.remove(currentIndex);
                        // return to previous activity
                        returnToPreviousActivityAfterDeletingImage();
                    }
                }
        );
    }

    private void finishActivityOnBackPress() {
        Intent resultIntent = new Intent();
        resultIntent.putExtra("imageArrayList", imageArrayList);
        setResult(Activity.RESULT_OK, resultIntent);
        finish();
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_image_info, menu);
        MenuItem disabledMenuItem = menu.findItem(R.id.RemoveImage);

        // Disable item if user go to this activity from ImageTab
        String PreviousActivity = getIntent().getStringExtra("PreviousActivity");
        if (disabledMenuItem != null && (PreviousActivity == null || !(PreviousActivity.equals("AlbumInfoActivity")))) {
            menu.findItem(R.id.RemoveImage).setVisible(false);
        }

        // adjust menu base on isFavored
        int isFavored = image.getIsFavored();
        if (isFavored == 1) {
            menu.findItem(R.id.removeImageFromFavorites).setVisible(true);
            menu.findItem(R.id.addImageToFavorites).setVisible(false);
        } else if (isFavored == 0) {
            menu.findItem(R.id.removeImageFromFavorites).setVisible(false);
            menu.findItem(R.id.addImageToFavorites).setVisible(true);
        }

        // adjust menu base on user go to this activity from BackupImagesActivity or not
        if (action == Constants.ACTION_SEE_BACKUP_IMAGES) {
            menu.findItem(R.id.deleteImage).setVisible(false);
            menu.findItem(R.id.RemoveImage).setVisible(false);
            menu.findItem(R.id.setAsWallpaper).setVisible(false);
            menu.findItem(R.id.shareImage).setVisible(false);
            menu.findItem(R.id.addImageToFavorites).setVisible(false);
            menu.findItem(R.id.removeImageFromFavorites).setVisible(false);
            menu.findItem(R.id.seeDescription).setVisible(false);
            menu.findItem(R.id.backupImage).setVisible(false);
        } else {
            menu.findItem(R.id.downloadBackupImage).setVisible(false);
            menu.findItem(R.id.deleteBackupImage).setVisible(false);
        }

        return super.onCreateOptionsMenu(menu);
    }

    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemID = item.getItemId();

        if (itemID == R.id.deleteImage) {
            deleteImage(getUriFromPath(ImageInfoActivity.this, new File(image.getPath())));
        } else if (itemID == R.id.RemoveImage) {
            RemoveImageFromAlbum();
        } else if (itemID == R.id.setAsWallpaper) {
            setAsWallpaper();
        } else if (itemID == R.id.shareImage) {
            shareImage();
        } else if (itemID == R.id.addImageToFavorites) {
            updateImageIsFavored(1);
            invalidateOptionsMenu();
        } else if (itemID == R.id.removeImageFromFavorites) {
            updateImageIsFavored(0);
            invalidateOptionsMenu();
        } else if (itemID == R.id.seeDescription) {
            seeDescriptionImage();
        } else if (itemID == R.id.backupImage) {
            backupImage(image.getPath());
        } else if (itemID == R.id.downloadBackupImage) {
            try {
                downloadImage();
            } catch (IOException e) {
                Log.e("aaaaa", Objects.requireNonNull(e.getMessage()));
            }
        } else if (itemID == R.id.deleteBackupImage) {
            deleteBackupImage();
        }

        return super.onOptionsItemSelected(item);
    }

    private void downloadImage() throws IOException {
        StorageReference storageReference = FirebaseStorage.getInstance().getReferenceFromUrl(image.getPath());

        File localFile = File.createTempFile("images", "jpg");

        // create notification through service
        Intent intent = new Intent(getApplicationContext(), ServiceNotification.class);
        intent.putExtra("action", Constants.ACTION_DOWNLOADING);
        startService(intent);

        storageReference.getFile(localFile).addOnSuccessListener(taskSnapshot -> {
            // copy to external storage
            copyFileToExternalStorage(getApplicationContext(), localFile);

            // create notification through service
            Intent intent2 = new Intent(getApplicationContext(), ServiceNotification.class);
            intent2.putExtra("action", Constants.ACTION_DOWNLOAD_COMPLETE);
            startService(intent2);

        }).addOnFailureListener(e -> Log.e("aaaa", Objects.requireNonNull(e.getMessage())));
    }

    private void backupImage(String path) {
        if (user == null) {
            MaterialAlertDialogBuilder dialogBuilder = new MaterialAlertDialogBuilder(ImageInfoActivity.this);
            dialogBuilder.setTitle("Please sign in to use this function");

            dialogBuilder.setPositiveButton("Ok", (dialog, id) -> {
                dialog.dismiss();
            });

            dialogBuilder.show();
            return;
        }

        Uri file = Uri.fromFile(new File(path));
        StorageReference storageRef = FirebaseStorage.getInstance().getReferenceFromUrl(getString(R.string.folder_path_storage));
        StorageReference riversRef = storageRef.child(user.getEmail() + "/" + file.getLastPathSegment());
        UploadTask uploadTask = riversRef.putFile(file);

        // create notification through service
        Intent intent = new Intent(getApplicationContext(), ServiceNotification.class);
        intent.putExtra("action", Constants.ACTION_UPLOADING);
        startService(intent);

        // Register observers to listen for when the download is done or if it fails
        uploadTask.addOnFailureListener(exception ->
                        Toast.makeText(ImageInfoActivity.this, exception.getMessage(), Toast.LENGTH_SHORT).show())
                .addOnSuccessListener(taskSnapshot -> {

                    // create notification through service
                    Intent intent2 = new Intent(getApplicationContext(), ServiceNotification.class);
                    intent2.putExtra("action", Constants.ACTION_UPLOAD_COMPLETE);
                    startService(intent2);
                });
    }

    private void deleteBackupImage() {
        StorageReference storageReference = FirebaseStorage.getInstance().getReferenceFromUrl(image.getPath());

        // Delete the file
        storageReference.delete()
                .addOnSuccessListener(aVoid -> {
                    imageArrayList.remove(image);
                    returnToPreviousActivityAfterDeletingImage();
                })
                .addOnFailureListener(exception -> {
                    Toast.makeText(ImageInfoActivity.this, "Cannot delete this image", Toast.LENGTH_SHORT).show();
                    Log.e("aaaa", Objects.requireNonNull(exception.getMessage()));
                });
    }

    private void seeDescriptionImage() {
        Intent intent = new Intent(ImageInfoActivity.this, DescriptionActivity.class);
        intent.putExtra("image", image);
        startIntentSeeDescription.launch(intent);
    }

    private void updateImageIsFavored(int isFavored) {
        Image temp = new Image(image.getPath(), image.getDescription(), isFavored);
        long rowID = update(temp);
        if (rowID > 0) {
            image.setIsFavored(isFavored);
        }
    }

    private void shareImage() {
        Bitmap bitmap = BitmapFactory.decodeFile(image.getPath());
        shareImageAndText(bitmap);
    }

    private void shareImageAndText(Bitmap bitmap) {
        Uri uri = getImageToShare(bitmap);
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.putExtra(Intent.EXTRA_STREAM, uri);
        intent.putExtra(Intent.EXTRA_TEXT, "Image Text");
        intent.putExtra(Intent.EXTRA_SUBJECT, "Image Subject");
        intent.setType("image/*");
        startActivity(Intent.createChooser(intent, "Share via"));
    }

    private Uri getImageToShare(Bitmap bitmap) {
        File folder = new File(getCacheDir(), "images");
        Uri uri = null;
        try {
            folder.mkdirs();
            File file = new File(folder, "image.ipg");
            FileOutputStream fileOutputStream = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fileOutputStream);
            fileOutputStream.flush();
            fileOutputStream.close();

            uri = FileProvider.getUriForFile(this, "com.example.imagesgallery.Activity", file);
        } catch (Exception e) {
            e.printStackTrace();

        }
        return uri;
    }

    private void setAsWallpaper() {
        try {
            WallpaperManager wallpaperManager = WallpaperManager.getInstance(this);
            // Load the image from the file
            Uri imageUri = Uri.fromFile(new File(image.getPath()));
            wallpaperManager.setStream(getContentResolver().openInputStream(imageUri));
            Toast.makeText(this, "Wallpaper set successfully", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e("error", Objects.requireNonNull(e.getMessage()));
            Toast.makeText(this, "Unable to set wallpaper", Toast.LENGTH_SHORT).show();
        }
    }

    private void deleteImage(Uri uri) {
        try {
            int row = getContentResolver().delete(uri, null, null);
            if (row > 0) {
                // update to database
                delete(image);
                imageArrayList.remove(currentIndex);
                // return to previous activity
                returnToPreviousActivityAfterDeletingImage();
            } else {
                Toast.makeText(this, "Unable to delete", Toast.LENGTH_SHORT).show();
            }
        } catch (SecurityException e) {
            PendingIntent pendingIntent = null;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                ArrayList<Uri> uriArrayList = new ArrayList<>();
                uriArrayList.add(uri);
                pendingIntent = MediaStore.createDeleteRequest(getContentResolver(), uriArrayList);
            } else {
                if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q) {
                    if (e instanceof RecoverableSecurityException) {
                        RecoverableSecurityException exception = (RecoverableSecurityException) e;
                        pendingIntent = exception.getUserAction().getActionIntent();
                    }
                } else {
                    File deleteImage = new File(image.getPath());
                    if (deleteImage.exists()) {
                        if (deleteImage.delete()) {
                            // update to database
                            delete(image);
                            imageArrayList.remove(currentIndex);
                            // return to previous activity
                            returnToPreviousActivityAfterDeletingImage();
                        }
                    } else {
                        Log.d("error", "Cannot delete image");
                    }
                }

            }
            if (pendingIntent != null) {
                IntentSender intentSender = pendingIntent.getIntentSender();
                try {
                    IntentSenderRequest request = new IntentSenderRequest.Builder(intentSender).build();
                    startIntentDeleteImage.launch(request);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

    private void RemoveImageFromAlbum() {
        int id_album = getIntent().getIntExtra("id_album", -1);

        long rowID = removeImageFromAlbum(image.getPath(), id_album);

        if (rowID > 0) {
            imageArrayList.remove(currentIndex);
            Intent resultIntent = new Intent();
            resultIntent.putExtra("imageArrayList", imageArrayList);
            setResult(Activity.RESULT_OK, resultIntent);
            finish();
        } else {
            Toast.makeText(ImageInfoActivity.this, "Cannot remove image from album", Toast.LENGTH_SHORT).show();
            Log.e("aaaa", "Unable to remove image " + image.getPath() + "from album ");
        }
    }

    private void returnToPreviousActivityAfterDeletingImage() {
        Intent resultIntent = new Intent();
        resultIntent.putExtra("imageArrayList", imageArrayList);
        setResult(Activity.RESULT_OK, resultIntent);
        finish();
    }
}

