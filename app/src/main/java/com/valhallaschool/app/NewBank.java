package com.valhallaschool.app;

import android.content.Context;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/** Banco nuevo de preguntas de lección (v1.3.6 fase 2): escenarios, parecidos
 *  y detección de errores. Una fila cubre una idea (ideaId 0..469). Cargado
 *  una vez desde assets/quiz_new.tsv, junto a Data. */
final class NewBank {
    static final class Row {
        String format;
        int diff;
        String prompt;
        String[] opts = new String[4];
        int answer;
        String expl;
    }

    private static final ArrayList<List<Row>> byIdea = new ArrayList<>();
    private static boolean loaded;

    private NewBank() {}

    static synchronized void load(Context c) {
        if (loaded) return;
        try (BufferedReader r = new BufferedReader(new InputStreamReader(c.getAssets().open("quiz_new.tsv"), StandardCharsets.UTF_8), 1 << 16)) {
            String line;
            while ((line = r.readLine()) != null) {
                String[] p = line.split("\t", -1);
                if (p.length < 10) continue;
                int id;
                try { id = Integer.parseInt(p[0]); } catch (NumberFormatException e) { continue; } // cabecera
                Row row = new Row();
                row.format = p[1];
                try { row.diff = Integer.parseInt(p[2]); } catch (NumberFormatException e) { row.diff = 0; }
                row.prompt = p[3];
                for (int i = 0; i < 4; i++) row.opts[i] = p[4 + i];
                try { row.answer = Integer.parseInt(p[8]); } catch (NumberFormatException e) { row.answer = 0; }
                if (row.answer < 0 || row.answer > 3) row.answer = 0;
                row.expl = p[9];
                while (byIdea.size() <= id) byIdea.add(new ArrayList<Row>());
                byIdea.get(id).add(row);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Missing quiz_new.tsv", e);
        }
        loaded = true;
    }

    static List<Row> rowsFor(int id) {
        if (id < 0 || id >= byIdea.size()) return null;
        return byIdea.get(id);
    }

    static boolean isLoaded() { return loaded; }
}
