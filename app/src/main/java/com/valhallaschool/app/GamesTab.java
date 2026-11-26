package com.valhallaschool.app;

import android.view.Gravity;
import android.view.View;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import static com.valhallaschool.app.Ui.*;

/** Minijuegos: memoria, arrastrar, crucigrama y sopa de letras, por módulo. */
final class GamesTab extends MainActivity.Tab {
    static final String[] MODDIRS = {"m1-elementos", "m2-modelo-a", "m3-tipos-cuadras", "m4-dicotomias", "m5-relaciones"};
    private static final String[] GAMES = {"memoria", "arrastrar", "crucigrama", "sopa"};
    private static final String[] TITLES = {"Memoria", "Arrastrar", "Crucigrama", "Sopa de letras"};
    private static final String[] SUBS = {"Elemento ↔ definición", "Función a su bloque", "Términos de sociónica", "Encuentra los términos"};
    private static final String[] ICONS = {"🃏", "↔️", "🔠", "🔍"};

    private int mod = 0;
    private LinearLayout content;
    private TextView[] chips;

    GamesTab(MainActivity a) { super(a); }

    @Override View build() {
        LinearLayout col = col(a);
        col.addView(pageTitle(a, "Minijuegos"));
        HorizontalScrollView hs = new HorizontalScrollView(a);
        hs.setHorizontalScrollBarEnabled(false);
        hs.setOverScrollMode(View.OVER_SCROLL_NEVER);
        LinearLayout r = row(a);
        r.setPadding(dp(16), dp(4), dp(16), dp(10));
        hs.addView(r);
        chips = new TextView[Data.catName.length];
        for (int i = 0; i < Data.catName.length; i++) {
            final int idx = i;
            TextView t = tv(a, Data.catName[i], 14, MUTED, MEDIUM);
            t.setGravity(Gravity.CENTER);
            t.setPadding(dp(14), 0, dp(14), 0);
            t.setOnClickListener(v -> { mod = idx; refresh(); });
            chips[i] = t;
            r.addView(t, lpm(W, dp(34), 0, 0, 8, 0));
        }
        col.addView(hs, lp(M, W));
        ScrollView sc = new ScrollView(a);
        content = col(a);
        content.setPadding(dp(16), 0, dp(16), dp(32));
        sc.addView(content);
        col.addView(sc, lp(M, 0, 1));
        return col;
    }

    @Override void refresh() {
        for (int i = 0; i < chips.length; i++) {
            chips[i].setBackground(i == mod ? round(TEXT, 17) : round(0, 17, LINE, 1));
            chips[i].setTextColor(i == mod ? BG : MUTED);
        }
        content.removeAllViews();
        content.addView(tv(a, "Practica y refuerza tus conocimientos de sociónica", 14, MUTED, REG), lpm(M, W, 0, 2, 0, 14));
        for (int g = 0; g < GAMES.length; g++) {
            final int gg = g;
            LinearLayout card = box(a);
            LinearLayout r = row(a);
            TextView ic = tv(a, ICONS[g], 34, TEXT, REG);
            ic.setGravity(Gravity.CENTER);
            r.addView(ic, lp(dp(52), dp(52)));
            LinearLayout tx = col(a);
            tx.setPadding(dp(14), 0, 0, 0);
            tx.addView(tv(a, TITLES[g], 18, TEXT, BLACK));
            tx.addView(tv(a, SUBS[g], 13, MUTED, REG), lpm(M, W, 0, 3, 0, 0));
            r.addView(tx, lp(0, W, 1));
            card.addView(r);
            TextView play = btn(a, "Jugar", B_YELLOW, v -> a.push(new GameScreen(a, MODDIRS[mod], GAMES[gg], TITLES[gg] + " · " + Data.catName[mod])));
            card.addView(play, lpm(M, W, 0, 14, 0, 0));
            content.addView(card, lpm(M, W, 0, 0, 0, 12));
        }
    }
}
