package com.valhallaschool.app;

import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.Locale;

import static com.valhallaschool.app.Ui.*;

/** Learn: streak / XP / level, daily goal, spaced-repetition review, and the Duolingo-style lesson path. */
final class LearnTab extends MainActivity.Tab {
    private final Rows rows = new Rows();
    private LinearLayoutManager lm;
    private int cb = -1, ck = -1;

    LearnTab(MainActivity a) { super(a); }

    @Override View build() {
        LinearLayout col = col(a);
        col.addView(pageTitle(a, "Aprender"));
        RecyclerView rv = new RecyclerView(a);
        lm = new LinearLayoutManager(a);
        rv.setLayoutManager(lm);
        rv.setAdapter(rows);
        rv.setItemViewCacheSize(6);
        rv.setPadding(dp(16), 0, dp(16), dp(32));
        rv.setClipToPadding(false);
        col.addView(rv, lp(M, 0, 1));
        return col;
    }

    @Override void refresh() {
        Store S = Store.I;
        cb = -1; ck = -1;
        outer:
        for (int b = 0; b < Data.NB; b++) for (int k = 0; k < 3; k++) if (!S.lessonDone(b, k)) { cb = b; ck = k; break outer; }
        rows.clear();
        rows.add(Rows.STATIC, spaced(stats()));
        rows.add(Rows.STATIC, spaced(goal()));
        rows.add(Rows.STATIC, spaced(review()));
        rows.add(Cards.SECTION, "Tu camino · " + S.lessons.size() + "/" + (Data.NB * 3) + " lecciones");
        int first = rows.size();
        for (int b = 0; b < Data.NB; b++) rows.add(unit, b);
        rows.done();
        if (cb > 0) lm.scrollToPositionWithOffset(first + cb, dp(110));
    }

    private static View spaced(View v) {
        FrameLayout.LayoutParams p = new FrameLayout.LayoutParams(M, W);
        p.bottomMargin = dp(14);
        v.setLayoutParams(p);
        return v;
    }

    /** Stat tile: icon, big value, small label. Shared with Profile. */
    static LinearLayout stat(Context c, String ico, String value, String label) {
        LinearLayout s = row(c);
        s.setBackground(round(SURF, 16, LINE, 1));
        s.setPadding(dp(12), dp(12), dp(10), dp(12));
        s.addView(tv(c, ico, 21, TEXT, REG));
        LinearLayout t = col(c);
        t.setPadding(dp(9), 0, 0, 0);
        t.addView(one(tv(c, value, 20, TEXT, BLACK)));
        t.addView(one(tv(c, label, 12, MUTED, MEDIUM)), lpm(W, W, 0, 2, 0, 0));
        s.addView(t, lp(0, W, 1));
        return s;
    }

    private View stats() {
        Store S = Store.I;
        int xp = S.totalXP();
        LinearLayout r = row(a);
        LinearLayout s1 = stat(a, "🔥", String.valueOf(S.streak()), "días de racha");
        s1.getChildAt(0).setAlpha(S.met.contains(Store.today()) ? 1f : 0.4f);
        r.addView(s1, lp(0, W, 1));
        r.addView(stat(a, "⚡", String.valueOf(xp), "XP total"), weighted(10));
        r.addView(stat(a, "🎖️", String.valueOf(Store.level(xp)), "nivel"), weighted(10));
        return r;
    }

    private static LinearLayout.LayoutParams weighted(int leftDp) {
        LinearLayout.LayoutParams p = lpm(0, W, leftDp, 0, 0, 0);
        p.weight = 1;
        return p;
    }

    private View goal() {
        Store S = Store.I;
        int x = S.todayXP();
        float pct = Math.min(1f, x / (float) S.goal);
        LinearLayout b = box(a);
        b.setOrientation(LinearLayout.HORIZONTAL);
        b.setGravity(Gravity.CENTER_VERTICAL);
        Views.Ring ring = new Views.Ring(a, 6);
        ring.set(pct);
        b.addView(ring, lp(dp(64), dp(64)));
        LinearLayout t = col(a);
        t.setPadding(dp(16), 0, 0, 0);
        t.addView(tv(a, pct >= 1 ? "¡Meta diaria lograda! 🎉" : "Meta diaria", 17, TEXT, BLACK));
        String sub = x + " / " + S.goal + " XP hoy";
        if (pct < 1) sub += " · unas " + (int) Math.ceil((S.goal - x) / (double) Store.XP_READ) + " ideas o una lección para llegar";
        TextView p = tv(a, sub, 14, MUTED, REG);
        p.setLineSpacing(0, 1.2f);
        t.addView(p, lpm(M, W, 0, 4, 0, 0));
        b.addView(t, lp(0, W, 1));
        return b;
    }

    private View review() {
        Store S = Store.I;
        int due = S.due().size(), nRead = S.read.size();
        LinearLayout b = box(a);
        b.setOrientation(LinearLayout.HORIZONTAL);
        b.setGravity(Gravity.CENTER_VERTICAL);
        TextView ico = tv(a, "🧠", 26, TEXT, REG);
        ico.setGravity(Gravity.CENTER);
        ico.setBackground(round(alpha(BLUE, 0.18f), 16));
        b.addView(ico, lp(dp(52), dp(52)));
        LinearLayout t = col(a);
        t.setPadding(dp(14), 0, dp(10), 0);
        t.addView(tv(a, due > 0 ? due + " por repasar" : "Repaso", 17, TEXT, BLACK));
        t.addView(tv(a, nRead < 3 ? "Lee algunas ideas para desbloquear los repasos" : due > 0 ? "La repetición espaciada mantiene frescas las ideas" : "Nada pendiente. ¿Practicas de todos modos?",
                14, MUTED, REG), lpm(M, W, 0, 3, 0, 0));
        b.addView(t, lp(0, W, 1));
        TextView go = btn(a, due > 0 ? "Empezar" : "Practicar", nRead < 3 ? B_OFF : due > 0 ? B_GREEN : B_GHOST, v -> SessionScreen.review(a));
        go.setMinHeight(dp(44));
        go.setTextSize(13);
        b.addView(go, lp(W, W));
        return b;
    }

    private final Rows.Kind unit = new Rows.Kind() {
        @Override public View create(ViewGroup p) {
            Context c = p.getContext();
            LinearLayout u = col(c);
            u.setPadding(0, dp(10), 0, 0);
            LinearLayout hdr = row(c);
            hdr.setPadding(dp(16), dp(12), dp(8), dp(16));
            LinearLayout tx = col(c);
            TextView small = one(tv(c, "", 12, 0xff111111, BLACK));
            small.setAlpha(0.7f);
            small.setLetterSpacing(0.08f);
            tx.addView(small);
            tx.addView(tv(c, "", 17, 0xff111111, BLACK), lpm(M, W, 0, 2, 0, 0));
            hdr.addView(tx, lp(0, W, 1));
            ImageView bk = iconBtn(c, R.drawable.ic_book, 0xff111111, null);
            bk.setBackground(oval(0x1f000000));
            hdr.addView(bk);
            u.addView(hdr, lp(M, W));
            LinearLayout nodes = col(c);
            nodes.setGravity(Gravity.CENTER_HORIZONTAL);
            nodes.setPadding(0, dp(22), 0, dp(14));
            for (int k = 0; k < 3; k++) {
                LinearLayout cell = col(c);
                cell.setGravity(Gravity.CENTER_HORIZONTAL);
                TextView bub = tv(c, "EMPEZAR", 13, TEXT, BLACK);
                bub.setLetterSpacing(0.05f);
                bub.setPadding(dp(12), dp(7), dp(12), dp(7));
                bub.setBackground(round(SURF, 12, LINE, 2));
                cell.addView(bub, lpm(W, W, 0, 0, 0, 2));
                cell.addView(new Views.Node(c), lp(dp(100), dp(98)));
                TextView lbl = tv(c, "", 11, MUTED, BLACK);
                lbl.setLetterSpacing(0.04f);
                cell.addView(lbl);
                cell.setTranslationX(dp(k == 1 ? -46 : k == 2 ? 30 : 0));
                nodes.addView(cell, lpm(W, W, 0, 0, 0, 18));
            }
            u.addView(nodes, lp(M, W));
            return u;
        }

        @Override public void bind(View v, Object d) {
            final int b = (Integer) d;
            Store S = Store.I;
            int col = Data.bColor(b);
            LinearLayout u = (LinearLayout) v;
            LinearLayout hdr = (LinearLayout) u.getChildAt(0);
            hdr.setBackground(raised(col, mix(col, 0xff000000, 0.6f), 16, 4, 0));
            LinearLayout tx = (LinearLayout) hdr.getChildAt(0);
            ((TextView) tx.getChildAt(0)).setText(("Unidad " + (b + 1) + " · " + Data.cat(b)).toUpperCase(Locale.ROOT));
            ((TextView) tx.getChildAt(1)).setText(Data.bTitle[b]);
            hdr.getChildAt(1).setOnClickListener(x -> a.openBook(b));
            LinearLayout nodes = (LinearLayout) u.getChildAt(1);
            String[] labels = {"IDEAS 1–5", "IDEAS 6–10", "QUIZ DE LECCIÓN"};
            for (int k = 0; k < 3; k++) {
                final int kk = k;
                LinearLayout cell = (LinearLayout) nodes.getChildAt(k);
                boolean done = S.lessonDone(b, k), cur = b == cb && k == ck;
                TextView bub = (TextView) cell.getChildAt(0);
                bub.setVisibility(cur ? View.VISIBLE : View.GONE);
                bub.setTextColor(col);
                Views.Node n = (Views.Node) cell.getChildAt(1);
                n.set(done ? ACCENT : col, done ? "✓" : k == 2 ? "🏆" : "★", cur, !done && !cur && b > cb);
                n.setOnClickListener(x -> { if (kk == 2) SessionScreen.bookQuiz(a, b); else SessionScreen.lesson(a, b, kk); });
                ((TextView) cell.getChildAt(2)).setText(labels[k]);
            }
        }
    };
}
