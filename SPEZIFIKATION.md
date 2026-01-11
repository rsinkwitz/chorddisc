# Android-App: Tonarten-Scheibe (Circle of Fifths)

## 🎵 Projektübersicht

Diese Android-App implementiert einen interaktiven **Quintenzirkel** (Circle of Fifths) als digitale Version einer physischen Tonarten-Scheibe. Sie dient als praktisches Hilfsmittel für Musiker zum schnellen Ablesen von:

- Tonarten (Dur und Moll)
- Tonleitern
- Vorzeichen (Kreuze und Bs)

## ✨ Hauptfunktionen

### 1. **Interaktive Drehscheibe**
   - Untere Scheibe durch Wischgesten drehbar
   - Obere Scheibe fest mit Ausschnitten (Löchern)
   - Flüssige Touch-Steuerung

### 2. **Musikalische Informationen**
   - **19 chromatische Positionen** mit allen Dur- und Moll-Tonarten
   - **Blauer Kreis**: Markiert die gewählte Dur-Tonart
   - **Roter Kreis**: Zeigt die parallele Moll-Tonart
   - **Löcher**: Zeigen die Töne der Tonleiter
   - **Rechteck**: Zeigt Anzahl der Vorzeichen (♯ oder ♭)

### 3. **Intelligente Textdarstellung**
   - Text rotiert automatisch entgegen der Scheibendrehung
   - Alle Beschriftungen bleiben stets aufrecht lesbar
   - **Vorteil gegenüber der Papierversion!**

## 📱 Technische Spezifikationen

### Systemanforderungen
- **Minimum**: Android 5.0 (API Level 21)
- **Target**: Android 13 (API Level 33)
- **Orientierung**: Portrait (Hochformat)

### Entwicklungsumgebung
- **Sprache**: Java
- **Build-System**: Gradle
- **UI-Framework**: Custom View (Canvas-basiert)
- **IDE**: Android Studio

### Architektur
```
├── MainActivity.java          # Einstiegspunkt der App
└── ChordDiscView.java         # Custom View mit gesamter Logik
    ├── Zeichnen der Scheiben
    ├── Touch-Event-Handling
    └── Rotationslogik
```

## 🎨 Visuelle Gestaltung

### Farbschema
- **Schwarz**: Hauptlinien und Text
- **Weiß**: Hintergrund und Flächen
- **Blau**: Dur-Tonart Markierung
- **Rot**: Moll-Tonart Markierung
- **Hellgrau**: App-Hintergrund (#F5F5F5)

### Proportionen
Alle Dimensionen sind relativ zur Bildschirmgröße:
- Äußerer Radius: 40% der kleineren Bildschirmdimension
- Noten-Kreis: 13% des äußeren Radius
- Noten-Position: 80% des äußeren Radius
- Indikator: 55% des äußeren Radius

## 🎼 Musikalische Daten

### 19-teiliger Quintenzirkel
```
Position  Note      Typ   Vorzeichen
   0      C        Dur      0
   1      C♯       -        7♯
   2      D♭       -        5♭
   3      D        Dur      2♯
   4      D♯       -        -
   5      E♭       -        3♭
   6      E        Dur      4♯
   7      E♯,F♭    -        -
   8      F        Dur      1♭
   9      F♯       -        6♯
  10      G♭       -        -
  11      G        Dur      1♯
  12      G♯       -        -
  13      A♭       -        4♭
  14      A        Dur      3♯  (auch Moll-Position)
  15      A♯       -        -
  16      B        -        2♭
  17      H        Dur      5♯
  18      H♯,C♭    -        -
```

- **Dur-Position (blau)**: Index 0 (C-Dur)
- **Moll-Position (rot)**: Index 14 (A-Moll)
- **Löcher**: Nur bei `hole = true` (reine Dur-Tonarten)

## 🚀 Installation und Build

### Voraussetzungen installieren
1. **Android Studio** herunterladen: https://developer.android.com/studio
2. **JDK 8+** installieren (meist in Android Studio enthalten)
3. Android SDK über Android Studio installieren

### Projekt öffnen
1. Android Studio starten
2. "Open an Existing Project" wählen
3. Dieses Verzeichnis auswählen
4. Gradle Sync abwarten (erfolgt automatisch)

### App ausführen
**Option 1: Emulator**
```
1. AVD Manager öffnen (Tools > Device Manager)
2. Virtuelles Gerät erstellen (z.B. Pixel 5, API 33)
3. Emulator starten
4. Run > Run 'app' (oder Shift+F10)
```

**Option 2: Echtes Gerät**
```
1. USB-Debugging auf dem Android-Gerät aktivieren
   (Einstellungen > Entwickleroptionen > USB-Debugging)
2. Gerät per USB verbinden
3. Run > Run 'app' (oder Shift+F10)
```

### APK erstellen (für Verbreitung)
```bash
# Debug-Version
gradlew assembleDebug

# Release-Version (unsigned)
gradlew assembleRelease
```

APK-Speicherort: `app/build/outputs/apk/`

## 📖 Verwendungsanleitung

### Starten der App
1. App-Icon "Tonarten-Scheibe" antippen
2. App öffnet sich im Vollbildmodus

### Bedienung
1. **Drehen**: Mit dem Finger auf der äußeren Scheibe wischen (kreisförmig)
2. **Tonart einstellen**: Gewünschte Note im blauen Kreis positionieren
3. **Ablesen**:
   - Im **blauen Kreis**: Aktuelle Dur-Tonart
   - Im **roten Kreis**: Parallele Moll-Tonart
   - In den **Löchern**: Alle Töne der Tonleiter (von unten sichtbar)
   - Im **Rechteck**: Anzahl und Art der Vorzeichen

### Beispiel: C-Dur einstellen
- "C" im blauen Kreis positionieren
- Rechteck zeigt "0" (keine Vorzeichen)
- Roter Kreis zeigt "A" (A-Moll ist die Parallele)
- Löcher zeigen: C, D, E, F, G, H (C-Dur Tonleiter)

## 🔧 Anpassungsmöglichkeiten

### Text-Größe ändern
In `ChordDiscView.java`, Methode `onSizeChanged()`:
```java
textPaint.setTextSize(outerRadius * 0.12f);  // Faktor ändern
textPaintSmall.setTextSize(outerRadius * 0.09f);
```

### Farben anpassen
In `ChordDiscView.java`, Methode `initPaints()`:
```java
majorPaint.setColor(Color.BLUE);  // Dur-Farbe
minorPaint.setColor(Color.RED);   // Moll-Farbe
```

### Startposition ändern
In `ChordDiscView.java`:
```java
private float bottomDiscRotation = 0f;  // Winkel in Grad
```

## 🐛 Fehlerbehebung

### Problem: Gradle Sync fehlgeschlagen
**Lösung**: 
- Android Studio aktualisieren
- Gradle-Version in `gradle/wrapper/gradle-wrapper.properties` prüfen
- Internet-Verbindung prüfen

### Problem: App stürzt beim Start ab
**Lösung**:
- LogCat in Android Studio prüfen
- Minimale API-Version des Geräts kontrollieren (muss >= 21 sein)

### Problem: Touch-Gesten werden nicht erkannt
**Lösung**:
- Auf den äußeren Ring der Scheibe tippen (nicht Zentrum)
- Kreisförmige Wischbewegung ausführen

## 📂 Projektstruktur (vollständig)

```
chord_disc/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/chorddisc/
│   │   │   │   ├── MainActivity.java
│   │   │   │   └── ChordDiscView.java
│   │   │   ├── res/
│   │   │   │   ├── values/
│   │   │   │   │   ├── strings.xml
│   │   │   │   │   └── colors.xml
│   │   │   │   ├── mipmap-*/
│   │   │   │   │   └── ic_launcher.xml
│   │   │   │   └── mipmap-anydpi-v26/
│   │   │   │       ├── ic_launcher.xml
│   │   │   │       └── ic_launcher_foreground.xml
│   │   │   └── AndroidManifest.xml
│   │   └── old-source/
│   │       ├── CircleCreate2.java
│   │       └── tonleitern-byjava2.svg
│   ├── build.gradle
│   └── proguard-rules.pro
├── gradle/
│   └── wrapper/
│       └── gradle-wrapper.properties
├── build.gradle
├── settings.gradle
├── gradle.properties
├── .gitignore
├── README.md
├── SPEZIFIKATION.md
└── prompt.txt
```

## 🎓 Pädagogischer Nutzen

Diese App ist ideal für:
- **Musikstudenten**: Schnelles Nachschlagen von Tonarten
- **Komponisten**: Übersicht über Tonarten und Modulationen
- **Instrumentallehrer**: Visualisierung des Quintenzirkels
- **Hobby-Musiker**: Verständnis von Tonarten und Vorzeichen

## 🔄 Verbesserungsideen (für die Zukunft)

1. **Snap-to-Grid**: Automatisches Einrasten auf exakte Positionen
2. **Sound-Feedback**: Abspielen der Tonleiter beim Drehen
3. **Info-Dialog**: Detaillierte Erklärung zur gewählten Tonart
4. **Themes**: Hell-/Dunkel-Modus
5. **Export**: Screenshot-Funktion für aktuelle Position
6. **Sprachen**: Englische Notennamen (B statt H, etc.)
7. **Akkorde**: Anzeige der Haupt-Akkorde (I, IV, V)

## 📄 Lizenz

Dieses Projekt ist frei verfügbar für Bildungszwecke und private Nutzung.

## 👨‍💻 Entwicklung

Basierend auf der ursprünglichen SVG-Generator-Implementierung in Java, wurde diese App als moderne Android-Native-Lösung mit Custom Canvas Drawing neu entwickelt.

**Original**: `CircleCreate2.java` (SVG-Generator)  
**Neu**: `ChordDiscView.java` (Interactive Android View)

---

**Viel Erfolg beim Musizieren! 🎶**

