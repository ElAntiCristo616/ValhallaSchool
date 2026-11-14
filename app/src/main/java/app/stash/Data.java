package app.stash;

import android.content.Context;
import android.graphics.Color;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Read-only book content, loaded once from assets. Idea id = bookIndex * PER + ideaIndex. */
final class Data {
    static final int PER = 10;

    static String[] catName;
    static int[] catColor;
    static String[] bTitle, bAuthor;
    static int[] bCat;
    static String[] head, body;
    /** Lower-cased search haystacks. */
    static String[] bSearch, iSearch;
    static int N, NB;
    private static boolean loaded;

    private Data() {}

    static synchronized void load(Context c) {
        if (loaded) return;
        try {
            List<String[]> cats = read(c, "cats.tsv"), books = read(c, "books.tsv"), ideas = read(c, "ideas.tsv");
            catName = new String[cats.size()];
            catColor = new int[cats.size()];
            for (int i = 0; i < cats.size(); i++) {
                catName[i] = cats.get(i)[0];
                catColor[i] = Color.parseColor(cats.get(i)[1]);
            }
            NB = books.size();
            bTitle = new String[NB]; bAuthor = new String[NB]; bCat = new int[NB]; bSearch = new String[NB];
            for (int i = 0; i < NB; i++) {
                String[] r = books.get(i);
                bTitle[i] = r[0]; bAuthor[i] = r[1]; bCat[i] = Integer.parseInt(r[2]);
                bSearch[i] = (r[0] + " " + r[1]).toLowerCase(Locale.ROOT);
            }
            N = ideas.size();
            head = new String[N]; body = new String[N]; iSearch = new String[N];
            for (int i = 0; i < N; i++) {
                String[] r = ideas.get(i);
                head[i] = r[0]; body[i] = r.length > 1 ? r[1] : "";
                iSearch[i] = (head[i] + " " + body[i]).toLowerCase(Locale.ROOT);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Missing content assets", e);
        }
        loaded = true;
    }

    private static List<String[]> read(Context c, String file) throws IOException {
        ArrayList<String[]> out = new ArrayList<>();
        try (BufferedReader r = new BufferedReader(new InputStreamReader(c.getAssets().open(file), StandardCharsets.UTF_8), 1 << 16)) {
            String line;
            while ((line = r.readLine()) != null) if (!line.isEmpty()) out.add(line.split("\t", -1));
        }
        return out;
    }

    static int book(int id) { return id / PER; }
    static int bColor(int b) { return catColor[bCat[b]]; }
    static String cat(int b) { return catName[bCat[b]]; }

    static String initials(int b) {
        String t = bTitle[b].replaceFirst("^(The|A|An) ", "");
        StringBuilder sb = new StringBuilder();
        for (String w : t.split("\\s+")) {
            if (w.isEmpty() || !Character.isLetterOrDigit(w.charAt(0))) continue;
            sb.append(Character.toUpperCase(w.charAt(0)));
            if (sb.length() == 2) break;
        }
        return sb.length() == 0 ? t.substring(0, 1) : sb.toString();
    }
}
