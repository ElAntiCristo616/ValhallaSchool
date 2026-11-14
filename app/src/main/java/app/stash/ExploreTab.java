package app.stash;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.LinearLayout;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static app.stash.Ui.*;

/** Explore: book library grid with progress, topic filter and instant search over books + ideas. */
final class ExploreTab extends MainActivity.Tab {
    private final Rows rows = new Rows();
    private int cat = -1;
    private String q = "";

    ExploreTab(MainActivity a) { super(a); }

    @Override View build() {
        LinearLayout col = col(a);
        col.addView(pageTitle(a, "Explore"));
        LinearLayout sr = row(a);
        sr.setBackground(round(SURF2, 14));
        sr.setPadding(dp(14), 0, dp(8), 0);
        sr.addView(icon(a, R.drawable.ic_search, MUTED), lp(dp(20), dp(20)));
        EditText e = new EditText(a);
        e.setBackground(null);
        e.setHint("Search books & ideas");
        e.setHintTextColor(MUTED);
        e.setTextColor(TEXT);
        e.setTextSize(16);
        e.setSingleLine(true);
        e.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
        e.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int af) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {}
            @Override public void afterTextChanged(Editable s) {
                q = s.toString().trim().toLowerCase(Locale.ROOT);
                refresh();
            }
        });
        e.setOnEditorActionListener((v, id, ev) -> { a.hideKeyboard(); return true; });
        LinearLayout.LayoutParams ep = lpm(0, M, 10, 0, 0, 0);
        ep.weight = 1;
        sr.addView(e, ep);
        col.addView(sr, lpm(M, dp(46), 16, 0, 16, 10));
        col.addView(chips(a, "All", -1, i -> { cat = i; refresh(); }), lp(M, W));
        RecyclerView rv = new RecyclerView(a);
        rv.setLayoutManager(new LinearLayoutManager(a));
        rv.setAdapter(rows);
        rv.setPadding(dp(16), 0, dp(16), dp(24));
        rv.setClipToPadding(false);
        rv.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override public void onScrollStateChanged(RecyclerView r, int st) {
                if (st == RecyclerView.SCROLL_STATE_DRAGGING) a.hideKeyboard();
            }
        });
        col.addView(rv, lp(M, 0, 1));
        return col;
    }

    private boolean inCat(int b) { return cat < 0 || Data.bCat[b] == cat; }

    @Override void refresh() {
        Store S = Store.I;
        rows.clear();
        if (q.length() >= 2) {
            ArrayList<Integer> bs = new ArrayList<>(), is = new ArrayList<>();
            for (int b = 0; b < Data.NB; b++) if (inCat(b) && Data.bSearch[b].contains(q)) bs.add(b);
            for (int i = 0; i < Data.N && is.size() < 60; i++) if (inCat(Data.book(i)) && Data.iSearch[i].contains(q)) is.add(i);
            if (!bs.isEmpty()) { rows.add(Cards.SECTION, "Books"); addBooks(bs); }
            if (!is.isEmpty()) {
                rows.add(Cards.SECTION, "Ideas");
                for (int id : is) {
                    View.OnClickListener click = v -> a.openReader(Data.bTitle[Data.book(id)], MainActivity.from(id));
                    rows.add(Cards.ROW, new Object[]{id, Data.bTitle[Data.book(id)], click});
                }
            }
            if (bs.isEmpty() && is.isEmpty()) rows.add(Cards.EMPTY, new String[]{"🔍", "Nothing found for “" + q + "”"});
        } else {
            ArrayList<Integer> bs = new ArrayList<>(), going = new ArrayList<>();
            for (int b = 0; b < Data.NB; b++) {
                if (!inCat(b)) continue;
                bs.add(b);
                int n = S.bookRead(b);
                if (n > 0 && n < Data.PER) going.add(b);
            }
            if (!going.isEmpty() && cat < 0) {
                rows.add(Cards.SECTION, "Continue reading");
                addBooks(going.subList(0, Math.min(6, going.size())));
            }
            rows.add(Cards.SECTION, (cat < 0 ? "All books" : Data.catName[cat]) + " · " + bs.size());
            addBooks(bs);
        }
        rows.done();
    }

    private void addBooks(List<Integer> bs) {
        for (int i = 0; i < bs.size(); i += 3) {
            int[] r = new int[Math.min(3, bs.size() - i)];
            for (int k = 0; k < r.length; k++) r[k] = bs.get(i + k);
            rows.add(Cards.BOOKROW, r);
        }
    }
}
