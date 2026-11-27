package com.valhallaschool.app;

import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;

import static com.valhallaschool.app.Ui.*;

/**
 * Ronda de práctica (modo Científico): 10 ejercicios del nivel, 4 opciones
 * con etiquetas [Ne rol] etc. Acierto +10 XP; fallo sin castigo: la
 * pregunta se reencola al final de la ronda. Sin corazones, sin límites.
 * Al responder se muestra la justificación. Completar el nivel (≥7/10
 * a la primera) desbloquea el siguiente. Los personajes son anónimos:
 * el ejercicio solo trae la frase, el comportamiento o la acción.
 */
final class PracticeScreen extends MainActivity.Screen {
    private static final int ASK = 1, FEEDBACK = 2, END = 3;
    private static final String[] PRAISE = {"¡Bien!", "¡Correcto!", "¡Muy bien!", "¡Genial!", "¡Exacto!"};

    private final ArrayDeque<PracticeData.Ex> queue = new ArrayDeque<>();
    private final int level, total;
    private final HashSet<String> missed = new HashSet<>(), graded = new HashSet<>(), tried = new HashSet<>();
    private final long t0 = System.currentTimeMillis();
    private int done, combo, firstOk, answered, sel = -1, phase;
    private PracticeData.Ex item;
    private Companion companion;
    private String[] labels;
    private int answerIdx;
    private TextView[] opts;

    private final Views.Bar bar;
    private final TextView comboTx, fbTitle, fbText, btn;
    private final LinearLayout main, foot;
    private final View divider;
    private final ScrollView sc;

    static void start(MainActivity a, int level) {
        List<PracticeData.Ex> pool = PracticeData.ofLevel(level);
        if (pool.isEmpty()) { a.toast("Aún no hay ejercicios en este nivel", false); return; }
        a.push(new PracticeScreen(a, level, new ArrayList<>(pool.subList(0, Math.min(10, pool.size())))));
    }

    private PracticeScreen(MainActivity a, int level, List<PracticeData.Ex> round) {
        super(a);
        up = true;
        this.level = level;
        queue.addAll(round);
        total = queue.size();

        LinearLayout col = col(a);
        LinearLayout top = row(a);
        top.setPadding(dp(6), dp(8), dp(14), dp(6));
        top.addView(iconBtn(a, R.drawable.ic_close, MUTED, x -> close()));
        bar = new Views.Bar(a);
        LinearLayout.LayoutParams bl = lpm(0, dp(14), 6, 0, 10, 0);
        bl.weight = 1;
        top.addView(bar, bl);
        comboTx = tv(a, "", 14, ACCENT, BLACK);
        comboTx.setGravity(Gravity.END);
        top.addView(comboTx, lp(dp(52), W));
        col.addView(top, lp(M, W));

        sc = new ScrollView(a);
        main = col(a);
        main.setPadding(dp(18), dp(12), dp(18), dp(20));
        sc.addView(main);
        col.addView(sc, lp(M, 0, 1));

        divider = new View(a);
        divider.setBackgroundColor(LINE);
        col.addView(divider, lp(M, dp(2)));
        foot = col(a);
        foot.setPadding(dp(18), dp(16), dp(18), dp(18));
        fbTitle = tv(a, "", 20, GREEN, BLACK);
        fbText = tv(a, "", 15, TEXT, REG);
        fbText.setLineSpacing(0, 1.25f);
        foot.addView(fbTitle);
        foot.addView(fbText, lpm(M, W, 0, 4, 0, 0));
        btn = Ui.btn(a, "Continuar", B_GREEN, x -> onBtn());
        foot.addView(btn, lpm(M, W, 0, 14, 0, 0));
        col.addView(foot, lp(M, W));
        v = col;
        next();
    }

    /* ---------------- flow ---------------- */
    private void close() {
        if (phase == END) { onBtn(); return; }
        a.ask("¿Salir de la práctica?", "Pierdes el XP de esta ronda.", "Salir", a::back, true);
    }

    @Override boolean onBack() { close(); return true; }

    private void updateBar() {
        bar.set(done / (float) total, GREEN);
        comboTx.setText(combo >= 2 ? combo + "×🔥" : "");
    }

    private void setFoot(int state) {
        foot.setBackgroundColor(state == 1 ? mix(GREEN, BG, 0.16f) : state == 2 ? mix(RED, BG, 0.14f) : BG);
        divider.setVisibility(state == 0 ? View.VISIBLE : View.INVISIBLE);
        fbTitle.setVisibility(state == 0 ? View.GONE : View.VISIBLE);
        fbText.setVisibility(state == 2 ? View.VISIBLE : View.GONE);
    }

    private void setBtn(String label, int style) {
        btn.setText(label.toUpperCase(Locale.ROOT));
        styleBtn(btn, style);
    }

    private void next() {
        updateBar();
        setFoot(0);
        sc.scrollTo(0, 0);
        main.removeAllViews();
        item = queue.poll();
        if (item == null) { finish(); return; }
        phase = ASK;
        sel = -1;

        // Acompañante inmersivo (v1.2.2): personaje GRANDE que anima en cada ejercicio.
        companion = new Companion(a);
        main.addView(companion.root(), lpm(M, W, 0, 0, 0, 4));
        companion.next();

        main.addView(tv(a, "¿Qué función en qué posición muestra esto?", 22, TEXT, BLACK), lpm(M, W, 0, 6, 0, 16));
        LinearLayout ctx = col(a);
        ctx.setBackground(round(SURF, 18, LINE, 2));
        ctx.setPadding(dp(16), dp(16), dp(16), dp(16));
        TextView tag = tv(a, "PERSONAJE ANÓNIMO · " + item.kindLabel(), 13, BLUE, BLACK);
        tag.setLetterSpacing(0.08f);
        ctx.addView(tag, lpm(M, W, 0, 0, 0, 8));
        TextView bd = tv(a, item.prompt, 17, TEXT, REG);
        bd.setLineSpacing(0, 1.3f);
        ctx.addView(bd);
        main.addView(ctx, lpm(M, W, 0, 0, 0, 18));

        // 4 opciones [Elemento posición], barajadas.
        ArrayList<String> all = new ArrayList<>();
        all.add(item.answerLabel());
        all.add(item.label(item.d1El, item.d1Pos));
        all.add(item.label(item.d2El, item.d2Pos));
        all.add(item.label(item.d3El, item.d3Pos));
        Collections.shuffle(all, Quiz.R);
        labels = all.toArray(new String[0]);
        answerIdx = all.indexOf(item.answerLabel());
        opts = new TextView[labels.length];
        for (int r = 0; r < 2; r++) {
            LinearLayout row = row(a);
            for (int c = 0; c < 2; c++) {
                int i = r * 2 + c;
                final int idx = i;
                TextView o = tv(a, labels[i], 18, TEXT, MEDIUM);
                o.setGravity(Gravity.CENTER);
                o.setPadding(dp(10), dp(16), dp(10), dp(18));
                optStyle(o, 0);
                o.setOnClickListener(x -> {
                    if (phase != ASK) return;
                    sel = idx;
                    for (int k = 0; k < opts.length; k++) optStyle(opts[k], k == idx ? 1 : 0);
                    setBtn("Comprobar", B_GREEN);
                });
                opts[i] = o;
                LinearLayout.LayoutParams p = lpm(0, W, c == 0 ? 0 : 6, 0, c == 0 ? 6 : 0, 10);
                p.weight = 1;
                row.addView(o, p);
            }
            main.addView(row, lp(M, W));
        }
        setBtn("Comprobar", B_OFF);
    }

    /** 0 idle, 1 selected, 2 correct, 3 wrong. */
    private static void optStyle(TextView o, int s) {
        int border = s == 1 ? BLUE : s == 2 ? GREEN : s == 3 ? RED : LINE;
        int fill = s == 1 ? mix(BLUE, SURF, 0.12f) : s == 2 ? mix(GREEN, SURF, 0.14f) : s == 3 ? mix(RED, SURF, 0.12f) : SURF;
        o.setBackground(raised(fill, border, 16, 3, border));
    }

    private void onBtn() {
        switch (phase) {
            case ASK: if (sel >= 0) check(); break;
            case FEEDBACK: next(); break;
            case END: a.back(); break;
            default: break;
        }
    }

    private void check() {
        Store S = Store.I;
        boolean ok = sel == answerIdx;
        if (companion != null) companion.react(ok);
        for (int k = 0; k < opts.length; k++) optStyle(opts[k], k == answerIdx ? 2 : (k == sel && !ok) ? 3 : 0);
        boolean firstTry = tried.add(item.id);
        if (firstTry) {
            if (graded.add(item.id)) {
                answered++;
                S.qa++;
                if (ok) { S.qc++; firstOk++; }
            }
        }
        S.recordPractice(ok, item.aEl, item.aPos, item.quadra);
        if (ok) {
            combo++;
            done++;
            Sfx.play(Sfx.OK);
            fbTitle.setText(PRAISE[Quiz.R.nextInt(PRAISE.length)]);
            fbTitle.setTextColor(GREEN);
            setFoot(1);
            setBtn("Continuar", B_GREEN);
        } else {
            combo = 0;
            missed.add(item.id);
            queue.add(item); // sin corazones: fallar solo reencola el ejercicio
            Sfx.play(Sfx.BAD);
            v.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
            fbTitle.setText("Casi");
            fbTitle.setTextColor(RED);
            setFoot(2);
            setBtn("Continuar", B_RED);
        }
        fbText.setText((ok ? "Es " + item.answerLabel() + ".\n\n" : "La correcta es " + item.answerLabel() + ".\n\n") + item.justification);
        phase = FEEDBACK;
        updateBar();
        S.save();
    }

    private void finish() {
        Store S = Store.I;
        phase = END;
        boolean levelDone = firstOk >= 7;
        boolean perfect = answered > 0 && missed.isEmpty();
        int xp = firstOk * Store.XP_Q;
        if (perfect) S.perfect++;
        long secs = (System.currentTimeMillis() - t0) / 1000;
        int acc = answered > 0 ? Math.round(firstOk * 100f / answered) : 100;

        LinearLayout fin = col(a);
        fin.setGravity(Gravity.CENTER_HORIZONTAL);
        fin.setPadding(0, dp(48), 0, 0);
        fin.addView(Avatars.view(a, 120), lpm(W, W, 0, 0, 0, 4));
        fin.addView(tv(a, perfect ? "💎" : levelDone ? "🏆" : "🎯", 84, TEXT, REG));
        fin.addView(tv(a, perfect ? "¡Perfecto!" : levelDone ? "¡Nivel " + level + " completado!" : "¡Ronda terminada!", 28, ACCENT, BLACK), lpm(W, W, 0, 10, 0, 0));
        TextView p = tv(a, levelDone
                ? (level < PracticeData.LEVELS
                        ? "Acertaste " + firstOk + " de 10 a la primera. Nivel " + (level + 1) + " desbloqueado."
                        : "Acertaste " + firstOk + " de 10 a la primera. ¡Dominaste todos los niveles!")
                : "Acertaste " + firstOk + " de 10 a la primera. Necesitas 7 para pasar de nivel.",
                15, MUTED, REG);
        p.setGravity(Gravity.CENTER);
        fin.addView(p, lpm(W, W, 0, 10, 0, 24));
        LinearLayout tiles = row(a);
        tiles.addView(tile("XP TOTAL", String.valueOf(xp), ACCENT), lp(0, W, 1));
        LinearLayout.LayoutParams p2 = lpm(0, W, 10, 0, 0, 0), p3 = lpm(0, W, 10, 0, 0, 0);
        p2.weight = 1;
        p3.weight = 1;
        tiles.addView(tile("PRECISIÓN", acc + "%", GREEN), p2);
        tiles.addView(tile("TIEMPO", (secs / 60) + ":" + String.format(Locale.ROOT, "%02d", secs % 60), BLUE), p3);
        fin.addView(tiles, lp(M, W));
        main.addView(fin, lp(M, W));

        setFoot(0);
        setBtn("Continuar", B_GREEN);
        updateBar();
        Sfx.play(Sfx.WIN);
        a.confetti();
        if (levelDone) {
            S.completePracticeLevel(level);
            a.celebrate("🏆", "¡Nivel " + level + " completado!",
                    level < PracticeData.LEVELS
                            ? "«" + PracticeData.LEVEL_NAMES[level - 1] + "» dominado. El Nivel " + (level + 1) + " ya está abierto."
                            : "«" + PracticeData.LEVEL_NAMES[level - 1] + "» dominado. ¡Completaste todos los niveles de práctica!");
        }
        if (xp > 0) S.addXP(xp, true); else S.save();
        if (xp > 0) a.toast("+" + xp + " XP", true);
        finishPractice(a, level, levelDone);
    }

    private LinearLayout tile(String label, String value, int color) {
        LinearLayout t = col(a);
        t.setBackground(round(SURF, 16, color, 2));
        TextView l = tv(a, label, 11, 0xff111111, BLACK);
        l.setGravity(Gravity.CENTER);
        l.setLetterSpacing(0.06f);
        l.setPadding(0, dp(5), 0, dp(5));
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        float r = dp(14);
        g.setCornerRadii(new float[]{r, r, r, r, 0, 0, 0, 0});
        l.setBackground(g);
        t.addView(l, lp(M, W));
        TextView v = tv(a, value, 20, TEXT, BLACK);
        v.setGravity(Gravity.CENTER);
        v.setPadding(0, dp(12), 0, dp(12));
        t.addView(v, lp(M, W));
        return t;
    }

    /** Espejo en la nube (módulo 6 = práctica). Sin DDL. */
    private static void finishPractice(MainActivity a, int level, boolean completed) {
        Store S = Store.I;
        a.dirty();
        try {
            if (SupabaseAuth.INSTANCE.isSignedIn()) {
                GameProgressSync.INSTANCE.upsertProgress(6, level, S.todayXP(), S.streak(), 3, S.gems, completed,
                        (v, e) -> {});
                SupabaseAuth.Session sess = SupabaseAuth.INSTANCE.currentSession();
                String email = sess != null ? sess.getEmail() : null;
                String name = S.nickname != null && !S.nickname.trim().isEmpty() ? S.nickname.trim() : email;
                GameProgressSync.INSTANCE.submitScore(S.totalXP(), Store.level(S.totalXP()), name,
                        (v, e) -> {});
            }
        } catch (Exception ignored) { /* la nube es espejo, no bloquea */ }
    }
}
