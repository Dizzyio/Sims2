package com.example.sims;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

/*
    This activity allows users to name and add storage locations.
    It currently just confirms input via Toast. List saving will come next.
*/
public class StorageActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_storage);

        EditText storageNameInput = findViewById(R.id.storageNameInput);
        Button addStorageButton = findViewById(R.id.addStorageButton);
        Button backToMainButton = findViewById(R.id.backToMainButton);

        // When user clicks "Add Location", check if the input is not empty
        addStorageButton.setOnClickListener(v -> {
            String locationName = storageNameInput.getText().toString().trim();

            if (!locationName.isEmpty()) {
                // Later, this will save the location to JSON
                Toast.makeText(StorageActivity.this, "Location added: " + locationName, Toast.LENGTH_SHORT).show();
                storageNameInput.setText(""); // Clear the input
            } else {
                Toast.makeText(StorageActivity.this, "Please enter a location name", Toast.LENGTH_SHORT).show();
            }
        });

        // Takes user back to main screen
        backToMainButton.setOnClickListener(v -> {
            Intent intent = new Intent(StorageActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        });


    }
}
