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

/*
    This helper class manages reading, initializing, and writing to the inventory JSON file.
    It checks if the file exists in internal storage. If not, it copies the default version from assets.
    Provides methods for loading and saving the data as a JSONObject.
 */
public class JsonStorageHelper {

    private static final String FILE_NAME = "inventory_data.json";

    // Checks for existing file in internal storage, copies from assets if missing
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

    // Reads and returns the entire file as a JSONObject
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

    // Saves the provided JSONObject back to the file
    public static void writeJson(Context context, JSONObject jsonObject) {
        File file = new File(context.getFilesDir(), FILE_NAME);
        try {
            FileOutputStream fos = new FileOutputStream(file);
            try {
                fos.write(jsonObject.toString(4).getBytes()); // Pretty print
            } catch (JSONException e) {
                fos.write(jsonObject.toString().getBytes()); // Fallback if indentation fails
            }
            fos.close();
        } catch (IOException e) {
            Log.e("JsonHelper", "Failed to write JSON", e);
        }
    }

    // Adds an item to a specific storage location in the JSON structure
    public static void addItemToStorage(Context context, String location, String productName, String quantity) {
        try {
            JSONObject root = readJson(context);
            if (root == null) {
                root = new JSONObject();
            }

            // Check if location exists; if not, create it
            JSONArray locationArray = root.optJSONArray(location);
            if (locationArray == null) {
                locationArray = new JSONArray();
                root.put(location, locationArray);
            }

            // Create new product object
            JSONObject newItem = new JSONObject();
            newItem.put("name", productName);
            newItem.put("quantity", quantity);

            // Add to storage array
            locationArray.put(newItem);

            // Save the changes
            writeJson(context, root);

        } catch (JSONException e) {
            Log.e("JsonHelper", "Failed to add item to storage", e);
        }
    }
}