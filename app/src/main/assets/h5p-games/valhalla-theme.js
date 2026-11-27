/* Valhalla School — tema activo para las paginas de minijuegos.
 * Las 16 paletas son las mismas de Ui.java (THEME_PAL: fondo, superficie,
 * acento, secundario, texto). La pagina se abre con ?theme=<id> desde
 * GameScreen; sin parametro, Navidad. El fondo queda TRANSPARENTE para
 * que las particulas (ParticleView) se vean detras del WebView.
 */
(function () {
  var THEMES = {
    navidad:    ['#04140e', '#0e3426', '#d9a441', '#cd2d32', '#f5efe0'],
    invierno:   ['#0b1626', '#16273e', '#7fb8e8', '#bfe0f5', '#eaf3fb'],
    sakura:     ['#2a1220', '#3d1b2e', '#f4a7c3', '#ffd6e8', '#fff0f5'],
    fuego:      ['#1a0a06', '#2e130a', '#ff6b35', '#ffc53d', '#fff1e6'],
    oceano:     ['#06222e', '#0b3547', '#2aa198', '#4dd0e1', '#e6f7f5'],
    galaxia:    ['#0d0620', '#1b0f38', '#9b7ede', '#ffd166', '#f0eafb'],
    bosque:     ['#0c1f12', '#162e1b', '#7fb069', '#d9a441', '#edf5e9'],
    atardecer:  ['#22101a', '#3a1b28', '#e87b2e', '#f4a259', '#fff0e6'],
    neon:       ['#0a0a14', '#14142a', '#00f5d4', '#ff2e88', '#f2f2ff'],
    otono:      ['#201209', '#33200f', '#d9822b', '#a63d2f', '#fbf0dc'],
    dulce:      ['#2b1030', '#3e1a45', '#ff8fab', '#ffc8dd', '#fff0f6'],
    lluvia:     ['#0e1b26', '#16283a', '#5b8def', '#7fb8e8', '#eaf2fa'],
    claro:      ['#f7f3e8', '#ffffff', '#8a6d1b', '#2aa198', '#1f2937'],
    aurora:     ['#061a1e', '#0b2e33', '#7ef0c1', '#b388ff', '#e6fff4'],
    desierto:   ['#241a0c', '#3a2c15', '#e0a458', '#c97b2d', '#fdf3dc'],
    medianoche: ['#050510', '#0e0e24', '#8e9bff', '#e8e8ff', '#ededf7']
  };

  function themeId() {
    try {
      var t = new URLSearchParams(window.location.search).get('theme');
      if (t && THEMES[t]) return t;
    } catch (e) {}
    return 'navidad';
  }

  function apply() {
    var pal = THEMES[themeId()];
    var r = document.documentElement;
    r.style.setProperty('--vh-bg', pal[0]);
    r.style.setProperty('--vh-surf', pal[1]);
    r.style.setProperty('--vh-accent', pal[2]);
    r.style.setProperty('--vh-secondary', pal[3]);
    r.style.setProperty('--vh-text', pal[4]);
    r.setAttribute('data-theme', themeId());
  }

  var _won = false;
  function win() {
    if (_won) return;
    _won = true;
    var w = document.getElementById('win');
    if (w) w.hidden = false;
    try {
      if (window.Android && Android.gameCompleted) Android.gameCompleted();
    } catch (e) {}
  }

  window.ValhallaTheme = { apply: apply, win: win, id: themeId, themes: THEMES };
})();
