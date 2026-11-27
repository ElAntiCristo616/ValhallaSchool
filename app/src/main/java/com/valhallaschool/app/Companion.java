package com.valhallaschool.app;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.view.Gravity;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;

import static com.valhallaschool.app.Ui.*;

/**
 * Acompañante inmersivo de los ejercicios: un personaje de Binky GRANDE
 * (~210dp) con bocadillo, protagonista de la pantalla, no un icono en
 * una esquina. Cambia en cada ejercicio (baraja de los 48 avatares, sin
 * repetir hasta agotarla) y se mueve SIEMPRE: rebote suave en bucle,
 * saludo al aparecer, celebración al acertar y decepción al fallar.
 *
 * DOS TONOS (orden de Remy 2026-11-27, v1.3.2):
 * - GANAR / ACERTAR: sarcasmo y desprestigio, pero SIN INSULTOS: una línea
 *   de indiferencia sobre si aciertas («Ah, una bien. Tampoco te emociones»).
 * - FALLAR: el personaje SE ENOJA: expresión de disgusto extremo (emoji
 *   grande 🤢😤 + marco rojo) y un regaño que se mete contigo y con tu
 *   persona: tu papá, tu mamá, tu perro o gato, tu ex, tu inteligencia y
 *   tu físico. El sonido también cambia: acierto agradable, fallo de
 *   disgusto/enojo (Sfx.YAY / Sfx.ANGRY, centralizado aquí).
 * Nunca contra grupos protegidos (nada de raza, género, religión,
 * discapacidad ni orientación): la humillación es solo contra el jugador,
 * que lo pidió así. TODAS las frases usan el nombre actual (nickname o
 * «Sin nombre»).
 */
final class Companion {
    /** Ánimo ANTES de responder: sarcasmo indiferente, SIN insultos (orden de Remy 2026-11-27). */
    static final String[] CHEER = {
            "Vamos, {name}, a ver qué sale esta vez.",
            "Respira, {name}… que pensar no es tu fuerte, pero inténtalo.",
            "Esta es tuya, {name}. O eso dicen los que no te conocen.",
            "Dale, {name}. ¿Qué función será? Apuesto a que la fallas.",
            "Tú ya sabes esto, {name}… o eso crees tú.",
            "Vamos por otra, {name}. A ver cuánto aguantas sin llorar.",
            "¿Listo, {name}? A darle. Total, fallar es gratis.",
            "Tú puedes con esto, {name}. Bueno, eso dicen.",
            "Piensa como tipador, {name}… si es que puedes pensar.",
            "Otra más, {name}. A ver si esta no la arruinas como las demás.",
            "Ánimo, {name}. Yo te acompaño… alguien tiene que reírse.",
            "Mira bien la frase, {name}… aunque ni mirando la vas a sacar.",
            "Tú sí sabes, {name}. ¿O no? Ya ni sé.",
            "Vamos, {name}. Que nadie te pare. Bueno, tú mismo te paras solo.",
            "¿Otra ronda, {name}? Claro que sí. Total, fallar es gratis y tú eres experto.",
            "Tú puedes, {name}. Tú sí sabes… ¿o no? Ay, qué triste.",

    };

    /** Al ACERTAR: sarcasmo e indiferencia, SIN insultos (orden de Remy 2026-11-27). */
    static final String[] CELEBRATE = {
            "Ah, una bien, {name}. Tampoco te emociones.",
            "Vaya, hasta tú aciertas a veces, {name}.",
            "Mira, {name}, una correcta. No te acostumbres.",
            "Bien, {name}. Siguiente, que esto no se va a hacer solo.",
            "Una bien, {name}. Qué sorpresa, la verdad.",
            "Acertaste, {name}. No sé cómo, pero acertaste.",
            "Mira nada más, {name}, hasta acertaste. Qué milagro.",
            "Sí, {name}, esa era. No te emociones, que fue una.",
            "Genial, {name}. ¿Seguro que no fue suerte? Yo digo que sí.",
            "Increíble, {name}. Hasta tú la sacaste. Ni tú te la crees.",
            "Bravo, {name}. Una bien de cada cien, qué promedio.",
            "Imparable, {name}. Bueno, por esta vez.",

    };

    /** Al FALLAR: enojo con disgusto extremo y regaño personal (papá, mamá, ex, perro/gato,
     *  inteligencia, físico). Solo contra el jugador, nunca contra grupos protegidos. */
    static final String[] SCOLD = {
            "Por eso tu ex te dejó, {name}.",
            "Tu mamá te ve fallando y ni ella te defiende, {name}.",
            "Hasta tu perro te mira con vergüenza, {name}.",
            "Eres un gordo, {name}. Agarra la pala y haz algo útil.",
            "Tu papá trabajó tanto para esto, {name}, y tú ni una aciertas.",
            "Con esa inteligencia ni para adivinar, {name}.",
            "Tu ex tenía razón, {name}: eres un desastre.",
            "Ni tu gato te quiere, {name}, y eso que los gatos quieren a cualquiera.",
            "Tu mamá te parió y ni ella te reconoce, {name}.",
            "Qué vergüenza, {name}. Hasta tu moral da pena.",
            "Patético, {name}. Ni con suerte.",
            "Hasta mi abuela lo sabía, {name}, y eso que es un dibujo.",
            "Eres un desastre, {name}. Un fracasado con patas.",
            "Qué oso, {name}. Ni para eso sirves.",
            "Terrible, {name}. Vuélvelo a intentar, a ver si ahora sí.",
            "Ay, no, {name}. ¿Qué fue eso? Qué horror.",
            "Pésimo, {name}. Pésimo. Tu cerebro tiene menos neuronas que una tostadora.",
            "Vergüenza ajena, {name}. Hasta yo me sonrojé.",
            "Ese error me dolió hasta a mí, {name}, y eso que soy un dibujo.",
            "Ni adivinando le atinas, {name}.",
            "Qué barbaridad, {name}. Con esa cara ni el espejo te aguanta.",
            "Me das pena, {name}… y eso que soy un dibujo.",
            "Eso estuvo feo, {name}. Muy feo. Feo como tú.",
            "Qué mierda de respuesta, {name}. Qué asco.",
            "Joder, {name}. Concéntrate, si es que puedes.",
            "De pena ajena, {name}. Hasta el teléfono se avergonzó de ti.",
            "Horrible, {name}. Ni el azar te quiere.",
            "Eres un inútil, {name}. Un cuck de manual.",
            "Qué asco de respuesta, {name}. Da vergüenza verte.",
            "Das pena ajena, {name}. Hasta tu moral da pena.",
            "Eres la vergüenza de Valhalla, {name}. Y de tu familia también.",
            "Ni tu mamá te salva, {name}. Ni ella te aguanta.",
            "Vuelve a la primaria, {name}. Que ni ahí pasabas.",
            "Basura, {name}. Basura con nombre.",
            "Hasta un niño lo sabía y tú no, {name}. Qué triste.",
            "Qué forma de fallar, {name}, da lástima verte.",
            "Esto no es para ti, {name}. Acéptalo.",
            "Dedícate a otra cosa, {name}, la sociónica no es lo tuyo.",
            "Ríndete, {name}, nunca vas a entender esto.",
            "Tu familia se avergüenza de ti, {name}. Y con razón.",
            "Tu ex te dejó y tu perro ni te pela, {name}. Piensa en eso.",
            "Tu mamá te dice que eres especial, {name}, y es mentira.",

    };

    /** Colas cortas para los títulos de retroalimentación (fbTitle) de los juegos:
     *  ganar = sarcasmo indiferente sin insultos; fallar = insulto personal. */
    static final String[] WIN_TAILS = {
            "Tampoco te emociones.",
            "No te acostumbres.",
            "Qué sorpresa.",
            "Una bien, nada más.",
            "Vaya, hasta tú.",
            "No sé cómo, pero bien.",
            "Qué milagro.",
            "No te emociones, que fue una.",
            "¿Seguro que no fue suerte?",
            "Ni tú te la crees.",
            "Por esta vez.",
            "A ver si se repite.",
    };

    static final String[] FAIL_TAILS = {
            "Por eso tu ex te dejó.",
            "Tu mamá te ve fallando y ni ella te defiende.",
            "Hasta tu perro te mira con vergüenza.",
            "Eres un gordo, agarra la pala.",
            "Tu papá trabajó tanto para esto.",
            "Con esa inteligencia ni para adivinar.",
            "Tu ex tenía razón.",
            "Ni tu gato te quiere.",
            "Qué vergüenza.",
            "Patético.",
            "Ni adivinando le atinas.",
            "Con esa cara ni el espejo te aguanta.",
            "Me das pena.",
            "Qué asco de respuesta.",
            "Eres la vergüenza de tu familia.",
            "Ni tu mamá te salva.",
            "Vuelve a la primaria.",
            "Basura con nombre.",
            "Hasta un niño lo sabía y tú no.",
            "Da lástima verte.",
            "Tu ex te dejó y tu perro ni te pela.",
            "Tu mamá te dice que eres especial y es mentira.",
            "Con ese físico ni el gimnasio te quiere.",
            "Tu familia se avergüenza de ti.",
    };

    /** Frase completa de ganar (sarcasmo sin insultos) / de fallar (regaño personal), con el nombre. */
    static String winLine() {
        return CELEBRATE[Quiz.R.nextInt(CELEBRATE.length)].replace("{name}", Store.I.displayName());
    }
    static String failLine() {
        return SCOLD[Quiz.R.nextInt(SCOLD.length)].replace("{name}", Store.I.displayName());
    }
    /** Cola corta para fbTitle (el título ya lleva el nombre). */
    static String winTail() { return WIN_TAILS[Quiz.R.nextInt(WIN_TAILS.length)]; }
    static String failTail() { return FAIL_TAILS[Quiz.R.nextInt(FAIL_TAILS.length)]; }

    /** Baraja de los 48 (en memoria, por sesión): sin repetir hasta agotarla. */
    private static final ArrayList<String> DECK = new ArrayList<>();

    private static String nextDeckFile() {
        if (DECK.isEmpty()) {
            for (String f : Avatars.FILES) DECK.add(f);
            Collections.shuffle(DECK, Quiz.R);
        }
        return DECK.remove(0);
    }

    private final MainActivity a;
    private final LinearLayout root;
    private final TextView bubble;
    private final TextView disgustTx;
    private final ImageView img;
    private AnimatorSet idle, reaction;
    private Runnable idleStarter;
    private int lastCheer = -1, lastCeleb = -1, lastScold = -1;

    Companion(MainActivity a) {
        this.a = a;
        root = col(a);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        // v1.3.2 — indicador de disgusto extremo al fallar: emoji grande de asco/enojo
        // junto al bocadillo. Oculto hasta que fallas.
        disgustTx = tv(a, "🤢", 56, TEXT, REG);
        disgustTx.setGravity(Gravity.CENTER);
        disgustTx.setVisibility(View.GONE);
        root.addView(disgustTx, lpm(W, W, 0, 0, 0, 0));
        bubble = tv(a, "", 15, TEXT, BLACK);
        bubble.setGravity(Gravity.CENTER);
        bubble.setMaxWidth(dp(300));
        bubble.setPadding(dp(14), dp(10), dp(14), dp(10));
        bubble.setBackground(round(SURF, 18, ACCENT, 2));
        root.addView(bubble, lpm(W, W, 0, 0, 0, 2));
        img = new ImageView(a);
        img.setAdjustViewBounds(true);
        img.setScaleType(ImageView.ScaleType.FIT_CENTER);
        img.setLayoutParams(lp(dp(210), dp(210)));
        root.addView(img, lpm(W, W, 0, 0, 0, 0));
        root.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
            @Override public void onViewAttachedToWindow(View v) {}
            @Override public void onViewDetachedFromWindow(View v) { stopAll(); }
        });
    }

    LinearLayout root() { return root; }

    /** Nuevo ejercicio: otro personaje (baraja) + frase de ánimo. Saluda al aparecer. */
    void next() {
        stopAll();
        disgustTx.setVisibility(View.GONE);
        img.setBackground(null);
        bubble.setBackground(round(SURF, 18, ACCENT, 2));
        bubble.setText(cheer());
        android.graphics.Bitmap b = Avatars.load(a, nextDeckFile(), dp(420));
        if (b != null) img.setImageBitmap(b);
        img.setAlpha(1f);
        img.setScaleX(1f); img.setScaleY(1f);
        img.setTranslationX(0f); img.setTranslationY(0f); img.setRotation(0f);
        // Saludo: aparece con un brinquito y un meneo.
        ObjectAnimator pop = ObjectAnimator.ofFloat(img, "scaleX", 0.7f, 1.05f, 1f);
        ObjectAnimator popY = ObjectAnimator.ofFloat(img, "scaleY", 0.7f, 1.05f, 1f);
        ObjectAnimator wave = ObjectAnimator.ofFloat(img, "rotation", 0f, -8f, 8f, 0f);
        AnimatorSet greet = new AnimatorSet();
        greet.playTogether(pop, popY, wave);
        greet.setDuration(650);
        greet.setInterpolator(new OvershootInterpolator());
        greet.start();
        scheduleIdle(700);
    }

    /** Reacción súper expresiva al responder + SONIDO (centralizado aquí, v1.3.2).
     *  Al acertar: celebración + confeti + sonido agradable (YAY).
     *  Al fallar: decepción + emoji de disgusto + marco rojo + sonido de enojo (ANGRY). */
    void react(boolean ok) {
        stopAll();
        Sfx.play(ok ? Sfx.YAY : Sfx.ANGRY);
        if (ok) {
            disgustTx.setVisibility(View.GONE);
            img.setBackground(null);
            bubble.setBackground(round(mix(GREEN, SURF, 0.20f), 18, GREEN, 2));
            bubble.setText(celebrate());
            ObjectAnimator jump = ObjectAnimator.ofFloat(img, "translationY", 0f, -dp(48), 0f);
            ObjectAnimator big = ObjectAnimator.ofFloat(img, "scaleX", 1f, 1.18f, 1f);
            ObjectAnimator bigY = ObjectAnimator.ofFloat(img, "scaleY", 1f, 1.18f, 1f);
            ObjectAnimator spin = ObjectAnimator.ofFloat(img, "rotation", 0f, -8f, 8f, 0f);
            reaction = new AnimatorSet();
            reaction.playTogether(jump, big, bigY, spin);
            reaction.setDuration(900);
            reaction.setInterpolator(new OvershootInterpolator());
            reaction.start();
            a.confetti();
        } else {
            disgustTx.setText(Quiz.R.nextBoolean() ? "🤢" : "😤");
            disgustTx.setVisibility(View.VISIBLE);
            img.setBackground(round(mix(RED, SURF, 0.30f), 24, RED, 4));
            bubble.setBackground(round(mix(RED, SURF, 0.25f), 18, RED, 2));
            bubble.setText(scold());
            float x = dp(12);
            ObjectAnimator shake = ObjectAnimator.ofFloat(img, "translationX", 0f, -x, x, -x * 0.7f, x * 0.7f, 0f);
            ObjectAnimator small = ObjectAnimator.ofFloat(img, "scaleX", 1f, 0.9f, 1f);
            ObjectAnimator smallY = ObjectAnimator.ofFloat(img, "scaleY", 1f, 0.9f, 1f);
            ObjectAnimator droop = ObjectAnimator.ofFloat(img, "rotation", 0f, -5f, 5f, 0f);
            ObjectAnimator dim = ObjectAnimator.ofFloat(img, "alpha", 1f, 0.6f, 1f);
            reaction = new AnimatorSet();
            reaction.playTogether(shake, small, smallY, droop, dim);
            reaction.setDuration(850);
            reaction.start();
        }
        scheduleIdle(1000);
    }

    private void scheduleIdle(long delayMs) {
        if (idleStarter != null) img.removeCallbacks(idleStarter);
        idleStarter = this::startIdle;
        img.postDelayed(idleStarter, delayMs);
    }

    /** Rebote suave en bucle + meneo leve: el personaje nunca está quieto. */
    private void startIdle() {
        if (root.getWindowToken() == null) return;
        stopIdle();
        ObjectAnimator bounce = ObjectAnimator.ofFloat(img, "translationY", 0f, -dp(12), 0f);
        bounce.setDuration(1600);
        bounce.setRepeatCount(ObjectAnimator.INFINITE);
        bounce.setRepeatMode(ObjectAnimator.REVERSE);
        ObjectAnimator wobble = ObjectAnimator.ofFloat(img, "rotation", -2f, 2f);
        wobble.setDuration(1600);
        wobble.setRepeatCount(ObjectAnimator.INFINITE);
        wobble.setRepeatMode(ObjectAnimator.REVERSE);
        idle = new AnimatorSet();
        idle.playTogether(bounce, wobble);
        idle.setInterpolator(new AccelerateDecelerateInterpolator());
        idle.start();
    }

    private void stopIdle() {
        if (idle != null) { idle.cancel(); idle = null; }
    }

    private void stopAll() {
        stopIdle();
        if (reaction != null) { reaction.cancel(); reaction = null; }
        if (idleStarter != null) { img.removeCallbacks(idleStarter); idleStarter = null; }
    }

    private String name() { return Store.I.displayName(); }

    /** El acompañante SIEMPRE dice el nombre de usuario actual (nickname o «Sin nombre»). */
    private String cheer() {
        int i = Quiz.R.nextInt(CHEER.length); if (i == lastCheer) i = (i + 1) % CHEER.length; lastCheer = i;
        return CHEER[i].replace("{name}", name());
    }
    private String celebrate() {
        int i = Quiz.R.nextInt(CELEBRATE.length); if (i == lastCeleb) i = (i + 1) % CELEBRATE.length; lastCeleb = i;
        return CELEBRATE[i].replace("{name}", name());
    }
    private String scold() {
        int i = Quiz.R.nextInt(SCOLD.length); if (i == lastScold) i = (i + 1) % SCOLD.length; lastScold = i;
        return SCOLD[i].replace("{name}", name());
    }
}
