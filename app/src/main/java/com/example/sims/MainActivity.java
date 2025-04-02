// --- MAIN ACTIVITY ---
// This is the launchpad for everything in the app. It’s your home base.
// From here, users can scan barcodes, manually enter codes, view storage, or sync data.
// It also quietly handles some behind-the-scenes setup, like reading shared files when the app is opened from Nearby Share.
// In short: if this app were a house, this would be the front door.

package com.example.sims;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        /*
            This block checks if the app was launched by a file intent (like from Nearby Share).
            If it detects a file was shared, it asks the user if they want to sync using that file.
            Pretty slick way to keep things in sync without needing the cloud.
         */
        Intent intent = getIntent();
        if (Intent.ACTION_VIEW.equals(intent.getAction()) && intent.getData() != null) {
            try {
                Uri incomingUri = intent.getData();

                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("Sync File Detected")
                        .setMessage("Would you like to sync using the received file?")
                        .setPositiveButton("Yes", (dialog, which) -> {
                            if (incomingUri != null) {
                                SyncHelper.performSync(MainActivity.this, incomingUri);
                            } else {
                                Toast.makeText(MainActivity.this, "No file found to sync.", Toast.LENGTH_SHORT).show();
                            }
                        })
                        .setNegativeButton("No", null)
                        .show();

            } catch (Exception e) {
                Toast.makeText(this, "Could not load received sync file.", Toast.LENGTH_SHORT).show();
            }
        }

        // Load the main layout and set up the initial JSON file if it’s the first time running
        setContentView(R.layout.activity_main);
        JsonStorageHelper.initializeIfMissing(getApplicationContext());

        // Hook up all the buttons from the layout to variables
        Button scanButton = findViewById(R.id.scanButton);
        Button manualEntryButton = findViewById(R.id.manualButton);
        EditText manualBarcodeInput = findViewById(R.id.manualBarcodeInput);
        Button submitManualCodeButton = findViewById(R.id.submitManualCodeButton);
        Button manageStorageButton = findViewById(R.id.manageStorageButton);
        Button syncButton = findViewById(R.id.syncButton);

        // Opens the Sync screen, where users can export or import inventory files
        syncButton.setOnClickListener(v -> {
            Intent syncIntent = new Intent(MainActivity.this, SyncActivity.class);
            startActivity(syncIntent);
        });

        // This one opens the manual item entry form (for stuff with no barcode)
        Button openManualEntryButton = findViewById(R.id.openManualEntryButton);
        openManualEntryButton.setOnClickListener(view -> {
            Intent intentManual = new Intent(MainActivity.this, ManualEntryActivity.class);
            startActivity(intentManual);
        });

        // Lets users view and manage all the custom storage locations
        manageStorageButton.setOnClickListener(v -> {
            Intent intentStorage = new Intent(MainActivity.this, StorageActivity.class);
            startActivity(intentStorage);
        });

        // This triggers the actual camera-based barcode scanner
        scanButton.setOnClickListener(view -> {
            ScanOptions options = new ScanOptions();
            options.setPrompt("Scan a barcode");
            options.setBeepEnabled(true);
            options.setOrientationLocked(true);
            barcodeLauncher.launch(options);
        });

        // Toggles the manual barcode input field on and off when user clicks "Manual Entry"
        manualEntryButton.setOnClickListener(view -> {
            if (manualBarcodeInput.getVisibility() == View.GONE) {
                manualBarcodeInput.setVisibility(View.VISIBLE);
                submitManualCodeButton.setVisibility(View.VISIBLE);
            } else {
                manualBarcodeInput.setVisibility(View.GONE);
                submitManualCodeButton.setVisibility(View.GONE);
            }
        });

        // When the user manually enters a barcode, this sends it off to get processed
        submitManualCodeButton.setOnClickListener(view -> {
            String code = manualBarcodeInput.getText().toString().trim();
            if (!code.isEmpty()) {
                handleBarcode(code);
            } else {
                Toast.makeText(MainActivity.this, "Please enter a barcode number.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // This is the launcher that receives results from the barcode scanner
    // If you scan something, this is what catches the result and forwards it to handleBarcode()
    private final androidx.activity.result.ActivityResultLauncher<ScanOptions> barcodeLauncher =
            registerForActivityResult(new ScanContract(), result -> {
                if (result.getContents() != null) {
                    handleBarcode(result.getContents());
                }
            });

    // This method takes a barcode and sends it to the OpenFoodFacts API helper
    // If the product exists, we launch the results page and pass all the info along
    private void handleBarcode(String barcode) {
        OpenFoodApiHelper.fetchProductInfo(barcode, new OpenFoodApiHelper.ProductCallback() {
            @Override
            public void onProductReceived(String productName, String quantity, String imageUrl) {
                runOnUiThread(() -> {
                    Intent intent = new Intent(MainActivity.this, ResultActivity.class);
                    intent.putExtra("productName", productName);
                    intent.putExtra("quantity", quantity);
                    intent.putExtra("imageUrl", imageUrl);
                    intent.putExtra("barcode", barcode);
                    startActivity(intent);
                });
            }

            @Override
            public void onError(String errorMessage) {
                runOnUiThread(() ->
                        Toast.makeText(MainActivity.this, "Failed: " + errorMessage, Toast.LENGTH_SHORT).show());
            }
        });
    }
}