#!/usr/bin/env python3
"""Genera los 20 minijuegos de Valhalla School con H5P real (v1.2.0).

Lee el contenido FUENTE en formato neutral canonico:
    <content>/h5p/<modulo>/games.json   (m1-elementos ... m5-relaciones)
y emite en app/src/main/assets/h5p-games/<modulo>/:
  memoria.html + memoria/content.json + memoria/content/images/*.png
      H5P.MemoryGame 1.3.53 real (h5p-standalone). Cada par texto<->texto
      se convierte en dos imagenes (termino y definicion) dibujadas con PIL,
      porque MemoryGame solo acepta cartas con imagen.
  arrastrar.html + arrastrar/content.json
      H5P.DragQuestion 1.15.37 real. El games.json trae zonas+items; aqui
      se reestructura al formato 1.15 (question.task.elements/dropZones,
      indices como strings) y se calculan las coordenadas.
  crucigrama.html + crucigrama/content.json
      H5P.Crossword 0.7.6 real. Las palabras se normalizan a MAYUSCULAS
      (hay una «INTROTImIA» con m minuscula en m4), fila/columna pasan de
      base 0 a base 1, horizontal->across, vertical->down, fixWord=true.
  sopa.html + sopa/content.json
      Sopa propia de Valhalla (no existe tipo H5P oficial; Find the Words
      es GPL y esta prohibido). Rejilla y ubicaciones fijas de games.json,
      seleccion por arrastre con el dedo, adaptada del patron de
      wordsearch.js de Pixel-Arcade (MIT).

Al completar un juego, la pagina llama a window.Android.gameCompleted()
(puente de GameScreen, +10 XP) — en los 3 H5P por eventos xAPI
('completed', o 'answered' con success) y en la sopa directamente.

Uso: python3 tools/gen_h5p.py [--content /ruta/a/content] [--root .]
  La ruta del contenido es ABSOLUTA por defecto
  (~/workspace/socionics-game/content/h5p): el generador viejo calculaba
  rutas relativas y eso se rompe en un worktree.
"""
import json
import os
import sys
import textwrap

CONTENT_DEFAULT = os.path.expanduser("~/workspace/socionics-game/content/h5p")
ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT = os.path.join(ROOT, "app", "src", "main", "assets", "h5p-games")

MODULOS = ["m1-elementos", "m2-modelo-a", "m3-tipos-cuadras",
           "m4-dicotomias", "m5-relaciones"]

# ------------------------------------------------------------------ imagenes
CARD = 600
BG = (4, 7, 15)          # #04070f
GOLD = (217, 164, 65)    # #d9a441
CREAM = (245, 239, 224)  # #f5efe0
SURF = (14, 52, 38)      # #0e3426 (Navidad)


def _font(bold, size):
    from PIL import ImageFont
    cands = [
        "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf" if bold
        else "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf",
    ]
    return ImageFont.truetype(cands[0], size)


def card_image(text, path, term):
    """Dibuja una carta cuadrada 600x600 con el texto centrado."""
    from PIL import Image, ImageDraw
    img = Image.new("RGB", (CARD, CARD), BG)
    d = ImageDraw.Draw(img)
    d.rounded_rectangle([14, 14, CARD - 14, CARD - 14], radius=42,
                        fill=SURF, outline=GOLD, width=8)
    if term:
        size = 200
        while size > 60:
            f = _font(True, size)
            if d.textlength(text, font=f) <= CARD - 120:
                break
            size -= 10
        f = _font(True, size)
        d.text((CARD / 2, CARD / 2), text, font=f, fill=GOLD,
               anchor="mm")
    else:
        size = 64
        lines = []
        while size > 30:
            f = _font(True, size)
            # ancho aprox: caracteres por linea segun el tamano
            width_chars = max(8, int((CARD - 140) / (size * 0.58)))
            lines = textwrap.wrap(text, width=width_chars)
            if len(lines) * size * 1.35 <= CARD - 160:
                break
            size -= 4
        f = _font(True, size)
        total = len(lines) * size * 1.35
        y = (CARD - total) / 2 + size * 0.65
        for ln in lines:
            d.text((CARD / 2, y), ln, font=f, fill=CREAM, anchor="mm")
            y += size * 1.35
    img.save(path, "PNG", optimize=True)


def _img_entry(fname):
    return {"path": "images/" + fname, "mime": "image/png",
            "width": CARD, "height": CARD, "copyright": {"license": "U"}}


# ------------------------------------------------------------------ memoria
MEM_L10N = {
    "cardTurns": "Turnos", "timeSpent": "Tiempo",
    "feedback": "¡Muy bien!", "tryAgain": "Reintentar",
    "closeLabel": "Cerrar", "playAudio": "Reproducir audio",
    "pauseAudio": "Pausar audio",
    "label": "Juego de memoria. Encuentra las parejas.",
    # OJO: en el semantics.json de MemoryGame este campo se llama tambien
    # 'label' por error (deberia ser 'labelInstructions'); el codigo lee
    # l10n.labelInstructions, asi que se pone con ese nombre.
    "labelInstructions": "Usa las flechas izquierda y derecha para moverte "
                         "entre las cartas. Usa espacio o Enter para voltear "
                         "una carta.",
    "done": "Se encontraron todas las cartas.",
    "cardPrefix": "Carta %num de %total:",
    "cardUnturned": "Sin voltear. Toca para voltear.",
    "cardTurned": "Volteada.", "cardMatched": "¡Pareja encontrada!",
    "cardMatchedA11y": "¡Tus cartas coinciden!",
    "cardNotMatchedA11y": "Tus cartas no coinciden. Intenta con otras.",
}


def memoria_content(data, imgdir):
    cards = []
    for i, (term, defi) in enumerate(data["memoria"]["pares"]):
        fa, fb = f"card-{i:02d}a.png", f"card-{i:02d}b.png"
        card_image(term, os.path.join(imgdir, fa), term=True)
        card_image(defi, os.path.join(imgdir, fb), term=False)
        cards.append({
            "image": _img_entry(fa), "imageAlt": term,
            "match": _img_entry(fb), "matchAlt": defi,
            "description": defi,
        })
    # OJO: content.json lleva los params DIRECTOS, sin envoltura {"params"}:
    # h5p-standalone pasa el contenido de content.json tal cual al
    # constructor del tipo (asi lo trae su propio fixture de pruebas).
    return {
        "cards": cards,
        "behaviour": {"useGrid": True, "allowRetry": True},
        "l10n": MEM_L10N,
    }


# ---------------------------------------------------------------- arrastrar
DRAG_L10N = {
    "scoreShow": "Comprobar", "submit": "Enviar",
    "tryAgain": "Reintentar",
    "scoreExplanation": "Las respuestas correctas dan +1 punto. "
                        "Las incorrectas dan -1 punto. "
                        "La puntuación más baja posible es 0.",
}
DRAG_LOCALIZE = {
    "fullscreen": "Pantalla completa",
    "exitFullscreen": "Salir de pantalla completa",
    "grabbablePrefix": "Arrastrable {num} de {total}.",
    "grabbableSuffix": "Colocado en la zona {num}.",
    "dropzonePrefix": "Zona {num} de {total}.",
    "noDropzone": "Sin zona.", "tipLabel": "Mostrar pista.",
    "tipAvailable": "Pista disponible",
    "correctAnswer": "Respuesta correcta",
    "wrongAnswer": "Respuesta incorrecta",
    "feedbackHeader": "Comentarios",
    "scoreBarLabel": "Tienes :num de :total puntos",
    "scoreExplanationButtonLabel": "Mostrar explicación de la puntuación",
    "a11yCheck": "Comprobar las respuestas. Las respuestas se marcarán.",
    "a11yRetry": "Reintentar la tarea. Se reinician las respuestas.",
}


def arrastrar_content(data):
    # UNIDADES (critico, verificado en el codigo de la libreria):
    # x/y van en % del area; width/height van en em (dropzone.js:114-115,
    # draggable.js:219-220). El area es size 360x480; con font-size
    # 16*(W/360)px el area mide 22.5em de ancho x 30em de alto sea cual
    # sea el ancho real W. Por eso se calcula todo en em y se convierte
    # x/y a % (x% = xEm/22.5*100, y% = yEm/30*100).
    zonas = data["arrastrar"]["zonas"]
    items = data["arrastrar"]["items"]
    n = len(zonas)
    W_EM, H_EM = 22.5, 30.0
    GAP = 0.5
    zw = (W_EM - GAP * (n + 1)) / n  # ancho de zona (em)
    drop_zones = []
    for i, z in enumerate(zonas):
        correct = [str(j) for j, it in enumerate(items) if it["zona"] == z]
        drop_zones.append({
            "label": z, "showLabel": True,
            # y=1.5em: la etiqueta va 20px POR ENCIMA de la zona y el area
            # (.h5p-inner, overflow hidden) la recortaria con y=0.6em.
            "x": round((GAP + i * (zw + GAP)) / W_EM * 100, 2),
            "y": round(1.5 / H_EM * 100, 2),
            "width": round(zw, 2), "height": 5.5,
            "correctElements": correct, "single": False, "autoAlign": True,
            # OJO: el semantics lo marca opcional, pero dropzone.js lee
            # dropZone.tipsAndFeedback.tip sin comprobar que exista.
            "tipsAndFeedback": {"tip": "", "feedbackOnCorrect": "",
                                "feedbackOnIncorrect": ""},
        })
    # Items: 4 por fila; si hay textos largos (m4), 2 por fila mas anchos.
    max_len = max(len(it["texto"]) for it in items)
    per_row = 2 if max_len > 12 else 4
    iw = (W_EM - GAP * (per_row + 1)) / per_row
    ih = 3.0 if per_row == 2 else 2.4
    pitch = ih + 0.6
    y0 = 1.5 + 5.5 + 0.9
    elements = []
    for j, it in enumerate(items):
        row, col = divmod(j, per_row)
        elements.append({
            "type": {"library": "H5P.AdvancedText 1.1",
                     "params": {"text": "<p>" + it["texto"] + "</p>"}},
            "x": round((GAP + col * (iw + GAP)) / W_EM * 100, 2),
            "y": round((y0 + row * pitch) / H_EM * 100, 2),
            "width": round(iw, 2), "height": ih,
            "dropZones": [str(zonas.index(it["zona"]))],
            "multiple": False,
        })
    return {
        "scoreShow": DRAG_L10N["scoreShow"],
        "submit": DRAG_L10N["submit"],
        "tryAgain": DRAG_L10N["tryAgain"],
        "scoreExplanation": DRAG_L10N["scoreExplanation"],
        "overallFeedback": [{"from": 0, "to": 100,
                             "feedback": "¡Muy bien!"}],
        "question": {
            "settings": {"size": {"width": 360, "height": 480}},
            "task": {"elements": elements, "dropZones": drop_zones},
        },
        "behaviour": {
            "enableRetry": True, "enableCheckButton": True,
            "singlePoint": False, "applyPenalties": True,
            "enableScoreExplanation": True,
            "dropZoneHighlighting": "dragging", "autoAlignSpacing": 2,
            "enableFullScreen": False, "showScorePoints": True,
            "showTitle": True, "dragHandleVisibility": True,
        },
        "localize": DRAG_LOCALIZE,
    }


# -------------------------------------------------------------- crucigrama
CROSS_L10N = {
    "across": "Horizontal", "down": "Vertical",
    "checkAnswer": "Comprobar", "submitAnswer": "Enviar",
    "tryAgain": "Reintentar", "showSolution": "Mostrar solución",
    "couldNotGenerateCrossword": "No se pudo generar un crucigrama "
                                 "con estas palabras.",
    "couldNotGenerateCrosswordTooFewWords": "No se pudo generar un "
                                            "crucigrama. Se necesitan al "
                                            "menos dos palabras.",
    "probematicWords": "Algunas palabras no se pudieron colocar.",
    "extraClue": "Pista extra", "closeWindow": "Cerrar ventana",
}
CROSS_A11Y = {
    "crosswordGrid": "Crucigrama. Usa las flechas para moverte y el "
                     "teclado para escribir.",
    "column": "Columna", "row": "Fila",
    "across": "Horizontal", "down": "Vertical", "empty": "Vacía",
    "resultFor": "Resultado de: @clue",
    "correct": "Correcta", "wrong": "Incorrecta", "point": "punto",
    "solutionFor": "Para @clue la solución es: @solution",
    "extraClueFor": "Abrir pista extra de @clue",
    "letterSevenOfNine": "Letra @position de @length",
    "lettersWord": "Palabra de @length letras",
    "check": "Comprobar los caracteres.",
    "submitAndcheck": "Enviar la respuesta y comprobar los caracteres.",
    "showSolution": "Mostrar la solución.",
    "retry": "Reintentar la tarea.",
    "yourResult": "Tienes @score de @total puntos",
}


def crucigrama_content(data):
    words = []
    for u in data["crucigrama"]["ubicaciones"]:
        words.append({
            "clue": u["pista"],
            "answer": u["palabra"].upper(),  # «INTROTImIA» -> INTROTIMIA
            "fixWord": True,
            "row": u["fila"] + 1,       # semantics: base 1
            "column": u["columna"] + 1,  # semantics: base 1
            "orientation": ("across" if u["direccion"] == "horizontal"
                            else "down"),
        })
    return {
        "taskDescription": "<p>Completa las palabras con las pistas.</p>",
        "words": words,
        "overallFeedback": {"overallFeedback": [
            {"from": 0, "to": 100, "feedback": "¡Muy bien!"}]},
        "theme": {"backgroundColor": "#0e3426"},
        "behaviour": {
            "poolSize": len(words), "enableInstantFeedback": False,
            "scoreWords": True, "applyPenalties": False,
            "enableRetry": True, "enableSolutionsButton": True,
            "keepCorrectAnswers": False,
            "addExtraMarkerForEmptyCells": False,
        },
        "l10n": CROSS_L10N,
        "a11y": CROSS_A11Y,
    }


# -------------------------------------------------------------------- h5p
def h5p_json(title, main_library, deps):
    return {
        "title": title,
        "language": "es",
        "mainLibrary": main_library,  # sin version: asi lo espera el player
        "embedTypes": ["iframe"],
        "license": "U",
        "authors": [{"name": "Valhalla Typology", "role": "Author"}],
        "preloadedDependencies": [
            {"machineName": m, "majorVersion": maj, "minorVersion": mn}
            for (m, maj, mn) in deps
        ],
    }


MEM_DEPS = [("H5P.MemoryGame", 1, 3), ("H5P.Timer", 0, 4),
            ("FontAwesome", 4, 5), ("H5P.Components", 1, 0)]
DRAG_DEPS = [("H5P.DragQuestion", 1, 15), ("FontAwesome", 4, 5),
             ("jQuery.ui", 1, 10), ("H5P.JoubelUI", 1, 3),
             ("H5P.Question", 1, 5), ("H5P.Components", 1, 0),
             ("H5P.Transition", 1, 0), ("H5P.FontIcons", 1, 0),
             # Subcontenido de los elementos (no es dependencia transitiva
             # de DragQuestion: hay que declararlo aqui o el player no lo
             # carga y los elementos salen sin texto).
             ("H5P.AdvancedText", 1, 1)]
CROSS_DEPS = [("H5P.Crossword", 0, 7), ("H5P.Question", 1, 5),
              ("H5P.JoubelUI", 1, 3), ("H5P.Image", 1, 1),
              ("FontAwesome", 4, 5), ("H5P.Components", 1, 0),
              ("H5P.Transition", 1, 0), ("H5P.FontIcons", 1, 0)]

TITLES = {"memoria": "Memoria", "arrastrar": "Arrastra y clasifica",
          "crucigrama": "Crucigrama", "sopa": "Sopa de letras"}
MAIN_LIB = {"memoria": "H5P.MemoryGame",
            "arrastrar": "H5P.DragQuestion",
            "crucigrama": "H5P.Crossword"}
DEPS = {"memoria": MEM_DEPS, "arrastrar": DRAG_DEPS,
        "crucigrama": CROSS_DEPS}


def player_page(title):
    return f"""<!DOCTYPE html>
<html lang="es"><head><meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>{title}</title>
<link rel="stylesheet" href="../../../h5p-player/styles/h5p.css">
<link rel="stylesheet" href="../../valhalla-h5p.css">
<script src="../../valhalla-theme.js"></script>
</head><body>
<div id="h5p-container"></div>
<div id="win" hidden><b>¡Completado! 🎉</b><br><span>+10 XP ganados</span></div>
<div id="h5p-error" hidden></div>
<script src="../../../h5p-player/main.bundle.js" charset="UTF-8"></script>
<script>
ValhallaTheme.apply();
var h5p = new H5PStandalone.H5P(document.getElementById('h5p-container'), {{
  h5pJsonPath: '.',
  librariesPath: '../../../h5p-libraries',
  frameJs: '../../../h5p-player/frame.bundle.js',
  frameCss: '../../../h5p-player/styles/h5p.css',
  embedType: 'div',
  copyright: false, export: false, embed: false, icon: false,
  fullScreen: false
}});
h5p.then(function () {{
  if (window.H5P && H5P.externalDispatcher) {{
    // OJO: el tipo de evento es 'xAPI' (asi lo crea H5P.XAPIEvent en el
    // core que trae h5p-standalone); 'xapi' en minuscula no llega nunca.
    var onXapi = function (event) {{
      var st = event && event.data && event.data.statement;
      if (!st || !st.verb || !st.verb.id) return;
      var verb = st.verb.id;
      var res = st.result || {{}};
      if (verb.indexOf('completed') !== -1 ||
          (verb.indexOf('answered') !== -1 && res.success === true)) {{
        ValhallaTheme.win();
      }}
    }};
    H5P.externalDispatcher.on('xAPI', onXapi);
    H5P.externalDispatcher.on('xapi', onXapi);
  }}
}}).catch(function (err) {{
  var e = document.getElementById('h5p-error');
  e.hidden = false;
  e.textContent = 'No se pudo cargar el juego: ' + err;
}});
</script>
</body></html>
"""


def sopa_page(data):
    s = data["sopa"]
    grid = [row.upper() for row in s["cuadricula"]]
    words = [w.upper() for w in s["palabras"]]
    placements = [{
        "palabra": u["palabra"].upper(), "fila": u["fila"],
        "columna": u["columna"], "direccion": u["direccion"],
    } for u in s["ubicaciones"]]
    return f"""<!DOCTYPE html>
<html lang="es"><head><meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>Sopa de letras: {data["nombre"]}</title>
<link rel="stylesheet" href="../../valhalla-h5p.css">
<script src="../../valhalla-theme.js"></script>
</head><body>
<h1>Sopa de letras: {data["nombre"]}</h1>
<p class="sub">Encuentra las palabras escondidas.</p>
<div id="ws-grid"></div>
<h3>Palabras por encontrar</h3>
<ul id="ws-words"></ul>
<p class="muted" id="ws-status">Arrastra el dedo desde la primera letra hasta la última de cada palabra.</p>
<div id="win" hidden><b>¡Completado! 🎉</b><br><span>+10 XP ganados</span></div>
<script>
ValhallaTheme.apply();
var GRID = {json.dumps(grid, ensure_ascii=False)};
var WORDS = {json.dumps(words, ensure_ascii=False)};
var PLACEMENTS = {json.dumps(placements, ensure_ascii=False)};
(function () {{
  var R = GRID.length, C = GRID[0].length;
  var gridEl = document.getElementById('ws-grid');
  gridEl.style.gridTemplateColumns = 'repeat(' + C + ', 1fr)';
  var cells = {{}}, found = new Set(), start = null, dragging = false;
  for (var r = 0; r < R; r++) for (var c = 0; c < C; c++) {{
    var d = document.createElement('div');
    d.className = 'ws-cell'; d.textContent = GRID[r][c];
    d.dataset.r = r; d.dataset.c = c;
    cells[r + ',' + c] = d; gridEl.appendChild(d);
  }}
  var listEl = document.getElementById('ws-words');
  WORDS.forEach(function (w) {{
    var li = document.createElement('li');
    li.dataset.w = w; li.textContent = w; listEl.appendChild(li);
  }});
  function cellsBetween(a, b) {{
    var dr = b.r - a.r, dc = b.c - a.c;
    var adr = Math.abs(dr), adc = Math.abs(dc);
    if (adr !== 0 && adc !== 0 && adr !== adc) return null;
    var len = Math.max(adr, adc) + 1;
    var sr = dr === 0 ? 0 : dr / adr, sc = dc === 0 ? 0 : dc / adc;
    var out = [];
    for (var k = 0; k < len; k++) out.push({{r: a.r + sr * k, c: a.c + sc * k}});
    return out;
  }}
  function clearSel() {{
    Object.keys(cells).forEach(function (k) {{
      if (!cells[k].classList.contains('found')) cells[k].classList.remove('sel');
    }});
  }}
  function markSel(list) {{
    clearSel();
    (list || []).forEach(function (p) {{
      var cell = cells[p.r + ',' + p.c];
      if (cell && !cell.classList.contains('found')) cell.classList.add('sel');
    }});
  }}
  function evaluate(a, b) {{
    var list = cellsBetween(a, b);
    if (!list) return false;
    var word = list.map(function (p) {{ return GRID[p.r][p.c]; }}).join('');
    var rev = word.split('').reverse().join('');
    var hit = null, fwd = true;
    PLACEMENTS.forEach(function (u) {{
      var dr = u.direccion[0], dc = u.direccion[1];
      if (u.palabra === word && u.fila === a.r && u.columna === a.c) {{ hit = u; fwd = true; }}
      if (u.palabra === rev && u.fila === b.r && u.columna === b.c) {{ hit = u; fwd = false; }}
    }});
    if (!hit || found.has(hit.palabra)) return false;
    found.add(hit.palabra);
    list.forEach(function (p) {{
      var cell = cells[p.r + ',' + p.c];
      cell.classList.remove('sel'); cell.classList.add('found');
    }});
    var li = listEl.querySelector('li[data-w="' + hit.palabra + '"]');
    if (li) li.classList.add('done');
    document.getElementById('ws-status').textContent =
      'Encontradas: ' + found.size + ' / ' + WORDS.length;
    if (found.size === WORDS.length) ValhallaTheme.win();
    return true;
  }}
  function cellFromPoint(x, y) {{
    var el = document.elementFromPoint(x, y);
    if (!el || !el.classList.contains('ws-cell')) return null;
    return {{r: +el.dataset.r, c: +el.dataset.c}};
  }}
  gridEl.addEventListener('pointerdown', function (e) {{
    var p = cellFromPoint(e.clientX, e.clientY);
    if (!p) return;
    e.preventDefault();
    start = p; dragging = false; markSel([p]);
  }});
  gridEl.addEventListener('pointermove', function (e) {{
    if (!start) return;
    var p = cellFromPoint(e.clientX, e.clientY);
    if (!p) return;
    if (p.r !== start.r || p.c !== start.c) dragging = true;
    if (dragging) markSel(cellsBetween(start, p));
  }});
  gridEl.addEventListener('pointerup', function (e) {{
    if (!start) return;
    var p = cellFromPoint(e.clientX, e.clientY) || start;
    if (dragging) {{
      evaluate(start, p);
    }} else if (window.__wsPending) {{
      var a = window.__wsPending; window.__wsPending = null;
      evaluate(a, p);
    }} else {{
      window.__wsPending = p;
      markSel([p]);
      document.getElementById('ws-status').textContent =
        'Ahora toca la última letra de la palabra.';
      start = null;
      return;
    }}
    start = null; dragging = false; clearSel();
  }});
  gridEl.addEventListener('pointercancel', function () {{
    start = null; dragging = false; clearSel();
  }});
}})();
</script>
</body></html>
"""


def write_json(path, obj):
    with open(path, "w", encoding="utf-8") as f:
        json.dump(obj, f, ensure_ascii=False, indent=2)


def main():
    content = CONTENT_DEFAULT
    if "--content" in sys.argv:
        content = sys.argv[sys.argv.index("--content") + 1]
    if "--root" in sys.argv:
        global ROOT, OUT
        ROOT = sys.argv[sys.argv.index("--root") + 1]
        OUT = os.path.join(ROOT, "app", "src", "main", "assets", "h5p-games")
    counts = {}
    for moddir in MODULOS:
        with open(os.path.join(content, moddir, "games.json"),
                  encoding="utf-8") as f:
            data = json.load(f)
        nombre = data["nombre"]
        counts[moddir] = {
            "memoria": len(data["memoria"]["pares"]),
            "arrastrar": (len(data["arrastrar"]["items"]),
                          len(data["arrastrar"]["zonas"])),
            "crucigrama": len(data["crucigrama"]["palabras"]),
            "sopa": len(data["sopa"]["palabras"]),
        }
        # --- memoria
        mdir = os.path.join(OUT, moddir, "memoria")
        os.makedirs(os.path.join(mdir, "content", "images"), exist_ok=True)
        write_json(os.path.join(mdir, "content", "content.json"),
                   memoria_content(data, os.path.join(mdir, "content", "images")))
        write_json(os.path.join(mdir, "h5p.json"),
                   h5p_json(f"Memoria: {nombre}", MAIN_LIB["memoria"],
                            DEPS["memoria"]))
        with open(os.path.join(mdir, "memoria.html"), "w",
                  encoding="utf-8") as f:
            f.write(player_page(f"Memoria: {nombre}"))
        # --- arrastrar
        adir = os.path.join(OUT, moddir, "arrastrar")
        os.makedirs(os.path.join(adir, "content"), exist_ok=True)
        write_json(os.path.join(adir, "content", "content.json"),
                   arrastrar_content(data))
        write_json(os.path.join(adir, "h5p.json"),
                   h5p_json(f"Arrastra y clasifica: {nombre}",
                            MAIN_LIB["arrastrar"], DEPS["arrastrar"]))
        with open(os.path.join(adir, "arrastrar.html"), "w",
                  encoding="utf-8") as f:
            f.write(player_page(f"Arrastra y clasifica: {nombre}"))
        # --- crucigrama
        cdir = os.path.join(OUT, moddir, "crucigrama")
        os.makedirs(os.path.join(cdir, "content"), exist_ok=True)
        write_json(os.path.join(cdir, "content", "content.json"),
                   crucigrama_content(data))
        write_json(os.path.join(cdir, "h5p.json"),
                   h5p_json(f"Crucigrama: {nombre}",
                            MAIN_LIB["crucigrama"], DEPS["crucigrama"]))
        with open(os.path.join(cdir, "crucigrama.html"), "w",
                  encoding="utf-8") as f:
            f.write(player_page(f"Crucigrama: {nombre}"))
        # --- sopa (propia, sin H5P)
        sdir = os.path.join(OUT, moddir, "sopa")
        os.makedirs(os.path.join(sdir, "content"), exist_ok=True)
        write_json(os.path.join(sdir, "content", "content.json"),
                   {"words": [w.upper() for w in data["sopa"]["palabras"]],
                    "grid": [r.upper() for r in data["sopa"]["cuadricula"]],
                    "placements": [{
                        "palabra": u["palabra"].upper(),
                        "fila": u["fila"], "columna": u["columna"],
                        "direccion": u["direccion"],
                    } for u in data["sopa"]["ubicaciones"]]})
        write_json(os.path.join(sdir, "h5p.json"), {
            "title": f"Sopa de letras: {nombre}",
            "language": "es",
            "mainLibrary": "Valhalla.WordSearch 1.0",
            "license": "U",
            "authors": [{"name": "Valhalla Typology", "role": "Author"}],
            "nota": "Juego propio de Valhalla School (MIT), no es un tipo "
                    "H5P: el tipo oficial de sopa (Find the Words) es "
                    "GPL-3.0 y no se usa.",
        })
        with open(os.path.join(sdir, "sopa.html"), "w",
                  encoding="utf-8") as f:
            f.write(sopa_page(data))
        print(moddir, counts[moddir])
    print("Generados 20 juegos en", OUT)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
