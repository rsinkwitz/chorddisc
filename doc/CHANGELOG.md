# Changelog - Tonarten-Scheibe App

## Version 1.1 (11. Januar 2026)

### ✨ Neue Features

#### Audio-Funktionalität
- ✅ **Akkord-Synthese** - Echtzeit-Generierung von Akkorden mittels Sinuswellen
- ✅ **Arpeggio-Wiedergabe** - Noten nacheinander für natürlichen Klang
- ✅ **Dur/Moll-Erkennung** - Automatische Akkord-Typ-Auswahl basierend auf Kontext
- ✅ **Intelligente Moll-Logik** - Dominante & Subdominante des Parallel-Molls = Moll-Akkord
- ✅ **H-Exception** - Verminderte Septime spielt keinen Ton

#### Interaktions-Modi (Radio-Gruppe)
- ✅ **🎵 Akkord-Modus** - Tap auf Note → Spielt Akkord (Dur oder Moll)
- ✅ **🔵 Dur-Modus** - Tap auf Note → Rotiert zur Dur-Position (blau)
- ✅ **🔴 Moll-Modus** - Tap auf Note → Rotiert zur parallelen Moll-Position (rot)

#### UI-Verbesserungen
- ✅ **Radio-Gruppe** - Kompakte 3-Button-Leiste oben
- ✅ **Emoji-Icons** - Intuitive visuelle Modus-Anzeige
- ✅ **Responsive Layout** - RadioGroup + ScrollView-Bereich

### 🎵 Musikalische Logik

**Akkord-Bestimmung bei C-Dur oben:**
```
Dur-Akkorde: C, D, E, F, G (Standard-Positionen)
Moll-Akkorde: A-Moll (Dominante von A-Moll)
              D-Moll (Subdominante von A-Moll)
Kein Ton: H (vermindert)
```

**Technische Umsetzung:**
- Parallel-Moll: -5 Positionen im Quintenzirkel (3 Halbtöne)
- Dominante: +12 Positionen (7 Halbtöne)
- Subdominante: +7 Positionen (5 Halbtöne)

### 🔧 Technische Verbesserungen

**Audio-Engine:**
- AudioTrack mit 44.1kHz Sampling
- Sinuswellen-Synthese in Echtzeit
- Fade-out Envelope für natürlichen Klang
- Threading für non-blocking Wiedergabe

**Performance:**
- 0.5s pro Note (Arpeggio)
- Überlappungs-Schutz (nur ein Akkord gleichzeitig)
- Low-Latency Modus (API 26+, fallback für API 21)

### 📊 Code-Statistik

**Neue Dateien:**
- `ChordPlayer.java` - 180 Zeilen (Audio-Engine)

**Erweiterte Dateien:**
- `MainActivity.java` - +30 Zeilen (RadioGroup-Handling)
- `ChordDiscView.java` - +85 Zeilen (TapMode-Logik)
- `activity_main.xml` - Komplettes Redesign

---

## Version 1.0 (11. Januar 2026)

### ✨ Implementierte Features

#### Kern-Funktionalität
- ✅ **Zwei-Scheiben-System** mit 19 chromatischen Noten
- ✅ **Echte Transparenz** in oberer Scheibe mittels PorterDuff.Mode.CLEAR
- ✅ **Opaker Rahmen** um herausragende Texte zu verdecken
- ✅ **Responsive Design** nutzt 96% der Bildschirmbreite

#### Interaktions-Modi
- ✅ **Manual Drag** - Scheibe durch Wischen drehen
- ✅ **Snap-to-Position** - Automatisches Einrasten (0.5s Animation)
- ✅ **Tap-to-Rotate** - Note antippen → rotiert nach oben zur blauen Markierung

#### Visuelle Features
- ✅ **Aufrechte Textdarstellung** - Text dreht gegen Scheibendrehung
- ✅ **Dynamische Vorzeichen** - Nur aktuelle Tonart im Rechteck-Indikator
- ✅ **Dur/Moll-Markierungen** - Blauer (C) und roter (A) Kreis
- ✅ **Transparente Löcher** - 10 Tonleiter-Töne sichtbar

#### Technische Verbesserungen
- ✅ **Koordinatensystem-Konvertierung** - atan2 ↔ Canvas-Koordinaten
- ✅ **Optimierte Drehrichtung** - Negative Rotation für korrekte Notenbewegung
- ✅ **Intelligente Touch-Erkennung** - Unterscheidet Tap vs. Drag (20px Threshold)
- ✅ **Shortest-Path-Rotation** - Dreht nie mehr als 180°

### 🐛 Behobene Probleme

#### Phase 1: Transparenz
- Problem: Weiße Kreise statt echter Löcher
- Lösung: Bitmap-Layer mit PorterDuff.Mode.CLEAR

#### Phase 2: Koordinatensysteme
- Problem: Touch-Koordinaten (atan2) vs. Canvas-Koordinaten
- Lösung: Konvertierung bei Touch-Input (+90°)

#### Phase 3: Drehrichtung
- Problem: Falsche Note rotierte nach oben
- Lösung: Negative Rotation verwenden (canvas.rotate im Uhrzeigersinn)

#### Phase 4: Vorzeichen-Position
- Problem: Vorzeichen nur 10% im Rechteck sichtbar
- Lösung: Y-Position-Anpassung mit indicatorSize * 0.1f

#### Phase 5: Tap-Funktion
- Problem: Falsche Noten erkannt, falsche Ziel-Rotation
- Lösung: Vollständige Koordinatensystem-Vereinheitlichung

### 📊 Technische Spezifikationen

**Plattform:**
- Min SDK: Android 5.0 (API 21)
- Target SDK: Android 13 (API 33)
- Sprache: Java
- Build-System: Gradle 8.0

**Abhängigkeiten:**
- androidx.appcompat:appcompat:1.6.1

**Performance:**
- 60 FPS bei Drag-Operationen
- 0.5s Animationsdauer (Snap & Tap-to-Rotate)
- DecelerateInterpolator für natürliche Bewegung

**Layout:**
- Portrait-Orientierung
- Responsive: 96% der kleineren Bildschirmdimension
- Automatische Textgrößen-Anpassung

### 🎯 Musikalische Korrektheit

**19 Positionen im chromatischen Kreis:**
- Winkel pro Position: 18.947° (360° / 19)
- 10 Löcher für Dur-Tonleitern
- Korrekte Vorzeichen-Anzeige (♯ und ♭)
- Parallel-Moll-Anzeige (3 Halbtöne tiefer)

**Unterstützte Tonarten:**
- Alle 12 Dur-Tonarten
- Alle 12 Moll-Tonarten
- Enharmonische Verwechslungen (D♭/C♯, etc.)

### 📝 Code-Qualität

**Dokumentation:**
- Inline-Kommentare in allen Methoden
- README.md mit Übersicht
- SPEZIFIKATION.md mit technischen Details
- SCHNELLSTART.md mit Anleitung
- PROJEKTÜBERSICHT.md mit Zusammenfassung

**Code-Organisation:**
- 2 Java-Klassen: MainActivity, ChordDiscView
- Clean Architecture: View-Logic getrennt
- Keine externen Abhängigkeiten außer AndroidX

### 🚀 Nächste mögliche Features

**Für zukünftige Versionen:**
- [ ] Sound-Feedback beim Notenwechsel
- [ ] Akkord-Anzeige für aktuelle Tonart
- [ ] Dark Mode / Themes
- [ ] Landscape-Orientierung
- [ ] Einstellungen (Snap-Geschwindigkeit, etc.)
- [ ] Internationalisierung (EN, DE, etc.)
- [ ] Akkord-Progressionen anzeigen
- [ ] MIDI-Output für externe Instrumente

---

**Entwickelt:** 11. Januar 2026  
**Status:** ✅ Produktionsreif  
**Lizenz:** [Ihre Lizenz hier]

