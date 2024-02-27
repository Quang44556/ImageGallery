package com.example.imagesgallery.Network;

import com.example.imagesgallery.Model.SearchResponse;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface SearchApiService {
    String BaseUrl = "https://serpapi.com/";
    Gson gson = new GsonBuilder().setLenient().create();

    OkHttpClient builder = new OkHttpClient.Builder()
            .readTimeout(10000, TimeUnit.MILLISECONDS)
            .connectTimeout(10000, TimeUnit.MILLISECONDS)
            .retryOnConnectionFailure(true)
            .build();

    SearchApiService apiService = new Retrofit.Builder()
            .baseUrl(BaseUrl)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .client(builder)
            .build()
            .create(SearchApiService.class);

    @GET("search.json")
    Call<SearchResponse> searchImages(@Query("q") String q, @Query("engine") String engine, @Query("ijn") String ijn, @Query("api_key") String api_key);
}
