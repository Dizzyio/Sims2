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

/*
    This activity shows detailed product information by using the saved barcode.
    It fetches data from the Open Food Facts API and displays:
    - Full product name, brand, quantity
    - Nutrition info (calories, fat, sugar, salt)
    - Ingredients, allergens, Nutri-Score, NOVA Group
    - Product origin and store availability
    - Product image
*/
public class ItemDetailActivity extends AppCompatActivity {

    private TextView titleView, detailsView;
    private ImageView productImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_item_detail);

        titleView = findViewById(R.id.detailTitle);
        detailsView = findViewById(R.id.detailText);
        productImage = findViewById(R.id.detailImage);

        String barcode = getIntent().getStringExtra("barcode");

        if (barcode == null || barcode.isEmpty()) {
            Toast.makeText(this, "No barcode provided.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        fetchProductDetails(barcode);
    }

    // Pulls product info and updates the detail view with useful fields
    private void fetchProductDetails(String barcode) {
        OkHttpClient client = new OkHttpClient();
        String url = "https://world.openfoodfacts.org/api/v2/product/" + barcode + ".json";

        Request request = new Request.Builder().url(url).build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> Toast.makeText(ItemDetailActivity.this, "API request failed.", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    runOnUiThread(() -> Toast.makeText(ItemDetailActivity.this, "Product not found.", Toast.LENGTH_SHORT).show());
                    return;
                }

                try {
                    JSONObject root = new JSONObject(response.body().string());
                    JSONObject product = root.getJSONObject("product");

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

                    // Build detailed nutrition view from top keys
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

                    // Format the final block of info
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

                    // Push everything to the UI
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
