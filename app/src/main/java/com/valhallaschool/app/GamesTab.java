package com.valhallaschool.app;

import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import static com.valhallaschool.app.Ui.*;

/**
 * Juegos: los 8 minijuegos nativos de la Biblia creativa (v1.3.0).
 * v1.3.1: hub en cuadrícula de tarjetas cuadradas y detalladas
 * (icono, nombre, descripción de qué va, mejor puntaje) con
 * «Encuentra al intruso» arriba y destacado como el más interesante.
 */
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

        // Intruso arriba y diferenciado: tarjeta ancha con acento dorado.
        featured("🔍", "Encuentra al intruso",
                "30 conjuntos: encuentra al que no pertenece y di por qué. Cuadras, elementos, posiciones, bloques, anillos y más.",
                "intruso");

        // Los otros 7 en cuadrícula de 2 columnas, tarjetas cuadradas.
        String[][] g = {
                {"🕵️", "Detective sociónico", "Investiga 3 expedientes: marca evidencias, lanza hipótesis y cierra el caso.", "detective"},
                {"🧪", "Laboratorio del Modelo A", "Arma los 16 modelos del Modelo A en 8 ranuras. 7 grados, con tostadora.", "lab"},
                {"⚖️", "Tribunal tipológico", "Juzga 20 argumentos: sella, localiza el error y reformula. Sin piedad.", "tribunal"},
                {"🔗", "Conexiones intertipo", "12 encargos para armar parejas y equipos con la matriz 16×16.", "conexiones"},
                {"🥊", "Arena sociónica", "Media 6 discusiones cotidianas sin que explote la tensión.", "arena"},
                {"🗺️", "Aventura por las cuadras", "Explora la Biblioteca de Alfa: 5 acertijos, claves y la puerta final.", "aventura"},
                {"🌧️", "Lluvia informacional", "Clasifica 30 fichas antes de que caigan: elementos, posiciones y bloques.", "lluvia"},
        };
        for (int i = 0; i < g.length; i += 2) {
            LinearLayout r = row(a);
            LinearLayout.LayoutParams p1 = lp(0, M, 1);
            p1.setMargins(0, 0, dp(6), 0);
            r.addView(cell(g[i][0], g[i][1], g[i][2], g[i][3]), p1);
            if (i + 1 < g.length) {
                LinearLayout.LayoutParams p2 = lp(0, M, 1);
                p2.setMargins(dp(6), 0, 0, 0);
                r.addView(cell(g[i + 1][0], g[i + 1][1], g[i + 1][2], g[i + 1][3]), p2);
            } else {
                // Última fila impar: un hueco del mismo tamaño para no estirar la tarjeta.
                r.addView(new View(a), lp(0, W, 1));
            }
            content.addView(r, lpm(M, W, 0, 0, 0, 12));
        }
    }

    /** Tarjeta destacada de ancho completo, con borde dorado y etiqueta. */
    private void featured(String emoji, String name, String desc, String id) {
        int best = Store.I.gameBest(id);
        LinearLayout b = col(a);
        b.setBackground(round(mix(ACCENT, SURF, 0.12f), 20, ACCENT, 2));
        b.setPadding(dp(16), dp(16), dp(16), dp(16));
        TextView badge = tv(a, "⭐  LA MÁS INTERESANTE", 11, 0xFF1A1206, BLACK);
        badge.setBackground(round(ACCENT, 10));
        badge.setPadding(dp(10), dp(5), dp(10), dp(5));
        b.addView(badge);
        b.addView(tv(a, emoji, 34, TEXT, REG), lpm(W, W, 0, 12, 0, 0));
        b.addView(tv(a, name, 19, TEXT, BLACK), lpm(M, W, 0, 6, 0, 0));
        TextView d = tv(a, desc, 14, MUTED, REG);
        d.setLineSpacing(0, 1.3f);
        b.addView(d, lpm(M, W, 0, 4, 0, 0));
        b.addView(tv(a, best > 0 ? "Mejor: " + best + "%" : "Sin jugar", 13, best > 0 ? ACCENT : MUTED, BLACK),
                lpm(M, W, 0, 8, 0, 0));
        b.setOnClickListener(v -> open(id));
        content.addView(b, lpm(M, W, 0, 0, 0, 12));
    }

    /** Celda cuadrada de la cuadrícula: icono, nombre, descripción y mejor puntaje. */
    private LinearLayout cell(String emoji, String name, String desc, String id) {
        int best = Store.I.gameBest(id);
        LinearLayout b = box(a);
        b.addView(tv(a, emoji, 30, TEXT, REG));
        TextView n = tv(a, name, 16, TEXT, BLACK);
        n.setMaxLines(2);
        b.addView(n, lpm(M, W, 0, 8, 0, 0));
        TextView d = tv(a, desc, 13, MUTED, REG);
        d.setLineSpacing(0, 1.25f);
        d.setMaxLines(4);
        b.addView(d, lpm(M, W, 0, 4, 0, 0));
        // Empuja el mejor puntaje al fondo de la tarjeta.
        b.addView(new View(a), lp(M, 0, 1));
        b.addView(tv(a, best > 0 ? "Mejor: " + best + "%" : "Sin jugar", 13, best > 0 ? ACCENT : MUTED, BLACK));
        b.setOnClickListener(v -> open(id));
        return b;
    }

    private void open(String id) {
        switch (id) {
            case "detective": a.push(DetectiveScreen.start(a)); break;
            case "lab": a.push(LabScreen.start(a)); break;
            case "tribunal": a.push(TribunalScreen.start(a)); break;
            case "intruso": a.push(IntrusoScreen.start(a)); break;
            case "conexiones": a.push(ConexionesScreen.start(a)); break;
            case "arena": a.push(ArenaScreen.start(a)); break;
            case "aventura": a.push(AventuraScreen.start(a)); break;
            case "lluvia": a.push(LluviaScreen.start(a)); break;
        }
    }
}
