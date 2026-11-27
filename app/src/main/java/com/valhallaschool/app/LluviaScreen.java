package com.valhallaschool.app;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.valhallaschool.app.Ui.*;

/**
 * Lluvia informacional (v1.3.6): 48 fichas revisadas en 6 rondas de 5.
 * Las fichas caen y hay que clasificarlas antes de que toquen el suelo;
 * si una cae sin respuesta, es omisión (se explica, no se falla en
 * automático). Dos velocidades + modo estático con el MISMO banco. La
 * caída se pausa al perder el foco (o con el botón ⏸). La dificultad
 * global filtra cada ronda por `diff` (cada ronda tiene 5 de Normal
 * y 1 de cada nivel más duro).
 */
class LluviaScreen extends MiniGame {
    private static final String[] ROUNDS = {
            "Elementos A", "Elementos B", "Posiciones A", "Posiciones B", "Bloques", "Anillos"};
    private static final String[] SPEEDS = {"Rápida (6 s)", "Normal (10 s)", "Tranquila (sin caída)"};

    private int stage = 0; // 0 elegir, 1 jugar
    private int roundIdx = -1, speedIdx = -1;
    private List<GamesData.Rain> queue;
    private int idx = 0;
    private LinearLayout chosenRoundH, chosenSpeedH, chosenH;
    private final List<LinearLayout> roundViews = new ArrayList<>();
    private final List<LinearLayout> speedViews = new ArrayList<>();
    private final List<LinearLayout> optViews = new ArrayList<>();
    private ObjectAnimator fall;
    private LinearLayout fallingCard;
    private boolean fallingPaused = false;
    private boolean cardDone = false; // true cuando la ficha ya se resolvió o la pantalla se cerró (cancel() también dispara onAnimationEnd)
    private TextView pauseBtn;

    static LluviaScreen start(MainActivity a) {
        LluviaScreen s = new LluviaScreen(a);
        s.next();
        return s;
    }

    private LluviaScreen(MainActivity a) {
        super(a, "lluvia", 5);
    }

    @Override void paused() { pauseFall(); }

    @Override void resume() {
        if (fall != null && fallingPaused && phase == ASK) { fall.resume(); fallingPaused = false; updatePauseBtn(); }
    }

    @Override void closed() { cardDone = true; cancelFall(); }

    private void cancelFall() {
        if (fall != null) { fall.cancel(); fall = null; }
    }

    private void pauseFall() {
        if (fall != null && fall.isRunning() && !fallingPaused) {
            fall.pause();
            fallingPaused = true;
            updatePauseBtn();
        }
    }

    private void updatePauseBtn() {
        if (pauseBtn != null) pauseBtn.setText(fallingPaused ? "▶ Reanudar" : "⏸ Pausa");
    }

    @Override protected void next() {
        main.removeAllViews();
        setFoot(0);
        updateBar();
        cancelFall();
        fallingPaused = false;
        if (stage == 0) {
            phase = ASK;
            addCompanion();
            main.addView(card("LLUVIA INFORMACIONAL", "Las fichas caen del cielo: clasifícalas antes de que toquen el suelo. "
                    + "Si una cae sin respuesta, es omisión y se te explica, no se te falla en automático. El modo tranquilo usa las mismas fichas, sin caída."), lpm(M, W, 0, 10, 0, 12));
            TextView tagR = tv(a, "Ronda", 13, BLUE, BLACK);
            main.addView(tagR, lpm(M, W, 0, 0, 0, 8));
            roundViews.clear();
            for (int r = 0; r < 6; r++) {
                final int rr = r;
                TextView opt = optBtn(ROUNDS[r] + "  ·  5 fichas");
                LinearLayout holder = col(a);
                holder.addView(opt, lp(M, W));
                holder.setOnClickListener(x -> {
                    for (LinearLayout h : roundViews) optStyle((TextView) h.getChildAt(0), h == holder ? 1 : 0);
                    chosenRoundH = holder;
                    roundIdx = rr;
                    refreshStartBtn();
                });
                main.addView(holder, lpm(M, W, 0, 0, 0, 10));
                roundViews.add(holder);
            }
            TextView tagS = tv(a, "Velocidad", 13, BLUE, BLACK);
            main.addView(tagS, lpm(M, W, 0, 8, 0, 8));
            speedViews.clear();
            for (int s = 0; s < 3; s++) {
                final int ss = s;
                TextView opt = optBtn(SPEEDS[s]);
                LinearLayout holder = col(a);
                holder.addView(opt, lp(M, W));
                holder.setOnClickListener(x -> {
                    for (LinearLayout h : speedViews) optStyle((TextView) h.getChildAt(0), h == holder ? 1 : 0);
                    chosenSpeedH = holder;
                    speedIdx = ss;
                    refreshStartBtn();
                });
                main.addView(holder, lpm(M, W, 0, 0, 0, 10));
                speedViews.add(holder);
            }
            setBtn("Empezar", B_OFF);
            return;
        }
        if (idx >= queue.size()) {
            finish("Lluvia informacional: ronda de " + ROUNDS[roundIdx] + " (" + SPEEDS[speedIdx] + "). Vuelve a la pestaña Juegos para otra ronda.");
            return;
        }
        phase = ASK;
        chosenH = null;
        cardDone = false;
        addCompanion();
        GamesData.Rain c = queue.get(idx);
        if (speedIdx < 2) {
            pauseBtn = tv(a, "⏸ Pausa", 15, MUTED, MEDIUM);
            pauseBtn.setGravity(Gravity.END);
            pauseBtn.setPadding(0, dp(2), dp(4), dp(8));
            pauseBtn.setOnClickListener(v -> {
                if (fall == null) return;
                if (fallingPaused) { fall.resume(); fallingPaused = false; }
                else pauseFall();
                updatePauseBtn();
            });
            main.addView(pauseBtn, lp(M, W));
        } else {
            pauseBtn = null;
        }
        fallingCard = col(a);
        fallingCard.setBackground(round(mix(BLUE, SURF, 0.12f), 18, BLUE, 3));
        fallingCard.setPadding(dp(16), dp(18), dp(16), dp(18));
        TextView prompt = tv(a, c.text, 18, TEXT, MEDIUM);
        prompt.setLineSpacing(0, 1.3f);
        fallingCard.addView(prompt);
        main.addView(fallingCard, lpm(M, W, 0, 0, 0, 14));
        TextView q = tv(a, "Clasifica la ficha:", 15, MUTED, REG);
        main.addView(q, lpm(M, W, 0, 0, 0, 10));
        optViews.clear();
        // v1.3.5: las opciones se barajan en cada ficha (antes iban en el
        // orden fijo del TSV: Ne/Ni/Se/Si y la correcta caía siempre en el
        // mismo sitio). El tag sigue siendo el índice ORIGINAL.
        int[] order = Quiz.shuffled(c.nOpts);
        for (int i = 0; i < c.nOpts; i++) {
            final int ii = order[i];
            TextView opt = optBtn(c.opts[ii]);
            LinearLayout holder = col(a);
            holder.addView(opt, lp(M, W));
            holder.setTag(ii);
            holder.setOnClickListener(x -> {
                if (phase != ASK) return;
                for (LinearLayout h : optViews) optStyle((TextView) h.getChildAt(0), h == holder ? 1 : 0);
                chosenH = holder;
                setBtn("Comprobar", B_GREEN);
            });
            main.addView(holder, lpm(M, W, 0, 0, 0, 10));
            optViews.add(holder);
        }
        setBtn("Comprobar", B_OFF);
        comboTx.setText("Ficha " + (idx + 1) + "/" + queue.size());
        if (speedIdx < 2) {
            long dur = speedIdx == 0 ? 6000 : 10000;
            fall = ObjectAnimator.ofFloat(fallingCard, "translationY", 0f, dp(380));
            fall.setDuration(dur);
            fall.addListener(new AnimatorListenerAdapter() {
                @Override public void onAnimationEnd(Animator animation) {
                    if (phase == ASK) onOmission();
                }
            });
            fall.start();
        }
    }

    private void refreshStartBtn() {
        setBtn(roundIdx >= 0 && speedIdx >= 0 ? "Empezar" : "Elige ronda y velocidad", roundIdx >= 0 && speedIdx >= 0 ? B_GREEN : B_OFF);
    }

    private void onOmission() {
        if (cardDone) return;
        cardDone = true;
        cancelFall();
        GamesData.Rain c = queue.get(idx);
        answered++;
        for (LinearLayout h : optViews) {
            TextView o = (TextView) h.getChildAt(0);
            int hi = (Integer) h.getTag();
            optStyle(o, hi == c.answer ? 2 : 0);
            h.setOnClickListener(null);
        }
        reactCompanion(false);
        fbTitle.setText(failTitle("¡Se te cayó, " + name() + "!"));
        fbText.setText("Omisión: la ficha era «" + c.opts[c.answer] + "». " + c.explanation);
        hapticBad();
        phase = FEEDBACK;
        setFoot(2);
        setBtn("Siguiente", B_GREEN);
        updateBar();
    }

    @Override protected void onBtn() {
        if (phase == END) { a.back(); return; }
        if (stage == 0) {
            if (roundIdx < 0 || speedIdx < 0) { a.toast("Elige ronda y velocidad, " + name() + ".", false); return; }
            // v1.3.6: el pool es la ronda con diff <= nivel, barajado, 5 fichas.
            int d = 0;
            try { d = Store.I == null ? 0 : Store.I.difficulty; } catch (Exception ignored) { /* sin Store = Normal */ }
            queue = new ArrayList<>();
            for (GamesData.Rain x : GamesData.LLUVIA) if (x.round == roundIdx && x.diff <= d) queue.add(x);
            if (queue.size() < 5) { // guarda: si el pool quedara corto, la ronda de siempre
                queue.clear();
                int from = roundIdx * 5;
                for (int i = from; i < from + 5 && i < GamesData.LLUVIA.size(); i++) queue.add(GamesData.LLUVIA.get(i));
            }
            Collections.shuffle(queue);
            queue = new ArrayList<>(queue.subList(0, 5));
            idx = 0;
            stage = 1;
            next();
            return;
        }
        if (phase == FEEDBACK) {
            idx++;
            done = idx;
            next();
            return;
        }
        if (chosenH == null || chosenH.getTag() == null) { a.toast("Elige una opción primero, " + name() + ".", false); return; }
        cardDone = true;
        cancelFall();
        GamesData.Rain c = queue.get(idx);
        int pick = (Integer) chosenH.getTag();
        boolean ok = pick == c.answer;
        answered++;
        if (ok) { firstOk++; award("lluvia:" + c.id); }
        for (LinearLayout h : optViews) {
            TextView o = (TextView) h.getChildAt(0);
            int hi = (Integer) h.getTag();
            optStyle(o, hi == c.answer ? 2 : h == chosenH ? 3 : 0);
            h.setOnClickListener(null);
        }
        reactCompanion(ok);
        if (ok) {
            fbTitle.setText(winTitle("¡Atrapada, " + name() + "!"));
            fbText.setText(c.explanation);
            setFoot(1);
        } else {
            fbTitle.setText(failTitle("¡Mal clasificada, " + name() + "!"));
            fbText.setText("Era «" + c.opts[c.answer] + "». " + c.explanation);
            hapticBad();
            setFoot(2);
        }
        phase = FEEDBACK;
        setBtn("Siguiente", B_GREEN);
        updateBar();
    }
}
