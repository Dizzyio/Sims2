package com.example.sims;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;

import java.util.ArrayList;

/*
    This activity displays the scanned or manually entered product's details:
    - Product name
    - Quantity
    - Image (if available from the API)
    - Allows user to choose a storage location and save the item there

    It receives data from MainActivity using Intent extras.
*/
public class ResultActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);

        TextView productNameView = findViewById(R.id.productName);
        TextView quantityView = findViewById(R.id.productQuantity);
        ImageView productImageView = findViewById(R.id.productImage);
        Spinner locationSpinner = findViewById(R.id.storageSpinner);
        Button addToStorageButton = findViewById(R.id.addToStorageButton);

        // Get data from the Intent
        String productName = getIntent().getStringExtra("productName");
        String quantity = getIntent().getStringExtra("quantity");
        String imageUrl = getIntent().getStringExtra("imageUrl");
        String barcode = getIntent().getStringExtra("barcode");


        // Prompt for rename if product name is unknown
        if ("Unknown Product".equals(productName)) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Product not recognized");

            final EditText input = new EditText(this);
            input.setHint("Enter custom name");
            builder.setView(input);

            builder.setPositiveButton("Save", (dialog, which) -> {
                String newName = input.getText().toString().trim();
                if (!newName.isEmpty()) {
                    productNameView.setText(newName);
                } else {
                    productNameView.setText("Unnamed Item");
                }
            });

            builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

            builder.show();
        } else {
            productNameView.setText(productName);
        }

        quantityView.setText(quantity);
        if (imageUrl != null && !imageUrl.isEmpty()) {
            Glide.with(this).load(imageUrl).into(productImageView);
        }

        // TEMPORARY: Hardcoded locations until real loading from file is added
        ArrayList<String> storageLocations = new ArrayList<>();
        storageLocations.add("Fridge");
        storageLocations.add("Pantry");
        storageLocations.add("Cold Room");

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, storageLocations);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        locationSpinner.setAdapter(adapter);

        // Button to add this item to storage
        addToStorageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String selectedLocation = locationSpinner.getSelectedItem().toString();
                String finalProductName = productNameView.getText().toString();

                JsonStorageHelper.addItemToStorage(ResultActivity.this, selectedLocation, finalProductName, quantity, barcode);

                Toast.makeText(ResultActivity.this, "Item saved to " + selectedLocation, Toast.LENGTH_SHORT).show();
                finish(); // Optionally return to MainActivity after saving
            }
        });
    }
}
