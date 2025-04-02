// --- MANUAL ENTRY ACTIVITY ---
// This screen is where users go when they want to add something that doesn’t have a barcode.
// Picture this: your homemade jam, Aunt May’s meatballs, or a basket of farm eggs—none of them have barcodes,
// but you still want to track them. That’s where this form comes in.
// Users fill in a name, how many they have, pick where it's stored, and boom—added to the inventory.

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

        // Grabbing UI elements from the layout
        EditText nameInput = findViewById(R.id.manualProductName);
        EditText quantityInput = findViewById(R.id.manualQuantity);
        Spinner locationSpinner = findViewById(R.id.manualLocationSpinner);
        Button saveButton = findViewById(R.id.saveManualItemButton);

        // Pulling a list of existing storage locations from the inventory JSON
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

        // Populate the dropdown menu with location names
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, locationNames);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        locationSpinner.setAdapter(adapter);

        /*
            When the user clicks the "Save" button:
            - Grab the input fields
            - Validate that they're filled out
            - Add the item to the chosen location using our trusty JSON helper
            - Show a confirmation toast and go back to the previous screen
        */
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

                // null barcode tells the system this was a manual entry
                JsonStorageHelper.addItemToStorage(ManualEntryActivity.this, location, name, quantity, null);

                Toast.makeText(ManualEntryActivity.this, "Item added to " + location, Toast.LENGTH_SHORT).show();
                finish(); // Done here, back to where we came from
            }
        });
    }
}
