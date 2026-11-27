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

    /** Descripción completa de la relación (v1.3.5: ya no es una frase, es una
     *  explicación de la dinámica real: qué le hace cada uno al otro con sus
     *  funciones fuertes y débiles, qué funciona, qué choca, los malentendidos
     *  típicos, el día a día y cómo evoluciona). Sin fuentes ni menciones de
     *  origen: el texto va limpio. La cola específica de la pareja
     *  (pairTail) se calcula con los 16 modelos clásicos. */
    static String relDesc(int code, String a, String b) {
        String d;
        switch (code) {
            case 0: d = "Mismo tipo: los dos procesan el mundo con las mismas funciones en el mismo orden, así que se entienden sin hablar y casi nunca discuten de fondo. Cada uno ve en el otro su propio reflejo: lo que uno piensa, el otro ya lo pensó. Eso hace la convivencia fácil y sin sorpresas, pero también plana: nadie cubre lo que al otro le falta, porque les falta lo mismo. Con el tiempo pueden aburrirse el uno del otro, o acomodarse en una rutina cómoda donde nada cambia. Funciona bien como compañía tranquila; para crecer, necesitan a alguien de fuera que les traiga lo que ninguno de los dos tiene."; break;
            case 1: d = "Dualidad: la pareja ideal del Modelo A. La base de uno es justo la sugestiva del otro: lo que uno hace sin esfuerzo es exactamente lo que el otro necesita que le den, y al revés. Por eso el trato se siente natural desde el primer día: cada uno cubre el hueco del otro sin que nadie tenga que pedirlo ni explicarlo. No se corrigen ni se juzgan: el dual acepta la ayuda con gratitud porque llega en la medida justa. Los roces existen, pero se disuelven solos: lo que a uno le cuesta, al otro le sale solo. Con el tiempo la relación se hace cada vez más sólida y cómoda, y los dos terminan más seguros de sí mismos de lo que eran solos."; break;
            case 2: d = "Activación: se animan mutuamente y se contagian energía: cada uno ve en el otro algo que le gusta y lo activa. Comparten la misma forma de ver las cosas en lo importante, así que la conversación fluye y el trato es cálido. El problema es el ritmo: ninguno de los dos descansa de verdad en el otro, y los dos se cansan a la vez de lo mismo. En el día a día se llevan bien y se divierten, pero si pasan demasiado tiempo juntos terminan agotados sin saber por qué. Con distancia y pausas, la relación se mantiene viva y agradable; pegados todo el día, se desgasta."; break;
            case 3: d = "Espejo: mismos valores y mismas funciones fuertes, pero en distinto orden: cada uno pone primero lo que el otro pone segundo. Por eso se entienden a fondo y a la vez se corrigen sin parar: cada uno ve en el otro su propia forma de pensar, un poco torcida. Las conversaciones son largas y estimulantes, porque hablan el mismo idioma con distinto acento. El roce típico es que cada uno cree que su orden es el correcto y trata de convencer al otro. En el día a día es una relación cómoda y de confianza, que mejora con el tiempo si aprenden a no tomarse las correcciones como ataques."; break;
            case 4: d = "Afinidad: comparten las funciones fuertes, así que ven el mundo de forma parecida y se sienten cómodos juntos desde el principio. La conversación es fácil y no hay fricción: ninguno pisa el punto débil del otro. El límite es que se parecen demasiado en lo fuerte y en lo débil: nadie cubre lo que falta, y los dos evitan las mismas cosas. En el día a día es una compañía agradable y sin sobresaltos, buena para pasar el rato. Con el tiempo puede quedarse en eso, una amistad tranquila que no profundiza, a menos que busquen juntos lo que ninguno tiene."; break;
            case 5: d = "Semidualidad: casi duales, pero no del todo: se complementan en una parte y fallan en la otra. Al principio el trato se siente muy bien, como si hubieran encontrado a su dual, y hay atracción y confianza. Pero hay un hueco que se nota cada vez más: uno de los dos no recibe lo que esperaba del otro, y lo siente como una falta. El malentendido típico es esperar del otro lo que solo un dual da, y decepcionarse cuando no llega. En el día a día funciona si los dos aceptan ese hueco sin echárselo en cara. Con el tiempo, o se acomodan con lo que sí hay, o la falta pesa más que lo bueno."; break;
            case 6: d = "Espejismo: desde lejos se admiran y se atraen: cada uno ve en el otro algo que le gusta y que no tiene. De cerca, en cambio, no encajan: lo que uno necesita, el otro no se lo da, y lo que el otro ofrece, uno no lo pide. La convivencia se vuelve confusa: hay cariño, pero también una sensación constante de que algo no cuadra. El malentendido típico es creer que con más esfuerzo sí va a funcionar, cuando el problema no es el esfuerzo sino que sus funciones no se complementan. En el día a día es mejor de lejos, con encuentros cortos y agradables; viviendo juntos, el desencanto crece."; break;
            case 7: d = "Superyó: se juzgan mutuamente por lo que el otro no valora: cada uno ve en el otro justo lo que desprecia o lo que le parece una pérdida de tiempo. La relación es tensa desde el principio, aunque al principio pueda disfrazarse de interés. Cada uno siente que el otro lo mira por encima del hombro y lo corrige en lo que más le importa. En el día a día los roces son constantes y pequeños, y se acumulan: nadie se siente aceptado. Con el tiempo, si no hay distancia, el juicio mutuo se vuelve costumbre y la relación se enfría. Funciona solo con trato corto y temas neutros."; break;
            case 8: d = "Conflicto: la peor relación del Modelo A. Cada uno pisa con su función más fuerte justo el punto más débil del otro: lo que uno hace sin pensar es lo que al otro más le duele. Por eso los choques son constantes y parecen personales, aunque nadie lo haga a propósito. Cada uno siente que el otro lo ataca en lo que menos domina, y se defiende o se cierra. En el día a día, hasta las cosas simples se vuelven discusión: no hay tema seguro. Con el tiempo la relación no mejora sola: solo funciona con mucha distancia, trato corto y temas muy delimitados, y aun así cansa."; break;
            case 9: d = "Cuasi-identidad: se parecen muchísimo por fuera —mismos temas, mismos gustos, misma forma de hablar— y por eso al principio creen que se entienden del todo. Pero por dentro procesan distinto: lo que uno dice, el otro lo entiende a medias y responde con algo parecido que no es lo mismo. El malentendido típico es esa sensación de hablar y no llegar nunca al punto: se escuchan, se caen bien, pero no se encuentran. En el día a día es una relación amable y sin conflicto, pero superficial. Con el tiempo se queda en una buena compañía para pasar el rato, sin la profundidad que ambos esperaban al principio."; break;
            case 10: d = "Extinción: se anulan mutuamente. Cada uno, con su sola presencia, apaga lo que el otro quiere hacer: lo que uno propone, el otro lo desarma sin querer, y al revés. No hay mala intención: simplemente sus funciones fuertes chocan y ninguna de las dos avanza. El trato se siente pesado y estéril, como hablarle a una pared amable. En el día a día es mejor de lejos: juntos, los dos terminan cansados y sin haber logrado nada. Con el tiempo, la relación se enfría sola si se ven poco; si se ven mucho, el desgaste es mutuo."; break;
            case 11: d = "Negocios: cooperan bien sin intimidad. Cada uno sabe lo suyo y respeta lo del otro: se reparten el trabajo, cumplen y no se meten en la vida del otro. No hay química ni conflicto: es una relación práctica y ordenada. El límite es que no hay cercanía: ninguno de los dos le da al otro lo que necesita por dentro, y ninguno lo espera. En el día a día funciona muy bien para trabajar juntos, hacer trámites o compartir un proyecto. Con el tiempo se mantiene estable tal como es: no se profundiza, pero tampoco se rompe."; break;
            case 12: d = a + " es benefactor de " + b + ": le da justo lo que " + b + " necesita y no sabe pedir, y la ayuda llega en la medida correcta, así que " + b + " la recibe con gusto sin darse cuenta de dónde viene. " + b + ", en cambio, no puede devolverle lo mismo: lo que " + b + " ofrece, " + a + " no lo necesita, y " + a + " lo nota. Por eso la relación es asimétrica: " + a + " se siente útil y generoso, " + b + " se siente agradecido, pero " + a + " puede terminar sintiendo que da más de lo que recibe. En el día a día funciona bien si " + a + " no espera reciprocidad. Con el tiempo, si " + a + " se cansa de dar sin recibir, se aleja; " + b + " lo extraña sin entender bien por qué."; break;
            case 13: d = b + " es benefactor de " + a + ": le da justo lo que " + a + " necesita y no sabe pedir, y la ayuda llega en la medida correcta, así que " + a + " la recibe con gusto sin darse cuenta de dónde viene. " + a + ", en cambio, no puede devolverle lo mismo: lo que " + a + " ofrece, " + b + " no lo necesita, y " + b + " lo nota. Por eso la relación es asimétrica: " + b + " se siente útil y generoso, " + a + " se siente agradecido, pero " + b + " puede terminar sintiendo que da más de lo que recibe. En el día a día funciona bien si " + b + " no espera reciprocidad. Con el tiempo, si " + b + " se cansa de dar sin recibir, se aleja; " + a + " lo extraña sin entender bien por qué."; break;
            case 14: d = a + " supervisa a " + b + ": marca los errores de " + b + " sin proponérselo, porque con su función fuerte toca justo el punto débil de " + b + ", y " + b + " lo siente como una crítica aunque " + a + " no critique nada. " + b + " acepta la corrección porque la ve correcta, pero le duele: cada señalamiento le recuerda lo que no domina. " + a + ", por su parte, no entiende por qué " + b + " se toma tan en serio algo que para él es obvio. En el día a día, " + b + " termina pendiente de lo que " + a + " piensa y tratando de no fallar delante de él. Con el tiempo, " + b + " se cansa de sentirse vigilado y busca distancia; si hay afecto, la relación sobrevive con trato corto y temas seguros."; break;
            default: d = b + " supervisa a " + a + ": marca los errores de " + a + " sin proponérselo, porque con su función fuerte toca justo el punto débil de " + a + ", y " + a + " lo siente como una crítica aunque " + b + " no critique nada. " + a + " acepta la corrección porque la ve correcta, pero le duele: cada señalamiento le recuerda lo que no domina. " + b + ", por su parte, no entiende por qué " + a + " se toma tan en serio algo que para él es obvio. En el día a día, " + a + " termina pendiente de lo que " + b + " piensa y tratando de no fallar delante de él. Con el tiempo, " + a + " se cansa de sentirse vigilado y busca distancia; si hay afecto, la relación sobrevive con trato corto y temas seguros."; break;
        }
        return d + pairTail(a, b);
    }

    /** Cola específica de la pareja: qué función fuerte de uno se encuentra
     *  con qué posición del otro, calculada con los 16 modelos clásicos
     *  (p. ej. en dualidad la base de uno es la sugestiva del otro; en
     *  conflicto, la base de uno es la PoLR del otro). */
    private static String pairTail(String a, String b) {
        int ai = GamesData.typeIdx(a), bi = GamesData.typeIdx(b);
        int[] ma = GamesData.MODELS[ai], mb = GamesData.MODELS[bi];
        int posAb = 0, posBb = 0;
        for (int p = 0; p < 8; p++) {
            if (mb[p] == ma[0]) posAb = p;
            if (ma[p] == mb[0]) posBb = p;
        }
        return " En esta pareja, el " + GamesData.ELEMENTS[ma[0]] + " de " + a + " es "
                + (posAb == 2 ? "el " : "la ") + PracticeData.POSITIONS_DISPLAY[posAb] + " de " + b
                + ", y el " + GamesData.ELEMENTS[mb[0]] + " de " + b + " es "
                + (posBb == 2 ? "el " : "la ") + PracticeData.POSITIONS_DISPLAY[posBb] + " de " + a + ".";
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
        // v1.3.4: los 4 candidatos se barajan en cada encargo (en el banco el
        // correcto era el primero en 10 de 12 encargos); el tag sigue siendo
        // el índice de tipo, así `pick == c.cands[c.answer]` sigue bien.
        int[] order = Quiz.shuffled4();
        for (int k = 0; k < 4; k++) {
            int ci = c.cands[order[k]];
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
        // v1.3.5: el encargo muestra también la descripción completa de la
        // relación entre el contratado y el elegido, no solo la explicación corta.
        String fullRel = relDesc(code, GamesData.TYPES[c.hired], GamesData.TYPES[pick]);
        if (ok) {
            fbTitle.setText(winTitle("¡Encargo correcto, " + name() + "!"));
            fbText.setText(GamesData.TYPES[c.hired] + " → " + GamesData.TYPES[pick] + ": " + rel + ". " + c.explanation + "\n\n" + fullRel);
            setFoot(1);
        } else {
            fbTitle.setText(failTitle("¡Mal encargo, " + name() + "!"));
            fbText.setText(GamesData.TYPES[c.hired] + " → " + GamesData.TYPES[pick] + ": " + rel + ". " + c.explanation + "\n\n" + fullRel);
            hapticBad();
            setFoot(2);
        }
        phase = FEEDBACK;
        setBtn("Siguiente", B_GREEN);
        updateBar();
    }
}
