// --- SYNC ACTIVITY ---
// This is where the inventory gets to go on a little vacation—from your phone to someone else’s.
// You can export your current stash as a JSON file and share it with anyone using Nearby Share,
// Bluetooth, or carrier pigeon if they can decode binary. Receiving files also gets handled here,
// using some safe file-copying and a sync helper to merge it into your local setup.

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

public class SyncActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sync);

        Button shareButton = findViewById(R.id.shareButton);

        // --- EXPORT FUNCTION ---
        // This chunk handles exporting your inventory to a JSON file you can send to someone else.
        // Could be used to clone your kitchen's contents or just flex your snack game.
        shareButton.setOnClickListener(v -> {
            try {
                // Load the local inventory data (if it exists)
                JSONObject json = JsonStorageHelper.readJson(this);
                if (json == null) {
                    Toast.makeText(this, "No inventory data to share.", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Write that data to a temporary file
                File outFile = new File(getExternalCacheDir(), "SIMS_inventory_export.json");
                try (FileOutputStream fos = new FileOutputStream(outFile)) {
                    fos.write(json.toString(4).getBytes()); // Fancy pretty-printed JSON
                }

                // Generate a URI that Android will allow us to send
                Uri uri = FileProvider.getUriForFile(
                        this,
                        "com.example.sims.fileprovider",
                        outFile
                );

                // Trigger the system share panel so user can pick their app of choice
                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("application/json");
                shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
                shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                startActivity(Intent.createChooser(shareIntent, "Share Inventory")); // This sneaky little guy launches the share menu

            } catch (IOException | JSONException e) {
                Toast.makeText(this, "Failed to share inventory: " + e.getMessage(), Toast.LENGTH_LONG).show();
                e.printStackTrace();
            }
        });

        // --- IMPORT FUNCTION ---
        // If the app was launched via a file (like from Nearby Share), we pick it up here.
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

            // Copy file to a temp directory we control
            File tempFile = new File(getCacheDir(), "incoming_sync.json");

            FileOutputStream outputStream = new FileOutputStream(tempFile);
            byte[] buffer = new byte[4096];
            int bytesRead;

            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }

            inputStream.close();
            outputStream.close();

            // Create a URI for our internal copy and pass to our merge logic
            Uri fileUri = FileProvider.getUriForFile(
                    this,
                    "com.example.sims.fileprovider", // Has to match the manifest authority
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
