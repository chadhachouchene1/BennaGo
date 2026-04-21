package com.example.bennago;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.bennago.db.AppDatabase;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class CheckoutActivity extends AppCompatActivity {

    private TextInputEditText etPhone, etAddress, etNote;
    private TextInputLayout   tilPhone, tilAddress;
    private TextView          tvOrderTotal, tvOrderSummary;
    private MaterialButton    btnConfirm;

    private AppDatabase db;
    private CartManager cart;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_checkout);

        db   = AppDatabase.getInstance(this);
        cart = CartManager.getInstance();

        ImageButton btnBack = findViewById(R.id.btn_back);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        etPhone        = findViewById(R.id.et_checkout_phone);
        etAddress      = findViewById(R.id.et_checkout_address);
        etNote         = findViewById(R.id.et_checkout_note);
        tilPhone       = findViewById(R.id.til_checkout_phone);
        tilAddress     = findViewById(R.id.til_checkout_address);
        tvOrderTotal   = findViewById(R.id.tv_checkout_total);
        tvOrderSummary = findViewById(R.id.tv_checkout_summary);
        btnConfirm     = findViewById(R.id.btn_confirm_order);

        tvOrderTotal.setText(String.format("%.3f TND", cart.getTotalPrice()));
        buildSummary();

        // ✅ Bouton redirige vers PaymentActivity au lieu de passer la commande directement
        btnConfirm.setText("→  Choisir le paiement");
        btnConfirm.setOnClickListener(v -> validateAndGoToPayment());
    }

    private void buildSummary() {
        StringBuilder sb = new StringBuilder();
        for (CartManager.CartItem item : cart.getItems()) {
            sb.append("• ").append(item.dish.getName())
                    .append("  x").append(item.quantity)
                    .append("  (").append(String.format("%.3f", item.getSubtotal())).append(" TND)\n");
        }
        tvOrderSummary.setText(sb.toString().trim());
    }

    private void validateAndGoToPayment() {
        String phone   = getText(etPhone);
        String address = getText(etAddress);
        String note    = getText(etNote);

        tilPhone.setError(null);
        tilAddress.setError(null);

        boolean valid = true;
        if (TextUtils.isEmpty(phone))   { tilPhone.setError("Téléphone requis");  valid = false; }
        if (TextUtils.isEmpty(address)) { tilAddress.setError("Adresse requise"); valid = false; }
        if (!valid) return;

        // ✅ Passer les infos à PaymentActivity
        Intent i = new Intent(this, PaymentActivity.class);
        i.putExtra("phone",   phone);
        i.putExtra("address", address);
        i.putExtra("note",    note);
        i.putExtra("total",   cart.getTotalPrice());
        startActivity(i);
    }

    private String getText(TextInputEditText et) {
        return et != null && et.getText() != null ? et.getText().toString().trim() : "";
    }
}