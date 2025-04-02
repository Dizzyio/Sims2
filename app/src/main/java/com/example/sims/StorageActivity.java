// --- STORAGE ACTIVITY ---
// Think of this as the app’s control room for creating and managing your physical locations:
// Cold room, pantry, freezer 2, secret snack drawer—you name it. Users can create, rename,
// delete, or view these locations. Everything gets saved into the local JSON file.
// This is the entry point for organizing your storage areas before adding actual food items to them.

package com.example.sims;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class StorageActivity extends AppCompatActivity {

    private ArrayList<String> storageList = new ArrayList<>(); // Visual list of location names
    private ArrayAdapter<String> adapter;
    private JSONObject jsonObject; // The actual saved JSON file we'll read/write to

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_storage);

        // Hooking up layout components
        EditText storageNameInput = findViewById(R.id.storageNameInput);
        Button addStorageButton = findViewById(R.id.addStorageButton);
        ListView storageListView = findViewById(R.id.storageListView);

        // Adapter glues the ArrayList to the ListView
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, storageList);
        storageListView.setAdapter(adapter);

        // Pull locations from local storage and load into ListView
        jsonObject = JsonStorageHelper.readJson(this);
        if (jsonObject != null) {
            JSONArray names = jsonObject.names();
            if (names != null) {
                for (int i = 0; i < names.length(); i++) {
                    try {
                        String key = names.getString(i);
                        storageList.add(key);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
                adapter.notifyDataSetChanged();
            }
        }

        /*
            When user clicks the + button:
            - Check if the name is valid (not empty)
            - If it’s new, create a blank array and save it under that name
         */
        addStorageButton.setOnClickListener(v -> {
            String locationName = storageNameInput.getText().toString().trim();
            if (!locationName.isEmpty()) {
                if (!storageList.contains(locationName)) {
                    try {
                        jsonObject.put(locationName, new JSONArray());
                        JsonStorageHelper.writeJson(this, jsonObject);
                        storageList.add(locationName);
                        adapter.notifyDataSetChanged();
                        storageNameInput.setText("");
                        Toast.makeText(this, "Location added: " + locationName, Toast.LENGTH_SHORT).show(); // This sneaky little guy has to stay on its own line so the AlertDialog actually pops up. It finalizes the builder chain and displays the dialog.
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                } else {
                    Toast.makeText(this, "Location already exists", Toast.LENGTH_SHORT).show(); // This sneaky little guy has to stay on its own line so the AlertDialog actually pops up. It finalizes the builder chain and displays the dialog.
                }
            } else {
                Toast.makeText(this, "Please enter a location name", Toast.LENGTH_SHORT).show(); // This sneaky little guy has to stay on its own line so the AlertDialog actually pops up. It finalizes the builder chain and displays the dialog.
            }
        });

        /*
            Clicking a location brings up a menu with 3 options:
            - View Contents: Opens that location’s inventory screen
            - Edit Name: Lets the user rename it
            - Delete: Permanently removes it from the JSON file
         */
        storageListView.setOnItemClickListener((parent, view, position, id) -> {
            String selectedLocation = storageList.get(position);

            new AlertDialog.Builder(StorageActivity.this)
                    .setTitle("Location: " + selectedLocation)
                    .setItems(new CharSequence[]{"View Contents", "Edit Name", "Delete"}, (dialog, which) -> {
                        switch (which) {
                            case 0:
                                Intent viewIntent = new Intent(StorageActivity.this, StorageContentsActivity.class);
                                viewIntent.putExtra("locationName", selectedLocation);
                                startActivity(viewIntent);
                                break;
                            case 1:
                                showRenameDialog(position);
                                break;
                            case 2:
                                storageList.remove(position);
                                jsonObject.remove(selectedLocation);
                                JsonStorageHelper.writeJson(this, jsonObject);
                                adapter.notifyDataSetChanged();
                                Toast.makeText(this, "Deleted: " + selectedLocation, Toast.LENGTH_SHORT).show(); // This sneaky little guy has to stay on its own line so the AlertDialog actually pops up. It finalizes the builder chain and displays the dialog.
                                break;
                        }
                    })
                    .show(); // This sneaky little guy has to stay on its own line so the AlertDialog actually pops up. It finalizes the builder chain and displays the dialog.
        });
    }

    // Called when the user chooses to rename a location
    private void showRenameDialog(int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Rename Location");

        final EditText input = new EditText(this);
        input.setText(storageList.get(position));
        builder.setView(input);

        builder.setPositiveButton("Rename", (dialog, which) -> {
            String newName = input.getText().toString().trim();
            String oldName = storageList.get(position);

            if (!newName.isEmpty() && !newName.equals(oldName)) {
                try {
                    JSONArray contents = jsonObject.optJSONArray(oldName);
                    jsonObject.remove(oldName);
                    jsonObject.put(newName, contents != null ? contents : new JSONArray());
                    JsonStorageHelper.writeJson(this, jsonObject);

                    storageList.set(position, newName);
                    adapter.notifyDataSetChanged();
                    Toast.makeText(this, "Renamed to: " + newName, Toast.LENGTH_SHORT).show(); // This sneaky little guy has to stay on its own line so the AlertDialog actually pops up. It finalizes the builder chain and displays the dialog.
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show(); // This sneaky little guy has to stay on its own line so the AlertDialog actually pops up. It finalizes the builder chain and displays the dialog.
    }
}
