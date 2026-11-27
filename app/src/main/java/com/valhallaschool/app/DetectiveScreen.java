package com.valhallaschool.app;

import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.valhallaschool.app.Ui.*;

/**
 * Detective sociónico (v1.3.0): 30 fragmentos de evidencia en 3 expedientes
 * lineales. En cada fragmento se marca el elemento que está hablando; al
 * cerrar el expediente se concluye el tipo (o «indeterminado» cuando los
 * datos no alcanzan). La hipótesis provisional aparece en la tarjeta de
 * evidencia.
 */
class DetectiveScreen extends MiniGame {
    private static class Frag {
        String id, text, answer, just;
        List<String> opts;
    }

    private final List<Frag> queue = new ArrayList<>();
    private int caseId;
    private String caseTitle, caseIntro, caseAnsA, caseAnsB, caseExpl;
    private int caseAnswer; // 0 tipo A, 1 tipo B, 2 indeterminado
    private boolean picking = false;
    private int step = 0; // 0..9 fragmentos, 10 = conclusión
    private int caseFragsOk = 0;
    private Frag cur;
    private boolean picked = false;
    private LinearLayout chosenH;
    private final List<LinearLayout> optViews = new ArrayList<>();
    private TextView hypTx;

    static DetectiveScreen start(MainActivity a) {
        DetectiveScreen s = new DetectiveScreen(a);
        s.picking = true;
        s.next();
        return s;
    }

    private DetectiveScreen(MainActivity a) {
        super(a, "detective", 11);
    }

    private void setCase(int c) {
        String[] meta = GamesData.CASES[c];
        caseId = c;
        caseTitle = meta[0];
        caseIntro = meta[1];
        caseAnsA = meta[2];
        caseAnsB = meta[3];
        caseExpl = meta[4];
        caseAnswer = Integer.parseInt(meta[5]);
        queue.clear();
        for (GamesData.Det d : GamesData.detectiveOfCase(c)) {
            Frag g = new Frag();
            g.id = d.id; g.text = d.fragment; g.answer = GamesData.ELEMENTS[d.aEl]; g.just = d.justification;
            g.opts = new ArrayList<>();
            g.opts.add(GamesData.ELEMENTS[d.aEl]);
            g.opts.add(GamesData.ELEMENTS[d.d1El]);
            g.opts.add(GamesData.ELEMENTS[d.d2El]);
            g.opts.add(GamesData.ELEMENTS[d.d3El]);
            Collections.shuffle(g.opts);
            queue.add(g);
        }
    }

    @Override protected void next() {
        main.removeAllViews();
        setFoot(0);
        updateBar();
        picked = false;
        chosenH = null;
        if (picking) {
            phase = ASK;
            addCompanion();
            main.addView(card("DETECTIVE SOCIÓNICO", "Tres expedientes, treinta fragmentos de evidencia. "
                    + "Marca en cada fragmento qué elemento está hablando y cierra el caso con tu conclusión."), lpm(M, W, 0, 10, 0, 12));
            TextView q = tv(a, "¿Qué expediente abres?", 17, TEXT, MEDIUM);
            main.addView(q, lpm(M, W, 0, 0, 0, 12));
            optViews.clear();
            for (int c = 0; c < 3; c++) {
                String[] meta = GamesData.CASES[c];
                TextView opt = optBtn("Expediente " + (c + 1) + ": " + meta[0] + "\n" + meta[1]);
                final int cc = c;
                LinearLayout holder = col(a);
                holder.addView(opt, lp(M, W));
                holder.setTag(cc);
                holder.setOnClickListener(v -> {
                    for (LinearLayout h : optViews) optStyle((TextView) h.getChildAt(0), h == holder ? 1 : 0);
                    chosenH = holder;
                    picked = true;
                    setBtn("Abrir expediente", B_GREEN);
                });
                main.addView(holder, lpm(M, W, 0, 0, 0, 10));
                optViews.add(holder);
            }
            setBtn("Abrir expediente", B_OFF);
            return;
        }
        if (step < queue.size()) {
            phase = ASK;
            cur = queue.get(step);
            addCompanion();
            LinearLayout c = card("EXPEDIENTE " + (caseId + 1) + " · " + caseTitle.toUpperCase(java.util.Locale.ROOT), caseIntro);
            main.addView(c, lpm(M, W, 0, 10, 0, 12));
            TextView tag = tv(a, "Fragmento " + (step + 1) + " de " + queue.size(), 13, BLUE, BLACK);
            main.addView(tag, lpm(M, W, 0, 0, 0, 6));
            LinearLayout frag = card(null, "«" + cur.text + "»");
            main.addView(frag, lpm(M, W, 0, 0, 0, 14));
            hypTx = tv(a, "Hipótesis provisional: —", 14, MUTED, REG);
            main.addView(hypTx, lpm(M, W, 0, 0, 0, 10));
            TextView q = tv(a, "¿Qué elemento está hablando aquí?", 17, TEXT, MEDIUM);
            main.addView(q, lpm(M, W, 0, 0, 0, 12));
            optViews.clear();
            for (String o : cur.opts) {
                TextView opt = optBtn(o + " · " + GamesData.elName(o));
                LinearLayout holder = col(a);
                holder.addView(opt, lp(M, W));
                final String el = o;
                holder.setTag(el);
                holder.setOnClickListener(v -> pickElement(holder, el));
                main.addView(holder, lpm(M, W, 0, 0, 0, 10));
                optViews.add(holder);
            }
            comboTx.setText(caseFragsOk + "/" + step + " ✓");
            setBtn("Comprobar", B_OFF);
        } else {
            phase = ASK;
            addCompanion();
            main.addView(card("CONCLUSIÓN DEL EXPEDIENTE", "Ya revisaste los " + queue.size()
                    + " fragmentos. Tu acierto en evidencia fue " + caseFragsOk + " de " + queue.size()
                    + ". Ahora concluye: ¿qué tipo es el sospechoso?"), lpm(M, W, 0, 10, 0, 12));
            TextView q = tv(a, "¿Cuál es tu conclusión?", 17, TEXT, MEDIUM);
            main.addView(q, lpm(M, W, 0, 0, 0, 12));
            List<String> answers = new ArrayList<>();
            answers.add(caseAnsA); answers.add(caseAnsB);
            answers.add("Indeterminado");
            Collections.shuffle(answers);
            optViews.clear();
            for (String ans : answers) {
                TextView opt = optBtn(ans.equals("Indeterminado") ? "Indeterminado — los datos no alcanzan" : "Es un " + ans);
                LinearLayout holder = col(a);
                holder.addView(opt, lp(M, W));
                holder.setTag(ans);
                holder.setOnClickListener(v -> pickCase(holder, ans));
                main.addView(holder, lpm(M, W, 0, 0, 0, 10));
                optViews.add(holder);
            }
            setBtn("Concluir", B_OFF);
        }
    }

    private void pickElement(LinearLayout holder, String el) {
        if (phase != ASK) return;
        for (LinearLayout h : optViews) optStyle((TextView) h.getChildAt(0), h == holder ? 1 : 0);
        hypTx.setText("Hipótesis provisional: " + el + " · " + GamesData.elName(el));
        picked = true;
        chosenH = holder;
        setBtn("Comprobar", B_GREEN);
    }

    private void pickCase(LinearLayout holder, String ans) {
        if (phase != ASK) return;
        for (LinearLayout h : optViews) optStyle((TextView) h.getChildAt(0), h == holder ? 1 : 0);
        picked = true;
        chosenH = holder;
        setBtn("Concluir", B_GREEN);
    }

    private String correctConclusion() {
        return caseAnswer == 2 ? "Indeterminado" : caseAnswer == 0 ? caseAnsA : caseAnsB;
    }

    @Override protected void onBtn() {
        if (phase == END) { a.back(); return; }
        if (picking) {
            if (!picked || chosenH == null || chosenH.getTag() == null) { a.toast("Elige un expediente primero, " + name() + ".", false); return; }
            setCase((Integer) chosenH.getTag());
            picking = false;
            step = 0;
            caseFragsOk = 0;
            next();
            return;
        }
        if (phase == FEEDBACK) {
            step++;
            done = step;
            if (step >= queue.size() + 1) {
                finish("Expediente " + (caseId + 1) + " cerrado. Vuelve a la pestaña Juegos para otro caso.");
                return;
            }
            next();
            return;
        }
        if (!picked || chosenH == null) { a.toast("Elige una opción primero, " + name() + ".", false); return; }
        LinearLayout chosen = chosenH;
        if (step < queue.size()) {
            String el = (String) chosen.getTag();
            boolean ok = el.equals(cur.answer);
            answered++;
            if (ok) { firstOk++; caseFragsOk++; award("det:" + cur.id); }
            for (LinearLayout h : optViews) {
                TextView o = (TextView) h.getChildAt(0);
                String hEl = (String) h.getTag();
                optStyle(o, hEl.equals(cur.answer) ? 2 : h == chosen ? 3 : 0);
                h.setOnClickListener(null);
            }
            fbTitle.setText(ok ? "¡Acertaste, " + name() + "! ¿Copiaste?" : "¡Te equivocaste, " + name() + "!");
            fbText.setText(cur.just);
            reactCompanion(ok);
            if (ok) Sfx.play(Sfx.OK); else hapticBad();
            phase = FEEDBACK;
            setFoot(ok ? 1 : 2);
            setBtn("Continuar", B_GREEN);
            comboTx.setText(caseFragsOk + "/" + (step + 1) + " ✓");
        } else {
            String ans = (String) chosen.getTag();
            boolean ok = ans.equals(correctConclusion());
            answered++;
            if (ok) { firstOk++; award("det:case" + (caseId + 1)); }
            for (LinearLayout h : optViews) {
                TextView o = (TextView) h.getChildAt(0);
                String hAns = (String) h.getTag();
                optStyle(o, hAns.equals(correctConclusion()) ? 2 : h == chosen ? 3 : 0);
                h.setOnClickListener(null);
            }
            fbTitle.setText(ok ? "¡Caso resuelto, " + name() + "! Milagro." : "¡Ni el caso lo cerraste, " + name() + "!");
            fbText.setText(caseExpl);
            reactCompanion(ok);
            if (ok) Sfx.play(Sfx.OK); else hapticBad();
            phase = FEEDBACK;
            setFoot(ok ? 1 : 2);
            setBtn("Terminar", B_GREEN);
        }
        updateBar();
    }
}
