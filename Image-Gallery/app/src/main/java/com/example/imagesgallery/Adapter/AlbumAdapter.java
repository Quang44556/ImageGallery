package com.example.imagesgallery.Adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.example.imagesgallery.Activity.AddFavoriteAlbumActivity;
import com.example.imagesgallery.Activity.MainActivity;
import com.example.imagesgallery.Listener.ClickListener;
import com.example.imagesgallery.Model.Album;
import com.example.imagesgallery.R;

import java.util.ArrayList;

public class AlbumAdapter extends BaseAdapter {
    Context context;
    ArrayList<Album> albumArrayList;
    private boolean isMultiSelectMode = false;
    private final ArrayList<Integer> selectedPositions;
    private final ArrayList<Album> selectedAlbums;
    ClickListener clickListener;

    public Context getContext() {
        return context;
    }

    public void setContext(Context context) {
        this.context = context;
    }

    public void setAlbumArrayList(ArrayList<Album> albumArrayList) {
        this.albumArrayList = albumArrayList;
    }

    public AlbumAdapter(Context context, ArrayList<Album> albumArrayList, ClickListener clickListener) {
        this.context = context;
        this.albumArrayList = albumArrayList;
        this.clickListener = clickListener;
        selectedAlbums = new ArrayList<>();
        selectedPositions = new ArrayList<>();
    }

    @Override
    public int getCount() {
        return albumArrayList.size();
    }

    @Override
    public Object getItem(int i) {
        return null;
    }

    @Override
    public long getItemId(int i) {
        return 0;
    }

    public void setMultiSelectMode(boolean multiSelectMode) {
        this.isMultiSelectMode = multiSelectMode;
    }

    public boolean isInMultiSelectMode() {
        return isMultiSelectMode;
    }

    public ArrayList<Album> getSelectedAlbums() {
        return selectedAlbums;
    }

    public void toggleSelection(int position) {
        Album album = albumArrayList.get(position);
        if (selectedAlbums.contains(album)) {
            selectedAlbums.remove(album);
        } else {
            selectedAlbums.add(album);
        }

        if (selectedPositions.contains(position)) {
            selectedPositions.remove(Integer.valueOf(position));
        } else {
            selectedPositions.add(position);
        }
        notifyDataSetChanged(); // Update the UI to reflect the selection
    }

    public ArrayList<Integer> getSelectedPositions() {
        return selectedPositions;
    }

    public void clearSelection() {
        selectedAlbums.clear();
        selectedPositions.clear();
        notifyDataSetChanged(); // Update the UI to clear selection
    }

    private static class ViewHolder {
        TextView AlbumName;
        ImageView AlbumCover;
        CheckBox checkBox;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        ViewHolder viewHolder;
        if (view == null) {
            viewHolder = new ViewHolder();
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = inflater.inflate(R.layout.item_album, null);
            viewHolder.AlbumName = view.findViewById(R.id.nameAlbum);
            viewHolder.AlbumCover =  view.findViewById(R.id.imageCoverAlbum);
            viewHolder.checkBox = view.findViewById(R.id.checkboxAlbum);
            view.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) view.getTag();
        }

        Album album = albumArrayList.get(i);
        String coverPath = album.getCover().getPath();
        if (coverPath.equals(MainActivity.pathNoImage)) {
            viewHolder.AlbumCover.setImageResource(R.drawable.no_image);
        } else {
            Glide.with(context)
                    .load(coverPath)
                    .placeholder(R.drawable.loading)
                    .error(R.drawable.no_image)
                    .into(viewHolder.AlbumCover);
        }
        viewHolder.AlbumName.setText(album.getName());

        // use these codes to avoid item in DefaultArray have same UI of item in SearchArray due to viewHolder
        viewHolder.AlbumCover.setEnabled(true);
        viewHolder.AlbumCover.setAlpha(1f);

        // check album the user choose in multi selection mode
        if (selectedPositions.contains(i)) {
            viewHolder.checkBox.setVisibility(View.VISIBLE);
            viewHolder.checkBox.setChecked(true);
        } else {
            viewHolder.checkBox.setVisibility(View.GONE);
            viewHolder.checkBox.setChecked(false);
        }

        // mark albums that have already been in favorites
        if (context instanceof AddFavoriteAlbumActivity && album.getIsFavored() == 1) {
            viewHolder.checkBox.setVisibility(View.VISIBLE);
            viewHolder.checkBox.setChecked(true);
            viewHolder.AlbumCover.setEnabled(false);
            viewHolder.AlbumCover.setAlpha(0.5f);
        }

        viewHolder.AlbumCover.setOnClickListener(view1 -> clickListener.click(i));

        viewHolder.AlbumCover.setOnLongClickListener(view12 -> {
            clickListener.longClick(i);
            return true;
        });

        return view;
    }
}
