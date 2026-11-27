package com.valhallaschool.app;

import android.graphics.Bitmap;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.Locale;

import static com.valhallaschool.app.Ui.*;

/** Profile: level, library progress, weekly XP, topics, achievements, settings and backup. */
final class ProfileTab extends MainActivity.Tab {
    private LinearLayout content;
    private ScrollView sc;
    /** v1.2.3 — la cuadrícula de los 48 avatares sale colapsada por defecto. */
    private boolean avatarsOpen = false;

    ProfileTab(MainActivity a) { super(a); }

    @Override View build() {
        LinearLayout col = col(a);
        col.addView(pageTitle(a, "Perfil"));
        sc = new ScrollView(a);
        content = col(a);
        content.setPadding(dp(16), 0, dp(16), dp(32));
        sc.addView(content);
        col.addView(sc, lp(M, 0, 1));
        return col;
    }

    @Override void refresh() {
        Store S = Store.I;
        content.removeAllViews();
        int xp = S.totalXP(), lv = Store.level(xp), base = Store.levelBase(lv), next = Store.levelBase(lv + 1);

        // v1.3.2 — el cambiador de tema va AL INICIO, hasta ARRIBA del Perfil,
        // RESALTADO (borde dorado + fondo tintado): antes estaba escondido en Ajustes.
        themeCard();

        avatarSection();

        LinearLayout me = row(a);
        TextView av = tv(a, String.valueOf(lv), 28, ACCENT_INK, BLACK);
        av.setGravity(Gravity.CENTER);
        av.setBackground(oval(ACCENT));
        me.addView(av, lp(dp(72), dp(72)));
        LinearLayout mc = col(a);
        mc.setPadding(dp(16), 0, 0, 0);
        mc.addView(tv(a, "Nivel " + lv, 22, TEXT, BLACK));
        mc.addView(tv(a, (xp - base) + " / " + (next - base) + " XP para el nivel " + (lv + 1), 14, MUTED2, REG), lpm(M, W, 0, 3, 0, 0));
        mc.addView(new Views.Bar(a).set((xp - base) / (float) (next - base), ACCENT), lpm(M, dp(8), 0, 8, 0, 0));
        me.addView(mc, lp(0, W, 1));
        content.addView(me, lpm(M, W, 0, 4, 0, 18));

        accountSection();

        LinearLayout lib = box(a);
        LinearLayout lr = row(a);
        lr.addView(tv(a, "Progreso", 13, MUTED2, BOLD), lp(0, W, 1));
        lr.addView(tv(a, S.read.size() + " / " + Data.N + " ideas", 13, MUTED2, BOLD));
        lib.addView(lr);
        lib.addView(new Views.Bar(a).set(S.read.size() / (float) Data.N, GREEN), lpm(M, dp(14), 0, 10, 0, 0));
        content.addView(lib, lpm(M, W, 0, 0, 0, 12));

        int acc = S.qa > 0 ? Math.round(S.qc * 100f / S.qa) : 0;
        String[][] st = {
                {"🔥", String.valueOf(S.streak()), "días de racha"}, {"🏅", String.valueOf(S.best), "mejor racha"},
                {"📚", String.valueOf(S.booksDone()), "lecciones terminadas"}, {"⭐", String.valueOf(S.lessons.size()), "lecciones"},
                {"🎯", acc + "%", "precisión en quiz"}, {"🧠", String.valueOf(S.qc), "respuestas correctas"}};
        for (int i = 0; i < st.length; i += 2) {
            LinearLayout r = row(a);
            r.addView(LearnTab.stat(a, st[i][0], st[i][1], st[i][2]), lp(0, W, 1));
            LinearLayout.LayoutParams p = lpm(0, W, 10, 0, 0, 0);
            p.weight = 1;
            r.addView(LearnTab.stat(a, st[i + 1][0], st[i + 1][1], st[i + 1][2]), p);
            content.addView(r, lpm(M, W, 0, 0, 0, 10));
        }

        content.addView(section(a, "Esta semana"));
        content.addView(week());

        content.addView(section(a, "Módulos"));
        LinearLayout topics = box(a);
        for (int c = 0; c < Data.catName.length; c++) {
            int tot = 0, r = 0;
            for (int b = 0; b < Data.NB; b++) if (Data.bCat[b] == c) { tot += Data.PER; r += S.bookRead(b); }
            LinearLayout row = row(a);
            row.setPadding(0, dp(6), 0, dp(6));
            row.addView(one(tv(a, Data.catName[c], 14, TEXT, MEDIUM)), lp(dp(122), W));
            row.addView(new Views.Bar(a).set(r / (float) tot, Data.catColor[c]), lp(0, dp(6), 1));
            TextView n = tv(a, r + "/" + tot, 12, MUTED2, REG);
            n.setGravity(Gravity.END);
            row.addView(n, lp(dp(56), W));
            topics.addView(row);
        }
        content.addView(topics);

        content.addView(section(a, "Logros · " + S.ach.size() + "/" + Store.ACH.length));
        for (int i = 0; i < Store.ACH.length; i += 3) {
            LinearLayout r = row(a);
            r.setGravity(Gravity.TOP);
            for (int k = 0; k < 3; k++) {
                LinearLayout.LayoutParams p = lpm(0, M, k == 0 ? 0 : 10, 0, 0, 0);
                p.weight = 1;
                if (i + k >= Store.ACH.length) { r.addView(space(a), p); continue; }
                String[] ac = Store.ACH[i + k];
                boolean on = S.ach.containsKey(ac[0]);
                LinearLayout bd = col(a);
                bd.setGravity(Gravity.CENTER_HORIZONTAL);
                bd.setBackground(round(SURF, 16, LINE, 1));
                bd.setPadding(dp(6), dp(12), dp(6), dp(12));
                TextView ic = tv(a, ac[1], 30, TEXT, REG);
                ic.setAlpha(on ? 1f : 0.25f);
                bd.addView(ic);
                TextView nm = tv(a, ac[2], 12, on ? TEXT : MUTED2, BLACK);
                nm.setGravity(Gravity.CENTER);
                bd.addView(nm, lpm(W, W, 0, 6, 0, 0));
                TextView ds = tv(a, ac[3], 11, MUTED2, REG);
                ds.setGravity(Gravity.CENTER);
                bd.addView(ds, lpm(W, W, 0, 2, 0, 0));
                r.addView(bd, p);
            }
            content.addView(r, lpm(M, W, 0, 0, 0, 10));
        }

        content.addView(section(a, "Ajustes"));
        int gi = 1;
        for (int i = 0; i < Store.GOALS.length; i++) if (Store.GOALS[i] == S.goal) gi = i;
        setting("Meta diaria", seg(a, Store.GOAL_NAMES, gi, i -> { S.setGoal(Store.GOALS[i]); a.changed(); a.dirty(); }));
        setting("Sonidos", seg(a, new String[]{"Sí", "No"}, S.sound ? 0 : 1, i -> { S.sound = i == 0; S.save(); }));
        // v1.2.6 — recordatorio diario de racha (20:00, solo si la meta no está cumplida).
        setting("Recordatorio de racha", seg(a, new String[]{"Sí", "No"}, S.remind ? 0 : 1, i -> {
            S.remind = i == 0;
            S.save();
            if (S.remind) { StreakReminder.schedule(a); S.remindAsked = false; S.save(); a.askNotificationPermission(); }
            else StreakReminder.cancel(a);
            a.changed(); a.dirty();
        }));
        content.addView(link("Exportar respaldo", BLUE, v -> a.exportBackup()), lpm(M, W, 0, 10, 0, 0));
        content.addView(link("Importar respaldo", BLUE, v -> a.importBackup()));
        content.addView(link("Borrar todo el progreso", RED, v -> a.ask("¿Borrar todo?",
                "Esto borra el historial, las ideas guardadas, el XP, las rachas y las lecciones. Exporta un respaldo primero si quieres conservarlo.",
                "Borrar", a::resetAll, true)));
        // v1.2.3 — el pie «sin internet» se quitó por completo: sobraba.

        content.addView(section(a, "Créditos"));
        LinearLayout cr = box(a);
        cr.addView(tv(a, "Avatares de los 16 tipos sociónicos por Binky (@binkybee_art)", 14, TEXT, REG));
        TextView cr2 = tv(a, "Uso libre con su permiso. ¿Hiciste algo con ellos? Escríbele: binkybeemail@gmail.com", 13, MUTED2, REG);
        cr2.setLineSpacing(0, 1.3f);
        cr.addView(cr2, lpm(M, W, 0, 6, 0, 0));
        content.addView(cr);
    }

    /** Orden de las fichas de type como en el mockup aprobado (por cuadras). */
    private static final String[] TYPE_ORDER = {"ILE", "LII", "ESE", "SEI", "EIE",
            "IEI", "SLE", "LSI", "SEE", "ESI",
            "LSE", "SLI", "IEE", "EII", "LIE",
            "ILI"};

    /** v1.2.2 — Mi avatar: personaje + nickname + type, guardados en Store. */
    private void avatarSection() {
        Store S = Store.I;
        content.addView(section(a, "Mi avatar"));
        LinearLayout card = box(a);
        card.setGravity(Gravity.CENTER_HORIZONTAL);

        ImageView iv = new ImageView(a);
        Bitmap b = Avatars.load(a, Avatars.FILES[S.myAvatar], dp(256));
        if (b != null) {
            iv.setImageBitmap(b);
            iv.setAdjustViewBounds(true);
            iv.setScaleType(ImageView.ScaleType.FIT_CENTER);
        }
        card.addView(iv, lpm(dp(130), dp(130), 0, 0, 0, 8));

        TextView nick = tv(a, S.displayName(), 24, TEXT, BLACK);
        nick.setGravity(Gravity.CENTER);
        card.addView(nick, lp(M, W));

        TextView pill = tv(a, S.myType.isEmpty() ? "SIN TYPE" : S.myType + " · Mi type", 15, ACCENT_INK, BLACK);
        pill.setGravity(Gravity.CENTER);
        // v1.2.6 — dorado fijo + tinta oscura: 8.2:1 en los 16 temas.
        pill.setBackground(round(GOLD, 20));
        pill.setPadding(dp(16), dp(6), dp(16), dp(8));
        card.addView(pill, lpm(W, W, 0, 10, 0, 0));

        TextView sub = tv(a, "Tu avatar y tu type se ven en el ranking y cuando te animan los personajes.", 13, MUTED2, REG);
        sub.setGravity(Gravity.CENTER);
        sub.setLineSpacing(0, 1.3f);
        card.addView(sub, lpm(M, W, 0, 10, 0, 0));

        // v1.2.6 — texto del botón en TEXT (el acento del tema sobre SURF2
        // quedaba en 3.1:1 en Océano/Lluvia; TEXT da ≥8.9:1 en los 16 temas).
        TextView toggle = tv(a, avatarsOpen ? "Ocultar avatares ▴" : "Elige tu personaje · 48 avatares ▾", 14, TEXT, BLACK);
        toggle.setGravity(Gravity.CENTER);
        toggle.setBackground(round(SURF2, 12, LINE, 1));
        toggle.setPadding(dp(14), dp(12), dp(14), dp(12));
        toggle.setOnClickListener(v -> { avatarsOpen = !avatarsOpen; refresh(); });
        ripple(toggle, true);
        card.addView(toggle, lpm(M, W, 0, 18, 0, 10));
        if (avatarsOpen) {
        for (int r = 0; r < 12; r++) {
            LinearLayout row = row(a);
            for (int c = 0; c < 4; c++) {
                int idx = r * 4 + c;
                boolean sel = idx == S.myAvatar;
                LinearLayout cell = col(a);
                cell.setGravity(Gravity.CENTER_HORIZONTAL);
                cell.setBackground(round(SURF2, 14, sel ? ACCENT : LINE, sel ? 3 : 1));
                cell.setPadding(dp(4), dp(6), dp(4), dp(6));
                ImageView th = new ImageView(a);
                Bitmap tb = Avatars.thumb(a, Avatars.FILES[idx]);
                if (tb != null) {
                    th.setImageBitmap(tb);
                    th.setAdjustViewBounds(true);
                    th.setScaleType(ImageView.ScaleType.FIT_CENTER);
                }
                cell.addView(th, lp(M, dp(64)));
                TextView tl = tv(a, Avatars.FILES[idx].substring(0, 3), 11, MUTED2, REG);
                tl.setGravity(Gravity.CENTER);
                cell.addView(tl, lpm(M, W, 0, 2, 0, 0));
                final int fi = idx;
                cell.setOnClickListener(v -> { S.myAvatar = fi; S.save(); refresh(); });
                ripple(cell, true);
                LinearLayout.LayoutParams p = lpm(0, W, c == 0 ? 0 : 8, 0, 0, 8);
                p.weight = 1;
                row.addView(cell, p);
            }
            card.addView(row, lp(M, W));
        }
        } // fin del desplegable de avatares

        TextView tl2 = tv(a, "Tu type", 13, MUTED2, REG);
        card.addView(tl2, lpm(M, W, 0, 10, 0, 10));
        for (int r = 0; r < 4; r++) {
            LinearLayout row = row(a);
            for (int c = 0; c < 4; c++) {
                String t = TYPE_ORDER[r * 4 + c];
                boolean sel = t.equals(S.myType);
                // v1.2.6 — chips de type legibles en los 16 temas: sin seleccionar,
                // texto DORADO sobre superficie oscura (≥4.0:1); seleccionado,
                // fondo dorado con tinta oscura (8.2:1). Antes era texto blanco
                // sobre píldora casi blanca (1.2:1, no se leía nada).
                TextView chip = tv(a, t, 14, sel ? ACCENT_INK : GOLD_TXT, BLACK);
                chip.setGravity(Gravity.CENTER);
                chip.setBackground(sel ? round(GOLD, 20) : round(SURF2, 20, LINE, 1));
                chip.setPadding(dp(10), dp(10), dp(10), dp(10));
                final String ft = t;
                chip.setOnClickListener(v -> { S.myType = ft; S.save(); refresh(); });
                ripple(chip, true);
                LinearLayout.LayoutParams p = lpm(0, W, c == 0 ? 0 : 8, 0, 0, 8);
                p.weight = 1;
                row.addView(chip, p);
            }
            card.addView(row, lp(M, W));
        }

        TextView nl = tv(a, "Nickname", 13, MUTED2, REG);
        card.addView(nl, lpm(M, W, 0, 10, 0, 6));
        EditText ne = new EditText(a);
        ne.setHint("Sin nombre");
        ne.setHintTextColor(MUTED2);
        ne.setTextColor(TEXT);
        ne.setTextSize(16);
        ne.setSingleLine(true);
        ne.setText(S.nickname);
        ne.setBackground(round(SURF2, 12, LINE, 1));
        ne.setPadding(dp(14), dp(12), dp(14), dp(12));
        ne.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a2) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {}
            @Override public void afterTextChanged(Editable s) {
                S.nickname = s.toString().trim();
                S.save();
                nick.setText(S.displayName());
            }
        });
        card.addView(ne, lp(M, W));

        content.addView(card, lpm(M, W, 0, 0, 0, 12));
    }

    /** Cuenta de Valhalla School (Supabase) + ranking. */
    private void accountSection() {
        content.addView(section(a, "Cuenta"));
        LinearLayout box = box(a);
        if (SupabaseAuth.INSTANCE.isSignedIn()) {
            SupabaseAuth.Session s = SupabaseAuth.INSTANCE.currentSession();
            String email = s != null && s.getEmail() != null ? s.getEmail() : "Conectado";
            box.addView(tv(a, email, 16, TEXT, BLACK));
            box.addView(tv(a, "Tu progreso se guarda en la nube.", 13, MUTED2, REG), lpm(M, W, 0, 4, 0, 10));
            box.addView(link("Cerrar sesión", RED, v -> { SupabaseAuth.INSTANCE.signOut(); refresh(); }));
            content.addView(box, lpm(M, W, 0, 0, 0, 12));
        } else {
            EditText em = new EditText(a);
            em.setHint("Correo");
            em.setHintTextColor(MUTED2);
            em.setTextColor(TEXT);
            em.setTextSize(16);
            em.setSingleLine(true);
            em.setBackground(round(SURF2, 12, LINE, 1));
            em.setPadding(dp(14), dp(12), dp(14), dp(12));
            box.addView(em, lp(M, W));
            EditText pw = new EditText(a);
            pw.setHint("Contraseña (mín. 6 caracteres)");
            pw.setHintTextColor(MUTED2);
            pw.setTextColor(TEXT);
            pw.setTextSize(16);
            pw.setSingleLine(true);
            pw.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
            pw.setBackground(round(SURF2, 12, LINE, 1));
            pw.setPadding(dp(14), dp(12), dp(14), dp(12));
            box.addView(pw, lpm(M, W, 0, 10, 0, 0));
            TextView st = tv(a, "Crea tu cuenta o entra: tu progreso se guarda en la nube.", 13, MUTED2, REG);
            box.addView(st, lpm(M, W, 0, 10, 0, 10));
            LinearLayout bs = row(a);
            bs.addView(btn(a, "Crear cuenta", B_YELLOW, v -> authGo(em.getText().toString().trim(), pw.getText().toString(), true, st)), lp(0, W, 1));
            LinearLayout.LayoutParams p2 = lpm(0, W, 10, 0, 0, 0);
            p2.weight = 1;
            bs.addView(btn(a, "Entrar", B_GREEN, v -> authGo(em.getText().toString().trim(), pw.getText().toString(), false, st)), p2);
            box.addView(bs, lp(M, W));
            content.addView(box, lpm(M, W, 0, 0, 0, 12));
        }
        // v1.2.3 — el Ranking se ve SIEMPRE: con sesión, el ranking normal;
        // sin sesión, una tarjeta que explica que la nube necesita cuenta.
        if (SupabaseAuth.INSTANCE.isSignedIn()) rankingBox();
        else rankingNoSession(box);
    }

    /** Ranking sin sesión: explica que la nube y el ranking necesitan cuenta. */
    private void rankingNoSession(final View accountBox) {
        content.addView(section(a, "Ranking"));
        LinearLayout box = box(a);
        box.addView(tv(a, "Sin cuenta no hay ranking.", 16, TEXT, BLACK));
        TextView t = tv(a, "Tu progreso se guarda en este teléfono, pero el ranking y la nube necesitan cuenta. Crea tu cuenta o entra para que tu XP aparezca en el ranking.", 13, MUTED2, REG);
        t.setLineSpacing(0, 1.3f);
        box.addView(t, lpm(M, W, 0, 6, 0, 10));
        box.addView(btn(a, "Crear cuenta o entrar", B_YELLOW, v -> sc.post(() -> sc.smoothScrollTo(0, accountBox.getTop()))), lp(M, W));
        content.addView(box, lpm(M, W, 0, 0, 0, 12));
    }

    private void authGo(String email, String pass, boolean signup, TextView st) {
        if (email.isEmpty() || pass.length() < 6) {
            st.setText("Escribe un correo y una contraseña de 6 caracteres o más.");
            st.setTextColor(RED);
            return;
        }
        st.setText("Conectando…");
        st.setTextColor(MUTED2);
        Callback<SupabaseAuth.Session> cb = (session, err) -> {
            a.runOnUiThread(() -> {
                if (err == null) {
                    a.toast("¡Bienvenido a Valhalla School!", false);
                    refresh();
                    // v1.2.3 — espejo en la nube al entrar: antes, al iniciar sesión
                    // nada se subía hasta terminar otra lección, así que el ranking
                    // no te veía. Ahora tu XP y nivel se suben de inmediato.
                    try {
                        Store S = Store.I;
                        GameProgressSync.INSTANCE.submitScore(S.totalXP(), Store.level(S.totalXP()), S.displayName(), (v, e2) -> {});
                    } catch (Exception ignored) { /* la nube es espejo, no bloquea */ }
                } else {
                    st.setText(err.getMessage() != null ? err.getMessage() : "Error al entrar.");
                    st.setTextColor(RED);
                }
            });
        };
        if (signup) SupabaseAuth.INSTANCE.signUp(email, pass, cb);
        else SupabaseAuth.INSTANCE.signIn(email, pass, cb);
    }

    private void rankingBox() {
        content.addView(section(a, "Ranking"));
        LinearLayout box = box(a);
        box.addView(tv(a, "Cargando ranking…", 14, MUTED2, REG));
        content.addView(box, lpm(M, W, 0, 0, 0, 12));
        GameProgressSync.INSTANCE.fetchLeaderboard(10, (list, err) -> {
            a.runOnUiThread(() -> {
                box.removeAllViews();
                if (err != null || list == null || list.isEmpty()) {
                    box.addView(tv(a, "Aún no hay ranking. ¡Sé el primero!", 14, MUTED2, REG));
                    return;
                }
                int i = 1;
                for (GameProgressSync.ScoreEntry e : list) {
                    String name = e.getUsername() != null ? e.getUsername() : "Jugador";
                    if (name.contains("@")) name = name.substring(0, name.indexOf('@'));
                    LinearLayout r = row(a);
                    r.addView(tv(a, "#" + i, 14, ACCENT, BLACK), lp(dp(36), W));
                    r.addView(one(tv(a, name, 14, TEXT, MEDIUM)), lp(0, W, 1));
                    r.addView(tv(a, e.getTotalXp() + " XP · Nv " + e.getLevel(), 13, MUTED2, REG));
                    box.addView(r, lpm(M, W, 0, 6, 0, 6));
                    i++;
                }
            });
        });
    }

    private View week() {
        Store S = Store.I;
        LinearLayout b = box(a);
        LinearLayout r = row(a);
        r.setGravity(Gravity.BOTTOM);
        int t = Store.today(), mx = S.goal;
        for (int d = t - 6; d <= t; d++) mx = Math.max(mx, S.xpOn(d));
        for (int d = t - 6; d <= t; d++) {
            int v = S.xpOn(d);
            LinearLayout c = col(a);
            c.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM);
            c.addView(tv(a, v > 0 ? String.valueOf(v) : "", 11, TEXT, BLACK), lpm(W, W, 0, 0, 0, 4));
            View bar = new View(a);
            bar.setBackground(round(S.met.contains(d) ? GREEN : v > 0 ? ACCENT : LINE, 5));
            c.addView(bar, lp(dp(26), Math.max(dp(4), Math.round(dp(100) * v / (float) mx))));
            c.addView(tv(a, LocalDate.ofEpochDay(d).getDayOfWeek().getDisplayName(TextStyle.NARROW, Locale.getDefault()), 11, MUTED2, BOLD), lpm(W, W, 0, 6, 0, 0));
            r.addView(c, lp(0, dp(136), 1));
        }
        b.addView(r, lp(M, W));
        return b;
    }

    /** v1.3.2 — tarjeta de tema DESTACADA arriba del Perfil: el tema se
     *  aplica en toda la app (lecciones, lector, hubs, juegos, Perfil). */
    private void themeCard() {
        Store S = Store.I;
        content.addView(section(a, "🎨 Tema"));
        LinearLayout card = box(a);
        card.setBackground(round(mix(ACCENT, SURF, 0.18f), 16, ACCENT, 3));
        card.setPadding(dp(16), dp(16), dp(16), dp(16));
        card.addView(tv(a, "Tema actual: " + Ui.themeName(S.theme), 18, TEXT, BLACK));
        TextView sub = tv(a, "El tema se aplica en toda la app: lecciones, juegos y perfil.", 13, MUTED2, REG);
        sub.setLineSpacing(0, 1.3f);
        card.addView(sub, lpm(M, W, 0, 4, 0, 12));
        card.addView(themeButton());
        ripple(card, true);
        card.setOnClickListener(v -> a.showThemePicker());
        content.addView(card, lpm(M, W, 0, 0, 0, 12));
    }

    /** Botón desplegable del tema activo (abre el selector de 16 temas). */
    private View themeButton() {
        Store S = Store.I;
        LinearLayout b = box(a);
        LinearLayout r = row(a);
        View dot = new View(a);
        android.graphics.drawable.GradientDrawable od = oval(Ui.THEME_PAL[Ui.themeIndex(S.theme)][2]);
        od.setSize(dp(26), dp(26));
        dot.setBackground(od);
        r.addView(dot, lp(dp(26), dp(26)));
        TextView nm = tv(a, Ui.themeName(S.theme), 16, TEXT, MEDIUM);
        nm.setPadding(dp(14), 0, 0, 0);
        r.addView(nm, lp(0, W, 1));
        r.addView(tv(a, "▾", 18, MUTED2, BOLD));
        b.addView(r);
        ripple(b, true);
        b.setOnClickListener(v -> a.showThemePicker());
        return b;
    }

    private void setting(String label, View control) {
        content.addView(tv(a, label, 15, TEXT, MEDIUM), lpm(M, W, 0, 6, 0, 8));
        content.addView(control, lpm(M, W, 0, 0, 0, 14));
    }

    private TextView link(String s, int color, View.OnClickListener l) {
        TextView t = ripple(tv(a, s, 15, color, BOLD), true);
        t.setPadding(dp(4), dp(14), dp(4), dp(14));
        t.setOnClickListener(l);
        return t;
    }
}
