package com.example.sims;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
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
    Now also allows clicking items to view detailed info (if barcode is available).
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
                    .setItems(new CharSequence[]{"Rename", "Delete"}, (dialog, which) -> {
                        switch (which) {
                            case 0:
                                showRenameDialog(position);
                                break;
                            case 1:
                                deleteItem(position);
                                break;
                        }
                    })
                    .show();
            return true;
        });

        // Handle normal click to view item details
        contentsList.setOnItemClickListener((parent, view, position, id) -> {
            JSONObject json = JsonStorageHelper.readJson(this);
            if (json != null) {
                try {
                    JSONArray items = json.getJSONArray(locationName);
                    JSONObject item = items.getJSONObject(position);

                    String barcode = item.optString("barcode", null);
                    if (barcode != null && !barcode.isEmpty()) {
                        Intent intent = new Intent(StorageContentsActivity.this, ItemDetailActivity.class);
                        intent.putExtra("barcode", barcode);
                        startActivity(intent);
                    } else {
                        Toast.makeText(this, "No barcode available for this item", Toast.LENGTH_SHORT).show();
                    }

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
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
                    int stockQty = item.optInt("stockQuantity", 1);

                    String display = name + " - Qty: " + stockQty + " (" + quantity + ")";
                    itemList.add(display);
                }
                adapter.notifyDataSetChanged();
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    private void deleteItem(int position) {
        String item = itemList.get(position);
        itemList.remove(position);
        adapter.notifyDataSetChanged();

        JSONObject json = JsonStorageHelper.readJson(this);
        if (json != null) {
            try {
                JSONArray items = json.getJSONArray(locationName);
                JSONArray updatedArray = new JSONArray();

                for (int i = 0, j = 0; i < items.length(); i++) {
                    JSONObject currentItem = items.getJSONObject(i);
                    String display = currentItem.optString("name", "Unnamed") + " - Qty: " + currentItem.optInt("stockQuantity", 1) + " (" + currentItem.optString("quantity", "Unknown Size") + ")";
                    if (j < itemList.size() && display.equals(itemList.get(j))) {
                        updatedArray.put(currentItem);
                        j++;
                    }
                }
                json.put(locationName, updatedArray);
                JsonStorageHelper.writeJson(this, json);
                Toast.makeText(this, "Deleted: " + item, Toast.LENGTH_SHORT).show();
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
