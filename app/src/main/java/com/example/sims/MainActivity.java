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

/*
    This activity allows users to either scan a barcode or manually enter one,
    then uses the OpenFoodFacts API to retrieve product details like name,
    quantity, and image URL. Also handles syncing via shared JSON files.
*/
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        /*
            NEW: This checks if the app was opened by a shared file (via Nearby Share, etc)
            If so, it prompts the user to sync using the attached file.
        */
        Intent intent = getIntent();
        if (Intent.ACTION_VIEW.equals(intent.getAction()) && intent.getData() != null) {
            try {
                // Create a file from the URI and prompt sync
                java.io.File importedFile = new java.io.File(intent.getData().getPath());

                Uri incomingUri = intent.getData();

                new AlertDialog.Builder(MainActivity.this)  // ✅ Use MainActivity.this for context
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

        setContentView(R.layout.activity_main);
        JsonStorageHelper.initializeIfMissing(getApplicationContext());

        Button scanButton = findViewById(R.id.scanButton);
        Button manualEntryButton = findViewById(R.id.manualButton);
        EditText manualBarcodeInput = findViewById(R.id.manualBarcodeInput);
        Button submitManualCodeButton = findViewById(R.id.submitManualCodeButton);
        Button manageStorageButton = findViewById(R.id.manageStorageButton);
        Button syncButton = findViewById(R.id.syncButton);

        syncButton.setOnClickListener(v -> {
            Intent syncIntent = new Intent(MainActivity.this, SyncActivity.class);
            startActivity(syncIntent);
        });

        Button openManualEntryButton = findViewById(R.id.openManualEntryButton);
        openManualEntryButton.setOnClickListener(view -> {
            Intent intentManual = new Intent(MainActivity.this, ManualEntryActivity.class);
            startActivity(intentManual);
        });

        manageStorageButton.setOnClickListener(v -> {
            Intent intentStorage = new Intent(MainActivity.this, StorageActivity.class);
            startActivity(intentStorage);
        });

        scanButton.setOnClickListener(view -> {
            ScanOptions options = new ScanOptions();
            options.setPrompt("Scan a barcode");
            options.setBeepEnabled(true);
            options.setOrientationLocked(true);
            barcodeLauncher.launch(options);
        });

        manualEntryButton.setOnClickListener(view -> {
            if (manualBarcodeInput.getVisibility() == View.GONE) {
                manualBarcodeInput.setVisibility(View.VISIBLE);
                submitManualCodeButton.setVisibility(View.VISIBLE);
            } else {
                manualBarcodeInput.setVisibility(View.GONE);
                submitManualCodeButton.setVisibility(View.GONE);
            }
        });

        submitManualCodeButton.setOnClickListener(view -> {
            String code = manualBarcodeInput.getText().toString().trim();
            if (!code.isEmpty()) {
                handleBarcode(code);
            } else {
                Toast.makeText(MainActivity.this, "Please enter a barcode number.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private final androidx.activity.result.ActivityResultLauncher<ScanOptions> barcodeLauncher =
            registerForActivityResult(new ScanContract(), result -> {
                if (result.getContents() != null) {
                    handleBarcode(result.getContents());
                }
            });

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
