/* Valhalla School Web (PWA) — v1.0.0 web. Datos: data/*.json (29 docs SCS).
   Lógica espejo de la Android v1.3.7. Sin frameworks, sin CDN. */
'use strict';
const $ = s => document.querySelector(s);
const view = $('#view');
const LS_KEY = 'vs_web_state_v1';
const EL = ['Ne','Ni','Se','Si','Te','Ti','Fe','Fi'];
const EL_NAME = {Ne:'Intuición de energía potencial',Ni:'Intuición del tiempo',Se:'Sensación de fuerza',Si:'Sensación de bienestar',Te:'Lógica del trabajo',Ti:'Lógica de sistemas',Fe:'Ética de emociones',Fi:'Ética de relaciones'};
const POS = ['base','creativa','rol','dolorosa','sugestiva','activación','limitativa','fondo'];
const POS_NAME = {base:'Base',creativa:'Creativa',rol:'Rol',dolorosa:'PoLR',sugestiva:'Sugestiva','activación':'Mobilizing',limitativa:'Observing',fondo:'Demostrativa'};
const DIFF_NAMES = ['Normal','Difícil','Experto','Imposible'];
const TYPES = ['ILE','SEI','ESE','LII','EIE','LSI','SLE','IEI','SEE','ILI','LIE','ESI','LSE','EII','IEE','SLI'];
const QUADRA_OF = {ILE:'Alfa',SEI:'Alfa',ESE:'Alfa',LII:'Alfa',EIE:'Beta',LSI:'Beta',SLE:'Beta',IEI:'Beta',SEE:'Gamma',ILI:'Gamma',LIE:'Gamma',ESI:'Gamma',LSE:'Delta',EII:'Delta',IEE:'Delta',SLI:'Delta'};
const EL_AXIS = [0,0,1,1,2,2,3,3], AXIS_NAMES = ['intuición','sensación','lógica','ética'];
const EL_EXPLICIT = [true,false,true,false,true,false,true,false];
const EL_STATIC = [true,false,true,false,false,true,false,true];
const EL_CAT = ['el del potencial','el del tiempo','el de la apariencia','el del bienestar','el del trabajo','el de las estructuras','el de las emociones','el de las relaciones'];
const EL_CUE = ['mira en qué se puede convertir algo','mira cómo va a cambiar algo con el tiempo','mira cómo se ve algo y qué fuerza tiene','mira cómo se siente algo por dentro','mira cómo se hace el trabajo','mira cómo se ordena todo en un sistema','mira cómo se siente la gente','mira cómo se llevan las personas entre sí'];
const AVATARS = []; TYPES.forEach(t => { for (let i=1;i<=3;i++) AVATARS.push(t+'_Avatar'+i+'.png'); });
const esc = s => String(s==null?'':s).replace(/[&<>"']/g, c => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
const shuffle = a => { a=[...a]; for (let i=a.length-1;i>0;i--){ const j=Math.floor(Math.random()*(i+1)); [a[i],a[j]]=[a[j],a[i]]; } return a; };
const pick = a => a[Math.floor(Math.random()*a.length)];
const clamp = (v,a,b) => Math.max(a, Math.min(b, v));
const todayStr = (d=new Date()) => d.getFullYear()+'-'+String(d.getMonth()+1).padStart(2,'0')+'-'+String(d.getDate()).padStart(2,'0');
const seedFromDate = s => { let h=0; for (const c of s) h=(h*31 + c.charCodeAt(0))>>>0; return h; };
function seededShuffle(arr, seed){ const a=[...arr]; let s=seed>>>0||1; const rnd=()=>{ s=(s*1664525+1013904223)>>>0; return s/4294967296; }; for(let i=a.length-1;i>0;i--){ const j=Math.floor(rnd()*(i+1)); [a[i],a[j]]=[a[j],a[i]]; } return a; }

/* ---------- Datos ---------- */
let D = {}; // books, ideas, quiz_new, practice, themes, companion, detective, lab, tribunal, conexiones, arena, aventura, lluvia, intruso, config
let DATA_OK = false;
async function loadData(){
  const names = ['config','books','ideas','quiz_new','practice','themes','companion','detective','lab','tribunal','conexiones','arena','aventura','lluvia','intruso'];
  const got = await Promise.all(names.map(n => fetch('data/'+n+'.json').then(r => { if(!r.ok) throw new Error(n+' '+r.status); return r.json(); }).catch(e => { console.error('data fail', n, e); return null; })));
  names.forEach((n,i) => D[n]=got[i]);
  DATA_OK = names.every((n,i)=>got[i]!=null);
  if (DATA_OK) {
    // Índice quiz por idea
    D.quizByIdea = {};
    D.quiz_new.forEach(r => { (D.quizByIdea[r.ideaId] = D.quizByIdea[r.ideaId] || []).push(r); });
    D.ideasByLesson = {};
    D.ideas.forEach(it => { (D.ideasByLesson[it.lesson] = D.ideasByLesson[it.lesson] || []).push(it); });
    D.labByType = {}; D.lab.forEach(t => D.labByType[t.type]=t);
    D.fragByCase = {}; D.detective.fragments.forEach(f => { (D.fragByCase[f.case]=D.fragByCase[f.case]||[]).push(f); });
    D.rainByRound = {}; D.lluvia.cards.forEach(c => { (D.rainByRound[c.round]=D.rainByRound[c.round]||[]).push(c); });
  }
}

/* ---------- Estado local (manda lo local; la nube solo sincroniza) ---------- */
function defaultState(){ return {
  xp:0, xpByDay:{}, xpItems:[], lessonsDone:[], readIdeas:[], savedIdeas:[],
  nickname:'', avatarIdx:0, myType:'', theme:'navidad', sound:true, difficulty:0,
  gems:0, frozen:[], gameBest:{}, gameDay:{}, gamesXp:[],
  achievements:[], practiceOk:{}, practiceStreak:0, practiceBestStreak:0,
  correctCount:0, advKeys:{}, scoldBag:{}, scoldLast:null, avatarBag:[],
  arenaBag:{}, auth:null, goal:50
};}
let S = defaultState();
try { const raw = localStorage.getItem(LS_KEY); if (raw) S = Object.assign(defaultState(), JSON.parse(raw)); } catch(e){}
function save(){ try { localStorage.setItem(LS_KEY, JSON.stringify(S)); } catch(e){} }
const displayName = () => (S.nickname||'').trim() || 'Sin nombre';
const levelOf = xp => Math.floor((Math.sqrt(1 + 8*xp/100) - 1)/2) + 1;
const levelBase = l => 50*(l-1)*l;
const todayXP = () => S.xpByDay[todayStr()]||0;
function addXP(n, itemId){
  if (itemId) { if (S.xpItems.includes(itemId)) return false; S.xpItems.push(itemId); }
  S.xp += n; S.xpByDay[todayStr()] = todayXP()+n; save(); updateTopXp(); return true;
}
function streak(){
  let st=0; const d=new Date();
  if (todayXP() < S.goal && !S.frozen.includes(todayStr())) d.setDate(d.getDate()-1);
  while (true){ const k=todayStr(d); if ((S.xpByDay[k]||0) >= S.goal || S.frozen.includes(k)) { st++; d.setDate(d.getDate()-1); } else break; }
  return st;
}
function gameDayCount(gid){ const g=S.gameDay[todayStr()]; return g ? (g[gid]||0) : 0; }
function bumpGameDay(gid, n){ const t=todayStr(); S.gameDay[t] = S.gameDay[t]||{}; S.gameDay[t][gid]=(S.gameDay[t][gid]||0)+(n||1); save(); }
function setBest(gid, score){ if (score > (S.gameBest[gid]||0)) { S.gameBest[gid]=score; save(); return true; } return false; }
function updateTopXp(){ const el=$('#topXpVal'); if (el) el.textContent = S.xp; }

/* ---------- Sonidos (WebAudio, receta exacta de Sfx.java) ---------- */
let AC = null;
function sfx(kind){
  if (!S.sound) return;
  try {
    AC = AC || new (window.AudioContext||window.webkitAudioContext)();
    if (AC.state==='suspended') AC.resume();
    const map = { ok:[[660,880],false,.22], bad:[[220,180],true,.07], yay:[[784,1047,1319],false,.22], angry:[[160,110,80],true,.10], win:[[523,659,784,1047],false,.22] };
    const [freqs, square, vol] = map[kind]||map.ok;
    const sr = AC.sampleRate, step = Math.floor(sr*0.09), dur = Math.floor(sr*0.16);
    const len = step*(freqs.length-1)+dur;
    const buf = AC.createBuffer(1, len, sr), ch = buf.getChannelData(0);
    const attack = sr*0.015;
    for (let i=0;i<freqs.length;i++){ const s=i*step;
      for (let k=0;k<dur;k++){ const env = k<attack ? k/attack : Math.exp(-(k-attack)/(sr*0.045));
        let v = Math.sin(2*Math.PI*freqs[i]*k/sr); if (square) v = v>=0?1:-1;
        ch[s+k] += v*env*vol; } }
    const src = AC.createBufferSource(); src.buffer=buf; src.connect(AC.destination); src.start();
  } catch(e){}
}

/* ---------- Temas y partículas ---------- */
function themeDef(){ return (D.themes?D.themes.themes:[]).find(t=>t.id===S.theme) || {id:'navidad',name:'Navidad',particle:{code:0},colors:{background:'#04140e',surface:'#0e3426',accent:'#d9a441',secondary:'#cd2d32',text:'#f5efe0'}}; }
function applyTheme(){
  const t = themeDef(), c = t.colors;
  const r = document.documentElement.style;
  r.setProperty('--bg', c.background); r.setProperty('--surface', c.surface);
  r.setProperty('--accent', c.accent); r.setProperty('--secondary', c.secondary); r.setProperty('--text', c.text);
  r.setProperty('--accent2', c.accent);
  document.querySelector('meta[name=theme-color]').setAttribute('content', c.background);
  startParticles(t.particle.code, c);
}
let pRAF=null;
function startParticles(code, colors){
  const cv = $('#particles'); if (!cv) return;
  const ctx = cv.getContext('2d');
  const resize = () => { cv.width = innerWidth; cv.height = innerHeight; };
  resize(); window.onresize = resize;
  const N = 50;
  const palette = {0:['#ffffff'],1:['#ffffff','#dfeeff'],2:['#f4a7c3','#ffd6e8','#ffb3cd'],3:['#ff6b35','#ffc53d','#ff8c42'],4:['#bfefff','#7fd4e8'],5:['#ffffff','#ffd166','#c9b8ff'],6:['#9ed69a','#d9e8a3'],7:['#f1cf75','#e87b2e'],8:['#00f5d4','#ff2e88','#ffffff'],9:['#d9822b','#a63d2f','#e8a33d'],10:['#ff8fab','#ffc8dd','#bde0fe','#caffbf'],11:['#7fb8e8','#a8c8e8'],12:['#f5efe0','#d9c9a3'],13:['#7ef0c1','#b388ff','#7fd4e8'],14:['#e0a458','#f0d0a0'],15:['#ffffff','#cdd6ff']};
  const cols = palette[code]||['#ffffff'];
  const P = [];
  for (let i=0;i<N;i++) P.push(newP(true));
  function newP(anyY){ return { x:Math.random()*cv.width, y:anyY?Math.random()*cv.height:-10, r:1+Math.random()*2.6, s:0.2+Math.random()*0.7, ph:Math.random()*6.28, c:pick(cols), drift:(Math.random()-0.5)*0.4 }; }
  if (pRAF) cancelAnimationFrame(pRAF);
  let t=0;
  (function loop(){
    t+=0.016; ctx.clearRect(0,0,cv.width,cv.height);
    for (let i=0;i<P.length;i++){ const p=P[i];
      const rising = (code===3||code===4||code===15);
      p.y += rising ? -p.s : p.s*(code===11?3.2:1);
      p.x += p.drift + (code===2?Math.sin(t+p.ph)*0.4:0) + (code===9?Math.sin(t*0.7+p.ph)*0.5:0);
      if (p.y > cv.height+12) Object.assign(p, newP(false), {y:-10});
      if (p.y < -14) Object.assign(p, newP(false), {y:cv.height+8});
      if (p.x<-12) p.x=cv.width+10; if (p.x>cv.width+12) p.x=-10;
      const tw = (code===5||code===8||code===15) ? 0.45+0.55*Math.abs(Math.sin(t*1.5+p.ph)) : 0.75;
      ctx.globalAlpha = tw;
      if (code===11){ ctx.strokeStyle=p.c; ctx.lineWidth=1; ctx.beginPath(); ctx.moveTo(p.x,p.y); ctx.lineTo(p.x-1.5,p.y+9); ctx.stroke(); }
      else if (code===13){ ctx.fillStyle=p.c; ctx.beginPath(); ctx.arc(p.x,p.y,p.r*2.2,0,6.29); ctx.fill(); }
      else { ctx.fillStyle=p.c; ctx.beginPath(); ctx.arc(p.x,p.y,p.r,0,6.29); ctx.fill(); }
    }
    ctx.globalAlpha=1;
    pRAF=requestAnimationFrame(loop);
  })();
}

/* ---------- Monito acompañante ---------- */
let compAvatar = null, compAngry = false;
function nextAvatar(){
  if (!S.avatarBag.length) S.avatarBag = shuffle(AVATARS.map((_,i)=>i));
  const idx = S.avatarBag.pop(); save();
  compAvatar = AVATARS[idx];
  return compAvatar;
}
function companionHTML(big){
  if (!compAvatar) nextAvatar();
  return '<div class="companion'+(compAngry?' angry':'')+(big?' big':'')+'" id="companion">'
    +'<img src="avatars/'+esc(compAvatar)+'" alt="Acompañante">'
    +'<div class="bubble" id="compBubble">Vamos, '+esc(displayName())+', a ver qué sale esta vez.</div></div>';
}
function compSay(text, angry){
  compAngry = !!angry;
  const c = $('#companion'); if (c) c.classList.toggle('angry', compAngry);
  const b = $('#compBubble'); if (b) b.textContent = text;
}
function cheerText(){
  const arr = D.companion.cheer; let i = Math.floor(Math.random()*arr.length);
  return arr[i].replaceAll('{name}', displayName());
}
function celebrateText(){
  const arr = D.companion.celebrate; let i = Math.floor(Math.random()*arr.length);
  return arr[i].replaceAll('{name}', displayName());
}
/* Bolsa de insultos por elemento: 25 por elemento, sin repetir hasta agotar (persistente). */
function nextScold(element){
  const el = element && EL.includes(element) ? element : pick(EL);
  const bank = D.companion.scoldByElement.find(x=>x.element===el);
  if (!bank) return 'Fallaste, '+displayName()+'.';
  S.scoldBag = S.scoldBag||{};
  let bag = S.scoldBag[el];
  if (!bag || !bag.length){
    bag = shuffle(bank.phrases.map((_,i)=>i));
    if (S.scoldLast && bag[0]===S.scoldLast.idx && el===S.scoldLast.el){ if (bag.length>1) [bag[0],bag[1]]=[bag[1],bag[0]]; }
  }
  const idx = bag.pop(); S.scoldBag[el]=bag; S.scoldLast={el,idx}; save();
  return bank.phrases[idx].replaceAll('{name}', displayName());
}
function compReact(ok, element){
  if (ok){ sfx('yay'); compSay(Math.random()<0.5?cheerText():celebrateText(), false); }
  else { sfx('angry'); compSay(nextScold(element), true); }
}

/* ---------- Toast / navegación ---------- */
let toastT=null;
function toast(msg){ const t=$('#toast'); t.textContent=msg; t.classList.add('show'); clearTimeout(toastT); toastT=setTimeout(()=>t.classList.remove('show'),2600); }
const navStack = [];
function go(fn, title){
  navStack.push({fn, title});
  render();
}
function back(){
  if (navStack.length>1){ navStack.pop(); render(); }
  else showTab(currentTab);
}
function render(){
  const cur = navStack[navStack.length-1];
  $('#backBtn').classList.toggle('hidden', navStack.length<=1);
  $('#topTitle').textContent = cur.title || '⚔️ Valhalla School';
  cur.fn();
  window.scrollTo(0,0);
  updateTopXp();
}
let currentTab = 'home';
function showTab(tab){
  currentTab = tab;
  document.querySelectorAll('.tab').forEach(b => b.classList.toggle('active', b.dataset.tab===tab));
  navStack.length = 0;
  const map = {home:[renderHome,'⚔️ Valhalla School'],challenges:[renderChallenges,'🎯 Retos'],practice:[renderPractice,'🧠 Práctica'],games:[renderGames,'🎮 Juegos'],profile:[renderProfile,'👤 Perfil']};
  const [fn,title] = map[tab]||map.home;
  navStack.push({fn,title}); render();
}
document.querySelectorAll('.tab').forEach(b => b.addEventListener('click', ()=>showTab(b.dataset.tab)));
$('#backBtn').addEventListener('click', back);

/* ---------- INICIO: lecciones ---------- */
function renderHome(){
  const cats = D.books.categories, lessons = D.books.lessons;
  const done = new Set(S.lessonsDone);
  const goal = S.goal, xp = todayXP(), lv = levelOf(S.xp), base = levelBase(lv), next = levelBase(lv+1);
  let h = '<h1>⚔️ Valhalla School</h1>'
    + '<div class="card gold"><div class="row"><div class="grow"><b>Nivel '+lv+' · '+esc(displayName())+'</b>'
    + '<div class="muted">'+S.xp+' XP total · Racha '+streak()+' día'+(streak()===1?'':'s')+'</div></div>'
    + '<div style="font-size:26px">🔥</div></div>'
    + '<div class="bar"><i style="width:'+clamp(100*(S.xp-base)/Math.max(1,next-base),0,100)+'%"></i></div>'
    + '<div class="row"><span class="muted">Meta diaria: '+xp+' / '+goal+' XP</span><span class="grow"></span><span>'+(xp>=goal?'✅ ¡Meta lograda!':'⏳ Faltan '+(goal-xp)+' XP')+'</span></div>'
    + '<div class="bar"><i style="width:'+clamp(100*xp/goal,0,100)+'%"></i></div></div>'
    + '<div class="row" style="margin-bottom:12px"><button class="btn gold grow" id="loopBtn" style="margin:0">🔁 Loop infinito</button></div>'
    + '<p class="muted">47 lecciones · 470 ideas · Aprende sociónica jugando.</p>';
  cats.forEach(c => {
    const ls = lessons.filter(l=>l.category===c.id);
    const nDone = ls.filter(l=>done.has(l.id)).length;
    h += '<div class="cat-head"><span class="swatch" style="background:'+esc(c.color)+'"></span><h2>'+esc(c.name)+'</h2><span class="muted">· '+nDone+'/'+ls.length+'</span></div>';
    ls.forEach(l => {
      const d = done.has(l.id);
      h += '<button class="lesson'+(d?' done':'')+'" data-lesson="'+l.id+'"><span class="dot" style="background:'+(d?'var(--ok)':esc(c.color))+'"></span>'
        + '<span class="t">'+esc(l.title)+'</span><span class="s">'+(d?'✓ Completada':'10 ideas')+'</span></button>';
    });
  });
  view.innerHTML = h;
  $('#loopBtn').onclick = () => go(renderLoop, '🔁 Loop infinito');
  view.querySelectorAll('[data-lesson]').forEach(b => b.onclick = () => go(()=>renderReader(+b.dataset.lesson), '📖 Lección'));
}

/* ---------- Lector de lección ---------- */
function renderReader(lessonId){
  const lesson = D.books.lessons.find(l=>l.id===lessonId);
  const ideas = D.ideasByLesson[lessonId]||[];
  const read = new Set(S.readIdeas);
  let h = '<h1>'+esc(lesson.title)+'</h1><p class="muted">'+esc(lesson.module)+' · '+esc(lesson.categoryName)+'</p>';
  ideas.forEach((it,i) => {
    const isRead = read.has(it.id);
    h += '<div class="card" data-idea="'+it.id+'"><div class="tag">Idea '+(i+1)+' de '+ideas.length+'</div>'
      + '<h3>'+esc(it.title)+'</h3><p>'+esc(it.body)+'</p>'
      + '<div class="row"><button class="btn small read-btn" data-idea="'+it.id+'">'+(isRead?'✓ Leída':'Marcar leída')+'</button>'
      + '<button class="btn small save-btn" data-idea="'+it.id+'">'+(S.savedIdeas.includes(it.id)?'🔖 Guardada':'🔖 Guardar')+'</button></div></div>';
  });
  const allRead = ideas.every(it=>read.has(it.id));
  h += '<button class="btn gold" id="quizBtn">📝 Hacer el quiz ('+ideas.length+' preguntas)</button>'
    + (allRead && !S.lessonsDone.includes(lessonId) ? '<p class="muted">Leíste todas las ideas. El quiz te da +20 XP al completarlo.</p>' : '');
  view.innerHTML = h;
  view.querySelectorAll('.read-btn').forEach(b => b.onclick = () => {
    const id = +b.dataset.idea;
    if (!S.readIdeas.includes(id)) { S.readIdeas.push(id); addXP(5, 'read:'+id); }
    save(); renderReader(lessonId);
  });
  view.querySelectorAll('.save-btn').forEach(b => b.onclick = () => {
    const id = +b.dataset.idea;
    const i = S.savedIdeas.indexOf(id);
    if (i>=0) S.savedIdeas.splice(i,1); else S.savedIdeas.push(id);
    save(); renderReader(lessonId);
  });
  $('#quizBtn').onclick = () => go(()=>renderQuizSession(lessonId, ideas.map(i=>i.id), true), '📝 Quiz');
}

/* ---------- Motor de quiz (Quiz.java: nuevo 62 / fill 28 / book 10, sin repetir tipo) ---------- */
const WORD_RE = /[A-Za-zÁÉÍÓÚÜÑáéíóúüñ][A-Za-zÁÉÍÓÚÜÑáéíóúüñ'-]{3,}/g;
const STOP = new Set('that this with your from into than them they their what when will have more less over only just about make makes does dont doesnt every other others before after where which while being been very much most some de la el en y los las un una con por para se su es del al como mas pero sus este esta son o lo no si ya le les mi tu nos te me yo el ella esto eso hay fue han ser sin sobre entre cuando cada tambien muy todo todos asi porque cual quien donde hasta desde puede pueden tiene tienen esta estan era eran somos suyo suya que'.split(' '));
function wordPool(){
  const s = new Set();
  D.ideas.forEach(it => { const m = it.title.match(WORD_RE)||[]; m.forEach(w => { const lw=w.toLowerCase(); if (!STOP.has(lw)) s.add(lw); }); });
  return [...s];
}
let _wordPool = null;
function fillQ(id){
  const idea = D.ideas.find(x=>x.id===id);
  const hd = idea.title;
  const cands = [...(hd.match(WORD_RE)||[])].filter(w => !STOP.has(w.toLowerCase()));
  if (!cands.length) return bookQ(id);
  const w = pick(cands), lw = w.toLowerCase();
  const L = S.difficulty, cap = L>=3?1 : L===2?2 : 3;
  const lesson = idea.lesson;
  let pool = [];
  (D.ideasByLesson[lesson]||[]).forEach(it => { if (it.id===id) return;
    [...(it.title.match(WORD_RE)||[])].forEach(x => { const xl=x.toLowerCase(); if (xl!==lw && !STOP.has(xl) && Math.abs(xl.length-lw.length)<=cap && !pool.includes(xl)) pool.push(xl); }); });
  if (pool.length<3){ (_wordPool=_wordPool||wordPool()).forEach(x => { if (x!==lw && Math.abs(x.length-lw.length)<=cap && !pool.includes(x)) pool.push(x); }); }
  if (pool.length<3){ (_wordPool||[]).forEach(x => { if (x!==lw && Math.abs(x.length-lw.length)<=3 && !pool.includes(x)) pool.push(x); }); }
  const opts = shuffle([...shuffle(pool).slice(0,3), lw]);
  const at = hd.indexOf(w);
  const head = esc(hd.slice(0,at)) + ' <b style="color:var(--accent2);text-decoration:underline">&nbsp;?&nbsp;</b> ' + esc(hd.slice(at+w.length));
  const body = idea.body.replace(new RegExp('\\b'+w.replace(/[.*+?^${}()|[\]\\]/g,'\\$&')+'\\b','gi'), '___');
  return { id, kind:'fill', prompt:'Completa la palabra que falta', head, body, opts, answer:opts.indexOf(lw), explain:'' };
}
function bookQ(id){
  const idea = D.ideas.find(x=>x.id===id);
  const b = idea.lesson, cat = D.books.lessons[b].category;
  const pool = [], rest = [];
  D.books.lessons.forEach((l,x) => { if (x!==b) (l.category===cat?pool:rest).push(x); });
  const o = shuffle([...shuffle(pool).slice(0,3), ...shuffle(rest).slice(0,Math.max(0,3-shuffle(pool).slice(0,3).length))]);
  if (o.length<3) o.push(...shuffle(rest).slice(0,3-o.length));
  o.push(b); const opts = shuffle(o);
  return { id, kind:'book', prompt:'¿De qué lección es esta idea?', head:esc(idea.title), body:esc(idea.body), opts:opts.map(x=>D.books.lessons[x].title), answer:opts.indexOf(b), explain:'' };
}
function newQ(id, avoidFormat){
  const rows = (D.quizByIdea[id]||[]).filter(r => !avoidFormat || r.format!==avoidFormat);
  const pool = rows.length?rows:(D.quizByIdea[id]||[]);
  if (!pool.length) return fillQ(id);
  const L = S.difficulty;
  let best = -1; pool.forEach(r => { if (r.diff<=L && r.diff>best) best=r.diff; });
  if (best<0) best = Math.min(...pool.map(r=>r.diff));
  const cands = pool.filter(r=>r.diff===best);
  const row = pick(cands);
  const perm = shuffle([0,1,2,3]);
  const opts = perm.map(i=>row.options[i]);
  return { id, kind:'new', sub:row.format, prompt:row.prompt, head:'', body:'', opts, answer:perm.indexOf(row.answer), explain:row.explanation||'' };
}
function pickKind(lastKind, withBook){
  const r = Math.floor(Math.random()*100);
  if (lastKind<0) return r<62?0 : r<90?1 : (withBook?2:0);
  if (lastKind===0) return !withBook?1 : (r<74?1:2);
  if (lastKind===1) return !withBook?0 : (r<86?0:2);
  return r<69?0:1;
}
function makeQuiz(ids, withBook){
  const out=[]; let lastKind=-1, lastSub=null;
  ids.forEach(id => {
    const kind = pickKind(lastKind, withBook);
    let q;
    if (kind===0){ q=newQ(id, lastSub); lastSub=q.sub||null; }
    else if (kind===1){ q=fillQ(id); lastSub=null; }
    else { q=bookQ(id); lastSub=null; }
    lastKind=kind; out.push(q);
  });
  return out;
}

/* Sesión de quiz de lección (con XP) o Loop (sin XP) */
function runQuestionSession(opts){
  // opts: {title, questions, xp:boolean, onFinish(subtitle), itemPrefix}
  let idx=0, correct=0, answered=0, picked=-1, phase='ask';
  const qs = opts.questions;
  function show(){
    if (idx>=qs.length){ finish(); return; }
    const q = qs[idx];
    let h = '<div class="muted">Pregunta '+(idx+1)+' de '+qs.length+' · Aciertos '+correct+'</div>'
      + '<div class="bar"><i style="width:'+(100*idx/qs.length)+'%"></i></div>';
    if (opts.withCompanion) h += companionHTML();
    h += '<div class="card"><div class="qprompt">'+esc(q.prompt)+'</div>';
    if (q.head) h += '<h3>'+q.head+'</h3>';
    if (q.body) h += '<p>'+q.body+'</p>';
    h += '</div>';
    q.opts.forEach((o,i) => { h += '<button class="opt" data-i="'+i+'">'+esc(o)+'</button>'; });
    h += '<div id="fb"></div><button class="btn gold hidden" id="nextBtn">Siguiente ›</button>';
    view.innerHTML = h;
    picked=-1; phase='ask';
    view.querySelectorAll('.opt').forEach(b => b.onclick = () => {
      if (phase!=='ask') return;
      view.querySelectorAll('.opt').forEach(x=>x.classList.remove('sel'));
      b.classList.add('sel'); picked=+b.dataset.i;
      $('#nextBtn').classList.remove('hidden');
      $('#nextBtn').textContent = 'Comprobar';
    });
    $('#nextBtn').onclick = () => {
      if (phase==='ask'){
        if (picked<0) { toast('Elige una opción primero'); return; }
        phase='fb'; answered++;
        const ok = picked===q.answer;
        view.querySelectorAll('.opt').forEach(x => {
          const i=+x.dataset.i; x.disabled=true;
          if (i===q.answer) x.classList.add('correct');
          else if (i===picked) x.classList.add('wrong');
        });
        const el = q.element || null;
        if (ok){ correct++; if (opts.xp) addXP(10, (opts.itemPrefix||'q')+':'+q.id+':'+q.kind); if (opts.onCorrect) opts.onCorrect(q); if (opts.withCompanion) compReact(true); else sfx('yay'); }
        else { if (opts.withCompanion) compReact(false, el); else sfx('angry'); if (opts.onWrong) opts.onWrong(q); }
        let fb = '<div class="expl'+(ok?'':' bad')+'">'+(ok?'✅ ¡Correcto!':'❌ No era esa.')+(q.explain?'<br>'+esc(q.explain):'')+'</div>';
        $('#fb').innerHTML = fb;
        $('#nextBtn').textContent = idx+1>=qs.length ? 'Ver resultado' : 'Siguiente ›';
      } else { idx++; show(); }
    };
  }
  function finish(){
    const acc = answered? Math.round(100*correct/answered):0;
    let h = '<h1>Resultado</h1><div class="card gold"><div style="font-size:40px;text-align:center">'+(acc>=70?'🏆':acc>=40?'🙂':'💪')+'</div>'
      + '<h2 style="text-align:center">'+correct+' de '+answered+' correctas · '+acc+'%</h2>'
      + '<p style="text-align:center">'+esc(opts.finishText||'')+'</p></div>';
    if (opts.xp && correct===answered && answered>0){ addXP(10, 'perfect:'+opts.lessonId); h += '<p>💎 ¡Quiz perfecto! +10 XP extra.</p>'; }
    if (opts.xp && opts.lessonId!=null && !S.lessonsDone.includes(opts.lessonId)){ S.lessonsDone.push(opts.lessonId); addXP(20, 'lesson:'+opts.lessonId); save(); h += '<p>📚 ¡Lección completada! +20 XP.</p>'; }
    if (opts.xp) bumpGameDay('quiz',1);
    h += '<button class="btn gold" id="doneBtn">Volver</button>';
    view.innerHTML = h;
    $('#doneBtn').onclick = () => back();
  }
  show();
}
function renderQuizSession(lessonId, ideaIds, withBook){
  const qs = makeQuiz(ideaIds, withBook);
  runQuestionSession({title:'Quiz', questions:qs, xp:true, lessonId, itemPrefix:'q', finishText:'Quiz de “'+D.books.lessons[lessonId].title+'” terminado.'});
}

/* ---------- LOOP infinito (sin XP, con monito) ---------- */
function renderLoop(){
  const qs = [];
  for (let i=0;i<10;i++){
    if (Math.random()<0.5){
      const id = Math.floor(Math.random()*D.ideas.length);
      qs.push(newQ(id));
    } else {
      const ex = pick(D.practice);
      qs.push(practiceToQ(ex));
    }
  }
  view.innerHTML = '<h1>🔁 Loop infinito</h1><p class="muted">Mezcla de preguntas de lecciones y ejercicios de práctica. Sin XP, sin logros: solo practicar.</p><button class="btn gold" id="startLoop">Empezar</button><button class="btn" id="backLoop">Volver</button>';
  $('#startLoop').onclick = () => {
    runQuestionSession({title:'Loop', questions:qs, xp:false, withCompanion:true, finishText:'Loop terminado. ¿Otra ronda?'});
    // runQuestionSession muestra su propio finish con Volver; añadir "otra ronda" vía back
  };
  $('#backLoop').onclick = () => back();
}

/* ---------- PRÁCTICA ---------- */
function practiceToQ(ex){
  const ans = ex.answer.element+' · '+POS_NAME[ex.answer.position];
  const opts = shuffle([ans, ...ex.distractors.map(d=>d.element+' · '+POS_NAME[d.position])]);
  return { id:ex.id, kind:'practice', prompt:ex.prompt, head:'', body:'', opts, answer:opts.indexOf(ans), explain:ex.justification||'', element:ex.answer.element,
    _ex:ex };
}
let practiceFilter = 0; // 0 todos, 1-4 nivel
function renderPractice(){
  const levels = ['Cimientos','Espejo social','Lo que se anhela','Maestría'];
  let h = '<h1>🧠 Práctica</h1><p class="muted">217 ejercicios: identifica el elemento y su posición. +10 XP por acierto (primera vez).</p>';
  h += '<div><button class="chip'+(practiceFilter===0?' sel':'')+'" data-f="0">Todos</button>';
  levels.forEach((n,i) => h += '<button class="chip'+(practiceFilter===i+1?' sel':'')+'" data-f="'+(i+1)+'">Nivel '+(i+1)+' · '+n+'</button>');
  h += '</div>';
  const okCount = Object.keys(S.practiceOk).length;
  h += '<p class="muted">Aciertos: '+okCount+' · Racha de práctica: '+S.practiceStreak+' (mejor: '+S.practiceBestStreak+')</p>';
  h += '<button class="btn gold" id="startPractice">▶ Empezar (10 ejercicios)</button>';
  h += '<button class="btn" id="loopFromPractice">🔁 Loop infinito</button>';
  view.innerHTML = h;
  view.querySelectorAll('[data-f]').forEach(b => b.onclick = () => { practiceFilter=+b.dataset.f; renderPractice(); });
  $('#startPractice').onclick = () => {
    let pool = D.practice.filter(e => practiceFilter===0 || e.level===practiceFilter);
    pool = shuffle(pool).slice(0,10);
    const qs = pool.map(practiceToQ);
    runQuestionSession({title:'Práctica', questions:qs, xp:true, withCompanion:true, itemPrefix:'p',
      finishText:'Práctica terminada.',
      onCorrect:(q)=>{ S.practiceStreak++; S.practiceBestStreak=Math.max(S.practiceBestStreak,S.practiceStreak); if (q._ex) S.practiceOk[q._ex.id]=1; save(); },
      onWrong:()=>{ S.practiceStreak=0; save(); },
      });
    // envolver: contar aciertos de práctica al terminar no es trivial aquí; el onWrong resetea racha.
  };
  $('#loopFromPractice').onclick = () => go(renderLoop, '🔁 Loop infinito');
}

/* ---------- RETOS ---------- */
const DAILY_POOL = [
  ['🌧️','Juega 3 rondas de Lluvia','lluvia',3,'lluvia'],
  ['🕵️','Resuelve un caso de Detective','detective',1,'detective'],
  ['🧪','Arma un modelo en el Laboratorio','lab',1,'lab'],
  ['🔍','Encuentra 5 intrusos','intruso',5,'intruso'],
  ['⚖️','Juzga 3 argumentos en el Tribunal','tribunal',3,'tribunal'],
  ['🧠','Haz 10 ejercicios de Práctica','practice',10,'practice'],
  ['⚔️','Media 2 combates en la Arena','arena',2,'arena'],
  ['🗺️','Resuelve un acertijo de Aventura','aventura',1,'aventura'],
  ['🔗','Completa 3 encargos de Conexiones','conexiones',3,'conexiones'],
];
function renderChallenges(){
  let h = '<h1>🎯 Retos</h1><div class="card"><div class="row"><span style="font-size:22px">💎</span><b>'+S.gems+' gemas</b></div></div>';
  h += '<h2>Misiones de hoy</h2>';
  const missions = [
    ['⚡','Gana 100 XP hoy', todayXP(), 100],
    ['🔥','Racha de 7 días', streak(), 7],
    ['📚','Completa 5 lecciones', S.lessonsDone.length, 5],
  ];
  missions.forEach(m => {
    const done = m[2]>=m[3];
    h += '<div class="card"><div class="row"><span style="font-size:22px">'+m[0]+'</span><span class="grow"><b>'+esc(m[1])+'</b> · '+Math.min(m[2],m[3])+'/'+m[3]+'</span>'+(done?'✅':'')+'</div><div class="bar"><i style="width:'+Math.min(100,100*m[2]/m[3])+'%"></i></div></div>';
  });
  h += '<h2>Retos de hoy · se renuevan cada día</h2>';
  const defs = seededShuffle(DAILY_POOL, seedFromDate(todayStr())).slice(0,4);
  defs.forEach(d => {
    const cur = gameDayCount(d[2]), done = cur>=d[3];
    h += '<button class="game-card" data-open="'+d[4]+'"><div class="row"><span style="font-size:22px">'+d[0]+'</span><span class="grow"><b>'+esc(d[1])+'</b> · '+Math.min(cur,d[3])+'/'+d[3]+'</span>'+(done?'✅':'›')+'</div><div class="bar"><i style="width:'+Math.min(100,100*cur/d[3])+'%"></i></div></button>';
  });
  h += '<h2>Tienda</h2><div class="card"><div class="row"><span style="font-size:26px">❄️</span><div class="grow"><b>Congelar racha · 50 gemas</b><div class="muted">Tu racha no se rompe mañana aunque no juegues</div></div></div>'
    + '<button class="btn" id="buyFreeze">'+(S.gems>=50?'Comprar':'Te faltan gemas')+'</button></div>';
  view.innerHTML = h;
  $('#buyFreeze').onclick = () => {
    if (S.gems<50){ toast('Te faltan gemas'); return; }
    S.gems-=50; const t=new Date(); t.setDate(t.getDate()+1); S.frozen.push(todayStr(t)); save();
    toast('Racha congelada para mañana ❄️'); renderChallenges();
  };
  view.querySelectorAll('[data-open]').forEach(b => b.onclick = () => {
    const g = b.dataset.open;
    if (g==='practice'){ showTab('practice'); return; }
    showTab('games');
    setTimeout(()=>openGame(g), 50);
  });
}

/* ---------- JUEGOS: hub ---------- */
const GAMES = [
  ['intruso','🔍','Encuentra al intruso','¿Cuál no pertenece? 38 conjuntos, 10 por ronda.', true],
  ['detective','🕵️','Detective sociónico','5 casos, 50 fragmentos: marca el elemento que se ve.'],
  ['lab','🧪','Laboratorio del Modelo A','Arma los 16 modelos clásicos en 8 ranuras.'],
  ['tribunal','⚖️','Tribunal tipológico','Sella 30 argumentos: válido, incorrecto o insuficiente.'],
  ['conexiones','🔗','Conexiones intertipo','20 encargos con la matriz 16×16.'],
  ['arena','⚔️','Arena sociónica','Media entre dos tipos sin que explote la tensión.'],
  ['aventura','🗺️','Aventura por las cuadras','4 regiones, acertijos y puerta final.'],
  ['lluvia','🌧️','Lluvia informacional','Clasifica las fichas antes de que caigan.'],
];
function renderGames(){
  const d = S.difficulty;
  let h = '<h1>🎮 Juegos</h1>'
    + '<div class="switch-row" id="diffSwitch"><span class="fire">🔥</span><div class="grow"><b>MÁS DIFÍCIL</b><div class="muted">Dificultad: '+DIFF_NAMES[d]+'. Toca para subir de nivel.</div></div><span style="font-size:22px">›</span></div>';
  const feat = GAMES[0];
  h += '<button class="game-card featured" data-game="'+feat[0]+'"><div class="tag">⭐ LA MÁS INTERESANTE</div><div class="ico">'+feat[1]+'</div><div class="nm">'+feat[2]+'</div><div class="ds">'+feat[3]+'</div><div class="best">Mejor: '+(S.gameBest[feat[0]]||0)+'</div></button>';
  h += '<div class="grid2">';
  GAMES.slice(1).forEach(g => {
    h += '<button class="game-card" data-game="'+g[0]+'"><div class="ico">'+g[1]+'</div><div class="nm">'+g[2]+'</div><div class="ds">'+g[3]+'</div><div class="best">Mejor: '+(S.gameBest[g[0]]||0)+'</div></button>';
  });
  h += '</div>';
  view.innerHTML = h;
  $('#diffSwitch').onclick = () => {
    S.difficulty = (S.difficulty+1)%4; save();
    toast('Dificultad: '+DIFF_NAMES[S.difficulty]); renderGames();
  };
  view.querySelectorAll('[data-game]').forEach(b => b.onclick = () => openGame(b.dataset.game));
}
function openGame(id){
  const map = {intruso:[renderIntruso,'🔍 Encuentra al intruso'],detective:[renderDetectivePick,'🕵️ Detective sociónico'],lab:[renderLab,'🧪 Laboratorio del Modelo A'],tribunal:[renderTribunal,'⚖️ Tribunal tipológico'],conexiones:[renderConexiones,'🔗 Conexiones intertipo'],arena:[renderArenaPick,'⚔️ Arena sociónica'],aventura:[renderAventura,'🗺️ Aventura por las cuadras'],lluvia:[renderLluviaPick,'🌧️ Lluvia informacional']};
  const m = map[id]; if (m) go(m[0], m[1]);
}
function gameFinish(gid, subtitle, score){
  if (typeof score==='number') setBest(gid, score);
  view.innerHTML = '<h1>'+esc(subtitle.split(':')[0])+'</h1><div class="card gold"><p>'+esc(subtitle)+'</p>'
    + (typeof score==='number' ? '<p>Mejor puntaje: '+(S.gameBest[gid]||0)+'</p>' : '')
    + '</div><button class="btn gold" id="gfBack">Volver a Juegos</button>';
  $('#gfBack').onclick = () => { showTab('games'); };
}
function maxHints(){ return S.difficulty<=0?3 : S.difficulty===1?2 : S.difficulty===2?1 : 0; }

/* ---------- DETECTIVE ---------- */
function detectiveHintQ1(frag){
  const ans = EL.indexOf(frag.answer);
  let expCount=0, statCount=0;
  fragOpts(frag).forEach(o => { const e=EL.indexOf(o);
    if (EL_EXPLICIT[e]===EL_EXPLICIT[ans]) expCount++;
    if (EL_STATIC[e]===EL_STATIC[ans]) statCount++;
  });
  if (expCount>=2) return 'Pregúntate: ¿la persona está mirando algo de fuera — lo que se ve, se hace o se dice — o algo de dentro — lo que se siente o se percibe por dentro?';
  if (statCount>=2) return 'Pregúntate: ¿lo que describe se queda igual, o está cambiando? ¿Es una foto o una película?';
  return 'Lee el fragmento otra vez despacio: ¿qué está HACIENDO la persona? Fíjate en el verbo, no en el tema.';
}
function fragOpts(f){ return [f.answer, f.d1, f.d2, f.d3]; }
function renderDetectivePick(){
  let h = '<h1>🕵️ Detective sociónico</h1>'
    + '<div class="card"><b>CÓMO SE INVESTIGA</b><p>Lee cada fragmento y marca el elemento de información que se ve en él. Puedes marcar fragmentos como evidencia y consultar el expediente en cualquier momento. Al final, concluye el tipo… o di que es indeterminado si los datos no alcanzan.</p></div>';
  D.detective.cases.forEach(c => {
    h += '<button class="game-card" data-case="'+c.id+'"><div class="nm">📁 '+esc(c.title)+'</div><div class="ds">'+esc(c.intro)+'</div><div class="best">10 fragmentos · 3 escenas</div></button>';
  });
  view.innerHTML = h;
  view.querySelectorAll('[data-case]').forEach(b => b.onclick = () => startDetectiveCase(+b.dataset.case));
}
function startDetectiveCase(caseId){
  const c = D.detective.cases.find(x=>x.id===caseId);
  const frags = D.fragByCase[caseId]||[];
  if (!frags.length){ toast('Ese expediente no cargó fragmentos. Elige otro.'); return; }
  let step=0, okCount=0, answered=0, hintLevel=0, hintPartner=-1, hintOut1=-1, hintOut2=-1;
  const marked=[];
  function showIntro(){
    view.innerHTML = '<h1>📁 '+esc(c.title)+'</h1><div class="card"><p>'+esc(c.intro)+'</p><p class="muted">10 fragmentos en 3 escenas. '
      + (maxHints()>0 ? 'Si no sabes por dónde empezar, toca 💡 Pista: primero una pregunta para hacerte, luego descarta dos, luego te deja con dos candidatos. Nunca te dice la respuesta.' : 'Dificultad Imposible: sin pistas.')
      + '</p></div><button class="btn gold" id="dStart">Empezar la investigación</button>';
    $('#dStart').onclick = showFrag;
  }
  function showFrag(){
    if (step>=frags.length){ showConclusion(); return; }
    const f = frags[step];
    hintLevel=0; hintPartner=-1; hintOut1=-1; hintOut2=-1;
    const opts = shuffle(fragOpts(f));
    let h = '<div class="muted">Fragmento '+(step+1)+' de '+frags.length+' · Escena '+f.scene+': '+esc(c.scenes[f.scene-1]||('Escena '+f.scene))+'</div>'
      + '<div class="bar"><i style="width:'+(100*step/frags.length)+'%"></i></div>'
      + companionHTML()
      + '<div class="card"><div class="tag">Evidencia</div><p style="font-size:17px">«'+esc(f.fragment)+'»</p></div>'
      + '<div class="row"><button class="btn small" id="markBtn">📌 Marcar como evidencia</button>'
      + '<button class="btn small" id="fileBtn">📋 Expediente ('+marked.length+')</button></div>'
      + '<div id="fileBox" class="hidden"><div class="card"><p>'+(marked.length?esc(marked.join('\n')):'Aún no marcaste nada en este expediente.')+'</p></div></div>'
      + (maxHints()>0 ? '<button class="btn" id="hintBtn">💡 Pista (0/'+maxHints()+')</button><div id="hintBox"></div>' : '')
      + '<p><b>¿Qué elemento se ve aquí?</b></p>';
    opts.forEach(o => h += '<button class="opt" data-el="'+o+'">'+o+' · '+esc(EL_NAME[o])+'</button>');
    h += '<div id="fb"></div><button class="btn gold hidden" id="nextBtn">Comprobar</button>';
    view.innerHTML = h;
    let picked=null;
    $('#markBtn').onclick = () => { marked.push(f.fragment+' → ?'); toast('Marcado en el expediente'); $('#fileBtn').textContent='📋 Expediente ('+marked.length+')'; };
    $('#fileBtn').onclick = () => $('#fileBox').classList.toggle('hidden');
    const hb = $('#hintBtn');
    if (hb) hb.onclick = () => {
      if (hintLevel>=maxHints()) return;
      hintLevel++;
      const ans = EL.indexOf(f.answer);
      let txt='';
      if (hintLevel===1){
        let partner=-1;
        for (const o of fragOpts(f)){ const e=EL.indexOf(o); if (e!==ans && EL_AXIS[e]===EL_AXIS[ans]){ partner=e; break; } }
        if (partner<0) for (const o of fragOpts(f)){ const e=EL.indexOf(o); if (e!==ans){ partner=e; break; } }
        hintPartner=partner; let n=0;
        for (const o of fragOpts(f)){ const e=EL.indexOf(o); if (e!==ans && e!==partner){ if (n===0) hintOut1=e; else hintOut2=e; n++; } }
        txt = 'Pista 1 — '+detectiveHintQ1(f);
      } else if (hintLevel===2){
        txt = 'Pista 2 — Descarta dos: no es '+EL_CAT[hintOut1]+' ni '+EL_CAT[hintOut2]+'.';
      } else {
        const a=EL_CUE[ans], b=EL_CUE[hintPartner], swap=Math.random()<0.5;
        const first=swap?b:a, second=swap?a:b;
        const axisBit = EL_AXIS[ans]===EL_AXIS[hintPartner] ? 'Los dos son de '+AXIS_NAMES[EL_AXIS[ans]]+': ' : '';
        txt = 'Pista 3 — '+axisBit+'quedan dos: uno '+first+' y el otro '+second+'. ¿Cuál dice el fragmento?';
      }
      $('#hintBox').innerHTML = '<div class="expl">'+esc(txt)+'</div>';
      hb.textContent = '💡 Pista ('+hintLevel+'/'+maxHints()+')'+(hintLevel>=maxHints()?' · ya está todo':'');
    };
    view.querySelectorAll('.opt').forEach(b => b.onclick = () => {
      view.querySelectorAll('.opt').forEach(x=>x.classList.remove('sel'));
      b.classList.add('sel'); picked=b.dataset.el;
      $('#nextBtn').classList.remove('hidden'); $('#nextBtn').textContent='Comprobar';
    });
    $('#nextBtn').onclick = () => {
      const ok = picked===f.answer; answered++;
      view.querySelectorAll('.opt').forEach(x => { x.disabled=true;
        if (x.dataset.el===f.answer) x.classList.add('correct');
        else if (x.dataset.el===picked) x.classList.add('wrong'); });
      if (ok){ okCount++; addXP(10,'det:'+f.id); compReact(true); }
      else compReact(false, f.answer);
      $('#fb').innerHTML = '<div class="expl'+(ok?'':' bad')+'">'+(ok?'✅ ¡Correcto! ':'❌ Era '+f.answer+'. ')+esc(f.justification)+'</div>';
      $('#nextBtn').textContent = step+1>=frags.length?'Concluir el caso ›':'Siguiente fragmento ›';
      $('#nextBtn').onclick = () => { step++; showFrag(); };
    };
  }
  function showConclusion(){
    const acc = answered? Math.round(100*okCount/answered):0;
    let h = '<h1>🔎 Conclusión</h1><div class="card"><p>Fragmentos correctos: '+okCount+' de '+answered+' ('+acc+'%).</p><p>¿Qué tipo es?</p></div>';
    const opts = shuffle([c.typeA, c.typeB, 'Indeterminado']);
    opts.forEach(o => h += '<button class="opt" data-v="'+o+'">'+o+'</button>');
    h += '<div id="fb"></div><button class="btn gold hidden" id="concBtn">Concluir</button>';
    view.innerHTML = h;
    let picked=null;
    view.querySelectorAll('.opt').forEach(b => b.onclick = () => {
      view.querySelectorAll('.opt').forEach(x=>x.classList.remove('sel'));
      b.classList.add('sel'); picked=b.dataset.v; $('#concBtn').classList.remove('hidden');
    });
    $('#concBtn').onclick = () => {
      const correct = c.closingAnswer===2 ? 'Indeterminado' : c.closingAnswer===0 ? c.typeA : c.typeB;
      const ok = picked===correct;
      // Corrección verificada: caso «La biblioteca» (id 3): closingAnswer=1 (IEI) pero el texto decía EII.
      let closing = c.closing;
      if (c.id===3){
        closing = closing.replace('El tipo es EII','El tipo es IEI')
          .replace('Fi base con Ne creativa','Ni base con Fe creativa')
          .replace('IEI comparte la intuición, pero su base es Ni y su creativa Fe','EII comparte la intuición, pero su base es Fi y su creativa Ne');
      }
      if (ok) { addXP(10,'det:case'+c.id); bumpGameDay('detective',1); sfx('win'); }
      else sfx('bad');
      setBest('detective', acc);
      view.innerHTML = '<h1>'+(ok?'✅ Caso resuelto':'❌ Conclusión distinta')+'</h1>'
        + '<div class="card gold"><p>'+esc(closing)+'</p><p class="muted">Tu conclusión: '+esc(picked)+' · Correcta: '+esc(correct)+'</p></div>'
        + '<button class="btn gold" id="backBtn2">Volver a Juegos</button>';
      $('#backBtn2').onclick = () => showTab('games');
    };
  }
  showIntro();
}

/* ---------- LABORATORIO DEL MODELO A ---------- */
const LAB_GRADES = ['Nombres','Letras y símbolos','Mezcla','Solo letras','Reconstrucción parcial','Deducción por pistas','Reparación'];
const LAB_DESC = ['Fichas con el nombre completo del elemento','Fichas con letras y un color por elemento','Mitad nombres, mitad letras','Solo las letras: Ne, Ni, Se, Si, Te, Ti, Fe, Fi','4 posiciones ya puestas, completa las otras 4','Adivina el tipo por acercamientos (sin que te regalen nada) y luego arma su modelo','Un modelo imposible: encuentra el error y arréglalo'];
function renderLab(){
  const dd = S.difficulty;
  const minGrade = dd>=3?7 : dd+1;
  let h = '<h1>🧪 Laboratorio del Modelo A</h1>'
    + '<div class="card"><p>Arma los 16 modelos clásicos, posición por posición. Toca una ficha y luego una ranura.</p>'
    + '<p class="muted">'+(dd>=3?'Dificultad Imposible: solo grado 7.':'Dificultad '+DIFF_NAMES[dd]+': grados '+minGrade+'–7.')+'</p></div>'
    + '<p><b>¿Qué grado quieres practicar?</b></p>';
  for (let g=minGrade; g<=7; g++){
    h += '<button class="opt" data-g="'+g+'"><b>'+g+'. '+LAB_GRADES[g-1]+'</b><br><span class="muted">'+LAB_DESC[g-1]+'</span></button>';
  }
  view.innerHTML = h;
  view.querySelectorAll('[data-g]').forEach(b => b.onclick = () => startLab(+b.dataset.g));
}
function labChipLabel(el, grade){
  if (grade===1) return EL_NAME[el];
  if (grade===2 || grade===4) return el;
  if (grade===3) return (EL.indexOf(el)%2===0)? EL_NAME[el] : el;
  return el;
}
function startLab(grade){
  const type = pick(TYPES);
  const model = D.labByType[type].functions.slice().sort((a,b)=>a.position-b.position).map(f=>f.element);
  const placed = new Array(8).fill(null);
  const locked = new Array(8).fill(false);
  if (grade===5){ [0,1,4,5].forEach(i=>{ placed[i]=model[i]; locked[i]=true; }); }
  if (grade===7){
    for (let i=0;i<8;i++) placed[i]=model[i];
    const a=Math.floor(Math.random()*8); let b=Math.floor(Math.random()*8); while(b===a) b=Math.floor(Math.random()*8);
    [placed[a],placed[b]]=[placed[b],placed[a]];
  }
  let stage = grade===6?1:2, selectedEl=null, tried=false, typeOk=true;
  function hintsText(){
    const percEl = model.slice(0,2).find(e=>EL.indexOf(e)<=3);
    const judEl = model.slice(0,2).find(e=>EL.indexOf(e)>3);
    const percAxis = EL.indexOf(percEl)<=1?'intuición':'sensación';
    const judAxis = EL.indexOf(judEl)<=5?'lógica':'ética';
    const p1 = 'Pista 1: su Ego mezcla '+percAxis+' y '+judAxis+'. ¿Cuál de las dos manda?';
    if (S.difficulty>=2) return p1+'\n¿Qué tipo es?';
    const myQ = QUADRA_OF[type];
    const comboQ=[];
    TYPES.forEach(t => { const mm=D.labByType[t].functions.slice().sort((a,b)=>a.position-b.position).map(f=>f.element);
      const p=mm.slice(0,2).find(e=>EL.indexOf(e)<=3), j=mm.slice(0,2).find(e=>EL.indexOf(e)>3);
      const pa=EL.indexOf(p)<=1?'intuición':'sensación', ja=EL.indexOf(j)<=5?'lógica':'ética';
      if (pa===percAxis && ja===judAxis && !comboQ.includes(QUADRA_OF[t])) comboQ.push(QUADRA_OF[t]); });
    const otherQ = comboQ[0]===myQ?comboQ[1]:comboQ[0];
    const extraQ = ['Alfa','Beta','Gamma','Delta'].find(q=>q!==myQ && q!==otherQ);
    return p1+'\nPista 2: no es de la cuadra '+otherQ+' ni de la cuadra '+extraQ+'.\n¿Qué tipo es?';
  }
  function show(){
    let h = companionHTML();
    if (stage===1){
      h += '<div class="card"><div class="tag">Deducción por pistas</div><p>'+esc(hintsText()).replace(/\n/g,'<br>')+'</p></div><p><b>¿Qué tipo es?</b></p>';
      shuffle(TYPES).forEach(t => h += '<button class="opt" data-t="'+t+'">'+t+' · '+QUADRA_OF[t]+'</button>');
      view.innerHTML = h;
      view.querySelectorAll('[data-t]').forEach(b => b.onclick = () => {
        if (b.dataset.t===type){ typeOk=true; sfx('yay'); compSay(celebrateText(), false); stage=2; show(); }
        else { typeOk=false; sfx('angry'); compSay(nextScold(null), true); toast('No es ese tipo. Lee las pistas otra vez.'); }
      });
      return;
    }
    h += '<div class="card"><div class="tag">Arma el modelo de '+type+'</div><p class="muted">Grado '+grade+' · '+LAB_GRADES[grade-1]+'</p></div>';
    POS.forEach((p,i) => {
      h += '<div class="slot'+(placed[i]?' filled':'')+'" data-slot="'+i+'"><span class="pos">'+POS_NAME[p]+'</span><div class="el">'+(placed[i]?esc(labChipLabel(placed[i],grade)):'—')+'</div></div>';
    });
    h += '<p><b>Fichas:</b></p><div class="tray">';
    EL.forEach(el => {
      const used = placed.includes(el);
      h += '<button class="tile'+(selectedEl===el?' sel':'')+(used?' used':'')+'" data-el="'+el+'">'+esc(labChipLabel(el,grade))+'</button>';
    });
    h += '</div><button class="btn gold" id="checkLab">Comprobar modelo</button><div id="fb"></div>';
    view.innerHTML = h;
    view.querySelectorAll('.tile').forEach(b => b.onclick = () => {
      if (placed.includes(b.dataset.el)){ toast('Esa ficha ya está puesta'); return; }
      selectedEl = selectedEl===b.dataset.el?null:b.dataset.el; show();
    });
    view.querySelectorAll('.slot').forEach(s => s.onclick = () => {
      const i=+s.dataset.slot;
      if (locked[i]){ toast('Esa posición ya viene puesta'); return; }
      if (selectedEl){ placed[i]=selectedEl; selectedEl=null; show(); }
      else if (placed[i]){ placed[i]=null; show(); }
    });
    $('#checkLab').onclick = () => {
      if (placed.some(x=>!x)){ toast('Completa las 8 ranuras'); return; }
      const ok = placed.every((el,i)=>el===model[i]);
      tried=true;
      if (ok){
        addXP(10,'lab:'+type+':g'+grade); bumpGameDay('lab',1); sfx('win'); compSay(celebrateText(), false);
        $('#fb').innerHTML = '<div class="expl">✅ ¡Modelo correcto! '+type+': '+model.join(' · ')+'</div>';
        setTimeout(()=>gameFinish('lab','Laboratorio del Modelo A · grado '+grade+' ('+LAB_GRADES[grade-1]+'). Modelo de '+type+' armado.'), 900);
      } else {
        sfx('angry'); compSay(nextScold(null), true);
        const wrong = placed.map((el,i)=>el===model[i]?null:i).filter(x=>x!==null);
        $('#fb').innerHTML = '<div class="expl bad">❌ Hay '+wrong.length+' ranura(s) mal: '+wrong.map(i=>POS_NAME[POS[i]]).join(', ')+'. Inténtalo otra vez.</div>';
      }
    };
  }
  show();
}

/* ---------- TRIBUNAL ---------- */
function renderTribunal(){
  const d = S.difficulty;
  let all = D.tribunal.arguments.filter(t=>t.diff<=d);
  if (all.length<5) all = D.tribunal.arguments;
  const queue = shuffle(all).slice(0,5);
  let idx=0, step=0, score=0, picked=-1, correctCount=0;
  const TRIB_STOP = new Set('que de la el en y los las un una con por para se su es del al como mas pero sus este esta son o a lo no si ya le les mi tu nos te me yo el ella esto eso hay fue han ser sin sobre entre cuando cada tambien muy todo todos asi porque cual quien donde hasta desde puede pueden tiene tienen esta estan era eran somos suyo suya'.split(' '));
  const words = s => new Set(s.toLowerCase().normalize('NFD').replace(/[^a-z ]/g,'').split(/[^a-z]+/).filter(w=>w.length>=4 && !TRIB_STOP.has(w)));
  const overlap = (a,b) => { const wa=words(a); let n=0; words(b).forEach(w=>{ if(wa.has(w)) n++; }); return n; };
  function show(){
    if (idx>=queue.length){
      setBest('tribunal', score);
      gameFinish('tribunal','Tribunal tipológico: 5 argumentos revisados. Puntaje '+score+'/500.', score);
      return;
    }
    const t = queue[idx];
    let h = '<div class="muted">Argumento '+(idx+1)+' de '+queue.length+' · Puntaje '+score+'</div>'+companionHTML()
      + '<div class="card"><div class="tag">Argumento</div><p style="font-size:17px">«'+esc(t.argument)+'»</p></div>';
    if (step===0){
      h += '<p><b>¿Qué sello le pones?</b></p>';
      D.tribunal.verdictOptions.forEach((v,i) => h += '<button class="opt" data-v="'+i+'">'+v+(i===0?'  ✓':i===1?'  ✗':'  ?')+'</button>');
    } else if (step===1){
      h += '<p><b>El argumento es incorrecto. ¿Dónde está el error?</b></p>';
      D.tribunal.categoryOptions.slice(0,5).forEach((c,i) => h += '<button class="opt" data-v="'+i+'">'+esc(c)+'</button>');
    } else {
      h += '<p><b>Elige la reformulación correcta:</b></p>';
      const distract = D.tribunal.arguments.filter(x=>x.id!==t.id && x.correction && x.correction.trim()!=='—' && x.category===t.category)
        .sort((a,b)=>overlap(t.argument,b.correction)-overlap(t.argument,a.correction));
      const others = distract.length?distract:D.tribunal.arguments.filter(x=>x.id!==t.id && x.correction && x.correction.trim()!=='—');
      const opts = shuffle([t.correction, ...shuffle(others).slice(0,2).map(x=>x.correction)]);
      opts.forEach((o,i) => h += '<button class="opt" data-v="'+i+'" data-txt="'+esc(o)+'">'+esc(o)+'</button>');
      h += '<div id="fb"></div>';
      view.innerHTML = h;
      let sel=null;
      view.querySelectorAll('.opt').forEach(b => b.onclick = () => {
        view.querySelectorAll('.opt').forEach(x=>x.classList.remove('sel'));
        b.classList.add('sel'); sel=b;
        const ok = b.dataset.txt===t.correction;
        if (ok){ score+=30; correctCount++; addXP(10,'trib:'+t.id); compReact(true); }
        else compReact(false, null);
        view.querySelectorAll('.opt').forEach(x=>{ x.disabled=true; if (x.dataset.txt===t.correction) x.classList.add('correct'); else if (x===sel) x.classList.add('wrong'); });
        $('#fb').innerHTML = '<div class="expl'+(ok?'':' bad')+'">'+(ok?'✅ Reformulación correcta. ':'❌ Esa no era. ')+esc(t.explanation)+'</div>'
          + '<button class="btn gold" id="nextTrib">Siguiente ›</button>';
        $('#nextTrib').onclick = () => { idx++; step=0; show(); };
      });
      return;
    }
    h += '<div id="fb"></div><button class="btn gold hidden" id="actBtn">Sellar</button>';
    view.innerHTML = h;
    let sel=null;
    view.querySelectorAll('.opt').forEach(b => b.onclick = () => {
      view.querySelectorAll('.opt').forEach(x=>x.classList.remove('sel'));
      b.classList.add('sel'); sel=b; picked=+b.dataset.v;
      $('#actBtn').classList.remove('hidden');
      $('#actBtn').textContent = step===0?'Sellar':'Comprobar';
    });
    $('#actBtn').onclick = () => {
      if (step===0){
        const ok = picked===t.verdict;
        if (ok){
          score+=40;
          if (t.verdict===1){ step=1; show(); return; }
          correctCount++; addXP(10,'trib:'+t.id); compReact(true);
          $('#fb').innerHTML = '<div class="expl">✅ Sello correcto: '+esc(t.verdictName)+'.</div><button class="btn gold" id="nextTrib">Siguiente ›</button>';
          $('#nextTrib').onclick = () => { idx++; step=0; show(); };
        } else {
          compReact(false, null);
          $('#fb').innerHTML = '<div class="expl bad">❌ Ese sello no es. El veredicto es: '+esc(t.verdictName)+'. '+esc(t.explanation)+'</div><button class="btn gold" id="nextTrib">Siguiente ›</button>';
          $('#nextTrib').onclick = () => { idx++; step=0; show(); };
        }
      } else {
        const ok = picked===t.category;
        if (ok){ score+=30; step=2; show(); }
        else {
          compReact(false, null);
          $('#fb').innerHTML = '<div class="expl bad">❌ No es esa categoría. Es: '+esc(t.categoryName)+'. '+esc(t.explanation)+'</div><button class="btn gold" id="nextTrib">Siguiente ›</button>';
          $('#nextTrib').onclick = () => { idx++; step=0; show(); };
        }
      }
    };
  }
  show();
}

/* ---------- CONEXIONES ---------- */
function pairTail(A, B){
  const mA = D.labByType[A].functions, mB = D.labByType[B].functions;
  const baseA = mA.find(f=>f.position===1).element, baseB = mB.find(f=>f.position===1).element;
  const posAinB = mB.find(f=>f.element===baseA), posBinA = mA.find(f=>f.element===baseB);
  const art = p => p.positionName==='Rol' ? 'el' : 'la';
  return ' En esta pareja, el '+baseA+' de '+A+' es '+art(posAinB)+' '+posAinB.positionName+' de '+B+', y el '+baseB+' de '+B+' es '+art(posBinA)+' '+posBinA.positionName+' de '+A+'.';
}
function relationOf(A, B){ return D.conexiones.matrix[TYPES.indexOf(A)][TYPES.indexOf(B)]; }
function showPair(A, B){
  const rel = D.conexiones.relations[relationOf(A,B)];
  let h = '<h1>'+A+' × '+B+'</h1><div class="card gold"><div class="tag">'+esc(rel.name)+'</div><p>'+esc(rel.description)+pairTail(A,B)+'</p></div>'
    + '<button class="btn" id="backConn">‹ Volver</button>';
  view.innerHTML = h;
  $('#backConn').onclick = () => back();
}
function renderConexiones(){
  let h = '<h1>🔗 Conexiones intertipo</h1><p class="muted">20 encargos: elige al candidato correcto para cada cliente. Toca un encargo para resolverlo, o explora cualquier pareja abajo.</p>';
  D.conexiones.encargos.forEach(e => {
    h += '<button class="game-card" data-enc="'+e.id+'"><div class="nm">📋 '+esc(e.client)+'</div><div class="ds">'+esc(e.goal)+'</div></button>';
  });
  h += '<h2>Explorar parejas</h2><div class="row"><div class="grow"><label>Tipo A</label><select id="selA">'+TYPES.map(t=>'<option>'+t+'</option>').join('')+'</select></div>'
    + '<div class="grow"><label>Tipo B</label><select id="selB">'+TYPES.map(t=>'<option>'+t+'</option>').join('')+'</select></div></div>'
    + '<button class="btn gold" id="showPairBtn">Ver la relación</button>';
  view.innerHTML = h;
  $('#showPairBtn').onclick = () => { const A=$('#selA').value, B=$('#selB').value; go(()=>showPair(A,B), A+' × '+B); };
  view.querySelectorAll('[data-enc]').forEach(b => b.onclick = () => {
    const e = D.conexiones.encargos.find(x=>x.id===b.dataset.enc);
    const opts = shuffle(e.candidates);
    let hh = '<h1>📋 Encargo</h1><div class="card"><p><b>'+esc(e.client)+'</b></p><p>'+esc(e.goal)+'</p><p class="muted">Ya contratado: '+esc(e.hired)+'</p></div><p><b>¿A quién contratas?</b></p>';
    opts.forEach(c => hh += '<button class="opt" data-c="'+c+'">'+c+' · '+QUADRA_OF[c]+'</button>');
    hh += '<div id="fb"></div>';
    view.innerHTML = hh;
    view.querySelectorAll('.opt').forEach(x => x.onclick = () => {
      const ok = x.dataset.c===e.candidates[e.answer];
      view.querySelectorAll('.opt').forEach(y=>{ y.disabled=true; if (y.dataset.c===e.candidates[e.answer]) y.classList.add('correct'); else if (y===x) y.classList.add('wrong'); });
      if (ok){ addXP(10,'conn:'+e.id); bumpGameDay('conexiones',1); sfx('yay'); }
      else sfx('angry');
      const A=e.hired, B=e.candidates[e.answer];
      $('#fb').innerHTML = '<div class="expl'+(ok?'':' bad')+'">'+(ok?'✅ ¡Correcto! ':'❌ No era ese. ')+esc(e.explanation)+'</div>'
        + '<button class="btn gold" id="seePair">Ver la relación '+A+' × '+B+'</button><button class="btn" id="backConn2">‹ Volver a encargos</button>';
      $('#seePair').onclick = () => go(()=>showPair(A,B), A+' × '+B);
      $('#backConn2').onclick = () => renderConexiones();
    });
  });
}

/* ---------- ARENA ---------- */
function arenaFill(text, A, B, p){
  const mA = D.labByType[A].functions.slice().sort((a,b)=>a.position-b.position).map(f=>f.element);
  const mB = D.labByType[B].functions.slice().sort((a,b)=>a.position-b.position).map(f=>f.element);
  const rep = {'{AB}':mA[0],'{BB}':mB[0],'{AC}':mA[1],'{BC}':mB[1],'{AP}':mA[3],'{BP}':mB[3],'{AR}':mA[2],'{BR}':mB[2],'{AS}':mA[4],'{BS}':mB[4],'{AM}':mA[5],'{BM}':mB[5],'{A}':A,'{B}':B,'{P}':p.toLowerCase(),'{R}':''};
  let out=text;
  Object.keys(rep).sort((a,b)=>b.length-a.length).forEach(k => out = out.split(k).join(rep[k]));
  return out;
}
function renderArenaPick(){
  let h = '<h1>⚔️ Arena sociónica</h1><p class="muted">Elige una pareja: 256 pares agrupados por relación. Durante el combate no verás el nombre de la relación: medía por lo que pasa, no por la etiqueta.</p>';
  D.conexiones.relations.forEach(rel => {
    const pairs=[];
    TYPES.forEach(A => TYPES.forEach(B => { if (relationOf(A,B)===rel.code) pairs.push([A,B]); }));
    h += '<details><summary>'+esc(rel.name)+' ('+pairs.length+' parejas)</summary><div class="tray">'
      + pairs.map(p=>'<button class="tile" data-a="'+p[0]+'" data-b="'+p[1]+'">'+p[0]+' × '+p[1]+'</button>').join('')
      + '</div></details>';
  });
  view.innerHTML = h;
  view.querySelectorAll('[data-a]').forEach(b => b.onclick = () => startArena(b.dataset.a, b.dataset.b));
}
function startArena(A, B){
  // OJO: correct/mistake/nearMiss/endGood/endBad/dynamics viven en arena.json,
  // no en conexiones.json (que solo trae description + pairTail).
  const rel = D.arena.relations[relationOf(A,B)];
  const probs = shuffle(D.arena.problems).slice(0,3);
  let turn=0, tension=40, comprension=30, cooperacion=30;
  function showTurn(){
    if (turn>=3){ showTheory(); return; }
    const p = probs[turn];
    const correct = pick(rel.dynamics ? rel.correct : []);
    const mistake = pick(rel.mistake);
    const neutral = pick(D.arena.neutral);
    let opts = [
      {t:arenaFill(correct,A,B,p.title), kind:'correct'},
      {t:arenaFill(mistake,A,B,p.title), kind:'mistake'},
      {t:arenaFill(neutral,A,B,p.title), kind:'neutral'},
    ];
    if (S.difficulty>=2) opts.push({t:arenaFill(pick(rel.nearMiss).text,A,B,p.title), kind:'near'});
    if (S.difficulty>=3) opts[1] = {t:arenaFill(pick(rel.nearMiss).text,A,B,p.title), kind:'near'};
    opts = shuffle(opts);
    let h = '<div class="muted">Combate '+(turn+1)+' de 3 · '+A+' × '+B+'</div>'
      + '<div class="meter"><div class="m"><b>'+tension+'</b><span>Tensión</span></div><div class="m"><b>'+comprension+'</b><span>Comprensión</span></div><div class="m"><b>'+cooperacion+'</b><span>Cooperación</span></div></div>'
      + '<div class="card"><div class="tag">Problema</div><h3>'+esc(p.title)+'</h3><p>'+esc(p.context)+'</p></div>'
      + '<p><b>¿Qué haces?</b></p>';
    opts.forEach((o,i) => h += '<button class="opt" data-i="'+i+'">'+esc(o.t)+'</button>');
    h += '<div id="fb"></div>';
    view.innerHTML = h;
    view.querySelectorAll('.opt').forEach(b => b.onclick = () => {
      const o = opts[+b.dataset.i];
      let reaction='';
      if (o.kind==='correct'){ tension=clamp(tension-20,0,100); comprension=clamp(comprension+20,0,100); cooperacion=clamp(cooperacion+15,0,100); reaction='Funciona: '+rel.dynamics.whatWorks; sfx('yay'); }
      else if (o.kind==='mistake'){ tension=clamp(tension+25,0,100); comprension=clamp(comprension-10,0,100); cooperacion=clamp(cooperacion-10,0,100); reaction='La tensión sube: '+rel.dynamics.whatWorsens; sfx('angry'); }
      else if (o.kind==='near'){ const nm=rel.nearMiss.find(x=>arenaFill(x.text,A,B,p.title)===o.t)||rel.nearMiss[0]; tension=clamp(tension+5,0,100); comprension=clamp(comprension+5,0,100); cooperacion=clamp(cooperacion+5,0,100); reaction='¡Casi! '+nm.why; sfx('bad'); }
      else { tension=clamp(tension+5,0,100); comprension=clamp(comprension+5,0,100); cooperacion=clamp(cooperacion+5,0,100); reaction='Se calman un poco, pero nadie cede.'; sfx('ok'); }
      view.querySelectorAll('.opt').forEach(x=>x.disabled=true);
      let fb = '<div class="expl">'+esc(reaction)+'</div>';
      if (tension>=100) fb += '<div class="expl bad"><b>¡COMBO CÓMICO!</b> La tensión llegó a 100: los dos tipos hablan a la vez, nadie escucha, y el vecino ya tocó la pared. Aún se puede recuperar la discusión.</div>';
      fb += '<button class="btn gold" id="nextTurn">'+(turn+1>=3?'Ver el final ›':'Siguiente problema ›')+'</button>';
      $('#fb').innerHTML = fb;
      $('#nextTurn').onclick = () => { turn++; showTurn(); };
    });
  }
  function showTheory(){
    const good = tension<=40 && (comprension+cooperacion)>=70;
    const key = A+'x'+B;
    S.arenaBag = S.arenaBag||{};
    let bag = S.arenaBag[key];
    const pool = good? rel.endGood : rel.endBad;
    if (!bag || !bag.length) bag = shuffle(pool.map((_,i)=>i));
    const ei = bag.pop(); S.arenaBag[key]=bag; save();
    const ending = pool[ei];
    const weak = (ending.weakFunctionsUsed||[]).join(', ');
    bumpGameDay('arena',1);
    const conf = shuffle([rel, ...shuffle(rel.confusable.map(c=>D.conexiones.relations[c.code])).slice(0,3)]);
    let h = '<h1>'+(good?'🤝 ACUERDO':'🔥 ESCALADA')+'</h1>'
      + '<div class="card gold"><p>'+esc(arenaFill(ending.text,A,B,probs[0].title))+'</p>'
      + (weak?'<p class="muted">Funciones débiles en juego: '+esc(weak)+'</p>':'')
      + '<p class="muted">Tensión '+tension+' · Comprensión '+comprension+' · Cooperación '+cooperacion+'</p></div>'
      + '<div class="card"><p><b>Pregunta de teoría:</b> ¿qué relación hay entre '+A+' y '+B+'?</p></div>';
    conf.forEach(r => h += '<button class="opt" data-r="'+r.code+'">'+esc(r.name)+'</button>');
    h += '<div id="fb"></div>';
    view.innerHTML = h;
    view.querySelectorAll('.opt').forEach(b => b.onclick = () => {
      const ok = +b.dataset.r===rel.code;
      view.querySelectorAll('.opt').forEach(x=>{ x.disabled=true; if (+x.dataset.r===rel.code) x.classList.add('correct'); else if (x===b) x.classList.add('wrong'); });
      if (ok){ addXP(10,'arena:'+A+'x'+B); sfx('win'); } else sfx('bad');
      $('#fb').innerHTML = '<div class="expl'+(ok?'':' bad')+'">'+(ok?'✅ ¡Exacto! ':'❌ No: ')+'la relación entre '+A+' y '+B+' es <b>'+esc(rel.name)+'</b>.</div>'
        + '<button class="btn gold" id="arenaBack">Volver a Juegos</button>';
      $('#arenaBack').onclick = () => showTab('games');
    });
  }
  showTurn();
}

/* ---------- AVENTURA ---------- */
function renderAventura(){
  let h = '<h1>🗺️ Aventura por las cuadras</h1><p class="muted">4 regiones jugables. Resuelve los acertijos, guarda las claves en tu inventario y abre la puerta final (necesita 2 claves).</p>';
  D.aventura.regions.forEach(r => {
    const keys = S.advKeys[r.quadra]||[];
    h += '<button class="game-card" data-region="'+r.quadra+'"><div class="nm">🗺️ Cuadra '+esc(r.quadra)+'</div><div class="ds">'+r.riddles.length+' acertijos · Claves: '+(keys.length?esc(keys.join(', ')):'ninguna')+'</div></button>';
  });
  view.innerHTML = h;
  view.querySelectorAll('[data-region]').forEach(b => b.onclick = () => startAventura(b.dataset.region));
}
function startAventura(quadra){
  const region = D.aventura.regions.find(r=>r.quadra===quadra);
  S.advKeys = S.advKeys||{}; S.advKeys[quadra]=S.advKeys[quadra]||[];
  function showMenu(){
    const keys = S.advKeys[quadra];
    let h = '<h1>🗺️ Cuadra '+esc(quadra)+'</h1><div class="card"><p>Resuélvelos y guarda las claves en tu inventario. La puerta final necesita 2 claves.</p><p>🎒 Inventario: '+(keys.length?esc(keys.join(', ')):'vacío')+'</p></div>';
    region.riddles.forEach((r,i) => {
      const done = keys.includes(r.key);
      h += '<button class="opt" data-i="'+i+'">'+(done?'✓ ':'')+esc(r.point)+'<br><span class="muted">'+(done?'Resuelto — clave: '+esc(r.key):'Un acertijo te espera')+'</span></button>';
    });
    const canOpen = keys.length>=2;
    h += '<button class="opt" data-door="1" '+(canOpen?'':'style="opacity:.55"')+'>🚪 Puerta final<br><span class="muted">'+(canOpen?'Tienes '+keys.length+' claves: puedes intentar abrirla':'Necesitas 2 claves (tienes '+keys.length+')')+'</span></button>';
    view.innerHTML = h;
    view.querySelectorAll('[data-i]').forEach(b => b.onclick = () => showRiddle(+b.dataset.i));
    const door = view.querySelector('[data-door]');
    door.onclick = () => { if (!canOpen){ toast('Necesitas 2 claves'); return; } showDoor(); };
  }
  function showRiddle(i){
    const r = region.riddles[i];
    const opts = shuffle(r.options.map((o,oi)=>({o, oi})));
    let h = companionHTML()+'<div class="card"><div class="tag">'+esc(r.point)+'</div><h3>'+esc(r.question)+'</h3></div>';
    opts.forEach(x => h += '<button class="opt" data-i="'+x.oi+'">'+esc(x.o)+'</button>');
    h += '<div id="fb"></div>';
    view.innerHTML = h;
    view.querySelectorAll('.opt').forEach(b => b.onclick = () => {
      const ok = +b.dataset.i===r.answer;
      view.querySelectorAll('.opt').forEach(x=>{ x.disabled=true; if (+x.dataset.i===r.answer) x.classList.add('correct'); else if (x===b) x.classList.add('wrong'); });
      if (ok){
        if (!S.advKeys[quadra].includes(r.key)){ S.advKeys[quadra].push(r.key); addXP(10,'adv:'+r.point); bumpGameDay('aventura',1); save(); }
        sfx('yay'); compSay(celebrateText(), false);
        $('#fb').innerHTML = '<div class="expl">✅ «'+esc(r.key)+'» guardada en tu inventario. '+esc(r.explanation)+'</div><button class="btn gold" id="backMenu">‹ Volver a la región</button>';
      } else {
        sfx('angry'); compSay(nextScold(null), true);
        $('#fb').innerHTML = '<div class="expl bad">❌ No era esa. '+esc(r.explanation)+'</div><button class="btn gold" id="backMenu">‹ Volver a la región</button>';
      }
      $('#backMenu').onclick = showMenu;
    });
  }
  function showDoor(){
    const d = region.door;
    const opts = shuffle(d.options.map((o,oi)=>({o, oi})));
    let h = '<div class="card"><div class="tag">🚪 Puerta final</div><h3>'+esc(d.question)+'</h3><p class="muted">Claves: '+esc(d.keys)+'</p></div>';
    opts.forEach(x => h += '<button class="opt" data-i="'+x.oi+'">'+esc(x.o)+'</button>');
    h += '<div id="fb"></div>';
    view.innerHTML = h;
    view.querySelectorAll('.opt').forEach(b => b.onclick = () => {
      const ok = +b.dataset.i===d.answer;
      view.querySelectorAll('.opt').forEach(x=>{ x.disabled=true; if (+x.dataset.i===d.answer) x.classList.add('correct'); else if (x===b) x.classList.add('wrong'); });
      if (ok){ addXP(10,'adv:door:'+quadra); sfx('win'); }
      else sfx('bad');
      $('#fb').innerHTML = '<div class="expl'+(ok?'':' bad')+'">'+esc(d.why)+'</div><button class="btn gold" id="backMenu">‹ Volver a la región</button>';
      $('#backMenu').onclick = showMenu;
    });
  }
  showMenu();
}

/* ---------- LLUVIA ---------- */
function renderLluviaPick(){
  let h = '<h1>🌧️ Lluvia informacional</h1><div class="card"><p>Las fichas caen del cielo: clasifícalas antes de que se acabe el tiempo. Si se acaba, cuenta como fallo. El modo tranquilo usa las mismas fichas, sin caída.</p></div>';
  h += '<p><b>Ronda</b></p>';
  D.lluvia.rounds.forEach((r,i) => h += '<button class="opt" data-r="'+i+'">'+esc(r)+' · '+(D.rainByRound[i]||[]).length+' fichas</button>');
  h += '<p><b>Velocidad</b></p>';
  D.lluvia.speeds.forEach((s,i) => h += '<button class="opt" data-s="'+i+'">'+esc(s)+'</button>');
  h += '<button class="btn gold off" id="startRain">Elige ronda y velocidad</button>';
  view.innerHTML = h;
  let r=-1, s=-1;
  const refresh = () => { const b=$('#startRain'); if (r>=0&&s>=0){ b.classList.remove('off'); b.textContent='▶ Empezar la lluvia'; } };
  view.querySelectorAll('[data-r]').forEach(b => b.onclick = () => { view.querySelectorAll('[data-r]').forEach(x=>x.classList.remove('sel')); b.classList.add('sel'); r=+b.dataset.r; refresh(); });
  view.querySelectorAll('[data-s]').forEach(b => b.onclick = () => { view.querySelectorAll('[data-s]').forEach(x=>x.classList.remove('sel')); b.classList.add('sel'); s=+b.dataset.s; refresh(); });
  $('#startRain').onclick = () => { if (r>=0&&s>=0) startLluvia(r,s); };
}
function startLluvia(roundIdx, speedIdx){
  let pool = (D.rainByRound[roundIdx]||[]).filter(c=>c.diff<=S.difficulty);
  if (pool.length<5) pool = D.rainByRound[roundIdx]||[];
  const queue = shuffle(pool);
  const secs = speedIdx===0?6 : speedIdx===1?10 : 0;
  let idx=0, okCount=0, answered=0, timer=null, timeLeft=secs;
  function showCard(){
    if (idx>=queue.length){ finish(); return; }
    const c = queue[idx];
    const opts = shuffle(c.options.map((o,i)=>({o,i})));
    let h = '<div class="muted">Ronda: '+esc(D.lluvia.rounds[roundIdx])+' · Ficha '+(idx+1)+' de '+queue.length+' · Aciertos '+okCount+'</div>'
      + (secs? '<div class="timerbar"><i id="tbar" style="width:100%"></i></div><p class="muted" id="tlabel">'+secs+' s</p>' : '<p class="muted">Modo tranquilo: sin caída.</p>')
      + companionHTML()
      + '<div class="card falling"><p style="font-size:16px">'+esc(c.text)+'</p><p class="muted">¿Qué '+(['elemento','elemento','posición','posición','bloque','anillo'][roundIdx]||'cosa')+' es?</p></div>';
    opts.forEach(x => h += '<button class="opt" data-i="'+x.i+'">'+esc(x.o)+'</button>');
    h += '<div id="fb"></div>';
    view.innerHTML = h;
    let done=false;
    const resolve = (pickedIdx) => {
      if (done) return; done=true; clearInterval(timer); answered++;
      const ok = pickedIdx===c.answer;
      view.querySelectorAll('.opt').forEach(x=>{ x.disabled=true; if (+x.dataset.i===c.answer) x.classList.add('correct'); else if (+x.dataset.i===pickedIdx) x.classList.add('wrong'); });
      if (ok){ okCount++; addXP(10,'rain:'+c.id); compReact(true); }
      else compReact(false, c.answerText && EL.includes(c.answerText)? c.answerText : null);
      $('#fb').innerHTML = '<div class="expl'+(ok?'':' bad')+'">'+(ok?'✅ ¡Correcto! ':'❌ '+(pickedIdx==null?'Se acabó el tiempo. ':'Era '+esc(c.answerText)+'. '))+esc(c.explanation||'')+'</div>'
        + '<button class="btn gold" id="nextRain">'+(idx+1>=queue.length?'Ver resultado':'Siguiente ficha ›')+'</button>';
      $('#nextRain').onclick = () => { idx++; showCard(); };
    };
    view.querySelectorAll('.opt').forEach(b => b.onclick = () => resolve(+b.dataset.i));
    if (secs){
      timeLeft=secs;
      timer=setInterval(()=>{ timeLeft-=0.1;
        const bar=$('#tbar'); if (bar) bar.style.width=Math.max(0,100*timeLeft/secs)+'%';
        const lab=$('#tlabel'); if (lab) lab.textContent=Math.max(0,timeLeft).toFixed(1)+' s';
        if (timeLeft<=0) resolve(null);
      },100);
    }
  }
  function finish(){
    const acc = answered?Math.round(100*okCount/answered):0;
    bumpGameDay('lluvia',1); setBest('lluvia', acc);
    gameFinish('lluvia','Lluvia informacional · '+D.lluvia.rounds[roundIdx]+': '+okCount+' de '+answered+' ('+acc+'%).', acc);
  }
  showCard();
}

/* ---------- INTRUSO ---------- */
function renderIntruso(){
  let pool = D.intruso.filter(x=>x.diff<=S.difficulty);
  if (pool.length<10) pool = D.intruso;
  const queue = shuffle(pool).slice(0,10);
  let idx=0, okCount=0;
  function show(){
    if (idx>=queue.length){ bumpGameDay('intruso',1); setBest('intruso', okCount); gameFinish('intruso','Encuentra al intruso: '+okCount+' de '+queue.length+' correctos.', okCount); return; }
    const it = queue[idx];
    const opts = shuffle(it.items.map((o,i)=>({o,i})));
    let h = '<div class="muted">Conjunto '+(idx+1)+' de '+queue.length+' · Aciertos '+okCount+'</div>'
      + '<div class="bar"><i style="width:'+(100*idx/queue.length)+'%"></i></div>'
      + companionHTML()
      + '<div class="card"><div class="tag">Encuentra al intruso</div><h3>'+esc(it.rule)+'</h3><p class="muted">'+esc(it.property)+'</p></div>'
      + '<p><b>¿Cuál es el intruso?</b></p>';
    opts.forEach(x => h += '<button class="opt" data-i="'+x.i+'">'+esc(x.o)+'</button>');
    h += '<div id="fb"></div>';
    view.innerHTML = h;
    view.querySelectorAll('.opt').forEach(b => b.onclick = () => {
      const ok = +b.dataset.i===it.answer;
      view.querySelectorAll('.opt').forEach(x=>{ x.disabled=true; if (+x.dataset.i===it.answer) x.classList.add('correct'); else if (x===b) x.classList.add('wrong'); });
      if (ok){ okCount++; addXP(10,'int:'+it.id); compReact(true); } else compReact(false, null);
      $('#fb').innerHTML = '<div class="expl'+(ok?'':' bad')+'">'+(ok?'✅ ¡Ese era! ':'❌ El intruso era '+esc(it.items[it.answer])+'. ')+esc(it.explanation)+'</div>'
        + '<button class="btn gold" id="nextInt">'+(idx+1>=queue.length?'Ver resultado':'Siguiente ›')+'</button>';
      $('#nextInt').onclick = () => { idx++; show(); };
    });
  }
  show();
}

/* ---------- PERFIL ---------- */
const ACHIEVEMENTS = [
  ['first','🌱','Primera idea','Lee tu primera idea'],['r25','🔎','Curioso','Lee 25 ideas'],['r100','🐛','Ratón de biblioteca','Lee 100 ideas'],['r500','🎓','Erudito','Lee 500 ideas'],['r1200','🏛️','Biblioteca','Lee todas las ideas'],
  ['b1','📗','Finalista','Termina una lección'],['b10','📚','Estante','Termina 10 lecciones'],
  ['s3','🔥','En llamas','Racha de 3 días'],['s7','⚡','Guerrero semanal','Racha de 7 días'],['s30','🏆','Imparable','Racha de 30 días'],
  ['l1','⭐','Primera lección','Completa una lección'],['l25','🌟','Dedicado','Completa 25 lecciones'],
  ['perfect','💎','Impecable','Quiz perfecto'],['q100','🧠','Maestro del quiz','100 respuestas correctas'],
  ['save10','🔖','Coleccionista','Guarda 10 ideas'],['xp1k','💰','Cazador de XP','Gana 1,000 XP'],
  ['p_first','🕵️','Primera pista','Responde tu primer ejercicio de práctica'],['p10','🔍','Aprendiz de detective','10 aciertos en práctica'],['p25','🧩','Observador','25 aciertos en práctica'],['p50','🔬','Analista','50 aciertos en práctica'],['p100','🎯','Experto','100 aciertos en práctica'],['p150','🏛️','Maestro tipador','150 aciertos en práctica'],
  ['p_s3','🔥','En racha','3 aciertos seguidos en práctica'],['p_s5','⚡','Imparable','5 aciertos seguidos en práctica'],['p_s10','💎','Perfecto','10 aciertos seguidos en práctica'],
  ['p_l1','🌱','Cimientos','Completa el Nivel 1 de práctica'],['p_l2','🪞','Espejo social','Completa el Nivel 2 de práctica'],['p_l3','💭','Lo que se anhela','Completa el Nivel 3 de práctica'],['p_l4','👑','Maestría','Completa el Nivel 4 de práctica'],
];
function unlockedAch(){
  const u = new Set(S.achievements);
  if (S.readIdeas.length>=1) u.add('first');
  if (S.readIdeas.length>=25) u.add('r25');
  if (S.readIdeas.length>=100) u.add('r100');
  if (S.readIdeas.length>=470) u.add('r1200');
  if (S.lessonsDone.length>=1) u.add('b1'); if (S.lessonsDone.length>=10) u.add('b10');
  if (streak()>=3) u.add('s3'); if (streak()>=7) u.add('s7'); if (streak()>=30) u.add('s30');
  if (S.lessonsDone.length>=1) u.add('l1'); if (S.lessonsDone.length>=25) u.add('l25');
  if (S.savedIdeas.length>=10) u.add('save10');
  if (S.xp>=1000) u.add('xp1k');
  const pok = Object.keys(S.practiceOk).length;
  if (pok>=1) u.add('p_first'); if (pok>=10) u.add('p10'); if (pok>=25) u.add('p25'); if (pok>=50) u.add('p50'); if (pok>=100) u.add('p100'); if (pok>=150) u.add('p150');
  if (S.practiceBestStreak>=3) u.add('p_s3'); if (S.practiceBestStreak>=5) u.add('p_s5'); if (S.practiceBestStreak>=10) u.add('p_s10');
  return u;
}
function renderProfile(){
  const u = unlockedAch();
  const lv = levelOf(S.xp);
  let h = '<h1>👤 Perfil</h1>';
  h += '<div class="card gold"><div class="tag">🎨 Tema</div><p class="muted">Elige el tema de la app (arriba, destacado).</p><div class="theme-grid">'
    + D.themes.themes.map(t => '<button data-theme="'+t.id+'" class="'+(S.theme===t.id?'sel':'')+'" style="background:'+t.colors.surface+';border-color:'+t.colors.accent+'">'+esc(t.name)+'</button>').join('')
    + '</div></div>';
  h += '<div class="card"><div class="tag">Mi avatar</div><div class="row" style="margin:8px 0"><img src="avatars/'+esc(AVATARS[S.avatarIdx]||AVATARS[0])+'" style="width:72px;height:72px;border-radius:14px;border:2px solid var(--accent);object-fit:cover" alt="Mi avatar"><div class="grow"><b>'+esc(displayName())+'</b><div class="muted">'+(S.myType?'Mi type: '+esc(S.myType):'Sin type elegido')+'</div></div></div>'
    + '<details><summary>Ver los 48 avatares</summary><div class="avatar-grid" style="margin-top:8px">'
    + AVATARS.map((a,i)=>'<img src="avatars/'+a+'" data-av="'+i+'" class="'+(S.avatarIdx===i?'sel':'')+'" alt="'+a+'">').join('')
    + '</div></details></div>';
  h += '<div class="card"><label>Apodo (por defecto «Sin nombre», nunca «Remy»)</label><input type="text" id="nickInput" value="'+esc(S.nickname)+'" placeholder="Sin nombre" maxlength="24">'
    + '<label>Mi type</label><div>'+TYPES.map(t=>'<button class="chip'+(S.myType===t?' sel':'')+'" data-type="'+t+'">'+t+'</button>').join('')+'</div>'
    + '<label>Sonidos</label><div><button class="chip'+(S.sound?' sel':'')+'" data-sound="1">Sí 🔊</button><button class="chip'+(!S.sound?' sel':'')+'" data-sound="0">No 🔇</button></div></div>';
  h += '<div class="stat-grid">'
    + '<div class="stat"><b>'+S.xp+'</b><span>XP total</span></div><div class="stat"><b>'+lv+'</b><span>Nivel</span></div>'
    + '<div class="stat"><b>'+streak()+'</b><span>Racha (días)</span></div><div class="stat"><b>'+S.gems+'</b><span>Gemas</span></div>'
    + '<div class="stat"><b>'+S.lessonsDone.length+' / 47</b><span>Lecciones</span></div><div class="stat"><b>'+S.readIdeas.length+' / 470</b><span>Ideas leídas</span></div>'
    + '</div>';
  h += '<h2>🏆 Logros ('+u.size+' / '+ACHIEVEMENTS.length+')</h2>';
  ACHIEVEMENTS.forEach(a => {
    const has = u.has(a[0]);
    h += '<div class="ach'+(has?'':' locked')+'"><span class="e">'+a[1]+'</span><div><b>'+a[2]+'</b><div class="d">'+a[3]+'</div></div><span class="grow"></span>'+(has?'✅':'🔒')+'</div>';
  });
  h += '<h2>☁️ Cuenta</h2><div class="card" id="accountBox"></div>';
  h += '<h2>🏆 Ranking</h2><div class="card" id="rankBox"><p class="muted">Cargando…</p></div>';
  h += '<p class="muted">Créditos: avatares de Binky (@binkybee_art). Sociónica de los 29 documentos SCS.</p>';
  view.innerHTML = h;
  view.querySelectorAll('[data-theme]').forEach(b => b.onclick = () => { S.theme=b.dataset.theme; save(); applyTheme(); renderProfile(); });
  view.querySelectorAll('[data-av]').forEach(b => b.onclick = () => { S.avatarIdx=+b.dataset.av; save(); renderProfile(); });
  view.querySelectorAll('[data-type]').forEach(b => b.onclick = () => { S.myType = S.myType===b.dataset.type?'':b.dataset.type; save(); renderProfile(); syncScore(); });
  view.querySelectorAll('[data-sound]').forEach(b => b.onclick = () => { S.sound = b.dataset.sound==='1'; save(); renderProfile(); if (S.sound) sfx('ok'); });
  $('#nickInput').addEventListener('change', e => { S.nickname = e.target.value.trim(); save(); renderProfile(); syncScore(); });
  renderAccountBox();
  loadRanking();
}
function renderAccountBox(){
  const box = $('#accountBox'); if (!box) return;
  if (S.auth && S.auth.user_id){
    box.innerHTML = '<p>✅ Sesión iniciada: <b>'+esc(S.auth.email||'')+'</b></p><p class="muted">Tu progreso se sincroniza en la nube cuando hay red. Sin red, todo sigue funcionando en local.</p><button class="btn" id="logoutBtn">Cerrar sesión</button>';
    $('#logoutBtn').onclick = () => { S.auth=null; save(); renderProfile(); toast('Sesión cerrada'); };
    return;
  }
  box.innerHTML = '<p><b>Sin cuenta no hay ranking</b> · tu progreso se guarda solo en este teléfono.</p>'
    + '<label>Correo</label><input type="email" id="accEmail" placeholder="tu@correo.com">'
    + '<label>Contraseña</label><input type="password" id="accPass" placeholder="Mínimo 6 caracteres">'
    + '<div class="row"><button class="btn gold grow" id="signupBtn" style="margin:0">Registrarme</button><button class="btn grow" id="loginBtn" style="margin:0">Entrar</button></div>'
    + '<p class="muted" id="accMsg"></p>';
  const doAuth = async (mode) => {
    const email = $('#accEmail').value.trim(), pass = $('#accPass').value;
    const msg = $('#accMsg');
    if (!email || !pass || pass.length<6){ msg.textContent='Escribe un correo y una contraseña de al menos 6 caracteres.'; return; }
    msg.textContent = 'Conectando…';
    try {
      const url = mode==='signup'
        ? D.config.supabaseUrl+'/auth/v1/signup'
        : D.config.supabaseUrl+'/auth/v1/token?grant_type=password';
      const res = await fetch(url, { method:'POST', headers:{'apikey':D.config.supabaseAnonKey,'Content-Type':'application/json'}, body:JSON.stringify({email, password:pass}) });
      const j = await res.json();
      if (!res.ok) throw new Error(j.error_description || j.msg || j.message || ('Error '+res.status));
      const token = j.access_token, uid = (j.user&&j.user.id)||j.id;
      if (!token || !uid) throw new Error('La respuesta no trajo sesión. Inténtalo otra vez.');
      S.auth = {user_id:uid, email, access_token:token}; save();
      msg.textContent = '✅ Sesión iniciada. Sincronizando…';
      await syncAll(); renderProfile(); toast('Sesión iniciada');
    } catch(e){ msg.textContent = '❌ '+e.message; }
  };
  $('#signupBtn').onclick = () => doAuth('signup');
  $('#loginBtn').onclick = () => doAuth('login');
}

/* ---------- Supabase (REST directo, anon key pública) ---------- */
function sbHeaders(userToken){
  return {'apikey':D.config.supabaseAnonKey,'Authorization':'Bearer '+(userToken||D.config.supabaseAnonKey),'Content-Type':'application/json'};
}
async function syncAll(){
  if (!S.auth) return;
  try {
    const base = D.config.supabaseUrl+'/rest/v1';
    // game_scores: ranking propio
    await fetch(base+'/game_scores?on_conflict=user_id', { method:'POST',
      headers:Object.assign(sbHeaders(S.auth.access_token), {'Prefer':'resolution=merge-duplicates,return=representation'}),
      body:JSON.stringify({user_id:S.auth.user_id, username:displayName(), total_xp:S.xp, level:levelOf(S.xp), updated_at:new Date().toISOString()}) });
    // game_progress por lección completada
    for (const lid of S.lessonsDone){
      await fetch(base+'/game_progress?on_conflict=user_id,module,lesson', { method:'POST',
        headers:Object.assign(sbHeaders(S.auth.access_token), {'Prefer':'resolution=merge-duplicates,return=representation'}),
        body:JSON.stringify({user_id:S.auth.user_id, module:D.books.lessons[lid].category, lesson:lid, xp:S.xp, streak:streak(), hearts:3, gems:S.gems, completed:true, updated_at:new Date().toISOString()}) });
    }
  } catch(e){ console.warn('sync falló (local manda):', e); }
}
async function syncScore(){ if (S.auth) { try { await syncAll(); } catch(e){} } }
async function loadRanking(){
  const box = $('#rankBox'); if (!box) return;
  if (!S.auth){ box.innerHTML = '<p class="muted">Sin cuenta no hay ranking. Inicia sesión para ver el top 20.</p>'; return; }
  try {
    const res = await fetch(D.config.supabaseUrl+'/rest/v1/game_scores?select=user_id,username,total_xp,level&order=total_xp.desc&limit=20', { headers: sbHeaders(S.auth.access_token) });
    if (!res.ok) throw new Error('HTTP '+res.status);
    const rows = await res.json();
    if (!rows.length){ box.innerHTML = '<p class="muted">Aún no hay puntajes. ¡Sé el primero!</p>'; return; }
    box.innerHTML = rows.map((r,i)=>'<div class="rank-row"><b>#'+(i+1)+'</b><img src="avatars/'+esc(AVATARS[0])+'" alt=""><div class="grow"><b>'+esc(r.username||'Sin nombre')+'</b><div class="muted">Nivel '+(r.level||1)+'</div></div><b>'+(r.total_xp||0)+' XP</b></div>').join('');
  } catch(e){ box.innerHTML = '<p class="muted">No se pudo cargar el ranking (sin red o sin sesión). Tu progreso local sigue intacto.</p>'; }
}

/* ---------- Arranque ---------- */
async function boot(){
  await loadData();
  if (!DATA_OK){
    view.innerHTML = '<h1>⚠️ No se pudieron cargar los datos</h1><div class="card"><p>La app necesita los archivos de <b>data/*.json</b>. Sirve el sitio por HTTP (p. ej. <code>python3 -m http.server</code>) y recarga.</p></div>';
    return;
  }
  applyTheme();
  updateTopXp();
  if ('serviceWorker' in navigator && location.protocol.startsWith('http')){
    navigator.serviceWorker.register('sw.js').catch(e=>console.warn('SW no registrado:', e));
  }
  showTab('home');
}
document.addEventListener('DOMContentLoaded', boot);
