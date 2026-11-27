package com.valhallaschool.app;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.Gravity;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;

import static com.valhallaschool.app.Ui.*;

/**
 * Los 48 avatares de Binky (16 tipos sociónicos × 3 variantes Avatar1/2/3)
 * en assets/avatars/. Uso libre con su permiso (ver Créditos en Perfil).
 *
 * Rotación determinista: un contador persistido en Store (avIdx) recorre
 * los 48 en orden, sin repetir hasta agotarlos, así TODOS aparecen por
 * las clases, estilo Duolingo: pequeños, en esquina, sin tapar nada.
 * La mascota actual NO se quita: los avatares SE SUMAN.
 */
final class Avatars {
    /** Orden fijo de rotación: un tipo por cuadra, Avatar1/2/3. */
    static final String[] TYPES = {"ILE", "LII", "ESE", "SEI",   // Alfa
            "EIE", "IEI", "SLE", "LSI",   // Beta
            "ESI", "SEE", "LIE", "ILI",   // Gamma
            "LSE", "SLI", "IEE", "EII"};  // Delta
    static final String[] FILES = new String[48];

    static {
        int k = 0;
        for (String t : TYPES) for (int v = 1; v <= 3; v++) FILES[k++] = t + "_Avatar" + v + ".png";
    }

    /** Frases de la mascota (bocadillo pequeño en Inicio y Práctica). TODAS
     *  humillan al jugador por su nombre (orden de Remy 2026-11-27): NADA de
     *  elogios sinceros. {name} = nombre de usuario actual (nickname o «Sin nombre»). */
    static final String[] PHRASES = {
            "¡Tú puedes, {name}! Un ejemplo más… a ver si no la cagas.",
            "Ese estuvo bueno, {name}. ¿Casualidad o copiaste?",
            "La sociónica se aprende jugando… tú, {name}, a duras penas.",
            "¡Vamos, {name}! Cada acierto suma… cuando aciertas, claro.",
            "Fallar también enseña, {name}: tú ya eres todo un maestro del fracaso.",
            "¿Ya viste qué función era, {name}? Ni tú, ¿verdad?",
            "Sigue así, {name}… total, peor no se puede. O sí, contigo sí.",
            "Un tipo a la vez, {name}, sin prisa… que prisa no tienes, ¿o sí?",
            "¡Ese ejemplo era de los difíciles, {name}! Por eso lo fallaste.",
            "Respira y piensa en el Modelo A, {name}… si es que puedes.",
            "La práctica hace al tipador, {name}. A ti todavía te falta práctica. Mucha.",
            "¿Otro round, {name}? Yo te acompaño… alguien tiene que reírse de ti.",
    };

    private Avatars() {}

    /** Siguiente avatar en rotación (los 48 en orden, sin repetir hasta agotar). */
    static String nextFile() {
        Store S = Store.I;
        int i = S.avIdx % FILES.length;
        S.avIdx = (i + 1) % FILES.length;
        S.save(); // debounced (400 ms): barato aunque se llame seguido
        return FILES[i];
    }

    /** Bitmap del asset, reducido a maxPx en su lado mayor (los PNG son ~1050×1670). Null si falla. */
    static Bitmap load(Context c, String file, int maxPx) {
        try {
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inJustDecodeBounds = true;
            try (InputStream in = c.getAssets().open("avatars/" + file)) {
                BitmapFactory.decodeStream(in, null, o);
            }
            int w = Math.max(o.outWidth, 1), h = Math.max(o.outHeight, 1);
            int sample = 1;
            while (Math.max(w, h) / sample > maxPx) sample *= 2;
            BitmapFactory.Options o2 = new BitmapFactory.Options();
            o2.inSampleSize = sample;
            try (InputStream in = c.getAssets().open("avatars/" + file)) {
                return BitmapFactory.decodeStream(in, null, o2);
            }
        } catch (IOException e) {
            return null;
        }
    }

    /** Miniaturas (128px) con caché, para la cuadrícula de 48 del Perfil. */
    private static final HashMap<String, Bitmap> THUMBS = new HashMap<>();

    static Bitmap thumb(Context c, String file) {
        Bitmap b = THUMBS.get(file);
        if (b == null) {
            b = load(c, file, dp(128));
            if (b != null) THUMBS.put(file, b);
        }
        return b;
    }

    /** ImageView cuadrado con el siguiente avatar de la rotación. Si no carga, queda oculta. */
    static ImageView view(Context c, int sizeDp) {
        ImageView iv = new ImageView(c);
        Bitmap b = load(c, nextFile(), dp(256));
        if (b == null) { iv.setVisibility(android.view.View.GONE); return iv; }
        iv.setImageBitmap(b);
        iv.setAdjustViewBounds(true);
        iv.setScaleType(ImageView.ScaleType.FIT_CENTER);
        iv.setLayoutParams(lp(dp(sizeDp), dp(sizeDp)));
        return iv;
    }

    /** Fila estilo Duolingo: avatar pequeño + bocadillo con una frase corta. */
    static LinearLayout bubble(Context c) {
        LinearLayout r = row(c);
        r.addView(view(c, 56), lp(dp(56), dp(56)));
        TextView t = tv(c, PHRASES[Quiz.R.nextInt(PHRASES.length)].replace("{name}", Store.I.displayName()), 14, TEXT, REG);
        t.setBackground(round(SURF, 16, LINE, 1));
        t.setPadding(dp(14), dp(10), dp(14), dp(10));
        t.setLineSpacing(0, 1.25f);
        LinearLayout.LayoutParams p = lpm(0, W, 10, 0, 0, 0);
        p.weight = 1;
        r.addView(t, p);
        r.setGravity(Gravity.CENTER_VERTICAL);
        return r;
    }
}
