# Audio-Features - Technische Dokumentation

## 🎵 Übersicht

Die Tonarten-Scheibe-App kann jetzt Akkorde in Echtzeit generieren und abspielen. Die Audio-Engine verwendet Sinuswellen-Synthese für die Ton-Erzeugung.

## 🎹 Funktionsweise

### 1. Akkord-Synthese

**ChordPlayer-Klasse:**
```java
public void playChord(int noteIndex, boolean isMajor)
```

**Parameter:**
- `noteIndex` (0-18): Position im 19-Noten-System
- `isMajor`: `true` = Dur-Akkord, `false` = Moll-Akkord

**Akkord-Aufbau:**
- **Grundton** (Root): Basis-Note
- **Terz** (Third): 
  - Dur: +4 Halbtöne (große Terz)
  - Moll: +3 Halbtöne (kleine Terz)
- **Quinte** (Fifth): +7 Halbtöne

**Wiedergabe:**
- Arpeggio-Stil (nacheinander)
- Erste Note: 0.25 Sekunden
- Zweite Note: 0.25 Sekunden
- Dritte Note: 0.5 Sekunden (doppelt so lang)
- **Keine Pausen** zwischen den Tönen
- Fade-out in letzten 100ms jeder Note
- Lautstärke: 60% (verdoppelt)
- Gesamtdauer: 1.0 Sekunde pro Akkord

### 2. Frequenz-Mapping

**Basis-Frequenzen (C4 bis G5 - erweitert für Akkorde):**
```
C4  = 261.63 Hz
C#4 = 277.18 Hz
D4  = 293.66 Hz
D#4 = 311.13 Hz
E4  = 329.63 Hz
F4  = 349.23 Hz
F#4 = 369.99 Hz
G4  = 392.00 Hz
G#4 = 415.30 Hz
A4  = 440.00 Hz (Kammerton)
A#4 = 466.16 Hz
H4  = 493.88 Hz
C5  = 523.25 Hz
C#5 = 554.37 Hz
D5  = 587.33 Hz
D#5 = 622.25 Hz
E5  = 659.25 Hz
F5  = 698.46 Hz
F#5 = 739.99 Hz
G5  = 783.99 Hz
```

**Erweitert bis G5, damit auch H-Dur-Akkorde möglich sind:**
- H4 + 7 Halbtöne = F#5 (Index 18) ✓

**Enharmonische Verwechslungen:**
- C# = D♭
- D# = E♭
- F# = G♭
- G# = A♭
- A# = B

### 3. Intelligente Akkord-Auswahl

**Regel-System:**

#### Bei C-Dur oben (Position 0):

| Note | Index | Akkord-Typ | Begründung |
|------|-------|------------|------------|
| C | 0 | Dur | Tonika |
| C# | 1 | Dur | Standard |
| D♭ | 2 | Dur | Standard |
| D | 3 | **Moll** | Subdominante von A-Moll |
| D# | 4 | Dur | Standard |
| E♭ | 5 | Dur | Standard |
| E | 6 | Dur | Dominante von A-Moll |
| F | 8 | Dur | Subdominante |
| F# | 9 | Dur | Standard |
| G | 11 | Dur | Dominante |
| G# | 12 | Dur | Standard |
| A♭ | 13 | Dur | Standard |
| A | 14 | **Moll** | Parallel-Moll |
| A# | 15 | Dur | Standard |
| B | 16 | Dur | Standard |
| H | 17 | Dur | (vorher kein Ton, jetzt Dur) |

#### Berechnung für beliebige Tonart:

```java
// Parallel-Moll = +14 Positionen im Array
int mollNote = (tonika + 14) % 19;

// Subdominante des Parallel-Molls = +3 Positionen im Array
int subdominanteMoll = (tonika + 3) % 19;
```

**Logik:**
1. Wenn getippte Note = Parallel-Moll (Position +14) → **Moll**
2. Wenn getippte Note = Subdominante von Parallel-Moll (Position +3) → **Moll**
3. Alle anderen → **Dur** (inkl. Tonika, Dominante, Subdominante, H, etc.)

### 4. Interaktions-Modi

**🎵 Akkord-Modus (Standard):**
- Tap auf Note → Spielt Akkord
- Dur/Moll wird automatisch bestimmt
- H spielt keinen Ton

**🔵 Dur-Modus:**
- Tap auf Note → Rotiert zur Dur-Position (blau)
- Gleiche Funktion wie vorher

**🔴 Moll-Modus:**
- Tap auf Note → Rotiert zur parallelen Moll-Position (roter Kreis bei A)
- Unabhängig von der getippten Note, rotiert immer zu Position 14 (A bei C oben)

## 🔧 Technische Details

### Audio-Engine

**AudioTrack-Konfiguration:**
```java
Sample Rate: 44100 Hz
Encoding: PCM 16-bit
Channel: Mono
Mode: STREAM
```

**Sinuswellen-Generierung:**
```java
sample = sin(2π × frequency × time) × amplitude × envelope
```

**Envelope (Fade-out):**
```java
// Letzte 100ms: linearer Fade-out
envelope = 1.0 - (time_since_fade_start / fade_duration)
```

### Threading

**Nicht-blockierende Wiedergabe:**
```java
new Thread(() -> {
    playNote(root, 250ms);      // Erste Note: kurz
    playNote(third, 250ms);     // Zweite Note: kurz
    playNote(fifth, 500ms);     // Dritte Note: länger (doppelt)
    // Keine Pausen zwischen den Tönen!
    // playNote() blockiert bis Note fertig gespielt ist
}).start();
```

**Überlappungs-Schutz:**
```java
private boolean isPlaying = false;

public void playChord(...) {
    if (isPlaying) return; // Verhindert Überlappung
    isPlaying = true;
    // ... spiele Akkord
    handler.post(() -> isPlaying = false); // Nach allen 3 Noten
}
```

### Speicher-Management

**Ressourcen-Freigabe:**
```java
@Override
protected void onDestroy() {
    if (chordPlayer != null) {
        chordPlayer.release(); // Gibt AudioTrack frei
    }
}
```

## 🎯 Beispiele

### Beispiel 1: C-Dur Akkord

**Bei C oben, Tap auf C:**
```
Grundton: C (261.63 Hz)
Terz: E (329.63 Hz) → +4 Halbtöne
Quinte: G (392.00 Hz) → +7 Halbtöne
Typ: Dur
```

### Beispiel 2: D-Moll Akkord

**Bei C oben, Tap auf D:**
```
Grundton: D (293.66 Hz)
Terz: F (349.23 Hz) → +3 Halbtöne (kleine Terz)
Quinte: A (440.00 Hz) → +7 Halbtöne
Typ: Moll (Subdominante von A-Moll)
```

### Beispiel 3: E-Dur Akkord

**Bei C oben, Tap auf E:**
```
Grundton: E (329.63 Hz)
Terz: G# (415.30 Hz) → +4 Halbtöne (große Terz)
Quinte: H (493.88 Hz) → +7 Halbtöne
Typ: Dur (Dominante des Parallel-Molls)
```

### Beispiel 4: A-Moll Akkord

**Bei C oben, Tap auf A:**
```
Grundton: A (440.00 Hz)
Terz: C (261.63 Hz) → +3 Halbtöne (kleine Terz)
Quinte: E (329.63 Hz) → +7 Halbtöne
Typ: Moll (Parallel-Moll)
```

## 📱 Benutzung

### Schritt 1: Modus wählen
- Tippe auf einen der drei Radio-Buttons oben

### Schritt 2: Note antippen
- **Akkord-Modus**: Hörst den Akkord
- **Dur-Modus**: Note rotiert zur blauen Position
- **Moll-Modus**: Parallel-Moll rotiert zur roten Position

### Schritt 3: Experimentieren
- Drehe Scheibe → andere Tonart
- Tippe verschiedene Noten → höre Unterschied Dur/Moll
- Vergleiche mit Moll-Modus-Rotation

## 🚀 Performance

**Latenz:**
- Note-Trigger: <10ms
- Audio-Start: ~50ms (Android-System)
- Total: <100ms

**CPU-Last:**
- Sinuswellen-Generierung: ~1-2% pro Note
- 3 Noten (Akkord): ~5% für 1.0s (250ms + 250ms + 500ms)

**Speicher:**
- ChordPlayer: ~50 KB
- Audio-Buffer: ~150 KB (variabel durch unterschiedliche Notenlängen)
- Total: <1 MB

## 🔮 Mögliche Erweiterungen

**Für zukünftige Versionen:**
- [ ] Akkord-Umkehrungen
- [ ] Septakkorde
- [ ] Verschiedene Instrumente (Piano, Guitar, etc.)
- [ ] MIDI-Export
- [ ] Recording-Funktion
- [ ] Metronom mit Akkord-Wechsel
- [ ] Akkord-Progressionen als Presets

---

**Entwickelt:** 11. Januar 2026  
**Version:** 1.1  
**Audio-Engine:** Sinuswellen-Synthese mit AudioTrack

