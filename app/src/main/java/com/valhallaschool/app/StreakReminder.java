package com.valhallaschool.app;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

/**
 * Recordatorio diario LOCAL de la racha (v1.2.6): cada día a las 20:00 hora local,
 * solo si la meta diaria todavía no está cumplida, avisa con una notificación
 * en el tono sarcástico de la app. Sin servidor, sin FCM: WorkManager.
 * Se apaga/enciende con el interruptor de Perfil → Ajustes → «Recordatorio de racha».
 */
final class StreakReminder {
    static final String CHANNEL_ID = "streak_reminder";
    static final String WORK_NAME = "streak_reminder";
    private static final int NOTIF_ID = 1001;

    private StreakReminder() {}

    /** Canal propio de notificaciones («Recordatorio de racha»). */
    static void createChannel(Context c) {
        if (Build.VERSION.SDK_INT < 26) return;
        NotificationManager nm = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null || nm.getNotificationChannel(CHANNEL_ID) != null) return;
        NotificationChannel ch = new NotificationChannel(CHANNEL_ID, "Recordatorio de racha",
                NotificationManager.IMPORTANCE_DEFAULT);
        ch.setDescription("Te avisa a las 20:00 si tu meta diaria sigue sin cumplirse.");
        nm.createNotificationChannel(ch);
    }

    /** Programa el aviso diario (~20:00, con una ventana de 15 min). */
    static void schedule(Context c) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime next = now.withHour(20).withMinute(0).withSecond(0).withNano(0);
        if (!next.isAfter(now)) next = next.plusDays(1);
        long delayMin = Math.max(1, Duration.between(now, next).toMinutes());
        PeriodicWorkRequest req = new PeriodicWorkRequest.Builder(
                StreakReminderWorker.class, 24, TimeUnit.HOURS, 15, TimeUnit.MINUTES)
                .setInitialDelay(delayMin, TimeUnit.MINUTES)
                .build();
        WorkManager.getInstance(c).enqueueUniquePeriodicWork(
                WORK_NAME, ExistingPeriodicWorkPolicy.UPDATE, req);
    }

    /** Cancela el aviso diario (al apagar el interruptor). */
    static void cancel(Context c) {
        WorkManager.getInstance(c).cancelUniqueWork(WORK_NAME);
    }

    /** ¿La meta diaria ya está cumplida hoy? */
    static boolean goalMetToday(Store s) {
        int today = Store.today();
        return s.met.contains(today) || s.xpOn(today) >= s.goal;
    }

    /** Muestra la notificación (tocarla abre la app). Silenciosa si no hay permiso. */
    static void notify(Context c, String text) {
        createChannel(c);
        if (Build.VERSION.SDK_INT >= 33
                && ContextCompat.checkSelfPermission(c, android.Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) return;
        Intent i = new Intent(c, MainActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pi = PendingIntent.getActivity(c, 0, i,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        try {
            NotificationManagerCompat.from(c).notify(NOTIF_ID, new NotificationCompat.Builder(c, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_trophy)
                    .setContentTitle("Valhalla School")
                    .setContentText(text)
                    .setStyle(new NotificationCompat.BigTextStyle().bigText(text))
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setContentIntent(pi)
                    .setAutoCancel(true)
                    .build());
        } catch (Exception ignored) {
            // Sin permiso o canal bloqueado: no se muestra nada, no se rompe nada.
        }
    }
}
