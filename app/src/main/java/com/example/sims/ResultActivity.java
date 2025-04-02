// --- RESULT ACTIVITY ---
// This is the final pit stop before a product gets officially logged in your inventory.
// It takes whatever came from the barcode scan or manual entry and shows it nicely:
// name, size, and a picture if we got one from the API.
// Users then pick a storage location and hit save—easy peasy.
// If the product name came in as "Unknown Product" (thanks, Joe Louis),
// the app politely asks the user to give it a proper name.

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

public class ResultActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);

        // Hooking up the UI components
        TextView productNameView = findViewById(R.id.productName);
        TextView quantityView = findViewById(R.id.productQuantity);
        ImageView productImageView = findViewById(R.id.productImage);
        Spinner locationSpinner = findViewById(R.id.storageSpinner);
        Button addToStorageButton = findViewById(R.id.addToStorageButton);

        // Grab data passed from MainActivity
        String productName = getIntent().getStringExtra("productName");
        String quantity = getIntent().getStringExtra("quantity");
        String imageUrl = getIntent().getStringExtra("imageUrl");
        String barcode = getIntent().getStringExtra("barcode");

        /*
            If OpenFoodFacts failed us and gave a blank product name,
            prompt the user to type in a name manually.
            This keeps the inventory clean and meaningful.
        */
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

        // Display quantity and image (if available)
        quantityView.setText(quantity);
        if (imageUrl != null && !imageUrl.isEmpty()) {
            Glide.with(this).load(imageUrl).into(productImageView);
        }

        /*
            TEMPORARY: Hardcoded storage locations for testing/demo purposes.
            In a polished version, these would load dynamically from the local JSON.
         */
        ArrayList<String> storageLocations = new ArrayList<>();
        storageLocations.add("Fridge");
        storageLocations.add("Pantry");
        storageLocations.add("Cold Room");

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, storageLocations);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        locationSpinner.setAdapter(adapter);

        /*
            When user clicks “Add to Storage”:
            - Grab their selected location
            - Grab the final name (in case they renamed it)
            - Save it to the appropriate storage area via JsonStorageHelper
         */
        addToStorageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String selectedLocation = locationSpinner.getSelectedItem().toString();
                String finalProductName = productNameView.getText().toString();

                JsonStorageHelper.addItemToStorage(ResultActivity.this, selectedLocation, finalProductName, quantity, barcode);

                Toast.makeText(ResultActivity.this, "Item saved to " + selectedLocation, Toast.LENGTH_SHORT).show();
                finish(); // Done and out
            }
        });
    }
}
