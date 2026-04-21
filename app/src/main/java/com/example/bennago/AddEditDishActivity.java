package com.example.bennago;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.bennago.db.AppDatabase;
import com.example.bennago.entity.Dish;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AddEditDishActivity extends AppCompatActivity {

    private static final int PICK_IMAGE = 200;

    private static final String[] CATEGORIES = {
            "Burger", "Pizza", "Sandwich", "Salade",
            "Dessert", "Boisson", "Accompagnement", "Autre"
    };

    // ── Vues ─────────────────────────────────────────────────────────────────
    private ImageView            ivPreview;
    private TextInputEditText    etName, etPrice, etDescription;
    private TextInputLayout      tilName, tilPrice, tilCategory;
    private AutoCompleteTextView acCategory;
    private Switch               switchActive;
    private MaterialButton       btnSave;
    private TextView             tvTitle;

    // ── État ─────────────────────────────────────────────────────────────────
    // ✅ FIX : on stocke le chemin INTERNE (permanent) pas l'URI galerie (temporaire)
    private String  savedImagePath = ""; // chemin dans filesDir de l'app
    private boolean isEditMode     = false;
    private int     dishId         = -1;

    private AppDatabase           db;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit_dish);
        db = AppDatabase.getInstance(this);

        initViews();
        setupCategoryDropdown();
        checkEditMode();
    }

    private void initViews() {
        ImageButton btnBack = findViewById(R.id.btn_back);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        tvTitle       = findViewById(R.id.tv_form_title);
        ivPreview     = findViewById(R.id.iv_image_preview);
        etName        = findViewById(R.id.et_dish_name);
        etPrice       = findViewById(R.id.et_dish_price);
        etDescription = findViewById(R.id.et_dish_description);
        acCategory    = findViewById(R.id.ac_dish_category);
        switchActive  = findViewById(R.id.switch_dish_active);
        btnSave       = findViewById(R.id.btn_save_dish);
        tilName       = findViewById(R.id.til_dish_name);
        tilPrice      = findViewById(R.id.til_dish_price);
        tilCategory   = findViewById(R.id.til_dish_category);

        // Clic sur l'image → ouvre la galerie
        ivPreview.setOnClickListener(v -> pickImage());
        btnSave.setOnClickListener(v -> validateAndSave());
    }

    private void setupCategoryDropdown() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_dropdown_item_1line, CATEGORIES);
        acCategory.setAdapter(adapter);
    }

    // ── Mode Ajout ou Modification ────────────────────────────────────────────
    private void checkEditMode() {
        Intent intent = getIntent();
        dishId     = intent.getIntExtra("dish_id", -1);
        isEditMode = (dishId != -1);

        if (isEditMode) {
            tvTitle.setText("Modifier le plat");
            btnSave.setText("→  Enregistrer les modifications");

            etName.setText(intent.getStringExtra("dish_name"));
            // ✅ FIX prix : on reçoit un double, on l'affiche sans notation scientifique
            double price = intent.getDoubleExtra("dish_price", 0);
            etPrice.setText(String.format("%.3f", price));
            etDescription.setText(intent.getStringExtra("dish_description"));
            acCategory.setText(intent.getStringExtra("dish_category"), false);
            switchActive.setChecked(intent.getBooleanExtra("dish_active", true));

            // ✅ FIX image : on charge depuis le chemin interne permanent
            savedImagePath = intent.getStringExtra("dish_image");
            loadImageFromPath(savedImagePath);

        } else {
            tvTitle.setText("Ajouter un plat");
            btnSave.setText("→  Ajouter au menu");
            switchActive.setChecked(true);
        }
    }

    // ── Chargement image depuis chemin interne ────────────────────────────────
    private void loadImageFromPath(String path) {
        if (path == null || path.isEmpty()) return;
        try {
            File f = new File(path);
            if (f.exists()) {
                Bitmap bmp = BitmapFactory.decodeFile(path);
                ivPreview.setImageBitmap(bmp);
            }
        } catch (Exception e) {
            ivPreview.setImageResource(android.R.drawable.ic_menu_gallery);
        }
    }

    // ── Sélection image depuis galerie ────────────────────────────────────────
    private void pickImage() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");
        startActivityForResult(intent, PICK_IMAGE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE && resultCode == Activity.RESULT_OK && data != null) {
            Uri uri = data.getData();
            if (uri != null) {
                // ✅ FIX : copie l'image dans le stockage INTERNE de l'app (permanent)
                String copiedPath = copyImageToInternal(uri);
                if (copiedPath != null) {
                    savedImagePath = copiedPath;
                    ivPreview.setImageBitmap(BitmapFactory.decodeFile(copiedPath));
                } else {
                    Toast.makeText(this, "Erreur lors du chargement de l'image", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    // ── Copie l'image URI → fichier interne permanent ─────────────────────────
    private String copyImageToInternal(Uri uri) {
        try {
            // Dossier images de l'app : /data/data/com.example.bennago/files/dish_images/
            File dir = new File(getFilesDir(), "dish_images");
            if (!dir.exists()) dir.mkdirs();

            // Nom unique pour éviter les conflits
            String filename  = "dish_" + UUID.randomUUID().toString() + ".jpg";
            File   destFile  = new File(dir, filename);

            // Copie flux par flux
            InputStream  in  = getContentResolver().openInputStream(uri);
            OutputStream out = new FileOutputStream(destFile);
            byte[] buffer = new byte[4096];
            int    len;
            while ((len = in.read(buffer)) != -1) out.write(buffer, 0, len);
            in.close();
            out.close();

            return destFile.getAbsolutePath(); // chemin permanent ✅

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // ── Validation + Sauvegarde ───────────────────────────────────────────────
    private void validateAndSave() {
        String name     = getText(etName);
        String priceStr = getText(etPrice);
        String category = acCategory.getText().toString().trim();
        String desc     = getText(etDescription);
        boolean active  = switchActive.isChecked();

        tilName.setError(null);
        tilPrice.setError(null);
        tilCategory.setError(null);

        boolean valid = true;
        if (TextUtils.isEmpty(name))     { tilName.setError("Nom requis");           valid = false; }
        if (TextUtils.isEmpty(priceStr)) { tilPrice.setError("Prix requis");          valid = false; }
        if (TextUtils.isEmpty(category)) { tilCategory.setError("Catégorie requise"); valid = false; }

        double price = 0;
        if (valid) {
            try {
                price = Double.parseDouble(priceStr.replace(",", "."));
                if (price <= 0) { tilPrice.setError("Prix invalide"); valid = false; }
            } catch (NumberFormatException e) {
                tilPrice.setError("Prix invalide");
                valid = false;
            }
        }

        if (!valid) return;

        final double finalPrice = price;
        setLoading(true);

        executor.execute(() -> {
            if (isEditMode) {
                // ── Modification ──────────────────────────────────────────────
                Dish dish = new Dish(name, finalPrice, savedImagePath,
                        category, desc, active);
                dish.setId(dishId);
                db.dishDao().updateDish(dish);
            } else {
                // ── Ajout ─────────────────────────────────────────────────────
                Dish dish = new Dish(name, finalPrice, savedImagePath,
                        category, desc, active);
                db.dishDao().insertDish(dish);
            }

            runOnUiThread(() -> {
                setLoading(false);
                Toast.makeText(this,
                        isEditMode ? "Plat modifié ✅" : "Plat ajouté au menu ✅",
                        Toast.LENGTH_SHORT).show();
                setResult(RESULT_OK);
                finish();
            });
        });
    }

    private void setLoading(boolean loading) {
        btnSave.setEnabled(!loading);
        btnSave.setText(loading ? "Enregistrement..."
                : (isEditMode ? "→  Enregistrer les modifications" : "→  Ajouter au menu"));
    }

    private String getText(TextInputEditText et) {
        return et != null && et.getText() != null ? et.getText().toString().trim() : "";
    }
}