package com.example.imagesgallery.Fragment;

import static com.example.imagesgallery.Database.SqliteDatabase.delete;
import static com.example.imagesgallery.Database.SqliteDatabase.insert;
import static com.example.imagesgallery.Database.SqliteDatabase.update;

import android.app.Activity;
import android.app.Dialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;

import com.example.imagesgallery.Activity.AddFavoriteAlbumActivity;
import com.example.imagesgallery.Activity.AlbumInfoActivity;
import com.example.imagesgallery.Activity.FavoriteAlbumsActivity;
import com.example.imagesgallery.Activity.MainActivity;
import com.example.imagesgallery.Adapter.AlbumAdapter;
import com.example.imagesgallery.Database.SqliteDatabase;
import com.example.imagesgallery.Interface.ClickListener;
import com.example.imagesgallery.Model.Album;
import com.example.imagesgallery.Model.Image;
import com.example.imagesgallery.R;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.Objects;


public class AlbumFragment extends Fragment {

    // Default: default album (not found through search)
    // Search: albums are found through search
    // Current: current album (default, search)
    ArrayList<Album> DefaultAlbumArrayList, SearchAlbumArrayList, CurrentAlbumArrayList;
    int CurrentClickPosition = -1, DefaultAlbumClickPosition = -1;
    GridView gridView;
    AlbumAdapter albumAdapter;
    ImageButton btnAddAlbum;
    Button btnAdd, btnCancel;
    EditText edtNameAlbum;
    TextView txtTitleDialog;
    Dialog dialog;
    MainActivity mainActivity;
    Context context;
    ConstraintLayout constraintLayoutAlbum;
    ContentValues rowValues;
    Toolbar toolbar;
    private final String DefaultSearchName = "";
    private String SearchName = DefaultSearchName;
    SearchView searchView;
    AppCompatActivity activity;
    private ActivityResultLauncher<Intent> startIntentAlbumInfo, startIntentAddAlbumsToFavorites;
    ClickListener clickListener = new ClickListener() {
        @Override
        public void click(int index) {
            if (albumAdapter.isInMultiSelectMode()) {
                // set chosen album checked
                albumAdapter.toggleSelection(index);
                albumAdapter.notifyDataSetChanged();
            } else {
                CurrentClickPosition = index;
                if (context instanceof MainActivity || context instanceof FavoriteAlbumsActivity) {
                    // see information of album
                    Intent intent = new Intent(context, AlbumInfoActivity.class);

                    if (CurrentAlbumArrayList == SearchAlbumArrayList) {
                        for (int i = 0; i < DefaultAlbumArrayList.size(); i++) {
                            if (DefaultAlbumArrayList.get(i).getId() == CurrentAlbumArrayList.get(CurrentClickPosition).getId()) {
                                DefaultAlbumClickPosition = i;
                            }
                        }
                    }

                    intent.putExtra("album", CurrentAlbumArrayList.get(CurrentClickPosition));
                    startIntentAlbumInfo.launch(intent);
                } else if (context instanceof AddFavoriteAlbumActivity) {
                    // add clicked album to favorites
                    addAlbumToFavorites(CurrentClickPosition);
                }
            }
        }

        @Override
        public void longClick(int index) {
            if (CurrentAlbumArrayList == DefaultAlbumArrayList) {
                enterMultiselectMode(index);
            }
        }
    };

    @Override
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
        constraintLayoutAlbum = (ConstraintLayout) inflater.inflate(R.layout.fragment_album, container, false);
        return constraintLayoutAlbum;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        init();

        if (context instanceof MainActivity){
            mainActivity.hideLinearLayoutTitle();
        }

        // set minimum items per row of gridview = 2
        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        float screenWidthInDp = displayMetrics.widthPixels / displayMetrics.density;
        int itemWidth = 200; // size of an image
        int ColumnCount = (int) screenWidthInDp / itemWidth; // the number of images in a row
        if (ColumnCount < 2) {
            gridView.setNumColumns(2);
        }

        DefaultAlbumArrayList = new ArrayList<>();
        SearchAlbumArrayList = new ArrayList<>();
        CurrentAlbumArrayList = DefaultAlbumArrayList;
        albumAdapter = new AlbumAdapter(context, DefaultAlbumArrayList, clickListener);
        rowValues = new ContentValues();
        gridView.setAdapter(albumAdapter);

        if (context instanceof AddFavoriteAlbumActivity) {
            btnAddAlbum.setVisibility(View.GONE);
        }

        loadDataFromDatabase(SearchName, CurrentAlbumArrayList);

        // set toolbar
        activity = (AppCompatActivity) getActivity();
        if (activity != null) {
            activity.setSupportActionBar(toolbar);
            if (context instanceof MainActivity) {
                Objects.requireNonNull(activity.getSupportActionBar()).setTitle("");
            } else {
                if (context instanceof FavoriteAlbumsActivity) {
                    Objects.requireNonNull(activity.getSupportActionBar()).setTitle("Favorite albums");
                } else if (context instanceof AddFavoriteAlbumActivity) {
                    Objects.requireNonNull(activity.getSupportActionBar()).setTitle("Add album to favorites");
                }
                Objects.requireNonNull(activity.getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
            }
        }

        toolbar.setNavigationOnClickListener(view1 -> finishActivityOnBackPress());

        // set back press in device
        activity.getOnBackPressedDispatcher().addCallback(new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                finishActivityOnBackPress();
            }
        });

        // when click button add of activity
        btnAddAlbum.setOnClickListener(view12 -> {
            if (context instanceof MainActivity) { // add new album
                showDialog();
            } else if (context instanceof FavoriteAlbumsActivity) { // add an exist album to favorites
                Intent intent = new Intent(context, AddFavoriteAlbumActivity.class);
                startIntentAddAlbumsToFavorites.launch(intent);
            } else if (context instanceof AddFavoriteAlbumActivity) { // add chosen albums to favorites
                addAlbumsToFavorites();
            }
        });
    }

    private void finishActivityOnBackPress() {
        if (albumAdapter.isInMultiSelectMode()) {
            // cancel multi select mode
            exitMultiselectMode();
        } else {
            activity.finish();
        }
    }

    private void addAlbumToFavorites(int clickPos) {
        Album album = CurrentAlbumArrayList.get(clickPos);
        Album temp = new Album(
                album.getCover(),
                album.getName(),
                album.getDescription(),
                1,
                album.getId()
        );
        long rowID = update(temp);
        if (rowID > 0) {
            CurrentAlbumArrayList.get(CurrentClickPosition).setIsFavored(1);
            finishAddFavoriteAlbumActivity();
        } else {
            Toast.makeText(context, "Unable to add album " + album.getName() + " to favorites", Toast.LENGTH_SHORT).show();
        }
    }

    private void finishAddFavoriteAlbumActivity() {
        Intent resultIntent = new Intent();
        if (albumAdapter.isInMultiSelectMode()) {
            ArrayList<Album> selectedAlbums = albumAdapter.getSelectedAlbums();
            resultIntent.putExtra("AlbumsAddedToFavorites", selectedAlbums);
        } else {
            Album album = CurrentAlbumArrayList.get(CurrentClickPosition);
            resultIntent.putExtra("AlbumAddedToFavorites", album);
        }
        activity.setResult(Activity.RESULT_OK, resultIntent);
        activity.finish();
    }

    // Load album from database and add to arraylist
    private void loadDataFromDatabase(String SearchName, ArrayList<Album> albumArrayList) {
        String sql = "";
        Cursor cursor;

        String[] argsAlbum = {"%" + SearchName + "%"};
        try {
            if (context instanceof MainActivity || context instanceof AddFavoriteAlbumActivity) {
                sql = "SELECT * FROM Album WHERE name LIKE ? ORDER BY id_album DESC";
            } else if (context instanceof FavoriteAlbumsActivity) {
                sql = "SELECT * FROM Album WHERE isFavored = 1 AND name LIKE ? ORDER BY id_album DESC";
            }
            cursor = SqliteDatabase.db.rawQuery(sql, argsAlbum);
        } catch (Exception exception) {
            Toast.makeText(context, "Some errors have occurred while loading data", Toast.LENGTH_SHORT).show();
            return;
        }

        cursor.moveToPosition(-1);
        // load data and add album to arrayList
        while (cursor.moveToNext()) {
            int idAlbumColumn = cursor.getColumnIndex("id_album");
            int descriptionAlbumColumn = cursor.getColumnIndex("description");
            int nameAlbumColumn = cursor.getColumnIndex("name");
            int isFavoredAlbumColumn = cursor.getColumnIndex("isFavored");
            int coverAlbumColumn = cursor.getColumnIndex("cover");

            int idAlbum = cursor.getInt(idAlbumColumn);
            String descriptionAlbum = cursor.getString(descriptionAlbumColumn);
            String nameAlbum = cursor.getString(nameAlbumColumn);
            int isFavoredAlbum = cursor.getInt(isFavoredAlbumColumn);
            String coverAlbum = cursor.getString(coverAlbumColumn);

            String[] args = {coverAlbum};
            Cursor cursorImage;
            try {
                cursorImage = SqliteDatabase.db.rawQuery("SELECT * FROM Image WHERE path = ?", args);
            } catch (Exception exception) {
                return;
            }
            cursorImage.moveToPosition(-1);
            int pathImageColumn = cursorImage.getColumnIndex("path");
            int descriptionImageColumn = cursorImage.getColumnIndex("description");
            int isFavoredImageColumn = cursorImage.getColumnIndex("isFavored");

            String pathImage = MainActivity.pathNoImage;
            String descriptionImage = "";
            int isFavoredImage = 0;

            while (cursorImage.moveToNext()) {
                descriptionImage = cursorImage.getString(descriptionImageColumn);
                isFavoredImage = cursorImage.getInt(isFavoredImageColumn);
                pathImage = cursorImage.getString(pathImageColumn);
            }
            cursorImage.close();

            albumArrayList.add(new Album(new Image(pathImage, descriptionImage, isFavoredImage),
                    nameAlbum, descriptionAlbum, isFavoredAlbum, idAlbum));
        }

        cursor.close();
    }

    private void init() {
        gridView = constraintLayoutAlbum.findViewById(R.id.gridview_album);
        btnAddAlbum = constraintLayoutAlbum.findViewById(R.id.btnAdd_album);
        toolbar = constraintLayoutAlbum.findViewById(R.id.toolbar);
    }

    // show dialog when click button add album
    private void showDialog() {
        dialog = new Dialog(context);
        dialog.setContentView(R.layout.dialog_add_album);

        btnAdd = dialog.findViewById(R.id.buttonAdd);
        btnCancel = dialog.findViewById(R.id.buttonCancel);
        edtNameAlbum = dialog.findViewById(R.id.edtAlbumName);
        txtTitleDialog = dialog.findViewById(R.id.title_dialog);

        // when click button add of dialog
        btnAdd.setOnClickListener(view -> {
            String name = edtNameAlbum.getText().toString();
            if (name.equals("")) {
                Toast.makeText(context, "Enter name of album", Toast.LENGTH_SHORT).show();
            } else {
                Album temp = new Album(new Image(MainActivity.pathNoImage, "", 0), name, "", 0, 0);
                long rowId = insert(temp);
                if (rowId > 0) {
                    DefaultAlbumArrayList.add(0, new Album(new Image(MainActivity.pathNoImage, "", 0),
                            name, "", 0, (int) rowId));
                    albumAdapter.notifyDataSetChanged();
                    dialog.dismiss();
                } else {
                    Toast.makeText(mainActivity, "Unable to add, please try again", Toast.LENGTH_SHORT).show();
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
        int width = context.getApplicationContext().getResources().getDisplayMetrics().widthPixels;
        Objects.requireNonNull(dialog.getWindow()).setLayout((6 * width) / 7, ViewGroup.LayoutParams.WRAP_CONTENT);

        // get screen size
        DisplayMetrics displayMetrics = Resources.getSystem().getDisplayMetrics();
        float screenWidth = displayMetrics.widthPixels;

        // resize text size
        float newTextSize = screenWidth * 0.05f;
        edtNameAlbum.setTextSize(TypedValue.COMPLEX_UNIT_PX, newTextSize);

        newTextSize = screenWidth * 0.08f;
        txtTitleDialog.setTextSize(TypedValue.COMPLEX_UNIT_PX, newTextSize);

        newTextSize = screenWidth * 0.04f;
        btnAdd.setTextSize(TypedValue.COMPLEX_UNIT_PX, newTextSize);
        btnCancel.setTextSize(TypedValue.COMPLEX_UNIT_PX, newTextSize);
    }

    private void exitMultiselectMode() {
        albumAdapter.setMultiSelectMode(false);
        albumAdapter.clearSelection();
        changeUI();
    }

    private void enterMultiselectMode(int index) {
        albumAdapter.setMultiSelectMode(true);
        albumAdapter.toggleSelection(index);
        changeUI();
    }

    // change UI of activity when enter or exit multi selection mode
    private void changeUI() {
        if (albumAdapter.isInMultiSelectMode()) {
            if (context instanceof MainActivity) {
                mainActivity.hideBottomNavigationView();
            }
            if (context instanceof AddFavoriteAlbumActivity) {
                btnAddAlbum.setVisibility(View.VISIBLE);
            } else {
                btnAddAlbum.setVisibility(View.GONE);
            }

            Objects.requireNonNull(activity.getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
            Objects.requireNonNull(activity.getSupportActionBar()).setHomeAsUpIndicator(R.drawable.close_icon);

            activity.invalidateOptionsMenu();
        } else {
            if (context instanceof MainActivity) {
                mainActivity.showBottomNavigationView();
                Objects.requireNonNull(activity.getSupportActionBar()).setDisplayHomeAsUpEnabled(false);
            }
            if (context instanceof AddFavoriteAlbumActivity) {
                btnAddAlbum.setVisibility(View.GONE);
                Objects.requireNonNull(activity.getSupportActionBar()).setHomeAsUpIndicator(androidx.appcompat.R.drawable.abc_ic_ab_back_material);
            } else {
                btnAddAlbum.setVisibility(View.VISIBLE);
                Objects.requireNonNull(activity.getSupportActionBar()).setHomeAsUpIndicator(androidx.appcompat.R.drawable.abc_ic_ab_back_material);
            }
            activity.invalidateOptionsMenu();
        }
    }

    private void addAlbumsToFavorites() {
        ArrayList<Album> selectedAlbums = albumAdapter.getSelectedAlbums();

        for (Album album : selectedAlbums) {
            // change database
            Album temp = new Album(
                    album.getCover(),
                    album.getName(),
                    album.getDescription(),
                    1,
                    album.getId()
            );
            long rowID = update(temp);
            if (rowID > 0) {
                album.setIsFavored(1);
            } else {
                Log.e("aaaa", "Unable to add album " + album.getName() + " to favorite");
            }
        }
        finishAddFavoriteAlbumActivity();
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        if (albumAdapter.isInMultiSelectMode()) {
            requireActivity().getMenuInflater().inflate(R.menu.menu_album_home_page_long_click, menu);

            if (context instanceof MainActivity) {
                menu.findItem(R.id.removeAlbumsFromFavorites).setVisible(false);
            } else if (context instanceof AddFavoriteAlbumActivity) {
                menu.findItem(R.id.removeAlbumsFromFavorites).setVisible(false);
                menu.findItem(R.id.deleteAlbums).setVisible(false);
            }
        } else {
            requireActivity().getMenuInflater().inflate(R.menu.menu_album_home_page, menu);

            MenuItem menuItemSearch = menu.findItem(R.id.search_album);
            searchView = (SearchView) menuItemSearch.getActionView();
            if (searchView != null) {
                searchView.setMaxWidth(Integer.MAX_VALUE);

                // when click enter to search
                searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                    @Override
                    public boolean onQueryTextSubmit(String query) {
                        // hide other views
                        btnAddAlbum.setVisibility(View.GONE);
                        if (context instanceof MainActivity) {
                            mainActivity.hideBottomNavigationView();
                        }

                        // load data
                        SearchName = query;
                        SearchAlbumArrayList.clear();
                        CurrentAlbumArrayList = SearchAlbumArrayList;
                        albumAdapter.setAlbumArrayList(CurrentAlbumArrayList);
                        loadDataFromDatabase(SearchName, CurrentAlbumArrayList);
                        searchView.clearFocus();
                        albumAdapter.notifyDataSetChanged();
                        return true;
                    }

                    @Override
                    public boolean onQueryTextChange(String newText) {
                        return false;
                    }
                });
            }

            menuItemSearch.setOnActionExpandListener(new MenuItem.OnActionExpandListener() {
                // when click search button
                @Override
                public boolean onMenuItemActionExpand(@NonNull MenuItem menuItem) {
                    return true;
                }

                // when click button back on SearchView
                @Override
                public boolean onMenuItemActionCollapse(@NonNull MenuItem menuItem) {
                    // show other views
                    if (context instanceof MainActivity || context instanceof FavoriteAlbumsActivity) {
                        btnAddAlbum.setVisibility(View.VISIBLE);
                    }
                    if (context instanceof MainActivity) {
                        mainActivity.showBottomNavigationView();
                    }

                    // load data
                    SearchName = "";
                    SearchAlbumArrayList.clear();
                    CurrentAlbumArrayList = DefaultAlbumArrayList;
                    albumAdapter.setAlbumArrayList(CurrentAlbumArrayList);
                    albumAdapter.notifyDataSetChanged();
                    return true;
                }
            });
        }
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemID = item.getItemId();
        if (itemID == R.id.deleteAlbums) {
            createDialogDeleteAlbums();
        } else if (itemID == R.id.removeAlbumsFromFavorites) {
            createDialogRemoveAlbums();
        } else if (itemID == R.id.selectAllAlbums) {
            selectAllAlbums();
        }
        return super.onOptionsItemSelected(item);
    }

    private void createDialogRemoveAlbums() {
        if (albumAdapter.getSelectedAlbums().size() == 0) {
            Toast.makeText(context, "You have not chosen any albums", Toast.LENGTH_SHORT).show();
            return;
        }

        MaterialAlertDialogBuilder dialogBuilder = new MaterialAlertDialogBuilder(context);
        dialogBuilder.setTitle("Are you sure you want to remove these albums from favorites?");

        // click yes
        dialogBuilder.setPositiveButton("Yes", (dialog, id) -> removeAlbumsFromFavorites());
        // click no
        dialogBuilder.setNegativeButton("No", (dialog, id) -> dialog.dismiss());

        dialogBuilder.show();
    }

    private void removeAlbumsFromFavorites() {
        ArrayList<Album> selectedAlbums = albumAdapter.getSelectedAlbums();

        // change database
        for (Album album : selectedAlbums) {
            // change database
            Album temp = new Album(
                    album.getCover(),
                    album.getName(),
                    album.getDescription(),
                    0,
                    album.getId()
            );
            long rowID = update(temp);
            if (rowID > 0) {
                CurrentAlbumArrayList.remove(album);
            } else {
                Log.e("aaaa", "Unable to remove album " + album.getName() + " from favorites");
            }
        }

        albumAdapter.notifyDataSetChanged();
        exitMultiselectMode();
    }

    public void createDialogDeleteAlbums() {
        if (albumAdapter.getSelectedAlbums().size() == 0) {
            Toast.makeText(context, "You have not chosen any albums", Toast.LENGTH_SHORT).show();
            return;
        }

        MaterialAlertDialogBuilder dialogBuilder = new MaterialAlertDialogBuilder(context);

        dialogBuilder.setTitle("Are you sure you want to deletes these albums ?");

        // click yes
        dialogBuilder.setPositiveButton("Yes", (dialog, id) -> deleteAlbums());
        // click no
        dialogBuilder.setNegativeButton("No", (dialog, id) -> dialog.dismiss());

        dialogBuilder.show();
    }

    private void deleteAlbums() {
        ArrayList<Album> selectedAlbums = albumAdapter.getSelectedAlbums();

        // change database
        for (Album album : selectedAlbums) {
            delete(album);
            CurrentAlbumArrayList.remove(album);
        }

        albumAdapter.notifyDataSetChanged();
        exitMultiselectMode();
    }

    private void selectAllAlbums() {
        ArrayList<Album> selectedAlbums = albumAdapter.getSelectedAlbums();
        ArrayList<Integer> selectedPositions = albumAdapter.getSelectedPositions();

        selectedPositions.clear();
        selectedAlbums.clear();

        if (context instanceof AddFavoriteAlbumActivity) {
            for (int i = 0; i < CurrentAlbumArrayList.size(); i++) {
                Album album = CurrentAlbumArrayList.get(i);
                if (album.getIsFavored() == 0) {
                    selectedPositions.add(i);
                    selectedAlbums.add(album);
                }
            }
        } else {
            selectedAlbums.addAll(CurrentAlbumArrayList);
            for (int i = 0; i < CurrentAlbumArrayList.size(); i++) {
                selectedPositions.add(i);
            }
        }

        albumAdapter.notifyDataSetChanged();
    }

    public void initActivityResultLauncher() {
        // when click button back in toolbar or in smartphone to finish AlbumInfoActivity
        startIntentAlbumInfo = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        Intent data = result.getData();
                        if (data != null) {
                            int isDelete = data.getIntExtra("isDelete", 0);
                            Album album = data.getParcelableExtra("album");
                            ArrayList<Image> imageArrayListAfterChange = data.getParcelableArrayListExtra("images");

                            // change images in album if user choose button add image or delete image in album
                            if (imageArrayListAfterChange != null) {
                                CurrentAlbumArrayList.get(CurrentClickPosition).setListImage(imageArrayListAfterChange);
                                if (CurrentAlbumArrayList == SearchAlbumArrayList) {
                                    DefaultAlbumArrayList.get(DefaultAlbumClickPosition).setListImage(imageArrayListAfterChange);
                                }
                            }
                            // remove data if user choose delete album
                            if (isDelete != 0) {
                                CurrentAlbumArrayList.remove(CurrentClickPosition);
                                if (CurrentAlbumArrayList == SearchAlbumArrayList) {
                                    DefaultAlbumArrayList.remove(DefaultAlbumClickPosition);
                                }
                            } else { // change data of album if user change cover or description
                                CurrentAlbumArrayList.set(CurrentClickPosition, album);

                                if (context instanceof FavoriteAlbumsActivity && Objects.requireNonNull(album).getIsFavored() == 0) {
                                    // if user remove album from Favorites, remove it from arrayList
                                    CurrentAlbumArrayList.remove(CurrentClickPosition);
                                    if (CurrentAlbumArrayList == SearchAlbumArrayList) {
                                        DefaultAlbumArrayList.remove(DefaultAlbumClickPosition);
                                    }

                                } else if (context instanceof MainActivity || context instanceof FavoriteAlbumsActivity) {
                                    // update changes of album to arrayList
                                    if (CurrentAlbumArrayList == SearchAlbumArrayList) {
                                        DefaultAlbumArrayList.set(DefaultAlbumClickPosition, album);
                                    }
                                }
                            }
                            albumAdapter.notifyDataSetChanged();
                        }
                    }
                }
        );

        // after choosing an album to add to favorites and finish AddFavoriteAlbumActivity
        startIntentAddAlbumsToFavorites = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        Intent data = result.getData();
                        if (data != null) {
                            Album addedAlbum = data.getParcelableExtra("AlbumAddedToFavorites");
                            ArrayList<Album> addedAlbums = data.getParcelableArrayListExtra("AlbumsAddedToFavorites");

                            if (addedAlbums == null) {
                                Log.d("aaaa", "null");
                            }

                            if (addedAlbum != null) {
                                CurrentAlbumArrayList.add(0, addedAlbum);
                                albumAdapter.notifyDataSetChanged();
                            }
                            if (addedAlbums != null) {
                                CurrentAlbumArrayList.addAll(0, addedAlbums);
                                albumAdapter.notifyDataSetChanged();
                            }
                        }
                    }
                }
        );
    }
}