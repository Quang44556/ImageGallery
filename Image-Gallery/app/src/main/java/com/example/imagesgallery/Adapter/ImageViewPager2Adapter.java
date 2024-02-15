package com.example.imagesgallery.Adapter;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.imagesgallery.Activity.AlbumInfoActivity;
import com.example.imagesgallery.Model.Image;
import com.example.imagesgallery.R;

import java.util.ArrayList;

public class ImageViewPager2Adapter extends RecyclerView.Adapter<ImageViewPager2Adapter.ImageViewHolder> {
    private ArrayList<Image> imageArrayList;
    Context context;

    public ImageViewPager2Adapter(ArrayList<Image> imageArrayList, Context context) {
        this.imageArrayList = imageArrayList;
        this.context = context;
    }

    @NonNull
    @Override
    public ImageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_image_full_screen, parent, false);
        return new ImageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ImageViewHolder holder, int position) {
        Image image = imageArrayList.get(position);
        Glide.with(context)
                .load(image.getPath())
                .error(R.drawable.no_image)
                .into(holder.imageView);
    }

    @Override
    public int getItemCount() {
        return imageArrayList.size();
    }

    public static class ImageViewHolder extends RecyclerView.ViewHolder {

        private ImageView imageView;

        public ImageViewHolder(@NonNull View itemView) {
            super(itemView);

            imageView = itemView.findViewById(R.id.imageFullScreen);
        }
    }
}
