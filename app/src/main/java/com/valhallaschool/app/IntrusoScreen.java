package com.valhallaschool.app;

import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.valhallaschool.app.Ui.*;

/**
 * Encuentra al intruso (v1.3.0): 30 conjuntos revisados. De 4 fichas, una
 * no pertenece; después hay que nombrar la propiedad que comparten las
 * otras tres. Se aceptan reglas alternativas válidas: la propiedad
 * correcta está escrita explícitamente en cada conjunto.
 */
class IntrusoScreen extends MiniGame {
    private final List<GamesData.Intr> queue = new ArrayList<>();
    private int idx = 0;
    private int step = 0; // 0 intruso, 1 propiedad
    private boolean itemPerfect = true;
    private LinearLayout chosenH;
    private final List<LinearLayout> optViews = new ArrayList<>();

    static IntrusoScreen start(MainActivity a) {
        IntrusoScreen s = new IntrusoScreen(a);
        s.next();
        return s;
    }

    private IntrusoScreen(MainActivity a) {
        super(a, "intruso", 10);
        List<GamesData.Intr> all = new ArrayList<>(GamesData.INTRUSO);
        Collections.shuffle(all);
        queue.addAll(all.subList(0, 10));
    }

    private GamesData.Intr cur() { return queue.get(idx); }

    @Override protected void next() {
        main.removeAllViews();
        setFoot(0);
        updateBar();
        chosenH = null;
        if (idx >= queue.size()) {
            finish("Encuentra al intruso: 10 conjuntos revisados. Vuelve a la pestaña Juegos para otra ronda.");
            return;
        }
        phase = ASK;
        addCompanion();
        GamesData.Intr t = cur();
        main.addView(card("CONJUNTO " + (idx + 1) + " DE " + queue.size() + " · " + t.kind.toUpperCase(java.util.Locale.ROOT),
                "Regla: " + t.rule), lpm(M, W, 0, 10, 0, 12));
        if (step == 0) {
            TextView q = tv(a, "¿Cuál es el intruso?", 17, TEXT, MEDIUM);
            main.addView(q, lpm(M, W, 0, 0, 0, 12));
            optViews.clear();
            // v1.3.4: los 4 ítems se barajan en cada presentación (en el banco el
            // intruso era el último en 27 de 30 conjuntos); el tag sigue siendo
            // el índice ORIGINAL, así `pick == t.answer` sigue bien.
            int[] order = Quiz.shuffled4();
            for (int i = 0; i < 4; i++) {
                final int ii = order[i];
                TextView opt = optBtn(t.items[ii]);
                LinearLayout holder = col(a);
                holder.addView(opt, lp(M, W));
                holder.setTag(ii);
                holder.setOnClickListener(x -> pick(holder));
                main.addView(holder, lpm(M, W, 0, 0, 0, 10));
                optViews.add(holder);
            }
            setBtn("Comprobar", B_OFF);
        } else {
            TextView q = tv(a, "¿Qué comparten las otras tres?", 17, TEXT, MEDIUM);
            main.addView(q, lpm(M, W, 0, 0, 0, 12));
            List<String> opts = new ArrayList<>();
            opts.add(t.property);
            List<GamesData.Intr> pool = new ArrayList<>(GamesData.INTRUSO);
            Collections.shuffle(pool);
            for (GamesData.Intr o : pool) {
                if (o.kind.equals(t.kind) && !o.id.equals(t.id) && !opts.contains(o.property) && opts.size() < 3) opts.add(o.property);
            }
            while (opts.size() < 3) {
                for (GamesData.Intr o : pool) {
                    if (!o.id.equals(t.id) && !opts.contains(o.property)) { opts.add(o.property); break; }
                }
            }
            Collections.shuffle(opts);
            optViews.clear();
            for (String s : opts) {
                TextView opt = optBtn(s);
                LinearLayout holder = col(a);
                holder.addView(opt, lp(M, W));
                holder.setTag(s);
                holder.setOnClickListener(x -> pick(holder));
                main.addView(holder, lpm(M, W, 0, 0, 0, 10));
                optViews.add(holder);
            }
            setBtn("Comprobar", B_OFF);
        }
        comboTx.setText("Conjunto " + (idx + 1) + "/" + queue.size());
    }

    private void pick(LinearLayout holder) {
        if (phase != ASK) return;
        for (LinearLayout h : optViews) optStyle((TextView) h.getChildAt(0), h == holder ? 1 : 0);
        chosenH = holder;
        setBtn("Comprobar", B_GREEN);
    }

    @Override protected void onBtn() {
        if (phase == END) { a.back(); return; }
        if (phase == FEEDBACK) {
            if (step == 0 && itemPerfect) { step = 1; next(); return; }
            idx++;
            done = idx;
            step = 0;
            itemPerfect = true;
            next();
            return;
        }
        if (chosenH == null || chosenH.getTag() == null) { a.toast("Elige una opción primero, " + name() + ".", false); return; }
        GamesData.Intr t = cur();
        if (step == 0) {
            int pick = (Integer) chosenH.getTag();
            boolean ok = pick == t.answer;
            if (!ok) itemPerfect = false;
            for (LinearLayout h : optViews) {
                TextView o = (TextView) h.getChildAt(0);
                int hi = (Integer) h.getTag();
                optStyle(o, hi == t.answer ? 2 : h == chosenH ? 3 : 0);
                h.setOnClickListener(null);
            }
            reactCompanion(ok);
            if (ok) {
                fbTitle.setText(winTitle("¡Intruso encontrado, " + name() + "!"));
                fbText.setText("El intruso es " + t.items[t.answer] + ". Ahora di qué comparten las otras tres.");
                setFoot(1);
            } else {
                fbTitle.setText(failTitle("¡Ese no era, " + name() + "!"));
                fbText.setText("El intruso es " + t.items[t.answer] + ". " + t.explanation);
                hapticBad();
                setFoot(2);
                finishItem();
            }
            phase = FEEDBACK;
            setBtn(ok ? "Nombrar la propiedad" : "Siguiente", B_GREEN);
        } else {
            String s = (String) chosenH.getTag();
            boolean ok = s.equals(t.property);
            if (!ok) itemPerfect = false;
            for (LinearLayout h : optViews) {
                TextView o = (TextView) h.getChildAt(0);
                String hs = (String) h.getTag();
                optStyle(o, hs.equals(t.property) ? 2 : h == chosenH ? 3 : 0);
                h.setOnClickListener(null);
            }
            reactCompanion(ok);
            if (ok) {
                fbTitle.setText(winTitle("¡Regla correcta, " + name() + "!"));
                fbText.setText(t.explanation);
                setFoot(1);
            } else {
                fbTitle.setText(failTitle("¡Esa no es la regla, " + name() + "!"));
                fbText.setText("Lo que comparten es: " + t.property + ". " + t.explanation);
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
        if (itemPerfect) { firstOk++; award("int:" + cur().id); }
        Store.I.bumpGameDay("intruso"); // v1.3.1: cada conjunto cuenta para la misión de Retos
    }
}
