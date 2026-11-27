package com.valhallaschool.app;

import android.view.Gravity;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.valhallaschool.app.Ui.*;

/**
 * Conexiones intertipo (v1.3.0): 12 encargos iniciales con la matriz 16×16
 * validada, más un modo libre para explorar cualquier pareja. La dualidad
 * NO es solución universal: cada encargo pide lo que el objetivo necesita,
 * y las direcciones de beneficio y supervisión se respetan (el benefactor
 * ayuda sin que el otro lo note; el supervisor marca errores que el
 * supervisado acepta).
 */
class ConexionesScreen extends MiniGame {
    private int mode = 0; // 0 elegir, 1 encargos, 2 matriz
    private final List<GamesData.Conn> queue = new ArrayList<>();
    private int idx = 0;
    private LinearLayout chosenH;
    private final List<LinearLayout> optViews = new ArrayList<>();
    private String selA, selB;
    private TextView relOut;

    static ConexionesScreen start(MainActivity a) {
        ConexionesScreen s = new ConexionesScreen(a);
        s.next();
        return s;
    }

    private ConexionesScreen(MainActivity a) {
        super(a, "conexiones", 6);
        List<GamesData.Conn> all = new ArrayList<>(GamesData.CONEXIONES);
        Collections.shuffle(all);
        queue.addAll(all.subList(0, 6));
    }

    static String relDesc(int code, String a, String b) {
        switch (code) {
            case 0: return "Mismo tipo: se entienden sin hablar, pero se aburren igual.";
            case 1: return "Dualidad: se complementan en todo; la pareja ideal del Modelo A.";
            case 2: return "Activación: se animan mutuamente, pero se cansan rápido.";
            case 3: return "Espejo: mismos valores, distinto orden; se entienden y se corrigen.";
            case 4: return "Afinidad: mismos elementos fuertes, buena compañía sin fricción.";
            case 5: return "Semidualidad: casi duales, pero con un hueco que se nota.";
            case 6: return "Espejismo: se admiran desde lejos, de cerca no encajan.";
            case 7: return "Superyó: se juzgan por lo que el otro no valora.";
            case 8: return "Conflicto: la peor relación; cada uno pisa la PoLR del otro.";
            case 9: return "Cuasi-identidad: se parecen mucho, pero no se entienden.";
            case 10: return "Extinción: se anulan mutuamente; mejor de lejos.";
            case 11: return "Negocios: cooperan sin intimidad, cada uno en lo suyo.";
            case 12: return a + " es benefactor de " + b + ": le ayuda sin que " + b + " lo note.";
            case 13: return a + " recibe ayuda de " + b + " sin notarlo: " + b + " es su benefactor.";
            case 14: return a + " supervisa a " + b + ": le marca los errores y " + b + " lo acepta.";
            default: return a + " es supervisado por " + b + ": " + b + " le marca los errores.";
        }
    }

    @Override protected void next() {
        main.removeAllViews();
        setFoot(0);
        updateBar();
        chosenH = null;
        if (mode == 0) {
            phase = ASK;
            addCompanion();
            main.addView(card("CONEXIONES INTERTIPO", "La matriz 16×16 del Modelo A: 14 relaciones. "
                    + "En los encargos elige la pareja correcta según el objetivo; en la matriz explora libremente."), lpm(M, W, 0, 10, 0, 12));
            optViews.clear();
            String[][] modes = {{"Encargos", "12 casos reales: elige quién trabaja con quién"}, {"Matriz 16×16", "Explora cualquier pareja y ve su relación"}};
            for (int m = 0; m < 2; m++) {
                final int mm = m + 1;
                TextView opt = optBtn(modes[m][0] + "\n" + modes[m][1]);
                LinearLayout holder = col(a);
                holder.addView(opt, lp(M, W));
                holder.setTag(mm);
                holder.setOnClickListener(x -> {
                    for (LinearLayout h : optViews) optStyle((TextView) h.getChildAt(0), h == holder ? 1 : 0);
                    chosenH = holder;
                    setBtn("Empezar", B_GREEN);
                });
                main.addView(holder, lpm(M, W, 0, 0, 0, 10));
                optViews.add(holder);
            }
            setBtn("Empezar", B_OFF);
            return;
        }
        if (mode == 2) { buildMatrix(); return; }
        if (idx >= queue.size()) {
            finish("Conexiones intertipo: 6 encargos resueltos. Vuelve a la pestaña Juegos para otra ronda.");
            return;
        }
        phase = ASK;
        addCompanion();
        GamesData.Conn c = queue.get(idx);
        main.addView(card("ENCARGO " + (idx + 1) + " DE " + queue.size(), c.client + "\nObjetivo: " + c.goal
                + "\nYa contratado: " + GamesData.TYPES[c.hired]), lpm(M, W, 0, 10, 0, 12));
        TextView q = tv(a, "¿A quién le encargas el trabajo?", 17, TEXT, MEDIUM);
        main.addView(q, lpm(M, W, 0, 0, 0, 12));
        optViews.clear();
        for (int ci : c.cands) {
            String cand = GamesData.TYPES[ci];
            TextView opt = optBtn(cand + "  ·  " + GamesData.QUADRA_NAMES[GamesData.QUADRA[ci]]);
            LinearLayout holder = col(a);
            holder.addView(opt, lp(M, W));
            holder.setTag(ci);
            holder.setOnClickListener(x -> {
                for (LinearLayout h : optViews) optStyle((TextView) h.getChildAt(0), h == holder ? 1 : 0);
                chosenH = holder;
                setBtn("Encargar", B_GREEN);
            });
            main.addView(holder, lpm(M, W, 0, 0, 0, 10));
            optViews.add(holder);
        }
        setBtn("Encargar", B_OFF);
        comboTx.setText("Encargo " + (idx + 1) + "/" + queue.size());
    }

    private void buildMatrix() {
        phase = ASK;
        addCompanion();
        main.addView(card("MATRIZ 16×16", "Toca un tipo en cada fila y mira qué relación hay entre ellos. "
                + "Las relaciones asimétricas (beneficio y supervisión) dependen del orden: A → B no es lo mismo que B → A."), lpm(M, W, 0, 10, 0, 12));
        TextView tagA = tv(a, "Tipo A", 13, BLUE, BLACK);
        main.addView(tagA, lpm(M, W, 0, 0, 0, 8));
        main.addView(typeGrid(true), lp(M, W));
        TextView tagB = tv(a, "Tipo B", 13, BLUE, BLACK);
        main.addView(tagB, lpm(M, W, 0, 14, 0, 8));
        main.addView(typeGrid(false), lp(M, W));
        relOut = tv(a, "Elige un tipo A y un tipo B.", 16, TEXT, MEDIUM);
        relOut.setPadding(dp(4), dp(16), dp(4), dp(4));
        main.addView(relOut, lp(M, W));
        setBtn("Volver", B_GREEN);
        setFoot(0);
    }

    private GridLayout typeGrid(boolean isA) {
        GridLayout g = new GridLayout(a);
        g.setColumnCount(4);
        g.setRowCount(4);
        for (String t : GamesData.TYPES) {
            TextView chip = tv(a, t, 15, TEXT, MEDIUM);
            chip.setGravity(Gravity.CENTER);
            chip.setPadding(dp(4), dp(12), dp(4), dp(12));
            chip.setMinHeight(dp(48));
            final String ft = t;
            chip.setOnClickListener(v -> {
                if (isA) selA = ft; else selB = ft;
                updateMatrix();
            });
            GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
            lp.width = 0;
            lp.height = GridLayout.LayoutParams.WRAP_CONTENT;
            lp.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            lp.setMargins(dp(4), dp(4), dp(4), dp(4));
            g.addView(chip, lp);
            chip.setTag(ft);
            chip.setBackground(round(SURF, 12, LINE, 2));
        }
        g.setTag(isA ? "A" : "B");
        return g;
    }

    private void updateMatrix() {
        for (int i = 0; i < main.getChildCount(); i++) {
            if (main.getChildAt(i) instanceof GridLayout) {
                GridLayout g = (GridLayout) main.getChildAt(i);
                boolean isA = "A".equals(g.getTag());
                for (int j = 0; j < g.getChildCount(); j++) {
                    TextView c = (TextView) g.getChildAt(j);
                    String t = (String) c.getTag();
                    boolean sel = isA ? t.equals(selA) : t.equals(selB);
                    c.setBackground(round(SURF, 12, sel ? BLUE : LINE, sel ? 4 : 2));
                }
            }
        }
        if (selA != null && selB != null) {
            int ai = GamesData.typeIdx(selA), bi = GamesData.typeIdx(selB);
            int code = GamesData.REL[ai][bi];
            relOut.setText(selA + " → " + selB + ": " + GamesData.relName(ai, bi) + "\n" + relDesc(code, selA, selB));
        } else {
            relOut.setText("Elige un tipo A y un tipo B.");
        }
    }

    @Override protected void onBtn() {
        if (phase == END) { a.back(); return; }
        if (mode == 2) { mode = 0; next(); return; }
        if (mode == 0) {
            if (chosenH == null || chosenH.getTag() == null) { a.toast("Elige un modo primero, " + name() + ".", false); return; }
            mode = (Integer) chosenH.getTag();
            next();
            return;
        }
        if (phase == FEEDBACK) {
            idx++;
            done = idx;
            next();
            return;
        }
        if (chosenH == null || chosenH.getTag() == null) { a.toast("Elige a quién primero, " + name() + ".", false); return; }
        GamesData.Conn c = queue.get(idx);
        int pick = (Integer) chosenH.getTag();
        boolean ok = pick == c.cands[c.answer];
        answered++;
        if (ok) { firstOk++; award("conn:" + c.id); }
        int code = GamesData.REL[c.hired][pick];
        for (LinearLayout h : optViews) {
            TextView o = (TextView) h.getChildAt(0);
            int hc = (Integer) h.getTag();
            optStyle(o, hc == c.cands[c.answer] ? 2 : h == chosenH ? 3 : 0);
            h.setOnClickListener(null);
        }
        reactCompanion(ok);
        String rel = GamesData.relName(c.hired, pick);
        if (ok) {
            fbTitle.setText("¡Encargo correcto, " + name() + "! ¿Copiaste, beta?");
            fbText.setText(GamesData.TYPES[c.hired] + " → " + GamesData.TYPES[pick] + ": " + rel + ". " + c.explanation);
            Sfx.play(Sfx.OK);
            setFoot(1);
        } else {
            fbTitle.setText("¡Mal encargo, " + name() + "! ¿Ni eso te sale?");
            fbText.setText(GamesData.TYPES[c.hired] + " → " + GamesData.TYPES[pick] + ": " + rel + ". " + c.explanation);
            hapticBad();
            setFoot(2);
        }
        phase = FEEDBACK;
        setBtn("Siguiente", B_GREEN);
        updateBar();
    }
}
