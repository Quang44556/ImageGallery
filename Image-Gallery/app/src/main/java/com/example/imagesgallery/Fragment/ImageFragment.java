package com.example.imagesgallery.Fragment;

import static android.app.Activity.RESULT_CANCELED;
import static android.app.Activity.RESULT_OK;
import static android.os.Environment.MEDIA_MOUNTED;
import static com.example.imagesgallery.Database.SqliteDatabase.addImageToAlbum;
import static com.example.imagesgallery.Database.SqliteDatabase.delete;
import static com.example.imagesgallery.Database.SqliteDatabase.insert;
import static com.example.imagesgallery.Database.SqliteDatabase.update;
import static com.example.imagesgallery.Utils.FileHelper.copyFileToExternalStorage;
import static com.example.imagesgallery.Utils.PathUtils.getUriFromPath;

import android.app.Activity;
import android.app.PendingIntent;
import android.app.RecoverableSecurityException;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.IntentSenderRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.imagesgallery.Activity.AlbumInfoActivity;
import com.example.imagesgallery.Activity.BackupImagesActivity;
import com.example.imagesgallery.Activity.ChooseImagesActivity;
import com.example.imagesgallery.Activity.FavoriteImagesActivity;
import com.example.imagesgallery.Activity.ImageInfoActivity;
import com.example.imagesgallery.Activity.MainActivity;
import com.example.imagesgallery.Activity.SlideshowActivity;
import com.example.imagesgallery.Adapter.ImageAdapter;
import com.example.imagesgallery.Database.SqliteDatabase;
import com.example.imagesgallery.Listener.ClickListener;
import com.example.imagesgallery.Model.Album;
import com.example.imagesgallery.Model.Image;
import com.example.imagesgallery.R;
import com.example.imagesgallery.Service.ServiceNotification;
import com.example.imagesgallery.Utils.Constants;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;


public class ImageFragment extends Fragment {
    RecyclerView recycler;
    ArrayList<Image> imageArrayList;
    public ImageAdapter imageAdapter;
    GridLayoutManager manager;
    public TextView totalImages, titleTotalItems;
    MainActivity mainActivity;
    Context context;
    LinearLayout linearLayout;
    ActivityResultLauncher<Intent> startIntentSeeImageInfo, startIntentTakePicture;
    ActivityResultLauncher<IntentSenderRequest> startForDeletionLauncher;
    ActivityResultLauncher<Intent> startIntentAddImages;
    ImageButton imageBtnAddImages;
    int clickPosition = 0;
    int imagesCountBefore; // the number of image in device before take pictures
    Toolbar toolbar;
    AppCompatActivity activity;
    ArrayList<Image> imagesWantToDelete; // hold the current image in loop when execute delete images in multiselect mode
    int currentIndex; // hold the index in array {imagesWantToDelete} of image in each loop
    int action; // action user want to execute (change cover or add images)
    Album album;
    StorageReference storageRef;
    FirebaseAuth mAuth;
    FirebaseUser user;

    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = getContext();
        if (context instanceof MainActivity) {
            mainActivity = (MainActivity) getActivity();
        }

        setHasOptionsMenu(true);
        initActivityResultLauncher();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        linearLayout = (LinearLayout) inflater.inflate(R.layout.fragment_image, container, false);
        return linearLayout;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        init();

        activity = (AppCompatActivity) getActivity();
        if (activity != null) {
            action = activity.getIntent().getIntExtra("action", -1);
        }

        // get album
        if (context instanceof AlbumInfoActivity) {
            Bundle bundle = getArguments();
            if (bundle != null) {
                album = bundle.getParcelable("album");
            }
        } else {
            if (activity != null) {
                album = activity.getIntent().getParcelableExtra("album");
            }
        }

        // set the number of items in a row
        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        float screenWidthInDp = displayMetrics.widthPixels / displayMetrics.density;
        int imageWidth = 110; // size of an image
        int desiredColumnCount = (int) screenWidthInDp / imageWidth; // the number of images in a row

        if (!(context instanceof AlbumInfoActivity)) {
            // set toolbar
            if (activity != null) {
                activity.setSupportActionBar(toolbar);
            }

            toolbar.setNavigationOnClickListener(view1 -> clickBackPress());
        }

        if (context instanceof ChooseImagesActivity || context instanceof MainActivity) {
            // set back press in device
            activity.getOnBackPressedDispatcher().addCallback(new OnBackPressedCallback(true) {
                @Override
                public void handleOnBackPressed() {
                    clickBackPress();
                }
            });
        }

        // init firebase
        storageRef = FirebaseStorage.getInstance()
                .getReferenceFromUrl(context.getString(R.string.folder_path_storage));
        mAuth = FirebaseAuth.getInstance();
        user = mAuth.getCurrentUser();

        // adjust UI base on present activity
        if (context instanceof ChooseImagesActivity) {
            imageBtnAddImages.setVisibility(View.GONE);
            titleTotalItems.setVisibility(View.GONE);
            toolbar.setVisibility(View.VISIBLE);
            totalImages.setVisibility(View.GONE);
            Objects.requireNonNull(activity.getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
            Objects.requireNonNull(activity.getSupportActionBar()).setTitle(R.string.TitleChooseImages);
            imageBtnAddImages.setImageResource(R.drawable.add_icon);
        }
        if (context instanceof MainActivity) {
            toolbar.setVisibility(View.GONE);
            Objects.requireNonNull(activity.getSupportActionBar()).setTitle("");
            mainActivity.showLinearLayoutTitle();
        } else if (context instanceof AlbumInfoActivity) {
            imageBtnAddImages.setImageResource(R.drawable.add_icon);
            toolbar.setVisibility(View.GONE);
            Objects.requireNonNull(activity.getSupportActionBar()).setTitle("");
        } else if (context instanceof FavoriteImagesActivity) {
            imageBtnAddImages.setImageResource(R.drawable.add_icon);
            toolbar.setVisibility(View.VISIBLE);
            Objects.requireNonNull(activity.getSupportActionBar()).setTitle(R.string.TitleFavoriteImages);
            Objects.requireNonNull(activity.getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        } else if (context instanceof BackupImagesActivity) {
            Objects.requireNonNull(activity.getSupportActionBar()).setTitle(R.string.TitleBackup);
            Objects.requireNonNull(activity.getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
            imageBtnAddImages.setVisibility(View.GONE);
        }

        // init
        imageArrayList = new ArrayList<>();
        imageAdapter = new ImageAdapter(context, imageArrayList, clickListener);
        manager = new GridLayoutManager(context, desiredColumnCount);
        recycler.setLayoutManager(manager);
        recycler.setAdapter(imageAdapter);

        imageAdapter.setAction(Constants.NO_ACTION);

        if (context instanceof MainActivity || context instanceof ChooseImagesActivity) {
            // load images to array and insert to database
            loadImagesFromDevice();
        } else if (context instanceof AlbumInfoActivity) {
            loadImagesInAlbum();
        } else if (context instanceof FavoriteImagesActivity) {
            loadFavoriteImages();
        } else if (context instanceof BackupImagesActivity) {
            loadBackupImages();
        }

        imageBtnAddImages.setOnClickListener(view12 -> {
            if (context instanceof MainActivity) {
                openCamera();
            } else if (context instanceof ChooseImagesActivity) {
                if (action == Constants.ACTION_ADD_IMAGES) {
                    addImagesToAlbum();
                } else if (action == Constants.ACTION_CHOOSE_FAVORITE_IMAGES) {
                    addImagesToFavorites();
                }
            } else if (context instanceof AlbumInfoActivity) {
                chooseImagesToAddToAlbum();
            } else if (context instanceof FavoriteImagesActivity) {
                chooseImagesToAddToFavorites();
            }
        });
    }

    ClickListener clickListener = new ClickListener() {
        @Override
        public void click(int index) {
            clickPosition = index;
            if (context instanceof ChooseImagesActivity) {
                clickImageInChooseImagesActivity(index);
            } else if (context instanceof MainActivity
                    || context instanceof AlbumInfoActivity
                    || context instanceof FavoriteImagesActivity
                    || context instanceof BackupImagesActivity) {
                clickImage(index);
            }
        }

        @Override
        public void longClick(int index) {
            if (action != Constants.ACTION_CHANGE_COVER) {
                enterMultiselectMode(index);
            }
        }
    };

    private void clickImage(int index) {
        if (imageAdapter.isInMultiSelectMode()) {
            imageAdapter.toggleSelection(index);
            imageAdapter.notifyItemChanged(index);
        } else {
            // Create an intent to start the new activity
            Intent intent = new Intent(context, ImageInfoActivity.class);
            intent.putExtra("index", index);
            intent.putExtra("imageArraylist", imageArrayList);

            if (context instanceof BackupImagesActivity) {
                intent.putExtra("action", Constants.ACTION_SEE_BACKUP_IMAGES);
            }

            if (context instanceof AlbumInfoActivity) {
                intent.putExtra("PreviousActivity", "AlbumInfoActivity");
                intent.putExtra("id_album", album.getId());
            }
            startIntentSeeImageInfo.launch(intent);
        }
    }

    private void clickImageInChooseImagesActivity(int index) {
        if (imageAdapter.isInMultiSelectMode()) {
            imageAdapter.toggleSelection(index);
            imageAdapter.notifyItemChanged(index);
        }

        if (!imageAdapter.isInMultiSelectMode()) {
            if (action == Constants.ACTION_ADD_IMAGES) {
                // change database
                long rowID = addImageToAlbum(album, imageArrayList.get(index).getPath());

                if (rowID > 0) {
                    // finish activity
                    Intent resultIntent = new Intent();
                    resultIntent.putExtra("image", imageArrayList.get(clickPosition));
                    activity.setResult(Activity.RESULT_OK, resultIntent);
                    activity.finish();
                } else {
                    // show error
                    Toast.makeText(context, "Unable to add images", Toast.LENGTH_SHORT).show();
                }

            } else if (action == Constants.ACTION_CHANGE_COVER) {
                // change database
                Album temp = new Album(
                        imageArrayList.get(index),
                        album.getName(),
                        album.getDescription(),
                        album.getIsFavored(),
                        album.getId()
                );
                long rowID = update(temp);

                if (rowID > 0) { // if change cover successfully
                    // finish activity
                    Intent resultIntent = new Intent();
                    resultIntent.putExtra("image", imageArrayList.get(clickPosition));
                    activity.setResult(Activity.RESULT_OK, resultIntent);
                    activity.finish();
                } else { // if change cover failed
                    Toast.makeText(context, "Cannot change cover", Toast.LENGTH_SHORT).show();
                }
            } else if (action == Constants.ACTION_CHOOSE_FAVORITE_IMAGES) {
                // change database
                Image image = imageArrayList.get(index);
                Image temp = new Image(image.getPath(), image.getDescription(), 1);
                long rowID = update(temp);
                if (rowID > 0) {
                    image.setIsFavored(1);
                    // finish activity
                    Intent resultIntent = new Intent();
                    resultIntent.putExtra("image", image);
                    activity.setResult(Activity.RESULT_OK, resultIntent);
                    activity.finish();
                } else {
                    Toast.makeText(context, "Cannot add to favorites", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private void addImagesToAlbum() {
        ArrayList<Image> selectedImages = imageAdapter.getSelectedImages();
        ArrayList<Image> imagesAddedSuccessfullyArrayList = new ArrayList<>();
        for (Image image : selectedImages) {
            long rowID = addImageToAlbum(album, image.getPath());
            if (rowID > 0) {
                imagesAddedSuccessfullyArrayList.add(image);
            } else {
                Log.e("aaaa", "Unable to add image " + image.getPath() + "to album");
            }
        }

        Intent resultIntent = new Intent();
        resultIntent.putExtra("selectedImages", imagesAddedSuccessfullyArrayList);
        activity.setResult(Activity.RESULT_OK, resultIntent);
        activity.finish();
    }

    private void addImagesToFavorites() {
        ArrayList<Image> selectedImages = imageAdapter.getSelectedImages();
        ArrayList<Image> imagesAddedSuccessfullyArrayList = new ArrayList<>();
        for (Image image : selectedImages) {
            Image temp = new Image(image.getPath(), image.getDescription(), 1);
            long rowID = update(temp);
            if (rowID > 0) {
                image.setIsFavored(1);
                imagesAddedSuccessfullyArrayList.add(image);
            } else {
                Log.e("aaaa", "Unable to add image " + image.getPath() + "to album");
            }
        }

        Intent resultIntent = new Intent();
        resultIntent.putExtra("selectedImages", imagesAddedSuccessfullyArrayList);
        activity.setResult(Activity.RESULT_OK, resultIntent);
        activity.finish();
    }

    private void chooseImagesToAddToAlbum() {
        Intent intent = new Intent(context, ChooseImagesActivity.class);
        intent.putExtra("album", album);
        intent.putExtra("action", Constants.ACTION_ADD_IMAGES);
        startIntentAddImages.launch(intent);
    }

    private void chooseImagesToAddToFavorites() {
        Intent intent = new Intent(context, ChooseImagesActivity.class);
        intent.putExtra("action", Constants.ACTION_CHOOSE_FAVORITE_IMAGES);
        startIntentAddImages.launch(intent);
    }

    private void clickBackPress() {
        if (imageAdapter.isInMultiSelectMode()) {
            exitMultiselectMode();
        } else {
            Intent resultIntent = new Intent();
            activity.setResult(Activity.RESULT_OK, resultIntent);
            activity.finish();
        }
    }

    private void initActivityResultLauncher() {
        // when click button back in toolbar or in smartphone to finish ImageInfoActivity
        startIntentSeeImageInfo = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        Intent data = result.getData();
                        if (data != null) {
                            ArrayList<Image> newImageArrayList = data.getParcelableArrayListExtra("imageArrayList");

                            if (newImageArrayList != null) {

                                // remove images that users have just removed from favorites if current context is FavoriteImage
                                if (context instanceof FavoriteImagesActivity) {
                                    newImageArrayList.removeIf(image -> image.getIsFavored() == 0);
                                }

                                // update ArrayList
                                imageArrayList.clear();
                                imageArrayList.addAll(newImageArrayList);
                                imageAdapter.notifyDataSetChanged();

                                // update UI
                                totalImages.setText(String.valueOf(imageArrayList.size()));
                            }

                        }
                    }
                }
        );

        // when user finish taking pictures and press back button to return to app
        startIntentTakePicture = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    // load cursor to get data in device
                    Cursor cursor = loadCursor();

                    //get the paths to newly added images
                    String[] paths = getImagePaths(cursor, imagesCountBefore);
                    if (paths != null) {
                        for (String path : paths) {
                            // update to database
                            Image newImage = new Image(path, "", 0);
                            insert(newImage);

                            // add new image to array
                            imageArrayList.add(0, newImage);
                            imageAdapter.notifyItemInserted(0);

                            // update UI
                            totalImages.setText(String.valueOf(imageArrayList.size()));

                            // backup image if user choose auto backup
                            if (mainActivity.isEnableBackup && user != null) {
                                backupImage(path);
                            }
                        }
                    }
                    cursor.close();
                });

        // when delete image successfully in multiselect mode
        startForDeletionLauncher = registerForActivityResult(
                new ActivityResultContracts.StartIntentSenderForResult(),
                result -> {
                    // Handle the result in onActivityResult
                    if (result.getResultCode() == RESULT_OK) {
                        // update to database
                        delete(imagesWantToDelete.get(currentIndex));

                        // update to array
                        imageArrayList.remove(imagesWantToDelete.get(currentIndex));
                        imageAdapter.notifyDataSetChanged();
                        currentIndex++;

                        // set text which shows total image
                        totalImages.setText(String.valueOf(imageArrayList.size()));
                    } else if (result.getResultCode() == RESULT_CANCELED) {
                        currentIndex++;
                    }
                }
        );

        startIntentAddImages = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        Intent data = result.getData();
                        if (data != null) {
                            Image image = data.getParcelableExtra("image");
                            ArrayList<Image> addedImageArrayList = data.getParcelableArrayListExtra("selectedImages");

                            if (image != null) {
                                // add image to album
                                imageArrayList.add(0, image);
                                imageAdapter.notifyItemInserted(0);
                            }

                            if (addedImageArrayList != null) {
                                for (int i = 0; i < addedImageArrayList.size(); i++) {
                                    imageArrayList.add(0, addedImageArrayList.get(i));
                                }
                                imageAdapter.notifyItemRangeInserted(0, addedImageArrayList.size());
                            }

                            totalImages.setText(String.valueOf(imageArrayList.size()));
                        }
                    }
                });
    }

    private void backupImage(String path) {
        // create notification through service
        Intent intent = new Intent(context, ServiceNotification.class);
        intent.putExtra("action", Constants.ACTION_UPLOADING);
        context.startService(intent);

        Uri file = Uri.fromFile(new File(path));
        StorageReference storageRef = FirebaseStorage.getInstance().getReferenceFromUrl(getString(R.string.folder_path_storage));
        StorageReference riversRef = storageRef.child(user.getEmail() + "/" + file.getLastPathSegment());
        UploadTask uploadTask = riversRef.putFile(file);

        // Register observers to listen for when the download fails
        uploadTask.addOnFailureListener(exception ->
                        Toast.makeText(context, exception.getMessage(), Toast.LENGTH_SHORT).show())
                .addOnSuccessListener(taskSnapshot -> {
                    Intent intent2 = new Intent(context, ServiceNotification.class);
                    intent2.putExtra("action", Constants.ACTION_UPLOAD_COMPLETE);
                    context.startService(intent2);
                });
    }

    private void loadImagesInAlbum() {
        String sql = "SELECT * FROM Album_Contain_Images AS Contain, Image AS I " +
                "WHERE id_album = ? AND Contain.path = I.path";
        String[] args = {String.valueOf(album.getId())};

        Cursor cursorContainImages = SqliteDatabase.db.rawQuery(sql, args);
        cursorContainImages.moveToPosition(-1);

        int pathImageColumn = cursorContainImages.getColumnIndex("Contain.path");
        int descriptionImageColumn = cursorContainImages.getColumnIndex("I.description");
        int isFavoredImageColumn = cursorContainImages.getColumnIndex("I.isFavored");

        String pathImageInAlbum;
        String descriptionImageInAlbum;
        int isFavoredImageInAlbum;

        while (cursorContainImages.moveToNext()) {
            descriptionImageInAlbum = cursorContainImages.getString(descriptionImageColumn);
            isFavoredImageInAlbum = cursorContainImages.getInt(isFavoredImageColumn);
            pathImageInAlbum = cursorContainImages.getString(pathImageColumn);
            Image image = new Image(pathImageInAlbum, descriptionImageInAlbum, isFavoredImageInAlbum);
            imageArrayList.add(0, image);
        }
        cursorContainImages.close();
        album.setListImage(imageArrayList);

        imageAdapter.notifyDataSetChanged();
        totalImages.setText(String.valueOf(imageArrayList.size()));
    }

    public void loadImagesFromDevice() {
        if (action == Constants.ACTION_CHOOSE_FAVORITE_IMAGES) {
            imageAdapter.setAction(action);
        }

        boolean SDCard = Environment.getExternalStorageState().equals(MEDIA_MOUNTED);
        if (SDCard) {
            final String[] columns = {MediaStore.Images.Media.DATA, MediaStore.Images.Media._ID};
            final String order = MediaStore.Images.Media.DATE_ADDED + " DESC";
            ContentResolver contentResolver = requireActivity().getContentResolver();
            int count;
            Cursor cursor;
            try {
                cursor = contentResolver.query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, columns, null, null, order);
            } catch (Exception e) {
                Log.e("error", "unable to load image");
                return;
            }
            if (cursor != null) {
                count = cursor.getCount();
            } else {
                count = 0;
            }
            totalImages.setText(String.valueOf(count));

            for (int i = 0; i < count; i++) {
                cursor.moveToPosition(i);
                int columnIndex = cursor.getColumnIndex(MediaStore.Images.Media.DATA);
                String imgPath = cursor.getString(columnIndex);

                int isFavored = 0;
                String description = "";
                String[] args = {imgPath};
                Cursor cursor1 = SqliteDatabase.db.rawQuery("SELECT * FROM Image WHERE path = ?", args);

                if (!cursor1.moveToFirst()) {
                    // image that has path = {imgPath} does not exist in database
                    // then add that image to database
                    insert(new Image(imgPath, description, isFavored));

                    // backup image if user choose auto backup
                    if (mainActivity.isEnableBackup && user != null && context instanceof MainActivity) {
                        backupImage(imgPath);
                    }
                } else {
                    // image that has path = {imgPath} exists in database
                    // then get the description and isFavored of that image
                    cursor1.moveToPosition(-1);
                    while (cursor1.moveToNext()) {
                        int favorColumn = cursor1.getColumnIndex("isFavored");
                        int descriptionColumn = cursor1.getColumnIndex("description");
                        isFavored = cursor1.getInt(favorColumn);
                        description = cursor1.getString(descriptionColumn);
                    }
                }
                cursor1.close();

                // add to array
                Image newImage = new Image(imgPath, description, isFavored);
                imageArrayList.add(newImage);

            }

            imageAdapter.notifyDataSetChanged();
            totalImages.setText(String.valueOf(imageArrayList.size()));

            if (cursor != null) {
                cursor.close();
            }

            // if user is choosing image to add to album
            // then check whether each image has already been current album or not
            if (action == Constants.ACTION_ADD_IMAGES) {
                for (int i = 0; i < imageArrayList.size(); i++) {
                    for (int j = 0; j < album.getListImage().size(); j++) {
                        if (imageArrayList.get(i).getPath().equals(album.getListImage().get(j).getPath())) {
                            imageArrayList.get(i).setCanAddToCurrentAlbum(false);
                        }
                    }
                }
            }
        }
    }

    private void loadFavoriteImages() {
        String sql = "SELECT * FROM Image WHERE isFavored = 1";
        Cursor cursor = SqliteDatabase.db.rawQuery(sql, null);
        cursor.moveToPosition(-1);

        int pathImageColumn = cursor.getColumnIndex("path");
        int descriptionImageColumn = cursor.getColumnIndex("description");
        int isFavoredImageColumn = cursor.getColumnIndex("isFavored");

        while (cursor.moveToNext()) {
            String descriptionImageInAlbum = cursor.getString(descriptionImageColumn);
            int isFavoredImageInAlbum = cursor.getInt(isFavoredImageColumn);
            String pathImageInAlbum = cursor.getString(pathImageColumn);
            Image image = new Image(pathImageInAlbum, descriptionImageInAlbum, isFavoredImageInAlbum);
            imageArrayList.add(image);
        }
        cursor.close();

        imageAdapter.notifyDataSetChanged();
        totalImages.setText(String.valueOf(imageArrayList.size()));
    }

    private void loadBackupImages() {
        StorageReference listRef = storageRef.child(user.getEmail() + "/");

        listRef.listAll()
                .addOnSuccessListener(listResult -> {
                    int totalItems = listResult.getItems().size();
                    totalImages.setText(String.valueOf(totalItems));

                    for (StorageReference item : listResult.getItems()) {
                        item.getDownloadUrl().addOnSuccessListener(uri -> {
                            imageArrayList.add(new Image(uri.toString()));
                            imageAdapter.notifyItemInserted(imageArrayList.size() - 1);
                        });
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(context, e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void init() {
        recycler = linearLayout.findViewById(R.id.gallery_recycler);
        imageBtnAddImages = linearLayout.findViewById(R.id.imageBtnCamera);
        totalImages = linearLayout.findViewById(R.id.gallery_total_images);
        toolbar = linearLayout.findViewById(R.id.toolbar);
        titleTotalItems = linearLayout.findViewById(R.id.titleTotalItems);
    }

    void openCamera() {
        Cursor cursor = loadCursor();
        //get current number of images in mediaStore
        imagesCountBefore = cursor.getCount();
        cursor.close();

        Intent intent = new Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA);
        startIntentTakePicture.launch(intent);
    }

    // load cursor after take picrues
    public Cursor loadCursor() {
        final String[] columns = {MediaStore.Images.Media.DATA, MediaStore.Images.Media._ID};
        final String orderBy = MediaStore.Images.Media.DATE_ADDED;
        return context.getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, columns, null, null, orderBy);
    }

    // get image paths of all pictures that user has just taken
    public String[] getImagePaths(Cursor cursor, int startPosition) {
        int size = cursor.getCount() - startPosition;
        if (size <= 0) return null;

        String[] paths = new String[size];
        int dataColumnIndex = cursor.getColumnIndex(MediaStore.Images.Media.DATA);

        for (int i = startPosition; i < cursor.getCount(); i++) {
            cursor.moveToPosition(i);
            paths[i - startPosition] = cursor.getString(dataColumnIndex);
        }

        return paths;
    }

    public void exitMultiselectMode() {
        imageAdapter.setMultiSelectMode(false);
        imageAdapter.clearSelection();
        changeUI();
    }

    private void enterMultiselectMode(int index) {
        imageAdapter.setMultiSelectMode(true);
        imageAdapter.toggleSelection(index);
        changeUI();
    }

    // change UI of activity when enter or exit multi selection mode
    private void changeUI() {
        if (imageAdapter.isInMultiSelectMode()) {
            if (context instanceof MainActivity) {
                mainActivity.hideBottomNavigationView();
                imageBtnAddImages.setVisibility(View.GONE);
                toolbar.setVisibility(View.VISIBLE);
                mainActivity.hideLinearLayoutTitle();
            } else if (context instanceof ChooseImagesActivity) {
                imageBtnAddImages.setVisibility(View.VISIBLE);
            } else if (context instanceof FavoriteImagesActivity) {
                imageBtnAddImages.setVisibility(View.GONE);
            } else if (context instanceof AlbumInfoActivity) {
                imageBtnAddImages.setVisibility(View.GONE);
            }

            Objects.requireNonNull(activity.getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
            Objects.requireNonNull(activity.getSupportActionBar()).setHomeAsUpIndicator(R.drawable.close_icon);

            activity.invalidateOptionsMenu();
        } else {
            if (context instanceof MainActivity) {
                mainActivity.showBottomNavigationView();
                imageBtnAddImages.setVisibility(View.VISIBLE);
                toolbar.setVisibility(View.GONE);
                mainActivity.showLinearLayoutTitle();
            } else if (context instanceof ChooseImagesActivity) {
                imageBtnAddImages.setVisibility(View.GONE);
                Objects.requireNonNull(
                        activity.getSupportActionBar()).setHomeAsUpIndicator(androidx.appcompat.R.drawable.abc_ic_ab_back_material);
            } else if (context instanceof FavoriteImagesActivity) {
                imageBtnAddImages.setVisibility(View.VISIBLE);
                Objects.requireNonNull(
                        activity.getSupportActionBar()).setHomeAsUpIndicator(androidx.appcompat.R.drawable.abc_ic_ab_back_material);
            } else if (context instanceof AlbumInfoActivity) {
                imageBtnAddImages.setVisibility(View.VISIBLE);
            } else if (context instanceof BackupImagesActivity) {
                Objects.requireNonNull(
                        activity.getSupportActionBar()).setHomeAsUpIndicator(androidx.appcompat.R.drawable.abc_ic_ab_back_material);
            }

            activity.invalidateOptionsMenu();
        }
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        if (!(context instanceof AlbumInfoActivity)) {
            if (imageAdapter.isInMultiSelectMode()) {
                requireActivity().getMenuInflater().inflate(R.menu.menu_image_home_page_long_click, menu);

                // hide menu if current activity is ChooseImagesActivity
                if (!(context instanceof FavoriteImagesActivity)) {
                    menu.findItem(R.id.removeImagesFromFavorites).setVisible(false);
                }

                if (context instanceof ChooseImagesActivity) {
                    menu.findItem(R.id.deleteImages).setVisible(false);
                    menu.findItem(R.id.removeImagesFromFavorites).setVisible(false);
                    menu.findItem(R.id.slideshowImages).setVisible(false);
                    menu.findItem(R.id.backupImages).setVisible(false);
                }

                if (context instanceof BackupImagesActivity) {
                    menu.findItem(R.id.deleteImages).setVisible(false);
                    menu.findItem(R.id.slideshowImages).setVisible(false);
                    menu.findItem(R.id.removeImagesFromFavorites).setVisible(false);
                    menu.findItem(R.id.backupImages).setVisible(false);
                } else {
                    menu.findItem(R.id.downloadBackupImages).setVisible(false);
                    menu.findItem(R.id.deleteBackupImages).setVisible(false);
                }
            }
        }

        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemID = item.getItemId();
        if (itemID == R.id.deleteImages) {
            deleteChosenImages();
        } else if (itemID == R.id.slideshowImages) {
            slideshowImages();
        } else if (itemID == R.id.removeImagesFromFavorites) {
            removeImagesFromFavorites();
        } else if (itemID == R.id.selectAllImages) {
            selectAllImages();
        } else if (itemID == R.id.backupImages) {
            backupImages();
            exitMultiselectMode();
        } else if (itemID == R.id.deleteBackupImages) {
            deleteBackupImages();
        } else if (itemID == R.id.downloadBackupImages) {
            try {
                downloadBackupImages();
            } catch (IOException e) {
                Log.e("aaaa", Objects.requireNonNull(e.getMessage()));
            }
        }
        return super.onOptionsItemSelected(item);
    }

    private void downloadBackupImages() throws IOException {
        ArrayList<Image> selectedImages = imageAdapter.getSelectedImages();
        FirebaseStorage storage = FirebaseStorage.getInstance();

        // create notification through service
        Intent intent = new Intent(context, ServiceNotification.class);
        intent.putExtra("action", Constants.ACTION_DOWNLOADING);
        intent.putExtra("progress", 0);
        intent.putExtra("max", selectedImages.size());
        context.startService(intent);

        AtomicInteger progress = new AtomicInteger(0);
        AtomicInteger total = new AtomicInteger(selectedImages.size());

        for (Image image : selectedImages) {
            StorageReference storageReference = storage.getReferenceFromUrl(image.getPath());
            File localFile = File.createTempFile("images" + (int) new Date().getTime(), "jpg");

            storageReference.getFile(localFile).addOnSuccessListener(taskSnapshot -> {
                // move file to external storage
                copyFileToExternalStorage(context, localFile);

                // change content of notification if all images were downloaded
                int currentProgress = progress.incrementAndGet();
                if (currentProgress >= total.get()) {
                    // download complete
                    Intent intent2 = new Intent(context, ServiceNotification.class);
                    intent2.putExtra("action", Constants.ACTION_DOWNLOAD_COMPLETE);
                    context.startService(intent2);
                } else {
                    // update progress bar
                    Intent intent2 = new Intent(context, ServiceNotification.class);
                    intent2.putExtra("action", Constants.ACTION_DOWNLOADING);
                    intent2.putExtra("progress", currentProgress);
                    intent2.putExtra("max", total.get());
                    context.startService(intent2);
                }
            }).addOnFailureListener(e -> Log.e("aaaa", Objects.requireNonNull(e.getMessage())));
        }

        exitMultiselectMode();
    }

    private void deleteBackupImages() {
        ArrayList<Image> selectedImages = imageAdapter.getSelectedImages();
        totalImages.setText(String.valueOf(imageArrayList.size() - selectedImages.size()));

        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference storageReference;

        for (Image image : selectedImages) {
            // Delete file
            storageReference = storage.getReferenceFromUrl(image.getPath());
            storageReference.delete()
                    .addOnSuccessListener(unused -> {
                        imageArrayList.remove(image);
                        imageAdapter.notifyDataSetChanged();
                    })
                    .addOnFailureListener(exception -> Log.e("aaaa", Objects.requireNonNull(exception.getMessage())));
        }

        exitMultiselectMode();
    }

    public void backupImages() {
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        FirebaseUser user = mAuth.getCurrentUser();

        // ask user to sign in if they haven't
        if (user == null) {
            MaterialAlertDialogBuilder dialogBuilder = new MaterialAlertDialogBuilder(context);
            dialogBuilder.setTitle("Please sign in to use this function");

            dialogBuilder.setPositiveButton("Ok", (dialog, id) -> {
                dialog.dismiss();
            });

            dialogBuilder.show();
            return;
        }

        ArrayList<Image> selectedImages = imageAdapter.getSelectedImages();

        // create notification through service
        Intent intent = new Intent(context, ServiceNotification.class);
        intent.putExtra("action", Constants.ACTION_UPLOADING);
        intent.putExtra("progress", 0);
        intent.putExtra("max", selectedImages.size());
        context.startService(intent);

        // count the number of images that complete uploading
        AtomicInteger progress = new AtomicInteger(0);
        AtomicInteger total = new AtomicInteger(selectedImages.size());

        for (Image image : selectedImages) {
            Uri file = Uri.fromFile(new File(image.getPath()));
            StorageReference riversRef = storageRef.child(user.getEmail() + "/" + file.getLastPathSegment());
            UploadTask uploadTask = riversRef.putFile(file);

            // change content of notification if all images were uploaded
            uploadTask.addOnSuccessListener(taskSnapshot -> {
                int currentProgress = progress.incrementAndGet();
                if (currentProgress >= total.get()) {
                    // upload complete
                    Intent intent2 = new Intent(context, ServiceNotification.class);
                    intent2.putExtra("action", Constants.ACTION_UPLOAD_COMPLETE);
                    context.startService(intent2);
                } else {
                    // update progress bar
                    Intent intent2 = new Intent(context, ServiceNotification.class);
                    intent2.putExtra("action", Constants.ACTION_UPLOADING);
                    intent2.putExtra("progress", currentProgress);
                    intent2.putExtra("max", total.get());
                    context.startService(intent2);
                }
            });
        }
    }

    public void deleteChosenImages() {
        ArrayList<Image> selectedImages = imageAdapter.getSelectedImages();
        if (selectedImages.size() == 0) {
            Toast.makeText(context, "You have not chosen any images", Toast.LENGTH_SHORT).show();
            return;
        }

        ArrayList<Integer> selectedPositions = imageAdapter.getSelectedPositions();

        imagesWantToDelete = new ArrayList<>();
        for (int i = 0; i < selectedImages.size(); i++) {
            Image image = selectedImages.get(i);
            int position = selectedPositions.get(i);
            deleteImage(image, position);
        }
        // set text which shows total image
        // text of totalImages only change when the code block in catch does not execute (in api 28)
        totalImages.setText(String.valueOf(imageArrayList.size()));

        exitMultiselectMode();
    }

    // delete image that has index = {position} in array
    private void deleteImage(Image image, int position) {
        currentIndex = 0;
        Uri uri = getUriFromPath(context, new File(image.getPath()));
        if (uri == null) {
            Log.e("error", "cannot get uri from path when delete image");
            return;
        }
        try {
            int row = context.getContentResolver().delete(uri, null, null);
            if (row > 0) {
                // update to database
                delete(image);
                // update to array
                imageArrayList.remove(image);
                imageAdapter.notifyItemRemoved(position);
            } else {
                Toast.makeText(context, "Unable to delete", Toast.LENGTH_SHORT).show();
            }
        } catch (SecurityException e) {
            imagesWantToDelete.add(0, image);
            PendingIntent pendingIntent = null;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                ArrayList<Uri> uriArrayList = new ArrayList<>();
                uriArrayList.add(uri);
                pendingIntent = MediaStore.createDeleteRequest(context.getContentResolver(), uriArrayList);
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

                            imageArrayList.remove(image);
                            imageAdapter.notifyItemRemoved(position);
                        }
                    } else {
                        Log.e("error", "Cannot delete image");
                    }
                }

            }
            if (pendingIntent != null) {
                IntentSender intentSender = pendingIntent.getIntentSender();
                try {
                    IntentSenderRequest request = new IntentSenderRequest.Builder(intentSender).build();
                    startForDeletionLauncher.launch(request);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

    public void slideshowImages() {
        ArrayList<Image> selectedImages = imageAdapter.getSelectedImages();
        if (selectedImages.size() == 0) {
            Toast.makeText(context, "You have not chosen any images", Toast.LENGTH_SHORT).show();
            return;
        }

        startSlideshowActivity(selectedImages);
    }

    public void startSlideshowActivity(ArrayList<Image> selectedImages) {
        Intent intent = new Intent(context, SlideshowActivity.class);
        intent.putExtra("selectedImages", selectedImages);
        startActivity(intent);
    }

    private void removeImagesFromFavorites() {
        ArrayList<Image> selectedImages = imageAdapter.getSelectedImages();
        if (selectedImages.size() == 0) {
            Toast.makeText(context, "You have not chosen any images", Toast.LENGTH_SHORT).show();
            return;
        }

        for (Image image : selectedImages) {
            Image temp = new Image(image.getPath(), image.getDescription(), 0);
            long rowID = update(temp);
            if (rowID > 0) {
                imageArrayList.remove(image);
            }
        }

        imageAdapter.notifyDataSetChanged();
        exitMultiselectMode();
        totalImages.setText(String.valueOf(imageArrayList.size()));
    }

    private void selectAllImages() {
        ArrayList<Image> selectedImages = imageAdapter.getSelectedImages();
        ArrayList<Integer> selectedPositions = imageAdapter.getSelectedPositions();

        selectedImages.clear();
        selectedPositions.clear();

        if (context instanceof ChooseImagesActivity) {
            for (int i = 0; i < imageArrayList.size(); i++) {
                Image image = imageArrayList.get(i);
                if (action == Constants.ACTION_CHOOSE_FAVORITE_IMAGES) {
                    if (image.getIsFavored() == 0) {
                        selectedImages.add(image);
                        selectedPositions.add(i);
                    }
                } else if (action == Constants.ACTION_ADD_IMAGES) {
                    if (image.isCanAddToCurrentAlbum()) {
                        selectedImages.add(image);
                        selectedPositions.add(i);
                    }
                }
            }
        } else {
            selectedImages.addAll(imageArrayList);
            for (int i = 0; i < imageArrayList.size(); i++) {
                selectedPositions.add(i);
            }
        }

        imageAdapter.notifyDataSetChanged();
    }
}