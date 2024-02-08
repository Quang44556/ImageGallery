package com.example.imagesgallery.Model;

import java.io.Serializable;

public class Image implements Serializable {
    private String path;
    private String description;
    private int isFavored;
    private boolean canAddToCurrentAlbum;

    public Image(String path, String description, int isFavored) {
        this.path = path;
        this.description = description;
        this.isFavored = isFavored;
        canAddToCurrentAlbum = true;
    }

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
}
