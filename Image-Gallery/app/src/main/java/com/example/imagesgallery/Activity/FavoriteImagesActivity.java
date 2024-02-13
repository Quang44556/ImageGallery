package com.example.imagesgallery.Activity;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.imagesgallery.Fragment.AlbumFragment;
import com.example.imagesgallery.Fragment.ImageFragment;
import com.example.imagesgallery.R;

import java.util.Objects;

public class FavoriteImagesActivity extends AppCompatActivity {

    ImageFragment imageFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_favorite_images);

        imageFragment = new ImageFragment();
        getSupportFragmentManager().beginTransaction().replace(R.id.container, imageFragment).commit();
    }

}