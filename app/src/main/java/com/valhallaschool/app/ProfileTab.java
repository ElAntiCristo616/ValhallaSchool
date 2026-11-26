package com.valhallaschool.app;

import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
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

        LinearLayout me = row(a);
        TextView av = tv(a, String.valueOf(lv), 28, ACCENT_INK, BLACK);
        av.setGravity(Gravity.CENTER);
        av.setBackground(oval(ACCENT));
        me.addView(av, lp(dp(72), dp(72)));
        LinearLayout mc = col(a);
        mc.setPadding(dp(16), 0, 0, 0);
        mc.addView(tv(a, "Nivel " + lv, 22, TEXT, BLACK));
        mc.addView(tv(a, (xp - base) + " / " + (next - base) + " XP para el nivel " + (lv + 1), 14, MUTED, REG), lpm(M, W, 0, 3, 0, 0));
        mc.addView(new Views.Bar(a).set((xp - base) / (float) (next - base), ACCENT), lpm(M, dp(8), 0, 8, 0, 0));
        me.addView(mc, lp(0, W, 1));
        content.addView(me, lpm(M, W, 0, 4, 0, 18));

        accountSection();

        LinearLayout lib = box(a);
        LinearLayout lr = row(a);
        lr.addView(tv(a, "Progreso", 13, MUTED, BOLD), lp(0, W, 1));
        lr.addView(tv(a, S.read.size() + " / " + Data.N + " ideas", 13, MUTED, BOLD));
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
            TextView n = tv(a, r + "/" + tot, 12, MUTED, REG);
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
                TextView nm = tv(a, ac[2], 12, on ? TEXT : MUTED, BLACK);
                nm.setGravity(Gravity.CENTER);
                bd.addView(nm, lpm(W, W, 0, 6, 0, 0));
                TextView ds = tv(a, ac[3], 11, MUTED, REG);
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
        setting("Tema", seg(a, new String[]{"Oscuro", "Claro"}, S.dark ? 0 : 1, i -> a.setTheme(i == 0)));
        setting("Sonidos", seg(a, new String[]{"Sí", "No"}, S.sound ? 0 : 1, i -> { S.sound = i == 0; S.save(); }));
        content.addView(link("Exportar respaldo", BLUE, v -> a.exportBackup()), lpm(M, W, 0, 10, 0, 0));
        content.addView(link("Importar respaldo", BLUE, v -> a.importBackup()));
        content.addView(link("Borrar todo el progreso", RED, v -> a.ask("¿Borrar todo?",
                "Esto borra el historial, las ideas guardadas, el XP, las rachas y las lecciones. Exporta un respaldo primero si quieres conservarlo.",
                "Borrar", a::resetAll, true)));
        TextView foot = tv(a, Data.NB + " lecciones · " + Data.N + " ideas · sin internet", 12, MUTED, REG);
        foot.setGravity(Gravity.CENTER);
        content.addView(foot, lpm(M, W, 0, 24, 0, 0));
    }

    /** Cuenta de Valhalla School (Supabase) + ranking. */
    private void accountSection() {
        content.addView(section(a, "Cuenta"));
        LinearLayout box = box(a);
        if (SupabaseAuth.INSTANCE.isSignedIn()) {
            SupabaseAuth.Session s = SupabaseAuth.INSTANCE.currentSession();
            String email = s != null && s.getEmail() != null ? s.getEmail() : "Conectado";
            box.addView(tv(a, email, 16, TEXT, BLACK));
            box.addView(tv(a, "Tu progreso se guarda en la nube.", 13, MUTED, REG), lpm(M, W, 0, 4, 0, 10));
            box.addView(link("Cerrar sesión", RED, v -> { SupabaseAuth.INSTANCE.signOut(); refresh(); }));
            content.addView(box, lpm(M, W, 0, 0, 0, 12));
            rankingBox();
        } else {
            EditText em = new EditText(a);
            em.setHint("Correo");
            em.setHintTextColor(MUTED);
            em.setTextColor(TEXT);
            em.setTextSize(16);
            em.setSingleLine(true);
            em.setBackground(round(SURF2, 12, LINE, 1));
            em.setPadding(dp(14), dp(12), dp(14), dp(12));
            box.addView(em, lp(M, W));
            EditText pw = new EditText(a);
            pw.setHint("Contraseña (mín. 6 caracteres)");
            pw.setHintTextColor(MUTED);
            pw.setTextColor(TEXT);
            pw.setTextSize(16);
            pw.setSingleLine(true);
            pw.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
            pw.setBackground(round(SURF2, 12, LINE, 1));
            pw.setPadding(dp(14), dp(12), dp(14), dp(12));
            box.addView(pw, lpm(M, W, 0, 10, 0, 0));
            TextView st = tv(a, "Crea tu cuenta o entra: tu progreso se guarda en la nube.", 13, MUTED, REG);
            box.addView(st, lpm(M, W, 0, 10, 0, 10));
            LinearLayout bs = row(a);
            bs.addView(btn(a, "Crear cuenta", B_YELLOW, v -> authGo(em.getText().toString().trim(), pw.getText().toString(), true, st)), lp(0, W, 1));
            LinearLayout.LayoutParams p2 = lpm(0, W, 10, 0, 0, 0);
            p2.weight = 1;
            bs.addView(btn(a, "Entrar", B_GREEN, v -> authGo(em.getText().toString().trim(), pw.getText().toString(), false, st)), p2);
            box.addView(bs, lp(M, W));
            content.addView(box, lpm(M, W, 0, 0, 0, 12));
        }
    }

    private void authGo(String email, String pass, boolean signup, TextView st) {
        if (email.isEmpty() || pass.length() < 6) {
            st.setText("Escribe un correo y una contraseña de 6 caracteres o más.");
            st.setTextColor(RED);
            return;
        }
        st.setText("Conectando…");
        st.setTextColor(MUTED);
        kotlin.jvm.functions.Function1<kotlin.Result<SupabaseAuth.Session>, kotlin.Unit> cb = result -> {
            a.runOnUiThread(() -> {
                if (result.isSuccess()) {
                    a.toast("¡Bienvenido a Valhalla School!", false);
                    refresh();
                } else {
                    Throwable e = result.exceptionOrNull();
                    st.setText(e != null && e.getMessage() != null ? e.getMessage() : "Error al entrar.");
                    st.setTextColor(RED);
                }
            });
            return kotlin.Unit.INSTANCE;
        };
        if (signup) SupabaseAuth.INSTANCE.signUp(email, pass, cb);
        else SupabaseAuth.INSTANCE.signIn(email, pass, cb);
    }

    private void rankingBox() {
        content.addView(section(a, "Ranking"));
        LinearLayout box = box(a);
        box.addView(tv(a, "Cargando ranking…", 14, MUTED, REG));
        content.addView(box, lpm(M, W, 0, 0, 0, 12));
        GameProgressSync.INSTANCE.fetchLeaderboard(10, result -> {
            a.runOnUiThread(() -> {
                box.removeAllViews();
                if (result.isFailure() || result.getOrNull() == null || result.getOrNull().isEmpty()) {
                    box.addView(tv(a, "Aún no hay ranking. ¡Sé el primero!", 14, MUTED, REG));
                    return;
                }
                int i = 1;
                for (GameProgressSync.ScoreEntry e : result.getOrNull()) {
                    String name = e.getUsername() != null ? e.getUsername() : "Jugador";
                    if (name.contains("@")) name = name.substring(0, name.indexOf('@'));
                    LinearLayout r = row(a);
                    r.addView(tv(a, "#" + i, 14, ACCENT, BLACK), lp(dp(36), W));
                    r.addView(one(tv(a, name, 14, TEXT, MEDIUM)), lp(0, W, 1));
                    r.addView(tv(a, e.getTotalXp() + " XP · Nv " + e.getLevel(), 13, MUTED, REG));
                    box.addView(r, lpm(M, W, 0, 6, 0, 6));
                    i++;
                }
            });
            return kotlin.Unit.INSTANCE;
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
            c.addView(tv(a, LocalDate.ofEpochDay(d).getDayOfWeek().getDisplayName(TextStyle.NARROW, Locale.getDefault()), 11, MUTED, BOLD), lpm(W, W, 0, 6, 0, 0));
            r.addView(c, lp(0, dp(136), 1));
        }
        b.addView(r, lp(M, W));
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
