/**
 * H5P.Timer 0.4 — reemplazo escrito desde cero para Valhalla School.
 *
 * Licencia: MIT (Valhalla School, 2026).
 *
 * Motivo: el H5P.Timer original solo existe dentro del core H5P, cuyo
 * repositorio ya no es publico y cuya licencia no se pudo verificar como
 * permisiva. Este archivo NO copia ese codigo: implementa desde cero la
 * API minima que usa H5P.MemoryGame (ver memory-game/src/scripts/timer.js):
 *
 *   new Timer(intervalMs)            — intervalMs = ms por tick (100)
 *   Timer.call(self, intervalMs)     — uso como "superclase" del adaptador
 *   setClockTime(ms) / getTime()     — tiempo acumulado en milisegundos
 *   play() / pause() / stop()        — arranca, pausa y detiene el conteo
 *   reset()                          — para, pone el tiempo a 0 y dispara
 *                                      el evento 'reset' (EventDispatcher)
 *   notify(evento, callback)         — registra UN callback por evento
 *                                      ('every_tenth_second', 'every_second',
 *                                      'every_minute'); registrar de nuevo
 *                                      reemplaza el anterior
 *   Timer.extractTimeElement(ms, 'hours' | 'minutes' | 'seconds')
 *
 * Hereda de H5P.EventDispatcher (del core que trae h5p-standalone),
 * asi que tambien dispone de on()/trigger().
 */
(function () {
  var H5P = window.H5P = window.H5P || {};

  /**
   * @param {number} [interval=100] Milisegundos entre ticks.
   */
  H5P.Timer = function (interval) {
    H5P.EventDispatcher.call(this);
    this.interval = interval || 100;
    this.time = 0;
    this._timerId = null;
    this._callbacks = {};
    this._lastSecond = -1;
    this._lastMinute = -1;
  };

  H5P.Timer.prototype = Object.create(H5P.EventDispatcher.prototype);
  H5P.Timer.prototype.constructor = H5P.Timer;

  /** Fija el tiempo acumulado (ms). */
  H5P.Timer.prototype.setClockTime = function (ms) {
    this.time = ms || 0;
  };

  /** Tiempo acumulado (ms). */
  H5P.Timer.prototype.getTime = function () {
    return this.time;
  };

  /**
   * Registra (reemplazando si ya habia uno) el callback de un evento
   * periodico: 'every_tenth_second', 'every_second' o 'every_minute'.
   */
  H5P.Timer.prototype.notify = function (eventName, callback) {
    this._callbacks[eventName] = callback;
  };

  H5P.Timer.prototype._fire = function (eventName) {
    var cb = this._callbacks[eventName];
    if (typeof cb === 'function') {
      cb();
    }
  };

  H5P.Timer.prototype._tick = function () {
    this.time += this.interval;
    this._fire('every_tenth_second');
    var seconds = Math.floor(this.time / 1000);
    if (seconds !== this._lastSecond) {
      this._lastSecond = seconds;
      this._fire('every_second');
    }
    var minutes = Math.floor(this.time / 60000);
    if (minutes !== this._lastMinute) {
      this._lastMinute = minutes;
      this._fire('every_minute');
    }
  };

  /** Arranca el conteo (si ya corre, no hace nada). */
  H5P.Timer.prototype.play = function () {
    var self = this;
    if (this._timerId !== null) {
      return;
    }
    this._timerId = setInterval(function () {
      self._tick();
    }, this.interval);
  };

  /** Pausa el conteo conservando el tiempo. */
  H5P.Timer.prototype.pause = function () {
    if (this._timerId !== null) {
      clearInterval(this._timerId);
      this._timerId = null;
    }
  };

  /** Detiene el conteo conservando el tiempo (igual que pause). */
  H5P.Timer.prototype.stop = function () {
    this.pause();
  };

  /** Para el conteo, pone el tiempo a 0 y dispara el evento 'reset'. */
  H5P.Timer.prototype.reset = function () {
    this.pause();
    this.time = 0;
    this._lastSecond = -1;
    this._lastMinute = -1;
    this.trigger('reset');
  };

  /**
   * Extrae un componente de un tiempo en milisegundos.
   *
   * @param {number} time Tiempo en milisegundos.
   * @param {string} part 'hours' | 'minutes' | 'seconds'.
   * @returns {number}
   */
  H5P.Timer.extractTimeElement = function (time, part) {
    var totalSeconds = Math.floor(time / 1000);
    if (part === 'hours') {
      return Math.floor(totalSeconds / 3600);
    }
    if (part === 'minutes') {
      return Math.floor(totalSeconds / 60) % 60;
    }
    if (part === 'seconds') {
      return totalSeconds % 60;
    }
    return 0;
  };
})();
