#!/usr/bin/env python3
"""Vendoriza el reproductor h5p-standalone y las librerias H5P en assets/.

Fuentes (checkouts ya descargados, versiones fijadas — ver LIBRARIES.md
que genera este script):
  h5p-standalone 3.8.2 (tunapanda, MIT) — dist/ del paquete npm
  H5P.MemoryGame 1.3.53 (Joubel, MIT)
  H5P.DragQuestion 1.15.37 (Joubel, MIT; compilado con webpack, incluye
      h5p-lib-controls 0.0.17, MIT)
  H5P.Crossword 0.7.6 (otacke, MIT; compilado con webpack)
  H5P.AdvancedText 1.1.21 (Joubel, MIT — licencia en README)
  H5P.Question 1.5.57 (Joubel, MIT)
  H5P.JoubelUI 1.3.46 (Joubel, MIT — licencia en README)
  H5P.Components 1.0.102 (Joubel, MIT; compilado con webpack)
  H5P.Image 1.1.33 (Joubel, MIT — licencia en README)
  H5P.Transition 1.0.4 (Joubel, MIT)
  H5P.FontIcons 1.0.6 (Joubel, MIT — licencia en README)
  FontAwesome 4.5.4 (Dave Gandy, MIT)
  jQuery.ui 1.10.4 (jQuery Foundation, MIT) — reempaquetado desde los
      archivos oficiales de jQuery UI 1.10.4 (code.jquery.com / cdnjs)
  H5P.Timer 0.4 — REEMPLAZO escrito desde cero para Valhalla (MIT, ver
      tools/h5p_timer.js): el H5P.Timer original solo vive en el core H5P,
      cuyo repo ya no es publico, y no se pudo verificar una licencia
      permisiva para el. Implementa la misma API que usa MemoryGame
      (play/pause/stop/reset/getTime/setClockTime/notify/extractTimeElement).

Solo se copian archivos de ejecucion (library.json, JS/CSS pre-cargados,
idiomas y recursos referenciados: fuentes, imagenes del tema jQuery UI).
Nada de codigo GPL/AGPL/LGPL.

Uso: python3 tools/vendor_h5p.py [--src /tmp/h5p-src] [--root .]
"""
import json
import os
import shutil
import sys

SRC = os.environ.get("H5P_SRC", "/tmp/h5p-src")
ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
ASSETS = os.path.join(ROOT, "app", "src", "main", "assets")
PLAYER = os.path.join(ASSETS, "h5p-player")
LIBS = os.path.join(ASSETS, "h5p-libraries")

# (carpeta destino, subdir en SRC, archivos/carpetas a copiar)
LIBRARIES = [
    # (destino, origen, [rutas relativas a copiar], version, repo, licencia)
    ("H5P.MemoryGame-1.3", "memory-game",
     ["library.json", "src", "language"], "1.3.53",
     "h5p/h5p-memory-game", "MIT (Joubel AS, 2014)"),
    ("H5P.DragQuestion-1.15", "drag-question",
     ["library.json", "h5p-drag-question.js", "css", "language"],
     "1.15.37", "h5p/h5p-drag-question", "MIT (Joubel AS, 2012-2014)"),
    ("H5P.Crossword-0.7", "crossword",
     ["library.json", "dist", "language"], "0.7.6",
     "otacke/h5p-crossword", "MIT (Language Training Center GmbH)"),
    ("H5P.AdvancedText-1.1", "advanced-text",
     ["library.json", "text.js", "text.css", "language"], "1.1.21",
     "h5p/h5p-advanced-text", "MIT (Joubel AS, 2012-2014, en README)"),
    ("H5P.Question-1.5", "question",
     ["library.json", "scripts", "styles", "language", "images"],
     "1.5.57", "h5p/h5p-question", "MIT (Joubel AS, 2015)"),
    ("H5P.JoubelUI-1.3", "joubel-ui",
     ["library.json", "js", "css", "fonts", "language"], "1.3.46",
     "h5p/h5p-joubel-ui", "MIT (Joubel AS, 2015, en README)"),
    ("H5P.Components-1.0", "components",
     ["library.json", "dist"], "1.0.102",
     "h5p/h5p-components", "MIT"),
    ("H5P.Image-1.1", "image",
     ["library.json", "image.js", "image.css", "language"], "1.1.33",
     "h5p/h5p-image", "MIT (Joubel, en README)"),
    ("H5P.Transition-1.0", "transition",
     ["library.json", "transition.js"], "1.0.4",
     "h5p/h5p-transition", "MIT"),
    ("H5P.FontIcons-1.0", "font-icons",
     ["library.json", "styles", "fonts"], "1.0.6",
     "h5p/h5p-font-icons", "MIT (Joubel, en README)"),
    ("FontAwesome-4.5", "testh5p/FontAwesome-4.5",
     ["library.json", "h5p-font-awesome.min.css", "fontawesome-webfont.eot",
      "fontawesome-webfont.svg", "fontawesome-webfont.ttf",
      "fontawesome-webfont.woff", "fontawesome-webfont.woff2",
      "FontAwesome.otf"], "4.5.4",
     "h5p/h5p-standalone (fixture de pruebas)", "MIT (Dave Gandy)"),
]


def copy_one(src, dst):
    if os.path.isdir(src):
        shutil.copytree(src, dst, dirs_exist_ok=True)
        if os.path.basename(src) == "language":
            # Solo espanol (+ fallback ingles): el resto son ~50 archivos
            # por libreria que el reproductor nunca pide.
            for f in os.listdir(dst):
                if f not in ("es.json", "es-mx.json", ".en.json"):
                    os.remove(os.path.join(dst, f))
    else:
        os.makedirs(os.path.dirname(dst), exist_ok=True)
        shutil.copy2(src, dst)


def main():
    src = sys.argv[sys.argv.index("--src") + 1] if "--src" in sys.argv else SRC
    if "--root" in sys.argv:
        global ROOT, ASSETS, PLAYER, LIBS
        ROOT = sys.argv[sys.argv.index("--root") + 1]
        ASSETS = os.path.join(ROOT, "app", "src", "main", "assets")
        PLAYER = os.path.join(ASSETS, "h5p-player")
        LIBS = os.path.join(ASSETS, "h5p-libraries")

    # 1. Reproductor h5p-standalone 3.8.2 (dist del paquete npm)
    dist = os.path.join(src, "standalone-dist", "package", "dist")
    if not os.path.isdir(dist):
        print("FALTA dist de h5p-standalone en", dist)
        print("  npm pack h5p-standalone@3.8.2 en el dir SRC y descomprime")
        return 1
    if os.path.isdir(PLAYER):
        shutil.rmtree(PLAYER)
    shutil.copytree(dist, PLAYER)
    for junk in ("main.bundle.js.LICENSE.txt", "h5p.d.ts", "cypress.config.js"):
        p = os.path.join(PLAYER, junk)
        if os.path.exists(p):
            os.remove(p)
    print("player: h5p-standalone 3.8.2 ->", PLAYER)

    # 2. Librerias H5P
    if os.path.isdir(LIBS):
        shutil.rmtree(LIBS)
    os.makedirs(LIBS)
    manifest = []
    for dest, origin, paths, version, repo, lic in LIBRARIES:
        odir = os.path.join(src, origin)
        ddir = os.path.join(LIBS, dest)
        os.makedirs(ddir)
        for rel in paths:
            s = os.path.join(odir, rel)
            if not os.path.exists(s):
                print(f"  AVISO: falta {rel} en {origin} (se omite)")
                continue
            copy_one(s, os.path.join(ddir, rel))
        lib = json.load(open(os.path.join(ddir, "library.json"), encoding="utf-8"))
        assert (lib["majorVersion"], lib["minorVersion"]) == tuple(
            int(x) for x in version.split(".")[:2]), (dest, version, lib)
        manifest.append((dest, version, repo, lic))
        print(f"lib: {dest} {version} ({repo})")

    # 3. jQuery.ui 1.10.4 (MIT) reempaquetado
    jq = os.path.join(LIBS, "jQuery.ui-1.10")
    os.makedirs(os.path.join(jq, "images"))
    # Shim: el core H5P hace jQuery.noConflict(true) y quita el jQuery
    # global antes de cargar las librerias; jQuery UI 1.10.4 (UMD) espera
    # un `jQuery` global. Se le pasa el H5P.jQuery del core.
    _jq_src = open(os.path.join(src, "jquery-ui-1.10.4.min.js"),
                   encoding="utf-8").read()
    _shim = ("/* Valhalla: jQuery UI necesita el jQuery global; el core H5P "
             "lo quita con noConflict(true), asi que se le pasa H5P.jQuery. */\n"
             "var jQuery = (window.H5P && H5P.jQuery) || window.jQuery;\n")
    with open(os.path.join(jq, "jquery-ui.min.js"), "w",
              encoding="utf-8") as f:
        f.write(_shim + _jq_src)
    shutil.copy2(os.path.join(src, "jquery-ui-1.10.4.css"),
                 os.path.join(jq, "jquery-ui.css"))
    for f in os.listdir(os.path.join(src, "jquery-ui-images")):
        shutil.copy2(os.path.join(src, "jquery-ui-images", f),
                     os.path.join(jq, "images", f))
    json.dump({
        "title": "jQuery UI",
        "description": "jQuery UI 1.10.4 (core, widget, mouse, draggable, "
                       "droppable, resizable, selectable, sortable, effect, "
                       "accordion, autocomplete, button, datepicker, dialog, "
                       "menu, progressbar, slider, spinner, tabs, tooltip).",
        "majorVersion": 1, "minorVersion": 10, "patchVersion": 4,
        "runnable": 0, "machineName": "jQuery.ui",
        "license": "MIT", "author": "jQuery Foundation",
        "coreApi": {"majorVersion": 1, "minorVersion": 10},
        "preloadedJs": [{"path": "jquery-ui.min.js"}],
        "preloadedCss": [{"path": "jquery-ui.css"}],
    }, open(os.path.join(jq, "library.json"), "w", encoding="utf-8"), indent=2)
    manifest.append(("jQuery.ui-1.10", "1.10.4",
                     "jquery/jquery-ui (code.jquery.com / cdnjs)",
                     "MIT (jQuery Foundation)"))
    print("lib: jQuery.ui-1.10 1.10.4 (jQuery UI oficial)")

    # 4. H5P.Timer 0.4 — reemplazo propio (ver tools/h5p_timer.js)
    tm = os.path.join(LIBS, "H5P.Timer-0.4")
    os.makedirs(tm)
    shutil.copy2(os.path.join(ROOT, "tools", "h5p_timer.js"),
                 os.path.join(tm, "timer.js"))
    json.dump({
        "title": "Timer",
        "description": "Temporizador para Memory Game. Reemplazo escrito "
                       "desde cero para Valhalla School (MIT) con la misma "
                       "API que usa H5P.MemoryGame; el H5P.Timer original "
                       "solo existe en el core H5P, sin licencia permisiva "
                       "verificable.",
        "majorVersion": 0, "minorVersion": 4, "patchVersion": 0,
        "runnable": 0, "machineName": "H5P.Timer",
        "license": "MIT", "author": "Valhalla School",
        "coreApi": {"majorVersion": 1, "minorVersion": 28},
        "preloadedJs": [{"path": "timer.js"}],
    }, open(os.path.join(tm, "library.json"), "w", encoding="utf-8"), indent=2)
    manifest.append(("H5P.Timer-0.4", "0.4.0",
                     "Valhalla School (reemplazo propio, ver tools/h5p_timer.js)",
                     "MIT"))
    print("lib: H5P.Timer-0.4 0.4.0 (reemplazo propio)")

    # 5. Manifiesto
    with open(os.path.join(LIBS, "LIBRARIES.md"), "w", encoding="utf-8") as f:
        f.write("# Librerias H5P empaquetadas (Valhalla School v1.2.0)\n\n")
        f.write("Todas con licencia permisiva (MIT). Nada GPL/AGPL/LGPL.\n")
        f.write("Generado por `tools/vendor_h5p.py`. Ver NOTICE en la raiz.\n\n")
        f.write("| Libreria | Version | Fuente | Licencia |\n|---|---|---|---|\n")
        for dest, version, repo, lic in manifest:
            f.write(f"| {dest} | {version} | {repo} | {lic} |\n")
        f.write("\nReproductor: h5p-standalone 3.8.2 (tunapanda, MIT) "
                "en `assets/h5p-player/`.\n")
    print("manifiesto:", os.path.join(LIBS, "LIBRARIES.md"))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
