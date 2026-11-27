package com.valhallaschool.app;

import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.valhallaschool.app.Ui.*;

/**
 * Detective sociónico (v1.3.2, rehecho según la biblia §01): 60 fragmentos
 * de evidencia en 6 expedientes con 3 escenas numeradas cada uno. Cada caso
 * abre con una explicación breve de QUÉ hay que hacer; las pistas son
 * escalonadas (escena relevante → fragmento destacado → contraste);
 * el expediente (pistas/hipótesis/notas) se consulta en cualquier momento.
 * Mecánica intacta: marcar fragmentos como evidencia, hipótesis provisional,
 * conclusión con justificación; «Indeterminado» es válido si faltan datos.
 * El XP sigue yendo a la racha (+10 por ítem la primera vez).
 */
class DetectiveScreen extends MiniGame {
    private static class Frag {
        String id, text, answer, just;
        int scene;
        List<String> opts;
    }

    /** Qué mide cada elemento (PDF de Elementos SCS): base de las pistas escalonadas. */
    private static final String[] EL_HINTS = {
            "la energía potencial, la esencia y lo que algo es capaz de ser", // Ne
            "el tiempo, la secuencia y si queda tiempo", // Ni
            "la apariencia, la fuerza y la voluntad", // Se
            "el presente, las sensaciones y el bienestar", // Si
            "el trabajo, la eficiencia y los métodos", // Te
            "las estructuras, las jerarquías y los sistemas", // Ti
            "las emociones, el ánimo y la excitación", // Fe
            "las relaciones, la atracción o repulsión y la distancia entre personas", // Fi
    };

    private final List<Frag> queue = new ArrayList<>();
    private int caseId;
    private String caseTitle, caseIntro, caseAnsA, caseAnsB, caseExpl;
    private int caseAnswer; // 0 tipo A, 1 tipo B, 2 indeterminado
    private boolean picking = false;
    private boolean intro = false;
    private int step = 0; // 0..9 fragmentos, 10 = conclusión
    private int caseFragsOk = 0;
    private Frag cur;
    private boolean picked = false;
    private LinearLayout chosenH;
    private final List<LinearLayout> optViews = new ArrayList<>();
    private TextView hypTx, hintTx, hintBtn, fileTx, fileBtn;
    private int hintLevel = 0;
    private boolean showFile = false;
    private final List<String> marked = new ArrayList<>();

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
            g.id = d.id; g.scene = d.scene; g.text = d.fragment; g.answer = GamesData.ELEMENTS[d.aEl]; g.just = d.justification;
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
            main.addView(card("DETECTIVE SOCIÓNICO", "Seis expedientes, sesenta fragmentos de evidencia. "
                    + "Marca en cada fragmento qué elemento está hablando y cierra el caso con tu conclusión."), lpm(M, W, 0, 10, 0, 12));
            TextView q = tv(a, "¿Qué expediente abres?", 17, TEXT, MEDIUM);
            main.addView(q, lpm(M, W, 0, 0, 0, 12));
            optViews.clear();
            for (int c = 0; c < GamesData.CASES.length; c++) {
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
        if (intro) {
            phase = ASK;
            addCompanion();
            main.addView(card("CÓMO SE INVESTIGA", "1 · Lee la escena: cada expediente tiene 3 escenas numeradas, en orden.\n"
                    + "2 · En cada fragmento marca qué elemento está hablando (Ne, Ni, Se, Si, Te, Ti, Fe, Fi).\n"
                    + "3 · Tu hipótesis provisional aparece arriba: cámbiala cuando quieras.\n"
                    + "4 · Al final concluye: qué tipo es el sospechoso, o «Indeterminado» si los datos no alcanzan.\n"
                    + "Si no sabes por dónde empezar, toca 💡 Pista: primero te dice en qué escena está, luego qué mirar, luego el contraste."), lpm(M, W, 0, 10, 0, 12));
            setBtn("Empezar la investigación", B_GREEN);
            return;
        }
        if (step < queue.size()) {
            phase = ASK;
            cur = queue.get(step);
            hintLevel = 0;
            addCompanion();
            LinearLayout c = card("EXPEDIENTE " + (caseId + 1) + " · " + caseTitle.toUpperCase(java.util.Locale.ROOT), caseIntro);
            main.addView(c, lpm(M, W, 0, 10, 0, 12));
            TextView tag = tv(a, "Escena " + cur.scene + " de 3 — " + GamesData.CASE_SCENES[caseId][cur.scene - 1]
                    + "  ·  Fragmento " + (step + 1) + " de " + queue.size(), 13, BLUE, BLACK);
            main.addView(tag, lpm(M, W, 0, 0, 0, 6));
            LinearLayout frag = card(null, "«" + cur.text + "»");
            main.addView(frag, lpm(M, W, 0, 0, 0, 14));
            hypTx = tv(a, "Hipótesis provisional: —", 14, MUTED, REG);
            main.addView(hypTx, lpm(M, W, 0, 0, 0, 10));
            // Pistas escalonadas (biblia §01): escena → fragmento → contraste.
            hintBtn = tv(a, "💡 Pista (0/3)", 15, BLUE, MEDIUM);
            hintBtn.setGravity(Gravity.CENTER);
            hintBtn.setBackground(round(SURF2, 12, LINE, 1));
            hintBtn.setPadding(dp(14), dp(12), dp(14), dp(12));
            hintBtn.setOnClickListener(v -> showHint());
            ripple(hintBtn, true);
            main.addView(hintBtn, lpm(M, W, 0, 0, 0, 8));
            hintTx = tv(a, "", 15, TEXT, REG);
            hintTx.setLineSpacing(0, 1.3f);
            hintTx.setVisibility(android.view.View.GONE);
            main.addView(hintTx, lpm(M, W, 0, 0, 0, 10));
            // Expediente (pistas/hipótesis/notas): lo marcado hasta ahora.
            fileBtn = tv(a, "📋 Expediente (" + marked.size() + "/" + queue.size() + ")", 15, MUTED, MEDIUM);
            fileBtn.setGravity(Gravity.CENTER);
            fileBtn.setBackground(round(SURF2, 12, LINE, 1));
            fileBtn.setPadding(dp(14), dp(12), dp(14), dp(12));
            fileBtn.setOnClickListener(v -> {
                showFile = !showFile;
                fileTx.setVisibility(showFile ? android.view.View.VISIBLE : android.view.View.GONE);
                fileBtn.setText("📋 Expediente (" + marked.size() + "/" + queue.size() + ")" + (showFile ? " ▴" : " ▾"));
            });
            ripple(fileBtn, true);
            main.addView(fileBtn, lpm(M, W, 0, 0, 0, 8));
            fileTx = tv(a, fileText(), 14, TEXT, REG);
            fileTx.setLineSpacing(0, 1.3f);
            fileTx.setVisibility(showFile ? android.view.View.VISIBLE : android.view.View.GONE);
            main.addView(fileTx, lpm(M, W, 0, 0, 0, 10));
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

    private String fileText() {
        if (marked.isEmpty()) return "Aún no marcaste nada en este expediente.";
        StringBuilder b = new StringBuilder();
        for (String m : marked) b.append("· ").append(m).append("\n");
        return b.toString().trim();
    }

    private void showHint() {
        if (hintLevel >= 3) return;
        hintLevel++;
        int el = PracticeData.elIdx(cur.answer);
        String txt;
        if (hintLevel == 1) {
            txt = "Pista 1 — La pista está en esta escena (Escena " + cur.scene + "): alguien procesa información de "
                    + cur.answer + " (" + GamesData.elName(cur.answer) + "). Pregúntate qué está midiendo.";
        } else if (hintLevel == 2) {
            txt = "Pista 2 — Fíjate en: " + EL_HINTS[el] + ".";
        } else {
            txt = "Pista 3 — " + cur.just;
        }
        hintTx.setText(txt);
        hintTx.setVisibility(android.view.View.VISIBLE);
        hintBtn.setText("💡 Pista (" + hintLevel + "/3)" + (hintLevel >= 3 ? " · ya está todo" : ""));
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
            intro = true;
            step = 0;
            caseFragsOk = 0;
            marked.clear();
            showFile = false;
            next();
            return;
        }
        if (intro) {
            intro = false;
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
            marked.add("Fragmento " + (step + 1) + ": elegiste " + el + (ok ? " ✓" : " ✗ (era " + cur.answer + ")"));
            for (LinearLayout h : optViews) {
                TextView o = (TextView) h.getChildAt(0);
                String hEl = (String) h.getTag();
                optStyle(o, hEl.equals(cur.answer) ? 2 : h == chosen ? 3 : 0);
                h.setOnClickListener(null);
            }
            fbTitle.setText(ok ? winTitle("¡Acertaste, " + name() + "!") : failTitle("¡Te equivocaste, " + name() + "!"));
            fbText.setText(cur.just);
            reactCompanion(ok);
            if (!ok) hapticBad();
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
            fbTitle.setText(ok ? winTitle("¡Caso resuelto, " + name() + "!") : failTitle("¡Ni el caso lo cerraste, " + name() + "!"));
            fbText.setText(caseExpl);
            reactCompanion(ok);
            if (!ok) hapticBad();
            phase = FEEDBACK;
            setFoot(ok ? 1 : 2);
            setBtn("Terminar", B_GREEN);
        }
        updateBar();
    }
}
