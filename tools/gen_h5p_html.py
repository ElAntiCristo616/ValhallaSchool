#!/usr/bin/env python3
"""Genera los 20 minijuegos HTML de Valhalla School.

Lee content/h5p/<modulo>/games.json (formato neutral canónico) y emite
un HTML autocontenido por juego en app/src/main/assets/h5p-games/.

Los juegos son HTML/JS propios de Valhalla School (MIT), no H5P real:
el reproductor H5P no se puede empaquetar offline sin sus librerías y
algunos tipos son GPL. Los content.json/h5p.json quedan en assets como
fuente en formato H5P.

Al ganar, el juego llama a window.Android.gameCompleted() (puente JS
de GameScreen), que da +10 XP en la app.

Uso: python3 tools/gen_h5p_html.py
"""
import json, os, html

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
GAME = os.path.dirname(ROOT)
CONTENT = os.path.join(GAME, "content", "h5p")
OUT = os.path.join(ROOT, "app", "src", "main", "assets", "h5p-games")

CSS = """
body{margin:0;background:#04070f;color:#f5efe0;font-family:sans-serif;padding:16px}
h1{color:#d9a441;font-size:22px;margin:6px 0 4px}
p.sub{color:#9aa3b2;font-size:14px;margin:0 0 16px}
button{background:#d9a441;color:#1a1400;border:none;border-radius:14px;padding:14px 22px;font-size:16px;font-weight:700;margin:14px 0}
button:disabled{background:#22304a;color:#9aa3b2}
#win{display:none;background:#0d1526;border:2px solid #d9a441;border-radius:16px;padding:18px;text-align:center;margin-top:16px}
#win b{color:#d9a441;font-size:20px}
.muted{color:#9aa3b2}
"""

def page(title, sub, body, script):
    return ("<!DOCTYPE html><html lang=\"es\"><head><meta charset=\"utf-8\">"
            "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">"
            "<style>" + CSS + "</style></head><body>"
            f"<h1>{html.escape(title)}</h1><p class=\"sub\">{html.escape(sub)}</p>"
            + body +
            "<div id=\"win\"><b>¡Completado! 🎉</b><br><span>+10 XP ganados</span></div>"
            "<script>" + script + "</script></body></html>")

WIN_JS = """
let _won=false;
function win(){if(_won)return;_won=true;document.getElementById('win').style.display='block';
try{if(window.Android)Android.gameCompleted();}catch(e){}}
"""

def memoria(mod, data):
    pairs = data["memoria"]["pares"]
    body = '<div id="grid" style="display:grid;grid-template-columns:repeat(4,1fr);gap:8px"></div><p class="muted" id="st">Toca dos cartas para encontrar la pareja.</p>'
    script = ("const PAIRS=" + json.dumps(pairs, ensure_ascii=False) + ";\n" + WIN_JS + """
let cards=[];PAIRS.forEach((p,i)=>{cards.push({t:p[0],pair:i});cards.push({t:p[1],pair:i});});
cards.sort(()=>Math.random()-0.5);
const g=document.getElementById('grid');let open=[],matched=0,lock=false;
cards.forEach((c,i)=>{const d=document.createElement('div');
d.textContent='?';d.style.cssText='background:#16233a;border:2px solid #22304a;border-radius:12px;min-height:64px;display:flex;align-items:center;justify-content:center;text-align:center;font-size:13px;font-weight:700;padding:6px;cursor:pointer';
d.onclick=()=>{if(lock||d.dataset.done||d.dataset.open)return;d.dataset.open='1';d.textContent=c.t;d.style.background='#2aa198';d.style.color='#04070f';open.push({d,c});
if(open.length===2){lock=true;const[a,b]=open;open=[];
if(a.c.pair===b.c.pair){setTimeout(()=>{a.d.dataset.done='1';b.d.dataset.done='1';a.d.style.background='#d9a441';b.d.style.background='#d9a441';a.d.style.color='#1a1400';b.d.style.color='#1a1400';matched++;document.getElementById('st').textContent='Parejas: '+matched+' / '+PAIRS.length;lock=false;if(matched===PAIRS.length)win();},400);}
else{setTimeout(()=>{a.d.textContent='?';b.d.textContent='?';a.d.style.background='#16233a';b.d.style.background='#16233a';a.d.style.color='';b.d.style.color='';delete a.d.dataset.open;delete b.d.dataset.open;lock=false;},800);}}};
g.appendChild(d);});
""")
    return page("Memoria: " + mod["nombre"], "Encuentra cada elemento con su definición.", body, script)

def arrastrar(mod, data):
    zonas = data["arrastrar"]["zonas"]
    items = data["arrastrar"]["items"]
    body = ('<div id="items" style="display:flex;flex-wrap:wrap;gap:8px;margin-bottom:14px"></div>'
            + ''.join(f'<div class="zone" data-zone="{html.escape(z)}" style="background:#0d1526;border:2px dashed #2aa198;border-radius:14px;padding:12px;margin-bottom:10px;min-height:56px"><b style="color:#2aa198">{html.escape(z)}</b><div class="slot"></div></div>' for z in zonas)
            + '<button id="chk">Comprobar</button><p class="muted" id="st">Arrastra cada elemento a su zona, o toca uno y luego toca su zona.</p>')
    # Arrastre real con Pointer Events (tactil + raton): presionar, arrastrar
    # con el dedo y soltar sobre el destino. El toque simple (sin mover)
    # sigue funcionando como tocar-tocar, por accesibilidad.
    script = ("const ZONAS=" + json.dumps(zonas, ensure_ascii=False) + ";const ITEMS=" + json.dumps(items, ensure_ascii=False) + ";\n" + WIN_JS + """
let sel=null;const box=document.getElementById('items');
function clearHi(){document.querySelectorAll('.zone').forEach(z=>{z.style.borderColor='#2aa198';z.style.background='#0d1526';});}
ITEMS.slice().sort(()=>Math.random()-0.5).forEach(it=>{const d=document.createElement('div');d.textContent=it.texto;d.dataset.zona=it.zona;
d.style.cssText='background:#16233a;border:2px solid #d9a441;border-radius:12px;padding:10px 14px;font-weight:700;cursor:grab;touch-action:none;user-select:none;-webkit-user-select:none';
let sx=0,sy=0,dragging=false,origParent=null,origNext=null,w=0,h=0;
d.addEventListener('pointerdown',e=>{e.preventDefault();sx=e.clientX;sy=e.clientY;dragging=false;origParent=d.parentNode;origNext=d.nextSibling;w=d.offsetWidth;h=d.offsetHeight;try{d.setPointerCapture(e.pointerId);}catch(err){}});
d.addEventListener('pointermove',e=>{if(!sx&&!sy)return;const dx=e.clientX-sx,dy=e.clientY-sy;
if(!dragging&&Math.hypot(dx,dy)>10){dragging=true;if(sel===d)sel=null;
d.style.position='fixed';d.style.left=(e.clientX-w/2)+'px';d.style.top=(e.clientY-h/2)+'px';d.style.width=w+'px';d.style.zIndex='999';d.style.pointerEvents='none';d.style.opacity='0.92';d.style.boxShadow='0 8px 20px rgba(0,0,0,.5)';d.style.cursor='grabbing';}
if(dragging){d.style.left=(e.clientX-w/2)+'px';d.style.top=(e.clientY-h/2)+'px';clearHi();
const under=document.elementFromPoint(e.clientX,e.clientY);const z=under?under.closest('.zone'):null;
if(z){z.style.borderColor='#d9a441';z.style.background='#12233d';}}});
function endDrag(e,cancelled){const under=(e.clientX!==undefined)?document.elementFromPoint(e.clientX,e.clientY):null;
const z=under?under.closest('.zone'):null;const tray=under?under.closest('#items'):null;
d.style.position='';d.style.left='';d.style.top='';d.style.width='';d.style.zIndex='';d.style.pointerEvents='';d.style.opacity='';d.style.boxShadow='';d.style.cursor='grab';
clearHi();
if(!cancelled&&z){z.querySelector('.slot').appendChild(d);d.style.borderColor='#22304a';}
else if(!cancelled&&tray){box.appendChild(d);d.style.borderColor='#d9a441';}
else{if(origParent)origParent.insertBefore(d,origNext);d.style.borderColor='#d9a441';}
dragging=false;sx=0;sy=0;}
d.addEventListener('pointerup',e=>{if(dragging){endDrag(e,false);}else{sx=0;sy=0;if(sel)sel.style.borderColor='#d9a441';sel=d;d.style.borderColor='#2aa198';}});
d.addEventListener('pointercancel',e=>{if(dragging)endDrag(e,true);dragging=false;sx=0;sy=0;});
box.appendChild(d);});
document.querySelectorAll('.zone').forEach(z=>{z.addEventListener('click',()=>{if(!sel)return;z.querySelector('.slot').appendChild(sel);sel.style.borderColor='#22304a';sel=null;});});
document.getElementById('chk').onclick=()=>{let ok=0,tot=0;
document.querySelectorAll('.zone').forEach(z=>{z.querySelectorAll('.slot > div').forEach(d=>{tot++;const good=d.dataset.zona===z.dataset.zone;d.style.background=good?'#2aa198':'#ff4b4b';d.style.color=good?'#04070f':'#fff';if(good)ok++;});});
document.getElementById('st').textContent='Correctos: '+ok+' / '+ITEMS.length;
if(ok===ITEMS.length&&tot===ITEMS.length)win();};
""")
    return page("Arrastra y clasifica: " + mod["nombre"], "Coloca cada elemento en su zona.", body, script)

def crucigrama(mod, data):
    c = data["crucigrama"]
    grid = c["cuadricula"]
    ubic = sorted(c["ubicaciones"], key=lambda u: (u["fila"], u["columna"]))
    clues = "".join(f'<li><b>{i+1}.</b> {html.escape(u["pista"])} ({len(u["palabra"])} letras)</li>' for i, u in enumerate(ubic))
    rows = len(grid); cols = len(grid[0])
    # Cuadriculas anchas (v1.1.2: crucigramas completos): celdas fijas de 30px
    # con desplazamiento lateral, para que las letras sigan legibles en el
    # telefono. Cuadriculas normales: ancho flexible y fuente segun tamano.
    if cols > 20:
        fs = 15
        grid_html = (f'<div style="overflow-x:auto;max-width:100%;padding-bottom:6px">'
                     f'<div id="grid" style="display:grid;grid-template-columns:repeat({cols},30px);gap:2px;width:max-content"></div></div>'
                     '<p class="muted">Desliza de lado para ver toda la cuadrícula.</p>')
    else:
        fs = 15 if cols <= 15 else (14 if cols <= 18 else 13)
        grid_html = f'<div id="grid" style="display:grid;grid-template-columns:repeat({cols},1fr);gap:2px;max-width:480px"></div>'
    body = (grid_html
            + f'<ol style="color:#f5efe0;font-size:14px;line-height:1.7">{clues}</ol>'
            + '<button id="chk">Comprobar</button><p class="muted" id="st"></p>')
    script = ("const GRID=" + json.dumps(grid, ensure_ascii=False) + ";const UBIC=" + json.dumps(ubic, ensure_ascii=False) + ";\n" + WIN_JS + """
const R=GRID.length,C=GRID[0].length,g=document.getElementById('grid');
const cells={};
for(let r=0;r<R;r++)for(let c=0;c<C;c++){const ch=GRID[r][c];const d=document.createElement('div');
if(ch==='#'){d.style.cssText='background:#04070f;aspect-ratio:1';}
else{const inp=document.createElement('input');inp.maxLength=1;inp.style.cssText='width:100%;aspect-ratio:1;text-align:center;text-transform:uppercase;background:#16233a;color:#f5efe0;border:1px solid #22304a;border-radius:4px;font-size:15px;font-weight:700;padding:0';
inp.dataset.r=r;inp.dataset.c=c;inp.dataset.a=ch;d.appendChild(inp);cells[r+','+c]=inp;}
g.appendChild(d);}
document.getElementById('chk').onclick=()=>{let ok=0;
UBIC.forEach(u=>{const dr=u.direccion==='vertical'?1:0,dc=u.direccion==='horizontal'?1:0;let good=true;
for(let k=0;k<u.palabra.length;k++){const inp=cells[(u.fila+dr*k)+','+(u.columna+dc*k)];if(!inp||inp.value.toUpperCase()!==u.palabra[k]){good=false;break;}}
for(let k=0;k<u.palabra.length;k++){const inp=cells[(u.fila+dr*k)+','+(u.columna+dc*k)];if(inp)inp.style.background=good?'#2aa198':'#ff4b4b';}
if(good)ok++;});
document.getElementById('st').textContent='Palabras correctas: '+ok+' / '+UBIC.length;
if(ok===UBIC.length)win();};
""")
    script = script.replace("font-size:15px", "font-size:%dpx" % fs)
    return page("Crucigrama: " + mod["nombre"], "Completa las palabras con las pistas.", body, script)

def sopa(mod, data):
    s = data["sopa"]
    grid = s["cuadricula"]
    ubic = s["ubicaciones"]
    words = s["palabras"]
    rows = len(grid); cols = len(grid[0])
    wl = "".join(f'<li data-w="{html.escape(w)}" style="margin:4px 0">{html.escape(w)}</li>' for w in words)
    body = (f'<div id="grid" style="display:grid;grid-template-columns:repeat({cols},1fr);gap:2px;max-width:480px"></div>'
            f'<h3 style="color:#d9a441">Palabras por encontrar</h3><ul id="words" style="columns:2">{wl}</ul>'
            '<p class="muted" id="st">Toca la primera letra y luego la última de cada palabra.</p>')
    script = ("const GRID=" + json.dumps(grid, ensure_ascii=False) + ";const UBIC=" + json.dumps(ubic, ensure_ascii=False) + ";\n" + WIN_JS + """
const R=GRID.length,C=GRID[0].length,g=document.getElementById('grid');
const cells={};let start=null,found=0;
for(let r=0;r<R;r++)for(let c=0;c<C;c++){const d=document.createElement('div');d.textContent=GRID[r][c];
d.style.cssText='background:#16233a;border-radius:6px;aspect-ratio:1;display:flex;align-items:center;justify-content:center;font-weight:700;cursor:pointer';
d.dataset.r=r;d.dataset.c=c;
d.onclick=()=>{if(!start){start=d;d.style.background='#2aa198';d.style.color='#04070f';return;}
const r0=+start.dataset.r,c0=+start.dataset.c,r1=+d.dataset.r,c1=+d.dataset.c;
const dr=Math.sign(r1-r0),dc=Math.sign(c1-c0),len=Math.max(Math.abs(r1-r0),Math.abs(c1-c0))+1;
let word='';for(let k=0;k<len;k++)word+=GRID[r0+dr*k][c0+dc*k];
const hit=UBIC.find(u=>u.palabra===word&&u.fila===r0&&u.columna===c0&&u.direccion[0]===dr&&u.direccion[1]===dc);
start.style.background='#16233a';start.style.color='';
if(hit){for(let k=0;k<len;k++){const cell=cells[(r0+dr*k)+','+(c0+dc*k)];cell.style.background='#d9a441';cell.style.color='#1a1400';}
const li=document.querySelector('li[data-w="'+word+'"]');if(li)li.style.textDecoration='line-through';
found++;document.getElementById('st').textContent='Encontradas: '+found+' / '+UBIC.length;
if(found===UBIC.length)win();}
start=null;};
cells[r+','+c]=d;g.appendChild(d);}
""")
    return page("Sopa de letras: " + mod["nombre"], "Encuentra las palabras escondidas.", body, script)

GENS = {"memoria": memoria, "arrastrar": arrastrar, "crucigrama": crucigrama, "sopa": sopa}
NAMES = {"memoria": "Memoria", "arrastrar": "Arrastra y clasifica",
         "crucigrama": "Crucigrama", "sopa": "Sopa de letras"}

def main():
    n = 0
    for moddir in sorted(os.listdir(CONTENT)):
        with open(os.path.join(CONTENT, moddir, "games.json"), encoding="utf-8") as f:
            data = json.load(f)
        outdir = os.path.join(OUT, moddir)
        os.makedirs(outdir, exist_ok=True)
        for game, fn in GENS.items():
            with open(os.path.join(outdir, game + ".html"), "w", encoding="utf-8") as f:
                f.write(fn(data, data))
            n += 1
    print(f"Generados {n} juegos HTML en {OUT}")

if __name__ == "__main__":
    main()
