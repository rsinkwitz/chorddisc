# Tonleiter-Feature - Implementierungsstatus

## ✅ Was wurde implementiert:

### 1. **UI-Erweiterung**
- ✅ Neue Radio-Gruppe für Tonleiter-Modi
- ✅ 4 Optionen: 🎵 Akkord | 🎼 Tonleiter | 🔵 Dur | 🔴 Moll
- ✅ Dritte Radio-Gruppe: Dur-Tonleiter | Nat. Moll | Harm. Moll

### 2. **ChordPlayer erweitert**
- ✅ `playScale()` Methode implementiert
- ✅ Tonleiter-Intervalle für alle drei Modi
- ✅ Auf- und abwärts spielen
- ✅ Callback-Interface für visuelle Hervorhebung

### 3. **Tonleiter-Intervalle**
```java
Dur:          0, 2, 4, 5, 7, 9, 11, 12 (W-W-H-W-W-W-H)
Nat. Moll:    0, 2, 3, 5, 7, 8, 10, 12 (W-H-W-W-H-W-W)
Harm. Moll:   0, 2, 3, 5, 7, 8, 11, 12 (W-H-W-W-H-1.5-H)
```

## 🚧 Noch zu implementieren:

### 1. **ChordDiscView Integration**
```java
// In handleNoteTap():
case PLAY_SCALE:
    playScaleForNote(tappedNoteIndex);
    break;

// Neue Methode:
private void playScaleForNote(int noteIndex) {
    ChordPlayer.ScaleType playerScaleType;
    switch (scaleType) {
        case MAJOR: playerScaleType = ChordPlayer.ScaleType.MAJOR; break;
        case NATURAL_MINOR: playerScaleType = ChordPlayer.ScaleType.NATURAL_MINOR; break;
        case HARMONIC_MINOR: playerScaleType = ChordPlayer.ScaleType.HARMONIC_MINOR; break;
    }
    
    chordPlayer.playScale(noteIndex, playerScaleType, new ChordPlayer.ScaleNoteCallback() {
        @Override
        public void onNotePlay(int rootIndex, int intervalInHalftones) {
            // Berechne welche Note hervorgehoben werden soll
            highlightedNoteIndex = calculateNoteIndexFromInterval(rootIndex, intervalInHalftones);
            invalidate(); // Neu zeichnen
        }
        
        @Override
        public void onScaleFinished() {
            highlightedNoteIndex = -1;
            invalidate();
        }
    });
}
```

### 2. **Visuelle Hervorhebung**
```java
// In drawBottomDisc(), nach dem Zeichnen der Note:
if (i == highlightedNoteIndex) {
    // Gelber Kreis um hervorgehobene Note
    Paint highlightPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    highlightPaint.setColor(Color.YELLOW);
    highlightPaint.setStyle(Paint.Style.STROKE);
    highlightPaint.setStrokeWidth(6f);
    canvas.drawCircle(0, 0, noteCircleRadius * 1.2f, highlightPaint);
}
```

### 3. **Löcher dynamisch ändern**
```java
// POSITIONS Array durch Methode ersetzen:
private boolean shouldShowHole(int index) {
    switch (scaleType) {
        case MAJOR:
            return POSITIONS[index].hole; // Original Dur-Löcher
            
        case NATURAL_MINOR:
            // Löcher für natürliches Moll (Relativ zur aktuellen Tonika)
            // Berechne welche Noten in nat. Moll-Tonleiter sind
            return isInNaturalMinorScale(index);
            
        case HARMONIC_MINOR:
            // Löcher für harmonisches Moll
            return isInHarmonicMinorScale(index);
    }
}
```

### 4. **MainActivity erweitern**
```java
// Setup RadioGroup für Tonleiter-Typ
RadioGroup scaleTypeRadioGroup = findViewById(R.id.scaleTypeRadioGroup);
scaleTypeRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
    if (checkedId == R.id.radioMajorScale) {
        chordDiscView.setScaleType(ChordDiscView.ScaleType.MAJOR);
    } else if (checkedId == R.id.radioNaturalMinor) {
        chordDiscView.setScaleType(ChordDiscView.ScaleType.NATURAL_MINOR);
    } else if (checkedId == R.id.radioHarmonicMinor) {
        chordDiscView.setScaleType(ChordDiscView.ScaleType.HARMONIC_MINOR);
    }
});

// PLAY_SCALE im Tap-Modus-Handler:
} else if (checkedId == R.id.radioPlayScale) {
    chordDiscView.setTapMode(ChordDiscView.TapMode.PLAY_SCALE);
}
```

### 5. **"Harmonie"-Modus deaktivieren bei Moll**
```java
// In MainActivity, beim Wechsel der Tonleiter:
scaleTypeRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
    // ... set scale type ...
    
    // "Harmonie" nur bei Dur verfügbar
    RadioButton harmonicButton = findViewById(R.id.radioHarmonic);
    if (checkedId == R.id.radioMajorScale) {
        harmonicButton.setEnabled(true);
    } else {
        harmonicButton.setEnabled(false);
        // Wenn Harmonie aktiv war, wechsle zu "Alles Dur" oder "Alles Moll"
        if (chordTypeRadioGroup.getCheckedRadioButtonId() == R.id.radioHarmonic) {
            chordTypeRadioGroup.check(R.id.radioAllMajor);
        }
    }
});
```

## 📊 Erwartetes Verhalten:

### **Dur-Tonleiter (C-Dur bei C oben):**
```
Aufwärts:  C → D → E → F → G → A → H → C'
Abwärts:   H → A → G → F → E → D → C (länger)
```
- Löcher wie bisher (Original)
- Gelbe Hervorhebung wandert mit

### **Natürliches Moll (A-Moll bei C oben):**
```
Aufwärts:  A → H → C → D → E → F → G → A'
Abwärts:   G → F → E → D → C → H → A (länger)
```
- Löcher ändern sich (nur Noten der nat. Moll-Tonleiter)
- Gelbe Hervorhebung wandert mit

### **Harmonisches Moll (A-Moll bei C oben):**
```
Aufwärts:  A → H → C → D → E → F → G# → A'
Abwärts:   G# → F → E → D → C → H → A (länger)
```
- Löcher ändern sich (nur Noten der harm. Moll-Tonleiter)
- G# statt G!

## 🎯 Nächste Schritte zur Fertigstellung:

1. ✅ Layout erweitert
2. ✅ ChordPlayer.playScale() implementiert
3. ⏳ ChordDiscView Integration (handleNoteTap)
4. ⏳ Visuelle Hervorhebung (drawBottomDisc)
5. ⏳ Dynamische Löcher (shouldShowHole)
6. ⏳ MainActivity Event-Handler
7. ⏳ "Harmonie"-Button deaktivieren bei Moll

## 💡 Vereinfachte Alternative (schnell umsetzbar):

Falls die vollständige Implementierung zu komplex ist, hier eine einfachere Version:

### **Ohne Loch-Änderung:**
- Löcher bleiben immer Dur-Konfiguration
- Nur Audio spielt die richtige Tonleiter
- Visuelle Hervorhebung funktioniert trotzdem

### **Ohne visuelle Hervorhebung:**
- Tonleiter wird nur abgespielt
- Keine gelbe Markierung
- Einfacher zu implementieren

---

**Status: Grundgerüst steht, Integration in ChordDiscView und MainActivity fehlt noch**
**Zeitaufwand für Vollendung: ~30-45 Minuten**

