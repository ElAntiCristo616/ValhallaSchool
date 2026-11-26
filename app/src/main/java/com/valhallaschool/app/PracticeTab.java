package com.valhallaschool.app;

import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import static com.valhallaschool.app.Ui.*;

/**
 * Práctica: laboratorio de identificación de función+posición.
 * Segmentado: Científico (rondas por nivel, con XP y logros) | Loop
 * (infinito, sin puntos: llega con los avatares).
 */
final class PracticeTab extends MainActivity.Tab {
    private LinearLayout content;
    private int mode; // 0 = Científico, 1 = Loop

    PracticeTab(MainActivity a) { super(a); }

    @Override View build() {
        LinearLayout col = col(a);
        col.addView(pageTitle(a, "Práctica"));
        ScrollView sc = new ScrollView(a);
        content = col(a);
        content.setPadding(dp(16), 0, dp(16), dp(32));
        sc.addView(content);
        col.addView(sc, lp(M, 0, 1));
        return col;
    }

    @Override void refresh() {
        content.removeAllViews();
        content.addView(seg(a, new String[]{"Científico", "Loop"}, mode, i -> { mode = i; refresh(); }),
                lpm(M, W, 0, 4, 0, 16));
        if (mode == 0) scientific();
        else loopSoon();
    }

    private void scientific() {
        Store S = Store.I;
        TextView intro = tv(a, "Identifica qué función en qué posición muestra cada ejemplo. Los personajes son anónimos: solo ves la frase, el comportamiento o la acción.", 14, MUTED, REG);
        intro.setLineSpacing(0, 1.3f);
        content.addView(intro, lpm(M, W, 0, 0, 0, 16));

        LinearLayout stats = row(a);
        stats.addView(LearnTab.stat(a, "🎯", String.valueOf(S.pCorrect), "aciertos"), lp(0, W, 1));
        LinearLayout.LayoutParams p = lpm(0, W, 10, 0, 0, 0);
        p.weight = 1;
        stats.addView(LearnTab.stat(a, "🔥", String.valueOf(S.pBest), "mejor racha"), p);
        LinearLayout.LayoutParams p2 = lpm(0, W, 10, 0, 0, 0);
        p2.weight = 1;
        stats.addView(LearnTab.stat(a, "🏆", levelsDone() + "/4", "niveles"), p2);
        content.addView(stats, lpm(M, W, 0, 0, 0, 16));

        content.addView(section(a, "Niveles · dificultad creciente"));
        for (int lv = 1; lv <= PracticeData.LEVELS; lv++) {
            final int level = lv;
            boolean open = S.practiceLevelOpen(level);
            boolean done = S.practiceLevelDone(level);
            LinearLayout b = box(a);
            LinearLayout r = row(a);
            r.addView(tv(a, done ? "🏆" : open ? "🎯" : "🔒", 26, TEXT, REG));
            LinearLayout tx = col(a);
            tx.setPadding(dp(12), 0, 0, 0);
            tx.addView(tv(a, "Nivel " + level + " · " + PracticeData.LEVEL_NAMES[level - 1], 16, open ? TEXT : MUTED, BLACK));
            tx.addView(tv(a, PracticeData.countLevel(level) + " ejercicios · " + (done ? "completado" : open ? "practica para desbloquear el siguiente" : "completa el nivel anterior para abrirlo"),
                    13, MUTED, REG), lpm(M, W, 0, 3, 0, 0));
            r.addView(tx, lp(0, W, 1));
            b.addView(r);
            TextView go = btn(a, done ? "Repetir" : "Empezar", open ? B_GREEN : B_OFF,
                    v -> { if (open) PracticeScreen.start(a, level); });
            go.setMinHeight(dp(44));
            b.addView(go, lpm(M, W, 0, 12, 0, 0));
            content.addView(b, lpm(M, W, 0, 0, 0, 10));
        }
    }

    private int levelsDone() {
        int n = 0;
        for (int i = 1; i <= PracticeData.LEVELS; i++) if (Store.I.practiceLevelDone(i)) n++;
        return n;
    }

    /** Loop llega con los avatares: placeholder, sin lógica todavía. */
    private void loopSoon() {
        LinearLayout b = box(a);
        b.setGravity(Gravity.CENTER_HORIZONTAL);
        b.setPadding(dp(16), dp(28), dp(16), dp(28));
        b.addView(tv(a, "🔁", 56, TEXT, REG));
        TextView t = tv(a, "Loop", 22, TEXT, BLACK);
        t.setGravity(Gravity.CENTER);
        b.addView(t, lpm(M, W, 0, 10, 0, 0));
        TextView p = tv(a, "Teoría y práctica revueltas y al azar, sin fin, sin puntos: solo bien o mal. Llega con los avatares, muy pronto.", 15, MUTED, REG);
        p.setGravity(Gravity.CENTER);
        p.setLineSpacing(0, 1.3f);
        b.addView(p, lpm(M, W, 0, 8, 0, 0));
        content.addView(b, lpm(M, W, 0, 0, 0, 10));
    }
}
