package com.valhallaschool.app;

import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import static com.valhallaschool.app.Ui.*;

/** Juegos: los 8 minijuegos nativos de la Biblia creativa (v1.3.0). */
final class GamesTab extends MainActivity.Tab {
    private LinearLayout content;

    GamesTab(MainActivity a) { super(a); }

    @Override View build() {
        LinearLayout col = col(a);
        col.addView(pageTitle(a, "Juegos"));
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
        content.addView(Avatars.bubble(a), lpm(M, W, 0, 0, 0, 14));
        TextView intro = tv(a, "Ocho juegos del Modelo A clásico. Cada acierto suma 10 XP a tu meta diaria, "
                + "pero solo la primera vez: repetir no repite XP.", 15, MUTED, REG);
        intro.setLineSpacing(0, 1.3f);
        content.addView(intro, lpm(M, W, 0, 0, 0, 16));

        game("🕵️", "Detective sociónico", "30 fragmentos · 3 expedientes · cierra el caso", "detective",
                () -> a.push(DetectiveScreen.start(a)));
        game("🧪", "Laboratorio del Modelo A", "16 modelos · 7 grados · toca o arrastra las fichas", "lab",
                () -> a.push(LabScreen.start(a)));
        game("⚖️", "Tribunal tipológico", "20 argumentos · sella, localiza el error y reformula", "tribunal",
                () -> a.push(TribunalScreen.start(a)));
        game("🔍", "Encuentra al intruso", "30 conjuntos · el que no pertenece y la regla", "intruso",
                () -> a.push(IntrusoScreen.start(a)));
        game("🔗", "Conexiones intertipo", "12 encargos · matriz 16×16 para explorar", "conexiones",
                () -> a.push(ConexionesScreen.start(a)));
        game("🥊", "Arena sociónica", "6 discusiones · media sin premiar la violencia", "arena",
                () -> a.push(ArenaScreen.start(a)));
        game("🗺️", "Aventura por las cuadras", "Biblioteca de Alfa · 5 acertijos · puerta final", "aventura",
                () -> a.push(AventuraScreen.start(a)));
        game("🌧️", "Lluvia informacional", "30 fichas · 2 velocidades + modo tranquilo", "lluvia",
                () -> a.push(LluviaScreen.start(a)));
    }

    private void game(String emoji, String name, String desc, String id, Runnable start) {
        int best = Store.I.gameBest(id);
        LinearLayout b = box(a);
        LinearLayout r = row(a);
        r.addView(tv(a, emoji, 26, TEXT, REG));
        LinearLayout t = col(a);
        t.setPadding(dp(12), 0, 0, 0);
        t.addView(tv(a, name, 17, TEXT, BLACK));
        TextView d = tv(a, desc, 14, MUTED, REG);
        d.setLineSpacing(0, 1.25f);
        t.addView(d);
        TextView bestTx = tv(a, best > 0 ? "Mejor: " + best + "%" : "Sin jugar", 13, best > 0 ? ACCENT : MUTED, BLACK);
        t.addView(bestTx, lpm(M, W, 0, 4, 0, 0));
        r.addView(t, lp(0, W, 1));
        r.addView(tv(a, "›", 28, ACCENT, BLACK));
        b.addView(r);
        b.setOnClickListener(v -> start.run());
        content.addView(b, lpm(M, W, 0, 0, 0, 12));
    }
}
