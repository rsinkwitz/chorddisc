package com.rsinkwitz.chorddisc;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioTrack;
import android.os.Handler;
import android.os.Looper;

/**
 * Klasse zum Generieren und Abspielen von Akkorden mittels Sinuswellen-Synthese.
 * Unterstützt Dur- und Moll-Akkorde.
 */
public class ChordPlayer {

    private static final int SAMPLE_RATE = 44100;
    private static final int NOTE_DURATION_MS = 250; // 0.25 Sekunden pro Note (doppelt so schnell)
    private static final int NOTE_DURATION_LONG_MS = 500; // 0.5 Sekunden für dritten Ton
    private static final double AMPLITUDE = 0.6; // Lautstärke (0.0 - 1.0) - verdoppelt

    private AudioTrack audioTrack;
    private final Handler handler;
    private boolean isPlaying = false;

    // Frequenzen der Noten in Hz (erweitert um volle Oktave für Akkorde)
    // C4 bis G#5 (damit H-Dur möglich ist: H4 + 7 = F#5)
    private static final double[] NOTE_FREQUENCIES = {
        261.63,  // 0:  C4
        277.18,  // 1:  C#4/Db4
        293.66,  // 2:  D4
        311.13,  // 3:  D#4/Eb4
        329.63,  // 4:  E4
        349.23,  // 5:  F4
        369.99,  // 6:  F#4/Gb4
        392.00,  // 7:  G4
        415.30,  // 8:  G#4/Ab4
        440.00,  // 9:  A4 (Kammerton)
        466.16,  // 10: A#4/Bb4
        493.88,  // 11: B4/H4
        523.25,  // 12: C5 (oktaviert)
        554.37,  // 13: C#5/Db5
        587.33,  // 14: D5
        622.25,  // 15: D#5/Eb5
        659.25,  // 16: E5
        698.46,  // 17: F5
        739.99,  // 18: F#5/Gb5
        783.99   // 19: G5
    };

    public ChordPlayer(Context context) {
        handler = new Handler(Looper.getMainLooper());
        initAudioTrack();
    }

    private void initAudioTrack() {
        int bufferSize = AudioTrack.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        );

        AudioAttributes audioAttributes = new AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build();

        AudioFormat audioFormat = new AudioFormat.Builder()
            .setSampleRate(SAMPLE_RATE)
            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
            .build();

        audioTrack = new AudioTrack(
            audioAttributes,
            audioFormat,
            bufferSize,
            AudioTrack.MODE_STREAM,
            0 // Session ID = 0 (kompatibel mit API 21)
        );
    }

    /**
     * Spielt einen Akkord als Arpegggio (nacheinander).
     * @param noteIndex Index der Grundnote (0-18)
     * @param isMajor true für Dur, false für Moll
     */
    public void playChord(int noteIndex, boolean isMajor) {
        if (isPlaying) {
            return; // Verhindere überlappende Akkorde
        }

        isPlaying = true;

        // Berechne die Noten des Akkords (Grundton, Terz, Quinte)
        int root = getNoteFrequencyIndex(noteIndex);
        int third = root + (isMajor ? 4 : 3); // Dur: +4 Halbtöne, Moll: +3 Halbtöne
        int fifth = root + 7; // Quinte: +7 Halbtöne

        // Spiele die Noten nacheinander (ohne Pausen zwischen den Tönen)
        new Thread(() -> {
            playNote(root, NOTE_DURATION_MS);
            playNote(third, NOTE_DURATION_MS);
            playNote(fifth, NOTE_DURATION_LONG_MS); // Dritter Ton länger

            // Erst nach allen drei Noten isPlaying zurücksetzen
            handler.post(() -> isPlaying = false);
        }).start();
    }

    /**
     * Konvertiert noteIndex (0-18) zu einem Frequenz-Array-Index.
     */
    private int getNoteFrequencyIndex(int noteIndex) {
        // Mapping: 0=C, 1=C#, 2=Db, 3=D, 4=D#, 5=Eb, 6=E, 7=E#/Fb, 8=F, ...
        // Vereinfachung: Enharmonische Verwechslungen auf gleiche Frequenz mappen
        int[] mapping = {
            0,  // C
            1,  // C# (Position 1)
            1,  // Db (Position 2) -> gleich wie C#
            2,  // D
            3,  // D#
            3,  // Eb -> gleich wie D#
            4,  // E
            4,  // E#/Fb -> gleich wie E (selten)
            5,  // F
            6,  // F#
            6,  // Gb -> gleich wie F#
            7,  // G
            8,  // G#
            8,  // Ab -> gleich wie G#
            9,  // A
            10, // A#
            10, // B (Position 16) -> gleich wie A#
            11, // H
            0   // H#/Cb (Position 18) -> gleich wie C
        };

        if (noteIndex < 0 || noteIndex >= mapping.length) {
            return 0; // Fallback auf C
        }

        return mapping[noteIndex];
    }

    /**
     * Spielt eine einzelne Note mit variabler Dauer.
     * @param frequencyIndex Index im Frequenz-Array
     * @param durationMs Dauer in Millisekunden
     */
    private void playNote(int frequencyIndex, int durationMs) {
        if (frequencyIndex < 0 || frequencyIndex >= NOTE_FREQUENCIES.length) {
            return;
        }

        double frequency = NOTE_FREQUENCIES[frequencyIndex];
        int numSamples = (SAMPLE_RATE * durationMs) / 1000;
        short[] buffer = new short[numSamples];

        // Generiere Sinuswelle mit Envelope (Fade-out)
        for (int i = 0; i < numSamples; i++) {
            double time = (double) i / SAMPLE_RATE;
            double sample = Math.sin(2.0 * Math.PI * frequency * time);

            // Envelope: Linear fade-out in den letzten 100ms
            double envelope = 1.0;
            int fadeStartSample = numSamples - (SAMPLE_RATE * 100 / 1000);
            if (i > fadeStartSample) {
                envelope = 1.0 - ((double)(i - fadeStartSample) / (numSamples - fadeStartSample));
            }

            buffer[i] = (short) (sample * AMPLITUDE * envelope * Short.MAX_VALUE);
        }

        // Spiele Audio synchron (blockiert bis fertig)
        if (audioTrack.getState() == AudioTrack.STATE_INITIALIZED) {
            audioTrack.play();
            audioTrack.write(buffer, 0, buffer.length);
            audioTrack.stop();
        }
    }

    /**
     * Spielt eine Tonleiter auf- und abwärts.
     * @param rootIndex Index der Grundnote (0-18)
     * @param scaleType Tonleiter-Typ (Dur, nat. Moll, harm. Moll)
     * @param callback Wird für jeden gespielten Ton aufgerufen (für visuelle Hervorhebung)
     */
    public void playScale(int rootIndex, ScaleType scaleType, ScaleNoteCallback callback) {
        if (isPlaying) {
            return;
        }

        isPlaying = true;

        // Bestimme Tonleiter-Intervalle (in Halbtönen)
        int[] intervals;
        switch (scaleType) {
            case MAJOR:
                intervals = new int[]{0, 2, 4, 5, 7, 9, 11, 12}; // Dur: W-W-H-W-W-W-H
                break;
            case NATURAL_MINOR:
                intervals = new int[]{0, 2, 3, 5, 7, 8, 10, 12}; // Nat. Moll: W-H-W-W-H-W-W
                break;
            case HARMONIC_MINOR:
                intervals = new int[]{0, 2, 3, 5, 7, 8, 11, 12}; // Harm. Moll: W-H-W-W-H-1.5-H
                break;
            default:
                intervals = new int[]{0, 2, 4, 5, 7, 9, 11, 12};
                break;
        }

        int root = getNoteFrequencyIndex(rootIndex);

        new Thread(() -> {
            // Aufwärts
            for (int interval : intervals) {
                int noteFreqIndex = root + interval;
                if (callback != null) {
                    handler.post(() -> callback.onNotePlay(rootIndex, interval));
                }
                playNote(noteFreqIndex, NOTE_DURATION_MS);
            }

            // Abwärts (ohne erste und letzte Note zu wiederholen)
            for (int i = intervals.length - 2; i >= 1; i--) {
                final int currentInterval = intervals[i];
                int noteFreqIndex = root + currentInterval;
                if (callback != null) {
                    handler.post(() -> callback.onNotePlay(rootIndex, currentInterval));
                }
                playNote(noteFreqIndex, NOTE_DURATION_MS);
            }

            // Letzte Note (Grundton) länger
            if (callback != null) {
                handler.post(() -> callback.onNotePlay(rootIndex, 0));
            }
            playNote(root, NOTE_DURATION_LONG_MS);

            // Hervorhebung zurücksetzen
            if (callback != null) {
                handler.post(() -> callback.onScaleFinished());
            }

            handler.post(() -> isPlaying = false);
        }).start();
    }

    /**
     * Interface für Tonleiter-Callback
     */
    public interface ScaleNoteCallback {
        void onNotePlay(int rootIndex, int intervalInHalftones);
        void onScaleFinished();
    }

    /**
     * Enum für Tonleiter-Typen
     */
    public enum ScaleType {
        MAJOR,
        NATURAL_MINOR,
        HARMONIC_MINOR
    }

    /**
     * Gibt Ressourcen frei.
     */
    public void release() {
        if (audioTrack != null) {
            audioTrack.release();
            audioTrack = null;
        }
    }
}

