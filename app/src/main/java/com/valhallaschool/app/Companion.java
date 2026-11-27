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
 * Las frases de decepción son a propósito feas y burlonas (coach que te
 * regaña, pedido por Remy): van contra el jugador, nunca contra grupos
 * protegidos (nada de raza, género, religión ni discapacidad).
 */
final class Companion {
    /** Ánimo antes de responder. {name} = nombre de usuario actual: el nickname
     *  si escribió uno, o «Sin nombre» si no (Store.displayName() siempre).
     *  El acompañante SIEMPRE dice el nombre, como pidió Remy. */
    static final String[] CHEER = {
            "¡Tú puedes, {name}! ¡Tú sí sabes! 💪",
            "¡Vamos, {name}, que esta la sacas!",
            "Respira… y piensa en el Modelo A, {name}.",
            "¡Esta es tuya, {name}! Yo te creo.",
            "¡Dale, {name}! ¿Qué función será?",
            "Tú ya sabes esto, {name}. ¡Confía!",
            "¡Vamos por otra, {name}! No pares.",
            "¿Listo, {name}? ¡A darle!",
            "¡Tú puedes con esto y con más, {name}!",
            "Piensa como tipador, {name}. ¡Tú eres tipador!",
            "¡Otra más, {name}! Cada una te hace más fuerte.",
            "¡Ánimo, {name}! Yo te acompaño.",
            "Mira bien la frase, {name}… la respuesta está ahí.",
            "¡Tú sí sabes, {name}! ¡Demuéstralo!",
            "¡Vamos, {name}! ¡Que nadie te pare!",
            "¿Otra ronda, {name}? ¡Claro que sí!",
    };

    /** Celebración al acertar. */
    static final String[] CELEBRATE = {
            "¡Esooo! ¡Qué crack!",
            "¡Lo sabías! ¡Yo lo sabía!",
            "¡Imparable, {name}!",
            "¡Qué bárbaro! ¡Otra así!",
            "¡Exacto! ¡Eres una máquina!",
            "¡Boom! ¡Directo al blanco!",
            "¡Genial, {name}! ¡Sigue así!",
            "¡Eso es! ¡Nadie te para!",
            "¡Perfecto! ¡Me encanta verte así!",
            "¡Increíble, {name}! ¡Qué nivel!",
            "¡Sí señor! ¡Esa era!",
            "¡Brutal! ¡Otra, otra!",
    };

    /** Decepción al fallar: feas y burlonas, contra el jugador, nunca contra grupos protegidos. */
    static final String[] SCOLD = {
            "¡Qué vergüenza, {name}!",
            "¡Patético! ¡Ni con suerte!",
            "¡Hasta mi abuela lo sabía!",
            "¡Eres un desastre, {name}!",
            "¡Qué oso! ¡Ni para eso!",
            "¡Terrible! ¡Vuélvelo a intentar, a ver si ahora sí!",
            "¡Ay, no! ¿Qué fue eso?",
            "¡Pésimo, {name}! ¡Pésimo!",
            "¡Vergüenza ajena! ¡Hasta yo me sonrojé!",
            "¡Ese error me dolió hasta a mí!",
            "¡Ni adivinando le atinas!",
            "¡Qué barbaridad, {name}!",
            "¡Me das pena… y eso que soy un dibujo!",
            "¡Eso estuvo feo. Muy feo!",
            "¡Qué mierda de respuesta, {name}!",
            "¡Joder, {name}! ¡Concéntrate!",
            "¡De pena ajena! ¡Hasta el teléfono se avergonzó!",
            "¡Horrible! ¡Ni el azar te quiere!",
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
