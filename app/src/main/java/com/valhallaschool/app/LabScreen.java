package com.valhallaschool.app;

import android.content.ClipData;
import android.view.DragEvent;
import android.view.Gravity;
import android.view.View;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static com.valhallaschool.app.Ui.*;

/**
 * Laboratorio del Modelo A (v1.3.0): mesa con 8 ranuras, fichas por TOQUE
 * (y arrastre si se quiere: pulsación larga). 7 grados: nombres → letras y
 * símbolos → mezcla → solo letras → reconstrucción parcial → deducción por
 * pistas → reparación de modelos imposibles. Los símbolos geométricos NO se
 * improvisan (el doc exige revisión editorial del catálogo): las fichas
 * usan nombres y letras del Modelo A clásico.
 */
class LabScreen extends MiniGame {
    private static final String[] POS_NAMES = {"Base", "Creativa", "Rol", "PoLR", "Sugestiva", "Mobilizing", "Observing", "Demostrativa"};
    private static final String[] GRADES = {
            "Nombres", "Letras y símbolos", "Mezcla", "Solo letras",
            "Reconstrucción parcial", "Deducción por pistas", "Reparación"};
    private static final String[] GRADE_DESC = {
            "Fichas con el nombre completo del elemento",
            "Fichas con letras y un color por elemento",
            "Mitad nombres, mitad letras",
            "Solo las letras: Ne, Ni, Se, Si, Te, Ti, Fe, Fi",
            "4 posiciones ya puestas, completa las otras 4",
            "Adivina el tipo por acercamientos (sin que te regalen nada) y luego arma su modelo",
            "Un modelo imposible: encuentra el error y arréglalo"};

    private int grade = 0; // 0 = elegir grado, 1..7
    private int stage = 0; // 0 grado, 1 pregunta tipo (G6), 2 construir
    private String type;
    private int[] model;
    private final String[] placed = new String[8];
    private final boolean[] locked = new boolean[8];
    private final boolean[] wrongSlot = new boolean[8];
    private String selectedEl;
    private boolean tried, typeOk = true, lastOk;
    private String initialInvalid;
    private final Map<String, TextView> trayViews = new HashMap<>();
    private final TextView[] slotViews = new TextView[8];
    private final List<LinearLayout> optViews = new ArrayList<>();
    private LinearLayout chosenH;
    private final Random rnd = new Random();

    static LabScreen start(MainActivity a) {
        LabScreen s = new LabScreen(a);
        s.next();
        return s;
    }

    private LabScreen(MainActivity a) {
        super(a, "lab", 0);
    }

    /* ---------------- colores por elemento (solo tintes) ---------------- */
    private static int elTint(String el) {
        switch (el) {
            case "Ne": return 0xFFBDD7EE;
            case "Ni": return 0xFFD9C2E9;
            case "Se": return 0xFFF8CBAD;
            case "Si": return 0xFFC6EFCE;
            case "Te": return 0xFFFFE699;
            case "Ti": return 0xFF9BC2E6;
            case "Fe": return 0xFFF4B8C1;
            default: return 0xFFD5A6BD; // Fi
        }
    }

    private String pieceLabel(String el) {
        switch (grade) {
            case 1: return el + " · " + GamesData.elName(el);
            case 2: return el;
            case 3: return (PracticeData.elIdx(el) % 2 == 0) ? el + " · " + GamesData.elName(el) : el;
            case 4: return el;
            default: return el + " · " + GamesData.elName(el);
        }
    }

    private String modelEl(int pos) { return GamesData.ELEMENTS[model[pos]]; }

    /**
     * Acercamientos del grado 6 (v1.3.7). Pista 1: los dos ejes del Ego
     * (quedan 4 tipos). Pista 2: descarta dos cuadras — la otra cuadra de
     * esta combinación de ejes y una ajena — y quedan los 2 tipos de la
     * cuadra correcta: todavía hay que elegir cuál de los dos manda.
     * Nunca se nombra el elemento, la posición ni el tipo.
     */
    private String labHints(int dd) {
        // El Ego mezcla siempre un elemento de percepción (Ne/Ni/Se/Si) con
        // uno de juicio (Te/Ti/Fe/Fi).
        int percEl = -1, judEl = -1;
        for (int k = 0; k < 2; k++) {
            int e = model[k];
            if (e <= 3) percEl = e; else judEl = e;
        }
        String percAxis = (percEl <= 1) ? "intuición" : "sensación";
        String judAxis = (judEl <= 5) ? "lógica" : "ética";
        String p1 = "Pista 1: su Ego mezcla " + percAxis + " y " + judAxis + ". ¿Cuál de las dos manda?";
        if (dd >= 2) return p1 + "\n¿Qué tipo es?";
        int myQ = GamesData.QUADRA[GamesData.typeIdx(type)];
        ArrayList<Integer> comboQ = new ArrayList<>();
        for (int t = 0; t < 16; t++) {
            int[] mm = GamesData.MODELS[t];
            int p = -1, j = -1;
            for (int k = 0; k < 2; k++) { int e = mm[k]; if (e <= 3) p = e; else j = e; }
            String pa = (p <= 1) ? "intuición" : "sensación";
            String ja = (j <= 5) ? "lógica" : "ética";
            if (pa.equals(percAxis) && ja.equals(judAxis)) {
                int q = GamesData.QUADRA[t];
                if (!comboQ.contains(q)) comboQ.add(q);
            }
        }
        int otherQ = comboQ.get(0) == myQ ? comboQ.get(1) : comboQ.get(0);
        int extraQ = -1;
        for (int q = 0; q < 4; q++) if (q != myQ && q != otherQ) { extraQ = q; break; }
        String p2 = "Pista 2: no es de la cuadra " + GamesData.QUADRA_NAMES[otherQ]
                + " ni de la cuadra " + GamesData.QUADRA_NAMES[extraQ] + ".";
        return p1 + "\n" + p2 + "\n¿Qué tipo es?";
    }

    @Override protected void next() {
        main.removeAllViews();
        setFoot(0);
        updateBar();
        comboTx.setText("Modelos: " + answered + " ✓ " + firstOk);
        if (stage == 0) {
            phase = ASK;
            addCompanion();
            main.addView(card("LABORATORIO DEL MODELO A", "Arma los 16 modelos clásicos, posición por posición. "
                    + "Toca una ficha y luego una ranura; también puedes arrastrar la ficha con una pulsación larga."), lpm(M, W, 0, 10, 0, 12));
            // v1.3.6: la dificultad global limita los grados que se ofrecen.
            int dd = 0;
            try { dd = Store.I == null ? 0 : Store.I.difficulty; } catch (Exception ignored) { /* sin Store = Normal */ }
            int minGrade = dd <= 0 ? 1 : dd == 1 ? 3 : dd == 2 ? 5 : 7;
            String note = dd >= 3
                    ? "Dificultad Imposible: solo grado 7."
                    : "Dificultad " + Store.DIFF_NAMES[dd] + ": grados " + minGrade + "–7.";
            main.addView(tv(a, note, 14, ACCENT, MEDIUM), lpm(M, W, 0, 0, 0, 12));
            TextView q = tv(a, "¿Qué grado quieres practicar?", 17, TEXT, MEDIUM);
            main.addView(q, lpm(M, W, 0, 0, 0, 12));
            optViews.clear();
            chosenH = null;
            for (int g = minGrade - 1; g < 7; g++) {
                TextView opt = optBtn((g + 1) + ". " + GRADES[g] + "\n" + GRADE_DESC[g]);
                final int gg = g + 1;
                LinearLayout holder = col(a);
                holder.addView(opt, lp(M, W));
                holder.setOnClickListener(v -> {
                    for (LinearLayout h : optViews) optStyle((TextView) h.getChildAt(0), h == holder ? 1 : 0);
                    chosenH = holder;
                    holder.setTag(gg);
                    setBtn("Empezar", B_GREEN);
                });
                main.addView(holder, lpm(M, W, 0, 0, 0, 10));
                optViews.add(holder);
            }
            setBtn("Empezar", B_OFF);
            return;
        }
        if (stage == 1) {
            phase = ASK;
            addCompanion();
            // v1.3.7: acercamientos, no regalos. Antes la Pista 1 nombraba la
            // base y la Pista 2 la creativa (con eso el tipo quedaba dado).
            // Ahora Pista 1 = los dos ejes del Ego (quedan 4 tipos) y Pista 2
            // = descarta dos cuadras (quedan 2 tipos y hay que elegir cuál
            // manda). Nunca se nombra el elemento, la posición ni el tipo.
            // v1.3.6: con dificultad >=2 solo se muestra la pista 1.
            int dd = 0;
            try { dd = Store.I == null ? 0 : Store.I.difficulty; } catch (Exception ignored) { /* sin Store = Normal */ }
            String pistas = labHints(dd);
            main.addView(card("DEDUCCIÓN POR PISTAS", pistas), lpm(M, W, 0, 10, 0, 12));
            List<String> opts = new ArrayList<>();
            opts.add(type);
            List<String> pool = new ArrayList<>();
            if (dd >= 2) {
                // v1.3.6: distractores cercanos: tipos que comparten la base o la
                // creativa de la respuesta en su Ego (mismo elemento en base o creativa).
                for (String t : GamesData.TYPES) {
                    if (t.equals(type)) continue;
                    int[] m = GamesData.MODELS[GamesData.typeIdx(t)];
                    if (m[0] == model[0] || m[1] == model[1] || m[0] == model[1] || m[1] == model[0]) pool.add(t);
                }
                Collections.shuffle(pool);
                // Si los cercanos no alcanzan, completa con el resto (no debería pasar).
                List<String> rest = new ArrayList<>();
                for (String t : GamesData.TYPES) if (!t.equals(type) && !pool.contains(t)) rest.add(t);
                Collections.shuffle(rest);
                pool.addAll(rest);
            } else {
                for (String t : GamesData.TYPES) pool.add(t);
                Collections.shuffle(pool);
            }
            for (String t : pool) if (!t.equals(type) && opts.size() < 4) opts.add(t);
            Collections.shuffle(opts);
            optViews.clear();
            chosenH = null;
            for (String t : opts) {
                TextView opt = optBtn(t);
                LinearLayout holder = col(a);
                holder.addView(opt, lp(M, W));
                holder.setOnClickListener(v -> {
                    for (LinearLayout h : optViews) optStyle((TextView) h.getChildAt(0), h == holder ? 1 : 0);
                    chosenH = holder;
                    holder.setTag(t);
                    setBtn("Adivinar", B_GREEN);
                });
                main.addView(holder, lpm(M, W, 0, 0, 0, 10));
                optViews.add(holder);
            }
            setBtn("Adivinar", B_OFF);
            return;
        }
        buildBoard();
    }

    private void buildBoard() {
        phase = ASK;
        addCompanion();
        String title = "Reconstruye el modelo de " + type;
        main.addView(card("MODELO DE " + type, title + ". Toca una ficha y luego una ranura para colocarla. "
                + "Toca una ficha ya puesta para recogerla."), lpm(M, W, 0, 10, 0, 12));
        for (int i = 0; i < 8; i++) {
            final int idx = i;
            TextView slot = tv(a, slotText(i), 16, TEXT, MEDIUM);
            slot.setPadding(dp(14), dp(12), dp(14), dp(14));
            slot.setMinHeight(dp(52));
            slotViews[i] = slot;
            styleSlot(i);
            slot.setOnClickListener(v -> onSlotTap(idx));
            slot.setOnDragListener((v, e) -> onSlotDrag(idx, e));
            main.addView(slot, lpm(M, W, 0, 0, 0, 8));
        }
        TextView trayTag = tv(a, "Fichas", 13, BLUE, BLACK);
        main.addView(trayTag, lpm(M, W, 0, 6, 0, 8));
        GridLayout tray = new GridLayout(a);
        tray.setColumnCount(4);
        tray.setRowCount(2);
        trayViews.clear();
        for (String el : GamesData.ELEMENTS) {
            TextView chip = tv(a, pieceLabel(el), 14, TEXT, MEDIUM);
            chip.setGravity(Gravity.CENTER);
            chip.setPadding(dp(6), dp(12), dp(6), dp(12));
            chip.setMinHeight(dp(48));
            chip.setBackground(round(grade == 2 ? elTint(el) : SURF, 12, LINE, 2));
            if (placedHas(el)) chip.setVisibility(View.INVISIBLE);
            final String fEl = el;
            chip.setOnClickListener(v -> {
                selectedEl = selectedEl != null && selectedEl.equals(fEl) ? null : fEl;
                refreshTraySelection();
            });
            chip.setOnLongClickListener(v -> {
                ClipData data = ClipData.newPlainText("el", fEl);
                v.startDragAndDrop(data, new View.DragShadowBuilder(v), fEl, 0);
                return true;
            });
            GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
            lp.width = 0;
            lp.height = GridLayout.LayoutParams.WRAP_CONTENT;
            lp.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            lp.setMargins(dp(4), dp(4), dp(4), dp(4));
            tray.addView(chip, lp);
            trayViews.put(el, chip);
        }
        main.addView(tray, lp(M, W));
        refreshBtn();
    }

    private boolean placedHas(String el) {
        for (String p : placed) if (el.equals(p)) return true;
        return false;
    }

    private String slotText(int i) {
        String base = (i + 1) + " · " + POS_NAMES[i] + "  —  ";
        if (placed[i] == null) return base + "toca para colocar";
        return base + placed[i] + " · " + GamesData.elName(placed[i]) + (locked[i] ? "  🔒" : "");
    }

    private void styleSlot(int i) {
        TextView s = slotViews[i];
        if (s == null) return;
        int border = wrongSlot[i] ? RED : (selectedEl != null && placed[i] == null) ? BLUE : LINE;
        s.setBackground(raised(wrongSlot[i] ? mix(RED, SURF, 0.12f) : SURF, border, 14, 3, border));
        s.setText(slotText(i));
    }

    private void refreshTraySelection() {
        for (Map.Entry<String, TextView> e : trayViews.entrySet()) {
            TextView c = e.getValue();
            boolean sel = e.getKey().equals(selectedEl);
            c.setBackground(round(grade == 2 ? elTint(e.getKey()) : SURF, 12, sel ? BLUE : LINE, sel ? 4 : 2));
        }
        for (int i = 0; i < 8; i++) styleSlot(i);
    }

    private void refreshBtn() {
        boolean full = true;
        for (String p : placed) if (p == null) full = false;
        setBtn(full ? "Comprobar" : "Coloca las 8 fichas", full ? B_GREEN : B_OFF);
    }

    private void onSlotTap(int idx) {
        if (phase != ASK || stage != 2) return;
        if (locked[idx]) { a.toast("Esa posición ya está puesta, " + name() + ".", false); return; }
        if (selectedEl != null) {
            if (placed[idx] != null) {
                String old = placed[idx];
                placed[idx] = null;
                trayViews.get(old).setVisibility(View.VISIBLE);
            }
            placed[idx] = selectedEl;
            trayViews.get(selectedEl).setVisibility(View.INVISIBLE);
            selectedEl = null;
        } else if (placed[idx] != null) {
            selectedEl = placed[idx];
            placed[idx] = null;
            trayViews.get(selectedEl).setVisibility(View.VISIBLE);
        }
        for (int i = 0; i < 8; i++) styleSlot(i);
        refreshTraySelection();
        refreshBtn();
    }

    private boolean onSlotDrag(int idx, DragEvent e) {
        if (phase != ASK || stage != 2) return false;
        switch (e.getAction()) {
            case DragEvent.ACTION_DRAG_STARTED:
                return e.getClipDescription().hasMimeType(android.content.ClipDescription.MIMETYPE_TEXT_PLAIN);
            case DragEvent.ACTION_DROP: {
                if (locked[idx]) return true;
                String el = e.getClipData().getItemAt(0).getText().toString();
                if (placedHas(el)) return true;
                if (placed[idx] != null) trayViews.get(placed[idx]).setVisibility(View.VISIBLE);
                placed[idx] = el;
                trayViews.get(el).setVisibility(View.INVISIBLE);
                selectedEl = null;
                for (int i = 0; i < 8; i++) styleSlot(i);
                refreshTraySelection();
                refreshBtn();
                return true;
            }
            default:
                return true;
        }
    }

    private void startModel() {
        type = GamesData.TYPES[rnd.nextInt(16)];
        model = GamesData.MODELS[GamesData.typeIdx(type)];
        for (int i = 0; i < 8; i++) { placed[i] = null; locked[i] = false; wrongSlot[i] = false; }
        selectedEl = null;
        tried = false;
        typeOk = true;
        initialInvalid = null;
        if (grade == 5) {
            int[] pre = {0, 1, 4, 5};
            for (int i : pre) { placed[i] = modelEl(i); locked[i] = true; }
        } else if (grade == 7) {
            for (int i = 0; i < 8; i++) placed[i] = modelEl(i);
            String tmp = placed[2];
            placed[2] = placed[4];
            placed[4] = tmp;
            initialInvalid = String.join(",", placed);
        }
        stage = grade == 6 ? 1 : 2;
        next();
    }

    @Override protected void onBtn() {
        if (phase == END) { a.back(); return; }
        if (stage == 0) {
            if (chosenH == null || chosenH.getTag() == null) { a.toast("Elige un grado primero, " + name() + ".", false); return; }
            grade = (Integer) chosenH.getTag();
            startModel();
            return;
        }
        if (stage == 1) {
            if (phase == FEEDBACK) { stage = 2; next(); return; }
            if (chosenH == null || chosenH.getTag() == null) { a.toast("Adivina un tipo primero, " + name() + ".", false); return; }
            String guess = (String) chosenH.getTag();
            typeOk = guess.equals(type);
            reactCompanion(typeOk);
            if (typeOk) {
                fbTitle.setText(winTitle("¡Era " + type + ", " + name() + "!"));
                fbText.setText("Base " + GamesData.elName(model[0]) + " y creativa " + GamesData.elName(model[1])
                        + ". Ahora arma el modelo completo.");
                setFoot(1);
            } else {
                fbTitle.setText(failTitle("¡Ni adivinando, " + name() + "!"));
                fbText.setText("Era " + type + ": base " + GamesData.elName(model[0]) + " y creativa "
                        + GamesData.elName(model[1]) + ". Ármalo de todos modos, a ver si al menos eso te sale.");
                hapticBad();
                setFoot(2);
            }
            phase = FEEDBACK;
            setBtn("Armar el modelo", B_GREEN);
            return;
        }
        if (stage == 2) {
            if (phase == FEEDBACK) {
                if (lastOk) {
                    startModel();
                } else {
                    phase = ASK;
                    stage = 2;
                    next();
                }
                return;
            }
            boolean full = true;
            for (String p : placed) if (p == null) full = false;
            if (!full) { a.toast("Faltan fichas, " + name() + ".", false); return; }
            boolean ok = true;
            for (int i = 0; i < 8; i++) {
                wrongSlot[i] = !modelEl(i).equals(placed[i]);
                if (wrongSlot[i]) ok = false;
            }
            boolean impossible = grade == 7 && initialInvalid != null && String.join(",", placed).equals(initialInvalid);
            lastOk = ok;
            answered++;
            if (ok && !tried && typeOk) { firstOk++; award("lab:" + type + ":g" + grade); }
            if (!ok) hapticBad();
            reactCompanion(ok);
            if (ok) {
                fbTitle.setText(winTitle("¡Modelo correcto, " + name() + "!"));
                fbText.setText("Ego: " + modelEl(0) + " + " + modelEl(1) + " · Superego: " + modelEl(2) + " + " + modelEl(3)
                        + " · Superid: " + modelEl(4) + " + " + modelEl(5) + " · Id: " + modelEl(6) + " + " + modelEl(7) + ".");
                setFoot(1);
            } else if (impossible) {
                a.toast("¡Acabas de inventar el sociotipo 17!", true);
                fbTitle.setText(failTitle("¡Acabas de inventar el sociotipo 17, " + name() + "!"));
                fbText.setText("Ese modelo no existe: con base " + GamesData.elName(model[0]) + " la sugestiva (5) tiene que ser "
                        + GamesData.elName(model[4]) + ", su dual, y el rol (3) " + GamesData.elName(model[2])
                        + ". Intercambia las fichas de las posiciones 3 y 5.");
                setFoot(2);
            } else {
                StringBuilder sb = new StringBuilder("Posiciones mal puestas: ");
                boolean first = true;
                for (int i = 0; i < 8; i++) if (wrongSlot[i]) {
                    if (!first) sb.append(", ");
                    sb.append((i + 1)).append(" (").append(POS_NAMES[i]).append(": va ").append(modelEl(i)).append(")");
                    first = false;
                }
                sb.append(". Recuerda: la 5 es el dual de la base y la 6 el dual de la creativa.");
                fbTitle.setText(failTitle("¡Mal puesto, " + name() + "!"));
                fbText.setText(sb.toString());
                setFoot(2);
                tried = true;
            }
            phase = FEEDBACK;
            setBtn(ok ? "Otro modelo" : "Intentar de nuevo", B_GREEN);
            TextView end = tv(a, "Terminar juego", 15, MUTED, MEDIUM);
            end.setGravity(Gravity.CENTER);
            end.setPadding(0, dp(10), 0, dp(6));
            end.setOnClickListener(v -> finish("Laboratorio del Modelo A · grado " + grade + " (" + GRADES[grade - 1] + "). "
                    + "Vuelve a la pestaña Juegos para otro grado."));
            main.addView(end, lpm(M, W, 0, 6, 0, 0));
            for (int i = 0; i < 8; i++) styleSlot(i);
            updateBar();
        }
    }
}
