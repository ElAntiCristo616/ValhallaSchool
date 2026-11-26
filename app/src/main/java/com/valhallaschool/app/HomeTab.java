package com.valhallaschool.app;

import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

import static com.valhallaschool.app.Ui.*;

/** Home: endless feed of unread ideas, shuffled once per install (stable order), filterable by topic. */
final class HomeTab extends MainActivity.Tab {
    private FrameLayout feedBox;
    private TextView streakTx, xpTx, flame;
    private Views.Ring ring;
    private int[] order;

    HomeTab(MainActivity a) { super(a); }

    @Override View build() {
        Store S = Store.I;
        LinearLayout col = col(a);
        LinearLayout top = row(a);
        top.setPadding(dp(16), 0, dp(12), 0);
        SpannableString brand = new SpannableString("Valhalla School");
        brand.setSpan(new ForegroundColorSpan(ACCENT), 0, 8, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        TextView bt = tv(a, brand, 22, TEXT, BLACK);
        bt.setLetterSpacing(-0.03f);
        top.addView(bt, lp(0, W, 1));

        LinearLayout p1 = pill();
        flame = tv(a, "🔥", 15, TEXT, REG);
        streakTx = tv(a, "0", 14, TEXT, BOLD);
        p1.addView(flame);
        p1.addView(streakTx, lpm(W, W, 5, 0, 0, 0));
        LinearLayout p2 = pill();
        ring = new Views.Ring(a, 3.5f);
        p2.addView(ring, lp(dp(20), dp(20)));
        xpTx = tv(a, "0 XP", 14, TEXT, BOLD);
        p2.addView(xpTx, lpm(W, W, 7, 0, 0, 0));
        top.addView(p1, lp(W, dp(34)));
        top.addView(p2, lpm(W, dp(34), 8, 0, 0, 0));
        col.addView(top, lp(M, dp(56)));

        col.addView(chips(a, "Para ti", S.cat, i -> { S.cat = i; S.save(); startFeed(); }), lp(M, W));
        feedBox = new FrameLayout(a);
        col.addView(feedBox, lp(M, 0, 1));
        startFeed();
        header();
        return col;
    }

    private LinearLayout pill() {
        LinearLayout p = ripple(row(a), true);
        p.setPadding(dp(10), 0, dp(12), 0);
        p.setBackground(round(SURF2, 17));
        p.setOnClickListener(v -> a.go(2));
        return p;
    }

    private void startFeed() {
        if (order == null) {
            ArrayList<Integer> l = new ArrayList<>(Data.N);
            for (int i = 0; i < Data.N; i++) l.add(i);
            Collections.shuffle(l, new Random(Store.I.seed));
            order = new int[Data.N];
            for (int i = 0; i < Data.N; i++) order[i] = l.get(i);
        }
        final int cat = Store.I.cat;
        final int[] p = {0};
        Feed f = new Feed(a, n -> {
            ArrayList<Integer> out = new ArrayList<>();
            while (out.size() < n && p[0] < Data.N) {
                int id = order[p[0]++];
                if (!Store.I.isRead(id) && (cat < 0 || Data.bCat[Data.book(id)] == cat)) out.add(id);
            }
            return out;
        }, new String[]{"🎉", "Ya viste todo",
                cat < 0 ? "Ya leíste todas las ideas. Refresca con los repasos." : "No quedan ideas sin leer en este módulo.", "Ir a Aprender"},
                () -> a.go(2));
        feedBox.removeAllViews();
        feedBox.addView(f, new FrameLayout.LayoutParams(M, M));
    }

    @Override void refresh() { header(); }

    void header() {
        if (streakTx == null) return;
        Store S = Store.I;
        streakTx.setText(String.valueOf(S.streak()));
        flame.setAlpha(S.met.contains(Store.today()) ? 1f : 0.4f);
        int x = S.todayXP();
        xpTx.setText(x + " XP");
        ring.set(x / (float) S.goal);
    }
}
