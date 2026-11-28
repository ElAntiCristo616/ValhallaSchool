package com.valhallaschool.app;

import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import static com.valhallaschool.app.Ui.*;

/**
 * Loop: modo infinito, teoría + práctica revueltas al azar (~50/50).
 * El mazo se rebaraja solo cuando se agota: no hay fin, no hay niveles.
 *
 * Aporta a la racha diaria (v1.2.5): cada acierto suma 10 XP (Store.XP_Q,
 * como Práctica) a la meta diaria, así jugar Loop marca el día como
 * cumplido y mantiene la racha. SIN gemas, SIN logros nuevos por Loop,
 * SIN qa/qc (para no contaminar «Maestro del quiz»), SIN nube; no
 * guarda nada más en Store. Solo correcto/incorrecto +
 * explicación/justificación. Fallar NO reencola: la carta se va y
 * viene otra nueva.
 */
final class LoopScreen extends MainActivity.Screen {
    private static final int ASK = 1, FEEDBACK = 2;
    /** Título al acertar: humillación por nombre, nunca elogio sincero (orden de Remy 2026-11-27). */

    /** Una carta del mazo: de teoría (Quiz.Q) o de práctica (PracticeData.Ex). */
    private static final class Card {
        String tag, title, body, explain;
        String[] opts;
        int answer;

        static Card fromPractice(PracticeData.Ex e) {
            Card c = new Card();
            c.tag = "PERSONAJE ANÓNIMO · " + e.kindLabel();
            c.title = "¿Qué función en qué posición muestra esto?";
            c.body = e.prompt;
            ArrayList<String> all = new ArrayList<>();
            all.add(e.answerLabel());
            all.add(e.label(e.d1El, e.d1Pos));
            all.add(e.label(e.d2El, e.d2Pos));
            all.add(e.label(e.d3El, e.d3Pos));
            Collections.shuffle(all, Quiz.R);
            c.opts = all.toArray(new String[0]);
            c.answer = all.indexOf(e.answerLabel());
            c.explain = e.justification;
            return c;
        }

        static Card fromTheory(Quiz.Q q) {
            Card c = new Card();
            c.tag = "TEORÍA";
            c.title = q.prompt;
            StringBuilder b = new StringBuilder();
            if (q.head != null) b.append(q.head).append("\n\n");
            if (q.body != null) b.append(q.body);
            c.body = b.toString();
            c.opts = q.opts;
            c.answer = q.answer;
            c.explain = q.explain;
            return c;
        }
    }

    private final ArrayDeque<Card> deck = new ArrayDeque<>();
    private int asked, correct, streak, sel = -1, phase;
    private Card item;
    private Companion companion;
    private TextView[] opts;

    private final TextView countTx, fbTitle, fbText, btn;
    private final LinearLayout main, foot;
    private final View divider;
    private final ScrollView sc;

    static void start(MainActivity a) {
        a.push(new LoopScreen(a));
    }

    private LoopScreen(MainActivity a) {
        super(a);
        up = true;

        LinearLayout col = col(a);
        LinearLayout top = row(a);
        top.setPadding(dp(6), dp(8), dp(14), dp(6));
        top.addView(iconBtn(a, R.drawable.ic_close, MUTED, x -> a.back()));
        TextView loopTx = tv(a, "🔁 Loop", 16, ACCENT, BLACK);
        loopTx.setGravity(Gravity.CENTER);
        top.addView(loopTx, lp(0, W, 1));
        countTx = tv(a, "", 14, MUTED, BOLD);
        countTx.setGravity(Gravity.END);
        top.addView(countTx, lp(W, W));
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
        refill();
        next();
    }

    /** Mazo nuevo: 217 ejercicios de práctica + 217 preguntas de teoría, revueltos. */
    private void refill() {
        ArrayList<Card> cards = new ArrayList<>();
        ArrayList<PracticeData.Ex> exs = new ArrayList<>(PracticeData.ALL);
        Collections.shuffle(exs, Quiz.R);
        for (PracticeData.Ex e : exs) cards.add(Card.fromPractice(e));
        ArrayList<Integer> ids = new ArrayList<>();
        for (int i = 0; i < Data.N; i++) ids.add(i);
        Collections.shuffle(ids, Quiz.R);
        List<Integer> pick = ids.subList(0, Math.min(exs.size(), ids.size()));
        for (Quiz.Q q : Quiz.make(pick, true)) cards.add(Card.fromTheory(q));
        Collections.shuffle(cards, Quiz.R);
        deck.addAll(cards);
    }

    private void updateCount() {
        countTx.setText(asked + " · " + correct + " ✓" + (streak >= 2 ? " · " + streak + "×🔥" : ""));
    }

    private void setFoot(int state) {
        foot.setBackgroundColor(state == 1 ? mix(GREEN, BG, 0.16f) : state == 2 ? mix(RED, BG, 0.14f) : BG);
        divider.setVisibility(state == 0 ? View.VISIBLE : View.INVISIBLE);
        fbTitle.setVisibility(state == 0 ? View.GONE : View.VISIBLE);
        fbText.setVisibility(state == 0 ? View.GONE : View.VISIBLE);
    }

    private void setBtn(String label, int style) {
        btn.setText(label.toUpperCase(Locale.ROOT));
        styleBtn(btn, style);
    }

    private void next() {
        updateCount();
        setFoot(0);
        sc.scrollTo(0, 0);
        main.removeAllViews();
        if (deck.isEmpty()) refill();
        item = deck.poll();
        phase = ASK;
        sel = -1;

        // Acompañante inmersivo (v1.2.2): personaje GRANDE que anima en cada carta.
        companion = new Companion(a);
        main.addView(companion.root(), lpm(M, W, 0, 0, 0, 4));
        companion.next();

        main.addView(tv(a, item.title, 22, TEXT, BLACK), lpm(M, W, 0, 6, 0, 16));
        // v1.3.6 fase 2: las preguntas nuevas de teoría son prompt-only
        // (body vacío): no se muestra la tarjeta de contexto vacía.
        if (item.body != null && !item.body.isEmpty()) {
            LinearLayout ctx = col(a);
            ctx.setBackground(round(SURF, 18, LINE, 2));
            ctx.setPadding(dp(16), dp(16), dp(16), dp(16));
            TextView tag = tv(a, item.tag, 13, BLUE, BLACK);
            tag.setLetterSpacing(0.08f);
            ctx.addView(tag, lpm(M, W, 0, 0, 0, 8));
            TextView bd = tv(a, item.body, 17, TEXT, REG);
            bd.setLineSpacing(0, 1.3f);
            ctx.addView(bd);
            main.addView(ctx, lpm(M, W, 0, 0, 0, 18));
        }

        LinearLayout ol = item.opts.length == 2 ? row(a) : col(a);
        opts = new TextView[item.opts.length];
        for (int i = 0; i < item.opts.length; i++) {
            final int idx = i;
            TextView o = tv(a, item.opts[i], item.opts.length == 2 ? 18 : 16, TEXT, MEDIUM);
            o.setPadding(dp(16), dp(14), dp(16), dp(17));
            if (item.opts.length == 2) o.setGravity(Gravity.CENTER);
            optStyle(o, 0);
            o.setOnClickListener(x -> {
                if (phase != ASK) return;
                sel = idx;
                for (int k = 0; k < opts.length; k++) optStyle(opts[k], k == idx ? 1 : 0);
                setBtn("Comprobar", B_GREEN);
            });
            opts[i] = o;
            if (item.opts.length == 2) {
                LinearLayout.LayoutParams p = lpm(0, W, i == 0 ? 0 : 6, 0, i == 0 ? 6 : 0, 0);
                p.weight = 1;
                ol.addView(o, p);
            } else ol.addView(o, lpm(M, W, 0, 0, 0, 10));
        }
        main.addView(ol, lp(M, W));
        setBtn("Comprobar", B_OFF);
    }

    /** 0 idle, 1 selected, 2 correct, 3 wrong. */
    private static void optStyle(TextView o, int s) {
        int border = s == 1 ? BLUE : s == 2 ? GREEN : s == 3 ? RED : LINE;
        int fill = s == 1 ? mix(BLUE, SURF, 0.12f) : s == 2 ? mix(GREEN, SURF, 0.14f) : s == 3 ? mix(RED, SURF, 0.12f) : SURF;
        o.setBackground(raised(fill, border, 16, 3, border));
    }

    private void onBtn() {
        switch (phase) {
            case ASK: if (sel >= 0) check(); break;
            case FEEDBACK: next(); break;
            default: break;
        }
    }

    /** Aporta a la racha diaria (v1.2.5): 10 XP por acierto, como Práctica.
     *  Sin gemas, sin qa/qc, sin nube. Solo bien/mal y la explicación. */
    private void check() {
        boolean ok = sel == item.answer;
        if (companion != null) companion.react(ok);
        for (int k = 0; k < opts.length; k++) optStyle(opts[k], k == item.answer ? 2 : (k == sel && !ok) ? 3 : 0);
        asked++;
        if (ok) {
            correct++;
            streak++;
            Store.I.addXP(Store.XP_Q, true);
            fbTitle.setText(Companion.winLine());
            fbTitle.setTextColor(GREEN);
            setFoot(1);
            setBtn("Continuar", B_GREEN);
        } else {
            streak = 0;
            v.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
            fbTitle.setText(Companion.failLine());
            fbTitle.setTextColor(RED);
            setFoot(2);
            setBtn("Continuar", B_RED);
        }
        String right = "La correcta es " + item.opts[item.answer] + ".";
        fbText.setText((ok ? "Es " + item.opts[item.answer] + ".\n\n" : right + "\n\n")
                + (item.explain != null ? item.explain : ""));
        phase = FEEDBACK;
        updateCount();
    }
}
