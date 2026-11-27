#!/usr/bin/env python3
"""Tests de conversion H5P (Valhalla School v1.2.0).

Verifica que los 20 juegos generados en app/src/main/assets/h5p-games/
conservan SIN PERDIDAS el contenido de
~/workspace/socionics-game/content/h5p/<modulo>/games.json:
  memoria    53 pares (m1 8, m2 8, m3 16, m4 15, m5 6)
  arrastrar  58 items / 19 zonas (m1 2/8, m2 4/8, m3 4/16, m4 5/10, m5 4/16)
  crucigrama 100 palabras (m1 18, m2 15, m3 26, m4 30, m5 11)
  sopa       95 palabras (m1 20, m2 13, m3 20, m4 30, m5 12)

Tambien verifica: respuestas de crucigrama en MAYUSCULAS (la «INTROTImIA»
de m4 se normaliza), orientaciones validas, ubicaciones de sopa que
deletrean la palabra en la rejilla, imagenes de memoria presentes,
h5p.json con la libreria principal correcta y dependencias que existen
en assets/h5p-libraries/, y que no hay texto GPL en lo empaquetado.

Sin la carpeta fuente (en CI, donde content/ no esta en el repo), verifica
solo el contenido generado contra los conteos esperados de EXPECTED y la
consistencia interna; con la fuente presente compara ademas contra ella.

Uso: python3 tools/tests/test_h5p_conversion.py [--content ...] [--root .]
"""
import json
import os
import re
import sys

CONTENT_DEFAULT = os.path.expanduser("~/workspace/socionics-game/content/h5p")
ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
GAMES = os.path.join(ROOT, "app", "src", "main", "assets", "h5p-games")
LIBS = os.path.join(ROOT, "app", "src", "main", "assets", "h5p-libraries")
MODULOS = ["m1-elementos", "m2-modelo-a", "m3-tipos-cuadras",
           "m4-dicotomias", "m5-relaciones"]

EXPECTED = {
    "m1-elementos": {"memoria": 8, "arrastrar": (8, 2),
                     "crucigrama": 18, "sopa": 20},
    "m2-modelo-a": {"memoria": 8, "arrastrar": (8, 4),
                    "crucigrama": 15, "sopa": 13},
    "m3-tipos-cuadras": {"memoria": 16, "arrastrar": (16, 4),
                         "crucigrama": 26, "sopa": 20},
    "m4-dicotomias": {"memoria": 15, "arrastrar": (10, 5),
                      "crucigrama": 30, "sopa": 30},
    "m5-relaciones": {"memoria": 6, "arrastrar": (16, 4),
                      "crucigrama": 11, "sopa": 12},
}
MAIN_LIB = {"memoria": ("H5P.MemoryGame", 1, 3),
            "arrastrar": ("H5P.DragQuestion", 1, 15),
            "crucigrama": ("H5P.Crossword", 0, 7)}

FAILURES = []


def check(cond, msg):
    if not cond:
        FAILURES.append(msg)
        print("FAIL:", msg)


def load(p):
    with open(p, encoding="utf-8") as f:
        return json.load(f)


def lib_folder(machine, maj, mn):
    return os.path.join(LIBS, f"{machine}-{maj}.{mn}")


def test_memoria(mod, data):
    gdir = os.path.join(GAMES, mod, "memoria")
    content = load(os.path.join(gdir, "content", "content.json"))
    cards = content["cards"]
    check(len(cards) == EXPECTED[mod]["memoria"],
          f"{mod}/memoria: {len(cards)} cartas, esperadas {EXPECTED[mod]['memoria']}")
    if data is not None:
        check(len(cards) == len(data["memoria"]["pares"]),
              f"{mod}/memoria: {len(cards)} cartas != fuente "
              f"{len(data['memoria']['pares'])}")
    pairs = data["memoria"]["pares"] if data is not None else [None] * len(cards)
    for i, (pair, card) in enumerate(zip(pairs, cards)):
        if pair is not None:
            term, defi = pair
            check(card.get("imageAlt") == term,
                  f"{mod}/memoria carta {i}: imageAlt {card.get('imageAlt')!r} != {term!r}")
            check(card.get("matchAlt") == defi,
                  f"{mod}/memoria carta {i}: matchAlt != definicion")
        else:
            check(bool(card.get("imageAlt")),
                  f"{mod}/memoria carta {i}: imageAlt vacío")
            check(bool(card.get("matchAlt")),
                  f"{mod}/memoria carta {i}: matchAlt vacío")
        for key in ("image", "match"):
            img = card.get(key, {})
            check(img.get("path", "").startswith("images/card-"),
                  f"{mod}/memoria carta {i}: {key} sin path de imagen")
            p = os.path.join(gdir, "content", img.get("path", ""))
            check(os.path.isfile(p) and os.path.getsize(p) > 1000,
                  f"{mod}/memoria carta {i}: falta imagen {img.get('path')}")
    h5p = load(os.path.join(gdir, "h5p.json"))
    check(h5p["mainLibrary"] == "H5P.MemoryGame",
          f"{mod}/memoria h5p.json mainLibrary={h5p['mainLibrary']}")
    check(os.path.isfile(os.path.join(gdir, "memoria.html")),
          f"{mod}/memoria: falta memoria.html")


def test_arrastrar(mod, data):
    gdir = os.path.join(GAMES, mod, "arrastrar")
    content = load(os.path.join(gdir, "content", "content.json"))
    task = content["question"]["task"]
    items, zonas = EXPECTED[mod]["arrastrar"]
    check(len(task["elements"]) == items,
          f"{mod}/arrastrar: {len(task['elements'])} items, esperados {items}")
    check(len(task["dropZones"]) == zonas,
          f"{mod}/arrastrar: {len(task['dropZones'])} zonas, esperadas {zonas}")
    zone_names = [z["label"] for z in task["dropZones"]]
    if data is not None:
        check(len(task["elements"]) == len(data["arrastrar"]["items"]),
              f"{mod}/arrastrar: {len(task['elements'])} items != fuente "
              f"{len(data['arrastrar']['items'])}")
        check(len(task["dropZones"]) == len(data["arrastrar"]["zonas"]),
              f"{mod}/arrastrar: {len(task['dropZones'])} zonas != fuente "
              f"{len(data['arrastrar']['zonas'])}")
        check(zone_names == data["arrastrar"]["zonas"],
              f"{mod}/arrastrar: nombres de zona distintos de games.json")
    for j, el in enumerate(task["elements"]):
        check(el["type"]["library"] == "H5P.AdvancedText 1.1",
              f"{mod}/arrastrar item {j}: libreria {el['type']['library']}")
        check(0 <= el["x"] <= 100 and 0 <= el["y"] <= 100,
              f"{mod}/arrastrar item {j}: coordenadas fuera de 0-100")
        if data is not None:
            it = data["arrastrar"]["items"][j]
            check(it["texto"] in el["type"]["params"]["text"],
                  f"{mod}/arrastrar item {j}: texto {it['texto']!r} no esta en el elemento")
            check(el["dropZones"] == [str(zone_names.index(it["zona"]))],
                  f"{mod}/arrastrar item {j}: dropZones {el['dropZones']} "
                  f"!= zona {it['zona']!r}")
        else:
            check(bool(el["type"]["params"].get("text")),
                  f"{mod}/arrastrar item {j}: texto vacío")
            check(all(z in [str(i) for i in range(len(zone_names))]
                      for z in el["dropZones"]),
                  f"{mod}/arrastrar item {j}: dropZones fuera de rango")
    for i, z in enumerate(task["dropZones"]):
        if data is not None:
            want = [str(j) for j, it in enumerate(data["arrastrar"]["items"])
                    if it["zona"] == z["label"]]
        else:
            want = [str(j) for j, el in enumerate(task["elements"])
                    if str(i) in el["dropZones"]]
        check(sorted(z["correctElements"]) == sorted(want),
              f"{mod}/arrastrar zona {i}: correctElements != items de la zona")
        check(0 <= z["x"] <= 100 and 0 <= z["y"] <= 100,
              f"{mod}/arrastrar zona {i}: coordenadas fuera de 0-100")
    h5p = load(os.path.join(gdir, "h5p.json"))
    check(h5p["mainLibrary"] == "H5P.DragQuestion",
          f"{mod}/arrastrar h5p.json mainLibrary={h5p['mainLibrary']}")
    check(os.path.isfile(os.path.join(gdir, "arrastrar.html")),
          f"{mod}/arrastrar: falta arrastrar.html")


def test_crucigrama(mod, data):
    gdir = os.path.join(GAMES, mod, "crucigrama")
    content = load(os.path.join(gdir, "content", "content.json"))
    words = content["words"]
    check(len(words) == EXPECTED[mod]["crucigrama"],
          f"{mod}/crucigrama: {len(words)} palabras, esperadas "
          f"{EXPECTED[mod]['crucigrama']}")
    src = None
    grid = None
    if data is not None:
        check(len(words) == len(data["crucigrama"]["palabras"]),
              f"{mod}/crucigrama: {len(words)} palabras != fuente "
              f"{len(data['crucigrama']['palabras'])}")
        grid = data["crucigrama"]["cuadricula"]
        src = {(u["palabra"].upper(), u["pista"]): u
               for u in data["crucigrama"]["ubicaciones"]}
    for w in words:
        check(re.fullmatch(r"[A-Z]+", w["answer"]) is not None,
              f"{mod}/crucigrama: respuesta no normalizada a MAYUSCULAS: "
              f"{w['answer']!r}")
        check(w["orientation"] in ("across", "down"),
              f"{mod}/crucigrama: orientation invalida {w['orientation']!r}")
        check(w["fixWord"] is True,
              f"{mod}/crucigrama: fixWord no es true en {w['answer']}")
        if src is None:
            continue
        u = src.get((w["answer"], w["clue"]))
        check(u is not None,
              f"{mod}/crucigrama: palabra {w['answer']!r} sin ubicacion fuente")
        if u:
            check(w["row"] == u["fila"] + 1 and w["column"] == u["columna"] + 1,
                  f"{mod}/crucigrama {w['answer']}: fila/columna no es "
                  f"fuente+1 (base 1)")
            dr, dc = (0, 1) if w["orientation"] == "across" else (1, 0)
            ok = True
            for k, ch in enumerate(w["answer"]):
                r, c = u["fila"] + dr * k, u["columna"] + dc * k
                if not (0 <= r < len(grid) and 0 <= c < len(grid[0])
                        and grid[r][c].upper() == ch):
                    ok = False
            check(ok, f"{mod}/crucigrama {w['answer']}: no coincide con "
                      f"la rejilla fuente en su ubicacion")
    h5p = load(os.path.join(gdir, "h5p.json"))
    check(h5p["mainLibrary"] == "H5P.Crossword",
          f"{mod}/crucigrama h5p.json mainLibrary={h5p['mainLibrary']}")
    check(os.path.isfile(os.path.join(gdir, "crucigrama.html")),
          f"{mod}/crucigrama: falta crucigrama.html")


def test_sopa(mod, data):
    gdir = os.path.join(GAMES, mod, "sopa")
    content = load(os.path.join(gdir, "content", "content.json"))
    p = content
    check(len(p["words"]) == EXPECTED[mod]["sopa"],
          f"{mod}/sopa: {len(p['words'])} palabras, esperadas "
          f"{EXPECTED[mod]['sopa']}")
    if data is not None:
        check(len(p["words"]) == len(data["sopa"]["palabras"]),
              f"{mod}/sopa: {len(p['words'])} palabras != fuente "
              f"{len(data['sopa']['palabras'])}")
        check([w.upper() for w in data["sopa"]["palabras"]] == p["words"],
              f"{mod}/sopa: palabras generadas != fuente en MAYUSCULAS")
        grid = p["grid"]
        check(grid == [r.upper() for r in data["sopa"]["cuadricula"]],
              f"{mod}/sopa: rejilla generada != fuente en MAYUSCULAS")
    else:
        check(all(w == w.upper() for w in p["words"]),
              f"{mod}/sopa: palabras no en MAYUSCULAS")
        grid = p["grid"]
    for u in p["placements"]:
        w, (dr, dc) = u["palabra"], u["direccion"]
        spelled = "".join(
            grid[u["fila"] + dr * k][u["columna"] + dc * k]
            for k in range(len(w)))
        check(spelled == w,
              f"{mod}/sopa: ubicacion de {w} deletrea {spelled!r}")
    check(os.path.isfile(os.path.join(gdir, "sopa.html")),
          f"{mod}/sopa: falta sopa.html")


def test_deps(mod, game):
    h5p = load(os.path.join(GAMES, mod, game, "h5p.json"))
    m, maj, mn = MAIN_LIB[game]
    dep = next((d for d in h5p["preloadedDependencies"]
                if d["machineName"] == m), None)
    check(dep is not None and dep["majorVersion"] == maj
          and dep["minorVersion"] == mn,
          f"{mod}/{game}: h5p.json no declara {m} {maj}.{mn}")
    for d in h5p["preloadedDependencies"]:
        folder = lib_folder(d["machineName"], d["majorVersion"],
                            d["minorVersion"])
        check(os.path.isdir(folder),
              f"{mod}/{game}: falta libreria {folder}")
        if os.path.isdir(folder):
            lib = load(os.path.join(folder, "library.json"))
            check((lib["majorVersion"], lib["minorVersion"])
                  == (d["majorVersion"], d["minorVersion"]),
                  f"{mod}/{game}: version de {folder} no coincide")
            for js in lib.get("preloadedJs", []):
                check(os.path.isfile(os.path.join(folder, js["path"])),
                      f"{mod}/{game}: falta JS {js['path']} en {folder}")
            for css in lib.get("preloadedCss", []):
                check(os.path.isfile(os.path.join(folder, css["path"])),
                      f"{mod}/{game}: falta CSS {css['path']} en {folder}")


def test_no_gpl():
    for base in (LIBS, os.path.join(ROOT, "app", "src", "main",
                                    "assets", "h5p-player")):
        for dirpath, _dirs, files in os.walk(base):
            for fn in files:
                if fn.endswith((".js", ".css", ".json", ".txt", ".md")):
                    p = os.path.join(dirpath, fn)
                    try:
                        txt = open(p, encoding="utf-8",
                                   errors="ignore").read()
                    except OSError:
                        continue
                    check("GNU GENERAL PUBLIC LICENSE" not in txt
                          and "GNU LESSER GENERAL PUBLIC LICENSE" not in txt
                          and "GNU AFFERO GENERAL PUBLIC LICENSE" not in txt,
                          f"Texto GPL encontrado en {p}")


def main():
    content = CONTENT_DEFAULT
    if "--content" in sys.argv:
        content = sys.argv[sys.argv.index("--content") + 1]
    if "--root" in sys.argv:
        global ROOT, GAMES, LIBS
        ROOT = sys.argv[sys.argv.index("--root") + 1]
        GAMES = os.path.join(ROOT, "app", "src", "main", "assets",
                             "h5p-games")
        LIBS = os.path.join(ROOT, "app", "src", "main", "assets",
                            "h5p-libraries")
    totals = {"memoria": 0, "arrastrar": 0, "crucigrama": 0, "sopa": 0}
    for mod in MODULOS:
        src = os.path.join(content, mod, "games.json")
        if os.path.isfile(src):
            with open(src, encoding="utf-8") as f:
                data = json.load(f)
        else:
            print(f"AVISO: sin fuente {src}; verificando solo el contenido "
                  f"generado contra los conteos esperados.")
            data = None
        test_memoria(mod, data)
        test_arrastrar(mod, data)
        test_crucigrama(mod, data)
        test_sopa(mod, data)
        for game in ("memoria", "arrastrar", "crucigrama"):
            test_deps(mod, game)
        if data is not None:
            totals["memoria"] += len(data["memoria"]["pares"])
            totals["arrastrar"] += len(data["arrastrar"]["items"])
            totals["crucigrama"] += len(data["crucigrama"]["palabras"])
            totals["sopa"] += len(data["sopa"]["palabras"])
        else:
            totals["memoria"] += EXPECTED[mod]["memoria"]
            totals["arrastrar"] += EXPECTED[mod]["arrastrar"][0]
            totals["crucigrama"] += EXPECTED[mod]["crucigrama"]
            totals["sopa"] += EXPECTED[mod]["sopa"]
    check(totals == {"memoria": 53, "arrastrar": 58,
                     "crucigrama": 100, "sopa": 95},
          f"Totales {totals} != 53/58/100/95")
    check(os.path.isfile(os.path.join(GAMES, "valhalla-theme.js")),
          "falta valhalla-theme.js")
    check(os.path.isfile(os.path.join(GAMES, "valhalla-h5p.css")),
          "falta valhalla-h5p.css")
    test_no_gpl()
    print(f"Totales verificados: {totals}")
    if FAILURES:
        print(f"\n{len(FAILURES)} FALLOS")
        return 1
    print("TODOS LOS TESTS PASARON")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
