package com.valhallaschool.app;

import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

/** Heterogeneous list adapter: each item is (Kind, data). Kinds create + bind their own views. */
final class Rows extends RecyclerView.Adapter<Rows.H> {
    interface Kind {
        View create(ViewGroup parent);
        void bind(View v, Object data);
    }

    static final class H extends RecyclerView.ViewHolder {
        H(View v) { super(v); }
    }

    /** Wraps a prebuilt one-off view (stats blocks etc.). */
    static final Kind STATIC = new Kind() {
        @Override public View create(ViewGroup p) { return new FrameLayout(p.getContext()); }

        @Override public void bind(View v, Object d) {
            FrameLayout f = (FrameLayout) v;
            View child = (View) d;
            if (child.getParent() == f) return;
            f.removeAllViews();
            if (child.getParent() != null) ((ViewGroup) child.getParent()).removeView(child);
            f.addView(child);
        }
    };

    private final ArrayList<Kind> kinds = new ArrayList<>();
    private final ArrayList<Kind> itemKind = new ArrayList<>();
    private final ArrayList<Object> itemData = new ArrayList<>();

    void clear() { itemKind.clear(); itemData.clear(); }

    Rows add(Kind k, Object d) { itemKind.add(k); itemData.add(d); return this; }

    int size() { return itemKind.size(); }

    @SuppressWarnings("NotifyDataSetChanged")
    void done() { notifyDataSetChanged(); }

    @Override public int getItemCount() { return itemKind.size(); }

    @Override public int getItemViewType(int p) {
        Kind k = itemKind.get(p);
        int i = kinds.indexOf(k);
        if (i < 0) { kinds.add(k); i = kinds.size() - 1; }
        return i;
    }

    @Override public H onCreateViewHolder(ViewGroup parent, int type) {
        View v = kinds.get(type).create(parent);
        v.setLayoutParams(new RecyclerView.LayoutParams(Ui.M, Ui.W));
        return new H(v);
    }

    @Override public void onBindViewHolder(H h, int p) { itemKind.get(p).bind(h.itemView, itemData.get(p)); }
}
