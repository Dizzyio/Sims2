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

/*
    This class makes a request to the Open Food Facts API with a given barcode,
    and then extracts the product name, quantity, and image URL from the JSON response.
    It returns the result to a callback function so the UI can update once the data arrives.
*/
public class OpenFoodApiHelper {

    public interface ProductCallback {
        void onProductReceived(String name, String quantity, String imageUrl);
        void onError(String error);
    }

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
                            Try to extract the most complete and human-readable product name.
                            It checks multiple fields in order and trims each to avoid false-positives
                            from empty strings.
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

                        String quantity = product.optString("quantity", "Unknown Size");
                        String imageUrl = product.optString("image_url", "");

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
