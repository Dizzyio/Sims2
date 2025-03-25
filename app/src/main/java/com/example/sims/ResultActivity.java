package com.example.sims;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;

/*
    This activity displays the scanned or manually entered product's details:
    - Product name
    - Quantity
    - Image (if available from the API)

    It receives data from MainActivity using Intent extras.
*/
public class ResultActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);

        TextView productNameView = findViewById(R.id.productName);
        TextView quantityView = findViewById(R.id.productQuantity);
        ImageView productImageView = findViewById(R.id.productImage);

        // Get data from the Intent
        String productName = getIntent().getStringExtra("productName");
        String quantity = getIntent().getStringExtra("quantity");
        String imageUrl = getIntent().getStringExtra("imageUrl");

        // Display data
        productNameView.setText(productName);
        quantityView.setText(quantity);

        // Load image using Glide if URL exists
        if (imageUrl != null && !imageUrl.isEmpty()) {
            Glide.with(this).load(imageUrl).into(productImageView);
        }
    }
}
