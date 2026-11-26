package com.valhallaschool.app;

import android.widget.LinearLayout;

import java.util.ArrayList;
import java.util.List;

import static com.valhallaschool.app.Ui.*;

/** Full-screen swipe reader for a fixed list of ideas (a book, a search hit, a saved idea). */
final class ReaderScreen extends MainActivity.Screen {
    ReaderScreen(MainActivity a, String title, List<Integer> ids) {
        super(a);
        LinearLayout col = col(a);
        LinearLayout bar = row(a);
        bar.setPadding(dp(4), dp(6), dp(16), dp(2));
        bar.addView(iconBtn(a, R.drawable.ic_back, TEXT, x -> a.back()));
        bar.addView(one(tv(a, title, 16, TEXT, BOLD)), lpm(0, W, 6, 0, 0, 0));
        ((LinearLayout.LayoutParams) bar.getChildAt(1).getLayoutParams()).weight = 1;
        col.addView(bar, lp(M, W));
        final int[] p = {0};
        Feed f = new Feed(a, n -> {
            int e = Math.min(ids.size(), p[0] + n);
            List<Integer> r = new ArrayList<>(ids.subList(p[0], e));
            p[0] = e;
            return r;
        }, new String[]{"✅", "Listo", "Buen trabajo. Fíjalo con un quiz en Aprender.", "Cerrar"}, a::back);
        col.addView(f, lp(M, 0, 1));
        v = col;
    }
}
