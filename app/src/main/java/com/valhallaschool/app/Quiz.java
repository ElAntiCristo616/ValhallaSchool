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
        return shuffled(4);
    }

    /** Permutación barajada de {0..n-1} (v1.3.5: Lluvia tiene 2 o 4 opciones). */
    static int[] shuffled(int n) {
        int[] o = new int[n];
        for (int i = 0; i < n; i++) o[i] = i;
        for (int i = o.length - 1; i > 0; i--) {
            int j = R.nextInt(i + 1);
            int t = o[i]; o[i] = o[j]; o[j] = t;
        }
        return o;
    }

    /** Palabras de contenido (español) para medir parecido entre ideas. */
    private static final Set<String> STOP_ES = new HashSet<>(Arrays.asList(("que de la el en y los las un una con por para se su es del al como mas pero sus este esta son o a lo no si ya le les mi tu nos te me yo el ella esto eso hay fue han ser sin sobre entre cuando cada tambien muy todo todos asi porque que cual quien donde hasta desde puede pueden tiene tienen esta estan era eran somos".split(" "))));

    private static Set<String> wordsOf(String s) {
        Set<String> out = new HashSet<>();
        for (String w : s.toLowerCase(Locale.ROOT).split("[^a-záéíóúñü]+")) {
            if (w.length() >= 3 && !STOP_ES.contains(w)) out.add(w);
        }
        return out;
    }

    private static int overlap(Set<String> base, int other) {
        int n = 0;
        for (String w : wordsOf(Data.body[other] + " " + Data.head[other])) if (base.contains(w)) n++;
        return n;
    }

    /** Las ideas de la misma lección más parecidas a `id` (por palabras en
     *  común): los distractores ya no son al azar, sino los vecinos que de
     *  verdad se confunden con la correcta (v1.3.5). */
    private static List<Integer> nearest(int id, int n) {
        int b = Data.book(id);
        Set<String> base = wordsOf(Data.body[id] + " " + Data.head[id]);
        ArrayList<Integer> cands = new ArrayList<>();
        for (int i = b * Data.PER; i < b * Data.PER + Data.PER; i++) if (i != id) cands.add(i);
        Collections.shuffle(cands, R);
        cands.sort((x, y) -> overlap(base, y) - overlap(base, x));
        return new ArrayList<>(cands.subList(0, Math.min(n, cands.size())));
    }

    /** v1.3.6: las ideas más parecidas a `id` de TODO el libro (470 ideas),
     *  no solo de la misma lección: para Experto e Imposible. */
    private static List<Integer> nearestAll(int id, int n) {
        Set<String> base = wordsOf(Data.body[id] + " " + Data.head[id]);
        ArrayList<Integer> cands = new ArrayList<>();
        for (int i = 0; i < Data.N; i++) if (i != id) cands.add(i);
        Collections.shuffle(cands, R);
        cands.sort((x, y) -> overlap(base, y) - overlap(base, x));
        return new ArrayList<>(cands.subList(0, Math.min(n, cands.size())));
    }

    /** Nivel global de dificultad (v1.3.6): en tests unitarios sin Android,
     *  Store puede no estar inicializado: se protege con try/catch. */
    private static int diff() {
        try { return Store.I == null ? 0 : Store.I.difficulty; } catch (Exception e) { return 0; }
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

    static Q headline(int id) {
        // v1.3.5: distractores = las 3 ideas más parecidas de la misma lección
        // (mismo tema, palabras en común), no 3 al azar que se descartan solas.
        // v1.3.6: con dificultad 2-3, las 3 más parecidas de TODO el libro.
        ArrayList<Integer> o = new ArrayList<>(diff() >= 2 ? nearestAll(id, 3) : nearest(id, 3));
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
        // v1.3.5: la explicación falsa es la más parecida de la lección, no
        // una al azar: hay que leerla de verdad para notar que no corresponde.
        // v1.3.6: con dificultad 3, la falsa es la más parecida de TODO el libro.
        int other = truth ? id : (diff() >= 3 ? nearestAll(id, 1).get(0) : nearest(id, 1).get(0));
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
        // v1.3.5: la palabra que falta se elige al azar entre las candidatas
        // (antes, el 60% de las veces era la MÁS LARGA del titular: se delataba).
        String w = cands.get(R.nextInt(cands.size()));
        String lw = w.toLowerCase(Locale.ROOT);
        // v1.3.5: distractores primero de la misma lección (palabras del mismo
        // tema, largo parecido); si no alcanzan, del resto del libro.
        // v1.3.6: el tope de largo es 3 (Normal/Difícil), 2 (Experto) o 1
        // (Imposible); en Imposible el pool es primero solo de la misma
        // lección y solo se amplía al resto del libro si no hay 3, y si
        // aun así no hay, se relaja a 3 como siempre.
        int cap = diff() >= 3 ? 1 : diff() == 2 ? 2 : 3;
        ArrayList<String> pool = new ArrayList<>();
        int b = Data.book(id);
        for (int i = b * Data.PER; i < b * Data.PER + Data.PER && i < Data.N; i++) {
            if (i == id) continue;
            Matcher mm = WORD.matcher(Data.head[i]);
            while (mm.find()) {
                String x = mm.group().toLowerCase(Locale.ROOT);
                if (!x.equals(lw) && !STOP.contains(x) && Math.abs(x.length() - lw.length()) <= cap && !pool.contains(x)) pool.add(x);
            }
        }
        if (pool.size() < 3) {
            for (String x : wordPool()) if (!x.equals(lw) && Math.abs(x.length() - lw.length()) <= cap && !pool.contains(x)) pool.add(x);
        }
        if (pool.size() < 3) {
            for (String x : wordPool()) if (!x.equals(lw) && Math.abs(x.length() - lw.length()) <= 3 && !pool.contains(x)) pool.add(x);
        }
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
        // v1.3.5: si el cuerpo muestra la palabra que falta, también se tapa
        // ahí (antes el cuerpo regalaba la respuesta).
        String body = Data.body[id];
        if (Pattern.compile("\\b" + Pattern.quote(w) + "\\b", Pattern.CASE_INSENSITIVE).matcher(body).find()) {
            body = Pattern.compile("\\b" + Pattern.quote(w) + "\\b", Pattern.CASE_INSENSITIVE).matcher(body).replaceAll("___");
        }
        q.body = body;
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
