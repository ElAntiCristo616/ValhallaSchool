package app.stash;

import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;

import static app.stash.Ui.*;

/**
 * Duolingo-style session: optional "new idea" cards, then questions. Wrong answers are re-queued at the end;
 * progress only advances on correct answers. Grades feed spaced repetition.
 */
final class SessionScreen extends MainActivity.Screen {
    private static final int LEARN = 0, ASK = 1, FEEDBACK = 2, END = 3;
    private static final String[] PRAISE = {"Nice!", "Correct!", "Great job!", "Awesome!", "Spot on!"};

    private final ArrayDeque<Object> queue = new ArrayDeque<>();
    private final int total, bonus;
    private final Runnable onDone;
    private final HashSet<Integer> missed = new HashSet<>(), graded = new HashSet<>();
    private final long t0 = System.currentTimeMillis();
    private int done, combo, firstOk, answered, xpRead, sel = -1, phase;
    private Object item;
    private TextView[] opts;

    private final Views.Bar bar;
    private final TextView comboTx, fbTitle, fbText, btn;
    private final LinearLayout main, foot;
    private final View divider;
    private final ScrollView sc;

    SessionScreen(MainActivity a, List<Integer> learn, List<Quiz.Q> qs, int bonus, Runnable onDone) {
        super(a);
        up = true;
        this.bonus = bonus;
        this.onDone = onDone;
        queue.addAll(learn);
        queue.addAll(qs);
        total = queue.size();

        LinearLayout col = col(a);
        LinearLayout top = row(a);
        top.setPadding(dp(6), dp(8), dp(14), dp(6));
        top.addView(iconBtn(a, R.drawable.ic_close, MUTED, x -> close()));
        bar = new Views.Bar(a);
        LinearLayout.LayoutParams bl = lpm(0, dp(14), 6, 0, 10, 0);
        bl.weight = 1;
        top.addView(bar, bl);
        comboTx = tv(a, "", 14, ACCENT, BLACK);
        comboTx.setGravity(Gravity.END);
        top.addView(comboTx, lp(dp(52), W));
        col.addView(top, lp(M, W));

        sc = new ScrollView(a);
        main = col(a);
        main.setPadding(dp(18), dp(12), dp(18), dp(20));
        sc.addView(main);
        col.addView(sc, lp(M, 0, 1));

        divider = new View(a);
        divider.setBackgroundColor(LINE);
        col.addView(divider, lp(M, dp(2)));
        foot = col(a);
        foot.setPadding(dp(18), dp(16), dp(18), dp(18));
        fbTitle = tv(a, "", 20, GREEN, BLACK);
        fbText = tv(a, "", 15, TEXT, REG);
        fbText.setLineSpacing(0, 1.25f);
        foot.addView(fbTitle);
        foot.addView(fbText, lpm(M, W, 0, 4, 0, 0));
        btn = Ui.btn(a, "Continue", B_GREEN, x -> onBtn());
        foot.addView(btn, lpm(M, W, 0, 14, 0, 0));
        col.addView(foot, lp(M, W));
        v = col;
        next();
    }

    /* ---------------- starters ---------------- */
    static void lesson(MainActivity a, int b, int k) {
        ArrayList<Integer> ids = new ArrayList<>();
        for (int i = 0; i < 5; i++) ids.add(b * Data.PER + k * 5 + i);
        ArrayList<Integer> shuffled = new ArrayList<>(ids);
        Collections.shuffle(shuffled, Quiz.R);
        a.push(new SessionScreen(a, ids, Quiz.make(shuffled, false), Store.XP_LESSON, () -> finishLesson(a, b + "." + k)));
    }

    static void bookQuiz(MainActivity a, int b) {
        ArrayList<Integer> ids = new ArrayList<>();
        for (int i = b * Data.PER; i < b * Data.PER + Data.PER; i++) ids.add(i);
        a.push(new SessionScreen(a, new ArrayList<>(), Quiz.make(Quiz.sample(ids, 8), false), Store.XP_LESSON, () -> finishLesson(a, b + ".2")));
    }

    static void review(MainActivity a) {
        Store S = Store.I;
        List<Integer> due = S.due();
        ArrayList<Integer> ids = new ArrayList<>(due.subList(0, Math.min(10, due.size())));
        if (ids.size() < 5) {
            ArrayList<Integer> rest = new ArrayList<>();
            for (int id : S.read.keySet()) if (!ids.contains(id)) rest.add(id);
            ids.addAll(Quiz.sample(rest, 5 - ids.size()));
        }
        if (ids.isEmpty()) { a.toast("Read a few ideas first", false); return; }
        Collections.shuffle(ids, Quiz.R);
        a.push(new SessionScreen(a, new ArrayList<>(), Quiz.make(ids, true), 0, a::dirty));
    }

    private static void finishLesson(MainActivity a, String key) {
        Store S = Store.I;
        S.lessons.put(key, System.currentTimeMillis());
        S.save();
        S.checkAch();
        a.dirty();
    }

    /* ---------------- flow ---------------- */
    private void close() {
        if (phase == END) { onBtn(); return; }
        a.ask("Quit this session?", "Answers so far still count toward your reviews, but you will lose the lesson XP.", "Quit", a::back, true);
    }

    @Override boolean onBack() { close(); return true; }

    private void updateBar() {
        bar.set(done / (float) total, GREEN);
        comboTx.setText(combo >= 2 ? combo + "×🔥" : "");
    }

    private void setFoot(int state) {
        foot.setBackgroundColor(state == 1 ? mix(GREEN, BG, 0.16f) : state == 2 ? mix(RED, BG, 0.14f) : BG);
        divider.setVisibility(state == 0 ? View.VISIBLE : View.INVISIBLE);
        fbTitle.setVisibility(state == 0 ? View.GONE : View.VISIBLE);
        fbText.setVisibility(state == 2 ? View.VISIBLE : View.GONE);
    }

    private void setBtn(String label, int style) {
        btn.setText(label.toUpperCase(Locale.ROOT));
        styleBtn(btn, style);
    }

    private void next() {
        updateBar();
        setFoot(0);
        sc.scrollTo(0, 0);
        main.removeAllViews();
        item = queue.poll();
        if (item == null) { finish(); return; }
        if (item instanceof Integer) {
            phase = LEARN;
            int id = (Integer) item;
            if (Store.I.markRead(id, true)) xpRead += Store.XP_READ;
            TextView tag = tv(a, "NEW IDEA", 13, BLUE, BLACK);
            tag.setLetterSpacing(0.08f);
            main.addView(tag, lpm(W, W, 0, 0, 0, 10));
            Cards.Idea c = Cards.idea(a, false, false);
            Cards.bind(c, id);
            c.card.setMinimumHeight(dp(380));
            main.addView(c.root, lp(M, W));
            setBtn("Continue", B_GREEN);
            return;
        }
        phase = ASK;
        sel = -1;
        Quiz.Q q = (Quiz.Q) item;
        main.addView(tv(a, q.prompt, 22, TEXT, BLACK), lpm(M, W, 0, 6, 0, 16));
        LinearLayout ctx = col(a);
        ctx.setBackground(round(SURF, 18, LINE, 2));
        ctx.setPadding(dp(16), dp(16), dp(16), dp(16));
        if (q.head != null) {
            TextView h = tv(a, q.head, 19, TEXT, BOLD);
            h.setLineSpacing(0, 1.15f);
            ctx.addView(h, lpm(M, W, 0, 0, 0, q.body != null ? 8 : 0));
        }
        if (q.body != null) {
            TextView bd = tv(a, q.body, 17, mix(TEXT, BG, 0.85f), REG);
            bd.setLineSpacing(0, 1.3f);
            ctx.addView(bd);
        }
        main.addView(ctx, lpm(M, W, 0, 0, 0, 18));
        LinearLayout ol = q.two ? row(a) : col(a);
        opts = new TextView[q.opts.length];
        for (int i = 0; i < q.opts.length; i++) {
            final int idx = i;
            TextView o = tv(a, q.opts[i], q.two ? 18 : 16, TEXT, MEDIUM);
            o.setPadding(dp(16), dp(14), dp(16), dp(17));
            if (q.two) o.setGravity(Gravity.CENTER);
            optStyle(o, 0);
            o.setOnClickListener(x -> {
                if (phase != ASK) return;
                sel = idx;
                for (int k = 0; k < opts.length; k++) optStyle(opts[k], k == idx ? 1 : 0);
                setBtn("Check", B_GREEN);
            });
            opts[i] = o;
            if (q.two) {
                LinearLayout.LayoutParams p = lpm(0, W, i == 0 ? 0 : 6, 0, i == 0 ? 6 : 0, 0);
                p.weight = 1;
                ol.addView(o, p);
            } else ol.addView(o, lpm(M, W, 0, 0, 0, 10));
        }
        main.addView(ol, lp(M, W));
        setBtn("Check", B_OFF);
    }

    /** 0 idle, 1 selected, 2 correct, 3 wrong. */
    private static void optStyle(TextView o, int s) {
        int border = s == 1 ? BLUE : s == 2 ? GREEN : s == 3 ? RED : LINE;
        int fill = s == 1 ? mix(BLUE, SURF, 0.12f) : s == 2 ? mix(GREEN, SURF, 0.14f) : s == 3 ? mix(RED, SURF, 0.12f) : SURF;
        o.setBackground(raised(fill, border, 16, 3, border));
    }

    private void onBtn() {
        switch (phase) {
            case LEARN: done++; next(); break;
            case ASK: if (sel >= 0) check(); break;
            case FEEDBACK: next(); break;
            case END:
                if (onDone != null) onDone.run();   // before back() so the tab underneath refreshes
                a.back();
                break;
            default: break;
        }
    }

    private void check() {
        Store S = Store.I;
        Quiz.Q q = (Quiz.Q) item;
        boolean ok = sel == q.answer;
        for (int k = 0; k < opts.length; k++) optStyle(opts[k], k == q.answer ? 2 : (k == sel && !ok) ? 3 : 0);
        if (graded.add(q.id)) {
            S.srsGrade(q.id, ok);
            answered++;
            S.qa++;
            if (ok) { S.qc++; firstOk++; }
        }
        if (ok) {
            combo++;
            done++;
            Sfx.play(Sfx.OK);
            fbTitle.setText(PRAISE[Quiz.R.nextInt(PRAISE.length)]);
            fbTitle.setTextColor(GREEN);
            setFoot(1);
            setBtn("Continue", B_GREEN);
        } else {
            combo = 0;
            missed.add(q.id);
            queue.add(q);
            Sfx.play(Sfx.BAD);
            v.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
            fbTitle.setText("Not quite");
            fbTitle.setTextColor(RED);
            fbText.setText("Correct answer: " + q.opts[q.answer] + (q.explain != null ? "\n\n" + q.explain : ""));
            setFoot(2);
            setBtn("Continue", B_RED);
        }
        phase = FEEDBACK;
        updateBar();
        S.save();
    }

    private void finish() {
        Store S = Store.I;
        phase = END;
        boolean perfect = answered > 0 && missed.isEmpty();
        int xp = firstOk * Store.XP_Q + bonus + (perfect ? Store.XP_PERFECT : 0);
        if (perfect) S.perfect++;
        long secs = (System.currentTimeMillis() - t0) / 1000;
        int acc = answered > 0 ? Math.round(firstOk * 100f / answered) : 100;

        LinearLayout fin = col(a);
        fin.setGravity(Gravity.CENTER_HORIZONTAL);
        fin.setPadding(0, dp(48), 0, 0);
        fin.addView(tv(a, perfect ? "💎" : "🏆", 84, TEXT, REG));
        fin.addView(tv(a, perfect ? "Perfect!" : "Lesson complete!", 28, ACCENT, BLACK), lpm(W, W, 0, 10, 0, 0));
        TextView p = tv(a, missed.isEmpty() ? "Every answer right on the first try." :
                missed.size() + (missed.size() == 1 ? " idea" : " ideas") + " will come back in your reviews.", 15, MUTED, REG);
        p.setGravity(Gravity.CENTER);
        fin.addView(p, lpm(W, W, 0, 10, 0, 24));
        LinearLayout tiles = row(a);
        tiles.addView(tile("TOTAL XP", String.valueOf(xp + xpRead), ACCENT), lp(0, W, 1));
        LinearLayout.LayoutParams p2 = lpm(0, W, 10, 0, 0, 0), p3 = lpm(0, W, 10, 0, 0, 0);
        p2.weight = 1;
        p3.weight = 1;
        tiles.addView(tile("ACCURACY", acc + "%", GREEN), p2);
        tiles.addView(tile("TIME", (secs / 60) + ":" + String.format(Locale.ROOT, "%02d", secs % 60), BLUE), p3);
        fin.addView(tiles, lp(M, W));
        main.addView(fin, lp(M, W));

        setFoot(0);
        setBtn("Continue", B_GREEN);
        updateBar();
        Sfx.play(Sfx.WIN);
        a.confetti();
        if (xp > 0) S.addXP(xp, true); else S.save();
        if (xp + xpRead > 0) a.toast("+" + (xp + xpRead) + " XP", true);
    }

    private LinearLayout tile(String label, String value, int color) {
        LinearLayout t = col(a);
        t.setBackground(round(SURF, 16, color, 2));
        TextView l = tv(a, label, 11, 0xff111111, BLACK);
        l.setGravity(Gravity.CENTER);
        l.setLetterSpacing(0.06f);
        l.setPadding(0, dp(5), 0, dp(5));
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        float r = dp(14);
        g.setCornerRadii(new float[]{r, r, r, r, 0, 0, 0, 0});
        l.setBackground(g);
        t.addView(l, lp(M, W));
        TextView v = tv(a, value, 20, TEXT, BLACK);
        v.setGravity(Gravity.CENTER);
        v.setPadding(0, dp(12), 0, dp(12));
        t.addView(v, lp(M, W));
        return t;
    }
}
