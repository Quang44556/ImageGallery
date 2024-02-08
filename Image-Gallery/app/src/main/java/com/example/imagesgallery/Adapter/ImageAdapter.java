package com.example.imagesgallery.Adapter;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.imagesgallery.Interface.ClickListener;
import com.example.imagesgallery.Model.Image;
import com.example.imagesgallery.R;

import java.io.File;
import java.util.ArrayList;

public class ImageAdapter extends RecyclerView.Adapter<ImageAdapter.ViewHolder> {
    private final Context context;
    private final ArrayList<Image> imageArrayList;
    private boolean isMultiSelectMode = false;
    private final ArrayList<Integer> selectedPositions;
    private final ArrayList<Image> selectedImages;

    public ArrayList<Integer> getSelectedPositions() {
        return selectedPositions;
    }

    public ArrayList<Image> getImageArrayList() {
        return imageArrayList;
    }

    ClickListener listener;

    public void setMultiSelectMode(boolean multiSelectMode) {
        this.isMultiSelectMode = multiSelectMode;
    }

    public boolean isInMultiSelectMode() {
        return isMultiSelectMode;
    }

    public ArrayList<Image> getSelectedImages() {
        return selectedImages;
    }

    public ImageAdapter(Context context, ArrayList<Image> images_list, ClickListener listener) {
        this.context = context;
        this.imageArrayList = images_list;
        this.listener = listener;
        this.selectedImages = new ArrayList<>();
        this.selectedPositions = new ArrayList<>();
    }

    public void toggleSelection(int position) {
        Image image = imageArrayList.get(position);
        if (selectedImages.contains(image)) {
            selectedImages.remove(image);
        } else {
            selectedImages.add(image);
        }

        if (selectedPositions.contains(position)) {
            selectedPositions.remove(Integer.valueOf(position));
        } else {
            selectedPositions.add(position);
        }
        notifyItemChanged(position);
    }

    public void clearSelection() {
        selectedImages.clear();
        selectedPositions.clear();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_image, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        File image_file = new File(imageArrayList.get(holder.getAdapterPosition()).getPath());
        if (image_file.exists()) {
            Glide.with(context)
                    .load(image_file)
                    .placeholder(R.drawable.loading)
                    .error(R.drawable.no_image)
                    .into(holder.image);
        }

        // Check if the item is selected and update its appearance
        boolean isSelected = selectedImages.contains(imageArrayList.get(holder.getAdapterPosition()));
        holder.itemView.setSelected(isSelected);

        if (selectedPositions.contains(holder.getAdapterPosition())) {
            holder.checkBox.setVisibility(View.VISIBLE);
            holder.checkBox.setChecked(true);
        } else {
            holder.checkBox.setVisibility(View.GONE);
            holder.checkBox.setChecked(false);
        }

        // use these codes to avoid item in DefaultArray have same UI of item in SearchArray due to viewHolder
        holder.image.setEnabled(true);
        holder.image.setAlpha(1f);

        if (!imageArrayList.get(holder.getAdapterPosition()).isCanAddToCurrentAlbum()) {
            // disable image and change its appearance if it is in current album
            holder.image.setEnabled(false);
            holder.image.setAlpha(0.5f);
            holder.checkBox.setVisibility(View.VISIBLE);
            holder.checkBox.setChecked(true);
            holder.itemView.setEnabled(false);
        }

        holder.itemView.setOnClickListener(view -> {
            // Get the position of the image
            listener.click(holder.getAdapterPosition());
        });

        holder.itemView.setOnLongClickListener(view -> {
            listener.longClick(holder.getAdapterPosition());
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return imageArrayList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        private final ImageView image;

        private final CheckBox checkBox;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            checkBox = itemView.findViewById(R.id.checkBox);
            image = itemView.findViewById(R.id.gallery_item);
        }
    }
}