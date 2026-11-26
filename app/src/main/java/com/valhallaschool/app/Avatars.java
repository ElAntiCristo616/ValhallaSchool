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

    static final String[] PHRASES = {
            "¡Tú puedes! Un ejemplo más.",
            "Ese estuvo bueno. ¿El siguiente?",
            "La sociónica se aprende jugando.",
            "¡Vamos! Cada acierto suma.",
            "Fallar también enseña: así se aprende.",
            "¿Ya viste qué función era?",
            "Sigue así, vas muy bien.",
            "Un tipo a la vez, sin prisa.",
            "¡Ese ejemplo era de los difíciles!",
            "Respira y piensa en el Modelo A.",
            "La práctica hace al tipador.",
            "¿Otro round? Yo te acompaño.",
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
        TextView t = tv(c, PHRASES[Quiz.R.nextInt(PHRASES.length)], 14, TEXT, REG);
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
