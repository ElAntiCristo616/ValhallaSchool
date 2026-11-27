package com.valhallaschool.app;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;

/**
 * Tiny synthesized feedback sounds (no audio assets).
 * v1.3.2 — YAY (acierto, agradable/corto) y ANGRY (fallo, buzzer grave):
 * los toca Companion.react() en cada respuesta. Respeta el silencio del
 * teléfono (modo silencio/vibración = sin sonido) y el ajuste de Sonidos.
 */
final class Sfx {
    static final int OK = 0, BAD = 1, WIN = 2, YAY = 3, ANGRY = 4;
    private static final AudioTrack[] tracks = new AudioTrack[5];
    private static Context appCtx;

    private Sfx() {}

    /** Se llama una vez en MainActivity.onCreate: guarda el contexto para revisar el modo de timbre. */
    static void init(Context c) { appCtx = c.getApplicationContext(); }

    static void play(int kind) {
        if (Store.I == null || !Store.I.sound) return;
        // Respeta el silencio: en modo silencio o vibración no suena nada.
        try {
            if (appCtx != null) {
                AudioManager am = (AudioManager) appCtx.getSystemService(Context.AUDIO_SERVICE);
                if (am != null && am.getRingerMode() != AudioManager.RINGER_MODE_NORMAL) return;
            }
        } catch (Exception ignored) { /* sin AudioManager, suena normal */ }
        try {
            AudioTrack t = tracks[kind];
            if (t == null) {
                if (kind == OK) t = make(new float[]{660, 880}, false, 0.22f);
                else if (kind == BAD) t = make(new float[]{220, 180}, true, 0.07f);
                else if (kind == YAY) t = make(new float[]{784, 1047, 1319}, false, 0.22f);
                else if (kind == ANGRY) t = make(new float[]{160, 110, 80}, true, 0.10f);
                else t = make(new float[]{523, 659, 784, 1047}, false, 0.22f);
                tracks[kind] = t;
            }
            if (t.getPlayState() != AudioTrack.PLAYSTATE_STOPPED) t.stop();
            t.reloadStaticData();
            t.play();
        } catch (Exception ignored) {
            // sound is optional
        }
    }

    private static AudioTrack make(float[] freqs, boolean square, float vol) {
        int sr = 22050, step = (int) (sr * 0.09), dur = (int) (sr * 0.16), len = step * (freqs.length - 1) + dur;
        float[] acc = new float[len];
        double attack = sr * 0.015;
        for (int i = 0; i < freqs.length; i++) {
            int s = i * step;
            for (int k = 0; k < dur; k++) {
                double env = k < attack ? k / attack : Math.exp(-(k - attack) / (sr * 0.045));
                double v = Math.sin(2 * Math.PI * freqs[i] * k / sr);
                if (square) v = v >= 0 ? 1 : -1;
                acc[s + k] += (float) (v * env * vol);
            }
        }
        short[] buf = new short[len];
        for (int k = 0; k < len; k++) buf[k] = (short) Math.max(-32767, Math.min(32767, acc[k] * 32767));
        AudioTrack t = new AudioTrack.Builder()
                .setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
                .setAudioFormat(new AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sr).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
                .setBufferSizeInBytes(len * 2).setTransferMode(AudioTrack.MODE_STATIC).build();
        t.write(buf, 0, len);
        return t;
    }
}
