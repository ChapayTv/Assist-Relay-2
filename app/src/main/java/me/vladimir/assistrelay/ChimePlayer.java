package me.vladimir.assistrelay;

import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioTrack;

/**
 * Проигрывает короткий сигнал активации через текущий аудиовыход (наушники).
 * Перед сигналом идёт тишина (leadMs): за это время Bluetooth-канал просыпается,
 * и сигнал не уходит в динамик.
 */
final class ChimePlayer {

    private static final int RATE = 16000;

    private ChimePlayer() {}

    /** Запускает воспроизведение в отдельном потоке и сразу возвращается. */
    static void play(final int leadMs) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                AudioTrack track = null;
                try {
                    short[] pcm = build(leadMs);
                    track = new AudioTrack.Builder()
                            .setAudioAttributes(new AudioAttributes.Builder()
                                    .setUsage(AudioAttributes.USAGE_MEDIA)
                                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                                    .build())
                            .setAudioFormat(new AudioFormat.Builder()
                                    .setSampleRate(RATE)
                                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                                    .build())
                            .setBufferSizeInBytes(pcm.length * 2)
                            .setTransferMode(AudioTrack.MODE_STATIC)
                            .build();
                    track.write(pcm, 0, pcm.length);
                    track.play();
                    Thread.sleep(pcm.length * 1000L / RATE + 100);
                } catch (Exception ignored) {
                    // сигнал необязателен: при ошибке просто молчим
                } finally {
                    if (track != null) {
                        try {
                            track.stop();
                        } catch (Exception ignored) {
                            // уже остановлен
                        }
                        track.release();
                    }
                }
            }
        }).start();
    }

    /** Тишина leadMs, затем два коротких тона вверх, затем 150 мс тишины. */
    private static short[] build(int leadMs) {
        int lead = RATE * Math.max(0, leadMs) / 1000;
        int t1 = RATE * 90 / 1000;
        int t2 = RATE * 120 / 1000;
        int tail = RATE * 150 / 1000;
        short[] out = new short[lead + t1 + t2 + tail];
        addTone(out, lead, t1, 880.0);
        addTone(out, lead + t1, t2, 1320.0);
        return out;
    }

    private static void addTone(short[] out, int start, int len, double freq) {
        for (int i = 0; i < len; i++) {
            // плавное нарастание и затухание, чтобы не щёлкало
            double env = Math.min(1.0, Math.min(i, len - i) / (RATE * 0.01));
            double v = Math.sin(2 * Math.PI * freq * i / RATE) * 0.35 * env;
            out[start + i] = (short) (v * Short.MAX_VALUE);
        }
    }
}
