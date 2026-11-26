package com.valhallaschool.app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Handler;
import android.os.Looper;
import android.view.View;

import java.util.Random;

import static com.valhallaschool.app.Ui.*;

/**
 * Partículas cayendo (o flotando) en tiempo real, una por tema, como
 * overlay por encima del fondo y por debajo de toasts/modales.
 * No intercepta toques (clickable=false). Se pausa en onPause.
 *
 * Tipos (Ui.THEME_PARTICLE): 0 nieve, 1 copos grandes, 2 pétalos,
 * 3 brasas, 4 burbujas, 5 estrellas, 6 hojas/polen, 7 chispas,
 * 8 destellos, 9 hojas de otoño, 10 dulces, 11 lluvia, 12 motas,
 * 13 aurora, 14 arena, 15 fugaces.
 */
final class ParticleView extends View {
    private static final int N = 60;
    private static final long FRAME_MS = 33; // ~30 fps

    private final Handler h = new Handler(Looper.getMainLooper());
    private final Random r = new Random();
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final float[] x = new float[N], y = new float[N], vx = new float[N], vy = new float[N],
            size = new float[N], phase = new float[N], rot = new float[N], vr = new float[N];
    private final int[] col = new int[N];
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

    private void seed() {
        int[] cols = paletteFor(kind);
        for (int i = 0; i < N; i++) {
            x[i] = r.nextFloat();
            y[i] = r.nextFloat();
            phase[i] = r.nextFloat() * 6.28f;
            rot[i] = r.nextFloat() * 360f;
            col[i] = cols[r.nextInt(cols.length)];
            switch (kind) {
                case 3: // brasas: suben
                    vx[i] = (r.nextFloat() - .5f) * 0.02f; vy[i] = -(0.05f + r.nextFloat() * 0.09f);
                    size[i] = dp(2 + r.nextFloat() * 3); break;
                case 4: // burbujas: suben
                    vx[i] = (r.nextFloat() - .5f) * 0.015f; vy[i] = -(0.03f + r.nextFloat() * 0.06f);
                    size[i] = dp(3 + r.nextFloat() * 7); break;
                case 5: case 8: case 15: // estrellas / destellos / fugaces: fijas
                    vx[i] = 0; vy[i] = 0; size[i] = dp(1.5f + r.nextFloat() * 2.5f); break;
                case 11: // lluvia: cae rápido
                    vx[i] = -0.02f; vy[i] = 0.55f + r.nextFloat() * 0.45f;
                    size[i] = dp(10 + r.nextFloat() * 10); break;
                case 13: // aurora: deriva horizontal lenta
                    vx[i] = (r.nextFloat() - .5f) * 0.03f; vy[i] = (r.nextFloat() - .5f) * 0.008f;
                    size[i] = dp(3 + r.nextFloat() * 6); break;
                default: // caen con vaivén: nieve, pétalos, hojas, dulces, motas, arena, chispas
                    vx[i] = (r.nextFloat() - .5f) * 0.02f; vy[i] = 0.03f + r.nextFloat() * 0.07f;
                    size[i] = dp(3 + r.nextFloat() * 5);
                    vr[i] = (r.nextFloat() - .5f) * 120f;
                    break;
            }
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
        for (int i = 0; i < N; i++) {
            float sway = 0;
            if (kind == 0 || kind == 1 || kind == 2 || kind == 6 || kind == 9 || kind == 10 || kind == 12 || kind == 14)
                sway = (float) Math.sin(t * 1.5f + phase[i]) * 0.0006f;
            x[i] += vx[i] * (FRAME_MS / 1000f) * 3f + sway;
            y[i] += vy[i] * (FRAME_MS / 1000f) * 3f;
            rot[i] += vr[i] * FRAME_MS / 1000f;
            if (y[i] > 1.05f) { y[i] = -0.05f; x[i] = r.nextFloat(); }
            if (y[i] < -0.05f) { y[i] = 1.05f; x[i] = r.nextFloat(); }
            if (x[i] < -0.05f) x[i] = 1.05f;
            if (x[i] > 1.05f) x[i] = -0.05f;
        }
    }

    @Override protected void onDraw(Canvas c) {
        float w = getWidth(), hgt = getHeight();
        if (w == 0 || hgt == 0) return;
        for (int i = 0; i < N; i++) {
            float px = x[i] * w, py = y[i] * hgt;
            float alpha = 1f;
            if (kind == 5 || kind == 8 || kind == 15) // titilan
                alpha = 0.35f + 0.65f * (0.5f + 0.5f * (float) Math.sin(t * 2.5f + phase[i]));
            if (kind == 3) // brasas parpadean
                alpha = 0.5f + 0.5f * (float) Math.sin(t * 6f + phase[i]);
            p.setColor(col[i]);
            p.setAlpha(Math.round(255 * Math.max(0.1f, Math.min(1f, alpha)) * 0.85f));
            p.setStyle(Paint.Style.FILL);
            switch (kind) {
                case 11: // lluvia: líneas cortas
                    p.setStrokeWidth(dp(1.5f));
                    c.drawLine(px, py, px + dp(2), py + size[i], p);
                    break;
                case 4: // burbujas: aro
                    p.setStyle(Paint.Style.STROKE);
                    p.setStrokeWidth(dp(1.2f));
                    c.drawCircle(px, py, size[i], p);
                    break;
                case 2: case 6: case 9: // pétalos / hojas: rectángulo girado
                    c.save();
                    c.translate(px, py);
                    c.rotate(rot[i]);
                    c.drawRoundRect(-size[i], -size[i] * 0.6f, size[i], size[i] * 0.6f, size[i] * 0.5f, size[i] * 0.5f, p);
                    c.restore();
                    break;
                case 15: // fugaces: punto con estela (solo algunas)
                    c.drawCircle(px, py, size[i], p);
                    if (i % 9 == 0) {
                        p.setStrokeWidth(dp(1.5f));
                        c.drawLine(px, py, px - dp(22), py + dp(8), p);
                    }
                    break;
                case 13: // aurora: puntos grandes y suaves
                    c.drawCircle(px, py, size[i] * 1.6f, p);
                    break;
                default: // nieve, copos, brasas, estrellas, chispas, destellos, dulces, motas, arena
                    c.drawCircle(px, py, size[i], p);
                    break;
            }
        }
    }
}
