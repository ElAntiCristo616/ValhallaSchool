/* Valhalla School Web — service worker, cache-first, versionado */
const CACHE = 'valhalla-school-v1';
const SHELL = [
  './', './index.html', './styles.css', './app.js', './manifest.json',
  './icon-192.png', './icon-512.png', './apple-touch-icon.png',
  './data/config.json', './data/books.json', './data/ideas.json', './data/quiz_new.json',
  './data/practice.json', './data/themes.json', './data/companion.json',
  './data/detective.json', './data/lab.json', './data/tribunal.json',
  './data/conexiones.json', './data/arena.json', './data/aventura.json',
  './data/lluvia.json', './data/intruso.json'
];
self.addEventListener('install', e => {
  e.waitUntil(caches.open(CACHE).then(c => c.addAll(SHELL)).then(() => self.skipWaiting()));
});
self.addEventListener('activate', e => {
  e.waitUntil(caches.keys().then(ks => Promise.all(ks.filter(k => k !== CACHE).map(k => caches.delete(k)))).then(() => self.clients.claim()));
});
self.addEventListener('fetch', e => {
  const url = new URL(e.request.url);
  // Solo cachear mismo origen (app + datos + avatares). Supabase y resto van a red.
  if (url.origin !== self.location.origin) return;
  e.respondWith(
    caches.match(e.request, { ignoreSearch: true }).then(hit => {
      const net = fetch(e.request).then(res => {
        if (res && res.ok) { const copy = res.clone(); caches.open(CACHE).then(c => c.put(e.request, copy)); }
        return res;
      }).catch(() => hit);
      return hit || net;
    })
  );
});
