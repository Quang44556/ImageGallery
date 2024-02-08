package com.example.imagesgallery.Activity;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager.widget.ViewPager;

import com.example.imagesgallery.Adapter.SlideshowPagerAdapter;
import com.example.imagesgallery.Model.Image;
import com.example.imagesgallery.R;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

public class SlideshowActivity extends AppCompatActivity {
    private ViewPager viewPager;
    private ArrayList<Image> selectedImages;
    private int currentPosition = 0;
    private Timer timer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_slideshow);

        // Get the selected image paths from the Intent
        selectedImages = (ArrayList<Image>) getIntent().getSerializableExtra("selectedImages");

        // Initialize ViewPager and set an adapter
        viewPager = findViewById(R.id.viewPager);
        SlideshowPagerAdapter adapter = new SlideshowPagerAdapter(this, selectedImages);
        viewPager.setAdapter(adapter);

        // Start a timer to switch between images automatically
        timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(() -> {
                    if (currentPosition < selectedImages.size() - 1) {
                        currentPosition++;
                        viewPager.setCurrentItem(currentPosition);
                    } else {
                        currentPosition = 0;
                        viewPager.setCurrentItem(currentPosition);
                    }
                });
            }
        }, 2000, 2000); // Change images every 5 seconds (adjust as needed)
    }

    // Ensure you cancel the timer when the activity is destroyed
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (timer != null) {
            timer.cancel();
        }
    }
}

