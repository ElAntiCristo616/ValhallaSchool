package com.valhallaschool.app;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.os.Handler;
import android.os.Looper;
import android.view.View;

import java.util.Random;

import static com.valhallaschool.app.Ui.*;

/**
 * Fondo ambiental del tema: degradado radial sutil + partículas
 * ambientales muy tenues y realistas. Es la CAPA DE FONDO del root
 * (índice 0): queda DETRÁS de todo el contenido — tarjetas, textos,
 * botones y barra de navegación — y nada se dibuja jamás por encima
 * de ellos. El movimiento es apenas perceptible; ante la duda, más
 * sutil. Se pausa en onPause (~20 fps, 50 partículas con tamaños
 * variados y 5 glows grandes y suaves para que el fondo no se vea
 * plano, v1.3.1).
 *
 * Tipos (Ui.THEME_PARTICLE): 0 nieve, 1 copos grandes, 2 pétalos,
 * 3 brasas, 4 burbujas, 5 estrellas, 6 polen, 7 chispas doradas,
 * 8 destellos, 9 hojas de otoño, 10 dulces, 11 lluvia, 12 motas,
 * 13 aurora, 14 arena, 15 fugaces.
 */
final class ParticleView extends View {
    private static final int N = 50; // v1.3.1: 2.5x más partículas (antes 20)
    private static final long FRAME_MS = 50; // ~20 fps: sobra para un ambiente
    /** Alfa máxima de una partícula: apenas perceptible. */
    private static final float A_MAX = 0.16f;

    private final Handler h = new Handler(Looper.getMainLooper());
    private final Random r = new Random();
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint bgPaint = new Paint();
    private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final float[] x = new float[N], y = new float[N], vx = new float[N], vy = new float[N],
            size = new float[N], phase = new float[N], rot = new float[N], vr = new float[N],
            aBase = new float[N];
    private final int[] col = new int[N];
    private int[] cols; // paleta del tema (col[i] es índice dentro de cols)
    private Bitmap[] dotBmp; // punto suave pre-renderizado, uno por color de paleta
    private RadialGradient bgGrad, bgGlow1, bgGlow2;
    private int kind;
    private boolean running;
    private float t;

    private final Runnable tick = new Runnable() {
        @Override public void run() {
            if (!running) return;
            t += FRAME_MS / 1000f;
            step();
            invalidate();
            h.postDelayed(this, FRAME_MS);
        }
    };

    ParticleView(Context c, String themeId) {
        super(c);
        setClickable(false);
        setFocusable(false);
        kind = Ui.THEME_PARTICLE[Ui.themeIndex(themeId)];
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeCap(Paint.Cap.ROUND);
        seed();
    }

    private int[] paletteFor(int kind) {
        switch (kind) {
            case 0: case 1: return new int[]{0xffffffff, 0xffeaf3fb};
            case 2: return new int[]{0xfff4a7c3, 0xffffd6e8, 0xffff8fab};
            case 3: return new int[]{0xffff6b35, 0xffffc53d, 0xffff8c42};
            case 4: return new int[]{0xff4dd0e1, 0xffb2ebf2, 0xffffffff};
            case 5: return new int[]{0xffffffff, 0xffffd166, 0xff9b7ede};
            case 6: return new int[]{0xff7fb069, 0xffa8d08d, 0xffd9a441};
            case 7: return new int[]{0xfff4a259, 0xffe87b2e, 0xffffd166};
            case 8: return new int[]{0xff00f5d4, 0xffff2e88, 0xffffffff};
            case 9: return new int[]{0xffd9822b, 0xffa63d2f, 0xffe0a458};
            case 10: return new int[]{0xffff8fab, 0xffffc8dd, 0xffffd166, 0xff7ef0c1, 0xff8e9bff};
            case 11: return new int[]{0xff7fb8e8, 0xffbfe0f5};
            case 12: return new int[]{0xffe8c96a, 0xfff5efe0};
            case 13: return new int[]{0xff7ef0c1, 0xffb388ff, 0xff4dd0e1};
            case 14: return new int[]{0xffe0a458, 0xffc97b2d, 0xfffdf3dc};
            default: return new int[]{0xffffffff, 0xff8e9bff, 0xffffd166};
        }
    }

    /** Punto suave: disco con degradado radial a transparente (nunca borde duro). */
    private Bitmap softDot(int color) {
        Bitmap b = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(b);
        Paint pp = new Paint(Paint.ANTI_ALIAS_FLAG);
        pp.setShader(new RadialGradient(32, 32, 32,
                new int[]{color, color & 0x00ffffff}, null, Shader.TileMode.CLAMP));
        c.drawRect(0, 0, 64, 64, pp);
        return b;
    }

    private void seed() {
        int[] cols = paletteFor(kind);
        this.cols = cols;
        dotBmp = new Bitmap[cols.length];
        for (int i = 0; i < cols.length; i++) dotBmp[i] = softDot(cols[i]);
        for (int i = 0; i < N; i++) {
            x[i] = r.nextFloat();
            y[i] = r.nextFloat();
            phase[i] = r.nextFloat() * 6.28f;
            rot[i] = r.nextFloat() * 360f;
            col[i] = r.nextInt(cols.length);
            aBase[i] = 0.08f + r.nextFloat() * 0.08f; // 0.08–0.16
            switch (kind) {
                case 0: // nieve: copitos suaves, caída muy lenta
                    vx[i] = (r.nextFloat() - .5f) * 0.006f; vy[i] = 0.008f + r.nextFloat() * 0.010f;
                    size[i] = dp(1.5f + r.nextFloat() * 1.5f); break;
                case 1: // copos grandes: algo más grandes, aún lentos
                    vx[i] = (r.nextFloat() - .5f) * 0.005f; vy[i] = 0.006f + r.nextFloat() * 0.007f;
                    size[i] = dp(2.5f + r.nextFloat() * 1.5f); break;
                case 2: // pétalos: elipse rosa con vaivén y giro lento
                    vx[i] = (r.nextFloat() - .5f) * 0.008f; vy[i] = 0.008f + r.nextFloat() * 0.008f;
                    size[i] = dp(2f + r.nextFloat() * 1.5f); vr[i] = (r.nextFloat() - .5f) * 40f; break;
                case 3: // brasas: suben lento, parpadeo suave
                    vx[i] = (r.nextFloat() - .5f) * 0.006f; vy[i] = -(0.006f + r.nextFloat() * 0.008f);
                    size[i] = dp(1.2f + r.nextFloat() * 1f); break;
                case 4: // burbujas: aros tenues subiendo lento
                    vx[i] = (r.nextFloat() - .5f) * 0.004f; vy[i] = -(0.006f + r.nextFloat() * 0.006f);
                    size[i] = dp(2f + r.nextFloat() * 2.5f); break;
                case 5: case 8: // estrellas / destellos: fijas, titilan lento
                    vx[i] = 0; vy[i] = 0; size[i] = dp(1f + r.nextFloat() * 1f); break;
                case 6: // polen: deriva horizontal casi quieto
                    vx[i] = (r.nextFloat() - .5f) * 0.006f; vy[i] = 0.002f + r.nextFloat() * 0.004f;
                    size[i] = dp(1.2f + r.nextFloat() * 1f); break;
                case 7: // chispas doradas: polvo tenue bajando muy lento
                    vx[i] = (r.nextFloat() - .5f) * 0.004f; vy[i] = 0.004f + r.nextFloat() * 0.006f;
                    size[i] = dp(1f + r.nextFloat() * 1f); break;
                case 9: // hojas de otoño: elipse suave, caída lenta con vaivén
                    vx[i] = (r.nextFloat() - .5f) * 0.008f; vy[i] = 0.007f + r.nextFloat() * 0.007f;
                    size[i] = dp(2f + r.nextFloat() * 1.5f); vr[i] = (r.nextFloat() - .5f) * 40f; break;
                case 10: // dulces: confeti diminuto bajando lento
                    vx[i] = (r.nextFloat() - .5f) * 0.006f; vy[i] = 0.008f + r.nextFloat() * 0.007f;
                    size[i] = dp(1.5f + r.nextFloat() * 1f); break;
                case 11: // lluvia: rayitas finas y tenues
                    vx[i] = -0.01f; vy[i] = 0.10f + r.nextFloat() * 0.06f;
                    size[i] = dp(8 + r.nextFloat() * 6); break; // largo de la rayita
                case 12: // motas (Claro): polvo dorado casi quieto
                    vx[i] = (r.nextFloat() - .5f) * 0.003f; vy[i] = 0.001f + r.nextFloat() * 0.003f;
                    size[i] = dp(1f + r.nextFloat() * 0.8f); break;
                case 13: // aurora: elipses alargadas tenues, deriva horizontal lenta
                    vx[i] = (r.nextFloat() - .5f) * 0.008f; vy[i] = (r.nextFloat() - .5f) * 0.002f;
                    size[i] = dp(3f + r.nextFloat() * 3f); break;
                case 14: // arena: granitos finos derivando
                    vx[i] = 0.004f + r.nextFloat() * 0.006f; vy[i] = (r.nextFloat() - .5f) * 0.002f;
                    size[i] = dp(1f + r.nextFloat() * 1f); break;
                default: // 15 fugaces: estrellas fijas + 2 estelas ocasionales
                    if (i >= N - 2) {
                        // estela: x/y = punto de partida; phase = periodo (s); rot = desfase (s)
                        phase[i] = 14f + r.nextFloat() * 6f;
                        rot[i] = r.nextFloat() * phase[i];
                        x[i] = 0.1f + r.nextFloat() * 0.5f;
                        y[i] = 0.05f + r.nextFloat() * 0.3f;
                        size[i] = dp(26);
                    } else {
                        vx[i] = 0; vy[i] = 0; size[i] = dp(1f + r.nextFloat() * 1f);
                    }
                    break;
            }
            // Variedad de tamaño y profundidad (v1.3.1): cada partícula
            // entre 0.6x y 1.7x su tamaño base, y 1 de cada 10 es un glow
            // grande y suave (2.2x, alfa a la mitad) para que el fondo
            // no se vea como un tono sólido plano. El alfa máximo no
            // cambia: el fondo sigue igual de opaco.
            size[i] *= 0.6f + r.nextFloat() * 1.1f;
            if (i % 10 == 4) { size[i] *= 2.2f; aBase[i] *= 0.55f; }
        }
    }

    void start() {
        if (running) return;
        running = true;
        h.post(tick);
    }

    void stop() {
        running = false;
        h.removeCallbacks(tick);
    }

    private void step() {
        float w = getWidth(), hgt = getHeight();
        if (w == 0 || hgt == 0) return;
        float dt = FRAME_MS / 1000f;
        for (int i = 0; i < N; i++) {
            if (kind == 15 && i >= N - 2) continue; // las estelas se calculan en onDraw
            float sway = 0;
            if (kind == 0 || kind == 1 || kind == 2 || kind == 6 || kind == 9 || kind == 10 || kind == 12 || kind == 14)
                sway = (float) Math.sin(t * 0.5f + phase[i]) * 0.0004f;
            x[i] += vx[i] * dt + sway;
            y[i] += vy[i] * dt;
            rot[i] += vr[i] * dt;
            if (y[i] > 1.05f) { y[i] = -0.05f; x[i] = r.nextFloat(); }
            if (y[i] < -0.05f) { y[i] = 1.05f; x[i] = r.nextFloat(); }
            if (x[i] < -0.05f) x[i] = 1.05f;
            if (x[i] > 1.05f) x[i] = -0.05f;
        }
    }

    @Override protected void onSizeChanged(int w, int h, int oldW, int oldH) {
        super.onSizeChanged(w, h, oldW, oldH);
        if (w == 0 || h == 0) return;
        // Fondo casi opaco y ambientado: centro apenas más cálido (acento al 7%),
        // bordes apenas más oscuros. Sutil por diseño.
        bgGrad = new RadialGradient(w * 0.5f, h * 0.38f, Math.max(w, h) * 0.85f,
                new int[]{mix(ACCENT, BG, 0.07f), mix(0xff000000, BG, 0.08f)},
                null, Shader.TileMode.CLAMP);
        // v1.3.1: dos brillos sutiles extra para matar el tono sólido plano:
        // azul frío abajo a la izquierda (5%) y acento cálido arriba a la
        // derecha (4%). El fondo sigue igual de opaco.
        bgGlow1 = new RadialGradient(w * 0.12f, h * 0.92f, Math.max(w, h) * 0.6f,
                new int[]{mix(BLUE, BG, 0.05f), BG},
                null, Shader.TileMode.CLAMP);
        bgGlow2 = new RadialGradient(w * 0.92f, h * 0.08f, Math.max(w, h) * 0.55f,
                new int[]{mix(ACCENT, BG, 0.04f), BG},
                null, Shader.TileMode.CLAMP);
    }

    /** Alfa de la partícula i en este instante (0..1), con titileo suave si aplica. */
    private float alphaAt(int i) {
        float a = aBase[i];
        if (kind == 3 || kind == 5 || kind == 7 || kind == 8 || (kind == 15 && i < N - 2))
            a *= 0.55f + 0.45f * (float) Math.sin(t * 0.9f + phase[i]);
        return Math.max(0.02f, Math.min(A_MAX, a));
    }

    @Override protected void onDraw(Canvas c) {
        float w = getWidth(), hgt = getHeight();
        if (w == 0 || hgt == 0) return;
        // 1) Fondo ambiental (siempre detrás de todo).
        if (bgGrad != null) {
            bgPaint.setShader(bgGrad);
            c.drawRect(0, 0, w, hgt, bgPaint);
            if (bgGlow1 != null) { bgPaint.setShader(bgGlow1); c.drawRect(0, 0, w, hgt, bgPaint); }
            if (bgGlow2 != null) { bgPaint.setShader(bgGlow2); c.drawRect(0, 0, w, hgt, bgPaint); }
        } else {
            c.drawColor(BG);
        }
        // 2) Partículas ambientales: pequeñas, tenues, de borde suave.
        for (int i = 0; i < N; i++) {
            float a = alphaAt(i);
            if (kind == 15 && i >= N - 2) { drawStreak(c, i, w, hgt); continue; }
            float px = x[i] * w, py = y[i] * hgt;
            switch (kind) {
                case 11: { // lluvia: rayita vertical fina y tenue
                    linePaint.setColor(cols[col[i]]);
                    linePaint.setStrokeWidth(dp(1f));
                    linePaint.setAlpha(Math.round(255 * a));
                    c.drawLine(px, py, px + dp(1.5f), py + size[i], linePaint);
                    break;
                }
                case 4: { // burbujas: aro tenue
                    linePaint.setColor(cols[col[i]]);
                    linePaint.setStrokeWidth(dp(1f));
                    linePaint.setAlpha(Math.round(255 * a * 0.8f));
                    c.drawCircle(px, py, size[i], linePaint);
                    break;
                }
                case 2: case 9: case 13: { // pétalos / hojas / aurora: elipse suave girada
                    float rx = kind == 13 ? size[i] * 2.2f : size[i] * 1.4f;
                    float ry = kind == 13 ? size[i] * 0.7f : size[i] * 0.9f;
                    c.save();
                    c.translate(px, py);
                    c.rotate(rot[i]);
                    p.setAlpha(Math.round(255 * a));
                    c.drawBitmap(dotBmp[col[i]], null,
                            new android.graphics.RectF(-rx, -ry, rx, ry), p);
                    c.restore();
                    break;
                }
                default: { // puntos suaves: nieve, copos, brasas, estrellas, polen,
                           // chispas, destellos, dulces, motas, arena
                    p.setAlpha(Math.round(255 * a));
                    float s = size[i];
                    c.drawBitmap(dotBmp[col[i]], null,
                            new android.graphics.RectF(px - s, py - s, px + s, py + s), p);
                    break;
                }
            }
        }
    }

    /** Estrella fugaz ocasional: aparece y se apaga en un ciclo largo y lento. */
    private void drawStreak(Canvas c, int i, float w, float hgt) {
        float period = phase[i];
        float prog = ((t + rot[i]) % period) / period; // 0..1 en ~14–20 s
        float a = A_MAX * 1.2f * (float) Math.pow(Math.sin(Math.PI * prog), 2);
        if (a < 0.01f) return;
        float px = (x[i] + prog * 0.30f) * w;
        float py = (y[i] + prog * 0.12f) * hgt;
        linePaint.setColor(0xffffffff);
        linePaint.setStrokeWidth(dp(1.2f));
        linePaint.setAlpha(Math.round(255 * a));
        c.drawLine(px, py, px - size[i], py - size[i] * 0.35f, linePaint);
        p.setAlpha(Math.round(255 * a));
        float s = dp(1.5f);
        c.drawBitmap(dotBmp[0], null,
                new android.graphics.RectF(px - s, py - s, px + s, py + s), p);
    }
}
