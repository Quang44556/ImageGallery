package com.example.imagesgallery.Model;

import java.io.Serializable;
import java.util.ArrayList;

public class Album implements Serializable {
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
}
