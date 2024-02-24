package com.example.imagesgallery.Adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.example.imagesgallery.Model.Image;
import com.example.imagesgallery.R;
import com.github.chrisbanes.photoview.PhotoView;

import java.util.ArrayList;

public class ImageViewPager2Adapter extends RecyclerView.Adapter<ImageViewPager2Adapter.ImageViewHolder> {
    private final ArrayList<Image> imageArrayList;
    private final Context context;
    private final ViewPager2 viewPager2;

    public ImageViewPager2Adapter(ArrayList<Image> imageArrayList, Context context, ViewPager2 viewPager2) {
        this.imageArrayList = imageArrayList;
        this.context = context;
        this.viewPager2 = viewPager2;
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
                .into(holder.photoView);

        // disable swiping to next image if user is zooming current image
        holder.photoView.setOnMatrixChangeListener(rect -> {
            float scale = holder.photoView.getScale();
            viewPager2.setUserInputEnabled(scale == 1.0f);
        });
    }

    @Override
    public int getItemCount() {
        return imageArrayList.size();
    }

    public static class ImageViewHolder extends RecyclerView.ViewHolder {
        private final PhotoView photoView;

        public ImageViewHolder(@NonNull View itemView) {
            super(itemView);
            photoView = itemView.findViewById(R.id.imageFullScreen);
        }
    }
}

