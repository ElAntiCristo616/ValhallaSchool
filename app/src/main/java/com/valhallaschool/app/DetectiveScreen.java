package com.valhallaschool.app;

import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.valhallaschool.app.Ui.*;

/**
 * Detective sociónico (v1.3.3): 50 fragmentos de evidencia en 5 expedientes
 * con 3 escenas numeradas cada uno. (v1.3.2 tenía 6; se eliminó «El taller
 * reutilizado» porque el desfase de índices lo dejaba con 0 fragmentos.) Cada caso
 * abre con una explicación breve de QUÉ hay que hacer; las pistas son
 * escalonadas (pregunta para hacerse → descartar dos → quedan dos
 * candidatos) y NUNCA regalan la respuesta: no nombran el elemento, no
 * dan su definición ni la justificación del fragmento (v1.3.7);
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

    /**
     * Tablas para construir acercamientos (v1.3.7): las pistas viejas
     * regalaban la respuesta (nombraban el elemento, daban su definición
     * y la justificación). Las nuevas solo acercan: una pregunta, un
     * descarte y un par de candidatos. Índices de elementos: Ne 0, Ni 1,
     * Se 2, Si 3, Te 4, Ti 5, Fe 6, Fi 7.
     */
    /** Eje de cada elemento: 0 intuición, 1 sensación, 2 lógica, 3 ética. */
    private static final int[] EL_AXIS = {0, 0, 1, 1, 2, 2, 3, 3};
    private static final String[] AXIS_NAMES = {"intuición", "sensación", "lógica", "ética"};
    /** ¿El elemento mira hacia fuera (explícito) o hacia dentro (implícito)? */
    private static final boolean[] EL_EXPLICIT = {true, false, true, false, true, false, true, false};
    /** ¿Lo que describe se queda igual (estático) o cambia (dinámico)? */
    private static final boolean[] EL_STATIC = {true, false, true, false, false, true, false, true};
    /** Palabra de categoría de cada elemento: solo sirve para DESCARTAR, nunca nombra el elemento. */
    private static final String[] EL_CAT = {
            "el del potencial", "el del tiempo", "el de la apariencia", "el del bienestar",
            "el del trabajo", "el de las estructuras", "el de las emociones", "el de las relaciones"};
    /** Pista corta para contrastar dos candidatos: qué mira cada uno (una cláusula, no la definición). */
    private static final String[] EL_CUE = {
            "mira en qué se puede convertir algo", // Ne
            "mira cómo va a cambiar algo con el tiempo", // Ni
            "mira cómo se ve algo y qué fuerza tiene", // Se
            "mira cómo se siente algo por dentro", // Si
            "mira cómo se hace el trabajo", // Te
            "mira cómo se ordena todo en un sistema", // Ti
            "mira cómo se siente la gente", // Fe
            "mira cómo se llevan las personas entre sí", // Fi
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
    /** v1.3.7: compañero del escalón 3 y los dos descartados del escalón 2 (índices de elemento, -1 si no hay). */
    private int hintPartner = -1, hintOut1 = -1, hintOut2 = -1;
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
            main.addView(card("DETECTIVE SOCIÓNICO", "Cinco expedientes, cincuenta fragmentos de evidencia. "
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
            String howto = "1 · Lee la escena: cada expediente tiene 3 escenas numeradas, en orden.\n"
                    + "2 · En cada fragmento marca qué elemento está hablando (Ne, Ni, Se, Si, Te, Ti, Fe, Fi).\n"
                    + "3 · Tu hipótesis provisional aparece arriba: cámbiala cuando quieras.\n"
                    + "4 · Al final concluye: qué tipo es el sospechoso, o «Indeterminado» si los datos no alcanzan."
                    // v1.3.6: en Imposible no hay pistas, así que no se ofrecen.
                    // v1.3.7: las pistas acercan sin regalar: pregunta, descarte y dos candidatos.
                    + (maxHints() > 0 ? "\nSi no sabes por dónde empezar, toca 💡 Pista: primero una pregunta para hacerte, luego descarta dos, luego te deja con dos candidatos. Nunca te dice la respuesta." : "");
            main.addView(card("CÓMO SE INVESTIGA", howto), lpm(M, W, 0, 10, 0, 12));
            setBtn("Empezar la investigación", B_GREEN);
            return;
        }
        // Guarda v1.3.3: un expediente nunca puede saltar a la conclusión con
        // 0 fragmentos (era el bug del Expediente 1: «0 de 0» y 100%). Si por
        // lo que sea la cola quedó vacía, error y vuelta al selector.
        if (!picking && !intro && queue.isEmpty()) {
            phase = ASK;
            addCompanion();
            main.addView(card("EXPEDIENTE VACÍO", "Este expediente no cargó ningún fragmento, así que no se puede concluir nada. Vuelve y elige otro expediente."), lpm(M, W, 0, 10, 0, 12));
            setBtn("Volver a expedientes", B_GREEN);
            return;
        }
        if (step < queue.size()) {
            phase = ASK;
            cur = queue.get(step);
            hintLevel = 0;
            hintPartner = -1; hintOut1 = -1; hintOut2 = -1;
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
            // v1.3.6: en Imposible no hay pistas (el botón no se muestra).
            int maxH = maxHints();
            if (maxH > 0) {
                hintBtn = tv(a, "💡 Pista (0/" + maxH + ")", 15, BLUE, MEDIUM);
                hintBtn.setGravity(Gravity.CENTER);
                hintBtn.setBackground(round(SURF2, 12, LINE, 1));
                hintBtn.setPadding(dp(14), dp(12), dp(14), dp(12));
                hintBtn.setOnClickListener(v -> showHint());
                ripple(hintBtn, true);
                main.addView(hintBtn, lpm(M, W, 0, 0, 0, 8));
            } else {
                hintBtn = null;
            }
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

    /**
     * Acercamientos no generosos (v1.3.7). Escalón 1: una pregunta para
     * hacerse (nunca nombra el elemento). Escalón 2: descarta dos
     * distractores por su categoría (nunca la correcta). Escalón 3: deja
     * dos candidatos — la correcta y su compañero — con una pista corta
     * cada uno, en orden aleatorio; todavía hay que elegir. La cantidad
     * por dificultad se queda (Normal 3, Difícil 2, Experto 1, Imposible 0).
     */
    private void showHint() {
        int maxH = maxHints();
        if (hintLevel >= maxH) return;
        hintLevel++;
        int ans = PracticeData.elIdx(cur.answer);
        String txt;
        if (hintLevel == 1) {
            // Compañero: el distractor que comparte eje con la correcta, si lo
            // hay (el confundible); si no, el primer distractor. Los otros dos
            // son los que descarta el escalón 2.
            int partner = -1;
            for (String o : cur.opts) {
                int e = PracticeData.elIdx(o);
                if (e != ans && EL_AXIS[e] == EL_AXIS[ans]) { partner = e; break; }
            }
            if (partner < 0) {
                for (String o : cur.opts) {
                    int e = PracticeData.elIdx(o);
                    if (e != ans) { partner = e; break; }
                }
            }
            hintPartner = partner;
            int n = 0;
            for (String o : cur.opts) {
                int e = PracticeData.elIdx(o);
                if (e != ans && e != partner) {
                    if (n == 0) hintOut1 = e; else hintOut2 = e;
                    n++;
                }
            }
            txt = "Pista 1 — " + hintQ1();
        } else if (hintLevel == 2) {
            txt = "Pista 2 — Descarta dos: no es " + EL_CAT[hintOut1] + " ni " + EL_CAT[hintOut2] + ".";
        } else {
            // Los dos candidatos en orden aleatorio: el que acierte el orden
            // por costumbre, pierde.
            String a = EL_CUE[ans], b = EL_CUE[hintPartner];
            boolean swap = Math.random() < 0.5;
            String first = swap ? b : a, second = swap ? a : b;
            String axisBit = EL_AXIS[ans] == EL_AXIS[hintPartner]
                    ? "Los dos son de " + AXIS_NAMES[EL_AXIS[ans]] + ": "
                    : "";
            txt = "Pista 3 — " + axisBit + "quedan dos: uno " + first + " y el otro " + second
                    + ". ¿Cuál dice el fragmento?";
        }
        hintTx.setText(txt);
        hintTx.setVisibility(android.view.View.VISIBLE);
        if (hintBtn != null) hintBtn.setText("💡 Pista (" + hintLevel + "/" + maxH + ")" + (hintLevel >= maxH ? " · ya está todo" : ""));
    }

    /**
     * Pregunta del escalón 1: sobre una dimensión en la que la correcta
     * comparte grupo con al menos otra opción, para que la pregunta sola
     * nunca identifique la respuesta. Si ningún lado tiene pareja, solo
     * queda un consejo de lectura (sin contenido).
     */
    private String hintQ1() {
        int ans = PracticeData.elIdx(cur.answer);
        int expCount = 0, statCount = 0;
        for (String o : cur.opts) {
            int e = PracticeData.elIdx(o);
            if (EL_EXPLICIT[e] == EL_EXPLICIT[ans]) expCount++;
            if (EL_STATIC[e] == EL_STATIC[ans]) statCount++;
        }
        if (expCount >= 2) return "Pregúntate: ¿la persona está mirando algo de fuera — lo que se ve, se hace o se dice — o algo de dentro — lo que se siente o se percibe por dentro?";
        if (statCount >= 2) return "Pregúntate: ¿lo que describe se queda igual, o está cambiando? ¿Es una foto o una película?";
        return "Lee el fragmento otra vez despacio: ¿qué está HACIENDO la persona? Fíjate en el verbo, no en el tema.";
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

    /** Pistas escalonadas por dificultad global (v1.3.6): Normal 3, Difícil 2, Experto 1, Imposible 0. */
    private static int maxHints() {
        int d = 0;
        try { d = Store.I == null ? 0 : Store.I.difficulty; } catch (Exception ignored) { /* sin Store = Normal */ }
        return d <= 0 ? 3 : d == 1 ? 2 : d == 2 ? 1 : 0;
    }

    @Override protected void onBtn() {
        if (phase == END) { a.back(); return; }
        if (picking) {
            if (!picked || chosenH == null || chosenH.getTag() == null) { a.toast("Elige un expediente primero, " + name() + ".", false); return; }
            setCase((Integer) chosenH.getTag());
            if (queue.isEmpty()) {
                a.toast("Ese expediente no cargó fragmentos, " + name() + ". Elige otro.", false);
                picked = false;
                chosenH = null;
                next();
                return;
            }
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
        // Guarda v1.3.3: si se está mostrando el error de expediente vacío,
        // el botón vuelve al selector de expedientes.
        if (!picking && !intro && queue.isEmpty()) {
            picking = true;
            step = 0;
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
