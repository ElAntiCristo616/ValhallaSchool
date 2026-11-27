package com.valhallaschool.app;

import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.graphics.drawable.GradientDrawable;

import java.util.Locale;

import static com.valhallaschool.app.Ui.*;

/**
 * Base de los 8 minijuegos (v1.3.0, NATIVOS, cero WebView/H5P): el mismo
 * andamiaje de PracticeScreen (barra superior, acompañante grande,
 * tarjeta, opciones, pie con retroalimentación) para que los juegos se
 * sientan parte de la app. El XP se da al acertar cada ítem la PRIMERA
 * vez (Store.awardGameXP, 10 XP, silencioso): repetir no repite XP.
 * Los títulos de fin humillan por nombre (orden de Remy 2026-11-27).
 */
abstract class MiniGame extends MainActivity.Screen {
    protected static final int ASK = 1, FEEDBACK = 2, END = 3;

    protected final String gameId;
    protected final Views.Bar bar;
    protected final TextView comboTx, fbTitle, fbText, btn;
    protected final LinearLayout main, foot;
    protected final View divider;
    protected final ScrollView sc;
    protected Companion companion;
    protected int done, total, firstOk, answered, xpEarned, phase;
    protected final long t0 = System.currentTimeMillis();

    MiniGame(MainActivity a, String gameId, int total) {
        super(a);
        up = true;
        this.gameId = gameId;
        this.total = total;

        LinearLayout col = col(a);
        LinearLayout top = row(a);
        top.setPadding(dp(6), dp(8), dp(14), dp(6));
        top.addView(iconBtn(a, R.drawable.ic_close, MUTED, x -> close()));
        bar = new Views.Bar(a);
        LinearLayout.LayoutParams bl = lpm(0, dp(14), 6, 0, 10, 0);
        bl.weight = 1;
        top.addView(bar, bl);
        comboTx = tv(a, "", 14, ACCENT, BLACK);
        comboTx.setGravity(Gravity.END);
        top.addView(comboTx, lp(dp(52), W));
        col.addView(top, lp(M, W));

        sc = new ScrollView(a);
        main = col(a);
        main.setPadding(dp(18), dp(12), dp(18), dp(20));
        sc.addView(main);
        col.addView(sc, lp(M, 0, 1));

        divider = new View(a);
        divider.setBackgroundColor(LINE);
        col.addView(divider, lp(M, dp(2)));
        foot = col(a);
        foot.setPadding(dp(18), dp(16), dp(18), dp(18));
        fbTitle = tv(a, "", 20, GREEN, BLACK);
        fbText = tv(a, "", 15, TEXT, REG);
        fbText.setLineSpacing(0, 1.25f);
        foot.addView(fbTitle);
        foot.addView(fbText, lpm(M, W, 0, 4, 0, 0));
        btn = Ui.btn(a, "Continuar", B_GREEN, x -> onBtn());
        foot.addView(btn, lpm(M, W, 0, 14, 0, 0));
        col.addView(foot, lp(M, W));
        v = col;
    }

    /* ---------------- helpers comunes ---------------- */
    protected void close() {
        if (phase == END) { onBtn(); return; }
        a.ask("¿Salir del juego?", "Tu progreso queda guardado.", "Salir", a::back, true);
    }

    @Override boolean onBack() { close(); return true; }

    protected void updateBar() {
        bar.set(total <= 0 ? 0 : done / (float) total, GREEN);
    }

    protected void setFoot(int state) {
        foot.setBackgroundColor(state == 1 ? mix(GREEN, BG, 0.16f) : state == 2 ? mix(RED, BG, 0.14f) : BG);
        divider.setVisibility(state == 0 ? View.VISIBLE : View.INVISIBLE);
        fbTitle.setVisibility(state == 0 ? View.GONE : View.VISIBLE);
        fbText.setVisibility(state == 2 ? View.VISIBLE : View.GONE);
    }

    protected void setBtn(String label, int style) {
        btn.setText(label.toUpperCase(Locale.ROOT));
        styleBtn(btn, style);
    }

    /** Acompañante grande con bocadillo (humilla por nombre). */
    protected void addCompanion() {
        companion = new Companion(a);
        main.addView(companion.root(), lpm(M, W, 0, 0, 0, 4));
        companion.next();
    }

    protected void reactCompanion(boolean ok) {
        if (companion != null) companion.react(ok);
    }

    /** 0 idle, 1 selected, 2 correct, 3 wrong. */
    protected static void optStyle(TextView o, int s) {
        int border = s == 1 ? BLUE : s == 2 ? GREEN : s == 3 ? RED : LINE;
        int fill = s == 1 ? mix(BLUE, SURF, 0.12f) : s == 2 ? mix(GREEN, SURF, 0.14f) : s == 3 ? mix(RED, SURF, 0.12f) : SURF;
        o.setBackground(raised(fill, border, 16, 3, border));
    }

    /** Opción grande (mínimo 44px de alto, por toque). */
    protected TextView optBtn(String text) {
        TextView o = tv(a, text, 16, TEXT, MEDIUM);
        o.setPadding(dp(16), dp(14), dp(16), dp(17));
        o.setMinHeight(dp(52));
        optStyle(o, 0);
        return o;
    }

    /** +10 XP la primera vez que se acierta este ítem; false si ya había dado XP. */
    protected boolean award(String itemId) {
        if (Store.I.awardGameXP(itemId)) { xpEarned += Store.XP_Q; return true; }
        return false;
    }

    protected String name() { return Store.I.displayName(); }

    /** Título de fin: humillación por nombre, nunca elogio sincero. */
    protected String endTitle(boolean good) {
        return good ? "¡Lo lograste, " + name() + "! ¿Casualidad?"
                : "¡Qué desastre, " + name() + "!";
    }

    /** Tarjeta de contexto (título pequeño + cuerpo). */
    protected LinearLayout card(String tag, String body) {
        LinearLayout ctx = col(a);
        ctx.setBackground(round(SURF, 18, LINE, 2));
        ctx.setPadding(dp(16), dp(16), dp(16), dp(16));
        if (tag != null) {
            TextView t = tv(a, tag, 13, BLUE, BLACK);
            t.setLetterSpacing(0.08f);
            ctx.addView(t, lpm(M, W, 0, 0, 0, 8));
        }
        TextView bd = tv(a, body, 17, TEXT, REG);
        bd.setLineSpacing(0, 1.3f);
        ctx.addView(bd);
        return ctx;
    }

    /** Pantalla final: XP ganado, precisión y tiempo; guarda el mejor puntaje. */
    protected void finish(String subtitle) {
        phase = END;
        int acc = answered > 0 ? Math.round(firstOk * 100f / answered) : 100;
        boolean good = acc >= 70;
        long secs = (System.currentTimeMillis() - t0) / 1000;

        LinearLayout fin = col(a);
        fin.setGravity(Gravity.CENTER_HORIZONTAL);
        fin.setPadding(0, dp(48), 0, 0);
        fin.addView(Avatars.view(a, 120), lpm(W, W, 0, 0, 0, 4));
        fin.addView(tv(a, good ? "🏆" : "🎯", 84, TEXT, REG));
        fin.addView(tv(a, endTitle(good), 28, ACCENT, BLACK), lpm(W, W, 0, 10, 0, 0));
        TextView p = tv(a, subtitle, 15, MUTED, REG);
        p.setGravity(Gravity.CENTER);
        fin.addView(p, lpm(W, W, 0, 10, 0, 24));
        LinearLayout tiles = row(a);
        tiles.addView(tile("XP TOTAL", String.valueOf(xpEarned), ACCENT), lp(0, W, 1));
        LinearLayout.LayoutParams p2 = lpm(0, W, 10, 0, 0, 0), p3 = lpm(0, W, 10, 0, 0, 0);
        p2.weight = 1;
        p3.weight = 1;
        tiles.addView(tile("PRECISIÓN", acc + "%", GREEN), p2);
        tiles.addView(tile("TIEMPO", (secs / 60) + ":" + String.format(Locale.ROOT, "%02d", secs % 60), BLUE), p3);
        fin.addView(tiles, lp(M, W));
        main.addView(fin, lp(M, W));

        setFoot(0);
        setBtn("Continuar", B_GREEN);
        updateBar();
        Sfx.play(Sfx.WIN);
        a.confetti();
        Store.I.setGameBest(gameId, acc);
        if (xpEarned > 0) a.toast("+" + xpEarned + " XP", true);
        a.dirty();
    }

    private LinearLayout tile(String label, String value, int color) {
        LinearLayout t = col(a);
        t.setBackground(round(SURF, 16, color, 2));
        TextView l = tv(a, label, 11, 0xff111111, BLACK);
        l.setGravity(Gravity.CENTER);
        l.setLetterSpacing(0.06f);
        l.setPadding(0, dp(5), 0, dp(5));
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        float r = dp(14);
        g.setCornerRadii(new float[]{r, r, r, r, 0, 0, 0, 0});
        l.setBackground(g);
        t.addView(l, lp(M, W));
        TextView v = tv(a, value, 20, TEXT, BLACK);
        v.setGravity(Gravity.CENTER);
        v.setPadding(0, dp(12), 0, dp(12));
        t.addView(v, lp(M, W));
        return t;
    }

    protected void hapticBad() {
        Sfx.play(Sfx.BAD);
        v.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
    }

    protected abstract void next();
    protected abstract void onBtn();
}
