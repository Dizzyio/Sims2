// --- STORAGE CONTENTS ACTIVITY ---
// This class handles everything about viewing and interacting with a single storage location's items.
// Think of it like opening your pantry and seeing all your stuff. This is the screen that shows you what's inside.
// It supports showing items, viewing details (if they have a barcode), and editing: renaming, deleting, and changing quantity.
// It's tied into a JSON-based system, so it pulls and saves your data from a local file.

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

public class StorageContentsActivity extends AppCompatActivity {

    // This is the list of items we're showing the user, like the lines you see in the ListView.
    private ArrayList<String> itemList;

    // This is what tells Android how to turn our list into something it can draw on screen.
    private ArrayAdapter<String> adapter;

    // This holds the name of the storage location we're looking at, like "Fridge" or "Cold Room".
    private String locationName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_storage_contents);

        // Hook up UI pieces: the big title at the top and the actual list that shows the items
        TextView header = findViewById(R.id.locationHeader);
        ListView contentsList = findViewById(R.id.contentsList);

        // Pull the storage name passed from the last screen and pretty it up (makes "cold room" become "Cold Room")
        locationName = getIntent().getStringExtra("locationName");
        locationName = toTitleCase(locationName);
        header.setText("Contents of: " + locationName);

        // Make the list usable and connect it to the adapter so Android can actually show stuff
        itemList = new ArrayList<>();
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, itemList);
        contentsList.setAdapter(adapter);

        // Fill the list with real data from our file
        loadItemsFromJson();

        // NORMAL CLICK: If the user taps on an item, try to open a detail page using its barcode
        contentsList.setOnItemClickListener((parent, view, position, id) -> {
            JSONObject json = JsonStorageHelper.readJson(this);
            if (json != null) {
                try {
                    JSONArray items = json.getJSONArray(locationName);
                    JSONObject item = items.getJSONObject(position);

                    // Try to get the barcode. If we have one, open the detail screen. If not, warn the user.
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

        // LONG CLICK: Show popup with edit options (adjust count, rename, delete)
        contentsList.setOnItemLongClickListener((parent, view, position, id) -> {
            String display = itemList.get(position);
            String name = display.split(" - Qty: ")[0]; // We split the text like "Milk - Qty: 3" to isolate the name only

            new AlertDialog.Builder(this)
                    .setTitle("Edit Item")
                    .setItems(new CharSequence[]{"Edit Quantity", "Rename", "Delete"}, (dialog, which) -> {
                        switch (which) {
                            case 0:
                                showAdjustQuantityDialog(position);
                                break;
                            case 1:
                                showRenameDialog(position);
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

    // This function loads the list from the JSON file based on the current storage location
    // Think of this like grabbing all the stuff from your freezer and laying it out so you can look at it
    private void loadItemsFromJson() {
        itemList.clear();
        JSONObject json = JsonStorageHelper.readJson(this);
        if (json != null && json.has(locationName)) {
            try {
                JSONArray items = json.getJSONArray(locationName);
                for (int i = 0; i < items.length(); i++) { // Loop through each item one at a time
                    JSONObject item = items.getJSONObject(i);
                    String name = item.optString("name", "Unnamed");
                    String quantity = item.optString("quantity", "Unknown Size");
                    int stockQty = item.optInt("stockQuantity", 1);

                    // We show the user something like "Apples - Qty: 5 (Bag)"
                    String display = name + " - Qty: " + stockQty + " (" + quantity + ")";
                    itemList.add(display);
                }
                adapter.notifyDataSetChanged(); // Tell the screen to update with our new info
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    // Shows a dialog box asking the user to enter a new quantity (e.g., "how many do you have now?")
    // This method lets us *change* the number without deleting and re-adding the item
    private void showAdjustQuantityDialog(int position) {
        String display = itemList.get(position);
        String name = display.split(" - Qty: ")[0];

        // We use 'final' here because the variable is used inside the popup box.
        // 'final' means it can't be reassigned later. You can't change it once it's been set.
        // In this case, it's like locking the input box reference so the system can safely use it later.
        final EditText input = new EditText(this);
        input.setHint("Enter new quantity");

        new AlertDialog.Builder(this)
                .setTitle("Adjust Quantity")
                .setView(input)
                .setPositiveButton("OK", (dialog, which) -> {
                    String newQtyText = input.getText().toString().trim();
                    if (newQtyText.isEmpty()) return;
                    try {
                        int newQty = Integer.parseInt(newQtyText); // This turns the input from text to a number

                        JSONObject json = JsonStorageHelper.readJson(this);
                        JSONArray items = json.getJSONArray(locationName);
                        for (int i = 0; i < items.length(); i++) {
                            JSONObject item = items.getJSONObject(i);
                            if (item.getString("name").equalsIgnoreCase(name)) {
                                item.put("stockQuantity", newQty); // Save the new count back into the file
                                break;
                            }
                        }
                        JsonStorageHelper.writeJson(this, json);
                        loadItemsFromJson(); // Refresh the screen
                    } catch (JSONException | NumberFormatException e) {
                        Toast.makeText(this, "Failed to update quantity.", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // This lets us rename an item by typing a new name into a box
    // Pretty straightforward UI-wise but tricky because we also have to update the file
    private void showRenameDialog(int position) {
        String display = itemList.get(position);
        String oldName = display.split(" - Qty: ")[0];

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Rename Item");

        final EditText input = new EditText(this);
        input.setText(oldName);
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
                        loadItemsFromJson();
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

    // This one fully deletes the item from the JSON list and removes it from view
    // Classic remove-by-position strategy
    private void deleteItem(int position) {
        String item = itemList.get(position);
        itemList.remove(position);
        adapter.notifyDataSetChanged();

        JSONObject json = JsonStorageHelper.readJson(this);
        if (json != null) {
            try {
                JSONArray items = json.getJSONArray(locationName);
                JSONArray updatedArray = new JSONArray();

                // Rebuild the whole array but skip the one we want to delete
                for (int i = 0; i < items.length(); i++) {
                    if (i != position) {
                        updatedArray.put(items.getJSONObject(i));
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

    // Just prettifies names. Takes "cold room" and turns it into "Cold Room"
    // Does not try to handle weird edge cases. We’re not Grammarly.
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
