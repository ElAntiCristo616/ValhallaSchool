package app.stash;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.RippleDrawable;
import android.graphics.drawable.StateListDrawable;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Locale;
import java.util.function.IntConsumer;

/** Palette, typefaces and tiny view factories. Everything is built in code (no XML inflation = fast). */
final class Ui {
    static final int M = ViewGroup.LayoutParams.MATCH_PARENT, W = ViewGroup.LayoutParams.WRAP_CONTENT;
    static float density;
    static boolean dark;
    static int BG, SURF, SURF2, LINE, TEXT, MUTED, ACCENT;
    static final int ACCENT_INK = 0xff1a1400, GREEN = 0xff58cc02, GREEN_D = 0xff46a302, RED = 0xffff4b4b, RED_D = 0xffd93838, BLUE = 0xff1cb0f6;
    static Typeface BLACK, BOLD, MEDIUM, REG, SERIF_B;

    static final int B_GREEN = 0, B_GHOST = 1, B_YELLOW = 2, B_RED = 3, B_OFF = 4, B_BLUE = 5;

    private Ui() {}

    static void init(Context c, boolean isDark) {
        density = c.getResources().getDisplayMetrics().density;
        BLACK = Typeface.create("sans-serif-black", Typeface.NORMAL);
        BOLD = Typeface.create("sans-serif", Typeface.BOLD);
        MEDIUM = Typeface.create("sans-serif-medium", Typeface.NORMAL);
        REG = Typeface.create("sans-serif", Typeface.NORMAL);
        SERIF_B = Typeface.create(Typeface.SERIF, Typeface.BOLD);
        dark = isDark;
        if (isDark) {
            BG = 0xff0f1014; SURF = 0xff18191f; SURF2 = 0xff22232b; LINE = 0xff2d2e38; TEXT = 0xfff4f2ec; MUTED = 0xff9b9ba8; ACCENT = 0xffffc83d;
        } else {
            BG = 0xfff5f3ec; SURF = 0xffffffff; SURF2 = 0xffeeebe2; LINE = 0xffe2ded2; TEXT = 0xff191920; MUTED = 0xff6c6c78; ACCENT = 0xfff2b100;
        }
    }

    static int dp(float v) { return Math.round(v * density); }

    /** t = share of colour a. */
    static int mix(int a, int b, float t) {
        return Color.rgb((int) (Color.red(a) * t + Color.red(b) * (1 - t)),
                (int) (Color.green(a) * t + Color.green(b) * (1 - t)),
                (int) (Color.blue(a) * t + Color.blue(b) * (1 - t)));
    }

    static int alpha(int c, float a) { return (c & 0x00ffffff) | (Math.round(a * 255) << 24); }

    static int hsl(float h, float s, float l) {
        h = ((h % 360) + 360) % 360;
        float c = (1 - Math.abs(2 * l - 1)) * s, x = c * (1 - Math.abs((h / 60) % 2 - 1)), m = l - c / 2;
        float r, g, b;
        if (h < 60) { r = c; g = x; b = 0; } else if (h < 120) { r = x; g = c; b = 0; } else if (h < 180) { r = 0; g = c; b = x; }
        else if (h < 240) { r = 0; g = x; b = c; } else if (h < 300) { r = x; g = 0; b = c; } else { r = c; g = 0; b = x; }
        return Color.rgb(Math.round((r + m) * 255), Math.round((g + m) * 255), Math.round((b + m) * 255));
    }

    static GradientDrawable round(int fill, float rDp) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(fill);
        g.setCornerRadius(dp(rDp));
        return g;
    }

    static GradientDrawable round(int fill, float rDp, int stroke, float swDp) {
        GradientDrawable g = round(fill, rDp);
        g.setStroke(Math.max(1, dp(swDp)), stroke);
        return g;
    }

    static GradientDrawable oval(int fill) {
        GradientDrawable g = new GradientDrawable();
        g.setShape(GradientDrawable.OVAL);
        g.setColor(fill);
        return g;
    }

    /** Face over a darker "3D" bottom edge, like Duolingo buttons. */
    static Drawable raised(int face, int edge, float rDp, float depthDp, int stroke) {
        GradientDrawable e = round(edge, rDp), f = stroke == 0 ? round(face, rDp) : round(face, rDp, stroke, 2);
        LayerDrawable ld = new LayerDrawable(new Drawable[]{e, f});
        ld.setLayerInset(1, 0, 0, 0, dp(depthDp));
        return ld;
    }

    static TextView tv(Context c, CharSequence s, float sp, int color, Typeface tf) {
        TextView t = new TextView(c);
        t.setText(s);
        t.setTextSize(sp);
        t.setTextColor(color);
        t.setTypeface(tf);
        return t;
    }

    static TextView one(TextView t) {
        t.setSingleLine(true);
        t.setEllipsize(TextUtils.TruncateAt.END);
        return t;
    }

    static LinearLayout col(Context c) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.VERTICAL);
        return l;
    }

    static LinearLayout row(Context c) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.HORIZONTAL);
        l.setGravity(Gravity.CENTER_VERTICAL);
        return l;
    }

    static LinearLayout.LayoutParams lp(int w, int h) { return new LinearLayout.LayoutParams(w, h); }

    static LinearLayout.LayoutParams lp(int w, int h, float weight) { return new LinearLayout.LayoutParams(w, h, weight); }

    static LinearLayout.LayoutParams lpm(int w, int h, int l, int t, int r, int b) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(w, h);
        p.setMargins(dp(l), dp(t), dp(r), dp(b));
        return p;
    }

    static View space(Context c) { return new View(c); }

    static ImageView icon(Context c, int res, int color) {
        ImageView i = new ImageView(c);
        i.setImageResource(res);
        i.setColorFilter(color);
        return i;
    }

    static <T extends View> T ripple(T v, boolean rounded) {
        Drawable mask = rounded ? round(0xffffffff, 14) : new android.graphics.drawable.ColorDrawable(0xffffffff);
        v.setForeground(new RippleDrawable(ColorStateList.valueOf(alpha(TEXT, 0.10f)), null, mask));
        v.setClickable(true);
        return v;
    }

    /** Round icon button. */
    static ImageView iconBtn(Context c, int res, int color, View.OnClickListener l) {
        ImageView i = icon(c, res, color);
        i.setPadding(dp(9), dp(9), dp(9), dp(9));
        RippleDrawable r = new RippleDrawable(ColorStateList.valueOf(alpha(TEXT, 0.12f)), null, oval(0xffffffff));
        i.setBackground(r);
        i.setOnClickListener(l);
        i.setLayoutParams(lp(dp(44), dp(44)));
        return i;
    }

    /* ---------------- 3D buttons ---------------- */
    static TextView btn(Context c, String label, int style, View.OnClickListener l) {
        TextView b = tv(c, label.toUpperCase(Locale.ROOT), 15, Color.WHITE, BLACK);
        b.setGravity(Gravity.CENTER);
        b.setLetterSpacing(0.05f);
        b.setMinHeight(dp(52));
        b.setOnClickListener(l);
        styleBtn(b, style);
        return b;
    }

    static void styleBtn(TextView b, int style) {
        int face, edge, text;
        switch (style) {
            case B_GHOST: face = SURF2; edge = LINE; text = TEXT; break;
            case B_YELLOW: face = ACCENT; edge = mix(ACCENT, 0xff000000, 0.65f); text = ACCENT_INK; break;
            case B_RED: face = RED; edge = RED_D; text = Color.WHITE; break;
            case B_OFF: face = SURF2; edge = LINE; text = MUTED; break;
            case B_BLUE: face = BLUE; edge = mix(BLUE, 0xff000000, 0.7f); text = Color.WHITE; break;
            default: face = GREEN; edge = GREEN_D; text = Color.WHITE;
        }
        b.setTextColor(text);
        b.setEnabled(style != B_OFF);
        StateListDrawable s = new StateListDrawable();
        LayerDrawable pressed = new LayerDrawable(new Drawable[]{round(face, 16)});
        pressed.setLayerInset(0, 0, dp(4), 0, 0);
        s.addState(new int[]{android.R.attr.state_pressed}, pressed);
        s.addState(new int[]{}, raised(face, edge, 16, 4, 0));
        b.setBackground(s);
        b.setPadding(dp(18), 0, dp(18), dp(4));
    }

    /* ---------------- section header ---------------- */
    static TextView section(Context c, String s) {
        TextView t = tv(c, s.toUpperCase(Locale.ROOT), 13, MUTED, BLACK);
        t.setLetterSpacing(0.08f);
        t.setPadding(0, dp(22), 0, dp(12));
        return t;
    }

    static TextView pageTitle(Context c, String s) {
        TextView t = tv(c, s, 28, TEXT, BLACK);
        t.setPadding(dp(16), dp(14), dp(16), dp(10));
        return t;
    }

    /** Rounded surface box. */
    static LinearLayout box(Context c) {
        LinearLayout b = col(c);
        b.setBackground(round(SURF, 20, LINE, 1));
        b.setPadding(dp(16), dp(16), dp(16), dp(16));
        return b;
    }

    /* ---------------- chips ---------------- */
    static HorizontalScrollView chips(Context c, String first, int sel, IntConsumer pick) {
        HorizontalScrollView hs = new HorizontalScrollView(c);
        hs.setHorizontalScrollBarEnabled(false);
        hs.setOverScrollMode(View.OVER_SCROLL_NEVER);
        LinearLayout r = row(c);
        r.setPadding(dp(16), dp(4), dp(16), dp(10));
        hs.addView(r);
        TextView[] all = new TextView[Data.catName.length + 1];
        for (int i = -1; i < Data.catName.length; i++) {
            final int idx = i;
            TextView t = tv(c, i < 0 ? first : Data.catName[i], 14, MUTED, MEDIUM);
            t.setGravity(Gravity.CENTER);
            t.setPadding(dp(14), 0, dp(14), 0);
            if (i >= 0) {
                GradientDrawable dot = oval(Data.catColor[i]);
                dot.setSize(dp(8), dp(8));
                t.setCompoundDrawablesRelativeWithIntrinsicBounds(dot, null, null, null);
                t.setCompoundDrawablePadding(dp(7));
            }
            all[i + 1] = t;
            t.setOnClickListener(v -> {
                for (int k = 0; k < all.length; k++) chipStyle(all[k], k - 1 == idx);
                int[] loc = new int[2];
                v.getLocationOnScreen(loc);
                hs.smoothScrollBy(loc[0] + v.getWidth() / 2 - hs.getWidth() / 2, 0);
                pick.accept(idx);
            });
            chipStyle(t, i == sel);
            r.addView(t, lpm(W, dp(34), 0, 0, 8, 0));
        }
        return hs;
    }

    private static void chipStyle(TextView t, boolean on) {
        t.setBackground(on ? round(TEXT, 17) : round(0, 17, LINE, 1));
        t.setTextColor(on ? BG : MUTED);
    }

    /* ---------------- segmented control ---------------- */
    static LinearLayout seg(Context c, String[] labels, int sel, IntConsumer pick) {
        LinearLayout r = row(c);
        r.setBackground(round(SURF2, 14));
        r.setPadding(dp(4), dp(4), dp(4), dp(4));
        TextView[] all = new TextView[labels.length];
        for (int i = 0; i < labels.length; i++) {
            final int idx = i;
            TextView t = tv(c, labels[i], 14, MUTED, BOLD);
            t.setGravity(Gravity.CENTER);
            t.setPadding(dp(10), 0, dp(10), 0);
            all[i] = t;
            t.setOnClickListener(v -> {
                for (int k = 0; k < all.length; k++) segStyle(all[k], k == idx);
                pick.accept(idx);
            });
            segStyle(t, i == sel);
            r.addView(t, lp(0, dp(36), 1));
        }
        return r;
    }

    private static void segStyle(TextView t, boolean on) {
        t.setBackground(on ? round(SURF, 10) : null);
        t.setTextColor(on ? TEXT : MUTED);
    }
}
