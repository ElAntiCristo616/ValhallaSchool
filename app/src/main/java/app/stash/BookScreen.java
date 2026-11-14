package app.stash;

import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static app.stash.Ui.*;

/** Book detail: cover, progress, continue / quiz, and the list of its 10 ideas. */
final class BookScreen extends MainActivity.Screen {
    private final int b;
    private final LinearLayout content;

    BookScreen(MainActivity a, int b) {
        super(a);
        this.b = b;
        LinearLayout col = col(a);
        LinearLayout bar = row(a);
        bar.setPadding(dp(4), dp(6), dp(4), dp(2));
        bar.addView(iconBtn(a, R.drawable.ic_back, TEXT, x -> a.back()));
        col.addView(bar, lp(M, W));
        ScrollView sc = new ScrollView(a);
        content = col(a);
        content.setPadding(dp(16), 0, dp(16), dp(32));
        sc.addView(content);
        col.addView(sc, lp(M, 0, 1));
        v = col;
        draw();
    }

    private void draw() {
        Store S = Store.I;
        content.removeAllViews();
        int n = S.bookRead(b), cc = Data.bColor(b);
        String title = Data.bTitle[b];

        LinearLayout hero = col(a);
        hero.setGravity(Gravity.CENTER_HORIZONTAL);
        Views.Cover cv = new Views.Cover(a, 2);
        cv.setBook(b);
        hero.addView(cv, lp(dp(132), dp(198)));
        TextView t = tv(a, title, 26, TEXT, BLACK);
        t.setGravity(Gravity.CENTER);
        hero.addView(t, lpm(W, W, 0, 18, 0, 0));
        hero.addView(tv(a, Data.bAuthor[b], 15, MUTED, REG), lpm(W, W, 0, 6, 0, 0));
        TextView tag = tv(a, Data.cat(b).toUpperCase(Locale.ROOT), 12, cc, BLACK);
        tag.setLetterSpacing(0.06f);
        tag.setPadding(dp(10), dp(6), dp(10), dp(6));
        tag.setBackground(round(alpha(cc, 0.15f), 8));
        hero.addView(tag, lpm(W, W, 0, 12, 0, 0));
        content.addView(hero, lpm(M, W, 0, 4, 0, 18));

        LinearLayout pl = row(a);
        pl.addView(tv(a, n == Data.PER ? "Completed" : "Progress", 13, MUTED, BOLD), lp(0, W, 1));
        pl.addView(tv(a, n + "/" + Data.PER + " ideas", 13, MUTED, BOLD));
        content.addView(pl);
        content.addView(new Views.Bar(a).set(n / (float) Data.PER, GREEN), lpm(M, dp(14), 0, 8, 0, 20));

        List<Integer> ids = new ArrayList<>(), unread = new ArrayList<>();
        for (int i = b * Data.PER; i < b * Data.PER + Data.PER; i++) { ids.add(i); if (!S.isRead(i)) unread.add(i); }
        LinearLayout bs = row(a);
        bs.addView(btn(a, n == 0 ? "Start reading" : n == Data.PER ? "Read again" : "Continue", B_GREEN,
                x -> a.openReader(title, unread.isEmpty() ? ids : unread)), lp(0, W, 1));
        LinearLayout.LayoutParams qp = lpm(0, W, 12, 0, 0, 0);
        qp.weight = 1;
        bs.addView(btn(a, "Quiz", B_GHOST, x -> SessionScreen.bookQuiz(a, b)), qp);
        content.addView(bs, lp(M, W));

        content.addView(section(a, "Key ideas"));
        for (int id : ids) {
            boolean read = S.isRead(id);
            LinearLayout r = ripple(row(a), true);
            r.setGravity(Gravity.TOP);
            r.setPadding(0, dp(10), 0, dp(10));
            FrameLayout tick = new FrameLayout(a);
            if (read) {
                tick.setBackground(oval(GREEN));
                tick.addView(icon(a, R.drawable.ic_check, 0xffffffff), new FrameLayout.LayoutParams(dp(15), dp(15), Gravity.CENTER));
            } else {
                android.graphics.drawable.GradientDrawable o = oval(0);
                o.setStroke(dp(2), LINE);
                tick.setBackground(o);
                TextView num = tv(a, String.valueOf(id % Data.PER + 1), 12, MUTED, BLACK);
                tick.addView(num, new FrameLayout.LayoutParams(W, W, Gravity.CENTER));
            }
            r.addView(tick, lpm(dp(26), dp(26), 0, 1, 0, 0));
            LinearLayout tx = col(a);
            tx.setPadding(dp(12), 0, 0, 0);
            tx.addView(tv(a, Data.head[id], 15, TEXT, BOLD));
            TextView bd = tv(a, Data.body[id], 13, MUTED, REG);
            bd.setMaxLines(2);
            bd.setEllipsize(android.text.TextUtils.TruncateAt.END);
            tx.addView(bd, lpm(M, W, 0, 3, 0, 0));
            r.addView(tx, lp(0, W, 1));
            r.setOnClickListener(x -> a.openReader(title, MainActivity.from(id)));
            content.addView(r, lp(M, W));
        }
    }

    @Override void resume() { draw(); }
}
