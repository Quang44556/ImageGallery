package com.example.imagesgallery.Fragment;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.imagesgallery.Activity.FavoriteAlbumsActivity;
import com.example.imagesgallery.Activity.FavoriteImagesActivity;
import com.example.imagesgallery.R;

public class PersonalFragment extends Fragment {
    Button btnFavoriteAlbums, btnFavoriteImages;
    Context context;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        context = getContext();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_personal, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        btnFavoriteAlbums = view.findViewById(R.id.buttonFavoriteAlbums);
        btnFavoriteImages = view.findViewById(R.id.buttonFavoriteImages);

        btnFavoriteAlbums.setOnClickListener(view1 -> {
            Intent intent = new Intent(context, FavoriteAlbumsActivity.class);
            startActivity(intent);
        });

        btnFavoriteImages.setOnClickListener(view2 -> {
            Intent intent = new Intent(context, FavoriteImagesActivity.class);
            startActivity(intent);
        });
    }
}
