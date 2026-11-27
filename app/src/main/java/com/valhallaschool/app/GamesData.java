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
            "Intuición de energía potencial", "Intuición de tiempo",
            "Sensación de fuerza", "Sensación de bienestar",
            "Lógica del trabajo", "Lógica de sistemas",
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
            {1, 4, 3, 6, 2, 7, 0, 5}, // ILI: Ni Te Si Fe Se Fi Ne Ti (v1.3.2: Fe/Fi en 4 y 6 estaban intercambiados)
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
    static String label(int el, int pos) { return "[" + ELEMENTS[el] + " " + PracticeData.POSITIONS_DISPLAY[pos] + "]"; }
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
        // v1.3.2: la Arena ya no se carga de games_arena.json: ArenaGen la
        // genera combinatoriamente (256 parejas × 14 problemáticas).
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

    /* ---- 1. Detective: 50 fragmentos en 5 expedientes (v1.3.3: se eliminó
       «El taller reutilizado»; los casos son 0..4, igual que el índice del
       selector — antes el TSV era 1..6 y el Expediente 1 cargaba 0 fragmentos) ---- */
    static final class Det {
        String id; int caseId, scene; String fragment; int aEl, d1El, d2El, d3El; String justification;
    }

    static final ArrayList<Det> DETECTIVE = new ArrayList<>();
    /** Expedientes: título, introducción, tipo A, tipo B, explicación de cierre, respuesta de cierre (0 tipo A, 1 tipo B, 2 indeterminado). */
    static final String[][] CASES = {
            {"La cafetería", "Una cafetería con una solicitud práctica: alguien pide lo concreto y otro anticipa problemas que nadie ha visto. ¿Qué información se está procesando en cada frase?",
                    "LSE", "IEI", "El tipo es LSE: Te base con Si creativa (avances concretos y cuidado del ambiente). IEI anticipa, pero su base es Ni, no Te.", "0"},
            {"El vecino de las zanahorias", "Un vecino compra doce kilos de zanahorias cada martes y todo el barrio sospecha. Hay pistas, pero no todas determinan un tipo: algunas frases no dicen nada tipológico.",
                    "SEI", "SLI", "Indeterminado: los datos no alcanzan. Comprar mucho de algo puede ser Si, Te o simple costumbre: ninguna frase aislada determina el tipo.", "2"},
            {"La oficina", "Una oficina con un proyecto atrasado: alguien pide resultados medibles y otro anticipa el derrumbe. ¿Qué información se procesa en cada frase?",
                    "LIE", "LSE", "El tipo es LIE: Te base con Ni creativa (rendimiento en el tiempo, no el ambiente físico). LSE comparte Te, pero su creativa es Si: cuida el espacio y la comodidad, no el horizonte.", "0"},
            {"La biblioteca", "Una biblioteca de barrio: alguien juzga a las personas por su lealtad y otro anticipa quién va a triunfar. Marca el foco informacional de cada fragmento.",
                    "EII", "IEI", "El tipo es EII: Fi base con Ne creativa (valora el vínculo y ve el potencial de las personas). IEI comparte la intuición, pero su base es Ni y su creativa Fe: anticipa el ambiente, no juzga el vínculo.", "1"},
            {"El mercado", "Un mercado en día de fiesta: alguien ambienta el lugar con su ánimo y otro impone su presencia. ¿Qué información se está procesando en cada frase?",
                    "ESE", "SEE", "El tipo es ESE: Fe base con Si creativa (el ambiente y el sabor del lugar). SEE comparte la sensación, pero su base es Se y su creativa Fi: impone presencia y cobra lealtades, no ambienta.", "0"},
    };
    /** Títulos de las 3 escenas numeradas de cada expediente (biblia §01). */
    static final String[][] CASE_SCENES = {
            {"La mañana en la cafetería", "El pedido", "El cierre"},
            {"El rumor del barrio", "Las entrevistas", "La contradicción"},
            {"La reunión", "El proyecto", "La entrega"},
            {"La sala de lectura", "El préstamo", "La devolución"},
            {"La llegada al mercado", "El regateo", "La despedida"},
    };

    private static void loadDetective(Context c) {
        try (BufferedReader r = openTsv(c, "games_detective.tsv")) {
            String line;
            boolean first = true;
            while ((line = r.readLine()) != null) {
                if (line.isEmpty()) continue;
                if (first) { first = false; continue; }
                String[] f = line.split("\t", -1);
                if (f.length != 9) continue;
                Det d = new Det();
                d.id = f[0]; d.caseId = Integer.parseInt(f[1]); d.scene = Integer.parseInt(f[2]);
                d.fragment = unquote(f[3]);
                d.aEl = PracticeData.elIdx(f[4]); d.d1El = PracticeData.elIdx(f[5]);
                d.d2El = PracticeData.elIdx(f[6]); d.d3El = PracticeData.elIdx(f[7]);
                d.justification = unquote(f[8]);
                DETECTIVE.add(d);
            }
        } catch (IOException e) {
            throw new IllegalStateException("games_detective.tsv", e);
        }
    }

    /** Fragmentos de un expediente, en orden de escenas (1, 2, 3); dentro de cada escena, barajados. */
    static ArrayList<Det> detectiveOfCase(int caseId) {
        ArrayList<Det> out = new ArrayList<>();
        for (int scene = 1; scene <= 3; scene++) {
            ArrayList<Det> sc = new ArrayList<>();
            for (Det d : DETECTIVE) if (d.caseId == caseId && d.scene == scene) sc.add(d);
            Collections.shuffle(sc, Quiz.R);
            out.addAll(sc);
        }
        return out;
    }

    /* ---- 3. Tribunal: 30 argumentos (v1.3.6: la columna 7 es `diff`) ---- */
    static final class Trib {
        String id, argument, correction, explanation;
        int verdict; // 0 válido, 1 incorrecto, 2 insuficiente
        int category; // 0 estereotipo, 1 conducta≠elemento, 2 posición sin evidencia, 3 contradicción, 4 bloques/anillos, 5 sin error
        int diff; // 0 Normal, 1 Difícil, 2 Experto, 3 Imposible
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
                // v1.3.6: la columna 7 es la dificultad (0-3); «x» vieja = 0.
                t.diff = 0;
                try { t.diff = Integer.parseInt(f[6].trim()); } catch (Exception ignored) { /* «x» o vacío = 0 */ }
                if (t.diff < 0 || t.diff > 3) t.diff = 0;
                TRIBUNAL.add(t);
            }
        } catch (IOException e) {
            throw new IllegalStateException("games_tribunal.tsv", e);
        }
    }

    /* ---- 4. Intruso: 38 conjuntos (v1.3.6: sin `posicion`, con `relacion` y `diff`) ---- */
    static final class Intr {
        String id, kind, rule, property, explanation;
        String[] items = new String[4];
        int answer;
        int diff; // 0 Normal, 1 Difícil, 2 Experto, 3 Imposible
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
                if (f.length != 10 && f.length != 11) continue;
                Intr x = new Intr();
                x.id = f[0]; x.kind = f[1];
                for (int i = 0; i < 4; i++) x.items[i] = f[2 + i];
                x.answer = Integer.parseInt(f[6]); x.rule = unquote(f[7]);
                x.property = unquote(f[8]); x.explanation = unquote(f[9]);
                x.diff = 0;
                if (f.length == 11) {
                    try { x.diff = Integer.parseInt(f[10].trim()); } catch (Exception ignored) { /* vacío = 0 */ }
                    if (x.diff < 0 || x.diff > 3) x.diff = 0;
                }
                INTRUSO.add(x);
            }
        } catch (IOException e) {
            throw new IllegalStateException("games_intruso.tsv", e);
        }
    }

    /* ---- 5. Conexiones: 20 encargos (v1.3.6: columna 11 `diff`) ---- */
    static final class Conn {
        String id, client, goal, explanation;
        int hired, answer;
        int[] cands = new int[4];
        int diff; // 0 Normal, 1 Difícil, 2 Experto, 3 Imposible
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
                if (f.length != 10 && f.length != 11) continue;
                Conn x = new Conn();
                x.id = f[0]; x.client = unquote(f[1]); x.goal = unquote(f[2]);
                x.hired = typeIdx(f[3]);
                for (int i = 0; i < 4; i++) x.cands[i] = typeIdx(f[4 + i]);
                x.answer = Integer.parseInt(f[8]); x.explanation = unquote(f[9]);
                x.diff = 0;
                if (f.length == 11) {
                    try { x.diff = Integer.parseInt(f[10].trim()); } catch (Exception ignored) { /* vacío = 0 */ }
                    if (x.diff < 0 || x.diff > 3) x.diff = 0;
                }
                CONEXIONES.add(x);
            }
        } catch (IOException e) {
            throw new IllegalStateException("games_conexiones.tsv", e);
        }
    }

    /* ---- 8. Lluvia: 48 fichas en 6 rondas (v1.3.6: columnas `round` y `diff`) ---- */
    static final class Rain {
        String id, text, explanation;
        String[] opts = new String[4];
        int answer, nOpts;
        int round, diff; // ronda 0-5, dificultad 0-3
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
                if (f.length != 9 && f.length != 11) continue;
                Rain x = new Rain();
                x.id = f[0]; x.text = unquote(f[1]); x.answer = Integer.parseInt(f[2]);
                x.nOpts = Integer.parseInt(f[3]);
                for (int i = 0; i < 4; i++) x.opts[i] = f[4 + i];
                x.explanation = unquote(f[8]);
                // v1.3.6: round/diff explícitos si están; si no, round = posición/5 y diff = 0.
                x.round = LLUVIA.size() / 5;
                x.diff = 0;
                if (f.length == 11) {
                    try { x.round = Integer.parseInt(f[9].trim()); } catch (Exception ignored) { /* se queda el de posición */ }
                    try { x.diff = Integer.parseInt(f[10].trim()); } catch (Exception ignored) { /* 0 */ }
                    if (x.round < 0 || x.round > 5) x.round = LLUVIA.size() / 5;
                    if (x.diff < 0 || x.diff > 3) x.diff = 0;
                }
                LLUVIA.add(x);
            }
        } catch (IOException e) {
            throw new IllegalStateException("games_lluvia.tsv", e);
        }
    }

    /* ---- 6. Arena: las clases las genera ArenaGen (v1.3.2, combinatorio);
       ya no hay games_arena.json. ---- */
    static final class ArenaOpt {
        String text, reaction; int dt, dc, dco;
        boolean near; // v1.3.6: casi-correcta (dificultad >=2): parece buena pero falla en un detalle fino
    }

    static final class ArenaTurn {
        String situation; ArenaOpt[] opts = new ArenaOpt[3];
    }

    static final class Arena {
        String id, title, intro, relation, endGood, endBad;
        int t1, t2; // tipos enfrentados
        ArenaTurn[] turns = new ArenaTurn[3];
    }

    /* ---- 7. Aventura: 1 sala por cuadra, 9 acertijos + puerta final (JSON, v1.3.6) ---- */
    static final class Riddle {
        String point, question, key, explanation, quadra;
        String[] opts = new String[4];
        int answer;
        int diff; // 0 Normal, 1 Difícil, 2 Experto, 3 Imposible
    }

    /** Sala de una región: sus 5 acertijos y su puerta final. */
    static final class Region {
        String quadra;
        ArrayList<Riddle> riddles = new ArrayList<>();
        String doorQ, doorKeys, doorWhy;
        String[] doorOpts = new String[4];
        int doorAnswer;
    }

    static final ArrayList<Riddle> AVENTURA = new ArrayList<>();
    static String AVENTURA_DOOR_Q, AVENTURA_DOOR_KEYS;
    static final String[] AVENTURA_DOOR_OPTS = new String[4];
    static int AVENTURA_DOOR_ANSWER;
    static String AVENTURA_DOOR_WHY;
    /** Las 4 regiones por cuadra (Beta, Gamma y Delta están cerradas en la
     *  pantalla, pero su contenido ya está filtrado por su cuadra). */
    static final java.util.LinkedHashMap<String, Region> AVENTURA_REGIONS = new java.util.LinkedHashMap<>();

    /** Acertijos de una región por su cuadra (para cuando se abra). */
    static ArrayList<Riddle> riddlesFor(String quadra) {
        Region r = AVENTURA_REGIONS.get(quadra);
        return r == null ? new ArrayList<Riddle>() : r.riddles;
    }

    private static Riddle readRiddle(JSONObject ro) throws org.json.JSONException {
        Riddle r = new Riddle();
        r.point = ro.getString("point"); r.question = ro.getString("question");
        r.key = ro.getString("key"); r.explanation = ro.getString("explanation");
        r.quadra = ro.optString("quadra", "");
        r.diff = ro.optInt("diff", 0);
        if (r.diff < 0 || r.diff > 3) r.diff = 0;
        JSONArray opts = ro.getJSONArray("opts");
        for (int k = 0; k < 4; k++) r.opts[k] = opts.getString(k);
        r.answer = ro.getInt("answer");
        return r;
    }

    private static Region readRegion(JSONObject o, String quadra) throws org.json.JSONException {
        Region reg = new Region();
        reg.quadra = quadra;
        JSONArray rs = o.getJSONArray("riddles");
        for (int i = 0; i < rs.length(); i++) {
            Riddle r = readRiddle(rs.getJSONObject(i));
            // v1.3.5: filtro por cuadra — una región solo sirve acertijos de
            // su propia cuadra; si el JSON trajera uno de otra, no entra.
            if (quadra.equals(r.quadra)) reg.riddles.add(r);
        }
        JSONObject d = o.getJSONObject("door");
        if (!quadra.equals(d.optString("quadra", quadra))) throw new IllegalStateException("games_aventura.json: puerta de " + quadra + " con cuadra distinta");
        reg.doorQ = d.getString("question");
        reg.doorKeys = d.getString("keys");
        JSONArray opts = d.getJSONArray("opts");
        for (int k = 0; k < 4; k++) reg.doorOpts[k] = opts.getString(k);
        reg.doorAnswer = d.getInt("answer");
        reg.doorWhy = d.getString("why");
        return reg;
    }

    private static void loadAventura(Context c) {
        try {
            JSONObject o = new JSONObject(readJson(c, "games_aventura.json"));
            // Alfa: la región abierta (formato anterior: riddles + door en raíz).
            Region alfa = readRegion(o, "Alfa");
            AVENTURA.addAll(alfa.riddles);
            AVENTURA_DOOR_Q = alfa.doorQ;
            AVENTURA_DOOR_KEYS = alfa.doorKeys;
            for (int k = 0; k < 4; k++) AVENTURA_DOOR_OPTS[k] = alfa.doorOpts[k];
            AVENTURA_DOOR_ANSWER = alfa.doorAnswer;
            AVENTURA_DOOR_WHY = alfa.doorWhy;
            AVENTURA_REGIONS.put("Alfa", alfa);
            // Beta, Gamma y Delta: ya filtradas por su cuadra, listas para
            // cuando se abran (sus acertijos solo preguntan de su cuadra).
            for (String q : new String[]{"Beta", "Gamma", "Delta"}) {
                if (o.has(q)) AVENTURA_REGIONS.put(q, readRegion(o.getJSONObject(q), q));
            }
        } catch (Exception e) {
            throw new IllegalStateException("games_aventura.json", e);
        }
    }
}
