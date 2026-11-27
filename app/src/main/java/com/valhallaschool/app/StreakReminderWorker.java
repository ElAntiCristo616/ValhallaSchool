package com.valhallaschool.app;

import android.content.Context;

import androidx.work.Worker;
import androidx.work.WorkerParameters;

/**
 * Worker del recordatorio de racha (v1.2.6): a las ~20:00 revisa la meta del día
 * y avisa solo si todavía no está cumplida. Si ya se cumplió, silencio total.
 */
public final class StreakReminderWorker extends Worker {

    private static final String[] TEXTS = {
            "¡Tu racha se muere hoy si no juegas, %s! ¿Vas a dejar que se rompa, inútil?",
            "¿En serio vas a dejar morir tu racha hoy, %s? ¡Ni una lección, qué vergüenza!",
            "%s, tu racha lleva todo el día esperándote… y tú aquí, sin jugar. ¡Muévete, desastre!",
    };

    public StreakReminderWorker(Context context, WorkerParameters params) {
        super(context, params);
    }

    @Override
    public Result doWork() {
        Context c = getApplicationContext();
        Store s = Store.get(c);
        if (StreakReminder.goalMetToday(s)) return Result.success(); // meta cumplida: silencio
        String text = String.format(TEXTS[Math.floorMod(Store.today(), TEXTS.length)], s.displayName());
        StreakReminder.notify(c, text);
        return Result.success();
    }
}
