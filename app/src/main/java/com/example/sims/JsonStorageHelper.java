// --- JSON STORAGE HELPER ---
// Helpers in programming are like your sidekicks—they’re not the main hero on screen,
// but they quietly handle all the dirty work behind the scenes. This one in particular manages our inventory file.
// Think of this as the class that checks if the storage file exists, reads it, writes to it, and keeps things from exploding.
// If we didn’t have this guy doing the file wrangling, every read/write would be chaos and duplication hell.
//
// The format is JSON, and the file is local—no cloud, no server.

package com.example.sims;

import android.content.Context;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class JsonStorageHelper {

    // This is the name of the inventory file we read/write from internal storage
    private static final String FILE_NAME = "inventory_data.json";

    /*
        Checks if the inventory file exists in app storage.
        If not, it copies a fresh copy from the assets folder.
        Think of this like setting up the first save file when the app is run the first time.
     */
    public static void initializeIfMissing(Context context) {
        File file = new File(context.getFilesDir(), FILE_NAME);
        if (!file.exists()) {
            try {
                InputStream inputStream = context.getAssets().open(FILE_NAME);
                FileOutputStream outputStream = new FileOutputStream(file);

                byte[] buffer = new byte[1024];
                int length;
                while ((length = inputStream.read(buffer)) > 0) {
                    outputStream.write(buffer, 0, length);
                }

                inputStream.close();
                outputStream.close();
            } catch (IOException e) {
                Log.e("JsonHelper", "Error copying JSON file from assets", e);
            }
        }
    }

    /*
        Opens and reads the JSON file from internal storage.
        Returns it as a JSONObject so other parts of the app can work with it.
        If the file is corrupted or missing, this quietly returns null instead of crashing.
     */
    public static JSONObject readJson(Context context) {
        File file = new File(context.getFilesDir(), FILE_NAME);
        try {
            FileInputStream fis = new FileInputStream(file);
            BufferedReader reader = new BufferedReader(new InputStreamReader(fis));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            reader.close();
            return new JSONObject(sb.toString());
        } catch (IOException | JSONException e) {
            Log.e("JsonHelper", "Failed to read JSON", e);
            return null;
        }
    }

    /*
        Saves the given JSONObject to our inventory file.
        If it fails to pretty-print (with indentation), it will still save using plain .toString()
        Think of this like hitting 'save' in a video game. We don't want to lose your progress.
     */
    public static void writeJson(Context context, JSONObject jsonObject) {
        File file = new File(context.getFilesDir(), FILE_NAME);
        try {
            FileOutputStream fos = new FileOutputStream(file);
            try {
                fos.write(jsonObject.toString(4).getBytes()); // Save with indentation
            } catch (JSONException e) {
                fos.write(jsonObject.toString().getBytes()); // Backup plan if formatting fails
            }
            fos.close();
        } catch (IOException e) {
            Log.e("JsonHelper", "Failed to write JSON", e);
        }
    }

    /*
        Adds a product to a specific storage location.
        If the same barcode already exists at that location, it increases the quantity instead of duplicating.
        If it’s new, it builds the object with all the key info: name, quantity, barcode, and sets stockQuantity to 1.
        Think of this as the auto-restock logic that avoids accidentally showing "Coke" ten times in a row.
     */
    public static void addItemToStorage(Context context, String location, String name, String quantity, String barcode) {
        try {
            JSONObject json = readJson(context);
            if (json == null) return;

            JSONArray locationItems = json.optJSONArray(location);
            if (locationItems == null) {
                locationItems = new JSONArray();
            }

            boolean itemFound = false;

            // Loop through everything already in that storage location
            // This is like scanning all the items on a shelf to see if you've already got the one you're holding
            for (int i = 0; i < locationItems.length(); i++) {
                JSONObject item = locationItems.getJSONObject(i);
                if (barcode != null && barcode.equals(item.optString("barcode"))) {
                    // Matching barcode found. Add 1 to the count instead of creating a duplicate.
                    int currentQty = item.optInt("stockQuantity", 1);
                    item.put("stockQuantity", currentQty + 1);
                    itemFound = true;
                    break;
                }
            }

            // If no match found, treat it as a brand new item
            if (!itemFound) {
                JSONObject newItem = new JSONObject();
                newItem.put("name", name);
                newItem.put("quantity", quantity);
                newItem.put("barcode", barcode);
                newItem.put("stockQuantity", 1);
                locationItems.put(newItem);
            }

            // Save it all back to the file
            json.put(location, locationItems);
            writeJson(context, json);

        } catch (JSONException e) {
            Log.e("JsonHelper", "Error adding item to storage", e);
        }
    }
}