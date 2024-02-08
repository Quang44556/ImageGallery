package com.example.imagesgallery.Activity;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.example.imagesgallery.Fragment.AlbumFragment;
import com.example.imagesgallery.R;

public class FavoriteAlbumsActivity extends AppCompatActivity {

    AlbumFragment albumFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_favorite_albums);

        albumFragment = new AlbumFragment();
        getSupportFragmentManager().beginTransaction().replace(R.id.container, albumFragment).commit();
    }
}