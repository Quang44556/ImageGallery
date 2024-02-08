package com.example.imagesgallery.Database;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;

import com.example.imagesgallery.Activity.MainActivity;
import com.example.imagesgallery.Model.Album;
import com.example.imagesgallery.Model.Image;

import java.io.File;

public class SqliteDatabase {
    public static SQLiteDatabase db;
    static String DatabaseName = "ImageGallery";

    public static void createDatabase(File storagePath) {
        String myDbPath = storagePath + "/" + DatabaseName;
        try {
            db = SQLiteDatabase.openDatabase(myDbPath, null, SQLiteDatabase.CREATE_IF_NECESSARY);
            db.execSQL("CREATE TABLE IF NOT EXISTS Album(id_album INTEGER PRIMARY KEY AUTOINCREMENT, description TEXT, cover INTEGER, name TEXT, isFavored INTEGER)");
            db.execSQL("CREATE TABLE IF NOT EXISTS Image(path TEXT PRIMARY KEY,  description TEXT, isFavored INTEGER)");
            db.execSQL("CREATE TABLE IF NOT EXISTS Album_Contain_Images(id INTEGER PRIMARY KEY AUTOINCREMENT, id_album INTEGER, path TEXT)");
        } catch (
                SQLiteException ignored) {
        }
    }

    public static long insert(Image image) {
        ContentValues values = new ContentValues();
        values.put("path", image.getPath());
        values.put("description", image.getDescription());
        values.put("isFavored", image.getIsFavored());
        return db.insert("Image", null, values);
    }

    public static long update(Image image) {
        ContentValues contentValues = new ContentValues();
        contentValues.put("isFavored", image.getIsFavored());
        contentValues.put("description", image.getDescription());
        String[] args = {String.valueOf(image.getPath())};
        return db.update("Image", contentValues, "path = ?", args);
    }

    public static void delete(Image image) {
        String[] args = {image.getPath()};
        db.delete("Image", "path = ?", args);
        db.delete("Album_Contain_Images", "path = ?", args);
    }

    public static long insert(Album album) {
        ContentValues rowValues = new ContentValues();
        rowValues.put("isFavored", album.getIsFavored());
        rowValues.put("name", album.getName());
        rowValues.put("cover", album.getCover().getPath());
        rowValues.put("description", album.getDescription());
        return db.insert("Album", null, rowValues);
    }

    public static long update(Album album) {
        ContentValues contentValues = new ContentValues();
        contentValues.put("name", album.getName());
        contentValues.put("isFavored", album.getIsFavored());
        contentValues.put("description", album.getDescription());
        contentValues.put("cover", album.getCover().getPath());
        String[] args = {String.valueOf(album.getId())};
        return db.update("Album", contentValues, "id_album = ?", args);
    }

    public static void delete(Album album) {
        String[] args = {String.valueOf(album.getId())};
        db.delete("Album", "id_album = ?", args);
        db.delete("Album_Contain_Images", "id_album = ?", args);
    }

    public static long addImageToAlbum(Album album, String imgPath) {
        ContentValues values = new ContentValues();
        values.put("id_album", album.getId());
        values.put("path", imgPath);
        return db.insert("Album_Contain_Images", null, values);
    }

    public static long removeImageFromAlbum(String imgPath, Album album) {
        String[] args = {imgPath, String.valueOf(album.getId())};
        return db.delete("Album_Contain_Images", "path = ? AND id_album = ?", args);
    }
}
