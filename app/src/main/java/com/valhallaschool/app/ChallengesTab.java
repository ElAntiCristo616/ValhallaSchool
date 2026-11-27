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
        gems.addView(tv(a, S.gems + " gemas", 18, TEXT, BLACK), lp(0, W, 1));
        content.addView(gems, lpm(M, W, 0, 4, 0, 16));

        content.addView(section(a, "Misiones de hoy"));
        mission("Gana 100 XP hoy", S.todayXP(), 100, "⚡");
        mission("Racha de 7 días", S.streak(), 7, "🔥");
        mission("Completa 5 lecciones", S.lessons.size(), 5, "📚");

        // v1.3.2: las tareas diarias CAMBIAN DE OBJETIVO cada día: 4 misiones
        // rotando de un pool de 9 (los 8 juegos + Práctica), con semilla del
        // día para que a todos les toque el mismo conjunto ese día. El progreso
        // del día se reinicia cada día como ya hacía (gameDay por fecha).
        content.addView(section(a, "Retos de hoy · se renuevan cada día"));
        java.util.ArrayList<String[]> defs = new java.util.ArrayList<>();
        defs.add(new String[]{"🌧️", "Juega 3 rondas de Lluvia", "lluvia", "3"});
        defs.add(new String[]{"🕵️", "Resuelve un caso de Detective", "detective", "1"});
        defs.add(new String[]{"🧪", "Arma un modelo en el Laboratorio", "lab", "1"});
        defs.add(new String[]{"🔍", "Encuentra 5 intrusos", "intruso", "5"});
        defs.add(new String[]{"⚖️", "Juzga 3 argumentos en el Tribunal", "tribunal", "3"});
        defs.add(new String[]{"🧠", "Haz 10 ejercicios de Práctica", "practice", "10"});
        defs.add(new String[]{"⚔️", "Media 2 combates en la Arena", "arena", "2"});
        defs.add(new String[]{"🗺️", "Resuelve un acertijo de Aventura", "aventura", "1"});
        defs.add(new String[]{"🔗", "Completa 3 encargos de Conexiones", "conexiones", "3"});
        java.util.Collections.shuffle(defs, new java.util.Random(Store.today()));
        for (int i = 0; i < 4; i++) {
            String[] d = defs.get(i);
            final String gid = d[2];
            Runnable open;
            switch (gid) {
                case "lluvia": open = () -> a.push(LluviaScreen.start(a)); break;
                case "detective": open = () -> a.push(DetectiveScreen.start(a)); break;
                case "lab": open = () -> a.push(LabScreen.start(a)); break;
                case "intruso": open = () -> a.push(IntrusoScreen.start(a)); break;
                case "tribunal": open = () -> a.push(TribunalScreen.start(a)); break;
                case "arena": open = () -> a.push(ArenaScreen.start(a)); break;
                case "aventura": open = () -> a.push(AventuraScreen.start(a)); break;
                case "conexiones": open = () -> a.push(ConexionesScreen.start(a)); break;
                default: open = () -> a.go(2); break;
            }
            missionLink(d[0], d[1], S.gameDay(gid), Integer.parseInt(d[3]), open);
        }

        content.addView(section(a, "Tienda"));
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

    /** Misión con navegación directa: toda la tarjeta es un botón que abre su juego o tarea. */
    private void missionLink(String ico, String name, int cur, int goal, Runnable open) {
        boolean done = cur >= goal;
        LinearLayout b = box(a);
        LinearLayout r = row(a);
        r.addView(tv(a, ico, 22, TEXT, REG));
        TextView t = tv(a, name + " · " + Math.min(cur, goal) + "/" + goal, 15, done ? ACCENT : TEXT, done ? BLACK : MEDIUM);
        t.setPadding(dp(12), 0, 0, 0);
        r.addView(t, lp(0, W, 1));
        if (done) r.addView(tv(a, "✓", 18, ACCENT, BLACK));
        r.addView(tv(a, "›", 22, done ? ACCENT : MUTED, BLACK));
        b.addView(r);
        b.addView(new Views.Bar(a).set(Math.min(1f, cur / (float) goal), done ? ACCENT : BLUE), lpm(M, dp(8), 0, 10, 0, 0));
        b.setOnClickListener(v -> open.run());
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
