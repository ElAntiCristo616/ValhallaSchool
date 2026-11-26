package com.valhallaschool.app;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Locale;

import static com.valhallaschool.app.Ui.*;

/** Idea card (Deepstash-style) and reusable list row kinds. */
final class Cards {
    private Cards() {}

    static MainActivity A(View v) { return (MainActivity) v.getContext(); }

    /* ================= idea card ================= */
    static final class Idea {
        FrameLayout root;
        LinearLayout card;
        Views.Cover cover;
        TextView kicker, title, author, num, head, body, saveTx;
        ImageView saveIc;
        View read;
        int id = -1;
    }

    /** @param actions show save/share/read row; @param fill card fills the parent (feed page). */
    static Idea idea(Context c, boolean actions, boolean fill) {
        Idea h = new Idea();
        h.root = new FrameLayout(c);
        if (fill) h.root.setPadding(dp(12), dp(4), dp(12), dp(12));
        h.card = col(c);
        h.root.addView(h.card, new FrameLayout.LayoutParams(M, fill ? M : W));

        LinearLayout src = row(c);
        src.setPadding(dp(20), dp(18), dp(20), dp(4));
        h.cover = new Views.Cover(c, 0);
        src.addView(h.cover, lp(dp(38), dp(54)));
        LinearLayout meta = col(c);
        meta.setPadding(dp(12), 0, 0, 0);
        h.kicker = one(tv(c, "", 11, ACCENT, BLACK));
        h.kicker.setLetterSpacing(0.08f);
        h.title = one(tv(c, "", 15, TEXT, BOLD));
        h.author = one(tv(c, "", 13, MUTED, REG));
        meta.addView(h.kicker);
        meta.addView(h.title, lpm(M, W, 0, 2, 0, 0));
        meta.addView(h.author, lpm(M, W, 0, 1, 0, 0));
        src.addView(meta, lp(0, W, 1));
        h.card.addView(src, lp(M, W));
        if (actions) src.setOnClickListener(v -> { if (h.id >= 0) A(v).openBook(Data.book(h.id)); });

        LinearLayout body = col(c);
        body.setGravity(Gravity.CENTER_VERTICAL);
        body.setPadding(dp(24), dp(16), dp(24), dp(16));
        h.num = tv(c, "", 12, MUTED, BLACK);
        h.num.setLetterSpacing(0.1f);
        h.head = tv(c, "", 27, TEXT, BLACK);
        h.head.setLineSpacing(0, 1.05f);
        h.body = tv(c, "", 19, mix(TEXT, BG, 0.86f), REG);
        h.body.setLineSpacing(0, 1.3f);
        body.addView(h.num, lpm(M, W, 0, 0, 0, 14));
        body.addView(h.head, lpm(M, W, 0, 0, 0, 16));
        body.addView(h.body, lp(M, W));
        h.card.addView(body, fill ? lp(M, 0, 1) : lp(M, W));

        if (actions) {
            LinearLayout acts = row(c);
            acts.setPadding(dp(10), dp(4), dp(14), dp(12));
            LinearLayout save = ripple(row(c), true);
            save.setPadding(dp(10), 0, dp(12), 0);
            h.saveIc = new ImageView(c);
            save.addView(h.saveIc, lp(dp(22), dp(22)));
            h.saveTx = tv(c, "Guardar", 14, MUTED, MEDIUM);
            save.addView(h.saveTx, lpm(W, W, 6, 0, 0, 0));
            save.setOnClickListener(v -> {
                if (h.id < 0) return;
                boolean on = Store.I.toggleSave(h.id);
                paintSave(h, on);
                A(v).toast(on ? "🔖 Guardado" : "Quitado de guardados", false);
                A(v).dirty();
            });
            acts.addView(save, lp(W, dp(40)));
            acts.addView(iconBtn(c, R.drawable.ic_share, MUTED, v -> share(v.getContext(), h.id)));
            acts.addView(space(c), lp(0, 1, 1));
            LinearLayout rd = row(c);
            rd.addView(icon(c, R.drawable.ic_check, GREEN), lp(dp(18), dp(18)));
            rd.addView(tv(c, "Leído", 13, MUTED, BOLD), lpm(W, W, 5, 0, 4, 0));
            h.read = rd;
            acts.addView(rd);
            h.card.addView(acts, lp(M, W));
        }
        return h;
    }

    static void bind(Idea h, int id) {
        h.id = id;
        int b = Data.book(id), cc = Data.bColor(b);
        GradientDrawable g = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, new int[]{mix(cc, SURF, 0.22f), SURF, SURF});
        g.setCornerRadius(dp(20));
        g.setStroke(Math.max(1, dp(1)), mix(cc, LINE, 0.18f));
        h.card.setBackground(g);
        h.cover.setBook(b);
        h.kicker.setText(("Lección · " + Data.cat(b)).toUpperCase(Locale.ROOT));
        h.kicker.setTextColor(cc);
        h.title.setText(Data.bTitle[b]);
        h.author.setText(Data.bAuthor[b]);
        h.num.setText("IDEA " + (id % Data.PER + 1) + " DE " + Data.PER);
        h.head.setText(Data.head[id]);
        h.body.setText(Data.body[id]);
        if (h.saveIc != null) {
            paintSave(h, Store.I.saved.containsKey(id));
            h.read.animate().cancel();
            h.read.setAlpha(Store.I.isRead(id) ? 1 : 0);
        }
    }

    private static void paintSave(Idea h, boolean on) {
        h.saveIc.setImageResource(on ? R.drawable.ic_bookmark_fill : R.drawable.ic_bookmark);
        h.saveIc.setColorFilter(on ? ACCENT : MUTED);
        h.saveTx.setText(on ? "Guardado" : "Guardar");
        h.saveTx.setTextColor(on ? ACCENT : MUTED);
    }

    static void share(Context c, int id) {
        if (id < 0) return;
        int b = Data.book(id);
        String txt = Data.head[id] + ". " + Data.body[id] + "\n— " + Data.bTitle[b] + ", " + Data.bAuthor[b];
        Intent i = new Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, txt);
        c.startActivity(Intent.createChooser(i, "Compartir idea"));
    }

    /* ================= list kinds ================= */

    /** data: Object[]{Integer ideaId, String subtitle, View.OnClickListener} */
    static final Rows.Kind ROW = new Rows.Kind() {
        @Override public View create(ViewGroup p) {
            Context c = p.getContext();
            LinearLayout r = ripple(row(c), true);
            r.setPadding(0, dp(10), 0, dp(10));
            r.addView(new Views.Cover(c, 0), lp(dp(38), dp(54)));
            LinearLayout t = col(c);
            t.setPadding(dp(12), 0, 0, 0);
            t.addView(tv(c, "", 15, TEXT, BOLD));
            t.addView(one(tv(c, "", 13, MUTED, REG)), lpm(M, W, 0, 3, 0, 0));
            r.addView(t, lp(0, W, 1));
            return r;
        }

        @Override public void bind(View v, Object d) {
            Object[] o = (Object[]) d;
            int id = (Integer) o[0];
            LinearLayout r = (LinearLayout) v;
            ((Views.Cover) r.getChildAt(0)).setBook(Data.book(id));
            LinearLayout t = (LinearLayout) r.getChildAt(1);
            ((TextView) t.getChildAt(0)).setText(Data.head[id]);
            ((TextView) t.getChildAt(1)).setText((String) o[1]);
            r.setOnClickListener((View.OnClickListener) o[2]);
        }
    };

    static final Rows.Kind SECTION = new Rows.Kind() {
        @Override public View create(ViewGroup p) { return section(p.getContext(), ""); }

        @Override public void bind(View v, Object d) { ((TextView) v).setText(((String) d).toUpperCase(Locale.ROOT)); }
    };

    /** data: String[]{emoji, text} */
    static final Rows.Kind EMPTY = new Rows.Kind() {
        @Override public View create(ViewGroup p) {
            Context c = p.getContext();
            LinearLayout l = col(c);
            l.setGravity(Gravity.CENTER_HORIZONTAL);
            l.setPadding(dp(20), dp(60), dp(20), dp(60));
            l.addView(tv(c, "", 44, TEXT, REG));
            TextView t = tv(c, "", 15, MUTED, REG);
            t.setGravity(Gravity.CENTER);
            t.setLineSpacing(0, 1.3f);
            l.addView(t, lpm(W, W, 0, 8, 0, 0));
            return l;
        }

        @Override public void bind(View v, Object d) {
            String[] s = (String[]) d;
            LinearLayout l = (LinearLayout) v;
            ((TextView) l.getChildAt(0)).setText(s[0]);
            ((TextView) l.getChildAt(1)).setText(s[1]);
        }
    };

    /** A row of up to 3 book tiles. data: int[] book indexes */
    static final Rows.Kind BOOKROW = new Rows.Kind() {
        @Override public View create(ViewGroup p) {
            Context c = p.getContext();
            LinearLayout r = row(c);
            r.setGravity(Gravity.TOP);
            r.setPadding(0, 0, 0, dp(18));
            for (int i = 0; i < 3; i++) {
                LinearLayout tile = ripple(col(c), true);
                FrameLayout fr = new FrameLayout(c);
                fr.addView(new Views.Cover(c, 1), new FrameLayout.LayoutParams(M, W));
                ImageView badge = icon(c, R.drawable.ic_check, 0xffffffff);
                badge.setBackground(oval(GREEN));
                badge.setPadding(dp(5), dp(5), dp(5), dp(5));
                FrameLayout.LayoutParams bp = new FrameLayout.LayoutParams(dp(24), dp(24), Gravity.TOP | Gravity.END);
                bp.setMargins(0, dp(6), dp(6), 0);
                fr.addView(badge, bp);
                tile.addView(fr, lp(M, W));
                TextView t = tv(c, "", 13, TEXT, BOLD);
                t.setMaxLines(2);
                t.setEllipsize(android.text.TextUtils.TruncateAt.END);
                tile.addView(t, lpm(M, W, 0, 8, 0, 0));
                tile.addView(one(tv(c, "", 12, MUTED, REG)), lpm(M, W, 0, 2, 0, 0));
                tile.addView(new Views.Bar(c), lpm(M, dp(6), 0, 6, 0, 0));
                r.addView(tile, lpm(0, W, 0, 0, i < 2 ? 14 : 0, 0));
                ((LinearLayout.LayoutParams) tile.getLayoutParams()).weight = 1;
            }
            return r;
        }

        @Override public void bind(View v, Object d) {
            int[] bs = (int[]) d;
            LinearLayout r = (LinearLayout) v;
            for (int i = 0; i < 3; i++) {
                LinearLayout tile = (LinearLayout) r.getChildAt(i);
                if (i >= bs.length) { tile.setVisibility(View.INVISIBLE); tile.setOnClickListener(null); continue; }
                final int b = bs[i];
                int n = Store.I.bookRead(b);
                tile.setVisibility(View.VISIBLE);
                FrameLayout fr = (FrameLayout) tile.getChildAt(0);
                ((Views.Cover) fr.getChildAt(0)).setBook(b);
                fr.getChildAt(1).setVisibility(n == Data.PER ? View.VISIBLE : View.GONE);
                ((TextView) tile.getChildAt(1)).setText(Data.bTitle[b]);
                ((TextView) tile.getChildAt(2)).setText(Data.bAuthor[b]);
                ((Views.Bar) tile.getChildAt(3)).set(n / (float) Data.PER, GREEN);
                tile.setOnClickListener(x -> A(x).openBook(b));
            }
        }
    };
}
