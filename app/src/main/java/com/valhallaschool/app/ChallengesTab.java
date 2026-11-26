package com.valhallaschool.app;

import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import static com.valhallaschool.app.Ui.*;

/** Retos: misiones del día y tienda de gemas (mecánicas estilo Lingo). */
final class ChallengesTab extends MainActivity.Tab {
    private LinearLayout content;

    ChallengesTab(MainActivity a) { super(a); }

    @Override View build() {
        LinearLayout col = col(a);
        col.addView(pageTitle(a, "Retos"));
        ScrollView sc = new ScrollView(a);
        content = col(a);
        content.setPadding(dp(16), 0, dp(16), dp(32));
        sc.addView(content);
        col.addView(sc, lp(M, 0, 1));
        return col;
    }

    @Override void refresh() {
        Store S = Store.I;
        content.removeAllViews();

        LinearLayout gems = row(a);
        gems.setBackground(round(SURF, 16, LINE, 1));
        gems.setPadding(dp(16), dp(14), dp(16), dp(14));
        gems.addView(tv(a, "💎", 22, TEXT, REG));
        gems.addView(tv(a, S.gems + " gemas", 18, TEXT, BLACK), lpm(0, W, 1));
        gems.addView(tv(a, "❤️ " + (3 + S.bonusHearts), 16, TEXT, BOLD));
        content.addView(gems, lpm(M, W, 0, 4, 0, 16));

        content.addView(section(a, "Misiones de hoy"));
        mission("Gana 100 XP hoy", S.todayXP(), 100, "⚡");
        mission("Racha de 7 días", S.streak(), 7, "🔥");
        mission("Completa 5 lecciones", S.lessons.size(), 5, "📚");

        content.addView(section(a, "Tienda"));
        shopItem("❤️", "Corazón extra", "Un corazón más en tu próxima lección", 20, () -> {
            S.bonusHearts++;
            S.save();
            a.toast("Corazón extra comprado ❤️", false);
        });
        shopItem("❄️", "Congelar racha", "Tu racha no se rompe mañana aunque no juegues", 50, () -> {
            S.frozen.add(Store.today() + 1);
            S.save();
            a.toast("Racha congelada para mañana ❄️", false);
        });
    }

    private void mission(String name, int cur, int goal, String ico) {
        boolean done = cur >= goal;
        LinearLayout b = box(a);
        LinearLayout r = row(a);
        r.addView(tv(a, ico, 22, TEXT, REG));
        TextView t = tv(a, name + " · " + Math.min(cur, goal) + "/" + goal, 15, done ? ACCENT : TEXT, done ? BLACK : MEDIUM);
        t.setPadding(dp(12), 0, 0, 0);
        r.addView(t, lp(0, W, 1));
        if (done) r.addView(tv(a, "✓", 18, ACCENT, BLACK));
        b.addView(r);
        b.addView(new Views.Bar(a).set(Math.min(1f, cur / (float) goal), done ? ACCENT : BLUE), lpm(M, dp(8), 0, 10, 0, 0));
        content.addView(b, lpm(M, W, 0, 0, 0, 10));
    }

    private void shopItem(String ico, String name, String desc, int price, Runnable buy) {
        Store S = Store.I;
        boolean afford = S.gems >= price;
        LinearLayout b = box(a);
        LinearLayout r = row(a);
        r.addView(tv(a, ico, 26, TEXT, REG));
        LinearLayout tx = col(a);
        tx.setPadding(dp(12), 0, 0, 0);
        tx.addView(tv(a, name + " · " + price + " gemas", 16, TEXT, BLACK));
        tx.addView(tv(a, desc, 13, MUTED, REG), lpm(M, W, 0, 3, 0, 0));
        r.addView(tx, lp(0, W, 1));
        b.addView(r);
        TextView btn = btn(a, afford ? "Comprar" : "Te faltan gemas", afford ? B_YELLOW : B_OFF, v -> {
            S.gems -= price;
            buy.run();
            refresh();
        });
        b.addView(btn, lpm(M, W, 0, 12, 0, 0));
        content.addView(b, lpm(M, W, 0, 0, 0, 10));
    }
}
