package com.example.imagesgallery.Activity;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import com.example.imagesgallery.Fragment.AlbumFragment;
import com.example.imagesgallery.Fragment.ImageFragment;
import com.example.imagesgallery.R;

public class ChooseImagesActivity extends AppCompatActivity {

    ImageFragment imageFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_choose_images);

        imageFragment = new ImageFragment();
        getSupportFragmentManager().beginTransaction().replace(R.id.container, imageFragment).commit();
    }
}