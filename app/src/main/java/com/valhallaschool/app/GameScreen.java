package com.valhallaschool.app;

import android.graphics.Color;
import android.view.Gravity;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.LinearLayout;
import android.widget.TextView;

import static com.valhallaschool.app.Ui.*;

/** Pantalla de minijuego: WebView con el juego H5P real (offline).
 * Memoria, arrastrar y crucigrama corren con h5p-standalone (MIT) en
 * modo div (sin iframe), asi los eventos xAPI de completado llegan
 * directos a window.Android.gameCompleted() (+10 XP). La sopa de letras
 * es un juego propio de Valhalla (mismo puente). El WebView es
 * transparente para que las particulas (ParticleView) se vean detras,
 * y la pagina aplica el tema activo (?theme=...) con valhalla-theme.js. */
final class GameScreen extends MainActivity.Screen {
    private final WebView web;
    private boolean awarded;

    GameScreen(MainActivity a, String modDir, String game, String title) {
        super(a);
        up = true;
        LinearLayout col = col(a);
        LinearLayout top = row(a);
        top.setPadding(dp(6), dp(8), dp(14), dp(6));
        top.addView(iconBtn(a, R.drawable.ic_close, MUTED, x -> a.back()));
        TextView t = tv(a, title, 17, TEXT, BLACK);
        t.setGravity(Gravity.CENTER);
        top.addView(t, lp(0, W, 1));
        col.addView(top, lp(M, W));

        web = new WebView(a);
        web.setBackgroundColor(Color.TRANSPARENT);
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setAllowFileAccess(true);
        // El reproductor H5P carga h5p.json, content.json y las librerias
        // con fetch() desde file://: hacen falta estos dos permisos.
        s.setAllowFileAccessFromFileURLs(true);
        s.setAllowUniversalAccessFromFileURLs(true);
        s.setDomStorageEnabled(true);
        web.addJavascriptInterface(new Object() {
            @JavascriptInterface
            public void gameCompleted() {
                if (awarded) return;
                awarded = true;
                a.runOnUiThread(() -> Store.I.addXP(10, false));
            }
        }, "Android");
        // v1.2.0: los juegos H5P reales viven en
        // h5p-games/<mod>/<juego>/<juego>.html (antes: <mod>/<juego>.html).
        web.loadUrl("file:///android_asset/h5p-games/" + modDir + "/" + game
                + "/" + game + ".html?theme=" + Store.I.theme);
        col.addView(web, lp(M, 0, 1));
        v = col;
    }

    @Override boolean onBack() {
        if (web.canGoBack()) { web.goBack(); return true; }
        return false;
    }

    @Override void closed() {
        web.stopLoading();
        web.destroy();
    }
}
