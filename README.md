# Chord Disc - Android App

Eine interaktive Android-App zur Darstellung des musikalischen Quintenzirkels (Circle of Fifths).

## Features

- **Zwei rotierende Scheiben**: Eine untere drehbare und eine obere feste Scheibe
- **Interaktive Steuerung**: Drehen der unteren Scheibe durch Wischen mit dem Finger
- **Aufrechte Textdarstellung**: Text rotiert automatisch entgegen der Scheibendrehung
- **Musikalische Informationen**:
  - Tonart-Anzeige im blauen Kreis (Dur)
  - Paralleltonart im roten Kreis (Moll)
  - Tonleiter sichtbar durch die Löcher
  - Anzahl der Kreuze (♯) oder Bs (♭) im Indikator-Rechteck
- **Kompatibilität**: Unterstützt Android 5.0 (API 21) und höher

## Installation

### Voraussetzungen
- Android Studio (neueste Version empfohlen)
- JDK 8 oder höher
- Android SDK

### Build-Anleitung

1. Projekt in Android Studio öffnen
2. Gradle Sync durchführen (falls nicht automatisch geschehen)
3. App auf Emulator oder echtem Gerät ausführen:
   - Menu: Run > Run 'app'
   - Oder Shortcut: Shift+F10

### APK erstellen

```
gradlew assembleRelease
```

Die APK befindet sich dann in: `app/build/outputs/apk/release/`

## Verwendung

1. App starten
2. Mit dem Finger auf der äußeren Scheibe wischen, um sie zu drehen
3. Die gewünschte Tonart im blauen Kreis einstellen
4. Ablesen:
   - **Blauer Kreis**: Dur-Tonart
   - **Roter Kreis**: Parallele Moll-Tonart
   - **Löcher**: Töne der Tonleiter
   - **Rechteck**: Anzahl der Vorzeichen (Kreuze oder Bs)

## Technische Details

- **Programmiersprache**: Java
- **Min SDK**: 21 (Android 5.0 Lollipop)
- **Target SDK**: 33 (Android 13)
- **Dependencies**: androidx.appcompat
- **Custom View**: Eigene Canvas-basierte Darstellung für optimale Performance

## Struktur

```
app/
├── src/
│   ├── main/
│   │   ├── java/com/chorddisc/
│   │   │   ├── MainActivity.java       # Haupt-Activity
│   │   │   └── ChordDiscView.java      # Custom View für die Scheiben
│   │   ├── res/
│   │   │   ├── values/
│   │   │   │   ├── strings.xml
│   │   │   │   └── colors.xml
│   │   │   └── mipmap-*/              # App Icons
│   │   └── AndroidManifest.xml
│   └── old-source/                     # Original Java/SVG Code
└── build.gradle
```

## Lizenz

Dieses Projekt dient als Hilfsmittel für Musiker und Musikstudenten.

