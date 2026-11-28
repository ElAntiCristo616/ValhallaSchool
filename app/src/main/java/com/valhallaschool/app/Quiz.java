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

/** Generates Duolingo-style questions from the idea data (no network, no AI).
 *  v1.3.6 fase 2: ya no hay preguntas de emparejar título con cuerpo
 *  (headline/trueFalse eliminados); las lecciones mezclan el banco nuevo
 *  (escenarios, parecidos, detección de errores) con fill y book. */
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

    static Q fill(int id) {
        String hd = Data.head[id];
        ArrayList<String> cands = new ArrayList<>();
        Matcher m = WORD.matcher(hd);
        while (m.find()) if (!STOP.contains(m.group().toLowerCase(Locale.ROOT))) cands.add(m.group());
        if (cands.isEmpty()) return book(id);
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

    /** Una pregunta del banco nuevo para `id` (escenario, parecido o error).
     *  Con nivel L se prefiere la fila con diff más cercano a L sin pasarse
     *  (diff<=L); si no hay, la de diff menor. Si se pasa un formato a evitar,
     *  se prefiere otro formato cuando el banco lo permite. Las opciones se
     *  barajan en tiempo de ejecución y la respuesta se remapea. Si el banco
     *  no está cargado o no tiene filas para la idea, cae a fill. */
    static Q newQ(int id, String avoidFormat) {
        List<NewBank.Row> rows = NewBank.rowsFor(id);
        if (rows == null || rows.isEmpty()) return fill(id);
        ArrayList<NewBank.Row> pool = new ArrayList<>();
        for (NewBank.Row r : rows) if (avoidFormat == null || !r.format.equals(avoidFormat)) pool.add(r);
        if (pool.isEmpty()) pool.addAll(rows);
        int L = diff(), best = -1;
        for (NewBank.Row r : pool) if (r.diff <= L && (best < 0 || r.diff > best)) best = r.diff;
        if (best < 0) {
            best = Integer.MAX_VALUE;
            for (NewBank.Row r : pool) best = Math.min(best, r.diff);
        }
        ArrayList<NewBank.Row> cands = new ArrayList<>();
        for (NewBank.Row r : pool) if (r.diff == best) cands.add(r);
        NewBank.Row row = cands.get(R.nextInt(cands.size()));
        int[] perm = shuffled4();
        Q q = new Q();
        q.id = id;
        q.prompt = row.prompt;
        q.opts = new String[4];
        for (int i = 0; i < 4; i++) q.opts[i] = row.opts[perm[i]];
        q.answer = 0;
        for (int i = 0; i < 4; i++) if (perm[i] == row.answer) q.answer = i;
        q.explain = row.expl;
        return q;
    }

    static Q newQ(int id) { return newQ(id, null); }

    /** Elige el tipo de pregunta: 0 nuevo, 1 fill, 2 book. Pesos base
     *  62/28/10; nunca se repite el mismo tipo dos veces seguidas (los pesos
     *  se renormalizan entre los otros dos). */
    private static int pickKind(int lastKind, boolean withBook) {
        int r = R.nextInt(100);
        if (lastKind < 0) return r < 62 ? 0 : r < 90 ? 1 : (withBook ? 2 : 0);
        if (lastKind == 0) { // quedan fill 28 / book 10
            if (!withBook) return 1;
            return r < 74 ? 1 : 2;
        }
        if (lastKind == 1) { // quedan nuevo 62 / book 10
            if (!withBook) return 0;
            return r < 86 ? 0 : 2;
        }
        // lastKind == 2: quedan nuevo 62 / fill 28
        return r < 69 ? 0 : 1;
    }

    /** One question per id: banco nuevo + fill + book, never the same kind
     *  twice in a row, and never the same new sub-format twice in a row when
     *  the bank allows it. Las preguntas nuevas son prompt-only (head y body
     *  nulos): la pantalla salta la tarjeta de contexto vacía. */
    static List<Q> make(List<Integer> ids, boolean withBook) {
        ArrayList<Q> out = new ArrayList<>();
        int lastKind = -1;
        String lastSub = null;
        for (int id : ids) {
            int kind = pickKind(lastKind, withBook);
            Q q;
            if (kind == 0) {
                q = newQ(id, lastSub);
                List<NewBank.Row> rows = NewBank.rowsFor(id);
                if (rows != null && !rows.isEmpty()) {
                    // el sub-formato usado es el de la fila elegida: se
                    // recupera por el prompt (único por fila en el banco).
                    for (NewBank.Row r : rows) if (r.prompt.equals(q.prompt)) { lastSub = r.format; break; }
                }
            } else if (kind == 1) {
                q = fill(id);
                lastSub = null;
            } else {
                q = book(id);
                lastSub = null;
            }
            lastKind = kind;
            out.add(q);
        }
        return out;
    }
}
