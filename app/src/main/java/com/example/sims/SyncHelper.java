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

/*
    This class handles syncing two JSON files by applying conflict resolution logic:
    newStock = yours + theirs - houseCount
    It now also handles edge cases where one device has locations the other doesn't.
*/
public class SyncHelper {

    // Entry point for syncing using a URI (shared file via intent)
    public static void performSync(Context context, Uri externalFileUri) {
        try {
            JSONObject localJson = JsonStorageHelper.readJson(context);
            JSONObject externalJson = readJsonFromUri(context, externalFileUri);
            JSONObject previousHouseCount = readHouseCountJson(context);

            if (localJson == null || externalJson == null) {
                Toast.makeText(context, "One of the JSON files is invalid.", Toast.LENGTH_SHORT).show();
                return;
            }

            JSONObject mergedJson = new JSONObject();
            JSONObject newHouseCount = new JSONObject();

            // Merge all locations from both local and external files
            Set<String> allLocations = new HashSet<>();
            allLocations.addAll(toKeySet(localJson));
            allLocations.addAll(toKeySet(externalJson));

            for (String location : allLocations) {
                JSONArray localItems = localJson.optJSONArray(location);
                JSONArray externalItems = externalJson.optJSONArray(location);
                JSONArray mergedArray = new JSONArray();

                if (localItems == null) localItems = new JSONArray();
                if (externalItems == null) externalItems = new JSONArray();

                for (int i = 0; i < localItems.length(); i++) {
                    JSONObject localItem = localItems.getJSONObject(i);
                    String barcode = localItem.optString("barcode", null);
                    String name = localItem.optString("name");
                    String quantity = localItem.optString("quantity");
                    int localCount = localItem.optInt("stockQuantity", 1);

                    JSONObject externalItem = findByBarcode(externalItems, barcode);
                    int externalCount = externalItem != null ? externalItem.optInt("stockQuantity", 1) : 0;

                    JSONObject prev = findByBarcode(previousHouseCount.optJSONArray(location), barcode);
                    int oldHouseCount = prev != null ? prev.optInt("stockQuantity", 0) : 0;

                    int newCount = localCount + externalCount - oldHouseCount;

                    JSONObject mergedItem = new JSONObject();
                    mergedItem.put("name", name);
                    mergedItem.put("quantity", quantity);
                    mergedItem.put("barcode", barcode);
                    mergedItem.put("stockQuantity", newCount);
                    mergedArray.put(mergedItem);
                }

                // Also check for any external-only items
                for (int i = 0; i < externalItems.length(); i++) {
                    JSONObject externalItem = externalItems.getJSONObject(i);
                    String barcode = externalItem.optString("barcode", null);
                    if (findByBarcode(mergedArray, barcode) != null) continue;

                    String name = externalItem.optString("name");
                    String quantity = externalItem.optString("quantity");
                    int externalCount = externalItem.optInt("stockQuantity", 1);

                    JSONObject prev = findByBarcode(previousHouseCount.optJSONArray(location), barcode);
                    int oldHouseCount = prev != null ? prev.optInt("stockQuantity", 0) : 0;

                    int newCount = externalCount - oldHouseCount;

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

    // Reads the shared file into a JSON object
    private static JSONObject readJsonFromUri(Context context, Uri uri) throws IOException, JSONException {
        InputStream inputStream = context.getContentResolver().openInputStream(uri);
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) sb.append(line);
        reader.close();
        return new JSONObject(sb.toString());
    }

    // Converts a JSONObject to a Set of keys for looping
    private static Set<String> toKeySet(JSONObject obj) {
        Set<String> keys = new HashSet<>();
        if (obj == null) return keys;
        Iterator<String> iter = obj.keys();
        while (iter.hasNext()) keys.add(iter.next());
        return keys;
    }

    // Finds a product in a list based on barcode
    private static JSONObject findByBarcode(JSONArray array, String barcode) throws JSONException {
        if (array == null || barcode == null) return null;
        for (int i = 0; i < array.length(); i++) {
            JSONObject obj = array.getJSONObject(i);
            if (barcode.equals(obj.optString("barcode"))) return obj;
        }
        return null;
    }

    // Load previous snapshot
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

    // Save updated snapshot
    private static void writeHouseCountJson(Context context, JSONObject houseCountJson) {
        File file = new File(context.getFilesDir(), "housecount.json");
        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(houseCountJson.toString(4).getBytes());
        } catch (IOException | JSONException e) {
            Log.e("SyncHelper", "Failed to write housecount.json", e);
        }
    }
}
