package com.valhallaschool.app;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;

import java.util.Random;

import static com.valhallaschool.app.Ui.*;

/** Small custom-drawn views: cheaper than nesting layouts. */
final class Views {
    private Views() {}

    /** Generated book cover: gradient + spine + serif title. mode 0 = small (initials), 1 = grid (2:3, fills width), 2 = large. */
    static final class Cover extends View {
        final int mode;
        int book = -1;
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG), spine = new Paint();
        private final TextPaint tp = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        private final Path clip = new Path();
        private final RectF rf = new RectF();
        private StaticLayout lay;

        Cover(Context c, int mode) {
            super(c);
            this.mode = mode;
            tp.setColor(Color.WHITE);
            tp.setTypeface(SERIF_B);
            tp.setShadowLayer(dp(1.5f), 0, dp(1), 0x55000000);
            spine.setColor(0x30000000);
        }

        void setBook(int b) {
            if (b == book) return;
            book = b;
            rebuild();
            invalidate();
        }

        @Override protected void onMeasure(int ws, int hs) {
            if (mode == 1) {
                int w = MeasureSpec.getSize(ws);
                setMeasuredDimension(w, w * 3 / 2);
            } else super.onMeasure(ws, hs);
        }

        @Override protected void onSizeChanged(int w, int h, int ow, int oh) { rebuild(); }

        private void rebuild() {
            int w = getWidth(), h = getHeight();
            if (w == 0 || book < 0) return;
            float hue = (book * 47 + 12) % 360;
            p.setShader(new LinearGradient(0, 0, w * 0.55f, h, hsl(hue, .58f, .52f), hsl(hue + 35, .62f, .24f), Shader.TileMode.CLAMP));
            float r = mode == 0 ? dp(5) : dp(9);
            rf.set(0, 0, w, h);
            clip.reset();
            clip.addRoundRect(rf, r, r, Path.Direction.CW);
            String t = mode == 0 ? Data.initials(book) : Data.bTitle[book];
            tp.setTextSize(mode == 0 ? dp(13) : mode == 1 ? w * 0.145f : dp(19));
            int lw = Math.max(1, mode == 0 ? w : w - dp(mode == 2 ? 24 : 16));
            lay = StaticLayout.Builder.obtain(t, 0, t.length(), tp, lw)
                    .setAlignment(mode == 0 ? Layout.Alignment.ALIGN_CENTER : Layout.Alignment.ALIGN_NORMAL)
                    .setLineSpacing(0, 0.95f).setIncludePad(false)
                    .setMaxLines(mode == 0 ? 1 : 5).setEllipsize(TextUtils.TruncateAt.END).build();
        }

        @Override protected void onDraw(Canvas c) {
            if (lay == null) return;
            int w = getWidth(), h = getHeight();
            c.save();
            c.clipPath(clip);
            c.drawRect(0, 0, w, h, p);
            c.drawRect(0, 0, dp(mode == 0 ? 3 : 4), h, spine);
            c.restore();
            c.save();
            if (mode == 0) c.translate(0, (h - lay.getHeight()) / 2f);
            else c.translate(dp(mode == 2 ? 14 : 10), h - dp(mode == 2 ? 14 : 10) - lay.getHeight());
            lay.draw(c);
            c.restore();
        }
    }

    /** Rounded progress bar. */
    static final class Bar extends View {
        float v;
        int color = GREEN;
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF r = new RectF();

        Bar(Context c) { super(c); }

        Bar set(float value, int col) {
            v = Math.max(0, Math.min(1, value));
            color = col;
            invalidate();
            return this;
        }

        @Override protected void onDraw(Canvas c) {
            float h = getHeight(), rad = h / 2;
            r.set(0, 0, getWidth(), h);
            p.setColor(LINE);
            c.drawRoundRect(r, rad, rad, p);
            if (v > 0) {
                r.right = Math.max(h, getWidth() * v);
                p.setColor(color);
                c.drawRoundRect(r, rad, rad, p);
            }
        }
    }

    /** Circular progress ring. */
    static final class Ring extends View {
        float v;
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF r = new RectF();

        Ring(Context c, float strokeDp) {
            super(c);
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeCap(Paint.Cap.ROUND);
            p.setStrokeWidth(dp(strokeDp));
        }

        void set(float value) { v = Math.max(0, Math.min(1, value)); invalidate(); }

        @Override protected void onDraw(Canvas c) {
            float s = Math.min(getWidth(), getHeight()), in = p.getStrokeWidth() / 2;
            r.set(in, in, s - in, s - in);
            p.setColor(LINE);
            c.drawArc(r, 0, 360, false, p);
            if (v > 0) {
                p.setColor(ACCENT);
                c.drawArc(r, -90, 360 * v, false, p);
            }
        }
    }

    /** Duolingo-style 3D lesson node. */
    static final class Node extends View {
        int color;
        String glyph = "★";
        boolean cur, idle;
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint tp = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF r = new RectF();

        Node(Context c) {
            super(c);
            tp.setTextAlign(Paint.Align.CENTER);
            tp.setTypeface(BLACK);
            tp.setTextSize(dp(28));
            setClickable(true);
        }

        void set(int col, String g, boolean current, boolean isIdle) {
            color = col; glyph = g; cur = current; idle = isIdle;
            invalidate();
        }

        @Override protected void drawableStateChanged() {
            super.drawableStateChanged();
            invalidate();
        }

        @Override protected void onDraw(Canvas c) {
            float cx = getWidth() / 2f, top = dp(12), off = isPressed() ? dp(5) : 0;
            int face = idle ? SURF2 : color, edge = idle ? LINE : mix(color, 0xff000000, 0.55f);
            if (cur) {
                p.setStyle(Paint.Style.STROKE);
                p.setStrokeWidth(dp(5));
                p.setColor(alpha(color, 0.4f));
                r.set(cx - dp(46), top - dp(10), cx + dp(46), top + dp(83));
                c.drawOval(r, p);
                p.setStyle(Paint.Style.FILL);
            }
            if (off == 0) {
                p.setColor(edge);
                r.set(cx - dp(36), top + dp(7), cx + dp(36), top + dp(73));
                c.drawOval(r, p);
            }
            p.setColor(face);
            r.set(cx - dp(36), top + off, cx + dp(36), top + dp(66) + off);
            c.drawOval(r, p);
            tp.setColor(idle ? MUTED : Color.WHITE);
            c.drawText(glyph, cx, r.centerY() - (tp.descent() + tp.ascent()) / 2, tp);
        }
    }

    /** One-shot confetti burst; removes itself when finished. */
    static final class Confetti extends View {
        private static final int N = 44;
        private static final int[] COLS = {0xffffc83d, 0xff58cc02, 0xff1cb0f6, 0xffff4b4b, 0xffce82ff};
        private final float[] x = new float[N], dx = new float[N], dur = new float[N], delay = new float[N], rot = new float[N];
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        private float t;

        Confetti(Context c) {
            super(c);
            Random r = new Random();
            for (int i = 0; i < N; i++) {
                x[i] = r.nextFloat();
                dx[i] = (r.nextFloat() - .5f) * dp(200);
                dur[i] = 1.6f + r.nextFloat() * 1.4f;
                delay[i] = r.nextFloat() * .3f;
                rot[i] = r.nextFloat() * 900;
            }
        }

        void start() {
            ValueAnimator a = ValueAnimator.ofFloat(0, 3.4f);
            a.setDuration(3400);
            a.setInterpolator(new LinearInterpolator());
            a.addUpdateListener(an -> { t = (float) an.getAnimatedValue(); invalidate(); });
            a.addListener(new AnimatorListenerAdapter() {
                @Override public void onAnimationEnd(Animator animation) {
                    if (getParent() != null) ((ViewGroup) getParent()).removeView(Confetti.this);
                }
            });
            a.start();
        }

        @Override protected void onDraw(Canvas c) {
            float h = getHeight(), w = getWidth(), pw = dp(9), ph = dp(14);
            for (int i = 0; i < N; i++) {
                float k = (t - delay[i]) / dur[i];
                if (k <= 0 || k >= 1) continue;
                p.setColor(COLS[i % COLS.length]);
                c.save();
                c.translate(x[i] * w + dx[i] * k, -ph + (h + ph * 2) * k);
                c.rotate(rot[i] * k);
                c.drawRect(-pw / 2, -ph / 2, pw / 2, ph / 2, p);
                c.restore();
            }
        }
    }
}
