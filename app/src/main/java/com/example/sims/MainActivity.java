package com.example.sims;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import android.content.Intent;


import androidx.appcompat.app.AppCompatActivity;

import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;

/*
    This activity allows users to either scan a barcode or manually enter one,
    then uses the OpenFoodFacts API to retrieve product details like name,
    quantity, and image URL. Future functionality will include showing these
    details in a new screen and letting users add them to storage.
*/
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        JsonStorageHelper.initializeIfMissing(getApplicationContext());
        Button scanButton = findViewById(R.id.scanButton);
        Button manualEntryButton = findViewById(R.id.manualButton);
        EditText manualBarcodeInput = findViewById(R.id.manualBarcodeInput);
        Button submitManualCodeButton = findViewById(R.id.submitManualCodeButton);
        Button manageStorageButton = findViewById(R.id.manageStorageButton);

        // Button to manually add an item (for barcode-less stuff)
        Button openManualEntryButton = findViewById(R.id.openManualEntryButton);
        openManualEntryButton.setOnClickListener(view -> {
            Intent intent = new Intent(MainActivity.this, ManualEntryActivity.class);
            startActivity(intent);
        });


// Opens the storage location setup screen
        manageStorageButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, StorageActivity.class);
            startActivity(intent);
        });


        // Launches the barcode scanner when the scan button is pressed
        scanButton.setOnClickListener(view -> {
            ScanOptions options = new ScanOptions();
            options.setPrompt("Scan a barcode");
            options.setBeepEnabled(true);
            options.setOrientationLocked(true);
            barcodeLauncher.launch(options);
        });

        // Toggles visibility of the manual input field and submit button
        manualEntryButton.setOnClickListener(view -> {
            if (manualBarcodeInput.getVisibility() == View.GONE) {
                manualBarcodeInput.setVisibility(View.VISIBLE);
                submitManualCodeButton.setVisibility(View.VISIBLE);
            } else {
                manualBarcodeInput.setVisibility(View.GONE);
                submitManualCodeButton.setVisibility(View.GONE);
            }
        });

        // Triggers the same barcode handling logic as scanner, but with typed input
        submitManualCodeButton.setOnClickListener(view -> {
            String code = manualBarcodeInput.getText().toString().trim();
            if (!code.isEmpty()) {
                handleBarcode(code);
            } else {
                Toast.makeText(MainActivity.this, "Please enter a barcode number.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Barcode scanner result callback
    private final androidx.activity.result.ActivityResultLauncher<ScanOptions> barcodeLauncher = registerForActivityResult(
            new ScanContract(),
            result -> {
                if (result.getContents() != null) {
                    handleBarcode(result.getContents());
                }
            }
    );

    /*
        Handles both scanned and manually entered barcodes by querying OpenFoodFacts API.
        For now, just shows the results in a Toast. Will be expanded into a proper screen later.
    */
    private void handleBarcode(String barcode) {
        OpenFoodApiHelper.fetchProductInfo(barcode, new OpenFoodApiHelper.ProductCallback() {
            @Override
            public void onProductReceived(String productName, String quantity, String imageUrl) {
                runOnUiThread(() -> {
                    String result = "Product: " + productName + "\nQuantity: " + quantity;
                    Toast.makeText(MainActivity.this, result, Toast.LENGTH_LONG).show();

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
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, "Failed: " + errorMessage, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }
}
