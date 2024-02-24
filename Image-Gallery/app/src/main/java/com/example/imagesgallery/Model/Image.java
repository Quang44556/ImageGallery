package com.example.imagesgallery.Model;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

public class Image implements Parcelable {
    private String path;
    private String description;
    private int isFavored;
    private boolean canAddToCurrentAlbum;

    public Image(String path) {
        this.path = path;
        canAddToCurrentAlbum = true;
    }

    public Image(String path, String description, int isFavored) {
        this.path = path;
        this.description = description;
        this.isFavored = isFavored;
        canAddToCurrentAlbum = true;
    }

    protected Image(Parcel in) {
        path = in.readString();
        description = in.readString();
        isFavored = in.readInt();
        canAddToCurrentAlbum = in.readByte() != 0;
    }

    public static final Creator<Image> CREATOR = new Creator<Image>() {
        @Override
        public Image createFromParcel(Parcel in) {
            return new Image(in);
        }

        @Override
        public Image[] newArray(int size) {
            return new Image[size];
        }
    };

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getIsFavored() {
        return isFavored;
    }

    public void setIsFavored(int isFavored) {
        this.isFavored = isFavored;
    }

    public boolean isCanAddToCurrentAlbum() {
        return canAddToCurrentAlbum;
    }

    public void setCanAddToCurrentAlbum(boolean canAddToCurrentAlbum) {
        this.canAddToCurrentAlbum = canAddToCurrentAlbum;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel parcel, int i) {
        parcel.writeString(path);
        parcel.writeString(description);
        parcel.writeInt(isFavored);
        parcel.writeByte((byte) (canAddToCurrentAlbum ? 1 : 0));
    }
}
