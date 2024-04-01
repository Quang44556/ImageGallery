package com.example.imagesgallery.Fragment;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;

import com.example.imagesgallery.Activity.BackupImagesActivity;
import com.example.imagesgallery.Activity.FavoriteAlbumsActivity;
import com.example.imagesgallery.Activity.FavoriteImagesActivity;
import com.example.imagesgallery.Activity.MainActivity;
import com.example.imagesgallery.Activity.SearchOnlineActivity;
import com.example.imagesgallery.R;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class PersonalFragment extends Fragment {
    CardView cardViewFavorite, cardViewBackup, cardViewSearch;
    Context context;
    ImageView imgImage;
    ImageView imgAlbum;
    MainActivity mainActivity;
    FirebaseAuth mAuth;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        context = getContext();
        if (context instanceof MainActivity) {
            mainActivity = (MainActivity) getActivity();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_personal, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mAuth = FirebaseAuth.getInstance();
        FirebaseUser user = mAuth.getCurrentUser();

        cardViewFavorite = view.findViewById(R.id.CardViewFavorite);
        cardViewBackup = view.findViewById(R.id.CardViewBackup);
        cardViewSearch = view.findViewById(R.id.CardViewSearch);

        mainActivity.hideLinearLayoutTitle();

        cardViewFavorite.setOnClickListener(view1 -> openBottomSheet());

        cardViewBackup.setOnClickListener(view12 -> {
            if (user == null) {
                Toast.makeText(context, "You must sign in to use this function", Toast.LENGTH_SHORT).show();
            } else {
                openBackupImagesActivity();
            }
        });

        cardViewSearch.setOnClickListener(view13 -> openSearchOnlineActivity());
    }

    private void openBackupImagesActivity() {
        Intent intent = new Intent(context, BackupImagesActivity.class);
        startActivity(intent);
    }

    private void openSearchOnlineActivity() {
        Intent intent = new Intent(context, SearchOnlineActivity.class);
        startActivity(intent);
    }

    private void openBottomSheet() {
        // open bottom sheet
        View viewDialog = getLayoutInflater().inflate(R.layout.bottom_sheet_choosing, null);
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(context);
        bottomSheetDialog.setContentView(viewDialog);
        bottomSheetDialog.show();

        // implement listener of views in bottom sheet
        imgImage = viewDialog.findViewById(R.id.imageViewFavoriteImage);
        imgAlbum = viewDialog.findViewById(R.id.imageViewFavoriteAlbum);

        imgImage.setOnClickListener(view -> {
            Intent intent = new Intent(context, FavoriteImagesActivity.class);
            startActivity(intent);
        });

        imgAlbum.setOnClickListener(view -> {
            Intent intent = new Intent(context, FavoriteAlbumsActivity.class);
            startActivity(intent);
        });
    }
}
