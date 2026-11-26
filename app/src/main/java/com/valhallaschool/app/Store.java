package com.valhallaschool.app;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * All user progress: read history, saves, spaced-repetition boxes, XP per day, streaks, lessons, achievements.
 * Persisted as JSON in filesDir (same shape as the web version's backup file). Days are local epoch-day numbers.
 */
final class Store {
    interface Events {
        void toast(String msg, boolean xp);
        void celebrate(String ico, String title, String text);
        void changed();
    }

    static final int XP_READ = 5, XP_Q = 10, XP_LESSON = 20, XP_PERFECT = 10;
    static final int[] SRS_DAYS = {1, 3, 7, 16, 35, 80};
    static final int[] GOALS = {20, 50, 100, 200};
    static final String[] GOAL_NAMES = {"Tranquilo", "Regular", "En serio", "Intenso"};

    static Store I;

    final HashMap<Integer, Long> read = new HashMap<>(), saved = new HashMap<>();
    final HashMap<Integer, int[]> srs = new HashMap<>();
    final HashMap<Integer, Integer> xp = new HashMap<>();
    final HashSet<Integer> met = new HashSet<>();
    final HashMap<String, Long> lessons = new HashMap<>(), ach = new HashMap<>();
    int qa, qc, perfect, goal = 50, best, cat = -1;
    long seed;
    boolean dark = true, sound = true;

    Events ev;
    private final File file;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private final Runnable saver = this::flush;

    static Store get(Context c) {
        if (I == null) I = new Store(c.getApplicationContext());
        return I;
    }

    private Store(Context c) {
        file = new File(c.getFilesDir(), "state.json");
        seed = new Random().nextLong();
        try {
            if (file.exists()) fromJson(new JSONObject(new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8)));
        } catch (Exception ignored) {
            // corrupt file: start fresh rather than crash
        }
    }

    /* ---------------- persistence ---------------- */
    void save() { main.removeCallbacks(saver); main.postDelayed(saver, 400); }

    void flush() {
        main.removeCallbacks(saver);
        final String json;
        try { json = toJson().toString(); } catch (JSONException e) { return; }
        io.execute(() -> {
            File tmp = new File(file.getPath() + ".tmp");
            try (FileOutputStream out = new FileOutputStream(tmp)) {
                out.write(json.getBytes(StandardCharsets.UTF_8));
                out.getFD().sync();
            } catch (Exception e) { return; }
            //noinspection ResultOfMethodCallIgnored
            tmp.renameTo(file);
        });
    }

    JSONObject toJson() throws JSONException {
        JSONObject o = new JSONObject();
        o.put("read", longs(read)).put("saved", longs(saved));
        JSONObject s = new JSONObject();
        for (Map.Entry<Integer, int[]> e : srs.entrySet()) s.put(String.valueOf(e.getKey()), new JSONArray().put(e.getValue()[0]).put(e.getValue()[1]));
        JSONObject x = new JSONObject();
        for (Map.Entry<Integer, Integer> e : xp.entrySet()) x.put(String.valueOf(e.getKey()), e.getValue());
        JSONObject m = new JSONObject();
        for (Integer d : met) m.put(String.valueOf(d), 1);
        JSONObject l = new JSONObject();
        for (Map.Entry<String, Long> e : lessons.entrySet()) l.put(e.getKey(), e.getValue());
        JSONObject a = new JSONObject();
        for (Map.Entry<String, Long> e : ach.entrySet()) a.put(e.getKey(), e.getValue());
        return o.put("srs", s).put("xp", x).put("met", m).put("lecciones", l).put("ach", a)
                .put("qa", qa).put("qc", qc).put("perfect", perfect).put("goal", goal).put("best", best)
                .put("cat", cat).put("seed", seed).put("theme", dark ? "dark" : "light").put("sound", sound ? 1 : 0);
    }

    private static JSONObject longs(HashMap<Integer, Long> map) throws JSONException {
        JSONObject o = new JSONObject();
        for (Map.Entry<Integer, Long> e : map.entrySet()) o.put(String.valueOf(e.getKey()), e.getValue());
        return o;
    }

    void fromJson(JSONObject o) {
        clear();
        readLongs(o.optJSONObject("read"), read);
        readLongs(o.optJSONObject("saved"), saved);
        JSONObject s = o.optJSONObject("srs");
        if (s != null) for (Iterator<String> it = s.keys(); it.hasNext(); ) {
            String k = it.next(); JSONArray v = s.optJSONArray(k);
            if (v != null && v.length() == 2) srs.put(Integer.parseInt(k), new int[]{v.optInt(0), v.optInt(1)});
        }
        JSONObject x = o.optJSONObject("xp");
        if (x != null) for (Iterator<String> it = x.keys(); it.hasNext(); ) { String k = it.next(); xp.put(Integer.parseInt(k), x.optInt(k)); }
        JSONObject m = o.optJSONObject("met");
        if (m != null) for (Iterator<String> it = m.keys(); it.hasNext(); ) met.add(Integer.parseInt(it.next()));
        readStrLongs(o.optJSONObject("lecciones"), lessons);
        readStrLongs(o.optJSONObject("ach"), ach);
        qa = o.optInt("qa"); qc = o.optInt("qc"); perfect = o.optInt("perfect");
        goal = o.optInt("goal", 50); best = o.optInt("best"); cat = o.optInt("cat", -1);
        seed = o.optLong("seed", seed);
        dark = !"light".equals(o.optString("theme", "dark"));
        sound = o.optInt("sound", 1) != 0;
        if (cat >= Data.catName.length) cat = -1;
    }

    private static void readLongs(JSONObject o, HashMap<Integer, Long> into) {
        if (o == null) return;
        for (Iterator<String> it = o.keys(); it.hasNext(); ) {
            String k = it.next();
            int id = Integer.parseInt(k);
            if (id >= 0 && id < Data.N) into.put(id, o.optLong(k));
        }
    }

    private static void readStrLongs(JSONObject o, HashMap<String, Long> into) {
        if (o == null) return;
        for (Iterator<String> it = o.keys(); it.hasNext(); ) { String k = it.next(); into.put(k, o.optLong(k)); }
    }

    void clear() {
        read.clear(); saved.clear(); srs.clear(); xp.clear(); met.clear(); lessons.clear(); ach.clear();
        qa = qc = perfect = best = 0; goal = 50; cat = -1;
    }

    /* ---------------- dates ---------------- */
    static int dayOf(long ms) { return (int) Math.floorDiv(ms + TimeZone.getDefault().getOffset(ms), 86_400_000L); }
    static int today() { return dayOf(System.currentTimeMillis()); }

    /* ---------------- derived stats ---------------- */
    boolean isRead(int id) { return read.containsKey(id); }

    int bookRead(int b) {
        int n = 0;
        for (int i = b * Data.PER; i < b * Data.PER + Data.PER; i++) if (read.containsKey(i)) n++;
        return n;
    }

    int booksDone() {
        int n = 0;
        for (int b = 0; b < Data.NB; b++) if (bookRead(b) == Data.PER) n++;
        return n;
    }

    int totalXP() { int t = 0; for (int v : xp.values()) t += v; return t; }
    int xpOn(int day) { Integer v = xp.get(day); return v == null ? 0 : v; }
    int todayXP() { return xpOn(today()); }
    static int level(int xp) { return (int) Math.floor((Math.sqrt(1 + 8.0 * xp / 100) - 1) / 2) + 1; }
    static int levelBase(int l) { return 50 * (l - 1) * l; }

    int streak() {
        int d = today(), n = 0;
        if (!met.contains(d)) d--;
        while (met.contains(d)) { n++; d--; }
        return n;
    }

    List<Integer> due() {
        int t = today();
        ArrayList<Integer> out = new ArrayList<>();
        for (Map.Entry<Integer, int[]> e : srs.entrySet()) if (e.getValue()[1] <= t) out.add(e.getKey());
        Collections.sort(out, (a, b) -> Integer.compare(srs.get(a)[1], srs.get(b)[1]));
        return out;
    }

    boolean lessonDone(int b, int k) { return lessons.containsKey(b + "." + k); }

    /* ---------------- mutations ---------------- */
    void addXP(int n, boolean quiet) {
        int d = today();
        xp.put(d, xpOn(d) + n);
        if (!quiet && ev != null) ev.toast("+" + n + " XP", true);
        if (!met.contains(d) && xpOn(d) >= goal) {
            met.add(d);
            int st = streak();
            best = Math.max(best, st);
            if (ev != null) ev.celebrate("🔥", "¡Meta diaria lograda!", "Ganaste " + xpOn(d) + " XP hoy. Racha: " + st + (st == 1 ? " día." : " días."));
        }
        save();
        checkAch();
        if (ev != null) ev.changed();
    }

    /** @return true if this idea was not read before. */
    boolean markRead(int id, boolean quiet) {
        if (read.containsKey(id)) return false;
        read.put(id, System.currentTimeMillis());
        if (!srs.containsKey(id)) srs.put(id, new int[]{0, today() + 1});
        addXP(XP_READ, quiet);
        int b = Data.book(id);
        if (bookRead(b) == Data.PER && ev != null)
            ev.celebrate("📚", "¡Lección terminada!", "Leíste todas las ideas de “" + Data.bTitle[b] + "”. Haz el quiz en Aprender para dominarla.");
        return true;
    }

    void srsGrade(int id, boolean ok) {
        if (!read.containsKey(id)) return;
        int[] cur = srs.get(id);
        int box = ok ? Math.min((cur == null ? 0 : cur[0]) + 1, SRS_DAYS.length - 1) : 0;
        srs.put(id, new int[]{box, today() + SRS_DAYS[box]});
    }

    boolean toggleSave(int id) {
        boolean on;
        if (saved.containsKey(id)) { saved.remove(id); on = false; } else { saved.put(id, System.currentTimeMillis()); on = true; }
        save();
        checkAch();
        return on;
    }

    void setGoal(int g) {
        goal = g;
        int d = today();
        if (xpOn(d) >= goal && !met.contains(d)) { met.add(d); best = Math.max(best, streak()); }
        save();
    }

    /* ---------------- achievements ---------------- */
    static final String[][] ACH = {
            {"first", "🌱", "Primera idea", "Lee tu primera idea"},
            {"r25", "🔎", "Curioso", "Lee 25 ideas"},
            {"r100", "🐛", "Ratón de biblioteca", "Lee 100 ideas"},
            {"r500", "🎓", "Erudito", "Lee 500 ideas"},
            {"r1200", "🏛️", "Biblioteca", "Lee todas las ideas"},
            {"b1", "📗", "Finalista", "Termina una lección"},
            {"b10", "📚", "Estante", "Termina 10 lecciones"},
            {"s3", "🔥", "En llamas", "Racha de 3 días"},
            {"s7", "⚡", "Guerrero semanal", "Racha de 7 días"},
            {"s30", "🏆", "Imparable", "Racha de 30 días"},
            {"l1", "⭐", "Primera lección", "Completa una lección"},
            {"l25", "🌟", "Dedicado", "Completa 25 lecciones"},
            {"perfect", "💎", "Impecable", "Quiz perfecto"},
            {"q100", "🧠", "Maestro del quiz", "100 respuestas correctas"},
            {"save10", "🔖", "Coleccionista", "Guarda 10 ideas"},
            {"xp1k", "💰", "Cazador de XP", "Gana 1,000 XP"},
    };

    private boolean achOk(int i) {
        switch (i) {
            case 0: return read.size() >= 1;
            case 1: return read.size() >= 25;
            case 2: return read.size() >= 100;
            case 3: return read.size() >= 500;
            case 4: return read.size() >= Data.N;
            case 5: return booksDone() >= 1;
            case 6: return booksDone() >= 10;
            case 7: return best >= 3;
            case 8: return best >= 7;
            case 9: return best >= 30;
            case 10: return lessons.size() >= 1;
            case 11: return lessons.size() >= 25;
            case 12: return perfect > 0;
            case 13: return qc >= 100;
            case 14: return saved.size() >= 10;
            case 15: return totalXP() >= 1000;
            default: return false;
        }
    }

    void checkAch() {
        for (int i = 0; i < ACH.length; i++) {
            if (!ach.containsKey(ACH[i][0]) && achOk(i)) {
                ach.put(ACH[i][0], System.currentTimeMillis());
                if (ev != null) ev.toast(ACH[i][1] + " Logro: " + ACH[i][2], false);
                save();
            }
        }
    }
}
