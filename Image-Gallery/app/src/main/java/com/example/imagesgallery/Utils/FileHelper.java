package com.example.imagesgallery.Utils;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.Objects;

public class FileHelper {
    public static void copyFileToExternalStorage(Context context, File srcFile) {
        ContentResolver contentResolver = context.getContentResolver();

        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.DISPLAY_NAME, srcFile.getName());
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");

        Uri uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

        if (uri == null) {
            Log.e("aaaa", "Cannot move " + srcFile.getPath() + " to external storage");
        } else {
            OutputStream outputStream;
            try {
                outputStream = contentResolver.openOutputStream(uri);
            } catch (FileNotFoundException e) {
                Log.e("aaaa", Objects.requireNonNull(e.getMessage()));
                throw new RuntimeException(e);
            }

            if (outputStream == null) {
                Log.e("aaaa", "Cannot move " + srcFile.getPath() + " to external storage");
            } else {
                try (InputStream inputStream = Files.newInputStream(srcFile.toPath())) {
                    byte[] buf = new byte[8192];
                    int bytesRead;
                    while ((bytesRead = inputStream.read(buf)) != -1) {
                        outputStream.write(buf, 0, bytesRead);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    try {
                        outputStream.close();
                    } catch (IOException e) {
                        Log.e("aaaa", Objects.requireNonNull(e.getMessage()));
                    }
                }
            }
        }

        srcFile.delete();
    }
}
