package com.example.imagesgallery.Activity;

import static com.example.imagesgallery.Database.SqliteDatabase.delete;
import static com.example.imagesgallery.Database.SqliteDatabase.update;
import static com.example.imagesgallery.Utils.PathUtils.getUriFromPath;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.PendingIntent;
import android.app.RecoverableSecurityException;
import android.app.WallpaperManager;
import android.content.Intent;
import android.content.IntentSender;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.IntentSenderRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.FileProvider;

import com.bumptech.glide.Glide;
import com.example.imagesgallery.Database.SqliteDatabase;
import com.example.imagesgallery.Model.Image;
import com.example.imagesgallery.R;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Objects;

public class ImageInfoActivity extends AppCompatActivity {
    ImageView imageView;
    Image image;
    private ActivityResultLauncher<Intent> startIntentSeeDescription;
    private ActivityResultLauncher<IntentSenderRequest> startIntentDeleteImage;

    @Override
    @SuppressLint("ClickableViewAccessibility")
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_see_full_image);

        initActivityResultLauncher();

        GestureDetector gestureDetector = new GestureDetector(ImageInfoActivity.this, new MyGesture());

        // Get the path to the image from the intent
        image = (Image) getIntent().getSerializableExtra("image");

        // Load the image into the ImageView element
        imageView = findViewById(R.id.imageFullScreen);
        Glide.with(this)
                .load(image.getPath())
                .error(R.drawable.no_image)
                .into(imageView);

        imageView.setOnTouchListener((view, motionEvent) -> {
            gestureDetector.onTouchEvent(motionEvent);
            return true;
        });

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

    static class MyGesture extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onFling(@Nullable MotionEvent e1, @NonNull MotionEvent e2, float velocityX, float velocityY) {
            Log.d("aaaa", "onFling");
            return super.onFling(e1, e2, velocityX, velocityY);
        }
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
                        // return to previous activity
                        returnToPreviousActivityAfterDeletingImage();
                    }
                }
        );
    }

    private void finishActivityOnBackPress() {
        Intent resultIntent = new Intent();
        resultIntent.putExtra("image", image);
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

        int isFavored = image.getIsFavored();
        if (isFavored == 1) {
            menu.findItem(R.id.removeImageFromFavorites).setVisible(true);
            menu.findItem(R.id.addImageToFavorites).setVisible(false);
        } else if (isFavored == 0) {
            menu.findItem(R.id.removeImageFromFavorites).setVisible(false);
            menu.findItem(R.id.addImageToFavorites).setVisible(true);
        }

        return super.onCreateOptionsMenu(menu);
    }

    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemID = item.getItemId();

        if (itemID == R.id.deleteImage) {
            createDialogDeleteImage();
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
        }

        return super.onOptionsItemSelected(item);
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
        BitmapDrawable bitmapDrawable = (BitmapDrawable) imageView.getDrawable();
        Bitmap bitmap = bitmapDrawable.getBitmap();
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

    public void createDialogDeleteImage() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Are you sure you want to delete this image ?");

        // click yes
        builder.setPositiveButton("Yes", (dialog, id) -> deleteImage(getUriFromPath(ImageInfoActivity.this, new File(image.getPath()))));
        // click no
        builder.setNegativeButton("No", (dialog, id) -> dialog.dismiss());

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void deleteImage(Uri uri) {
        try {
            int row = getContentResolver().delete(uri, null, null);
            if (row > 0) {
                // update to database
                delete(image);
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

        String[] args = {String.valueOf(id_album), image.getPath()};
        String sql = "DELETE FROM Album_Contain_Images WHERE id_album = ? AND path = ?";
        SqliteDatabase.db.execSQL(sql, args);

        Intent resultIntent = new Intent();
        resultIntent.putExtra("ImageRemoved", image.getPath());
        setResult(Activity.RESULT_OK, resultIntent);
        finish();
    }

    private void returnToPreviousActivityAfterDeletingImage() {
        Intent resultIntent = new Intent();
        resultIntent.putExtra("ImageDeleted", image.getPath());
        setResult(Activity.RESULT_OK, resultIntent);
        finish();
    }
}

