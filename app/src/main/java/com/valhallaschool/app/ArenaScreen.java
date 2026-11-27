package com.valhallaschool.app;

import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.valhallaschool.app.Ui.*;

/**
 * Arena sociónica (v1.3.0): 6 escenarios cotidianos de relaciones reales
 * del Modelo A. El jugador media con 3 intervenciones por turno; las barras
 * de tensión, comprensión y cooperación se mueven con cada decisión. Sin
 * violencia premiada: la mediación nunca es golpear, insultar ni imponer.
 * Si la tensión llega a 100 hay un combo cómico y una oportunidad de
 * recuperar la discusión. Al final, una pregunta de teoría.
 */
class ArenaScreen extends MiniGame {
    private int stage = 0; // 0 elegir escenario, 1 turnos, 2 recuperación, 3 teoría
    private GamesData.Arena sc;
    private int turn = 0;
    private int tens, comp, coop;
    private LinearLayout chosenH;
    private final List<LinearLayout> optViews = new ArrayList<>();
    private Views.Bar barT, barC, barCo;

    static ArenaScreen start(MainActivity a) {
        ArenaScreen s = new ArenaScreen(a);
        s.next();
        return s;
    }

    private ArenaScreen(MainActivity a) {
        super(a, "arena", 4);
    }

    private String tName(int t) { return GamesData.TYPES[t]; }

    @Override protected void next() {
        main.removeAllViews();
        setFoot(0);
        updateBar();
        chosenH = null;
        if (stage == 0) {
            phase = ASK;
            addCompanion();
            main.addView(card("ARENA SOCIÓNICA", "6 discusiones cotidianas entre tipos reales. "
                    + "Media sin premiar la violencia: escucha, traduce y negocia. Si la tensión llega a 100, la cosa se pone cómica."), lpm(M, W, 0, 10, 0, 12));
            optViews.clear();
            for (GamesData.Arena s : GamesData.ARENA) {
                TextView opt = optBtn(s.title + "\n" + tName(s.t1) + " – " + tName(s.t2) + " · " + s.relation);
                LinearLayout holder = col(a);
                holder.addView(opt, lp(M, W));
                holder.setTag(s);
                holder.setOnClickListener(x -> {
                    for (LinearLayout h : optViews) optStyle((TextView) h.getChildAt(0), h == holder ? 1 : 0);
                    chosenH = holder;
                    setBtn("Entrar", B_GREEN);
                });
                main.addView(holder, lpm(M, W, 0, 0, 0, 10));
                optViews.add(holder);
            }
            setBtn("Entrar", B_OFF);
            comboTx.setText("");
            return;
        }
        if (stage == 1) {
            phase = ASK;
            done = turn;
            updateBar();
            addCompanion();
            main.addView(card(sc.title.toUpperCase(java.util.Locale.ROOT) + " · " + sc.relation, sc.intro), lpm(M, W, 0, 10, 0, 12));
            buildBars();
            GamesData.ArenaTurn t = sc.turns[turn];
            TextView q = tv(a, "Turno " + (turn + 1) + " de 3 — " + t.situation, 17, TEXT, MEDIUM);
            main.addView(q, lpm(M, W, 0, 0, 0, 12));
            optViews.clear();
            for (int i = 0; i < 3; i++) {
                final int ii = i;
                TextView opt = optBtn(t.opts[i].text);
                LinearLayout holder = col(a);
                holder.addView(opt, lp(M, W));
                holder.setTag(ii);
                holder.setOnClickListener(x -> {
                    for (LinearLayout h : optViews) optStyle((TextView) h.getChildAt(0), h == holder ? 1 : 0);
                    chosenH = holder;
                    setBtn("Intervenir", B_GREEN);
                });
                main.addView(holder, lpm(M, W, 0, 0, 0, 10));
                optViews.add(holder);
            }
            setBtn("Intervenir", B_OFF);
            comboTx.setText("T " + tens + " · C " + comp + " · Co " + coop);
            return;
        }
        if (stage == 2) {
            phase = ASK;
            addCompanion();
            main.addView(card("¡COMBO CÓMICO!", "La tensión llegó a 100. " + tName(sc.t1) + " y " + tName(sc.t2)
                    + " están hablando a la vez, nadie escucha, y el vecino ya tocó la pared. Aún se puede recuperar la discusión."), lpm(M, W, 0, 10, 0, 12));
            buildBars();
            TextView q = tv(a, "¿Cómo recuperas la discusión?", 17, TEXT, MEDIUM);
            main.addView(q, lpm(M, W, 0, 0, 0, 12));
            optViews.clear();
            String[] rec = {"Pedir un minuto de silencio y que cada uno repita lo que oyó", "Subir la voz para que te hagan caso"};
            for (int i = 0; i < 2; i++) {
                final int ii = i;
                TextView opt = optBtn(rec[i]);
                LinearLayout holder = col(a);
                holder.addView(opt, lp(M, W));
                holder.setTag(ii);
                holder.setOnClickListener(x -> {
                    for (LinearLayout h : optViews) optStyle((TextView) h.getChildAt(0), h == holder ? 1 : 0);
                    chosenH = holder;
                    setBtn("Intentar", B_GREEN);
                });
                main.addView(holder, lpm(M, W, 0, 0, 0, 10));
                optViews.add(holder);
            }
            setBtn("Intentar", B_OFF);
            return;
        }
        // stage 3: teoría
        phase = ASK;
        done = 3;
        updateBar();
        addCompanion();
        boolean goodEnd = tens < 70;
        main.addView(card(goodEnd ? "ACUERDO" : "ESCALADA", goodEnd ? sc.endGood : sc.endBad), lpm(M, W, 0, 10, 0, 12));
        buildBars();
        TextView q = tv(a, "Pregunta de teoría: ¿qué relación hay entre " + tName(sc.t1) + " y " + tName(sc.t2) + "?", 17, TEXT, MEDIUM);
        main.addView(q, lpm(M, W, 0, 0, 0, 12));
        String correct = GamesData.REL_NAMES[GamesData.REL[sc.t1][sc.t2]];
        List<String> opts = new ArrayList<>();
        opts.add(correct);
        List<String> pool = new ArrayList<>();
        for (String r : GamesData.REL_NAMES) pool.add(r);
        Collections.shuffle(pool);
        for (String r : pool) if (!r.equals(correct) && opts.size() < 4) opts.add(r);
        Collections.shuffle(opts);
        optViews.clear();
        for (String r : opts) {
            TextView opt = optBtn(r);
            LinearLayout holder = col(a);
            holder.addView(opt, lp(M, W));
            holder.setTag(r);
            holder.setOnClickListener(x -> {
                for (LinearLayout h : optViews) optStyle((TextView) h.getChildAt(0), h == holder ? 1 : 0);
                chosenH = holder;
                setBtn("Responder", B_GREEN);
            });
            main.addView(holder, lpm(M, W, 0, 0, 0, 10));
            optViews.add(holder);
        }
        setBtn("Responder", B_OFF);
    }

    private void buildBars() {
        LinearLayout bars = col(a);
        barT = barRow(bars, "Tensión", RED);
        barC = barRow(bars, "Comprensión", GREEN);
        barCo = barRow(bars, "Cooperación", BLUE);
        main.addView(bars, lpm(M, W, 0, 0, 0, 12));
        refreshBars();
    }

    private Views.Bar barRow(LinearLayout parent, String label, int color) {
        LinearLayout r = row(a);
        TextView l = tv(a, label, 13, MUTED, BLACK);
        r.addView(l, lp(dp(110), W));
        Views.Bar b = new Views.Bar(a);
        LinearLayout.LayoutParams bl = lpm(0, dp(14), 1, 0, 6, 0);
        bl.weight = 1;
        r.addView(b, bl);
        parent.addView(r, lpm(M, W, 0, 0, 0, 6));
        return b;
    }

    private void refreshBars() {
        if (barT != null) { barT.set(tens / 100f, RED); barC.set(comp / 100f, GREEN); barCo.set(coop / 100f, BLUE); }
    }

    private static int clamp(int v) { return Math.max(0, Math.min(100, v)); }

    @Override protected void onBtn() {
        if (phase == END) { a.back(); return; }
        if (stage == 0) {
            if (chosenH == null || chosenH.getTag() == null) { a.toast("Elige un escenario primero, " + name() + ".", false); return; }
            sc = (GamesData.Arena) chosenH.getTag();
            tens = 40; comp = 30; coop = 30;
            turn = 0;
            stage = 1;
            next();
            return;
        }
        if (phase == FEEDBACK) {
            if (stage == 1) {
                if (tens >= 100) { stage = 2; next(); return; }
                turn++;
                if (turn >= 3) { stage = 3; next(); return; }
                next();
                return;
            }
            if (stage == 2) { stage = 1; next(); return; }
            boolean goodEnd = tens < 70;
            done = 4;
            updateBar();
            if (goodEnd) award("arena:" + sc.id);
            finish(goodEnd ? sc.title + ": acuerdo alcanzado." : sc.title + ": la discusión escaló.");
            return;
        }
        if (chosenH == null || chosenH.getTag() == null) { a.toast("Elige una opción primero, " + name() + ".", false); return; }
        if (stage == 1) {
            int i = (Integer) chosenH.getTag();
            GamesData.ArenaTurn t = sc.turns[turn];
            GamesData.ArenaOpt o = t.opts[i];
            tens = clamp(tens + o.dt);
            comp = clamp(comp + o.dc);
            coop = clamp(coop + o.dco);
            boolean good = o.dt <= 0 && (o.dc >= 0 || o.dco >= 0);
            answered++;
            if (good) firstOk++;
            for (LinearLayout h : optViews) {
                TextView opt = (TextView) h.getChildAt(0);
                int hi = (Integer) h.getTag();
                optStyle(opt, hi == i ? (good ? 2 : 3) : 0);
                h.setOnClickListener(null);
            }
            reactCompanion(good);
            fbTitle.setText(good ? "¡Mediación decente, " + name() + "! ¿Casualidad, cuck?" : "¡Peor no se podía, " + name() + "!");
            fbText.setText(o.reaction);
            if (good) Sfx.play(Sfx.OK); else hapticBad();
            phase = FEEDBACK;
            setFoot(good ? 1 : 2);
            setBtn(tens >= 100 ? "Ver qué pasó" : (turn >= 2 ? "Ver el final" : "Siguiente turno"), B_GREEN);
            refreshBars();
            comboTx.setText("T " + tens + " · C " + comp + " · Co " + coop);
        } else if (stage == 2) {
            int i = (Integer) chosenH.getTag();
            boolean good = i == 0;
            if (good) { tens = clamp(tens - 30); comp = clamp(comp + 10); }
            else { tens = clamp(tens + 10); comp = clamp(comp - 10); }
            for (LinearLayout h : optViews) {
                TextView opt = (TextView) h.getChildAt(0);
                int hi = (Integer) h.getTag();
                optStyle(opt, hi == i ? (good ? 2 : 3) : 0);
                h.setOnClickListener(null);
            }
            reactCompanion(good);
            fbTitle.setText(good ? "¡Recuperada, " + name() + "! Milagro, NPC." : "¡Ni recuperando, " + name() + "!");
            fbText.setText(good ? "Silencio, repetición, y la discusión vuelve a su cauce. Tensión -30, comprensión +10."
                    : "Subir la voz nunca recupera nada. Tensión +10, comprensión -10.");
            if (good) Sfx.play(Sfx.OK); else hapticBad();
            phase = FEEDBACK;
            setFoot(good ? 1 : 2);
            setBtn("Seguir", B_GREEN);
            refreshBars();
        } else {
            String r = (String) chosenH.getTag();
            String correct = GamesData.REL_NAMES[GamesData.REL[sc.t1][sc.t2]];
            boolean ok = r.equals(correct);
            answered++;
            if (ok) { firstOk++; award("arena:" + sc.id + ":t"); }
            for (LinearLayout h : optViews) {
                TextView opt = (TextView) h.getChildAt(0);
                String hr = (String) h.getTag();
                optStyle(opt, hr.equals(correct) ? 2 : h == chosenH ? 3 : 0);
                h.setOnClickListener(null);
            }
            reactCompanion(ok);
            fbTitle.setText(ok ? "¡Teoría correcta, " + name() + "! ¿Copiaste, beta?" : "¡Ni la teoría, " + name() + "!");
            fbText.setText(ok ? tName(sc.t1) + " y " + tName(sc.t2) + " tienen relación de " + correct + ". "
                    + ConexionesScreen.relDesc(GamesData.REL[sc.t1][sc.t2], tName(sc.t1), tName(sc.t2))
                    : "La relación correcta es " + correct + ".");
            if (ok) Sfx.play(Sfx.OK); else hapticBad();
            phase = FEEDBACK;
            setFoot(ok ? 1 : 2);
            setBtn("Terminar", B_GREEN);
        }
        updateBar();
    }
}
