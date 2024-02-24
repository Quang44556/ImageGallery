package com.example.imagesgallery.Model;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import java.util.ArrayList;

public class Album implements Parcelable {
    private Image cover;
    private String name;
    private String description;
    private int isFavored;
    private int id;
    private ArrayList<Image> listImage;

    public Album(Image cover, String name, String description, int isFavored, int id) {
        this.cover = cover;
        this.name = name;
        this.description = description;
        this.isFavored = isFavored;
        this.id = id;
        listImage = new ArrayList<>();
    }

    protected Album(Parcel in) {
        cover = in.readParcelable(Image.class.getClassLoader());
        name = in.readString();
        description = in.readString();
        isFavored = in.readInt();
        id = in.readInt();
        listImage = in.createTypedArrayList(Image.CREATOR);
    }

    public static final Creator<Album> CREATOR = new Creator<Album>() {
        @Override
        public Album createFromParcel(Parcel in) {
            return new Album(in);
        }

        @Override
        public Album[] newArray(int size) {
            return new Album[size];
        }
    };

    public ArrayList<Image> getListImage() {
        return listImage;
    }

    public void setListImage(ArrayList<Image> listImage) {
        this.listImage = listImage;
    }

    public Image getCover() {
        return cover;
    }

    public void setCover(Image cover) {
        this.cover = cover;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel parcel, int i) {
        parcel.writeParcelable(cover, i);
        parcel.writeString(name);
        parcel.writeString(description);
        parcel.writeInt(isFavored);
        parcel.writeInt(id);
        parcel.writeTypedList(listImage);
    }
}
