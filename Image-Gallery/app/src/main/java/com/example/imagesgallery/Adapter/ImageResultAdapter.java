package com.example.imagesgallery.Adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.imagesgallery.Listener.ClickListener;
import com.example.imagesgallery.Model.ImagesResult;
import com.example.imagesgallery.R;

import java.util.List;

public class ImageResultAdapter extends RecyclerView.Adapter<ImageResultAdapter.ViewHolder> {

    Context context;
    List<ImagesResult> imagesResultList;
    ClickListener listener;

    public ImageResultAdapter(Context context, List<ImagesResult> imagesResultList, ClickListener listener) {
        this.context = context;
        this.imagesResultList = imagesResultList;
        this.listener = listener;
    }

    public Context getContext() {
        return context;
    }

    public List<ImagesResult> getImagesResultList() {
        return imagesResultList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_image, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ImagesResult image = imagesResultList.get(holder.getAdapterPosition());
        Glide.with(context)
                .load(image.getThumbnail())
                .placeholder(R.drawable.loading)
                .error(R.drawable.no_image)
                .into(holder.imageView);

        holder.checkBox.setVisibility(View.GONE);

        holder.imageView.setOnClickListener(view -> {
            // Get the position of the image
            listener.click(holder.getAdapterPosition());
        });
    }

    @Override
    public int getItemCount() {
        return imagesResultList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        ImageView imageView;
        CheckBox checkBox;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            imageView = itemView.findViewById(R.id.gallery_item);
            checkBox = itemView.findViewById(R.id.checkBox);
        }
    }
}
