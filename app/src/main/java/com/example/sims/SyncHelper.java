// --- SYNC HELPER ---
// This class handles merging two different JSON inventories, applying logic to avoid double-counting.
// Basically, imagine two roommates updating the pantry list at the same time. This class makes sure you don't end up with 12 boxes of pasta when you only meant to get 6.
// It calculates: new = yours + theirs - what we already counted last time. That last part is key.
// It also gracefully handles stuff that only shows up on one device, which is harder than it sounds.

package com.example.sims;

import android.content.Context;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class SyncHelper {

    // Entry point: called when a sync file is received (e.g. Nearby Share).
    // Does all the merging and conflict resolution.
    public static void performSync(Context context, Uri externalFileUri) {
        try {
            // Pull in all three JSON blobs: local, incoming, and the historical 'house count' used for diffing
            JSONObject localJson = JsonStorageHelper.readJson(context);
            JSONObject externalJson = readJsonFromUri(context, externalFileUri);
            JSONObject previousHouseCount = readHouseCountJson(context);

            if (localJson == null || externalJson == null) {
                Toast.makeText(context, "One of the JSON files is invalid.", Toast.LENGTH_SHORT).show();
                return;
            }

            JSONObject mergedJson = new JSONObject();
            JSONObject newHouseCount = new JSONObject();

            // Union of all storage locations found in either file (handles new/unknown spots)
            Set<String> allLocations = new HashSet<>();
            allLocations.addAll(toKeySet(localJson));
            allLocations.addAll(toKeySet(externalJson));

            // Now for each location, merge its inventory
            for (String location : allLocations) {
                JSONArray localItems = localJson.optJSONArray(location);
                JSONArray externalItems = externalJson.optJSONArray(location);
                JSONArray mergedArray = new JSONArray();

                if (localItems == null) localItems = new JSONArray();
                if (externalItems == null) externalItems = new JSONArray();

                // Loop through local items and find their counterpart in the external set
                for (int i = 0; i < localItems.length(); i++) {
                    JSONObject localItem = localItems.getJSONObject(i);
                    String barcode = localItem.optString("barcode", null);
                    String name = localItem.optString("name");
                    String quantity = localItem.optString("quantity");
                    int localCount = localItem.optInt("stockQuantity", 1);

                    // Try to find a matching item in the incoming list
                    JSONObject externalItem = findByBarcode(externalItems, barcode);
                    int externalCount = externalItem != null ? externalItem.optInt("stockQuantity", 1) : 0;

                    // Find the previous agreed-upon quantity, if any
                    JSONObject prev = findByBarcode(previousHouseCount.optJSONArray(location), barcode);
                    int oldHouseCount = prev != null ? prev.optInt("stockQuantity", 0) : 0;

                    // Do the sync math: (you + them - last known shared state)
                    int newCount = localCount + externalCount - oldHouseCount;

                    JSONObject mergedItem = new JSONObject();
                    mergedItem.put("name", name);
                    mergedItem.put("quantity", quantity);
                    mergedItem.put("barcode", barcode);
                    mergedItem.put("stockQuantity", newCount);
                    mergedArray.put(mergedItem);
                }

                // Now add items that *only* exist in the incoming file
                for (int i = 0; i < externalItems.length(); i++) {
                    JSONObject externalItem = externalItems.getJSONObject(i);
                    String barcode = externalItem.optString("barcode", null);
                    if (findByBarcode(mergedArray, barcode) != null) continue; // Already merged

                    String name = externalItem.optString("name");
                    String quantity = externalItem.optString("quantity");
                    int externalCount = externalItem.optInt("stockQuantity", 1);

                    JSONObject prev = findByBarcode(previousHouseCount.optJSONArray(location), barcode);
                    int oldHouseCount = prev != null ? prev.optInt("stockQuantity", 0) : 0;

                    int newCount = externalCount - oldHouseCount; // Since local didn't know this existed

                    JSONObject mergedItem = new JSONObject();
                    mergedItem.put("name", name);
                    mergedItem.put("quantity", quantity);
                    mergedItem.put("barcode", barcode);
                    mergedItem.put("stockQuantity", newCount);
                    mergedArray.put(mergedItem);
                }

                mergedJson.put(location, mergedArray);
                newHouseCount.put(location, mergedArray);
            }

            JsonStorageHelper.writeJson(context, mergedJson);
            writeHouseCountJson(context, newHouseCount);

            Toast.makeText(context, "Sync complete.", Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            Log.e("SyncHelper", "Error syncing JSON", e);
            Toast.makeText(context, "Failed to sync: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    // Reads incoming file URI and parses it into a JSONObject
    private static JSONObject readJsonFromUri(Context context, Uri uri) throws IOException, JSONException {
        InputStream inputStream = context.getContentResolver().openInputStream(uri);
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) sb.append(line);
        reader.close();
        return new JSONObject(sb.toString());
    }

    // Helper that gives us all the keys from a JSON object as a Set so we can loop them
    private static Set<String> toKeySet(JSONObject obj) {
        Set<String> keys = new HashSet<>();
        if (obj == null) return keys;
        Iterator<String> iter = obj.keys();
        while (iter.hasNext()) keys.add(iter.next());
        return keys;
    }

    // Used to find matching item in an array by its barcode (our main unique identifier)
    private static JSONObject findByBarcode(JSONArray array, String barcode) throws JSONException {
        if (array == null || barcode == null) return null;
        for (int i = 0; i < array.length(); i++) {
            JSONObject obj = array.getJSONObject(i);
            if (barcode.equals(obj.optString("barcode"))) return obj;
        }
        return null;
    }

    // Reads the saved housecount file that keeps track of our last known shared state
    private static JSONObject readHouseCountJson(Context context) {
        File file = new File(context.getFilesDir(), "housecount.json");
        if (!file.exists()) return new JSONObject();
        try {
            FileInputStream fis = new FileInputStream(file);
            BufferedReader reader = new BufferedReader(new InputStreamReader(fis));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
            reader.close();
            return new JSONObject(sb.toString());
        } catch (Exception e) {
            Log.e("SyncHelper", "Failed to read housecount.json", e);
            return new JSONObject();
        }
    }

    // Writes the new snapshot after syncing so we can subtract it next time
    private static void writeHouseCountJson(Context context, JSONObject houseCountJson) {
        File file = new File(context.getFilesDir(), "housecount.json");
        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(houseCountJson.toString(4).getBytes());
        } catch (IOException | JSONException e) {
            Log.e("SyncHelper", "Failed to write housecount.json", e);
        }
    }
}