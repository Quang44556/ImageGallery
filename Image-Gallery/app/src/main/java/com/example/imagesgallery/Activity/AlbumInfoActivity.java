package com.example.imagesgallery.Activity;

import static com.example.imagesgallery.Database.SqliteDatabase.delete;
import static com.example.imagesgallery.Database.SqliteDatabase.update;
import static com.example.imagesgallery.Utils.Constants.ACTION_CHANGE_COVER;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.bumptech.glide.Glide;
import com.example.imagesgallery.Database.SqliteDatabase;
import com.example.imagesgallery.Fragment.ImageFragment;
import com.example.imagesgallery.Model.Album;
import com.example.imagesgallery.Model.Image;
import com.example.imagesgallery.R;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.Objects;

public class AlbumInfoActivity extends AppCompatActivity {

    TextView txtAlbumDescription;
    ImageView imgCoverAlbum;
    Toolbar toolbar;
    Album album;
    Button btnChange, btnCancel;
    EditText edtChangeNameAlbum;
    TextView txtTitleDialog;
    Dialog dialog;
    ImageFragment imageFragment;
    ActivityResultLauncher<Intent> startIntentChangeDescription, startIntentChangeCover;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_album_info);

        album = getIntent().getParcelableExtra("album");

        imageFragment = new ImageFragment();
        Bundle bundle = new Bundle();
        bundle.putParcelable("album", album);
        imageFragment.setArguments(bundle);
        getSupportFragmentManager().beginTransaction().replace(R.id.container, imageFragment).commit();

        init();
        initActivityResultLauncher();

        // add ellipsize at the end of textview if it is long
        txtAlbumDescription.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                txtAlbumDescription.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                int noOfLinesVisible = txtAlbumDescription.getHeight() / txtAlbumDescription.getLineHeight();
                txtAlbumDescription.setText(album.getDescription());
                txtAlbumDescription.setMaxLines(noOfLinesVisible);
                txtAlbumDescription.setEllipsize(TextUtils.TruncateAt.END);
            }
        });

        // set description of album
        String coverPath = "";
        if (album != null) {
            txtAlbumDescription.setText(album.getDescription());
            coverPath = album.getCover().getPath();
        }

        // set cover of album
        if (coverPath.equals(MainActivity.pathNoImage)) {
            imgCoverAlbum.setImageResource(R.drawable.no_image);
        } else {
            Glide.with(AlbumInfoActivity.this)
                    .load(coverPath)
                    .placeholder(R.drawable.loading)
                    .error(R.drawable.no_image)
                    .into(imgCoverAlbum);
        }

        // using toolbar as ActionBar
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(album.getName());

        // set return button
        toolbar.setNavigationOnClickListener(v -> finishActivityOnBackPress());

        // set back press in device
        getOnBackPressedDispatcher().addCallback(new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                finishActivityOnBackPress();
            }
        });

        // when click the description of album
        txtAlbumDescription.setOnClickListener(view -> moveToChangeDescriptionScreen());

        // when click the cover of album
        imgCoverAlbum.setOnClickListener(view -> moveToChangeCoverScreen());
    }

    private void finishActivityOnBackPress() {
        if (!imageFragment.imageAdapter.isInMultiSelectMode()) {
            finishActivity(); // return to album tab
        } else {
            // cancel multi select mode
            imageFragment.exitMultiselectMode();
            Objects.requireNonNull(getSupportActionBar()).setHomeAsUpIndicator(androidx.appcompat.R.drawable.abc_ic_ab_back_material);
        }
    }

    private void moveToChangeDescriptionScreen() {
        Intent intent = new Intent(AlbumInfoActivity.this, DescriptionActivity.class);
        intent.putExtra("album", album);
        startIntentChangeDescription.launch(intent);
    }

    private void init() {
        txtAlbumDescription = findViewById(R.id.txtAlbumDescription);
        imgCoverAlbum = findViewById(R.id.imgCoverAlbum);
        toolbar = findViewById(R.id.toolbar);
    }

    private void finishActivity() {
        Intent resultIntent = new Intent();
        resultIntent.putExtra("album", album);
        resultIntent.putExtra("PreviousActivity", "FavoriteAlbumActivity");
        setResult(Activity.RESULT_OK, resultIntent);
        finish();
    }

    private void moveToChangeCoverScreen() {
        Intent intent = new Intent(AlbumInfoActivity.this, ChooseImagesActivity.class);
        intent.putExtra("album", album);
        intent.putExtra("action", ACTION_CHANGE_COVER);
        startIntentChangeCover.launch(intent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (imageFragment.imageAdapter.isInMultiSelectMode()) {
            getMenuInflater().inflate(R.menu.menu_album_info_long_click, menu);
            Objects.requireNonNull(getSupportActionBar()).setHomeAsUpIndicator(R.drawable.close_icon);
        } else {
            getMenuInflater().inflate(R.menu.menu_album_info, menu);
            int isFavored = album.getIsFavored();
            if (isFavored == 1) {
                menu.findItem(R.id.removeAnAlbumFromFavorites).setVisible(true);
                menu.findItem(R.id.addAnAlbumToFavorites).setVisible(false);
            } else if (isFavored == 0) {
                menu.findItem(R.id.removeAnAlbumFromFavorites).setVisible(false);
                menu.findItem(R.id.addAnAlbumToFavorites).setVisible(true);
            }
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemID = item.getItemId();
        if (itemID == R.id.deleteAlbum) {
            createDialogDeleteAlbum();
        } else if (itemID == R.id.changeAlbumName) {
            showDialogChangeNameAlbum();
        } else if (itemID == R.id.addAnAlbumToFavorites) {
            updateAlbumIsFavored(1);
            invalidateOptionsMenu();
        } else if (itemID == R.id.removeAnAlbumFromFavorites) {
            updateAlbumIsFavored(0);
            invalidateOptionsMenu();
        } else if (itemID == R.id.deleteImagesFromAlbum) {
            imageFragment.deleteChosenImages();
            Objects.requireNonNull(getSupportActionBar()).setHomeAsUpIndicator(androidx.appcompat.R.drawable.abc_ic_ab_back_material);
        } else if (itemID == R.id.slideshow) {
            imageFragment.slideshowImages();
        } else if (itemID == R.id.removeFromAlbum) {
            createDialogRemoveImages();
        } else if (itemID == R.id.backupImagesInAlbum){
            imageFragment.backupImages();
            imageFragment.exitMultiselectMode();
        }
        return super.onOptionsItemSelected(item);
    }

    // show dialog when user choose "change name" in menu
    private void showDialogChangeNameAlbum() {
        dialog = new Dialog(AlbumInfoActivity.this);
        dialog.setContentView(R.layout.dialog_change_album_name);

        btnChange = dialog.findViewById(R.id.buttonChangeName);
        btnCancel = dialog.findViewById(R.id.buttonCancelChangeName);
        edtChangeNameAlbum = dialog.findViewById(R.id.edtChangeAlbumName);
        txtTitleDialog = dialog.findViewById(R.id.title_dialog_change_name);

        edtChangeNameAlbum.setText(album.getName());

        // when click button add of dialog
        btnChange.setOnClickListener(view -> {
            String name = edtChangeNameAlbum.getText().toString();
            if (name.equals("")) {
                Toast.makeText(AlbumInfoActivity.this, "Please enter name of album", Toast.LENGTH_SHORT).show();
            } else {
                String newName = edtChangeNameAlbum.getText().toString();
                Album temp = new Album(
                        album.getCover(),
                        newName,
                        album.getDescription(),
                        album.getIsFavored(),
                        album.getId()
                );
                long rowID = update(temp);

                if (rowID > 0) {
                    album.setName(newName);
                    Objects.requireNonNull(getSupportActionBar()).setTitle(album.getName());
                    dialog.dismiss();
                } else {
                    Toast.makeText(AlbumInfoActivity.this, "Unable to update", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // when click button cancel of dialog
        btnCancel.setOnClickListener(view -> dialog.dismiss());

        dialog.show();
        resizeDialog();
    }

    // resize the dialog to fit the screen size
    private void resizeDialog() {
        // resize dialog size
        int width = getApplicationContext().getApplicationContext().getResources().getDisplayMetrics().widthPixels;
        Objects.requireNonNull(dialog.getWindow()).setLayout((6 * width) / 7, ViewGroup.LayoutParams.WRAP_CONTENT);

        // get screen size
        DisplayMetrics displayMetrics = Resources.getSystem().getDisplayMetrics();
        float screenWidth = displayMetrics.widthPixels;

        // resize text size
        float newTextSize = screenWidth * 0.05f;
        edtChangeNameAlbum.setTextSize(TypedValue.COMPLEX_UNIT_PX, newTextSize);

        newTextSize = screenWidth * 0.08f;
        txtTitleDialog.setTextSize(TypedValue.COMPLEX_UNIT_PX, newTextSize);

        newTextSize = screenWidth * 0.04f;
        btnChange.setTextSize(TypedValue.COMPLEX_UNIT_PX, newTextSize);
        btnCancel.setTextSize(TypedValue.COMPLEX_UNIT_PX, newTextSize);
    }

    private void createDialogRemoveImages() {
        MaterialAlertDialogBuilder dialogBuilder = new MaterialAlertDialogBuilder(AlbumInfoActivity.this);
        if (imageFragment.imageAdapter.getSelectedImages().size() == 0) {
            Toast.makeText(this, "You have not chosen any images", Toast.LENGTH_SHORT).show();
            return;
        }
        dialogBuilder.setTitle("Are you sure you want to remove these images from album ?");

        // click yes
        dialogBuilder.setPositiveButton("Yes", (dialog, id) -> {
            ArrayList<Image> selectedImages = imageFragment.imageAdapter.getSelectedImages();

            // remove images
            for (Image image : selectedImages) {
                removeImageFromAlbum(image.getPath());
            }

            // change UI
            imageFragment.exitMultiselectMode();
            imageFragment.totalImages.setText(String.valueOf(imageFragment.imageAdapter.getImageArrayList().size()));
            Objects.requireNonNull(getSupportActionBar()).setHomeAsUpIndicator(androidx.appcompat.R.drawable.abc_ic_ab_back_material);
        });

        // click no
        dialogBuilder.setNegativeButton("No", (dialog, id) -> dialog.dismiss());

        dialogBuilder.show();
    }

    private void removeImageFromAlbum(String imagePath) {
        long rowID = SqliteDatabase.removeImageFromAlbum(imagePath, album.getId());
        if (rowID > 0) {
            // change arrayList image in album
            ArrayList<Image> imageArrayList = imageFragment.imageAdapter.getImageArrayList();
            imageArrayList.removeIf(image -> image.getPath().equals(imagePath));
        }
    }

    private void updateAlbumIsFavored(int isFavored) {
        Album temp = new Album(
                album.getCover(),
                album.getName(),
                album.getDescription(),
                isFavored,
                album.getId()
        );
        long rowID = update(temp);
        if (rowID > 0) {
            album.setIsFavored(isFavored);
        }
    }

    public void createDialogDeleteAlbum() {
        MaterialAlertDialogBuilder dialogBuilder = new MaterialAlertDialogBuilder(AlbumInfoActivity.this);
        dialogBuilder.setTitle("Are you sure you want to delete this album ?");

        // click yes
        dialogBuilder.setPositiveButton("Yes", (dialog, id) -> deleteAlbum());

        // click no
        dialogBuilder.setNegativeButton("No", (dialog, id) -> dialog.dismiss());

        dialogBuilder.show();
    }

    private void deleteAlbum() {
        // delete in database
        delete(album);

        // finish activity
        Intent resultIntent = new Intent();
        resultIntent.putExtra("isDelete", 1);
        setResult(Activity.RESULT_OK, resultIntent);
        finish();
    }

    private void initActivityResultLauncher() {
        // when click button back in toolbar or in smartphone to finish ChooseImageActivity
        startIntentChangeCover = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        Intent data = result.getData();
                        if (data != null) {
                            // get result from AddImageActivity and change cover
                            Image image = data.getParcelableExtra("image");
                            if (image != null) {
                                album.setCover(image);
                                Glide.with(AlbumInfoActivity.this)
                                        .load(album.getCover().getPath())
                                        .placeholder(R.drawable.loading)
                                        .error(R.drawable.no_image)
                                        .into(imgCoverAlbum);
                            }
                        }
                    }
                }
        );

        // when click button back in toolbar or in smartphone to finish DescriptionActivity
        startIntentChangeDescription = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        Intent data = result.getData();
                        if (data != null) {
                            // get result from DescriptionActivity and change description
                            String description = data.getStringExtra("description");
                            album.setDescription(description);
                            txtAlbumDescription.setText(description);
                        }
                    }
                }
        );
    }
}

//        boolean isLoading = false, isAllItemsLoaded = false;
//        int CurrentMaxPosition = 0, IdMaxWhenStartingLoadData = 0;

//        isAllItemsLoaded = false;
//        CurrentMaxPosition = 0;
//        IdMaxWhenStartingLoadData = 0;

//        load first images in album
//        loadDataFromDatabase();

//        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
//            @Override
//            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
//                super.onScrollStateChanged(recyclerView, newState);
//            }
//
//            @Override
//            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
//                super.onScrolled(recyclerView, dx, dy);
//                GridLayoutManager gridLayoutManager = (GridLayoutManager) recyclerView.getLayoutManager();
//                if (!isLoading && gridLayoutManager != null && gridLayoutManager.findLastCompletelyVisibleItemPosition() >= images.size() - 1 && !isAllItemsLoaded) {
//                    isLoading = true;
//                    // Create an executor that executes tasks in the main thread and background thread
//                    Executor mainExecutor = ContextCompat.getMainExecutor(AlbumInfoActivity.this);
//                    ScheduledExecutorService backgroundExecutor = Executors.newSingleThreadScheduledExecutor();
//                    // Load data in the background thread.
//                    backgroundExecutor.execute(() -> {
//                        loadDataFromDatabase();
//                        // Update list images in a album on the main thread
//                        mainExecutor.execute(() -> {
//                            adapter.notifyDataSetChanged();
//                            isLoading = false;
//                        });
//                    });
//                }
//            }
//        });

//    private void loadDataFromDatabase() {
//        String sql;
//        Cursor cursor;
//        if (IdMaxWhenStartingLoadData == 0) {
//            try {
//                sql = "SELECT MAX(id) FROM Album_Contain_Images";
//                cursor = SqliteDatabase.db.rawQuery(sql, null);
//            } catch (Exception exception) {
//                return;
//            }
//
//            cursor.moveToPosition(-1);
//            while (cursor.moveToNext()) {
//                IdMaxWhenStartingLoadData = cursor.getInt(0);
//            }
//            cursor.close();
//        }
//
//        String sqlContainImages = "SELECT * FROM Album_Contain_Images AS Contain, Image AS I " +
//                "WHERE id_album = ? AND Contain.path = I.path AND Contain.id <= ? ORDER BY id DESC LIMIT ? OFFSET ?";
//        int itemsPerLoading = 21;
//        String[] argsContainImages = {String.valueOf(album.getId()), String.valueOf(IdMaxWhenStartingLoadData), String.valueOf(itemsPerLoading), String.valueOf(CurrentMaxPosition)};
//        Cursor cursorContainImages;
//        try {
//            cursorContainImages = SqliteDatabase.db.rawQuery(sqlContainImages, argsContainImages);
//            if (!cursorContainImages.moveToFirst()) {
//                isAllItemsLoaded = true;
//            }
//            cursorContainImages.moveToPosition(-1);
//
//            int pathImageColumn = cursorContainImages.getColumnIndex("Contain.path");
//            int descriptionImageColumn = cursorContainImages.getColumnIndex("I.description");
//            int isFavoredImageColumn = cursorContainImages.getColumnIndex("I.isFavored");
//
//            String pathImageInAlbum;
//            String descriptionImageInAlbum;
//            int isFavoredImageInAlbum;
//
//            while (cursorContainImages.moveToNext()) {
//                descriptionImageInAlbum = cursorContainImages.getString(descriptionImageColumn);
//                isFavoredImageInAlbum = cursorContainImages.getInt(isFavoredImageColumn);
//                pathImageInAlbum = cursorContainImages.getString(pathImageColumn);
//                Image image = new Image(pathImageInAlbum, descriptionImageInAlbum, isFavoredImageInAlbum);
//                images.add(image);
//            }
//            cursorContainImages.close();
//            CurrentMaxPosition += itemsPerLoading;
//        } catch (Exception ignored) {
//        }
//    }
