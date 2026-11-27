package com.valhallaschool.app;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;

/**
 * Datos de los 8 minijuegos (v1.3.0, «Arcade de Socionics»): SOLO Modelo A
 * clásico. Fuente única: los 29 documentos SCS + la excepción autorizada
 * del Modelo A (sociomodel.ru). Los 16 modelos se derivaron de las reglas
 * clásicas y se verificaron contra: el ejemplo ILE del documento, las
 * ideas de la app (base/creativa por elemento), los valores de cuadra y
 * las parejas duales. La matriz 16×16 es la tabla canónica (Wikisocion).
 * FUERA: cargas, dimensionalidad, dicotomías de Reinin y variantes de
 * escuelas. Contenido de los juegos en assets/games_*.tsv/.json.
 */
final class GamesData {
    static final String[] ELEMENTS = PracticeData.ELEMENTS; // Ne Ni Se Si Te Ti Fe Fi
    static final String[] POSITIONS = PracticeData.POSITIONS; // base..fondo
    static final String[] BLOCK_NAMES = {"Ego", "Superego", "Superid", "Id"};
    static final String[] RING_NAMES = {"mental", "vital"};
    static final String[] QUADRA_NAMES = {"Alfa", "Beta", "Gamma", "Delta"};
    /** Nombres completos clásicos de los 8 elementos (español). */
    static final String[] ELEMENT_NAMES = {
            "Intuición de posibilidades", "Intuición de tiempo",
            "Sensación de fuerza", "Sensación de bienestar",
            "Lógica de hechos", "Lógica de sistemas",
            "Ética de emociones", "Ética de relaciones"};

    /** Orden de la matriz canónica (Wikisocion). */
    static final String[] TYPES = {"ILE", "SEI", "ESE", "LII", "EIE", "LSI", "SLE", "IEI",
            "SEE", "ILI", "LIE", "ESI", "LSE", "EII", "IEE", "SLI"};
    /** Cuadra por tipo (mismo orden): 0 Alfa, 1 Beta, 2 Gamma, 3 Delta. */
    static final int[] QUADRA = {0, 0, 0, 0, 1, 1, 1, 1, 2, 2, 2, 2, 3, 3, 3, 3};
    /** Valores de cada cuadra (índices de ELEMENTS). */
    static final int[][] QUADRA_VALUES = {{0, 5, 3, 6}, {2, 5, 1, 6}, {2, 7, 1, 4}, {0, 7, 3, 4}};

    /** Los 16 modelos clásicos (8 posiciones, índices de ELEMENTS). */
    static final int[][] MODELS = {
            {0, 5, 2, 7, 3, 6, 1, 4}, // ILE: Ne Ti Se Fi Si Fe Ni Te
            {3, 6, 1, 4, 0, 5, 2, 7}, // SEI: Si Fe Ni Te Ne Ti Se Fi
            {6, 3, 4, 1, 5, 0, 7, 2}, // ESE: Fe Si Te Ni Ti Ne Fi Se
            {5, 0, 7, 2, 6, 3, 4, 1}, // LII: Ti Ne Fi Se Fe Si Te Ni
            {6, 1, 4, 3, 5, 2, 7, 0}, // EIE: Fe Ni Te Si Ti Se Fi Ne
            {5, 2, 7, 0, 6, 1, 4, 3}, // LSI: Ti Se Fi Ne Fe Ni Te Si
            {2, 5, 0, 7, 1, 6, 3, 4}, // SLE: Se Ti Ne Fi Ni Fe Si Te
            {1, 6, 3, 4, 2, 5, 0, 7}, // IEI: Ni Fe Si Te Se Ti Ne Fi
            {2, 7, 0, 5, 1, 4, 3, 6}, // SEE: Se Fi Ne Ti Ni Te Si Fe
            {1, 4, 3, 7, 2, 6, 0, 5}, // ILI: Ni Te Si Fe Se Fi Ne Ti
            {4, 1, 6, 3, 7, 2, 5, 0}, // LIE: Te Ni Fe Si Fi Se Ti Ne
            {7, 2, 5, 0, 4, 1, 6, 3}, // ESI: Fi Se Ti Ne Te Ni Fe Si
            {4, 3, 6, 1, 7, 0, 5, 2}, // LSE: Te Si Fe Ni Fi Ne Ti Se
            {7, 0, 5, 2, 4, 3, 6, 1}, // EII: Fi Ne Ti Se Te Si Fe Ni
            {0, 7, 2, 5, 3, 4, 1, 6}, // IEE: Ne Fi Se Ti Si Te Ni Fe
            {3, 4, 1, 6, 0, 7, 2, 5}, // SLI: Si Te Ni Fe Ne Fi Se Ti
    };

    /* ---- relaciones intertipo (códigos) ---- */
    static final int R_ID = 0, R_DUAL = 1, R_ACT = 2, R_MIRROR = 3, R_KINDRED = 4, R_SEMIDUAL = 5,
            R_MIRAGE = 6, R_SUPEREGO = 7, R_CONFLICT = 8, R_QUASI = 9, R_EXT = 10, R_BUSINESS = 11,
            R_BENEFACTOR = 12, R_BENEFICIARY = 13, R_SUPERVISOR = 14, R_SUPERVISEE = 15;
    static final String[] REL_NAMES = {"Identidad", "Dualidad", "Activación", "Espejo", "Afinidad",
            "Semidualidad", "Espejismo", "Superyó", "Conflicto", "Cuasi-identidad", "Extinción",
            "Negocios", "Benefactor", "Beneficiario", "Supervisor", "Supervisado"};
    /** Matriz canónica 16×16 (Wikisocion): REL[fila][columna]. */
    static final int[][] REL = {
            {0, 1, 2, 3, 12, 14, 11, 6, 7, 10, 9, 8, 13, 15, 4, 5}, // ILE
            {1, 0, 3, 2, 14, 12, 6, 11, 10, 7, 8, 9, 15, 13, 5, 4}, // SEI
            {2, 3, 0, 1, 4, 5, 13, 15, 9, 8, 7, 10, 11, 6, 12, 14}, // ESE
            {3, 2, 1, 0, 5, 4, 15, 13, 8, 9, 10, 7, 6, 11, 14, 12}, // LII
            {13, 15, 4, 5, 0, 1, 2, 3, 12, 14, 11, 6, 7, 10, 9, 8}, // EIE
            {15, 13, 5, 4, 1, 0, 3, 2, 14, 12, 6, 11, 10, 7, 8, 9}, // LSI
            {11, 6, 12, 14, 2, 3, 0, 1, 4, 5, 13, 15, 9, 8, 7, 10}, // SLE
            {6, 11, 14, 12, 3, 2, 1, 0, 5, 4, 15, 13, 8, 9, 10, 7}, // IEI
            {7, 10, 9, 8, 13, 15, 4, 5, 0, 1, 2, 3, 12, 14, 11, 6}, // SEE
            {10, 7, 8, 9, 15, 13, 5, 4, 1, 0, 3, 2, 14, 12, 6, 11}, // ILI
            {9, 8, 7, 10, 11, 6, 12, 14, 2, 3, 0, 1, 4, 5, 13, 15}, // LIE
            {8, 9, 10, 7, 6, 11, 14, 12, 3, 2, 1, 0, 5, 4, 15, 13}, // ESI
            {12, 14, 11, 6, 7, 10, 9, 8, 13, 15, 4, 5, 0, 1, 2, 3}, // LSE
            {14, 12, 6, 11, 10, 7, 8, 9, 15, 13, 5, 4, 1, 0, 3, 2}, // EII
            {4, 5, 13, 15, 9, 8, 7, 10, 11, 6, 12, 14, 2, 3, 0, 1}, // IEE
            {5, 4, 15, 13, 8, 9, 10, 7, 6, 11, 14, 12, 3, 2, 1, 0}, // SLI
    };

    static int typeIdx(String t) {
        for (int i = 0; i < TYPES.length; i++) if (TYPES[i].equals(t)) return i;
        return -1;
    }

    static int blockOf(int pos) { return pos < 2 ? 0 : pos < 4 ? 1 : pos < 6 ? 2 : 3; }
    static boolean isStrong(int pos) { return pos < 2 || pos >= 6; }
    static boolean isValued(int pos) { return pos < 2 || (pos >= 4 && pos < 6); }
    static boolean isMental(int pos) { return pos < 4; }
    static String label(int el, int pos) { return "[" + ELEMENTS[el] + " " + POSITIONS[pos] + "]"; }
    /** Nombre completo de un elemento, por índice o por letra. */
    static String elName(int el) { return ELEMENT_NAMES[el]; }
    static String elName(String el) { return ELEMENT_NAMES[PracticeData.elIdx(el)]; }
    static String modelLine(int type) {
        StringBuilder b = new StringBuilder();
        for (int p = 0; p < 8; p++) {
            if (p > 0) b.append(" · ");
            b.append(p + 1).append(" ").append(ELEMENTS[MODELS[type][p]]);
        }
        return b.toString();
    }

    /** Nombre de la relación de a hacia b, con dirección en las asimétricas. */
    static String relName(int a, int b) {
        int r = REL[a][b];
        switch (r) {
            case R_BENEFACTOR: return "Beneficio: " + TYPES[a] + " es benefactor de " + TYPES[b];
            case R_BENEFICIARY: return "Beneficio: " + TYPES[a] + " es beneficiario de " + TYPES[b];
            case R_SUPERVISOR: return "Supervisión: " + TYPES[a] + " supervisa a " + TYPES[b];
            case R_SUPERVISEE: return "Supervisión: " + TYPES[a] + " es supervisado por " + TYPES[b];
            default: return REL_NAMES[r];
        }
    }

    /* ================= contenido de los juegos ================= */
    private static boolean loaded;

    private GamesData() {}

    static synchronized void load(Context c) {
        if (loaded) return;
        loadDetective(c);
        loadTribunal(c);
        loadIntruso(c);
        loadConexiones(c);
        loadLluvia(c);
        loadArena(c);
        loadAventura(c);
        loaded = true;
    }

    private static String unquote(String s) {
        if (s.length() >= 2 && s.charAt(0) == '"' && s.charAt(s.length() - 1) == '"')
            return s.substring(1, s.length() - 1).replace("\"\"", "\"");
        return s;
    }

    private static BufferedReader openTsv(Context c, String asset) {
        try {
            return new BufferedReader(new InputStreamReader(c.getAssets().open(asset), StandardCharsets.UTF_8), 1 << 16);
        } catch (IOException e) {
            throw new IllegalStateException("Missing games asset: " + asset, e);
        }
    }

    private static String readJson(Context c, String asset) {
        try (InputStream in = c.getAssets().open(asset)) {
            byte[] buf = new byte[in.available()];
            int n = 0, r;
            while ((r = in.read(buf, n, buf.length - n)) > 0) n += r;
            return new String(buf, 0, n, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("Missing games asset: " + asset, e);
        }
    }

    /* ---- 1. Detective: 30 fragmentos en 3 expedientes ---- */
    static final class Det {
        String id; int caseId; String fragment; int aEl, d1El, d2El, d3El; String justification;
    }

    static final ArrayList<Det> DETECTIVE = new ArrayList<>();
    /** Expedientes: título, introducción, tipo A, tipo B, explicación de cierre, respuesta de cierre (0 tipo A, 1 tipo B, 2 indeterminado). */
    static final String[][] CASES = {
            {"El taller reutilizado", "Un taller de barrio: alguien propone reutilizar piezas viejas para fines nuevos y otro pide un método con resultados verificables. Marca el foco informacional de cada fragmento.",
                    "ILE", "IEE", "El tipo es ILE: Ne base con Ti creativa (reutilizar con sistema). IEE comparte Ne, pero su creativa es Fi, no Ti.", "0"},
            {"La cafetería", "Una cafetería con una solicitud práctica: alguien pide lo concreto y otro anticipa problemas que nadie ha visto. ¿Qué información se está procesando en cada frase?",
                    "LSE", "IEI", "El tipo es LSE: Te base con Si creativa (avances concretos y cuidado del ambiente). IEI anticipa, pero su base es Ni, no Te.", "0"},
            {"El vecino de las zanahorias", "Un vecino compra doce kilos de zanahorias cada martes y todo el barrio sospecha. Hay pistas, pero no todas determinan un tipo: algunas frases no dicen nada tipológico.",
                    "SEI", "SLI", "Indeterminado: los datos no alcanzan. Comprar mucho de algo puede ser Si, Te o simple costumbre: ninguna frase aislada determina el tipo.", "2"},
    };

    private static void loadDetective(Context c) {
        try (BufferedReader r = openTsv(c, "games_detective.tsv")) {
            String line;
            boolean first = true;
            while ((line = r.readLine()) != null) {
                if (line.isEmpty()) continue;
                if (first) { first = false; continue; }
                String[] f = line.split("\t", -1);
                if (f.length != 8) continue;
                Det d = new Det();
                d.id = f[0]; d.caseId = Integer.parseInt(f[1]); d.fragment = unquote(f[2]);
                d.aEl = PracticeData.elIdx(f[3]); d.d1El = PracticeData.elIdx(f[4]);
                d.d2El = PracticeData.elIdx(f[5]); d.d3El = PracticeData.elIdx(f[6]);
                d.justification = unquote(f[7]);
                DETECTIVE.add(d);
            }
        } catch (IOException e) {
            throw new IllegalStateException("games_detective.tsv", e);
        }
    }

    static ArrayList<Det> detectiveOfCase(int caseId) {
        ArrayList<Det> out = new ArrayList<>();
        for (Det d : DETECTIVE) if (d.caseId == caseId) out.add(d);
        Collections.shuffle(out, Quiz.R);
        return out;
    }

    /* ---- 3. Tribunal: 20 argumentos ---- */
    static final class Trib {
        String id, argument, correction, explanation;
        int verdict; // 0 válido, 1 incorrecto, 2 insuficiente
        int category; // 0 estereotipo, 1 conducta≠elemento, 2 posición sin evidencia, 3 contradicción, 4 bloques/anillos, 5 sin error
    }

    static final ArrayList<Trib> TRIBUNAL = new ArrayList<>();
    static final String[] TRIB_VERDICTS = {"Válido", "Incorrecto", "Insuficiente"};
    static final String[] TRIB_CATS = {"Estereotipo", "Conducta ≠ elemento", "Posición sin evidencia",
            "Contradicción estructural", "Confusión de bloques o anillos", "Sin error"};

    private static void loadTribunal(Context c) {
        try (BufferedReader r = openTsv(c, "games_tribunal.tsv")) {
            String line;
            boolean first = true;
            while ((line = r.readLine()) != null) {
                if (line.isEmpty()) continue;
                if (first) { first = false; continue; }
                String[] f = line.split("\t", -1);
                if (f.length != 7) continue;
                Trib t = new Trib();
                t.id = f[0]; t.argument = unquote(f[1]); t.verdict = Integer.parseInt(f[2]);
                t.category = Integer.parseInt(f[3]); t.correction = unquote(f[4]); t.explanation = unquote(f[5]);
                // f[6] reservado
                TRIBUNAL.add(t);
            }
        } catch (IOException e) {
            throw new IllegalStateException("games_tribunal.tsv", e);
        }
    }

    /* ---- 4. Intruso: 30 conjuntos ---- */
    static final class Intr {
        String id, kind, rule, property, explanation;
        String[] items = new String[4];
        int answer;
    }

    static final ArrayList<Intr> INTRUSO = new ArrayList<>();

    private static void loadIntruso(Context c) {
        try (BufferedReader r = openTsv(c, "games_intruso.tsv")) {
            String line;
            boolean first = true;
            while ((line = r.readLine()) != null) {
                if (line.isEmpty()) continue;
                if (first) { first = false; continue; }
                String[] f = line.split("\t", -1);
                if (f.length != 10) continue;
                Intr x = new Intr();
                x.id = f[0]; x.kind = f[1];
                for (int i = 0; i < 4; i++) x.items[i] = f[2 + i];
                x.answer = Integer.parseInt(f[6]); x.rule = unquote(f[7]);
                x.property = unquote(f[8]); x.explanation = unquote(f[9]);
                INTRUSO.add(x);
            }
        } catch (IOException e) {
            throw new IllegalStateException("games_intruso.tsv", e);
        }
    }

    /* ---- 5. Conexiones: 12 encargos ---- */
    static final class Conn {
        String id, client, goal, explanation;
        int hired, answer;
        int[] cands = new int[4];
    }

    static final ArrayList<Conn> CONEXIONES = new ArrayList<>();

    private static void loadConexiones(Context c) {
        try (BufferedReader r = openTsv(c, "games_conexiones.tsv")) {
            String line;
            boolean first = true;
            while ((line = r.readLine()) != null) {
                if (line.isEmpty()) continue;
                if (first) { first = false; continue; }
                String[] f = line.split("\t", -1);
                if (f.length != 10) continue;
                Conn x = new Conn();
                x.id = f[0]; x.client = unquote(f[1]); x.goal = unquote(f[2]);
                x.hired = typeIdx(f[3]);
                for (int i = 0; i < 4; i++) x.cands[i] = typeIdx(f[4 + i]);
                x.answer = Integer.parseInt(f[8]); x.explanation = unquote(f[9]);
                CONEXIONES.add(x);
            }
        } catch (IOException e) {
            throw new IllegalStateException("games_conexiones.tsv", e);
        }
    }

    /* ---- 8. Lluvia: 30 fichas en 6 rondas ---- */
    static final class Rain {
        String id, text, explanation;
        String[] opts = new String[4];
        int answer, nOpts;
    }

    static final ArrayList<Rain> LLUVIA = new ArrayList<>();

    private static void loadLluvia(Context c) {
        try (BufferedReader r = openTsv(c, "games_lluvia.tsv")) {
            String line;
            boolean first = true;
            while ((line = r.readLine()) != null) {
                if (line.isEmpty()) continue;
                if (first) { first = false; continue; }
                String[] f = line.split("\t", -1);
                if (f.length != 9) continue;
                Rain x = new Rain();
                x.id = f[0]; x.text = unquote(f[1]); x.answer = Integer.parseInt(f[2]);
                x.nOpts = Integer.parseInt(f[3]);
                for (int i = 0; i < 4; i++) x.opts[i] = f[4 + i];
                x.explanation = unquote(f[8]);
                LLUVIA.add(x);
            }
        } catch (IOException e) {
            throw new IllegalStateException("games_lluvia.tsv", e);
        }
    }

    /* ---- 6. Arena: 6 escenarios (JSON) ---- */
    static final class ArenaOpt {
        String text, reaction; int dt, dc, dco;
    }

    static final class ArenaTurn {
        String situation; ArenaOpt[] opts = new ArenaOpt[3];
    }

    static final class Arena {
        String id, title, intro, relation, endGood, endBad;
        int t1, t2; // tipos enfrentados
        ArenaTurn[] turns = new ArenaTurn[3];
    }

    static final ArrayList<Arena> ARENA = new ArrayList<>();

    private static void loadArena(Context c) {
        try {
            JSONArray arr = new JSONArray(readJson(c, "games_arena.json"));
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                Arena s = new Arena();
                s.id = o.getString("id"); s.title = o.getString("title"); s.intro = o.getString("intro");
                s.relation = o.getString("relation"); s.endGood = o.getString("endGood"); s.endBad = o.getString("endBad");
                s.t1 = typeIdx(o.getString("t1")); s.t2 = typeIdx(o.getString("t2"));
                JSONArray turns = o.getJSONArray("turns");
                for (int t = 0; t < 3; t++) {
                    JSONObject to = turns.getJSONObject(t);
                    ArenaTurn turn = new ArenaTurn();
                    turn.situation = to.getString("situation");
                    JSONArray opts = to.getJSONArray("opts");
                    for (int k = 0; k < 3; k++) {
                        JSONObject oo = opts.getJSONObject(k);
                        ArenaOpt op = new ArenaOpt();
                        op.text = oo.getString("text"); op.reaction = oo.getString("reaction");
                        op.dt = oo.getInt("dt"); op.dc = oo.getInt("dc"); op.dco = oo.getInt("dco");
                        turn.opts[k] = op;
                    }
                    s.turns[t] = turn;
                }
                ARENA.add(s);
            }
        } catch (Exception e) {
            throw new IllegalStateException("games_arena.json", e);
        }
    }

    /* ---- 7. Aventura: 1 sala, 5 acertijos + puerta final (JSON) ---- */
    static final class Riddle {
        String point, question, key, explanation;
        String[] opts = new String[4];
        int answer;
    }

    static final ArrayList<Riddle> AVENTURA = new ArrayList<>();
    static String AVENTURA_DOOR_Q, AVENTURA_DOOR_KEYS;
    static final String[] AVENTURA_DOOR_OPTS = new String[4];
    static int AVENTURA_DOOR_ANSWER;
    static String AVENTURA_DOOR_WHY;

    private static void loadAventura(Context c) {
        try {
            JSONObject o = new JSONObject(readJson(c, "games_aventura.json"));
            JSONArray rs = o.getJSONArray("riddles");
            for (int i = 0; i < rs.length(); i++) {
                JSONObject ro = rs.getJSONObject(i);
                Riddle r = new Riddle();
                r.point = ro.getString("point"); r.question = ro.getString("question");
                r.key = ro.getString("key"); r.explanation = ro.getString("explanation");
                JSONArray opts = ro.getJSONArray("opts");
                for (int k = 0; k < 4; k++) r.opts[k] = opts.getString(k);
                r.answer = ro.getInt("answer");
                AVENTURA.add(r);
            }
            JSONObject d = o.getJSONObject("door");
            AVENTURA_DOOR_Q = d.getString("question");
            AVENTURA_DOOR_KEYS = d.getString("keys");
            JSONArray opts = d.getJSONArray("opts");
            for (int k = 0; k < 4; k++) AVENTURA_DOOR_OPTS[k] = opts.getString(k);
            AVENTURA_DOOR_ANSWER = d.getInt("answer");
            AVENTURA_DOOR_WHY = d.getString("why");
        } catch (Exception e) {
            throw new IllegalStateException("games_aventura.json", e);
        }
    }
}
