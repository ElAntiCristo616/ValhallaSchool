package com.valhallaschool.app;

import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.valhallaschool.app.Ui.*;

/**
 * Tribunal tipológico (v1.3.0): 20 argumentos revisados. El jugador pone el
 * sello (válido / incorrecto / insuficiente); si es incorrecto, localiza el
 * error, lo clasifica en una de 6 categorías y elige la reformulación
 * correcta. Puntaje por argumento: veredicto 40 + categoría 30 + corrección 30.
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
        List<GamesData.Trib> all = new ArrayList<>(GamesData.TRIBUNAL);
        Collections.shuffle(all);
        queue.addAll(all.subList(0, 5));
    }

    private GamesData.Trib cur() { return queue.get(idx); }

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
            List<GamesData.Trib> pool = new ArrayList<>(GamesData.TRIBUNAL);
            Collections.shuffle(pool);
            for (GamesData.Trib o : pool) {
                if (!o.id.equals(t.id) && !opts.contains(o.correction) && opts.size() < 3) opts.add(o.correction);
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
                fbTitle.setText("¡Sello correcto, " + name() + "! ¿Copiaste?");
                Sfx.play(Sfx.OK);
                setFoot(1);
                if (t.verdict == 1) {
                    fbText.setText("Es incorrecto. Ahora localiza el error.");
                } else {
                    fbText.setText(t.explanation);
                    finishItem();
                }
            } else {
                fbTitle.setText("¡Ese sello no, " + name() + "! ¿Ni leer sabes?");
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
                fbTitle.setText("¡Error localizado, " + name() + "! Milagro.");
                fbText.setText("Categoría: " + GamesData.TRIB_CATS[t.category] + ". Ahora reformúlalo bien.");
                Sfx.play(Sfx.OK);
                setFoot(1);
            } else {
                fbTitle.setText("¡Ahí no está el error, " + name() + "!");
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
                fbTitle.setText("¡Reformulación correcta, " + name() + "! ¿Casualidad?");
                fbText.setText(t.explanation);
                Sfx.play(Sfx.OK);
                setFoot(1);
            } else {
                fbTitle.setText("¡Ni reformulando, " + name() + "!");
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
    }
}
