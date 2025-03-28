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

/*
    This activity allows users to create, view, rename, and delete storage locations.
    Changes are persisted to a local JSON file. Each location can later be browsed to view its contents.
*/
public class StorageActivity extends AppCompatActivity {

    private ArrayList<String> storageList = new ArrayList<>();
    private ArrayAdapter<String> adapter;
    private JSONObject jsonObject; // This holds the full JSON file for update/delete/rename

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_storage);

        EditText storageNameInput = findViewById(R.id.storageNameInput);
        Button addStorageButton = findViewById(R.id.addStorageButton);
        ListView storageListView = findViewById(R.id.storageListView);

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, storageList);
        storageListView.setAdapter(adapter);

        // Load existing locations from JSON
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

        // Add new location
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
                        Toast.makeText(this, "Location added: " + locationName, Toast.LENGTH_SHORT).show();
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                } else {
                    Toast.makeText(this, "Location already exists", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "Please enter a location name", Toast.LENGTH_SHORT).show();
            }
        });

        // Handle item clicks for edit/delete/view
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
                                Toast.makeText(this, "Deleted: " + selectedLocation, Toast.LENGTH_SHORT).show();
                                break;
                        }
                    })
                    .show();
        });
    }

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
                    // Move the array to new key
                    JSONArray contents = jsonObject.optJSONArray(oldName);
                    jsonObject.remove(oldName);
                    jsonObject.put(newName, contents != null ? contents : new JSONArray());
                    JsonStorageHelper.writeJson(this, jsonObject);

                    storageList.set(position, newName);
                    adapter.notifyDataSetChanged();
                    Toast.makeText(this, "Renamed to: " + newName, Toast.LENGTH_SHORT).show();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }
}
