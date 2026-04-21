package com.example.bennago;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class BennaGoFirebaseService extends FirebaseMessagingService {

    public static final String CHANNEL_ORDERS = "channel_orders";
    public static final String CHANNEL_STATUS = "channel_status";

    @Override
    public void onNewToken(String token) {
        super.onNewToken(token);
        getSharedPreferences("fcm_prefs", MODE_PRIVATE)
                .edit().putString("fcm_token", token).apply();
        android.util.Log.d("FCM_TOKEN", "Nouveau token : " + token);
        sendTokenToServer(token);
    }

    private void sendTokenToServer(String token) {
    }

    @Override
    public void onMessageReceived(RemoteMessage message) {
        super.onMessageReceived(message);

        String type  = message.getData().getOrDefault("type",  "");
        String title = message.getData().getOrDefault("title", "BennaGO 🍽️");
        String body  = message.getData().getOrDefault("body",  "");

        if (body.isEmpty() && message.getNotification() != null) {
            if (message.getNotification().getTitle() != null)
                title = message.getNotification().getTitle();
            if (message.getNotification().getBody() != null)
                body = message.getNotification().getBody();
        }

        Class<?> destination = "new_order".equals(type)
                ? ManageOrdersActivity.class : MyOrdersActivity.class;
        String channelId = "new_order".equals(type)
                ? CHANNEL_ORDERS : CHANNEL_STATUS;

        showNotification(this, title, body, channelId, destination);
    }

    public static void showNotification(Context ctx, String title, String body,
                                        String channelId, Class<?> destination) {
        NotificationManager nm =
                (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            nm.createNotificationChannel(new NotificationChannel(
                    CHANNEL_ORDERS, "Nouvelles commandes",
                    NotificationManager.IMPORTANCE_HIGH));
            nm.createNotificationChannel(new NotificationChannel(
                    CHANNEL_STATUS, "Statut commandes",
                    NotificationManager.IMPORTANCE_DEFAULT));
        }

        Intent intent = new Intent(ctx, destination);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pi = PendingIntent.getActivity(
                ctx, (int) System.currentTimeMillis(), intent,
                PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE);

        nm.notify((int) System.currentTimeMillis(),
                new NotificationCompat.Builder(ctx, channelId)
                        .setSmallIcon(android.R.drawable.ic_dialog_info)
                        .setContentTitle(title)
                        .setContentText(body)
                        .setStyle(new NotificationCompat.BigTextStyle().bigText(body))
                        .setAutoCancel(true)
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setContentIntent(pi)
                        .build());
    }
}