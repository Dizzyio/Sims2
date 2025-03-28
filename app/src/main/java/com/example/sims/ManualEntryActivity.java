// ManualEntryActivity.java

/*
    This activity allows users to manually enter an item that does not have a barcode (e.g., homemade bread, farm meat).
    The user provides:
    - Product name
    - Quantity owned
    - Selects a storage location from the existing list

    Upon submission, the item is saved to the appropriate location in inventory_data.json.
 */

package com.example.sims;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class ManualEntryActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manual_entry);

        EditText nameInput = findViewById(R.id.manualProductName);
        EditText quantityInput = findViewById(R.id.manualQuantity);
        Spinner locationSpinner = findViewById(R.id.manualLocationSpinner);
        Button saveButton = findViewById(R.id.saveManualItemButton);

        // Load locations from JSON
        JSONObject json = JsonStorageHelper.readJson(this);
        ArrayList<String> locationNames = new ArrayList<>();

        if (json != null) {
            JSONArray names = json.names();
            if (names != null) {
                for (int i = 0; i < names.length(); i++) {
                    try {
                        locationNames.add(names.getString(i));
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        // Bind locations to dropdown
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, locationNames);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        locationSpinner.setAdapter(adapter);

        // Save Button Click
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String name = nameInput.getText().toString().trim();
                String quantity = quantityInput.getText().toString().trim();
                String location = locationSpinner.getSelectedItem().toString();

                if (name.isEmpty() || quantity.isEmpty()) {
                    Toast.makeText(ManualEntryActivity.this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Save using the helper method
                JsonStorageHelper.addItemToStorage(ManualEntryActivity.this, location, name, quantity, null);

                Toast.makeText(ManualEntryActivity.this, "Item added to " + location, Toast.LENGTH_SHORT).show();
                finish(); // Return to previous screen
            }
        });
    }
}
