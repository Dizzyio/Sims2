// --- ITEM DETAIL ACTIVITY ---
// This class handles the detail screen that shows up when a user taps an item with a barcode.
// Think of it like turning over a box in your pantry to read the nutrition label and ingredients—except we cheat by pulling the data from Open Food Facts.
// This screen displays: name, brand, nutrition, ingredients, allergens, NutriScore, NOVA group, origin, stores, and product image.
// It sends an API request, parses the result, and updates the screen on the main thread.

package com.example.sims;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class ItemDetailActivity extends AppCompatActivity {

    // These are the visual pieces of our screen: title at the top, detail text block, and an image
    private TextView titleView, detailsView;
    private ImageView productImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_item_detail);

        // Grab UI elements by their IDs
        titleView = findViewById(R.id.detailTitle);
        detailsView = findViewById(R.id.detailText);
        productImage = findViewById(R.id.detailImage);

        // This barcode comes from the last screen. Without it, we can’t do anything.
        String barcode = getIntent().getStringExtra("barcode");

        if (barcode == null || barcode.isEmpty()) {
            // User somehow got here without a barcode? No point continuing.
            Toast.makeText(this, "No barcode provided.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Fire off the data fetch using that barcode
        fetchProductDetails(barcode);
    }

    // This function does all the heavy lifting: reaches out to the API, parses the response,
    // and updates the screen with the product info
    private void fetchProductDetails(String barcode) {
        OkHttpClient client = new OkHttpClient(); // Think of this like a mailman for HTTP requests
        String url = "https://world.openfoodfacts.org/api/v2/product/" + barcode + ".json";

        Request request = new Request.Builder().url(url).build(); // Building the request like an envelope

        client.newCall(request).enqueue(new Callback() {
            // If the call fails (e.g., no internet), we give the user a heads-up
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> Toast.makeText(ItemDetailActivity.this, "API request failed.", Toast.LENGTH_SHORT).show());
            }

            // If the call succeeds, we try to unpack the box and display the goodies
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    runOnUiThread(() -> Toast.makeText(ItemDetailActivity.this, "Product not found.", Toast.LENGTH_SHORT).show());
                    return;
                }

                try {
                    // Crack open the JSON response and pull out what we care about
                    JSONObject root = new JSONObject(response.body().string());
                    JSONObject product = root.getJSONObject("product");

                    // Grab all the product details we want to show
                    String name = product.optString("product_name", "Unknown Product");
                    String brand = product.optString("brands", "N/A");
                    String quantity = product.optString("quantity", "");
                    String imageUrl = product.optString("image_url", "");
                    String categories = product.optString("categories", "");
                    String ingredients = product.optString("ingredients_text", "");
                    String allergens = product.optString("allergens", "None listed");
                    String nutriscore = product.optString("nutriscore_grade", "").toUpperCase();
                    String nova = product.optString("nova_group", "");
                    String origin = product.optString("origins", "Unknown");
                    String stores = product.optString("stores", "");

                    // Build a human-readable nutrition breakdown
                    // This is where we turn "nutriments.energy-kcal_100g" into "Calories: 320 kcal/100g"
                    StringBuilder nutritionDetails = new StringBuilder();
                    JSONObject nutrients = product.optJSONObject("nutriments");
                    if (nutrients != null) {
                        nutritionDetails.append("Calories: ").append(nutrients.optString("energy-kcal_100g", "N/A")).append(" kcal/100g\n");
                        nutritionDetails.append("Fat: ").append(nutrients.optString("fat_100g", "N/A")).append(" g\n");
                        nutritionDetails.append("Sugars: ").append(nutrients.optString("sugars_100g", "N/A")).append(" g\n");
                        nutritionDetails.append("Salt: ").append(nutrients.optString("salt_100g", "N/A")).append(" g\n");
                    } else {
                        nutritionDetails.append("No nutrition data available.\n");
                    }

                    // Now build the full detail block for the text view
                    // Basically a big sandwich of everything we found
                    StringBuilder details = new StringBuilder();
                    details.append("Brand: ").append(brand).append("\n");
                    details.append("Quantity: ").append(quantity).append("\n");
                    details.append("Categories: ").append(categories).append("\n");
                    details.append("Origin: ").append(origin).append("\n");
                    details.append("Nutri-Score: ").append(nutriscore.isEmpty() ? "N/A" : nutriscore).append("\n");
                    details.append("NOVA Group: ").append(nova.isEmpty() ? "N/A" : nova).append("\n");
                    if (!stores.isEmpty()) {
                        details.append("Sold at: ").append(stores).append("\n");
                    }
                    details.append("\nIngredients:\n").append(ingredients).append("\n\n");
                    details.append("Allergens: ").append(allergens).append("\n\n");
                    details.append("Nutrition Info:\n").append(nutritionDetails);

                    // Finally, we can now show all this on the screen (but ONLY on the UI thread)
                    runOnUiThread(() -> {
                        titleView.setText(name);
                        detailsView.setText(details.toString());
                        if (!imageUrl.isEmpty()) {
                            Glide.with(ItemDetailActivity.this).load(imageUrl).into(productImage);
                        }
                    });

                } catch (JSONException e) {
                    runOnUiThread(() -> Toast.makeText(ItemDetailActivity.this, "Failed to parse data.", Toast.LENGTH_SHORT).show());
                }
            }
        });
    }
}
