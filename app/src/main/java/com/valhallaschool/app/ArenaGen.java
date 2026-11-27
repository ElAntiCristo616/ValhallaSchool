package com.valhallaschool.app;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Locale;
import java.util.Random;

/**
 * Generador combinatorio de combates de Arena (v1.3.2).
 *
 * TODAS las relaciones: las 256 parejas (16×16) de la matriz canónica
 * de GamesData.REL, verificada contra los documentos SCS (duales 8/8,
 * supervisión base = PoLR 16/16, beneficio creativa = sugestiva 16/16).
 * Cada combate = pareja × problemática cotidiana × turnos, así que los
 * escenarios son MILES y nunca se repiten igual.
 *
 * COHERENCIA, no «al madrazo»: la mediación correcta sale de la dinámica
 * real de la relación (REL_INFO / CORRECT), y la mala es el error clásico
 * de esa misma relación (MISTAKE). La neutral es escucha genérica.
 * Las barras y el combo cómico a tensión 100 con recuperación se
 * mantienen en ArenaScreen; aquí solo se genera el contenido.
 */
final class ArenaGen {

    /** Problemáticas cotidianas (de la biblia §06 + las 6 del MVP). */
    private static final class Prob {
        final String title, context;
        Prob(String t, String c) { title = t; context = c; }
    }

    private static final Prob[] PROBS = {
            new Prob("La última palta", "En la feria queda una sola palta y dos manos la quieren."),
            new Prob("La fila del súper", "En la fila del súper, alguien se cuela y nadie dice nada."),
            new Prob("La impresora", "La impresora de la oficina se atascó justo antes de entregar el informe."),
            new Prob("El almuerzo robado", "Alguien se comió el almuerzo de otro en la cocina de la oficina."),
            new Prob("El corte de pelo", "El peluquero cortó más de lo pedido y el cliente no se reconoce."),
            new Prob("La ventana del micro", "En el micro, uno quiere la ventana abierta y el otro cerrada."),
            new Prob("El estacionamiento", "Dos autos llegan al mismo tiempo al último lugar libre."),
            new Prob("El proyecto", "El proyecto se atrasa y cada uno culpa al otro."),
            new Prob("El ascensor", "El ascensor se quedó entre pisos con dos vecinos adentro."),
            new Prob("El perro del vecino", "El perro del vecino ladra toda la noche y nadie duerme."),
            new Prob("El ruido de la fiesta", "La fiesta de al lado sigue a las tres de la mañana."),
            new Prob("El turno de limpieza", "Nadie quiere hacer el turno de limpieza de la casa."),
            new Prob("El préstamo", "Uno le prestó dinero al otro y ya pasaron tres meses."),
            new Prob("La reunión de vecinos", "En la reunión de vecinos, nadie se pone de acuerdo en la cuota."),
    };

    /**
     * Dinámica real de cada relación (índice = código REL 0..15):
     * {fricción, lo que funciona, lo que empeora}.
     * Del SCS: dualidad = complementación (naturaleza-dual); supervisión =
     * la base del supervisor pisa el PoLR del supervisado (anillos de
     * supervisión); las demás, de la tabla canónica y su descripción clásica.
     */
    private static final String[][] REL_INFO = {
            // 0 Identidad
            {"Los dos ven el mundo igual y se pelean por el mismo papel: cada uno cree que el otro le copia.",
                    "Repartan por tareas concretas y que cada uno cubra el punto débil del otro con alguien más.",
                    "Asumir que el otro ve lo mismo que tú: si tú no lo ves, él tampoco."},
            // 1 Dualidad
            {"Cada uno habla el idioma que el otro no entiende al principio: lo que a uno le sobra, al otro le falta.",
                    "Dejen que cada uno haga su parte fuerte y pidan lo que necesitan en concreto.",
                    "Hacer todo solo o esperar que el otro piense como tú."},
            // 2 Activación
            {"Se animan mutuamente, pero se cansan rápido: la energía dura poco.",
                    "Ráfagas cortas con roles claros y descansos entre medias.",
                    "Maratones largos juntos sin parar: terminan agotados y de mal humor."},
            // 3 Espejo
            {"Ven lo mismo pero al revés: se corrigen el método a cada rato.",
                    "Acuerden primero el objetivo y luego repartan los métodos.",
                    "Corregir el método del otro en plena acción."},
            // 4 Afinidad
            {"Les gusta lo mismo y compiten por el mismo nicho: cada uno cree hacerlo mejor.",
                    "Cada uno en su área: mismo gusto, distinto terreno.",
                    "Comparar quién lo hace mejor delante de todos."},
            // 5 Semidualidad
            {"Una parte encaja y la otra no: uno se entiende y el otro se queda fuera.",
                    "Usen la parte que sí encaja y no fuercen el resto.",
                    "Exigir que el otro te entienda en todo."},
            // 6 Espejismo
            {"Todo es fácil y relajado, pero los malentendidos se acumulan en silencio.",
                    "Manténganlo ligero: no conviertan lo casual en debate serio.",
                    "Cavar hondo en una cosa casual hasta romperla."},
            // 7 Superyó
            {"Distancia cortés: valores distintos y desconfianza mutua.",
                    "Respeto formal y reglas claras, sin intimidad.",
                    "Comentarios personales o intentar acercarse de más."},
            // 8 Conflicto
            {"Cada uno pisa el punto débil del otro sin querer: lo fuerte de uno es lo débil del otro.",
                    "Contacto mínimo, reglas concretas y un tercero que medie; nunca atacar el punto débil.",
                    "Criticar justo donde al otro le duele o atacar en público."},
            // 9 Cuasi-identidad
            {"Se parecen pero son distintos: «eres como yo, pero mal».",
                    "Nombrar la diferencia en voz alta y repartir desde ahí.",
                    "Asumir que están de acuerdo porque suenan igual."},
            // 10 Extinción
            {"Valores opuestos que se apagan mutuamente: aburrimiento y cansancio.",
                    "Contacto breve y centrado en la tarea, sin emociones.",
                    "Discusiones largas y emocionales."},
            // 11 Negocios
            {"Valores distintos pero trabajo posible: cada uno ve el negocio a su manera.",
                    "Repartir por competencia y mantenerlo en lo práctico.",
                    "Meter sentimientos en un asunto de trabajo."},
            // 12 Benefactor
            {"Uno da lo que el otro necesita y no recibe nada a cambio: ayuda de un solo sentido.",
                    "El benefactor ofrece en concreto y el beneficiario acepta sin dar explicaciones.",
                    "El benefactor exigiendo gratitud o el beneficiario criticando la ayuda."},
            // 13 Beneficiario
            {"Recibe ayuda que no pidió y se siente en deuda o molesto.",
                    "Aceptar lo concreto que sirve y agradecer sin más.",
                    "Rechazar la ayuda con desprecio o criticar cómo te ayudan."},
            // 14 Supervisor
            {"El supervisor corrige sin darse cuenta y el supervisado se siente atacado.",
                    "El supervisor suaviza la crítica en sugerencias concretas; el supervisado pide ejemplos.",
                    "Criticar en público o moralizar."},
            // 15 Supervisado
            {"Te corrigen todo el tiempo y no sabes por qué: su fuerte es tu punto débil.",
                    "Pedir ejemplos concretos y no tomarlo como ataque personal.",
                    "Responder a la defensiva o devolver la crítica."},
    };

    /** Mediaciones CORRECTAS por relación (2 por relación): la respuesta que corresponde a la dinámica real. */
    private static final String[][] CORRECT = {
            {"Repartir {P} por tareas concretas: que {A} haga una parte y {B} la otra, sin pisarse.",
                    "Que cada uno diga qué parte de {P} ve y que un tercero junte las dos versiones."},
            {"Preguntarle a {A} qué necesita en concreto y a {B} qué puede aportar de su parte fuerte.",
                    "Proponer que {A} se encargue de su parte fuerte y {B} de la suya, sin meterse en lo del otro."},
            {"Hacer {P} en ráfagas cortas: veinte minutos cada uno y un descanso entre medias.",
                    "Repartir roles claros para {P} y parar antes de que se agoten."},
            {"Acordar primero el objetivo de {P} y luego dejar que cada uno use su método.",
                    "Pedirles que digan el objetivo en voz alta antes de discutir el cómo."},
            {"Darle a cada uno su terreno en {P}: mismo gusto, distinto lugar.",
                    "Que {A} y {B} hagan su parte de {P} por separado y luego la junten."},
            {"Usar la parte de {P} en la que sí se entienden y dejar el resto para después.",
                    "Preguntar en qué parte de {P} están de acuerdo y empezar por ahí."},
            {"Mantener {P} ligero: resolver lo práctico y no convertirlo en debate serio.",
                    "Proponer una solución rápida para {P} y no cavar más hondo."},
            {"Poner reglas formales para {P} y que cada uno se mantenga en su papel.",
                    "Tratar {P} como un trámite: sin comentarios personales, solo lo necesario."},
            {"Poner una regla clara para {P} y que un tercero decida, sin tocar el punto débil de nadie.",
                    "Separarlos un momento y que cada uno diga qué necesita sin criticar al otro."},
            {"Nombrar la diferencia en voz alta: «ustedes suenan igual pero quieren cosas distintas».",
                    "Pedirle a cada uno que diga qué quiere de {P} con sus propias palabras."},
            {"Resolver {P} rápido y en corto: tarea concreta, sin emociones de por medio.",
                    "Ponerle un límite de tiempo a {P}: cinco minutos y se acabó."},
            {"Repartir {P} por competencia: que lo haga quien lo hace mejor y punto.",
                    "Mantener {P} en lo práctico: números, fechas y nada de sentimientos."},
            {"Que {A} ofrezca ayuda concreta para {P} y {B} la acepte sin dar explicaciones.",
                    "Pedirle a {A} que diga exactamente qué puede hacer por {P} y que {B} elija."},
            {"Que {B} acepte lo concreto que sirve de {P} y agradezca sin más.",
                    "Preguntarle a {B} qué parte de la ayuda le sirve y dejar el resto."},
            {"Pedirle a {A} que diga la crítica como sugerencia concreta, no como sentencia.",
                    "Que {B} pregunte «¿qué harías tú en mi lugar?» y tome nota sin discutir."},
            {"Pedirle a {B} ejemplos concretos de lo que le molesta de {P} y no tomarlo como ataque.",
                    "Que {A} explique qué quiere de {P} sin moralizar y {B} responda con hechos."},
    };

    /** Errores CLÁSICOS por relación (2 por relación): la respuesta que empeora justo esa dinámica. */
    private static final String[][] MISTAKE = {
            {"Decirles que si los dos ven lo mismo, uno de los dos sobra.",
                    "Ponerlos a competir por ver quién resuelve {P} mejor."},
            {"Decirle a {A} que haga todo solo porque «tú puedes».",
                    "Esperar que {B} piense como {A} y criticarlo cuando no lo hace."},
            {"Ponerlos a resolver {P} sin parar hasta que terminen.",
                    "Decirles que si se cansan es porque no quieren de verdad."},
            {"Dejar que {A} corrija el método de {B} en plena acción.",
                    "Decirles que discutan el cómo de {P} hasta que uno gane."},
            {"Comparar delante de todos quién hace mejor {P}.",
                    "Ponerlos a pelear por el mismo nicho de {P}."},
            {"Exigirle a {B} que entienda a {A} en todo, sin excepciones.",
                    "Decirles que si no se entienden es porque no se esfuerzan."},
            {"Convertir {P} en un debate serio y profundo hasta romperlo.",
                    "Cavar en lo que cada uno siente de verdad por {P}."},
            {"Hacer un comentario personal sobre {A} delante de {B}.",
                    "Forzarlos a acercarse y a confiar en {P}."},
            {"Decirle a {A} que su forma de hacer las cosas es un desastre.",
                    "Recordarles a los dos sus peores errores frente a todos."},
            {"Asumir que están de acuerdo porque suenan igual.",
                    "Decirle a {B} que copie a {A} porque «es lo mismo»."},
            {"Ponerlos a discutir {P} con emociones hasta que se agoten.",
                    "Decirles que hablen de sus sentimientos sobre {P}."},
            {"Meter sentimientos en {P}: «¿no te importa nada?».",
                    "Decirle a {A} que {B} no lo respeta."},
            {"Exigirle a {B} que agradezca la ayuda de {A} como se debe.",
                    "Decirle a {B} que sin {A} no es nadie."},
            {"Decirle a {B} que rechace la ayuda con desprecio.",
                    "Criticar delante de todos cómo {A} ayuda en {P}."},
            {"Dejar que {A} le explique a {B} todo lo que hace mal, en público.",
                    "Decirle a {B} que si no aguanta la crítica es porque es débil."},
            {"Decirle a {B} que devuelva la crítica con la misma fuerza.",
                    "Poner a {B} a la defensiva: «¿por qué siempre te equivocas?»."},
    };

    /** Escucha genérica (neutral): calma un poco, no resuelve. */
    private static final String[] NEUTRAL = {
            "Decirles que se calmen y respiren un momento.",
            "Preguntar a cada uno qué pasó, sin interrumpir.",
            "Proponer una pausa de cinco minutos.",
            "Repetir lo que cada uno dijo para que se oigan.",
            "Preguntar qué acuerdo les parecería justo a los dos.",
    };

    /** Las 256 parejas (16×16), en orden de la matriz. */
    static final int[][] PAIRS = new int[256][2];
    static {
        int n = 0;
        for (int a = 0; a < 16; a++) for (int b = 0; b < 16; b++) { PAIRS[n][0] = a; PAIRS[n][1] = b; n++; }
    }

    static String pairLabel(int a, int b) { return GamesData.TYPES[a] + " vs " + GamesData.TYPES[b]; }

    private static String baseOf(int t) {
        int el = GamesData.MODELS[t][0];
        return GamesData.ELEMENTS[el] + " · " + GamesData.elName(el);
    }

    private static String fill(String s, int t1, int t2, Prob p) {
        return s.replace("{A}", GamesData.TYPES[t1]).replace("{B}", GamesData.TYPES[t2])
                .replace("{P}", p.title.toLowerCase(Locale.ROOT));
    }

    private static String situation(int rel, int turn, int t1, int t2, Prob p) {
        String a = GamesData.TYPES[t1], b = GamesData.TYPES[t2];
        String pt = p.title.toLowerCase(Locale.ROOT);
        switch (turn) {
            case 0: return a + " y " + b + " se encuentran por " + pt + ". " + REL_INFO[rel][0];
            case 1: return "La cosa sube: " + a + " tira de su lado fuerte (" + baseOf(t1) + ") y " + b
                    + " responde con lo suyo (" + baseOf(t2) + ").";
            default: return "Nadie cede: hay que cerrar " + pt + " sin que explote todo.";
        }
    }

    private static GamesData.ArenaOpt opt(String text, String reaction, int dt, int dc, int dco) {
        GamesData.ArenaOpt o = new GamesData.ArenaOpt();
        o.text = text; o.reaction = reaction; o.dt = dt; o.dc = dc; o.dco = dco;
        return o;
    }

    /**
     * Genera un combate para la pareja (t1, t2): problemática al azar,
     * 3 turnos con opción correcta (dinámica real de la relación),
     * neutral y error clásico, en orden barajado.
     */
    static GamesData.Arena generate(int t1, int t2) {
        int rel = GamesData.REL[t1][t2];
        Random r = new Random();
        int pi = r.nextInt(PROBS.length);
        Prob p = PROBS[pi];
        GamesData.Arena s = new GamesData.Arena();
        s.id = "g" + t1 + "-" + t2 + "-" + pi;
        s.title = p.title;
        s.intro = p.context + " Relación: " + GamesData.relName(t1, t2) + ". " + REL_INFO[rel][0];
        s.relation = GamesData.relName(t1, t2);
        s.t1 = t1; s.t2 = t2;
        for (int turn = 0; turn < 3; turn++) {
            GamesData.ArenaTurn t = new GamesData.ArenaTurn();
            t.situation = situation(rel, turn, t1, t2, p);
            ArrayList<GamesData.ArenaOpt> opts = new ArrayList<>();
            opts.add(opt(fill(CORRECT[rel][r.nextInt(2)], t1, t2, p),
                    "Funciona: " + REL_INFO[rel][1], -20, 20, 15));
            opts.add(opt(NEUTRAL[r.nextInt(NEUTRAL.length)],
                    "Se calman un poco, pero nadie cede.", 5, 5, 5));
            opts.add(opt(fill(MISTAKE[rel][r.nextInt(2)], t1, t2, p),
                    "La tensión sube: " + REL_INFO[rel][2], 25, -10, -10));
            Collections.shuffle(opts, r);
            for (int k = 0; k < 3; k++) t.opts[k] = opts.get(k);
            s.turns[turn] = t;
        }
        s.endGood = "Acuerdo: " + p.title + " se resuelve. " + REL_INFO[rel][1];
        s.endBad = "Escalada: " + p.title + " termina mal. " + REL_INFO[rel][2];
        return s;
    }
}
