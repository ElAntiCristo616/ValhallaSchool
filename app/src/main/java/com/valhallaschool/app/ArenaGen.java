package com.valhallaschool.app;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Locale;
import java.util.Random;

/**
 * Generador combinatorio de combates de Arena (v1.3.6).
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
 * v1.3.6: finales aleatorios con sentido sociónico (END_GOOD/END_BAD por
 * relación, anclados en funciones débiles), distractores de teoría
 * confundibles (REL_CONFUSABLE) y near-miss por dificultad.
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
            {"Acordar primero el objetivo de {P} y luego dejar que cada uno lo haga a su manera.",
                    "Pedirles que digan el objetivo en voz alta antes de discutir el cómo."},
            {"Darle a cada uno su terreno en {P}: mismo gusto, distinto lugar.",
                    "Que {A} y {B} hagan su parte de {P} por separado y luego la junten."},
            {"Usar la zona de {P} en la que sí se entienden y dejar el resto para después.",
                    "Preguntar en qué zona de {P} están de acuerdo y empezar por ahí."},
            {"Mantener {P} ligero: resolver lo práctico y no convertirlo en debate serio.",
                    "Proponer una solución rápida para {P} y no cavar más hondo."},
            {"Poner reglas formales para {P} y que cada uno se mantenga en su papel.",
                    "Tratar {P} como un trámite: sin comentarios personales, solo lo necesario."},
            {"Poner una regla clara para {P} y que un tercero decida, sin tocar lo que a cada uno le duele.",
                    "Separarlos un momento y que cada uno diga qué necesita sin criticar al otro."},
            {"Nombrar la diferencia en voz alta: «ustedes suenan igual pero quieren cosas distintas».",
                    "Pedirle a cada uno que diga qué quiere de {P} con sus propias palabras."},
            {"Resolver {P} rápido y en corto: tarea concreta, sin emociones de por medio.",
                    "Ponerle un límite de tiempo a {P}: cinco minutos y se acabó."},
            {"Repartir {P} por competencia: que lo haga quien lo hace mejor y punto.",
                    "Mantener {P} en lo práctico: números, fechas y nada de sentimientos."},
            {"Que {A} ofrezca lo concreto que sirve para {P} y {B} lo acepte sin dar explicaciones.",
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
            {"Decirles que si los dos ven lo mismo en {P}, uno de los dos sobra.",
                    "Ponerlos a competir por ver quién resuelve {P} mejor."},
            {"Decirle a {A} que haga {P} todo solo porque «tú puedes».",
                    "Esperar que {B} piense como {A} en {P} y criticarlo cuando no lo hace."},
            {"Ponerlos a resolver {P} sin parar hasta que terminen.",
                    "Decirles que si se cansan con {P} es porque no quieren de verdad."},
            {"Dejar que {A} corrija el método de {B} para {P} en plena acción.",
                    "Decirles que discutan el cómo de {P} hasta que uno gane."},
            {"Comparar delante de todos quién hace mejor {P}.",
                    "Ponerlos a pelear por el mismo nicho de {P}."},
            {"Exigirle a {B} que entienda a {A} en todo lo de {P}, sin excepciones.",
                    "Decirles que si no se entienden en {P} es porque no se esfuerzan."},
            {"Convertir {P} en un debate serio y profundo hasta romperlo.",
                    "Cavar en lo que cada uno siente de verdad por {P}."},
            {"Hacer un comentario personal sobre {A} delante de {B} por lo de {P}.",
                    "Forzarlos a acercarse y a confiar en {P}."},
            {"Decirle a {A} que su forma de hacer {P} es un desastre.",
                    "Recordarles a los dos sus peores errores en {P} frente a todos."},
            {"Asumir que están de acuerdo en {P} porque suenan igual.",
                    "Decirle a {B} que copie a {A} en {P} porque «es lo mismo»."},
            {"Ponerlos a discutir {P} con emociones hasta que se agoten.",
                    "Decirles que hablen de sus sentimientos sobre {P}."},
            {"Meter sentimientos en {P}: «¿no te importa nada?».",
                    "Decirle a {A} que {B} no lo respeta en {P}."},
            {"Exigirle a {B} que agradezca la ayuda de {A} en {P} como se debe.",
                    "Decirle a {B} que sin {A} no es nadie en {P}."},
            {"Decirle a {B} que rechace la ayuda para {P} con desprecio.",
                    "Criticar delante de todos cómo {A} ayuda en {P}."},
            {"Dejar que {A} le explique a {B} todo lo que hace mal en {P}, en público.",
                    "Decirle a {B} que si no aguanta la crítica sobre {P} es porque es débil."},
            {"Decirle a {B} que devuelva la crítica sobre {P} con la misma fuerza.",
                    "Poner a {B} a la defensiva por {P}: «¿por qué siempre te equivocas?»."},
    };

    /** Escucha genérica (neutral): calma un poco, no resuelve. */
    private static final String[] NEUTRAL = {
            "Decirles que se calmen y respiren un momento antes de seguir con {P}.",
            "Preguntar a cada uno qué pasó con {P}, sin interrumpir.",
            "Proponer una pausa de cinco minutos en {P}.",
            "Repetir lo que cada uno dijo de {P} para que se oigan.",
            "Preguntar qué acuerdo sobre {P} les parecería justo a los dos.",
    };

    /**
     * Finales BUENOS (v1.3.6, 2 por relación): humor absurdo con giro
     * irónico, anclados en la dinámica real de la relación y en las
     * funciones débiles de los dos tipos. Placeholders: {A}, {B}, {P},
     * {R} y los de función débil ({AB} base de A, {BP} PoLR de B...).
     * Cada final menciona a los DOS tipos.
     */
    private static final String[][] END_GOOD = {
            // 0 Identidad
            {"Acuerdo: {A} y {B} descubren que piensan exactamente igual sobre {P}, así que se reparten el trabajo por turnos y terminan antes de lo previsto, cada uno convencido de que la idea fue suya. Puro acuerdo.",
                    "Acuerdo: {A} le dice a {B} lo que haría él y {B} asiente porque es justo lo que pensaba; {P} se resuelve en silencio, sin que nadie tenga que explicarle nada al otro."},
            // 1 Dualidad
            {"Acuerdo: {A} hace su parte fuerte sin que nadie se lo pida y {B} recibe la ayuda con gratitud; {P} se resuelve solo, como si los dos hubieran nacido para eso.",
                    "Acuerdo: {B} le pide a {A} justo lo que {A} sabe dar y {A} se lo da sin dramas; {P} queda resuelto y los dos terminan más seguros que antes. Todo funcionando como debe."},
            // 2 Activación
            {"Acuerdo: {A} y {B} resuelven {P} en una ráfaga de veinte minutos, con roles claros, y paran antes de agotarse: energía corta, resultado completo. En su mejor forma.",
                    "Acuerdo: {A} anima a {B}, {B} anima a {A}, y {P} se resuelve entre los dos antes de que a ninguno le dé tiempo de cansarse."},
            // 3 Espejo
            {"Acuerdo: {A} y {B} acuerdan primero el objetivo de {P} y luego cada uno usa su método; el resultado es el mismo y nadie tuvo que corregir a nadie.",
                    "Acuerdo: {A} le muestra a {B} su método para {P}, {B} le muestra el suyo, y los dos se van con un método mejor que el que traían."},
            // 4 Afinidad
            {"Acuerdo: {A} y {B} se reparten {P} por terrenos: mismo gusto, distinto lugar, y cada uno hace su parte sin pisar la del otro.",
                    "Acuerdo: {A} hace su parte de {P} por un lado y {B} la suya por el otro; al juntarlas descubren que les salió casi lo mismo, y se ríen."},
            // 5 Semidualidad
            {"Acuerdo: {A} y {B} usan la parte de {P} en la que sí se entienden y dejan el resto para después; lo que encaja, encaja de verdad.",
                    "Acuerdo: {A} le da a {B} lo que sí puede darle para {P} y {B} deja de pedirle lo que no tiene; con eso basta y {P} se resuelve."},
            // 6 Espejismo
            {"Acuerdo: {A} y {B} mantienen {P} ligero, resuelven lo práctico y no convierten lo casual en debate; {P} se acaba sin que nadie sufra.",
                    "Acuerdo: {A} propone una solución rápida para {P}, {B} acepta sin cavar más hondo, y los dos se van contentos sin saber muy bien por qué funcionó."},
            // 7 Superyó
            {"Acuerdo: {A} y {B} ponen reglas formales para {P} y cada uno se mantiene en su papel; trato de trámite, sin comentarios personales, y {P} se resuelve.",
                    "Acuerdo: {A} trata a {B} con respeto formal y {B} hace lo mismo; {P} se resuelve a distancia cortés, sin que nadie tenga que querer al otro."},
            // 8 Conflicto
            {"Acuerdo: {A} y {B} ponen una regla clara para {P} y dejan que un tercero decida, sin tocar el punto débil de nadie; contacto mínimo, {P} resuelto. La distancia funciona.",
                    "Acuerdo: {A} se muerde la lengua antes de criticar el {BP} de {B} y {B} hace lo mismo con el {AP} de {A}; {P} se resuelve por escrito, sin mirarse."},
            // 9 Cuasi-identidad
            {"Acuerdo: {A} y {B} nombran la diferencia en voz alta —suenan igual pero quieren cosas distintas— y reparten {P} desde ahí.",
                    "Acuerdo: {A} dice qué quiere de {P} con sus propias palabras, {B} hace lo mismo, y descubren que no estaban de acuerdo en nada, pero al menos ya lo saben."},
            // 10 Extinción
            {"Acuerdo: {A} y {B} resuelven {P} rápido y en corto, sin emociones: cinco minutos, tarea concreta, y cada uno por su lado.",
                    "Acuerdo: {A} y {B} le ponen un límite de tiempo a {P} y lo cumplen; contacto breve, sin discusiones largas, y {P} se acaba."},
            // 11 Negocios
            {"Acuerdo: {A} y {B} reparten {P} por competencia, cada uno hace lo suyo y nadie mete sentimientos; números, fechas y {P} resuelto.",
                    "Acuerdo: {A} hace lo que hace mejor en {P} y {B} lo que hace mejor él; sin química y sin conflicto, {P} queda hecho."},
            // 12 Benefactor
            {"Acuerdo: {A} ofrece ayuda concreta para {P} y {B} la acepta sin dar explicaciones; nadie pide gratitud y {P} se resuelve.",
                    "Acuerdo: {A} dice exactamente qué puede hacer por {P} y {B} elige; la ayuda llega en la medida justa y nadie se siente en deuda."},
            // 13 Beneficiario
            {"Acuerdo: {B} acepta lo concreto que sirve de {P} y agradece sin más; {A} no exige nada y {P} se resuelve.",
                    "Acuerdo: {B} dice qué parte de la ayuda le sirve para {P} y deja el resto; {A} lo entiende y {P} queda hecho."},
            // 14 Supervisor
            {"Acuerdo: {A} dice la crítica como sugerencia concreta para {P} y {B} pregunta qué haría {A} en su lugar; {P} se resuelve sin moralizar.",
                    "Acuerdo: {B} pide ejemplos concretos de lo que le molesta a {A} de {P} y no lo toma como ataque; {A} explica sin sentencias y {P} queda hecho."},
            // 15 Supervisado
            {"Acuerdo: {B} pide ejemplos concretos de lo que le molesta de {P} y {A} explica qué quiere sin moralizar; {B} responde con hechos y {P} se resuelve.",
                    "Acuerdo: {A} le explica a {B} qué quiere de {P} sin sentencias y {B} toma nota sin discutir; trato corto, {P} hecho."},
    };

    /**
     * Finales MALOS (v1.3.6, 2 por relación): humor negro absurdo y
     * exagerado, con giro irónico — un tipo se desata de forma
     * desproporcionada y termina dándole la razón al otro. Nacen de los
     * conflictos reales de la pareja y de sus funciones débiles
     * (PoLR, rol, sugestiva, Mobilizing). Sin contenido sexual y sin
     * insultos a grupos: la burla es solo entre los dos tipos, ficción.
     */
    private static final String[][] END_BAD = {
            // 0 Identidad
            {"Escalada: {A} y {B} se pelean por quién resuelve mejor {P}, cada uno copiando al otro al milímetro, hasta que los dos hacen exactamente lo mismo a la vez y se acusan mutuamente de plagio.",
                    "Escalada: {A} le explica a {B} cómo se hace {P} y {B} le explica lo mismo a {A}, palabra por palabra, durante una hora, hasta que los dos se van convencidos de que el otro no entendió nada."},
            // 1 Dualidad
            {"Escalada: {A} decide hacer {P} solo para demostrar que no necesita a nadie, se atasca en su {AS} y termina pidiéndole ayuda a {B} a gritos, que era justo lo que {B} le ofrecía desde el principio.",
                    "Escalada: {B} espera que {A} adivine lo que necesita para {P} y {A} espera que {B} lo pida en concreto; ninguno dice nada, {P} se pudre, y los dos se van convencidos de que el otro es un egoísta."},
            // 2 Activación
            {"Escalada: {A} y {B} se ponen a resolver {P} sin parar hasta terminar, se agotan a la vez, y terminan discutiendo por quién está más cansado mientras {P} sigue ahí, intacto.",
                    "Escalada: {A} le dice a {B} que si se cansa es porque no quiere de verdad; {B} se esfuerza hasta quedarse sin energía, se duerme encima de {P} y {A} lo toma como prueba de que tenía razón."},
            // 3 Espejo
            {"Escalada: {A} corrige el método de {B} en plena acción, {B} corrige el de {A} para vengarse, y {P} termina hecho de una forma que ninguno de los dos reconoce como suya.",
                    "Escalada: {A} y {B} discuten el cómo de {P} hasta que uno gane; gana {A}, aplica su método, sale mal, y {B} le recuerda el resto del día que él lo habría hecho con su método."},
            // 4 Afinidad
            {"Escalada: {A} y {B} comparan delante de todos quién hace mejor {P}; gana {B} por un pelo, {A} exige revancha, y la revancha dura tres días.",
                    "Escalada: {A} y {B} pelean por el mismo nicho de {P} hasta que los dos lo hacen a la vez, en el mismo sitio, estorbándose, y el resultado es peor que si lo hubiera hecho uno solo."},
            // 5 Semidualidad
            {"Escalada: {B} le exige a {A} que lo entienda en todo para {P}, {A} lo intenta, falla justo en la parte que no encaja, y {B} se va convencido de que {A} lo hace a propósito.",
                    "Escalada: {A} y {B} se entienden tan bien en la mitad de {P} que dan por hecho que se entienden en todo; la otra mitad explota y los dos se echan la culpa de haber confiado."},
            // 6 Espejismo
            {"Escalada: {A} convierte {P} en un debate serio y profundo hasta romperlo; {B} se queda mirando, sin entender cuándo una cosa casual se volvió una crisis existencial.",
                    "Escalada: {B} le pide a {A} para {P} justo lo que {A} no puede darle, {A} lo intenta por no quedar mal, y terminan los dos decepcionados de algo que nunca iba a funcionar."},
            // 7 Superyó
            {"Escalada: {A} le suelta a {B} un comentario personal sobre {P} delante de todos; {B} le devuelve el juicio con intereses y terminan juzgándose la vida entera por una tontería.",
                    "Escalada: {A} intenta acercarse a {B} para resolver {P} con confianza; {B} lo toma como una invasión, se cierra, y {P} queda peor que antes de intentarlo."},
            // 8 Conflicto
            {"Escalada: {A} se desata y baila sobre {P} nomás para darle la razón a {B} de que es mala persona: {B} asiente con su {BB} en alto y dice «¿ves?», mientras {A} sigue bailando, furioso de tener razón en contra suya.",
                    "Escalada: {A} le critica a {B} justo su {BP} delante de todos; {B} le devuelve la crítica en su {AP} y terminan peleando por lo que menos dominan, que es justo donde más duele."},
            // 9 Cuasi-identidad
            {"Escalada: {A} asume que {B} está de acuerdo porque suena igual; {B} asume lo mismo de {A}; los dos trabajan en direcciones opuestas y se encuentran al final, sorprendidos.",
                    "Escalada: {B} copia a {A} para resolver {P} porque «es lo mismo»; no es lo mismo, sale todo al revés, y {A} le dice a {B} que lo copió mal."},
            // 10 Extinción
            {"Escalada: {A} y {B} se ponen a discutir {P} con emociones hasta agotarse; ninguno convence al otro, {P} sigue igual, y los dos terminan más aburridos que al empezar.",
                    "Escalada: {A} propone algo para {P} y {B} lo desarma sin querer; {B} propone otra cosa y {A} la desarma; al final no queda nada en pie, ni siquiera la propuesta."},
            // 11 Negocios
            {"Escalada: {A} mete sentimientos en {P} y le pregunta a {B} «¿no te importa nada?»; {B} le contesta con un presupuesto y terminan peleando por si el presupuesto tiene sentimientos.",
                    "Escalada: {B} le dice a {A} que no lo respeta por cómo lleva {P}; {A} le muestra el Excel de todo lo que hizo y {B} se va, ofendido, sin mirar el Excel."},
            // 12 Benefactor
            {"Escalada: {A} le exige a {B} que agradezca la ayuda para {P} como se debe; {B} agradece con un discurso de media hora y {A} se va convencido de que lo hizo por obligación.",
                    "Escalada: {B} le critica a {A} cómo lo ayuda en {P}; {A} deja de ayudar, {B} se hunde en {P}, y los dos se echan la culpa de lo que el otro no hizo."},
            // 13 Beneficiario
            {"Escalada: {B} rechaza la ayuda de {A} para {P} con desprecio; {A} se ofende, {B} se queda sin ayuda, y {P} se lo come vivo delante de los dos.",
                    "Escalada: {B} acepta la ayuda de {A} para {P} pero se siente tan en deuda que le devuelve el favor con algo que {A} no quería; ahora {A} se siente en deuda y el ciclo no termina nunca."},
            // 14 Supervisor
            {"Escalada: {A} le explica a {B} todo lo que hace mal en {P}, en público, con su {AB} en alto; {B} aguanta porque sabe que {A} tiene razón en su {BP}, y eso le duele más que la crítica.",
                    "Escalada: {A} le dice a {B} que si no aguanta la crítica es porque es débil; {B} se traga la crítica, la procesa con su {BP} dolorosa y termina haciendo {P} peor, solo para demostrar que la crítica no servía."},
            // 15 Supervisado
            {"Escalada: {B} le devuelve la crítica a {A} con la misma fuerza; {A} ni se inmuta —para él era obvio— y {B} se queda gritándole a alguien que ya se fue a hacer {P}.",
                    "Escalada: {B} se pone a la defensiva con {P} y le pregunta a {A} «¿por qué siempre me equivoco?»; {A} le contesta con una lista, {B} llora, y la lista sigue creciendo."},
    };

    /**
     * Relaciones más confundibles con cada una (v1.3.6, 3 códigos por
     * relación; guiadas por los predicados verificados: dual~semidual/
     * activación, conflicto~superyó/cuasi-identidad, benefactor~
     * beneficiario, supervisor~supervisado, espejo~cuasi-identidad/
     * afinidad...). La correcta nunca está en su propio pool. Para la
     * pregunta de teoría con dificultad >=1.
     */
    private static final int[][] REL_CONFUSABLE = {
            {9, 3, 4}, // 0 Identidad ~ cuasi-identidad, espejo, afinidad
            {5, 2, 6}, // 1 Dualidad ~ semidualidad, activación, espejismo
            {1, 5, 6}, // 2 Activación ~ dualidad, semidualidad, espejismo
            {9, 4, 0}, // 3 Espejo ~ cuasi-identidad, afinidad, identidad
            {3, 0, 9}, // 4 Afinidad ~ espejo, identidad, cuasi-identidad
            {1, 2, 6}, // 5 Semidualidad ~ dualidad, activación, espejismo
            {5, 2, 1}, // 6 Espejismo ~ semidualidad, activación, dualidad
            {8, 9, 11}, // 7 Superyó ~ conflicto, cuasi-identidad, negocios
            {7, 9, 10}, // 8 Conflicto ~ superyó, cuasi-identidad, extinción
            {0, 3, 4}, // 9 Cuasi-identidad ~ identidad, espejo, afinidad
            {8, 7, 11}, // 10 Extinción ~ conflicto, superyó, negocios
            {7, 10, 8}, // 11 Negocios ~ superyó, extinción, conflicto
            {13, 14, 2}, // 12 Benefactor ~ beneficiario, supervisor, activación
            {12, 15, 2}, // 13 Beneficiario ~ benefactor, supervisado, activación
            {15, 12, 8}, // 14 Supervisor ~ supervisado, benefactor, conflicto
            {14, 13, 8}, // 15 Supervisado ~ supervisor, beneficiario, conflicto
    };

    /** Nombres de las 3 relaciones más confundibles con `rel`. */
    static String[] confusableNames(int rel) {
        String[] out = new String[3];
        for (int i = 0; i < 3; i++) out[i] = GamesData.REL_NAMES[REL_CONFUSABLE[rel][i]];
        return out;
    }

    /**
     * Casi-correctas / near-miss (v1.3.6, 2 por relación): mediaciones que
     * parecen correctas pero fallan en un detalle fino (la acción correcta
     * dirigida a la persona equivocada, o correcta en general pero sin el
     * dato clave). Su efecto es como el neutral (no es «buena») y la
     * reacción explica EL DETALLE FINO en que falla.
     */
    private static final String[][] NEAR_MISS = {
            {"Pedirle a {A} que haga la parte que le toca a {B} y a {B} la que le toca a {A}, para que cada uno vea {P} desde el otro lado.",
                    "Decirles que como los dos ven lo mismo, da igual quién resuelva {P}: que lo haga el que tenga tiempo."},
            {"Pedirle a {A} que le explique a {B} con todo detalle qué necesita, paso a paso, para que no haya malentendidos.",
                    "Proponer que {B} haga la parte fuerte de {A} en {P} para que aprenda a valerse solo."},
            {"Poner a {A} y a {B} a hacer {P} en una sola sesión larga, sin descansos, para aprovechar que están animados.",
                    "Pedirle a {A} y a {B} que se turnen cada cinco minutos para que nadie se canse."},
            {"Dejar que {A} y {B} usen cada uno su método para {P} sin acordar antes el objetivo.",
                    "Pedirle a {A} que copie el método de {B} para {P} porque «es el mismo, solo que al revés»."},
            {"Poner a {A} y a {B} a hacer {P} juntos, en el mismo sitio, porque les gusta lo mismo.",
                    "Pedirle a {A} que le enseñe a {B} su forma de hacer {P}, ya que los dos lo hacen parecido."},
            {"Decirle a {A} y a {B} que si se entienden en la mitad de {P}, con esfuerzo se entenderán en todo.",
                    "Pedirle a {B} que le dé a {A} justo lo que un dual le daría en {P}."},
            {"Convertir {P} en una charla profunda entre {A} y {B} para que se entiendan de verdad.",
                    "Pedirle a {A} y a {B} que se esfuercen más en {P}: con más esfuerzo sí va a funcionar."},
            {"Pedirle a {A} y a {B} que se acerquen y confíen para resolver {P} como amigos.",
                    "Pedirle a {A} que le haga un comentario personal a {B} para romper el hielo en {P}."},
            {"Pedirle a {A} que le explique a {B} con calma por qué su forma de hacer {P} está mal.",
                    "Poner a {A} y a {B} a hablar de {P} cara a cara, sin reglas, para que se digan las cosas claras."},
            {"Asumir que {A} y {B} están de acuerdo en {P} porque suenan igual y repartir el trabajo desde ahí.",
                    "Pedirle a {B} que copie a {A} porque «total, es lo mismo»."},
            {"Poner a {A} y a {B} a discutir {P} con emociones para que se desahoguen.",
                    "Pedirle a {A} que convenza a {B} de su propuesta para {P} con más argumentos."},
            {"Pedirle a {A} y a {B} que hablen de cómo se sienten con {P} para mejorar la relación.",
                    "Pedirle a {A} que le dé a {B} apoyo emocional con {P}, ya que trabajan juntos."},
            {"Pedirle a {B} que le devuelva a {A} la ayuda para {P} con algo equivalente.",
                    "Pedirle a {A} que le explique a {B} por qué su ayuda para {P} es tan valiosa."},
            {"Decirle a {B} que rechace la ayuda de {A} para {P} si no la pidió.",
                    "Pedirle a {B} que le diga a {A} exactamente cómo tiene que ayudarlo en {P}."},
            {"Dejar que {A} le explique a {B} todo lo que hace mal en {P}, en público, para que aprenda.",
                    "Decirle a {B} que si no aguanta la crítica de {A} sobre {P} es porque es débil."},
            {"Decirle a {B} que devuelva la crítica a {A} con la misma fuerza para que se respete.",
                    "Pedirle a {A} que deje de corregir a {B} en {P} para que {B} no se sienta vigilado."},
    };

    /** Por qué falla cada near-miss: el detalle fino (reacción de la opción). */
    private static final String[][] NEAR_MISS_WHY = {
            {"Casi: repartir está bien, pero al revés no: cada uno tiene que hacer SU parte, no la del otro. Intercambiar los papeles no es repartir.",
                    "Casi: ven lo mismo, sí, pero ninguno de los dos cubre lo que al otro le falta. Que lo haga «el que tenga tiempo» deja el hueco sin cubrir."},
            {"Casi: en dualidad no hace falta explicarlo todo: lo que uno necesita, el otro lo da sin que se lo pidan. Exigir la explicación convierte la ayuda en un trámite.",
                    "Casi: la parte fuerte de {A} es justo la débil de {B}. Pedirle que la haga solo es pedirle que falle en su punto débil."},
            {"Casi: en activación la energía dura poco. Sin descansos se agotan a la vez y terminan de mal humor.",
                    "Casi: los turnos tan cortos rompen la ráfaga. En activación hacen falta bloques de veinte minutos con un descanso entre medias, no microturnos."},
            {"Casi: en espejo los métodos son distintos aunque el objetivo sea el mismo. Sin acordar el objetivo primero, cada uno corrige el método del otro.",
                    "Casi: el Ego está al revés, no es el mismo. Copiar el método del otro es usar su segunda función como si fuera la primera."},
            {"Casi: les gusta lo mismo y compiten por el mismo nicho. Juntos en el mismo sitio se estorban; cada uno necesita su terreno.",
                    "Casi: se parecen en lo fuerte, pero nadie cubre lo que falta. Enseñarle tu forma no le da lo que no tiene."},
            {"Casi: la parte que no encaja no se arregla con esfuerzo. En semidualidad hay un hueco que no se cubre por más que se intente.",
                    "Casi: {B} no es el dual de {A}: le da una parte, no todo. Esperar de {B} lo que solo un dual da es pedirle lo imposible."},
            {"Casi: en espejismo hay que mantenerlo ligero. Cavar hondo en una cosa casual la rompe.",
                    "Casi: el problema no es el esfuerzo, es que sus funciones no se complementan. Más esfuerzo en lo mismo solo cansa más."},
            {"Casi: en superyó la distancia es cortés por algo. Forzar la cercanía se siente como una invasión, no como confianza.",
                    "Casi: los comentarios personales son justo lo que empeora el superyó. Lo que funciona es el trato formal, sin intimidad."},
            {"Casi: la base de {A} es la dolorosa de {B}. Explicarle «con calma» lo que hace mal sigue pisándole el punto débil, solo que con buenos modales.",
                    "Casi: sin reglas ni un tercero, cada uno pisa el punto débil del otro sin querer. En conflicto hace falta contacto mínimo y reglas concretas."},
            {"Casi: suenan igual pero procesan distinto. Hay que nombrar la diferencia en voz alta antes de repartir nada.",
                    "Casi: no es lo mismo: por dentro procesan distinto. Copiarlo es responder con algo parecido que no es lo mismo."},
            {"Casi: en extinción las discusiones largas y emocionales los agotan. Lo que funciona es contacto breve y centrado en la tarea.",
                    "Casi: sus funciones se anulan mutuamente. Más argumentos no avanzan nada: cada uno desarma lo del otro sin querer."},
            {"Casi: en negocios no hay que meter sentimientos. Hablar de cómo se sienten rompe justo lo que funciona: lo práctico.",
                    "Casi: ninguno de los dos le da al otro lo que necesita por dentro, y ninguno lo espera. El apoyo que funciona aquí es práctico, no emocional."},
            {"Casi: {B} no puede devolver lo mismo: lo que {B} ofrece, {A} no lo necesita. Exigir reciprocidad rompe el beneficio.",
                    "Casi: la ayuda del benefactor funciona porque llega sin que el otro note de dónde viene. Explicarla la convierte en una deuda."},
            {"Casi: rechazarla con desprecio es lo que empeora el beneficio. Lo que funciona es aceptar lo concreto que sirve y agradecer sin más.",
                    "Casi: la ayuda llega en la medida correcta cuando {A} la ofrece en concreto. Criticar cómo te ayudan es justo lo que rompe la relación."},
            {"Casi: la base de {A} es la dolorosa de {B}. Criticar en público es lo que más duele; la crítica tiene que ser sugerencia concreta, no sentencia.",
                    "Casi: {B} acepta la corrección porque la ve correcta, pero le duele. Decirle que es débil por sentirlo solo añade insulto a la herida."},
            {"Casi: devolver la crítica es lo que empeora la supervisión. Lo que funciona es pedir ejemplos concretos y no tomarlo como ataque personal.",
                    "Casi: {A} corrige sin darse cuenta: su fuerte es el punto débil de {B}. Pedirle que no corrija no quita la fricción; lo que funciona es pedir ejemplos concretos."},
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

    /** Nivel global de dificultad (v1.3.6): en tests sin Android, Store puede no estar inicializado. */
    private static int diff() {
        try { return Store.I == null ? 0 : Store.I.difficulty; } catch (Exception e) { return 0; }
    }

    private static String fill(String s, int t1, int t2, Prob p) {
        int[] m1 = GamesData.MODELS[t1], m2 = GamesData.MODELS[t2];
        // Placeholders largos primero: {AB} contiene "{A" y se rompería si {A} se reemplaza antes.
        return s.replace("{AB}", GamesData.ELEMENTS[m1[0]]).replace("{BB}", GamesData.ELEMENTS[m2[0]])
                .replace("{AC}", GamesData.ELEMENTS[m1[1]]).replace("{BC}", GamesData.ELEMENTS[m2[1]])
                .replace("{AP}", GamesData.ELEMENTS[m1[3]]).replace("{BP}", GamesData.ELEMENTS[m2[3]])
                .replace("{AR}", GamesData.ELEMENTS[m1[2]]).replace("{BR}", GamesData.ELEMENTS[m2[2]])
                .replace("{AS}", GamesData.ELEMENTS[m1[4]]).replace("{BS}", GamesData.ELEMENTS[m2[4]])
                .replace("{AM}", GamesData.ELEMENTS[m1[5]]).replace("{BM}", GamesData.ELEMENTS[m2[5]])
                .replace("{A}", GamesData.TYPES[t1]).replace("{B}", GamesData.TYPES[t2])
                .replace("{P}", p.title.toLowerCase(Locale.ROOT))
                .replace("{R}", GamesData.REL_NAMES[GamesData.REL[t1][t2]]);
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
     * neutral y error clásico, en orden barajado. v1.3.6: finales
     * aleatorios con sentido sociónico (END_GOOD/END_BAD por relación),
     * y con dificultad >=2 la neutral es un near-miss; con dificultad 3
     * también el error es un near-miss.
     */
    private static int lastGood = -1, lastBad = -1;

    static GamesData.Arena generate(int t1, int t2) {
        int rel = GamesData.REL[t1][t2];
        Random r = new Random();
        int d = diff();
        int pi = r.nextInt(PROBS.length);
        Prob p = PROBS[pi];
        GamesData.Arena s = new GamesData.Arena();
        s.id = "g" + t1 + "-" + t2 + "-" + pi;
        s.title = p.title;
        s.intro = p.context + " " + REL_INFO[rel][0];
        s.relation = GamesData.relName(t1, t2);
        s.t1 = t1; s.t2 = t2;
        for (int turn = 0; turn < 3; turn++) {
            GamesData.ArenaTurn t = new GamesData.ArenaTurn();
            t.situation = situation(rel, turn, t1, t2, p);
            ArrayList<GamesData.ArenaOpt> opts = new ArrayList<>();
            opts.add(opt(fill(CORRECT[rel][r.nextInt(2)], t1, t2, p),
                    "Funciona: " + REL_INFO[rel][1], -20, 20, 15));
            if (d >= 2) {
                GamesData.ArenaOpt nm = opt(fill(NEAR_MISS[rel][0], t1, t2, p),
                        fill(NEAR_MISS_WHY[rel][0], t1, t2, p), 5, 5, 5);
                nm.near = true;
                opts.add(nm);
            } else {
                opts.add(opt(fill(NEUTRAL[r.nextInt(NEUTRAL.length)], t1, t2, p),
                        "Se calman un poco, pero nadie cede.", 5, 5, 5));
            }
            if (d >= 3) {
                GamesData.ArenaOpt nm = opt(fill(NEAR_MISS[rel][1], t1, t2, p),
                        fill(NEAR_MISS_WHY[rel][1], t1, t2, p), 5, 5, 5);
                nm.near = true;
                opts.add(nm);
            } else {
                opts.add(opt(fill(MISTAKE[rel][r.nextInt(2)], t1, t2, p),
                        "La tensión sube: " + REL_INFO[rel][2], 25, -10, -10));
            }
            Collections.shuffle(opts, r);
            for (int k = 0; k < 3; k++) t.opts[k] = opts.get(k);
            s.turns[turn] = t;
        }
        // Final aleatorio por combate: se evita repetir el mismo índice del
        // combate anterior (con 2 por relación, el azar simple basta).
        int gi = r.nextInt(END_GOOD[rel].length);
        if (END_GOOD[rel].length > 1 && gi == lastGood) gi = (gi + 1) % END_GOOD[rel].length;
        lastGood = gi;
        int bi = r.nextInt(END_BAD[rel].length);
        if (END_BAD[rel].length > 1 && bi == lastBad) bi = (bi + 1) % END_BAD[rel].length;
        lastBad = bi;
        s.endGood = fill(END_GOOD[rel][gi], t1, t2, p);
        s.endBad = fill(END_BAD[rel][bi], t1, t2, p);
        return s;
    }
}
