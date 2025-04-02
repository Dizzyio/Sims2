// --- OPEN FOOD API HELPER ---
// This class is our designated scout—it runs off to the Open Food Facts API with a barcode in hand,
// and comes back (hopefully) with the product’s name, size, and image URL.
// Instead of making the UI wait around like a bored teenager, it uses a callback to say,
// “Hey, the info’s ready!” when the data comes back.

package com.example.sims;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class OpenFoodApiHelper {

    // Interface that lets us say “when the product is ready, here’s what to do with it”
    public interface ProductCallback {
        void onProductReceived(String name, String quantity, String imageUrl);
        void onError(String error);
    }

    /*
        This function builds and sends a request to the Open Food Facts API
        using the provided barcode. If successful, it extracts useful info and
        passes it to the callback so the UI can display it. Think of it like a courier
        delivering product details to the front-end.
     */
    public static void fetchProductInfo(String barcode, ProductCallback callback) {
        OkHttpClient client = new OkHttpClient();
        String url = "https://world.openfoodfacts.org/api/v0/product/" + barcode + ".json";

        Request request = new Request.Builder().url(url).build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onError("API call failed: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    callback.onError("Unexpected code: " + response);
                    return;
                }

                try {
                    JSONObject json = new JSONObject(response.body().string());

                    if (json.getInt("status") == 1) {
                        JSONObject product = json.getJSONObject("product");

                        /*
                            We want to get the best possible product name,
                            so we check multiple fields in descending order of clarity.
                            This is like checking the fancy label, then the side panel,
                            then the barcode area—until we get something useful.
                            (Because if we leave it to the system, you’ll end up with a blank field for Joe Louis cakes.)
                         */
                        String name = product.optString("product_name_complete", "").trim();

                        if (name.isEmpty()) {
                            name = product.optString("product_name_with_quantity", "").trim();
                        }

                        if (name.isEmpty()) {
                            name = product.optString("product_name_en", "").trim();
                        }

                        if (name.isEmpty()) {
                            name = product.optString("product_name", "").trim();
                        }

                        if (name.isEmpty()) {
                            name = "Unknown Product";
                        }

                        // Grab size info and image link
                        String quantity = product.optString("quantity", "Unknown Size");
                        String imageUrl = product.optString("image_url", "");

                        // Pass results back to whatever part of the app asked for it
                        callback.onProductReceived(name, quantity, imageUrl);
                    } else {
                        callback.onError("Product not found in OpenFoodFacts.");
                    }

                } catch (JSONException e) {
                    callback.onError("Failed to parse JSON: " + e.getMessage());
                }
            }
        });
    }
}