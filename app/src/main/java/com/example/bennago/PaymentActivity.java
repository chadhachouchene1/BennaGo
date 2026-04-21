package com.example.bennago;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.example.bennago.db.AppDatabase;
import com.example.bennago.entity.Order;
import com.example.bennago.entity.OrderItem;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.messaging.FirebaseMessaging;
import com.stripe.android.PaymentConfiguration;
import com.stripe.android.paymentsheet.PaymentSheet;
import com.stripe.android.paymentsheet.PaymentSheetResult;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PaymentActivity extends AppCompatActivity {

    private static final String TAG = "PaymentActivity";

    // ✅ Remplacez par votre clé publique Stripe TEST
    private static final String STRIPE_PUBLISHABLE_KEY =
            "pk_test_51TNz0MPtVN7fNQrc9FSIDDnfKOvrilV8i1QH0vtvVWg2qghahi71i7J8BnbxOA67FSKYERip3VYoKzmCyHIlO8bZ00EzxdVi0R";

    // ✅ URL de votre serveur Render
    private static final String SERVER_URL =
            "https://bennago-notif-server.onrender.com";


    private String phone, address, note;
    private double total;
    private String selectedMethod = "";

    private SessionManager        sessionManager;
    private AppDatabase           db;
    private CartManager           cart;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    // Stripe
    private PaymentSheet paymentSheet;
    private String       clientSecret   = "";
    private boolean      fetchingSecret = false;

    // TextWatcher flags
    private boolean formattingCard   = false;
    private boolean formattingExpiry = false;

    // ── Vues méthodes ─────────────────────────────────────────────────────────
    private CardView       cardLivraison, cardCash;
    private TextView       tvTotal, tvSelectedMethod;
    private LinearLayout   layoutMethods, layoutCarteForm,
            layoutCashInfo, layoutProcessing;
    private MaterialButton btnPay;
    private ProgressBar    progressBar;
    private TextView       tvProcessingMsg;

    // ── Formulaire carte (sous Livraison) ─────────────────────────────────────
    private TextInputEditText etCardNumber, etCardHolder, etExpiry, etCvv;
    private TextInputLayout   tilCardNumber, tilCardHolder, tilExpiry, tilCvv;
    private TextView          tvCardType, tvCardPreviewNumber,
            tvCardPreviewHolder, tvCardPreviewExpiry;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment);

        sessionManager = new SessionManager(this);
        db   = AppDatabase.getInstance(this);
        cart = CartManager.getInstance();

        Intent intent = getIntent();
        phone   = intent.getStringExtra("phone");
        address = intent.getStringExtra("address");
        note    = intent.getStringExtra("note");
        total   = intent.getDoubleExtra("total", 0);

        // Initialiser Stripe
        PaymentConfiguration.init(this, STRIPE_PUBLISHABLE_KEY);
        paymentSheet = new PaymentSheet(this, this::onPaymentSheetResult);

        ImageButton btnBack = findViewById(R.id.btn_back);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        initViews();
        fetchFcmToken();
    }

    private void initViews() {
        tvTotal          = findViewById(R.id.tv_payment_total);
        tvSelectedMethod = findViewById(R.id.tv_selected_method);
        btnPay           = findViewById(R.id.btn_pay_now);
        cardLivraison    = findViewById(R.id.card_livraison);
        cardCash         = findViewById(R.id.card_cash);
        layoutMethods    = findViewById(R.id.layout_payment_methods);
        layoutCarteForm  = findViewById(R.id.layout_carte_form);
        layoutCashInfo   = findViewById(R.id.layout_cash_info);
        layoutProcessing = findViewById(R.id.layout_processing);
        progressBar      = findViewById(R.id.progress_payment);
        tvProcessingMsg  = findViewById(R.id.tv_processing_msg);

        // Formulaire carte
        etCardNumber         = findViewById(R.id.et_card_number);
        etCardHolder         = findViewById(R.id.et_card_holder);
        etExpiry             = findViewById(R.id.et_expiry);
        etCvv                = findViewById(R.id.et_cvv);
        tilCardNumber        = findViewById(R.id.til_card_number);
        tilCardHolder        = findViewById(R.id.til_card_holder);
        tilExpiry            = findViewById(R.id.til_expiry);
        tilCvv               = findViewById(R.id.til_cvv);
        tvCardType           = findViewById(R.id.tv_card_type);
        tvCardPreviewNumber  = findViewById(R.id.tv_card_preview_number);
        tvCardPreviewHolder  = findViewById(R.id.tv_card_preview_holder);
        tvCardPreviewExpiry  = findViewById(R.id.tv_card_preview_expiry);

        tvTotal.setText(String.format("%.3f TND", total));
        btnPay.setEnabled(false);

        // ── Sélection méthode ─────────────────────────────────────────────────
        cardLivraison.setOnClickListener(v -> selectMethod("Livraison 🚚"));
        cardCash.setOnClickListener(v      -> selectMethod("Espèces 💵"));

        btnPay.setOnClickListener(v -> processPayment());

        setupCardNumberWatcher();
        setupExpiryWatcher();
        setupHolderWatcher();
    }

    // ── Sélection méthode ─────────────────────────────────────────────────────
    private void selectMethod(String method) {
        selectedMethod = method;
        tvSelectedMethod.setText("✓ " + method);
        tvSelectedMethod.setVisibility(View.VISIBLE);

        int def = 0xFFF5EFE6, sel = 0xFFEDE4D8;
        cardLivraison.setCardBackgroundColor(def);
        cardCash.setCardBackgroundColor(def);

        layoutCarteForm.setVisibility(View.GONE);
        layoutCashInfo.setVisibility(View.GONE);

        if (method.contains("Livraison")) {
            // ✅ Livraison → affiche le formulaire carte Stripe
            cardLivraison.setCardBackgroundColor(sel);
            layoutCarteForm.setVisibility(View.VISIBLE);
            btnPay.setEnabled(false); // désactivé jusqu'au clientSecret
            btnPay.setText("→  Payer " + String.format("%.3f TND", total) + " avec Stripe");
            fetchPaymentIntent();

        } else {
            // Espèces
            cardCash.setCardBackgroundColor(sel);
            layoutCashInfo.setVisibility(View.VISIBLE);
            btnPay.setText("→  Confirmer — Payer à la livraison");
            btnPay.setEnabled(true);
        }
    }

    // ── Récupérer le PaymentIntent ────────────────────────────────────────────
    private void fetchPaymentIntent() {
        if (fetchingSecret) return;
        fetchingSecret = true;
        clientSecret   = "";

        final int amountCents = Math.max(50, (int) Math.round(total * 100));
        Log.d(TAG, "Fetching PaymentIntent — cents: " + amountCents);

        executor.execute(() -> {
            try {
                pingServer();

                JSONObject body = new JSONObject();
                body.put("amount", amountCents);

                HttpURLConnection conn = (HttpURLConnection)
                        new URL(SERVER_URL + "/stripe/create-payment-intent").openConnection();
                conn.setDoOutput(true);
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setConnectTimeout(20000);
                conn.setReadTimeout(20000);

                OutputStream os = conn.getOutputStream();
                os.write(body.toString().getBytes("UTF-8"));
                os.close();

                int code = conn.getResponseCode();
                BufferedReader br = new BufferedReader(new InputStreamReader(
                        code >= 200 && code < 300
                                ? conn.getInputStream() : conn.getErrorStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) sb.append(line);
                br.close();
                conn.disconnect();

                String responseStr = sb.toString();
                Log.d(TAG, "Stripe response: " + responseStr);
                JSONObject response = new JSONObject(responseStr);

                if (response.has("error")) {
                    String err = response.getString("error");
                    Log.e(TAG, "Erreur serveur: " + err);
                    runOnUiThread(() -> {
                        Toast.makeText(this, "Erreur Stripe : " + err, Toast.LENGTH_LONG).show();
                        btnPay.setEnabled(false);
                        btnPay.setText("Erreur — réessayez");
                    });
                    fetchingSecret = false;
                    return;
                }

                clientSecret = response.getString("clientSecret");
                Log.d(TAG, "✅ clientSecret reçu");

                runOnUiThread(() -> {
                    btnPay.setEnabled(true);
                    btnPay.setText("→  Payer " + String.format("%.3f TND", total) + " avec Stripe");
                    Toast.makeText(this, "✅ Stripe prêt", Toast.LENGTH_SHORT).show();
                });

            } catch (Exception e) {
                Log.e(TAG, "Exception: " + e.getMessage());
                runOnUiThread(() -> {
                    Toast.makeText(this,
                            "Connexion échouée : " + e.getMessage(), Toast.LENGTH_LONG).show();
                    btnPay.setEnabled(false);
                });
            }
            fetchingSecret = false;
        });
    }

    private void pingServer() {
        try {
            HttpURLConnection c = (HttpURLConnection)
                    new URL(SERVER_URL + "/ping").openConnection();
            c.setConnectTimeout(15000);
            c.setReadTimeout(15000);
            c.getResponseCode();
            c.disconnect();
        } catch (Exception ignored) {}
    }

    // ── Traitement ────────────────────────────────────────────────────────────
    private void processPayment() {
        if (selectedMethod.contains("Livraison")) {
            // ✅ Ouvre la PaymentSheet Stripe officielle
            if (clientSecret.isEmpty()) {
                Toast.makeText(this, "Chargement en cours...", Toast.LENGTH_SHORT).show();
                if (!fetchingSecret) fetchPaymentIntent();
                return;
            }
            PaymentSheet.Configuration config =
                    new PaymentSheet.Configuration.Builder("BennaGO").build();
            paymentSheet.presentWithPaymentIntent(clientSecret, config);

        } else {
            // Espèces → simulation
            simulatePayment(new String[]{
                    "Enregistrement de la commande...",
                    "Préparation du bon de livraison...",
                    "✅ Commande confirmée !"
            }, 700);
        }
    }

    // ── Résultat Stripe ───────────────────────────────────────────────────────
    private void onPaymentSheetResult(PaymentSheetResult result) {
        if (result instanceof PaymentSheetResult.Completed) {
            Log.d(TAG, "✅ Stripe paiement réussi");
            showProcessing("✅ Paiement confirmé !");
            new Handler().postDelayed(this::placeOrder, 800);
        } else if (result instanceof PaymentSheetResult.Failed) {
            String msg = ((PaymentSheetResult.Failed) result).getError().getMessage();
            Toast.makeText(this, "Paiement échoué : " + msg, Toast.LENGTH_LONG).show();
        } else if (result instanceof PaymentSheetResult.Canceled) {
            Toast.makeText(this, "Paiement annulé", Toast.LENGTH_SHORT).show();
        }
    }

    private void showProcessing(String msg) {
        if (layoutMethods   != null) layoutMethods.setVisibility(View.GONE);
        if (layoutCarteForm != null) layoutCarteForm.setVisibility(View.GONE);
        if (layoutCashInfo  != null) layoutCashInfo.setVisibility(View.GONE);
        if (btnPay          != null) btnPay.setVisibility(View.GONE);
        if (layoutProcessing!= null) layoutProcessing.setVisibility(View.VISIBLE);
        if (progressBar     != null) progressBar.setVisibility(View.GONE);
        if (tvProcessingMsg != null) tvProcessingMsg.setText(msg);
    }

    private void simulatePayment(String[] messages, int delayMs) {
        if (layoutMethods   != null) layoutMethods.setVisibility(View.GONE);
        if (layoutCarteForm != null) layoutCarteForm.setVisibility(View.GONE);
        if (layoutCashInfo  != null) layoutCashInfo.setVisibility(View.GONE);
        if (btnPay          != null) btnPay.setVisibility(View.GONE);
        if (layoutProcessing!= null) layoutProcessing.setVisibility(View.VISIBLE);

        Handler h = new Handler();
        for (int i = 0; i < messages.length; i++) {
            final String msg = messages[i];
            h.postDelayed(() -> {
                if (tvProcessingMsg != null) tvProcessingMsg.setText(msg);
            }, (long) i * delayMs);
        }
        h.postDelayed(this::placeOrder, (long) messages.length * delayMs + 400);
    }

    // ── Création commande ─────────────────────────────────────────────────────
    private void placeOrder() {
        executor.execute(() -> {
            String finalNote = (note != null && !note.isEmpty())
                    ? note + " | Paiement : " + selectedMethod
                    : "Paiement : " + selectedMethod;

            Order order = new Order(
                    sessionManager.getUserId(),
                    sessionManager.getUserName(),
                    phone, address, finalNote,
                    total, "En attente",
                    System.currentTimeMillis());
            long orderId = db.orderDao().insertOrder(order);

            for (CartManager.CartItem item : cart.getItems())
                db.orderDao().insertOrderItem(new OrderItem(
                        (int) orderId, item.dish.getId(),
                        item.dish.getName(), item.dish.getPrice(), item.quantity));

            NotificationHelper.notifyAdminNewOrder(
                    (int) orderId, sessionManager.getUserName(), total);
            cart.clear();

            runOnUiThread(() -> {
                if (tvProcessingMsg != null) tvProcessingMsg.setText("🎉 Commande confirmée !");
                new Handler().postDelayed(() -> {
                    Toast.makeText(this,
                            "Commande #" + orderId + " passée ! ✅", Toast.LENGTH_LONG).show();
                    Intent i = new Intent(this, MainActivity.class);
                    i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(i);
                    finish();
                }, 1200);
            });
        });
    }

    // ── TextWatchers ──────────────────────────────────────────────────────────
    private void setupCardNumberWatcher() {
        if (etCardNumber == null) return;
        etCardNumber.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void afterTextChanged(Editable s) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (formattingCard) return;
                formattingCard = true;
                String raw = s.toString().replaceAll("[^0-9]", "");
                if (raw.length() > 16) raw = raw.substring(0, 16);
                StringBuilder fmt = new StringBuilder();
                for (int i = 0; i < raw.length(); i++) {
                    if (i > 0 && i % 4 == 0) fmt.append(' ');
                    fmt.append(raw.charAt(i));
                }
                etCardNumber.setText(fmt.toString());
                etCardNumber.setSelection(fmt.length());
                if (tvCardPreviewNumber != null) tvCardPreviewNumber.setText(buildPreviewNumber(raw));
                detectCardType(raw);
                formattingCard = false;
            }
        });
    }

    private void setupExpiryWatcher() {
        if (etExpiry == null) return;
        etExpiry.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void afterTextChanged(Editable s) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (formattingExpiry) return;
                formattingExpiry = true;
                String raw = s.toString().replaceAll("[^0-9]", "");
                if (raw.length() > 4) raw = raw.substring(0, 4);
                String fmt = raw.length() > 2
                        ? raw.substring(0, 2) + "/" + raw.substring(2) : raw;
                etExpiry.setText(fmt);
                etExpiry.setSelection(fmt.length());
                if (tvCardPreviewExpiry != null)
                    tvCardPreviewExpiry.setText(fmt.isEmpty() ? "MM/AA" : fmt);
                formattingExpiry = false;
            }
        });
    }

    private void setupHolderWatcher() {
        if (etCardHolder == null) return;
        etCardHolder.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void afterTextChanged(Editable s) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (tvCardPreviewHolder != null)
                    tvCardPreviewHolder.setText(
                            s.toString().isEmpty() ? "TITULAIRE" : s.toString().toUpperCase());
            }
        });
    }

    private String buildPreviewNumber(String digits) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 16; i++) {
            if (i > 0 && i % 4 == 0) sb.append("  ");
            sb.append(i < digits.length() ? digits.charAt(i) : '•');
        }
        return sb.toString();
    }

    private void detectCardType(String digits) {
        if (tvCardType == null || digits.isEmpty()) { if (tvCardType != null) tvCardType.setText(""); return; }
        if (digits.startsWith("4"))                              tvCardType.setText("VISA");
        else if (digits.startsWith("5") || digits.startsWith("2")) tvCardType.setText("MASTERCARD");
        else if (digits.startsWith("34") || digits.startsWith("37")) tvCardType.setText("AMEX");
        else                                                      tvCardType.setText("CARTE");
    }

    private void fetchFcmToken() {
        FirebaseMessaging.getInstance().getToken().addOnSuccessListener(token -> {
            if (token != null) {
                int userId = sessionManager.getUserId();
                if (userId != -1)
                    executor.execute(() -> db.userDao().updateFcmToken(userId, token));
            }
        });
    }
}