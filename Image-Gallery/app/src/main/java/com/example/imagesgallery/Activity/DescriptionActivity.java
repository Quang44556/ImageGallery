package com.example.imagesgallery.Activity;

import static com.example.imagesgallery.Database.SqliteDatabase.update;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.imagesgallery.Model.Album;
import com.example.imagesgallery.Model.Image;
import com.example.imagesgallery.R;

import java.util.Objects;

public class DescriptionActivity extends AppCompatActivity {

    Toolbar toolbar;
    EditText edtDescription;
    Album album;
    Image image;
    long rowID = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_description);

        init();

        // set description (album)
        album = (Album) getIntent().getSerializableExtra("album");
        if (album != null) {
            edtDescription.setText(album.getDescription());
        }

        // set description (image)
        image = (Image) getIntent().getSerializableExtra("image");
        if (image != null) {
            edtDescription.setText(image.getDescription());
        }

        edtDescription.setFocusableInTouchMode(false);
        edtDescription.setFocusable(false);

        // using toolbar as ActionBar
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Description");

        // set return button
        toolbar.setNavigationOnClickListener(v -> finishActivityOnBackPress());

        // set back press in device
        getOnBackPressedDispatcher().addCallback(new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                finishActivityOnBackPress();
            }
        });
    }

    private void init() {
        toolbar = findViewById(R.id.toolbar);
        edtDescription = findViewById(R.id.edtDescriptionAlbum);
    }

    private void finishActivityOnBackPress() {
        if (rowID > 0) {
            Intent resultIntent = new Intent();
            resultIntent.putExtra("description", edtDescription.getText().toString());
            setResult(Activity.RESULT_OK, resultIntent);
        }
        finish();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_description, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemID = item.getItemId();
        if (itemID == R.id.changeDescription) {
            if (edtDescription.isFocusableInTouchMode()) { // click done
                String description_changed = edtDescription.getText().toString();

                if (album != null) {
                    Album temp = new Album(
                            album.getCover(),
                            album.getName(),
                            description_changed,
                            album.getIsFavored(),
                            album.getId()
                    );
                    rowID = update(temp);
                } else if (image != null) {
                    Image temp = new Image(image.getPath(), description_changed, image.getIsFavored());
                    rowID = update(temp);
                }

                if (rowID > 0) {
                    edtDescription.setText(description_changed);
                } else {
                    Toast.makeText(this, "Unable to update", Toast.LENGTH_SHORT).show();
                }

                edtDescription.setFocusable(false);
                item.setIcon(R.drawable.edit);
            } else { // click edit
                item.setIcon(R.drawable.done);
                edtDescription.setFocusableInTouchMode(true);
            }
        }
        return super.onOptionsItemSelected(item);
    }
}