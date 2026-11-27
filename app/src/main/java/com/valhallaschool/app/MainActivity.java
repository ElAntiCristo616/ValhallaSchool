package com.valhallaschool.app;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Insets;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.animation.DecelerateInterpolator;
import android.view.inputmethod.InputMethodManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static com.valhallaschool.app.Ui.*;

/**
 * Single activity. Four tabs live side by side in tabHost (kept alive, toggled by visibility so the
 * home feed keeps its scroll position). Book / reader / lesson screens are pushed as overlay views.
 */
public final class MainActivity extends Activity implements Store.Events {

    abstract static class Tab {
        final MainActivity a;
        View v;
        boolean stale = true;
        Tab(MainActivity a) { this.a = a; }
        abstract View build();
        abstract void refresh();
    }

    abstract static class Screen {
        final MainActivity a;
        View v;
        boolean up;
        Screen(MainActivity a) { this.a = a; }
        void resume() {}
        void closed() {}
        /** Called when the activity loses focus (v1.3.0: Lluvia pauses the fall). */
        void paused() {}
        /** @return true if the screen handled back itself. */
        boolean onBack() { return false; }
    }

    private static final int REQ_EXPORT = 1, REQ_IMPORT = 2;
    // v1.3.1: Perfil al final (antes era la 3ª pestaña, en el centro).
    private static final int[] NAV_IC = {R.drawable.ic_home, R.drawable.ic_trophy, R.drawable.ic_learn, R.drawable.ic_games, R.drawable.ic_profile};
    private static final int[] NAV_ON = {R.drawable.ic_home_fill, R.drawable.ic_trophy, R.drawable.ic_learn, R.drawable.ic_games, R.drawable.ic_profile_fill};
    private static final String[] NAV = {"Inicio", "Retos", "Práctica", "Juegos", "Perfil"};

    Store S;
    FrameLayout root;
    private ParticleView particles;
    private FrameLayout tabHost;
    private final Tab[] tabs = new Tab[5];
    int cur = -1;
    final ArrayList<Screen> stack = new ArrayList<>();
    private TextView toastV;
    private final ArrayDeque<Object[]> toastQ = new ArrayDeque<>();
    private boolean toastBusy;
    private final ArrayDeque<String[]> celeQ = new ArrayDeque<>();
    private View celeV, modalV;
    private final ImageView[] navIc = new ImageView[5];
    private final TextView[] navTx = new TextView[5];
    private View dueDot;
    private int lastDay;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        Data.load(this);
        PracticeData.load(this);
        GamesData.load(this);
        SupabaseAuth.INSTANCE.init(this);
        S = Store.get(this);
        S.ev = this;
        Ui.init(this, S.theme);
        Sfx.init(this); // v1.3.2: los sonidos respetan el modo silencio del teléfono
        build();
        edgeToEdge();
        go(state != null ? state.getInt("tab", 0) : 0);
        lastDay = Store.today();
        // v1.2.6 — recordatorio diario de racha (20:00, solo si falta la meta).
        StreakReminder.createChannel(this);
        if (S.remind) StreakReminder.schedule(this);
        if (S.remind) askNotificationPermission();
    }

    private static final int REQ_NOTIFY = 9001;

    /** Pide el permiso de notificaciones (Android 13+) una sola vez; si lo niega, no se insiste. */
    void askNotificationPermission() {
        if (Build.VERSION.SDK_INT < 33) return;
        if (S.remindAsked) return;
        S.remindAsked = true;
        S.save();
        if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQ_NOTIFY);
        }
    }

    @Override public void onRequestPermissionsResult(int code, String[] perms, int[] grants) {
        super.onRequestPermissionsResult(code, perms, grants);
        if (code == REQ_NOTIFY && grants.length > 0 && grants[0] != PackageManager.PERMISSION_GRANTED) {
            toast("Activa las notificaciones en Ajustes del teléfono si quieres que te avise de tu racha.", false);
        }
    }

    @Override protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        out.putInt("tab", cur);
    }

    @Override protected void onPause() {
        super.onPause();
        if (particles != null) particles.stop();
        if (!stack.isEmpty()) stack.get(stack.size() - 1).paused();
        S.flush();
    }

    @Override protected void onResume() {
        super.onResume();
        if (particles != null) particles.start();
        int d = Store.today();
        if (d != lastDay) { lastDay = d; dirty(); changed(); }
    }

    @Override protected void onDestroy() {
        if (S.ev == this) S.ev = null;
        super.onDestroy();
    }

    /* ================= window / layout ================= */
    @SuppressWarnings("deprecation")
    private void edgeToEdge() {
        Window w = getWindow();
        w.setBackgroundDrawable(new ColorDrawable(BG));
        w.setStatusBarColor(Color.TRANSPARENT);
        w.setNavigationBarColor(Color.TRANSPARENT);
        if (Build.VERSION.SDK_INT >= 29) w.setNavigationBarContrastEnforced(false);
        if (Build.VERSION.SDK_INT >= 30) {
            w.setDecorFitsSystemWindows(false);
            WindowInsetsController c = w.getInsetsController();
            int mask = WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS | WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS;
            if (c != null) c.setSystemBarsAppearance(Ui.dark ? 0 : mask, mask);
        } else {
            int f = View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION;
            if (!Ui.dark) f |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
            w.getDecorView().setSystemUiVisibility(f);
        }
    }

    @SuppressWarnings("deprecation")
    private void build() {
        root = new FrameLayout(this);
        root.setBackgroundColor(BG);
        LinearLayout main = col(this);
        tabHost = new FrameLayout(this);
        main.addView(tabHost, lp(M, 0, 1));
        View line = new View(this);
        line.setBackgroundColor(LINE);
        main.addView(line, lp(M, Math.max(1, dp(1))));
        LinearLayout nav = row(this);
        nav.setBackgroundColor(BG);
        for (int i = 0; i < 5; i++) {
            final int idx = i;
            LinearLayout b = col(this);
            b.setGravity(Gravity.CENTER);
            FrameLayout icf = new FrameLayout(this);
            navIc[i] = icon(this, NAV_IC[i], MUTED);
            icf.addView(navIc[i], new FrameLayout.LayoutParams(dp(24), dp(24), Gravity.CENTER));
            if (i == 0) {
                dueDot = new View(this);
                GradientDrawable dot = oval(RED);
                dot.setStroke(dp(2), BG);
                dueDot.setBackground(dot);
                icf.addView(dueDot, new FrameLayout.LayoutParams(dp(10), dp(10), Gravity.TOP | Gravity.END));
            }
            b.addView(icf, lp(dp(32), dp(26)));
            navTx[i] = tv(this, NAV[i], 11, MUTED, MEDIUM);
            b.addView(navTx[i], lpm(W, W, 0, 3, 0, 0));
            b.setBackground(new RippleDrawable(ColorStateList.valueOf(alpha(TEXT, 0.1f)), null, null));
            b.setOnClickListener(v -> go(idx));
            nav.addView(b, lp(0, M, 1));
        }
        main.addView(nav, lp(M, dp(62)));
        root.addView(main, new FrameLayout.LayoutParams(M, M));

        toastV = tv(this, "", 14, TEXT, BLACK);
        toastV.setBackground(round(SURF2, 14, LINE, 1));
        toastV.setPadding(dp(16), dp(10), dp(16), dp(10));
        toastV.setElevation(dp(10));
        FrameLayout.LayoutParams tl = new FrameLayout.LayoutParams(W, W, Gravity.TOP | Gravity.CENTER_HORIZONTAL);
        tl.topMargin = dp(12);
        toastV.setTranslationY(-dp(160));
        root.addView(toastV, tl);
        setContentView(root);

        // Fondo ambiental del tema: capa de fondo (índice 0), DETRÁS de todo el
        // contenido. Nada se dibuja jamás por encima de tarjetas, textos,
        // botones o la barra de navegación. Toasts y modales siguen por encima.
        particles = new ParticleView(this, S.theme);
        root.addView(particles, 0, new FrameLayout.LayoutParams(M, M));
        particles.start();

        root.setOnApplyWindowInsetsListener((v, in) -> {
            int l, t, r, b;
            if (Build.VERSION.SDK_INT >= 30) {
                Insets s = in.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
                Insets ime = in.getInsets(WindowInsets.Type.ime());
                l = s.left; t = s.top; r = s.right; b = Math.max(s.bottom, ime.bottom);
            } else {
                l = in.getSystemWindowInsetLeft(); t = in.getSystemWindowInsetTop();
                r = in.getSystemWindowInsetRight(); b = in.getSystemWindowInsetBottom();
            }
            v.setPadding(l, t, r, b);
            return in;
        });
    }

    /* ================= tabs ================= */
    private Tab makeTab(int i) {
        switch (i) {
            case 1: return new ChallengesTab(this);
            case 2: return new PracticeTab(this);
            case 3: return new GamesTab(this);
            case 4: return new ProfileTab(this);
            default: return new LearnTab(this);
        }
    }

    void go(int i) {
        if (!stack.isEmpty()) popAll();
        if (cur == i) return;
        hideKeyboard();
        cur = i;
        Tab t = tabs[i];
        if (t == null) {
            t = tabs[i] = makeTab(i);
            t.v = t.build();
            tabHost.addView(t.v, new FrameLayout.LayoutParams(M, M));
        }
        for (int k = 0; k < 5; k++) if (tabs[k] != null) tabs[k].v.setVisibility(k == i ? View.VISIBLE : View.GONE);
        if (t.stale) { t.stale = false; t.refresh(); }
        for (int k = 0; k < 5; k++) {
            navIc[k].setImageResource(k == i ? NAV_ON[k] : NAV_IC[k]);
            navIc[k].setColorFilter(k == i ? ACCENT : MUTED);
            navTx[k].setTextColor(k == i ? TEXT : MUTED);
        }
        updateDot();
    }

    /** Progress changed: everything except the visible tab (when nothing covers it) must redraw on next show. */
    void dirty() {
        for (int k = 0; k < 5; k++) if (tabs[k] != null && (k != cur || !stack.isEmpty())) tabs[k].stale = true;
    }

    @Override public void changed() {
        dirty();
        updateDot();
    }

    private void updateDot() {
        if (dueDot != null) dueDot.setVisibility(S.due().isEmpty() ? View.GONE : View.VISIBLE);
    }

    void hideKeyboard() {
        InputMethodManager im = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (im != null && root != null) im.hideSoftInputFromWindow(root.getWindowToken(), 0);
    }

    /* ================= overlay screens ================= */
    void openBook(int b) { push(new BookScreen(this, b)); }

    void openReader(String title, List<Integer> ids) { push(new ReaderScreen(this, title, ids)); }

    /** Ideas of the same book starting at id. */
    static List<Integer> from(int id) {
        ArrayList<Integer> l = new ArrayList<>();
        int b = Data.book(id);
        for (int i = id; i < b * Data.PER + Data.PER; i++) l.add(i);
        return l;
    }

    void push(Screen s) {
        hideKeyboard();
        View v = s.v;
        v.setClickable(true);
        // Fondo ambiental del tema (degradado sutil), no un color plano:
        // v1.3.2 — SEMITRANSPARENTE (alfa 235, ~92%): el tema y las
        // partículas del fondo se ven detrás en TODAS las pantallas
        // (lecciones, lector, hubs, juegos) sin tapar el contenido.
        GradientDrawable bg = ambientBg();
        bg.setAlpha(235);
        v.setBackground(bg);
        root.addView(v, overlayIndex(), new FrameLayout.LayoutParams(M, M));
        stack.add(s);
        DecelerateInterpolator di = new DecelerateInterpolator(2f);
        if (s.up) {
            v.setTranslationY(root.getHeight());
            v.animate().translationY(0).setDuration(240).setInterpolator(di).start();
        } else {
            v.setTranslationX(root.getWidth());
            v.animate().translationX(0).setDuration(220).setInterpolator(di).start();
        }
    }

    /** Screens go under any open celebration/modal so those stay visible. */
    private int overlayIndex() {
        if (celeV != null) return root.indexOfChild(celeV);
        if (modalV != null) return root.indexOfChild(modalV);
        return root.indexOfChild(toastV);
    }

    void back() {
        if (stack.isEmpty()) return;
        Screen s = stack.remove(stack.size() - 1);
        s.closed();
        View v = s.v;
        (s.up ? v.animate().translationY(root.getHeight()) : v.animate().translationX(root.getWidth()))
                .setDuration(200).withEndAction(() -> root.removeView(v)).start();
        afterPop();
    }

    private void popAll() {
        while (!stack.isEmpty()) {
            Screen s = stack.remove(stack.size() - 1);
            s.closed();
            root.removeView(s.v);
        }
        afterPop();
    }

    private void afterPop() {
        if (!stack.isEmpty()) { stack.get(stack.size() - 1).resume(); return; }
        Tab t = cur >= 0 ? tabs[cur] : null;
        if (t != null && t.stale) { t.stale = false; t.refresh(); }
        changed();
    }

    @SuppressWarnings("deprecation")
    @Override public void onBackPressed() {
        if (modalV != null) { closeModal(); return; }
        if (celeV != null) { nextCele(); return; }
        if (!stack.isEmpty()) {
            if (!stack.get(stack.size() - 1).onBack()) back();
            return;
        }
        if (cur != 0) { go(0); return; }
        super.onBackPressed();
    }

    /* ================= toast / celebrate / modal ================= */
    @Override public void toast(String msg, boolean xp) {
        toastQ.add(new Object[]{msg, xp});
        if (!toastBusy) nextToast();
    }

    private void nextToast() {
        Object[] m = toastQ.poll();
        if (m == null) { toastBusy = false; return; }
        toastBusy = true;
        toastV.setText((String) m[0]);
        toastV.setTextColor((Boolean) m[1] ? ACCENT : TEXT);
        toastV.animate().translationY(0).setDuration(220).withEndAction(() ->
                toastV.postDelayed(() -> toastV.animate().translationY(-dp(160)).setDuration(220).withEndAction(this::nextToast).start(), 1200)).start();
    }

    @Override public void celebrate(String ico, String title, String text) {
        celeQ.add(new String[]{ico, title, text});
        if (celeV == null) showCele();
    }

    private void showCele() {
        String[] c = celeQ.peek();
        if (c == null) return;
        LinearLayout v = col(this);
        v.setGravity(Gravity.CENTER);
        v.setBackgroundColor(alpha(BG, 0.96f));
        v.setClickable(true);
        v.setPadding(dp(32), dp(32), dp(32), dp(32));
        v.addView(Avatars.view(this, 96), lpm(W, W, 0, 0, 0, 4));
        v.addView(tv(this, c[0], 96, TEXT, REG));
        TextView t = tv(this, c[1], 28, TEXT, BLACK);
        t.setGravity(Gravity.CENTER);
        v.addView(t, lpm(W, W, 0, 12, 0, 0));
        TextView p = tv(this, c[2], 15, MUTED, REG);
        p.setGravity(Gravity.CENTER);
        p.setLineSpacing(0, 1.3f);
        v.addView(p, lpm(W, W, 0, 12, 0, 28));
        v.addView(btn(this, "Continuar", B_YELLOW, x -> nextCele()), lp(dp(280), W));
        v.setAlpha(0);
        root.addView(v, root.indexOfChild(toastV), new FrameLayout.LayoutParams(M, M));
        v.animate().alpha(1).setDuration(200).start();
        celeV = v;
        confetti();
        Sfx.play(Sfx.WIN);
    }

    private void nextCele() {
        if (celeV != null) root.removeView(celeV);
        celeV = null;
        celeQ.poll();
        showCele();
    }

    void confetti() {
        Views.Confetti cf = new Views.Confetti(this);
        root.addView(cf, root.indexOfChild(toastV), new FrameLayout.LayoutParams(M, M));
        cf.start();
    }

    void ask(String title, String text, String okLabel, Runnable onOk, boolean danger) {
        FrameLayout m = new FrameLayout(this);
        m.setBackgroundColor(0x8c000000);
        m.setOnClickListener(v -> closeModal());
        LinearLayout sheet = col(this);
        GradientDrawable g = new GradientDrawable();
        g.setColor(SURF);
        float r = dp(24);
        g.setCornerRadii(new float[]{r, r, r, r, 0, 0, 0, 0});
        sheet.setBackground(g);
        sheet.setClickable(true);
        sheet.setPadding(dp(20), dp(24), dp(20), dp(20));
        sheet.addView(tv(this, title, 20, TEXT, BLACK));
        TextView p = tv(this, text, 15, MUTED, REG);
        p.setLineSpacing(0, 1.3f);
        sheet.addView(p, lpm(M, W, 0, 10, 0, 20));
        LinearLayout bs = row(this);
        bs.addView(btn(this, "Cancelar", B_GHOST, v -> closeModal()), lp(0, W, 1));
        LinearLayout.LayoutParams okp = lpm(0, W, 12, 0, 0, 0);
        okp.weight = 1;
        bs.addView(btn(this, okLabel, danger ? B_RED : B_GREEN, v -> { closeModal(); onOk.run(); }), okp);
        sheet.addView(bs, lp(M, W));
        m.addView(sheet, new FrameLayout.LayoutParams(M, W, Gravity.BOTTOM));
        root.addView(m, root.indexOfChild(toastV), new FrameLayout.LayoutParams(M, M));
        modalV = m;
    }

    private void closeModal() {
        if (modalV != null) root.removeView(modalV);
        modalV = null;
    }

    /* ================= settings actions ================= */
    void setTheme(String id) {
        if (!Ui.isTheme(id)) id = "navidad";
        S.theme = id;
        S.dark = !"claro".equals(id);
        S.flush();
        recreate();
    }

    /** Desplegable de temas: lista los 16 temas con muestra de color. */
    void showThemePicker() {
        FrameLayout m = new FrameLayout(this);
        m.setBackgroundColor(0x8c000000);
        m.setOnClickListener(v -> closeModal());
        LinearLayout sheet = col(this);
        GradientDrawable g = new GradientDrawable();
        g.setColor(SURF);
        float r = dp(24);
        g.setCornerRadii(new float[]{r, r, r, r, 0, 0, 0, 0});
        sheet.setBackground(g);
        sheet.setClickable(true);
        sheet.setPadding(dp(20), dp(24), dp(20), dp(20));
        sheet.addView(tv(this, "Tema", 20, TEXT, BLACK));
        android.widget.ScrollView sc = new android.widget.ScrollView(this);
        LinearLayout list = col(this);
        for (int i = 0; i < Ui.THEME_IDS.length; i++) {
            final String id = Ui.THEME_IDS[i];
            int[] pal = Ui.THEME_PAL[i];
            LinearLayout row = row(this);
            row.setPadding(dp(4), dp(10), dp(4), dp(10));
            View dot = new View(this);
            GradientDrawable od = oval(pal[2]);
            od.setSize(dp(26), dp(26));
            dot.setBackground(od);
            row.addView(dot, lp(dp(26), dp(26)));
            TextView nm = tv(this, Ui.THEME_NAMES[i], 16, TEXT, MEDIUM);
            nm.setPadding(dp(14), 0, 0, 0);
            row.addView(nm, lp(0, W, 1));
            if (id.equals(S.theme)) row.addView(tv(this, "✓", 18, ACCENT, BLACK));
            ripple(row, true);
            row.setOnClickListener(v -> { closeModal(); setTheme(id); });
            list.addView(row);
        }
        sc.addView(list);
        sheet.addView(sc, lpm(M, dp(380), 0, 12, 0, 0));
        m.addView(sheet, new FrameLayout.LayoutParams(M, W, Gravity.BOTTOM));
        root.addView(m, root.indexOfChild(toastV), new FrameLayout.LayoutParams(M, M));
        modalV = m;
    }

    void resetAll() {
        S.clear();
        S.seed = new Random().nextLong();
        S.flush();
        recreate();
    }

    @SuppressWarnings("deprecation")
    void exportBackup() {
        S.flush();
        Intent i = new Intent(Intent.ACTION_CREATE_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("application/json")
                .putExtra(Intent.EXTRA_TITLE, "valhalla-school-backup-" + LocalDate.now() + ".json");
        try { startActivityForResult(i, REQ_EXPORT); } catch (Exception e) { toast("⚠️ No hay app de archivos", false); }
    }

    @SuppressWarnings("deprecation")
    void importBackup() {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("*/*");
        try { startActivityForResult(i, REQ_IMPORT); } catch (Exception e) { toast("⚠️ No hay app de archivos", false); }
    }

    @SuppressWarnings("deprecation")
    @Override protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (res != RESULT_OK || data == null || data.getData() == null) return;
        Uri u = data.getData();
        if (req == REQ_EXPORT) {
            try (OutputStream o = getContentResolver().openOutputStream(u)) {
                if (o == null) throw new IllegalStateException();
                o.write(S.toJson().toString(1).getBytes(StandardCharsets.UTF_8));
                toast("✅ Respaldo guardado", false);
            } catch (Exception e) {
                toast("⚠️ No se pudo guardar el respaldo", false);
            }
        } else if (req == REQ_IMPORT) {
            try (InputStream in = getContentResolver().openInputStream(u)) {
                if (in == null) throw new IllegalStateException();
                ByteArrayOutputStream bo = new ByteArrayOutputStream();
                byte[] buf = new byte[8192];
                int n;
                while ((n = in.read(buf)) > 0) bo.write(buf, 0, n);
                JSONObject o = new JSONObject(bo.toString("UTF-8"));
                if (!o.has("read")) throw new IllegalArgumentException("not a backup");
                S.fromJson(o);
                S.flush();
                recreate();
            } catch (Exception e) {
                toast("⚠️ Archivo de respaldo no válido", false);
            }
        }
    }
}
