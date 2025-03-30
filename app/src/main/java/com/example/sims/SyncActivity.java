package com.example.sims;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import org.json.JSONException;
import org.json.JSONObject;
import java.io.InputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

/*
    This activity handles syncing your local inventory with other users.
    Right now, it allows you to export your JSON database and share it via Nearby Share, Bluetooth, etc.
    In the future, we can expand this to handle automatic merging.
*/

public class SyncActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sync);

        Button shareButton = findViewById(R.id.shareButton);
        shareButton.setOnClickListener(v -> {
            try {
                // Step 1: Load current local inventory
                JSONObject json = JsonStorageHelper.readJson(this);
                if (json == null) {
                    Toast.makeText(this, "No inventory data to share.", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Step 2: Write to a temporary file in external cache
                File outFile = new File(getExternalCacheDir(), "SIMS_inventory_export.json");
                try (FileOutputStream fos = new FileOutputStream(outFile)) {
                    fos.write(json.toString(4).getBytes());
                }

                // Step 3: Generate a content URI for the file
                Uri uri = FileProvider.getUriForFile(
                        this,
                        "com.example.sims.fileprovider",
                        outFile
                );

                // Step 4: Share using Android's system share sheet
                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("application/json");
                shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
                shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                startActivity(Intent.createChooser(shareIntent, "Share Inventory"));

            } catch (IOException | JSONException e) {
                Toast.makeText(this, "Failed to share inventory: " + e.getMessage(), Toast.LENGTH_LONG).show();
                e.printStackTrace();
            }
        });

        Uri dataUri = getIntent().getData();

        if (dataUri == null) {
            Toast.makeText(this, "No sync file received", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            InputStream inputStream = getContentResolver().openInputStream(dataUri);

            if (inputStream == null) {
                Toast.makeText(this, "Could not open file stream", Toast.LENGTH_SHORT).show();
                return;
            }

            // Safe temp file location
            File tempFile = new File(getCacheDir(), "incoming_sync.json");

            FileOutputStream outputStream = new FileOutputStream(tempFile);
            byte[] buffer = new byte[4096];
            int bytesRead;

            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }

            inputStream.close();
            outputStream.close();

            // Pass to sync logic
            Uri fileUri = FileProvider.getUriForFile(
                    this,
                    "com.example.sims.fileprovider", // Match your manifest provider authority
                    tempFile
            );
            SyncHelper.performSync(this, fileUri);

        } catch (FileNotFoundException fnfe) {
            Toast.makeText(this, "File not found: " + fnfe.getMessage(), Toast.LENGTH_LONG).show();
            fnfe.printStackTrace();
        } catch (Exception e) {
            Toast.makeText(this, "Failed to process sync file: " + e.getMessage(), Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }
}
