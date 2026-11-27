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
 * TODAS las frases humillan al jugador USANDO SU NOMBRE (nickname o «Sin
 * nombre»): ánimos, celebraciones y decepciones (orden de Remy 2026-11-27:
 * «hay que humillar al usuario»; la regla del documento «nunca humilla al
 * usuario» quedó ignorada por orden suya). Cero elogios sinceros, cero
 * sarcasmo suave. Nunca contra grupos protegidos (nada de raza, género,
 * religión, discapacidad ni orientación): la humillación es solo contra el
 * jugador, que lo pidió así.
 */
final class Companion {
    /** Ánimo: humillación por nombre ANTES de responder. Cero elogios sinceros, cero sarcasmo suave (orden de Remy 2026-11-27). */
    static final String[] CHEER = {
            "¡Vamos, {name}, a ver si no la cagas como siempre, fracasado!",
            "Respira, {name}… que con ese cerebro ni respirar te sale bien.",
            "¡Esta es tuya, {name}! O eso dicen los que no te conocen, cuck.",
            "¡Dale, {name}! ¿Qué función será? Apuesto a que la fallas, NPC.",
            "Tú ya sabes esto, {name}… o eso crees tú, beta.",
            "¡Vamos por otra, {name}! A ver cuánto aguantas sin llorar, inútil.",
            "¿Listo, {name}? ¡A darle! Tu moral ya da pena antes de empezar.",
            "¡Tú puedes con esto, {name}! Bueno, eso dicen. Yo no lo creo, fracasado.",
            "Piensa como tipador, {name}… si es que ese cerebro de NPC puede pensar.",
            "¡Otra más, {name}! A ver si esta no la arruinas como las demás, desastre.",
            "¡Ánimo, {name}! Yo te acompaño… alguien tiene que reírse de ti.",
            "Mira bien la frase, {name}… aunque ni mirando la vas a sacar, cuck.",
            "¡Tú sí sabes, {name}! ¿O no? Ya ni sé, qué asco.",
            "¡Vamos, {name}! ¡Que nadie te pare! Bueno, tú mismo te paras solo, beta.",
            "¿Otra ronda, {name}? ¡Claro que sí! Total, fallar es gratis y tú eres experto, inútil.",
            "¡Tú puedes, {name}! ¡Tú sí sabes!… ¿o no? Ay, qué triste, fracasado.",

    };

    /** Celebración HUMILLANTE al acertar: burla por nombre, nunca elogio sincero (orden de Remy 2026-11-27). */
    static final String[] CELEBRATE = {
            "¡Hasta tú la sabías, {name}! ¿Casualidad o copiaste, cuck?",
            "¡Una bien, {name}! No te acostumbres, que fue suerte, NPC.",
            "¡Wow, acertaste una, {name}! ¿Te dolió pensar, beta?",
            "¡Mira nada más, {name}, hasta acertaste! Qué milagro, fracasado.",
            "¡Qué suerte tienes hoy, {name}! Mañana se te acaba, inútil.",
            "¡Vaya, hasta tú puedes con esta, {name}! No me lo creo, qué asco.",
            "¡Wow, acertaste, {name}! ¿Casualidad o copiaste, eh?",
            "¡Imparable, {name}! Bueno, por esta vez. No te emociones, desastre.",
            "¡Genial, {name}! ¿Seguro que no fue suerte? Yo digo que sí, beta.",
            "¡Increíble, {name}! ¡Hasta tú la sacaste! Ni tú te la crees, cuck.",
            "¡Sí señor, {name}! ¡Esa era! Ni tú te la crees, ¿verdad, NPC?",
            "¡Bravo, {name}! Una bien de cada cien, qué promedio tan triste, fracasado.",

    };

    /** Decepción al fallar: humillación por nombre, contra el jugador, nunca contra grupos protegidos (orden de Remy 2026-11-27). */
    static final String[] SCOLD = {
            "¡Qué vergüenza, {name}! ¡Hasta tu moral da pena!",
            "¡Patético, {name}! ¡Ni con suerte, NPC!",
            "¡Hasta mi abuela lo sabía, {name}, y eso que es un dibujo, inútil!",
            "¡Eres un desastre, {name}! ¡Un fracasado con patas!",
            "¡Qué oso, {name}! ¡Ni para eso sirves, beta!",
            "¡Terrible, {name}! ¡Vuélvelo a intentar, a ver si ahora sí no la cagas, cuck!",
            "¡Ay, no, {name}! ¿Qué fue eso? ¡Qué horror, qué asco!",
            "¡Pésimo, {name}! ¡Pésimo! ¡Tu cerebro tiene menos neuronas que una tostadora!",
            "¡Vergüenza ajena, {name}! ¡Hasta yo me sonrojé, desastre!",
            "¡Ese error me dolió hasta a mí, {name}, y eso que soy un dibujo!",
            "¡Ni adivinando le atinas, {name}! ¡Eres un NPC con patas!",
            "¡Qué barbaridad, {name}! ¡Con esa cara ni el espejo te aguanta!",
            "¡Me das pena, {name}… y eso que soy un dibujo, fracasado!",
            "¡Eso estuvo feo, {name}! ¡Muy feo! ¡Feo como tú!",
            "¡Qué mierda de respuesta, {name}! ¡Qué asco!",
            "¡Joder, {name}! ¡Concéntrate, si es que puedes, inútil!",
            "¡De pena ajena, {name}! ¡Hasta el teléfono se avergonzó de ti!",
            "¡Horrible, {name}! ¡Ni el azar te quiere, beta!",
            "¡Eres un inútil, {name}! ¡Un cuck de manual!",
            "¡Qué asco de respuesta, {name}! ¡Da vergüenza verte!",
            "¡Das pena ajena, {name}! ¡Hasta tu moral da pena!",
            "¡Eres la vergüenza de Valhalla, {name}! ¡Y de tu familia también!",
            "¡Ni tu mamá te salva, {name}! ¡Ni ella te aguanta!",
            "¡Vuelve a la primaria, {name}! ¡Que ni ahí pasabas!",
            "¡Con ese cerebro ni para adivinar, {name}! ¡NPC!",
            "¡Basura, {name}! ¡Basura con nombre!",
            "¡Hasta un niño lo sabía y tú no, {name}! ¡Qué triste, fracasado!",
            "¡Qué forma de fallar, {name}, da lástima verte, inútil!",
            "¡Esto no es para ti, {name}! ¡Eres un cuck, acéptalo!",
            "¡Mejor deja la teoría, {name}, esto no es lo tuyo, beta!",
            "¡Dedícate a otra cosa, {name}, la sociónica no es para ti, desastre!",
            "¡Ríndete, {name}, nunca vas a entender esto, inútil!",
            "¿Para qué sigues, {name}? ¡Esto no es lo tuyo, fracasado!",
            "¡Deja de intentarlo, {name}, no naciste para esto, NPC!",
            "¡Tu nombre es vergüenza, {name}! ¡Eres un cuck con nombre!",
            "¡Eres una vergüenza para tu familia, {name}! ¡Hasta tu moral da pena!",
            "¡Tu familia se avergüenza de ti, {name}! ¡Y con razón, desastre!",
            "¡Con ese nombre deberías cambiarte el apellido, {name}! ¡Qué asco!",
            "¡Qué vergüenza, {name}, tu familia te ve fallando así! ¡Fracasado!",

    };

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
    private final ImageView img;
    private AnimatorSet idle, reaction;
    private Runnable idleStarter;
    private int lastCheer = -1, lastCeleb = -1, lastScold = -1;

    Companion(MainActivity a) {
        this.a = a;
        root = col(a);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
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

    /** Reacción súper expresiva al responder. Al acertar: celebración + confeti. Al fallar: decepción. */
    void react(boolean ok) {
        stopAll();
        if (ok) {
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
