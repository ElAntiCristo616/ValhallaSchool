package app.stash;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.PagerSnapHelper;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import static app.stash.Ui.*;

/**
 * Full-screen vertical idea pager (Deepstash/TikTok style). A card counts as read once it stays
 * snapped on screen for READ_MS; flicking past quickly does not count.
 */
@SuppressLint("ViewConstructor")
final class Feed extends RecyclerView {
    interface Source { List<Integer> next(int n); }

    static final long READ_MS = 1200;
    private static final int BATCH = 8;

    private final Source src;
    private final String[] end; // emoji, title, text, button label
    private final Runnable endAction;
    final ArrayList<Integer> ids = new ArrayList<>();
    private boolean ended;
    private int cur = -1;
    private Runnable pending;
    private final LinearLayoutManager lm;

    Feed(Context c, Source src, String[] end, Runnable endAction) {
        super(c);
        this.src = src;
        this.end = end;
        this.endAction = endAction;
        lm = new LinearLayoutManager(c);
        setLayoutManager(lm);
        new PagerSnapHelper().attachToRecyclerView(this);
        setOverScrollMode(OVER_SCROLL_NEVER);
        setItemViewCacheSize(3);
        setHasFixedSize(true);
        setAdapter(new Ad());
        more();
        addOnScrollListener(new OnScrollListener() {
            @Override public void onScrollStateChanged(RecyclerView r, int st) {
                if (st == SCROLL_STATE_IDLE) settle();
                else if (st == SCROLL_STATE_DRAGGING) { removeCallbacks(pending); cur = -1; }
            }
        });
    }

    @Override protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        post(this::settle);
    }

    @Override protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        // first layout (or after a refill): start the read timer for the card on screen
        if (cur < 0 && getScrollState() == SCROLL_STATE_IDLE) post(this::settle);
    }

    @Override protected void onDetachedFromWindow() {
        removeCallbacks(pending);
        super.onDetachedFromWindow();
    }

    private void settle() {
        int p = lm.findFirstCompletelyVisibleItemPosition();
        if (p < 0 || p == cur) return;
        cur = p;
        removeCallbacks(pending);
        if (p < ids.size()) {
            final int id = ids.get(p), pos = p;
            pending = () -> {
                if (Store.I.markRead(id, false)) {
                    ViewHolder h = findViewHolderForAdapterPosition(pos);
                    if (h instanceof CardH) ((CardH) h).c.read.animate().alpha(1).setDuration(250);
                }
            };
            postDelayed(pending, READ_MS);
        }
        if (p >= ids.size() - 4) more();
    }

    private void more() {
        if (ended) return;
        List<Integer> n = src.next(BATCH);
        int start = ids.size();
        ids.addAll(n);
        if (n.size() < BATCH) ended = true;
        int added = n.size() + (ended ? 1 : 0);
        if (added > 0) {
            Adapter<?> a = getAdapter();
            if (a != null) a.notifyItemRangeInserted(start, added);
        }
    }

    private static final class CardH extends ViewHolder {
        final Cards.Idea c;
        CardH(Cards.Idea c) { super(c.root); this.c = c; }
    }

    private View endView(Context c) {
        LinearLayout l = col(c);
        l.setGravity(Gravity.CENTER);
        l.setPadding(dp(32), dp(32), dp(32), dp(32));
        l.addView(tv(c, end[0], 56, TEXT, REG));
        l.addView(tv(c, end[1], 24, TEXT, BLACK), lpm(W, W, 0, 14, 0, 0));
        TextView t = tv(c, end[2], 15, MUTED, REG);
        t.setGravity(Gravity.CENTER);
        t.setLineSpacing(0, 1.3f);
        l.addView(t, lpm(W, W, 0, 10, 0, 22));
        l.addView(btn(c, end[3], B_GREEN, v -> endAction.run()), lp(dp(260), W));
        return l;
    }

    private final class Ad extends Adapter<ViewHolder> {
        @Override public int getItemCount() { return ids.size() + (ended ? 1 : 0); }

        @Override public int getItemViewType(int p) { return p < ids.size() ? 0 : 1; }

        @Override public ViewHolder onCreateViewHolder(ViewGroup parent, int type) {
            View v;
            ViewHolder h;
            if (type == 0) {
                Cards.Idea c = Cards.idea(parent.getContext(), true, true);
                h = new CardH(c);
                v = c.root;
            } else {
                v = endView(parent.getContext());
                h = new ViewHolder(v) {};
            }
            v.setLayoutParams(new RecyclerView.LayoutParams(M, M));
            return h;
        }

        @Override public void onBindViewHolder(ViewHolder h, int p) {
            if (h instanceof CardH) Cards.bind(((CardH) h).c, ids.get(p));
        }
    }
}
