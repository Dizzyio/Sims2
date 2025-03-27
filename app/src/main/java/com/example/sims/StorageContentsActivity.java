package com.example.sims;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

/*
    This activity shows the list of items currently stored in a given location.
    It pulls the item list from the JSON file based on the location passed via Intent.
*/
public class StorageContentsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_storage_contents);

        TextView locationHeader = findViewById(R.id.locationHeader);
        ListView contentsListView = findViewById(R.id.contentsList);

        String locationName = getIntent().getStringExtra("locationName");
        locationHeader.setText("Contents of: " + locationName);

        ArrayList<String> itemList = new ArrayList<>();
        JSONObject json = JsonStorageHelper.readJson(this);

        if (json != null && json.has(locationName)) {
            try {
                JSONArray items = json.getJSONArray(locationName);
                for (int i = 0; i < items.length(); i++) {
                    JSONObject item = items.getJSONObject(i);
                    String name = item.optString("name", "Unknown");
                    String quantity = item.optString("quantity", "?");
                    itemList.add(name + " - Qty: " + quantity);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, itemList);
        contentsListView.setAdapter(adapter);
    }
}
