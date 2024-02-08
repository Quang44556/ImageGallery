package com.example.imagesgallery.Adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.viewpager.widget.PagerAdapter;

import com.bumptech.glide.Glide;
import com.example.imagesgallery.Model.Image;
import com.example.imagesgallery.R;

import java.util.ArrayList;

public class SlideshowPagerAdapter extends PagerAdapter {
    private final Context context;
    private final ArrayList<Image> image;

    public SlideshowPagerAdapter(Context context, ArrayList<Image> image) {
        this.context = context;
        this.image = image;
    }

    @Override
    public int getCount() {
        return image.size();
    }

    @Override
    public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
        return view == object;
    }

    @NonNull
    @Override
    public Object instantiateItem(@NonNull ViewGroup container, int position) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_slideshow, container, false);
        ImageView imageView = view.findViewById(R.id.imageView);

        // Load and display the image using Glide or another image-loading library
        Glide.with(context)
                .load(image.get(position).getPath())
                .error(R.drawable.no_image)
                .into(imageView);

        container.addView(view);
        return view;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, @NonNull Object object) {
        container.removeView((View) object);
    }
}

