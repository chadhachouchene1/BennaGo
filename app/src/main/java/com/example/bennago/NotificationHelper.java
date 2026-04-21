package com.example.bennago;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Gère toutes les notifications de BennaGO :
 *  - createClientChannel()          → créer le canal local (MainActivity.onCreate)
 *  - notifyClientOrderConfirmed()   → notification locale immédiate au client
 *  - notifyAdminNewOrder()          → serveur Render → FCM topic "admin"
 *  - notifyClientStatusChanged()    → serveur Render → FCM token client
 */
public class NotificationHelper {

    // ── Serveur Render ────────────────────────────────────────────────────────
    private static final String SERVER_URL = "https://bennago-notif-server.onrender.com";

    // ── Canal notification locale client ─────────────────────────────────────
    private static final String CHANNEL_CLIENT_ID = "channel_client_orders";

    private static final String TAG  = "NotifHelper";
    private static final ExecutorService exec = Executors.newSingleThreadExecutor();

    // ─────────────────────────────────────────────────────────────────────────
    // 1. Créer le canal de notification client (Android 8+)
    //    → Appeler UNE SEULE FOIS dans MainActivity.onCreate()
    // ─────────────────────────────────────────────────────────────────────────
    public static void createClientChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_CLIENT_ID,
                    "Mes commandes",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Confirmation et suivi de vos commandes");
            channel.enableVibration(true);
            NotificationManager manager =
                    (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (manager != null) manager.createNotificationChannel(channel);
            Log.d(TAG, "Canal notifications client créé ✅");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 2. Notification locale immédiate au client après confirmation commande
    //    → Appeler dans CheckoutActivity après insertion en base
    // ─────────────────────────────────────────────────────────────────────────
    public static void notifyClientOrderConfirmed(Context context, int orderId, double total) {
        // Vérification permission Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ActivityCompat.checkSelfPermission(context,
                        Manifest.permission.POST_NOTIFICATIONS)
                        != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "Permission POST_NOTIFICATIONS non accordée — notif locale ignorée");
            return;
        }

        // Intent → ouvre MyOrdersActivity au clic sur la notification
        Intent intent = new Intent(context, MyOrdersActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                orderId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Notification notification = new NotificationCompat.Builder(context, CHANNEL_CLIENT_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("✅ Commande confirmée !")
                .setContentText(String.format(
                        "Commande #%d — %.3f TND bien reçue 🍽️", orderId, total))
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText(String.format(
                                "Votre commande #%d de %.3f TND a bien été enregistrée.\n" +
                                        "Nous la préparons dès maintenant !\n" +
                                        "Appuyez pour suivre l'état de votre commande.",
                                orderId, total)))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .build();

        NotificationManagerCompat.from(context).notify(orderId, notification);
        Log.d(TAG, "✅ Notif locale client envoyée — commande #" + orderId);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 3. Nouvelle commande → notifie l'admin via serveur Render
    // ─────────────────────────────────────────────────────────────────────────
    public static void notifyAdminNewOrder(int orderId, String clientName, double total) {
        exec.execute(() -> {
            try {
                // Réveiller le serveur Render s'il dort (plan gratuit)
                pingServer();

                JSONObject body = new JSONObject();
                body.put("orderId",    orderId);
                body.put("clientName", clientName);
                body.put("total",      total);

                String response = post(SERVER_URL + "/notify/admin", body.toString());
                Log.d(TAG, "✅ Notif admin — réponse : " + response);

            } catch (Exception e) {
                Log.e(TAG, "❌ notifyAdminNewOrder : " + e.getMessage());
            }
        });
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 4. Statut changé → notifie le client via serveur Render
    // ─────────────────────────────────────────────────────────────────────────
    public static void notifyClientStatusChanged(String clientToken, int orderId, String newStatus) {
        if (clientToken == null || clientToken.isEmpty()) {
            Log.w(TAG, "Token vide — notification ignorée");
            return;
        }
        exec.execute(() -> {
            try {
                // Réveiller le serveur Render si nécessaire
                pingServer();

                JSONObject body = new JSONObject();
                body.put("clientToken", clientToken);
                body.put("orderId",     orderId);
                body.put("newStatus",   newStatus);

                String response = post(SERVER_URL + "/notify/client", body.toString());
                Log.d(TAG, "✅ Notif client — réponse : " + response);

            } catch (Exception e) {
                Log.e(TAG, "❌ notifyClientStatusChanged : " + e.getMessage());
            }
        });
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Ping pour réveiller Render (cold start plan gratuit)
    // ─────────────────────────────────────────────────────────────────────────
    private static void pingServer() {
        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(SERVER_URL + "/ping").openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(15_000);
            conn.setReadTimeout(15_000);
            int code = conn.getResponseCode();
            conn.disconnect();
            Log.d(TAG, "Ping serveur → HTTP " + code);
        } catch (Exception e) {
            Log.w(TAG, "Ping échoué (ignoré) : " + e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // HTTP POST → retourne la réponse en String
    // ─────────────────────────────────────────────────────────────────────────
    private static String post(String urlStr, String json) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
        conn.setDoOutput(true);
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setConnectTimeout(20_000);
        conn.setReadTimeout(20_000);

        OutputStream os = conn.getOutputStream();
        os.write(json.getBytes("UTF-8"));
        os.close();

        int code = conn.getResponseCode();
        Log.d(TAG, "HTTP " + code + " ← POST " + urlStr);

        BufferedReader br = new BufferedReader(
                new InputStreamReader(
                        code >= 200 && code < 300
                                ? conn.getInputStream()
                                : conn.getErrorStream()));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) sb.append(line);
        br.close();
        conn.disconnect();

        String responseBody = sb.toString();
        if (code >= 400) {
            throw new Exception("Serveur a répondu HTTP " + code + " : " + responseBody);
        }
        return responseBody;
    }
}