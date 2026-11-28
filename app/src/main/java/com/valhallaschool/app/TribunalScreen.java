package com.valhallaschool.app;

import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.valhallaschool.app.Ui.*;

/**
 * Tribunal tipológico (v1.3.6): 30 argumentos revisados. El jugador pone el
 * sello (válido / incorrecto / insuficiente); si es incorrecto, localiza el
 * error, lo clasifica en una de 6 categorías y elige la reformulación
 * correcta. Puntaje por argumento: veredicto 40 + categoría 30 + corrección 30.
 * La dificultad global filtra el banco por `diff`.
 */
class TribunalScreen extends MiniGame {
    private final List<GamesData.Trib> queue = new ArrayList<>();
    private int idx = 0;
    private int step = 0; // 0 veredicto, 1 categoría, 2 corrección
    private boolean itemPerfect = true;
    private LinearLayout chosenH;
    private final List<LinearLayout> optViews = new ArrayList<>();

    static TribunalScreen start(MainActivity a) {
        TribunalScreen s = new TribunalScreen(a);
        s.next();
        return s;
    }

    private TribunalScreen(MainActivity a) {
        super(a, "tribunal", 5);
        // v1.3.6: el nivel de dificultad filtra el banco por `diff`.
        int d = 0;
        try { d = Store.I == null ? 0 : Store.I.difficulty; } catch (Exception ignored) { /* sin Store = Normal */ }
        List<GamesData.Trib> all = new ArrayList<>();
        for (GamesData.Trib t : GamesData.TRIBUNAL) if (t.diff <= d) all.add(t);
        if (all.size() < 5) all = new ArrayList<>(GamesData.TRIBUNAL); // guarda: nunca menos de 5
        Collections.shuffle(all);
        queue.addAll(all.subList(0, 5));
    }

    private GamesData.Trib cur() { return queue.get(idx); }

    /** Corrección real: texto con contenido, nunca el placeholder «—» ni vacío. */
    private static boolean isRealCorrection(String c) {
        return c != null && !c.trim().isEmpty() && !c.trim().equals("—");
    }

    /** Palabras de contenido (español) para medir solapamiento entre el
     *  argumento y cada corrección (v1.3.6 fase 2). */
    private static final java.util.Set<String> TRIB_STOP = new java.util.HashSet<>(java.util.Arrays.asList(("que de la el en y los las un una con por para se su es del al como mas pero sus este esta son o a lo no si ya le les mi tu nos te me yo el ella esto eso hay fue han ser sin sobre entre cuando cada tambien muy todo todos asi porque que cual quien donde hasta desde puede pueden tiene tienen esta estan era eran somos suyo suya".split(" "))));

    private static java.util.Set<String> tribWords(String s) {
        java.util.Set<String> out = new java.util.HashSet<>();
        String n = java.text.Normalizer.normalize(s.toLowerCase(java.util.Locale.ROOT), java.text.Normalizer.Form.NFD).replaceAll("[^\\p{ASCII}]", "");
        for (String w : n.split("[^a-z]+")) if (w.length() >= 4 && !TRIB_STOP.contains(w)) out.add(w);
        return out;
    }

    private static int tribOverlap(String a, String b) {
        java.util.Set<String> wa = tribWords(a);
        int n = 0;
        for (String w : tribWords(b)) if (wa.contains(w)) n++;
        return n;
    }

    @Override protected void next() {
        main.removeAllViews();
        setFoot(0);
        updateBar();
        chosenH = null;
        if (idx >= queue.size()) {
            finish("Tribunal tipológico: 5 argumentos revisados. Vuelve a la pestaña Juegos para otra ronda.");
            return;
        }
        phase = ASK;
        addCompanion();
        GamesData.Trib t = cur();
        main.addView(card("ARGUMENTO " + (idx + 1) + " DE " + queue.size(), "«" + t.argument + "»"), lpm(M, W, 0, 10, 0, 12));
        if (step == 0) {
            TextView q = tv(a, "¿Qué sello le pones?", 17, TEXT, MEDIUM);
            main.addView(q, lpm(M, W, 0, 0, 0, 12));
            optViews.clear();
            for (int v = 0; v < 3; v++) {
                final int vv = v;
                String label = GamesData.TRIB_VERDICTS[v] + (v == 0 ? "  ✓" : v == 1 ? "  ✗" : "  ?");
                TextView opt = optBtn(label);
                LinearLayout holder = col(a);
                holder.addView(opt, lp(M, W));
                holder.setTag(vv);
                holder.setOnClickListener(x -> pick(holder));
                main.addView(holder, lpm(M, W, 0, 0, 0, 10));
                optViews.add(holder);
            }
            setBtn("Sellar", B_OFF);
        } else if (step == 1) {
            TextView q = tv(a, "El argumento es incorrecto. ¿Dónde está el error?", 17, TEXT, MEDIUM);
            main.addView(q, lpm(M, W, 0, 0, 0, 12));
            optViews.clear();
            for (int c = 0; c < 5; c++) {
                final int cc = c;
                TextView opt = optBtn(GamesData.TRIB_CATS[c]);
                LinearLayout holder = col(a);
                holder.addView(opt, lp(M, W));
                holder.setTag(cc);
                holder.setOnClickListener(x -> pick(holder));
                main.addView(holder, lpm(M, W, 0, 0, 0, 10));
                optViews.add(holder);
            }
            setBtn("Comprobar", B_OFF);
        } else {
            TextView q = tv(a, "Elige la reformulación correcta:", 17, TEXT, MEDIUM);
            main.addView(q, lpm(M, W, 0, 0, 0, 12));
            List<String> opts = new ArrayList<>();
            opts.add(t.correction);
            // v1.3.6 fase 2: distractores por solapamiento con el argumento
            // (misma categoría primero, mayor solape primero): la correcta ya
            // no es la única que comparte palabras con el argumento. Se baraja
            // el pool antes de ordenar para desempatar los solapes iguales.
            List<GamesData.Trib> pool = new ArrayList<>(GamesData.TRIBUNAL);
            Collections.shuffle(pool);
            List<GamesData.Trib> same = new ArrayList<>(), others = new ArrayList<>();
            for (GamesData.Trib o : pool) {
                if (o.id.equals(t.id) || o.category != t.category || !isRealCorrection(o.correction) || opts.contains(o.correction)) continue;
                same.add(o);
            }
            for (GamesData.Trib o : pool) {
                if (o.id.equals(t.id) || o.category == t.category || !isRealCorrection(o.correction) || opts.contains(o.correction)) continue;
                others.add(o);
            }
            java.util.Comparator<GamesData.Trib> byOv = (x, y) -> tribOverlap(t.argument, y.correction) - tribOverlap(t.argument, x.correction);
            Collections.sort(same, byOv);
            Collections.sort(others, byOv);
            for (GamesData.Trib o : same) {
                if (opts.size() >= 3) break;
                opts.add(o.correction);
            }
            for (GamesData.Trib o : others) {
                if (opts.size() >= 3) break;
                opts.add(o.correction);
            }
            Collections.shuffle(opts);
            optViews.clear();
            for (String corr : opts) {
                TextView opt = optBtn(corr);
                LinearLayout holder = col(a);
                holder.addView(opt, lp(M, W));
                holder.setTag(corr);
                holder.setOnClickListener(x -> pick(holder));
                main.addView(holder, lpm(M, W, 0, 0, 0, 10));
                optViews.add(holder);
            }
            setBtn("Comprobar", B_OFF);
        }
        comboTx.setText("Ronda " + (idx + 1) + "/" + queue.size());
    }

    private void pick(LinearLayout holder) {
        if (phase != ASK) return;
        for (LinearLayout h : optViews) optStyle((TextView) h.getChildAt(0), h == holder ? 1 : 0);
        chosenH = holder;
        setBtn(step == 0 ? "Sellar" : "Comprobar", B_GREEN);
    }

    @Override protected void onBtn() {
        if (phase == END) { a.back(); return; }
        if (phase == FEEDBACK) {
            if (step == 0 && cur().verdict == 1 && itemPerfect) { step = 1; next(); return; }
            if (step == 1 && itemPerfect) { step = 2; next(); return; }
            idx++;
            done = idx;
            step = 0;
            itemPerfect = true;
            next();
            return;
        }
        if (chosenH == null || chosenH.getTag() == null) { a.toast("Elige una opción primero, " + name() + ".", false); return; }
        GamesData.Trib t = cur();
        if (step == 0) {
            int v = (Integer) chosenH.getTag();
            boolean ok = v == t.verdict;
            if (!ok) itemPerfect = false;
            for (LinearLayout h : optViews) {
                TextView o = (TextView) h.getChildAt(0);
                int hv = (Integer) h.getTag();
                optStyle(o, hv == t.verdict ? 2 : h == chosenH ? 3 : 0);
                h.setOnClickListener(null);
            }
            reactCompanion(ok);
            if (ok) {
                fbTitle.setText(winTitle("¡Sello correcto, " + name() + "!"));
                setFoot(1);
                if (t.verdict == 1) {
                    fbText.setText("Es incorrecto. Ahora localiza el error.");
                } else {
                    fbText.setText(t.explanation);
                    finishItem();
                }
            } else {
                fbTitle.setText(failTitle("¡Ese sello no, " + name() + "!"));
                fbText.setText("El veredicto correcto es " + GamesData.TRIB_VERDICTS[t.verdict] + ". " + t.explanation);
                hapticBad();
                setFoot(2);
                finishItem();
            }
            phase = FEEDBACK;
            setBtn(t.verdict == 1 && ok ? "Localizar el error" : "Siguiente", B_GREEN);
        } else if (step == 1) {
            int c = (Integer) chosenH.getTag();
            boolean ok = c == t.category;
            if (!ok) itemPerfect = false;
            for (LinearLayout h : optViews) {
                TextView o = (TextView) h.getChildAt(0);
                int hc = (Integer) h.getTag();
                optStyle(o, hc == t.category ? 2 : h == chosenH ? 3 : 0);
                h.setOnClickListener(null);
            }
            reactCompanion(ok);
            if (ok) {
                fbTitle.setText(winTitle("¡Error localizado, " + name() + "!"));
                fbText.setText("Categoría: " + GamesData.TRIB_CATS[t.category] + ". Ahora reformúlalo bien.");
                setFoot(1);
            } else {
                fbTitle.setText(failTitle("¡Ahí no está el error, " + name() + "!"));
                fbText.setText("La categoría correcta es " + GamesData.TRIB_CATS[t.category] + ". " + t.explanation);
                hapticBad();
                setFoot(2);
            }
            phase = FEEDBACK;
            setBtn(ok ? "Reformular" : "Siguiente", B_GREEN);
            if (!ok) finishItem();
        } else {
            String corr = (String) chosenH.getTag();
            boolean ok = corr.equals(t.correction);
            if (!ok) itemPerfect = false;
            for (LinearLayout h : optViews) {
                TextView o = (TextView) h.getChildAt(0);
                String hc = (String) h.getTag();
                optStyle(o, hc.equals(t.correction) ? 2 : h == chosenH ? 3 : 0);
                h.setOnClickListener(null);
            }
            reactCompanion(ok);
            if (ok) {
                fbTitle.setText(winTitle("¡Reformulación correcta, " + name() + "!"));
                fbText.setText(t.explanation);
                setFoot(1);
            } else {
                fbTitle.setText(failTitle("¡Ni reformulando, " + name() + "!"));
                fbText.setText("La reformulación correcta es: «" + t.correction + "». " + t.explanation);
                hapticBad();
                setFoot(2);
            }
            phase = FEEDBACK;
            setBtn("Siguiente", B_GREEN);
            finishItem();
        }
        updateBar();
    }

    private void finishItem() {
        answered++;
        if (itemPerfect) { firstOk++; award("trib:" + cur().id); }
        Store.I.bumpGameDay("tribunal"); // v1.3.1: cada argumento cuenta para la misión de Retos
    }
}
