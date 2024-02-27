package com.example.imagesgallery.Activity;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.airbnb.lottie.LottieAnimationView;
import com.example.imagesgallery.Adapter.ImageResultAdapter;
import com.example.imagesgallery.Listener.ClickListener;
import com.example.imagesgallery.Model.ImagesResult;
import com.example.imagesgallery.Model.SearchResponse;
import com.example.imagesgallery.Network.SearchApiService;
import com.example.imagesgallery.R;
import com.example.imagesgallery.Utils.NetworkUtils;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SearchOnlineActivity extends AppCompatActivity {

    RecyclerView recyclerView;
    GridLayoutManager manager;
    ImageResultAdapter adapter;
    List<ImagesResult> imagesResultList;
    Toolbar toolbar;
    ProgressBar progressBar;
    Button btnMore;
    LottieAnimationView lottieAnimationView;
    boolean isLoading = false, isOutOfItems = false;
    final String searchEngine = "google_images";
    final String api_key = "cebc987e4389dbbb2d5af1d030d1535ebbfeb2c7f1c8a65c4673796ec1fb7899";
    int ijn = 0;
    String mQuery;

    ClickListener listener = new ClickListener() {
        @Override
        public void click(int index) {
            ImagesResult imagesResult = imagesResultList.get(index);

            Uri webpage = Uri.parse(imagesResult.getLink());
            Intent intent = new Intent(Intent.ACTION_VIEW, webpage);

            // Check if there is an application that can open the URL
            PackageManager packageManager = getPackageManager();
            List<ResolveInfo> activities = packageManager.queryIntentActivities(intent, 0);
            boolean isIntentSafe = activities.size() > 0;

            // Open Chrome browser if available, or open default web browser if not
            if (isIntentSafe) {
                intent.setPackage("com.android.chrome");
            }
            startActivity(intent);
        }

        @Override
        public void longClick(int index) {

        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_online);

        // check if user connected to internet or not
        if (!NetworkUtils.isNetworkAvailable(getApplication())) {
            // ask users to connect to internet if they haven't connected
            MaterialAlertDialogBuilder dialogBuilder = new MaterialAlertDialogBuilder(SearchOnlineActivity.this);
            dialogBuilder.setTitle("Please connect to internet");

            dialogBuilder.setPositiveButton("Ok", (dialog, id) -> dialog.dismiss());

            dialogBuilder.show();
        }

        init();
        imagesResultList = new ArrayList<>();
        adapter = new ImageResultAdapter(getApplicationContext(), imagesResultList, listener);
        recyclerView.setAdapter(adapter);

        // set the number of items in a row
        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        float screenWidthInDp = displayMetrics.widthPixels / displayMetrics.density;
        int imageWidth = 110; // size of an image
        int desiredColumnCount = (int) screenWidthInDp / imageWidth; // the number of images in a row
        manager = new GridLayoutManager(getApplicationContext(), desiredColumnCount);
        recyclerView.setLayoutManager(manager);

        // set toolbar
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("");

        toolbar.setNavigationOnClickListener(view1 -> finish());

        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
            }

            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                // show load more button if user scroll to the end of recycler view
                int totalItemCount = manager.getItemCount();
                int lastVisibleItemPosition = manager.findLastVisibleItemPosition();
                int spanCount = manager.getSpanCount();

                if ((lastVisibleItemPosition + 1) == totalItemCount && totalItemCount >= spanCount && !isLoading && !isOutOfItems) {
                    btnMore.setVisibility(View.VISIBLE);
                } else {
                    btnMore.setVisibility(View.GONE);
                }
            }
        });

        btnMore.setOnClickListener(view -> {
            // load more images
            btnMore.setVisibility(View.GONE);
            progressBar.setVisibility(View.VISIBLE);
            searchImages(mQuery, ijn++);
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_search_online, menu);

        MenuItem menuItemSearch = menu.findItem(R.id.search_online);
        SearchView searchView = (SearchView) menuItemSearch.getActionView();
        if (searchView != null) {
            searchView.setMaxWidth(Integer.MAX_VALUE);

            // when click enter to search
            searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                @Override
                public boolean onQueryTextSubmit(String query) {
                    // clear old data and search new query
                    mQuery = query;
                    lottieAnimationView.setVisibility(View.VISIBLE);
                    recyclerView.setVisibility(View.GONE);
                    imagesResultList.clear();
                    ijn = 0;
                    isOutOfItems = false;

                    searchImages(query, ijn++);
                    searchView.clearFocus();

                    return true;
                }

                @Override
                public boolean onQueryTextChange(String newText) {
                    return false;
                }
            });
        }

        return super.onCreateOptionsMenu(menu);
    }

    private void searchImages(String query, int ijn) {
        isLoading = true;

        SearchApiService.apiService.searchImages(query, searchEngine, String.valueOf(ijn), api_key).enqueue(new Callback<SearchResponse>() {
            @Override
            public void onResponse(@NonNull Call<SearchResponse> call, @NonNull Response<SearchResponse> response) {
                isLoading = false;
                lottieAnimationView.setVisibility(View.GONE);
                recyclerView.setVisibility(View.VISIBLE);
                progressBar.setVisibility(View.GONE);

                if (response.isSuccessful()) {
                    SearchResponse searchResponse = response.body();

                    if (searchResponse != null) {
                        if (searchResponse.getResultList() != null) {
                            // add new images to array
                            List<ImagesResult> newResultList = searchResponse.getResultList();
                            imagesResultList.addAll(newResultList);
                            adapter.notifyDataSetChanged();
                        } else {
                            isOutOfItems = true;
                        }
                    }
                } else {
                    if (response.errorBody() != null) {
                        Toast.makeText(SearchOnlineActivity.this, response.errorBody().toString(), Toast.LENGTH_SHORT).show();
                    }
                }

            }

            @Override
            public void onFailure(@NonNull Call<SearchResponse> call, @NonNull Throwable t) {
                isLoading = false;
                lottieAnimationView.setVisibility(View.GONE);
                recyclerView.setVisibility(View.VISIBLE);

                Toast.makeText(SearchOnlineActivity.this, t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void init() {
        recyclerView = findViewById(R.id.RecyclerView_SearchOnline);
        toolbar = findViewById(R.id.toolbar);
        lottieAnimationView = findViewById(R.id.loading_lottie);
        progressBar = findViewById(R.id.progressBar);
        btnMore = findViewById(R.id.btnMore);
    }
}