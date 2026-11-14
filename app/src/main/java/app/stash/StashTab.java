package app.stash;

import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.text.DateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Locale;

import static app.stash.Ui.*;

/** Stash: saved ideas + full reading history grouped by day. */
final class StashTab extends MainActivity.Tab {
    private final Rows rows = new Rows();
    private int seg = 0;
    private FrameLayout segBox;

    StashTab(MainActivity a) { super(a); }

    @Override View build() {
        LinearLayout col = col(a);
        col.addView(pageTitle(a, "Stash"));
        segBox = new FrameLayout(a);
        col.addView(segBox, lpm(M, W, 16, 0, 16, 8));
        RecyclerView rv = new RecyclerView(a);
        rv.setLayoutManager(new LinearLayoutManager(a));
        rv.setAdapter(rows);
        rv.setPadding(dp(16), 0, dp(16), dp(24));
        rv.setClipToPadding(false);
        col.addView(rv, lp(M, 0, 1));
        return col;
    }

    static String dayLabel(int d) {
        int t = Store.today();
        if (d == t) return "Today";
        if (d == t - 1) return "Yesterday";
        return LocalDate.ofEpochDay(d).format(DateTimeFormatter.ofPattern("EEE, MMM d", Locale.getDefault()));
    }

    @Override void refresh() {
        Store S = Store.I;
        ArrayList<Integer> saved = new ArrayList<>(S.saved.keySet()), hist = new ArrayList<>(S.read.keySet());
        Collections.sort(saved, (x, y) -> Long.compare(S.saved.get(y), S.saved.get(x)));
        Collections.sort(hist, (x, y) -> Long.compare(S.read.get(y), S.read.get(x)));
        segBox.removeAllViews();
        segBox.addView(seg(a, new String[]{"Saved · " + saved.size(), "History · " + hist.size()}, seg, i -> { seg = i; refresh(); }));
        rows.clear();
        if (seg == 0) {
            if (saved.isEmpty()) rows.add(Cards.EMPTY, new String[]{"🔖", "Tap “Save” on any idea to keep it here."});
            for (int id : saved) rows.add(Cards.ROW, new Object[]{id, Data.bTitle[Data.book(id)], click("Saved", id)});
        } else {
            if (hist.isEmpty()) rows.add(Cards.EMPTY, new String[]{"🕘", "Ideas you read show up here."});
            DateFormat tf = android.text.format.DateFormat.getTimeFormat(a);
            int last = Integer.MIN_VALUE;
            for (int id : hist) {
                long ts = S.read.get(id);
                int d = Store.dayOf(ts);
                if (d != last) { rows.add(Cards.SECTION, dayLabel(d)); last = d; }
                rows.add(Cards.ROW, new Object[]{id, Data.bTitle[Data.book(id)] + " · " + tf.format(new Date(ts)), click("History", id)});
            }
        }
        rows.done();
    }

    private View.OnClickListener click(String title, int id) {
        ArrayList<Integer> one = new ArrayList<>();
        one.add(id);
        return v -> a.openReader(title, one);
    }
}
