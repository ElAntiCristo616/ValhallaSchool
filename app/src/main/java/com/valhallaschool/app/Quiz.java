package com.valhallaschool.app;

import android.graphics.Typeface;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.text.style.UnderlineSpan;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Generates Duolingo-style questions from the idea data (no network, no AI). */
final class Quiz {
    static final class Q {
        int id, answer;
        String prompt, body, explain;
        CharSequence head;
        String[] opts;
        boolean two;
    }

    static final Random R = new Random();

    /** Permutación barajada de {0,1,2,3} para mostrar 4 opciones en orden
     *  aleatorio (v1.3.4: la correcta ya no es siempre la primera). El tag de
     *  cada opción sigue siendo su índice ORIGINAL, así la corrección
     *  (comparar el tag contra `answer`) no cambia. */
    static int[] shuffled4() {
        int[] o = {0, 1, 2, 3};
        for (int i = o.length - 1; i > 0; i--) {
            int j = R.nextInt(i + 1);
            int t = o[i]; o[i] = o[j]; o[j] = t;
        }
        return o;
    }
    private static final Pattern WORD = Pattern.compile("[A-Za-z][A-Za-z'-]{3,}");
    private static final Set<String> STOP = new HashSet<>(Arrays.asList(("that this with your from into than them they their what when will have more "
            + "less over only just about make makes does dont doesnt every other others before after where which while being been very much most some").split(" ")));
    private static ArrayList<String> words;

    private Quiz() {}

    private static List<String> wordPool() {
        if (words != null) return words;
        LinkedHashSet<String> s = new LinkedHashSet<>();
        for (String h : Data.head) {
            Matcher m = WORD.matcher(h);
            while (m.find()) {
                String w = m.group().toLowerCase(Locale.ROOT);
                if (!STOP.contains(w)) s.add(w);
            }
        }
        return words = new ArrayList<>(s);
    }

    static <T> List<T> sample(List<T> a, int n) {
        ArrayList<T> c = new ArrayList<>(a);
        Collections.shuffle(c, R);
        return new ArrayList<>(c.subList(0, Math.min(n, c.size())));
    }

    private static List<Integer> others(int id, int n) {
        ArrayList<Integer> l = new ArrayList<>();
        int b = Data.book(id);
        for (int i = b * Data.PER; i < b * Data.PER + Data.PER; i++) if (i != id) l.add(i);
        return sample(l, n);
    }

    static Q headline(int id) {
        ArrayList<Integer> o = new ArrayList<>(others(id, 3));
        o.add(id);
        Collections.shuffle(o, R);
        Q q = new Q();
        q.id = id;
        q.prompt = "¿Qué idea describe esto?";
        q.body = Data.body[id];
        q.opts = new String[o.size()];
        for (int i = 0; i < o.size(); i++) q.opts[i] = Data.head[o.get(i)];
        q.answer = o.indexOf(id);
        return q;
    }

    static Q book(int id) {
        int b = Data.book(id), c = Data.bCat[b];
        ArrayList<Integer> pool = new ArrayList<>(), rest = new ArrayList<>();
        for (int x = 0; x < Data.NB; x++) if (x != b) (Data.bCat[x] == c ? pool : rest).add(x);
        ArrayList<Integer> o = new ArrayList<>(sample(pool, 3));
        if (o.size() < 3) o.addAll(sample(rest, 3 - o.size()));
        o.add(b);
        Collections.shuffle(o, R);
        Q q = new Q();
        q.id = id;
        q.prompt = "¿De qué lección es esta idea?";
        q.head = Data.head[id];
        q.body = Data.body[id];
        q.opts = new String[o.size()];
        for (int i = 0; i < o.size(); i++) q.opts[i] = Data.bTitle[o.get(i)];
        q.answer = o.indexOf(b);
        return q;
    }

    static Q trueFalse(int id) {
        boolean truth = R.nextBoolean();
        int other = truth ? id : others(id, 1).get(0);
        Q q = new Q();
        q.id = id;
        q.two = true;
        q.prompt = "¿Esta explicación corresponde a la idea?";
        q.head = Data.head[id];
        q.body = Data.body[other];
        q.opts = new String[]{"Verdadero", "Falso"};
        q.answer = truth ? 0 : 1;
        if (!truth) q.explain = "“" + Data.head[id] + "” significa: " + Data.body[id];
        return q;
    }

    static Q fill(int id) {
        String hd = Data.head[id];
        ArrayList<String> cands = new ArrayList<>();
        Matcher m = WORD.matcher(hd);
        while (m.find()) if (!STOP.contains(m.group().toLowerCase(Locale.ROOT))) cands.add(m.group());
        if (cands.isEmpty()) return headline(id);
        Collections.sort(cands, (a, b) -> b.length() - a.length());
        String w = R.nextFloat() < 0.6f ? cands.get(0) : cands.get(R.nextInt(cands.size()));
        String lw = w.toLowerCase(Locale.ROOT);
        ArrayList<String> pool = new ArrayList<>();
        for (String x : wordPool()) if (!x.equals(lw) && Math.abs(x.length() - lw.length()) <= 3) pool.add(x);
        ArrayList<String> o = new ArrayList<>(sample(pool, 3));
        o.add(lw);
        Collections.shuffle(o, R);
        int at = hd.indexOf(w);
        SpannableStringBuilder sb = new SpannableStringBuilder(hd.substring(0, at));
        int s = sb.length();
        sb.append("  ?  ");
        sb.setSpan(new ForegroundColorSpan(Ui.ACCENT), s, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        sb.setSpan(new UnderlineSpan(), s, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        sb.setSpan(new StyleSpan(Typeface.BOLD), s, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        sb.append(hd.substring(at + w.length()));
        Q q = new Q();
        q.id = id;
        q.prompt = "Completa la palabra que falta";
        q.head = sb;
        q.body = Data.body[id];
        q.opts = o.toArray(new String[0]);
        q.answer = o.indexOf(lw);
        return q;
    }

    /** One question per id, never the same type twice in a row. */
    static List<Q> make(List<Integer> ids, boolean withBook) {
        int n = withBook ? 4 : 3, last = -1;
        ArrayList<Q> out = new ArrayList<>();
        for (int id : ids) {
            int g;
            do g = R.nextInt(n); while (g == last);
            last = g;
            out.add(g == 0 ? headline(id) : g == 1 ? trueFalse(id) : g == 2 ? fill(id) : book(id));
        }
        return out;
    }
}
