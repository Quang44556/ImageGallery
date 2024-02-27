package com.example.imagesgallery.Model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class SearchResponse {
    @SerializedName("images_results")
    List<ImagesResult> resultList;

    public List<ImagesResult> getResultList() {
        return resultList;
    }

}
