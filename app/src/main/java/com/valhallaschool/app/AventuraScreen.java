package com.valhallaschool.app;

import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.valhallaschool.app.Ui.*;

/**
 * Aventura por las cuadras (v1.3.0): mapa con las 4 regiones; solo Alfa
 * está abierta (Beta, Gamma y Delta se muestran como futuras áreas, sin
 * fingir disponibilidad). La primera sala, la Biblioteca de Alfa, tiene
 * 5 acertijos que dan claves; la puerta final pide el dual de ILE y dos
 * claves. Las claves ya conseguidas se guardan (gamesXp) y no se repiten.
 */
class AventuraScreen extends MiniGame {
    private int stage = 0; // 0 mapa, 1 sala, 2 acertijo, 3 puerta
    private GamesData.Riddle curRiddle;
    private final Set<String> keys = new HashSet<>();
    private LinearLayout chosenH;
    private final List<LinearLayout> optViews = new ArrayList<>();

    static AventuraScreen start(MainActivity a) {
        AventuraScreen s = new AventuraScreen(a);
        s.next();
        return s;
    }

    private AventuraScreen(MainActivity a) {
        super(a, "aventura", 6);
        for (GamesData.Riddle r : GamesData.AVENTURA) {
            if (Store.I.gamesXp.contains("adv:" + r.point)) keys.add(r.key);
        }
    }

    private boolean riddleDone(GamesData.Riddle r) {
        return Store.I.gamesXp.contains("adv:" + r.point);
    }

    @Override protected void next() {
        main.removeAllViews();
        setFoot(0);
        updateBar();
        chosenH = null;
        if (stage == 0) {
            phase = ASK;
            addCompanion();
            main.addView(card("AVENTURA POR LAS CUADRAS", "Explora las cuatro cuadras del Modelo A. "
                    + "Por ahora solo Alfa está abierta; las demás regiones llegarán después."), lpm(M, W, 0, 10, 0, 12));
            String[][] regions = {
                    {"🟢 Alfa", "Biblioteca de Alfa — abierta", "true"},
                    {"🔵 Beta", "Próximamente — futura área", "false"},
                    {"🟠 Gamma", "Próximamente — futura área", "false"},
                    {"🟣 Delta", "Próximamente — futura área", "false"}};
            optViews.clear();
            for (String[] r : regions) {
                boolean open = Boolean.parseBoolean(r[2]);
                TextView opt = optBtn(r[0] + "\n" + r[1]);
                if (!open) opt.setAlpha(0.55f);
                LinearLayout holder = col(a);
                holder.addView(opt, lp(M, W));
                holder.setTag(open);
                holder.setOnClickListener(x -> {
                    if (!open) { a.toast("Esa región todavía no está abierta, " + name() + ".", false); return; }
                    for (LinearLayout h : optViews) optStyle((TextView) h.getChildAt(0), h == holder ? 1 : 0);
                    chosenH = holder;
                    setBtn("Entrar a Alfa", B_GREEN);
                });
                main.addView(holder, lpm(M, W, 0, 0, 0, 10));
                optViews.add(holder);
            }
            setBtn("Entrar a Alfa", B_OFF);
            return;
        }
        if (stage == 1) {
            phase = ASK;
            addCompanion();
            main.addView(card("BIBLIOTECA DE ALFA", "Cinco puntos esconden un acertijo cada uno. "
                    + "Resuélvelos y guarda las claves en tu inventario. La puerta final necesita 2 claves."), lpm(M, W, 0, 10, 0, 12));
            TextView inv = tv(a, "🎒 Inventario: " + (keys.isEmpty() ? "vacío" : String.join(", ", keys)), 15, ACCENT, MEDIUM);
            main.addView(inv, lpm(M, W, 0, 0, 0, 12));
            optViews.clear();
            for (GamesData.Riddle r : GamesData.AVENTURA) {
                boolean doneR = riddleDone(r);
                TextView opt = optBtn((doneR ? "✓ " : "") + r.point + "\n" + (doneR ? "Resuelto — clave: " + r.key : "Un acertijo te espera"));
                if (doneR) opt.setAlpha(0.6f);
                LinearLayout holder = col(a);
                holder.addView(opt, lp(M, W));
                holder.setTag(r);
                holder.setOnClickListener(x -> {
                    if (riddleDone(r)) { a.toast("Ese acertijo ya lo resolviste, " + name() + ".", false); return; }
                    for (LinearLayout h : optViews) optStyle((TextView) h.getChildAt(0), h == holder ? 1 : 0);
                    chosenH = holder;
                    setBtn("Investigar", B_GREEN);
                });
                main.addView(holder, lpm(M, W, 0, 0, 0, 10));
                optViews.add(holder);
            }
            boolean canOpen = keys.size() >= 2;
            TextView door = optBtn("🚪 Puerta final\n" + (canOpen ? "Tienes " + keys.size() + " claves: puedes intentar abrirla" : "Necesitas 2 claves (tienes " + keys.size() + ")"));
            if (!canOpen) door.setAlpha(0.55f);
            LinearLayout doorH = col(a);
            doorH.addView(door, lp(M, W));
            doorH.setTag("door");
            doorH.setOnClickListener(x -> {
                if (!canOpen) { a.toast("La puerta no se mueve: necesitas 2 claves, " + name() + ".", false); return; }
                for (LinearLayout h : optViews) optStyle((TextView) h.getChildAt(0), h == doorH ? 1 : 0);
                chosenH = doorH;
                setBtn("Abrir la puerta", B_GREEN);
            });
            main.addView(doorH, lpm(M, W, 0, 0, 0, 10));
            optViews.add(doorH);
            setBtn("Investigar", B_OFF);
            comboTx.setText("Claves: " + keys.size() + "/5");
            return;
        }
        if (stage == 2) {
            phase = ASK;
            addCompanion();
            main.addView(card(curRiddle.point.toUpperCase(java.util.Locale.ROOT), curRiddle.question), lpm(M, W, 0, 10, 0, 12));
            optViews.clear();
            for (int i = 0; i < 4; i++) {
                final int ii = i;
                TextView opt = optBtn(curRiddle.opts[i]);
                LinearLayout holder = col(a);
                holder.addView(opt, lp(M, W));
                holder.setTag(ii);
                holder.setOnClickListener(x -> {
                    for (LinearLayout h : optViews) optStyle((TextView) h.getChildAt(0), h == holder ? 1 : 0);
                    chosenH = holder;
                    setBtn("Responder", B_GREEN);
                });
                main.addView(holder, lpm(M, W, 0, 0, 0, 10));
                optViews.add(holder);
            }
            setBtn("Responder", B_OFF);
            return;
        }
        // stage 3: puerta final
        phase = ASK;
        addCompanion();
        main.addView(card("🚪 PUERTA FINAL", GamesData.AVENTURA_DOOR_Q), lpm(M, W, 0, 10, 0, 12));
        optViews.clear();
        for (int i = 0; i < 4; i++) {
            final int ii = i;
            TextView opt = optBtn(GamesData.AVENTURA_DOOR_OPTS[i]);
            LinearLayout holder = col(a);
            holder.addView(opt, lp(M, W));
            holder.setTag(ii);
            holder.setOnClickListener(x -> {
                for (LinearLayout h : optViews) optStyle((TextView) h.getChildAt(0), h == holder ? 1 : 0);
                chosenH = holder;
                setBtn("Abrir", B_GREEN);
            });
            main.addView(holder, lpm(M, W, 0, 0, 0, 10));
            optViews.add(holder);
        }
        setBtn("Abrir", B_OFF);
    }

    @Override protected void onBtn() {
        if (phase == END) { a.back(); return; }
        if (stage == 0) {
            if (chosenH == null) { a.toast("Toca Alfa para entrar, " + name() + ".", false); return; }
            stage = 1;
            next();
            return;
        }
        if (stage == 1) {
            if (phase == FEEDBACK) { stage = 1; next(); return; }
            if (chosenH == null || chosenH.getTag() == null) { a.toast("Elige un punto primero, " + name() + ".", false); return; }
            if ("door".equals(chosenH.getTag())) { stage = 3; next(); return; }
            curRiddle = (GamesData.Riddle) chosenH.getTag();
            stage = 2;
            next();
            return;
        }
        if (phase == FEEDBACK) {
            if (stage == 3) {
                finish("Biblioteca de Alfa completada. Las regiones Beta, Gamma y Delta llegarán en futuras versiones.");
                return;
            }
            stage = 1;
            next();
            return;
        }
        if (chosenH == null || chosenH.getTag() == null) { a.toast("Elige una opción primero, " + name() + ".", false); return; }
        if (stage == 2) {
            int i = (Integer) chosenH.getTag();
            boolean ok = i == curRiddle.answer;
            answered++;
            if (ok) {
                firstOk++;
                award("adv:" + curRiddle.point);
                keys.add(curRiddle.key);
                done = keys.size();
            }
            for (LinearLayout h : optViews) {
                TextView o = (TextView) h.getChildAt(0);
                int hi = (Integer) h.getTag();
                optStyle(o, hi == curRiddle.answer ? 2 : h == chosenH ? 3 : 0);
                h.setOnClickListener(null);
            }
            reactCompanion(ok);
            if (ok) {
                fbTitle.setText(winTitle("¡Clave conseguida, " + name() + "!"));
                fbText.setText("«" + curRiddle.key + "» guardada en tu inventario. " + curRiddle.explanation);
                setFoot(1);
            } else {
                fbTitle.setText(failTitle("¡Esa no era, " + name() + "!"));
                fbText.setText("La respuesta es «" + curRiddle.opts[curRiddle.answer] + "». " + curRiddle.explanation);
                hapticBad();
                setFoot(2);
            }
            phase = FEEDBACK;
            setBtn("Volver a la sala", B_GREEN);
        } else {
            int i = (Integer) chosenH.getTag();
            boolean ok = i == GamesData.AVENTURA_DOOR_ANSWER;
            answered++;
            if (ok) { firstOk++; award("adv:door"); }
            for (LinearLayout h : optViews) {
                TextView o = (TextView) h.getChildAt(0);
                int hi = (Integer) h.getTag();
                optStyle(o, hi == GamesData.AVENTURA_DOOR_ANSWER ? 2 : h == chosenH ? 3 : 0);
                h.setOnClickListener(null);
            }
            reactCompanion(ok);
            if (ok) {
                fbTitle.setText(winTitle("¡La puerta se abrió, " + name() + "!"));
                fbText.setText(GamesData.AVENTURA_DOOR_WHY);
                setFoot(1);
                done = 6;
            } else {
                fbTitle.setText(failTitle("¡La puerta ni se movió, " + name() + "!"));
                fbText.setText("La respuesta es " + GamesData.AVENTURA_DOOR_OPTS[GamesData.AVENTURA_DOOR_ANSWER] + ". " + GamesData.AVENTURA_DOOR_WHY);
                hapticBad();
                setFoot(2);
            }
            phase = FEEDBACK;
            setBtn(ok ? "Celebrar" : "Volver a la sala", B_GREEN);
            if (!ok) stage = 1;
        }
        updateBar();
    }
}
