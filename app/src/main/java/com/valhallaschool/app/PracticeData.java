package com.valhallaschool.app;

import android.content.Context;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Banco de práctica (modo Científico): ejercicios de identificación de
 * función+posición sacados de los 4 documentos de cuadras de Remy
 * (46 personajes, TODOS anónimos en la app: el TSV no trae nombres,
 * obras ni tipos). Cargado una vez desde assets/practice.tsv.
 *
 * Columnas (14): exercise_id, quadra, level, kind, prompt,
 * answer_element, answer_position, d1_element, d1_position,
 * d2_element, d2_position, d3_element, d3_position, justification.
 * d1 = mismo elemento / distinta posición; d2 = misma posición /
 * distinto elemento; d3 = distinto de la respuesta.
 */
final class PracticeData {
    static final String[] ELEMENTS = {"Ne", "Ni", "Se", "Si", "Te", "Ti", "Fe", "Fi"};
    static final String[] POSITIONS = {"base", "creativa", "rol", "dolorosa", "sugestiva", "activación", "limitativa", "fondo"};
    /** Nombres de posiciones para MOSTRAR (v1.3.2, nombres estándar en inglés pedidos por Remy).
     *  POSITIONS sigue siendo el código de datos del TSV; la etiqueta visible usa este arreglo. */
    static final String[] POSITIONS_DISPLAY = {"Base", "Creativa", "Rol", "PoLR", "Sugestiva", "Mobilizing", "Observing", "Demostrativa"};
    static final String[] QUADRAS = {"Alfa", "Beta", "Gamma", "Delta"};
    /** Nombres de nivel para la UI y los logros. */
    static final String[] LEVEL_NAMES = {"Cimientos", "Espejo social", "Lo que se anhela", "Maestría"};
    static final int LEVELS = 4;

    static final class Ex {
        String id;
        int quadra, level; // level 1..4
        String kind; // frase | comportamiento | acción
        String prompt;
        int aEl, aPos, d1El, d1Pos, d2El, d2Pos, d3El, d3Pos;
        String justification;

        /** Etiqueta de respuesta, p. ej. "[Ne rol]". Usa el nombre visible (v1.3.2). */
        String label(int el, int pos) { return "[" + ELEMENTS[el] + " " + POSITIONS_DISPLAY[pos] + "]"; }
        String answerLabel() { return label(aEl, aPos); }
        String kindLabel() {
            switch (kind) {
                case "frase": return "FRASE";
                case "comportamiento": return "COMPORTAMIENTO";
                default: return "ACCIÓN";
            }
        }
    }

    static final ArrayList<Ex> ALL = new ArrayList<>();
    private static boolean loaded;

    private PracticeData() {}

    static synchronized void load(Context c) {
        if (loaded) return;
        try (BufferedReader r = new BufferedReader(new InputStreamReader(c.getAssets().open("practice.tsv"), StandardCharsets.UTF_8), 1 << 16)) {
            String line;
            boolean first = true;
            while ((line = r.readLine()) != null) {
                if (line.isEmpty()) continue;
                if (first) { first = false; continue; } // cabecera
                String[] f = line.split("\t", -1);
                if (f.length != 14) continue;
                Ex e = new Ex();
                e.id = f[0];
                e.quadra = quadraIdx(f[1]);
                e.level = Integer.parseInt(f[2]);
                e.kind = f[3];
                e.prompt = unquote(f[4]);
                e.aEl = elIdx(f[5]); e.aPos = posIdx(f[6]);
                e.d1El = elIdx(f[7]); e.d1Pos = posIdx(f[8]);
                e.d2El = elIdx(f[9]); e.d2Pos = posIdx(f[10]);
                e.d3El = elIdx(f[11]); e.d3Pos = posIdx(f[12]);
                e.justification = unquote(f[13]);
                ALL.add(e);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Missing practice asset", e);
        }
        loaded = true;
    }

    /** Quita el entrecomillado estilo CSV ("..." con "" escapado) si lo hay. */
    private static String unquote(String s) {
        if (s.length() >= 2 && s.charAt(0) == '"' && s.charAt(s.length() - 1) == '"')
            return s.substring(1, s.length() - 1).replace("\"\"", "\"");
        return s;
    }

    static int elIdx(String el) {
        for (int i = 0; i < ELEMENTS.length; i++) if (ELEMENTS[i].equals(el)) return i;
        return 0;
    }

    static int posIdx(String pos) {
        for (int i = 0; i < POSITIONS.length; i++) if (POSITIONS[i].equals(pos)) return i;
        return 0;
    }

    static int quadraIdx(String q) {
        for (int i = 0; i < QUADRAS.length; i++) if (QUADRAS[i].equals(q)) return i;
        return 0;
    }

    /** Ejercicios de un nivel (1..4), barajados. */
    static List<Ex> ofLevel(int level) {
        ArrayList<Ex> out = new ArrayList<>();
        for (Ex e : ALL) if (e.level == level) out.add(e);
        Collections.shuffle(out, Quiz.R);
        return out;
    }

    static int countLevel(int level) {
        int n = 0;
        for (Ex e : ALL) if (e.level == level) n++;
        return n;
    }
}
