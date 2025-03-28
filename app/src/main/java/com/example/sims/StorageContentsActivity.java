package com.example.sims;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

/*
    This activity shows the list of items stored in a specific storage location.
    It supports viewing, renaming, and deleting items from that location.
*/
public class StorageContentsActivity extends AppCompatActivity {

    private ArrayList<String> itemList;
    private ArrayAdapter<String> adapter;
    private String locationName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_storage_contents);

        TextView header = findViewById(R.id.locationHeader);
        ListView contentsList = findViewById(R.id.contentsList);

        locationName = getIntent().getStringExtra("locationName");
        locationName = toTitleCase(locationName);
        header.setText("Contents of: " + locationName);

        itemList = new ArrayList<>();
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, itemList);
        contentsList.setAdapter(adapter);

        loadItemsFromJson();

        // Handle long-clicks for rename/delete options
        contentsList.setOnItemLongClickListener((parent, view, position, id) -> {
            String selectedItem = itemList.get(position);

            new AlertDialog.Builder(this)
                    .setTitle("Modify Item")
                    .setItems(new CharSequence[]{"Rename", "Edit Quantity", "Delete"}, (dialog, which) -> {
                        switch (which) {
                            case 0:
                                showRenameDialog(position);
                                break;
                            case 1:
                                showQuantityDialog(position);
                                break;
                            case 2:
                                deleteItem(position);
                                break;
                        }
                    })
                    .show();
            return true;
        });
    }

    private void loadItemsFromJson() {
        JSONObject json = JsonStorageHelper.readJson(this);
        if (json != null && json.has(locationName)) {
            try {
                JSONArray items = json.getJSONArray(locationName);
                for (int i = 0; i < items.length(); i++) {
                    JSONObject item = items.getJSONObject(i);
                    String name = item.optString("name", "Unnamed");
                    String quantity = item.optString("quantity", "Unknown Size");
                    int stockQuantity = item.optInt("stockQuantity", 1);
                    itemList.add(name + " - Qty: " + stockQuantity + " (" + quantity + ")");
                }
                adapter.notifyDataSetChanged();
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    private void deleteItem(int position) {
        itemList.remove(position);
        adapter.notifyDataSetChanged();

        JSONObject json = JsonStorageHelper.readJson(this);
        if (json != null) {
            try {
                JSONArray items = json.getJSONArray(locationName);
                items.remove(position);
                json.put(locationName, items);
                JsonStorageHelper.writeJson(this, json);
                Toast.makeText(this, "Item deleted.", Toast.LENGTH_SHORT).show();
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    private void showRenameDialog(int position) {
        String oldDisplay = itemList.get(position);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Rename Item");

        final EditText input = new EditText(this);
        input.setText(oldDisplay.split(" - Qty: ")[0]);
        builder.setView(input);

        builder.setPositiveButton("Save", (dialog, which) -> {
            String newName = input.getText().toString().trim();
            if (!newName.isEmpty()) {
                JSONObject json = JsonStorageHelper.readJson(this);
                if (json != null) {
                    try {
                        JSONArray items = json.getJSONArray(locationName);
                        JSONObject item = items.getJSONObject(position);
                        item.put("name", newName);
                        JsonStorageHelper.writeJson(this, json);

                        String updatedDisplay = newName + " - Qty: " + item.optInt("stockQuantity", 1) + " (" + item.optString("quantity", "Unknown Size") + ")";
                        itemList.set(position, updatedDisplay);
                        adapter.notifyDataSetChanged();

                        Toast.makeText(this, "Renamed to: " + newName, Toast.LENGTH_SHORT).show();
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void showQuantityDialog(int position) {
        String oldDisplay = itemList.get(position);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Edit Quantity");

        final EditText input = new EditText(this);
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        builder.setView(input);

        builder.setPositiveButton("Save", (dialog, which) -> {
            String newQtyStr = input.getText().toString().trim();
            if (!newQtyStr.isEmpty()) {
                try {
                    int newQty = Integer.parseInt(newQtyStr);
                    JSONObject json = JsonStorageHelper.readJson(this);
                    if (json != null) {
                        JSONArray items = json.getJSONArray(locationName);
                        JSONObject item = items.getJSONObject(position);
                        item.put("stockQuantity", newQty);
                        JsonStorageHelper.writeJson(this, json);

                        String updatedDisplay = item.optString("name", "Unnamed") + " - Qty: " + newQty + " (" + item.optString("quantity", "Unknown Size") + ")";
                        itemList.set(position, updatedDisplay);
                        adapter.notifyDataSetChanged();

                        Toast.makeText(this, "Quantity updated to: " + newQty, Toast.LENGTH_SHORT).show();
                    }
                } catch (NumberFormatException | JSONException e) {
                    Toast.makeText(this, "Invalid number.", Toast.LENGTH_SHORT).show();
                }
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private String toTitleCase(String input) {
        if (input == null || input.isEmpty()) return input;
        String[] words = input.split(" ");
        StringBuilder sb = new StringBuilder();
        for (String word : words) {
            if (word.length() > 1) {
                sb.append(Character.toUpperCase(word.charAt(0)))
                        .append(word.substring(1).toLowerCase());
            } else {
                sb.append(word.toUpperCase());
            }
            sb.append(" ");
        }
        return sb.toString().trim();
    }
}
