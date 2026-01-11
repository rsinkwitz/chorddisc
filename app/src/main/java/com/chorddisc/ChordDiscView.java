package com.chorddisc;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

/**
 * Custom View für die interaktive Akkord-Scheibe.
 * Zeigt zwei konzentrische Kreise mit musikalischen Tonarten,
 * wobei die untere Scheibe durch Wischen gedreht werden kann.
 */
public class ChordDiscView extends View {

    // Tap-Modi
    public enum TapMode {
        PLAY_CHORD,         // Spiele Akkord
        ROTATE_TO_DUR,      // Rotiere zur Dur-Position (blau)
        ROTATE_TO_MOLL      // Rotiere zur Moll-Position (rot)
    }

    // Akkord-Typen
    public enum ChordType {
        ALL_MAJOR,          // Alle Akkorde in Dur
        ALL_MINOR,          // Alle Akkorde in Moll
        HARMONIC            // Dur/Moll nach harmonischer Logik
    }

    // Tonleiter-Typen
    public enum ScaleType {
        MAJOR,              // Dur-Tonleiter
        NATURAL_MINOR,      // Natürliches Moll
        HARMONIC_MINOR      // Harmonisches Moll
    }

    // Musikalische Daten - 19 Positionen im Quintenzirkel
    private static final MusicalPosition[] POSITIONS = {
        new MusicalPosition("C", true, "0"),
        new MusicalPosition("C♯", false, "7♯"),
        new MusicalPosition("D♭", false, "5♭"),
        new MusicalPosition("D", true, "2♯"),
        new MusicalPosition("D♯", false, ""),
        new MusicalPosition("E♭", false, "3♭"),
        new MusicalPosition("E", true, "4♯"),
        new MusicalPosition("E♯,F♭", false, ""),
        new MusicalPosition("F", true, "1♭"),
        new MusicalPosition("F♯", false, "6♯"),
        new MusicalPosition("G♭", false, ""),
        new MusicalPosition("G", true, "1♯"),
        new MusicalPosition("G♯", false, ""),
        new MusicalPosition("A♭", false, "4♭"),
        new MusicalPosition("A", true, "3♯"),
        new MusicalPosition("A♯", false, ""),
        new MusicalPosition("B", false, "2♭"),
        new MusicalPosition("H", true, "5♯"),
        new MusicalPosition("H♯,C♭", false, "")
    };

    private static final int MAJOR_POS = 0;  // Position für Dur (blau)
    private static final int MINOR_POS = 14; // Position für Moll (rot)

    // Paint-Objekte für verschiedene Zeichenelemente
    private Paint circlePaint;
    private Paint holePaint;
    private Paint textPaint;
    private Paint textPaintSmall;
    private Paint majorPaint;
    private Paint minorPaint;
    private Paint rectPaint;
    private Paint topDiscPaint;
    private Paint clearPaint;

    // Geometrie
    private float centerX;
    private float centerY;
    private float outerRadius;
    private float innerRadius;
    private float noteCircleRadius;
    private float notePositionRadius;
    private float indicatorPositionRadius;
    private float indicatorSize;

    // Rotation der unteren Scheibe in Grad
    private float bottomDiscRotation = 0f;

    // Touch-Handling
    private float lastTouchAngle = 0f;
    private boolean isDragging = false;
    private float touchDownX = 0f;
    private float touchDownY = 0f;
    private static final float TAP_THRESHOLD = 20f; // Pixel-Toleranz für Tap vs. Drag

    // Snap-Animation
    private ValueAnimator snapAnimator;
    private static final float ANGLE_PER_POSITION = 360f / 19f; // 18.947°
    private static final int SNAP_DURATION_MS = 500; // 0.5 Sekunden

    // Audio & Modus
    private ChordPlayer chordPlayer;
    private TapMode tapMode = TapMode.ROTATE_TO_DUR; // Standard-Modus
    private ChordType chordType = ChordType.HARMONIC; // Standard: Harmonisch
    private ScaleType scaleType = ScaleType.MAJOR; // Standard: Dur-Tonleiter
    private int highlightedNoteIndex = -1; // -1 = keine Hervorhebung
    private boolean isTopDiscTransparent = false; // Transparenz der oberen Scheibe

    public ChordDiscView(Context context) {
        super(context);
        this.chordPlayer = null;
        initPaints();
    }

    public ChordDiscView(Context context, ChordPlayer chordPlayer) {
        super(context);
        this.chordPlayer = chordPlayer;
        initPaints();
    }

    public void setTapMode(TapMode mode) {
        this.tapMode = mode;
    }

    public void setChordType(ChordType type) {
        this.chordType = type;
    }

    public void setScaleType(ScaleType type) {
        this.scaleType = type;
        // Bei Wechsel der Tonleiter muss die Scheibe neu gezeichnet werden (Löcher ändern sich)
        invalidate();
    }

    /**
     * Setzt die Transparenz der oberen Scheibe.
     * @param transparent true = fast transparent, false = normal (deckend)
     */
    public void setTopDiscTransparent(boolean transparent) {
        this.isTopDiscTransparent = transparent;
        // Passe die Farbe der oberen Scheibe an
        if (transparent) {
            topDiscPaint.setColor(Color.parseColor("#30E8E8E8")); // 19% Alpha (30 in Hex)
        } else {
            topDiscPaint.setColor(Color.parseColor("#E8E8E8")); // Normal, deckend
        }
        invalidate();
    }

    /**
     * Spielt die Tonleiter der aktuell oben stehenden Note (beim blauen Kreis).
     * Wird vom Button aufgerufen.
     */
    public void playCurrentScale() {
        // Berechne welche Note aktuell oben steht (Dur-Position = bei blauem Kreis)
        float normalizedRotation = -bottomDiscRotation % 360;
        if (normalizedRotation < 0) normalizedRotation += 360;
        int currentNote = Math.round(normalizedRotation / ANGLE_PER_POSITION) % 19;

        // Spiele Tonleiter dieser Note
        playScaleForNote(currentNote);
    }

    private void initPaints() {
        // Haupt-Kreis (schwarz)
        circlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        circlePaint.setColor(Color.BLACK);
        circlePaint.setStyle(Paint.Style.STROKE);
        circlePaint.setStrokeWidth(3f);

        // Löcher (weiß gefüllt)
        holePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        holePaint.setColor(Color.WHITE);
        holePaint.setStyle(Paint.Style.FILL);

        // Text für Noten
        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.BLACK);
        textPaint.setTextSize(40f);
        textPaint.setTextAlign(Paint.Align.CENTER);

        // Text für Kreuze/Bs (kleiner)
        textPaintSmall = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaintSmall.setColor(Color.BLACK);
        textPaintSmall.setTextSize(28f);
        textPaintSmall.setTextAlign(Paint.Align.CENTER);

        // Dur-Markierung (blau)
        majorPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        majorPaint.setColor(Color.BLUE);
        majorPaint.setStyle(Paint.Style.STROKE);
        majorPaint.setStrokeWidth(8f);

        // Moll-Markierung (rot)
        minorPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        minorPaint.setColor(Color.RED);
        minorPaint.setStyle(Paint.Style.STROKE);
        minorPaint.setStrokeWidth(8f);

        // Indikator-Rechteck
        rectPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        rectPaint.setColor(Color.BLACK);
        rectPaint.setStyle(Paint.Style.STROKE);
        rectPaint.setStrokeWidth(3f);

        // Obere Scheibe (deckend, hellgrau)
        topDiscPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        topDiscPaint.setColor(Color.parseColor("#E8E8E8"));
        topDiscPaint.setStyle(Paint.Style.FILL);

        // Clear-Paint für echte transparente Löcher
        clearPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        clearPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        // Berechne Dimensionen basierend auf Bildschirmgröße
        centerX = w / 2f;
        centerY = h / 2f;

        // Verwende 96% der kleineren Dimension für den Radius (volle Breite nutzen)
        float minDimension = Math.min(w, h);
        outerRadius = minDimension * 0.48f;
        innerRadius = outerRadius * 0.05f;
        noteCircleRadius = outerRadius * 0.13f;
        notePositionRadius = outerRadius * 0.80f;
        indicatorPositionRadius = outerRadius * 0.55f;
        indicatorSize = outerRadius / 9f * 1.5f;

        // Passe Textgrößen an
        textPaint.setTextSize(outerRadius * 0.12f);
        textPaintSmall.setTextSize(outerRadius * 0.09f);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (outerRadius == 0) return; // Noch nicht initialisiert

        // Hintergrund
        canvas.drawColor(Color.parseColor("#F5F5F5"));

        // Zeichne untere (drehbare) Scheibe
        drawBottomDisc(canvas);

        // Zeichne obere (feste) Scheibe mit Löchern
        drawTopDisc(canvas);

        // Zeichne opaken Rahmen um die Scheiben, um herausragende Texte zu verdecken
        drawOpaqueFrame(canvas);

        // Zeichne zentralen Play-Button über allem
        drawCentralPlayButton(canvas);

        // Zeichne Drehpfeile als Hinweis
        drawRotationArrows(canvas);
    }

    /**
     * Zeichnet zwei gebogene Doppelpfeile als Hinweis zum Drehen der Scheibe
     */
    private void drawRotationArrows(Canvas canvas) {
        // Paint für die Pfeile (halbtransparent grau)
        Paint arrowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        arrowPaint.setColor(Color.argb(180, 80, 80, 80));
        arrowPaint.setStyle(Paint.Style.STROKE);
        arrowPaint.setStrokeWidth(6f);
        arrowPaint.setStrokeCap(Paint.Cap.ROUND);

        // Radius für die Pfeile (zwischen innerem Button und Noten)
        float arrowRadius = (innerRadius * 4f + notePositionRadius) / 2f;

        // LINKER Bogen: Von 150° bis 210° (linke Seite)
        // In Android: 0° = rechts, 90° = unten, 180° = links, 270° = oben
        canvas.save();
        canvas.translate(centerX, centerY);
        android.graphics.Path leftArrow = new android.graphics.Path();
        leftArrow.addArc(-arrowRadius, -arrowRadius, arrowRadius, arrowRadius, 150, 60);
        canvas.drawPath(leftArrow, arrowPaint);
        canvas.restore();

        // Linke Pfeilspitze bei 150° (oberes Ende, zeigt nach unten/cw)
        canvas.save();
        canvas.translate(centerX, centerY);
        canvas.rotate(150);
        canvas.translate(arrowRadius, 0);
        canvas.rotate(0);  // 90° - 90° = 0°
        drawArrowHead(canvas, arrowPaint);
        canvas.restore();

        // Linke Pfeilspitze bei 210° (unteres Ende, zeigt nach oben/ccw)
        canvas.save();
        canvas.translate(centerX, centerY);
        canvas.rotate(210);
        canvas.translate(arrowRadius, 0);
        canvas.rotate(-180);  // -90° - 90° = -180°
        drawArrowHead(canvas, arrowPaint);
        canvas.restore();

        // RECHTER Bogen: Von -30° bis +30° (rechte Seite)
        canvas.save();
        canvas.translate(centerX, centerY);
        android.graphics.Path rightArrow = new android.graphics.Path();
        rightArrow.addArc(-arrowRadius, -arrowRadius, arrowRadius, arrowRadius, -30, 60);
        canvas.drawPath(rightArrow, arrowPaint);
        canvas.restore();

        // Rechte Pfeilspitze bei -30° (oberes Ende, zeigt nach unten/cw)
        canvas.save();
        canvas.translate(centerX, centerY);
        canvas.rotate(-30);
        canvas.translate(arrowRadius, 0);
        canvas.rotate(0);  // 90° - 90° = 0°
        drawArrowHead(canvas, arrowPaint);
        canvas.restore();

        // Rechte Pfeilspitze bei 30° (unteres Ende, zeigt nach oben/ccw)
        canvas.save();
        canvas.translate(centerX, centerY);
        canvas.rotate(30);
        canvas.translate(arrowRadius, 0);
        canvas.rotate(-180);  // -90° - 90° = -180°
        drawArrowHead(canvas, arrowPaint);
        canvas.restore();
    }

    /**
     * Zeichnet eine Pfeilspitze (doppelt so groß für bessere Sichtbarkeit)
     */
    private void drawArrowHead(Canvas canvas, Paint paint) {
        android.graphics.Path arrowHead = new android.graphics.Path();
        arrowHead.moveTo(0, -24);    // Spitze (doppelt so lang: 24 statt 12)
        arrowHead.lineTo(-16, 16);   // Links (doppelt so breit: 16 statt 8)
        arrowHead.lineTo(16, 16);    // Rechts (doppelt so breit: 16 statt 8)
        arrowHead.close();

        Paint fillPaint = new Paint(paint);
        fillPaint.setStyle(Paint.Style.FILL);
        fillPaint.setColor(Color.argb(180, 80, 80, 80)); // Gleiche Farbe wie Linie
        canvas.drawPath(arrowHead, fillPaint);
    }

    /**
     * Zeichnet den zentralen runden Play-Button in der Mitte der Scheibe
     */
    private void drawCentralPlayButton(Canvas canvas) {
        canvas.save();
        canvas.translate(centerX, centerY);

        // Button-Radius (größer als innerRadius)
        float buttonRadius = innerRadius * 4f;

        // Button-Hintergrund (grün)
        Paint buttonPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        buttonPaint.setColor(Color.parseColor("#4CAF50")); // Grün
        buttonPaint.setStyle(Paint.Style.FILL);
        canvas.drawCircle(0, 0, buttonRadius, buttonPaint);

        // Button-Umriss (dunkel)
        Paint buttonStrokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        buttonStrokePaint.setColor(Color.BLACK);
        buttonStrokePaint.setStyle(Paint.Style.STROKE);
        buttonStrokePaint.setStrokeWidth(4f);
        canvas.drawCircle(0, 0, buttonRadius, buttonStrokePaint);

        // Play-Symbol (▶) - großer Text
        Paint playTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        playTextPaint.setColor(Color.WHITE);
        playTextPaint.setTextSize(buttonRadius * 1.2f);
        playTextPaint.setTextAlign(Paint.Align.CENTER);
        playTextPaint.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);

        // Zeichne das Play-Symbol leicht nach rechts versetzt (wegen Dreieck-Form)
        canvas.drawText("▶", buttonRadius * 0.1f, playTextPaint.getTextSize() * 0.35f, playTextPaint);

        canvas.restore();
    }

    /**
     * Zeichnet die untere drehbare Scheibe mit allen 19 Notenpositionen
     */
    private void drawBottomDisc(Canvas canvas) {
        canvas.save();
        canvas.translate(centerX, centerY);
        canvas.rotate(bottomDiscRotation);

        // Äußerer Kreis
        canvas.drawCircle(0, 0, outerRadius, circlePaint);

        // Innerer kleiner Kreis (Zentrum)
        canvas.drawCircle(0, 0, innerRadius, circlePaint);

        // Zeichne alle 19 Noten-Positionen
        for (int i = 0; i < 19; i++) {
            float angle = 360f / 19f * i;
            canvas.save();
            canvas.rotate(angle);
            canvas.translate(0, -notePositionRadius);

            // Kreis für die Note
            canvas.drawCircle(0, 0, noteCircleRadius, holePaint);
            canvas.drawCircle(0, 0, noteCircleRadius, circlePaint);

            // Visuelle Hervorhebung für aktuell gespielte Note (Tonleiter-Modus)
            if (i == highlightedNoteIndex) {
                // Gelber Hintergrund (gefüllt)
                Paint highlightFillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
                highlightFillPaint.setColor(Color.YELLOW);
                highlightFillPaint.setStyle(Paint.Style.FILL);
                highlightFillPaint.setAlpha(180); // Halbtransparent für bessere Sichtbarkeit
                canvas.drawCircle(0, 0, noteCircleRadius * 1.4f, highlightFillPaint);

                // Gelber Ring (Umriss)
                Paint highlightStrokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
                highlightStrokePaint.setColor(Color.YELLOW);
                highlightStrokePaint.setStyle(Paint.Style.STROKE);
                highlightStrokePaint.setStrokeWidth(10f); // Dicker für bessere Sichtbarkeit
                canvas.drawCircle(0, 0, noteCircleRadius * 1.5f, highlightStrokePaint);
            }

            // Text - rotiert zurück, damit er immer aufrecht steht
            canvas.save();
            canvas.rotate(-angle - bottomDiscRotation);

            // Notenname
            canvas.drawText(POSITIONS[i].note, 0, textPaint.getTextSize() * 0.3f, textPaint);

            canvas.restore();
            canvas.restore();
        }

        canvas.restore();

        // Zeichne Vorzeichen beim Indikator (oben, fest, nicht rotiert)
        canvas.save();
        canvas.translate(centerX, centerY);

        // Berechne, welche Note beim Indikator steht (oben bei 0°)
        // Bei bottomDiscRotation = -X° ist Note X oben (wegen canvas.rotate im Uhrzeigersinn)
        float normalizedRotation = -bottomDiscRotation % 360;
        if (normalizedRotation < 0) normalizedRotation += 360;

        // Finde die Note, die am nächsten bei 0° (oben) steht
        float anglePerPosition = 360f / 19f;
        int indicatorNoteIndex = Math.round(normalizedRotation / anglePerPosition) % 19;

        // Zeichne Vorzeichen der aktuellen Note beim Indikator
        if (!POSITIONS[indicatorNoteIndex].sharpFlat.isEmpty()) {
            canvas.drawText(POSITIONS[indicatorNoteIndex].sharpFlat, 0,
                -indicatorPositionRadius + indicatorSize * 0.1f,
                textPaintSmall);
        }

        canvas.restore();
    }

    /**
     * Bestimmt ob an Position i ein Loch gezeichnet werden soll,
     * basierend auf dem gewählten Tonleiter-Typ.
     * Die Löcher sind FEST wie die obere Scheibe, unabhängig von der Rotation!
     */
    private boolean shouldShowHole(int position) {
        // Bei Dur: Original-Löcher aus POSITIONS array
        if (scaleType == ScaleType.MAJOR) {
            return POSITIONS[position].hole;
        }

        // Bei Moll: Feste Löcher für Moll-Tonleitern
        // Diese Positionen entsprechen den Noten einer Moll-Tonleiter ausgehend von Position 0 (C)
        // Wenn C-Moll bei C oben steht, zeigen die Löcher: C, D, Eb, F, G, Ab, Bb

        if (scaleType == ScaleType.NATURAL_MINOR) {
            // Natürliches Moll ausgehend von C (Position 0):
            // C(0), D(3), Eb(5), F(8), G(11), Ab(13), Bb(16)
            // 7 Noten + C oben = 8 Löcher
            int[] naturalMinorHoles = {0, 3, 5, 8, 11, 13, 16};
            for (int hole : naturalMinorHoles) {
                if (position == hole) return true;
            }
            return false;

        } else { // HARMONIC_MINOR
            // Harmonisches Moll ausgehend von C (Position 0):
            // C(0), D(3), Eb(5), F(8), G(11), Ab(13), H(17)
            // 7 Noten + C oben = 8 Löcher (H statt Bb!)
            int[] harmonicMinorHoles = {0, 3, 5, 8, 11, 13, 17};
            for (int hole : harmonicMinorHoles) {
                if (position == hole) return true;
            }
            return false;
        }
    }

    /**
     * Zeichnet die obere feste Scheibe mit Löchern und Indikator
     */
    private void drawTopDisc(Canvas canvas) {
        // Erstelle ein Bitmap für die obere Scheibe mit echter Transparenz
        Bitmap topDiscBitmap = Bitmap.createBitmap(canvas.getWidth(), canvas.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas topDiscCanvas = new Canvas(topDiscBitmap);

        topDiscCanvas.save();
        topDiscCanvas.translate(centerX, centerY);

        // Zeichne die gefüllte obere Scheibe
        topDiscCanvas.drawCircle(0, 0, outerRadius, topDiscPaint);

        // Schneide echte Löcher aus (hole = true)
        for (int i = 0; i < 19; i++) {
            if (shouldShowHole(i)) {
                float angle = 360f / 19f * i;
                topDiscCanvas.save();
                topDiscCanvas.rotate(angle);
                topDiscCanvas.translate(0, -notePositionRadius);

                // Schneide ein echtes transparentes Loch aus
                topDiscCanvas.drawCircle(0, 0, noteCircleRadius, clearPaint);

                topDiscCanvas.restore();
            }
        }

        // Schneide ein echtes transparentes Rechteck für den Indikator aus
        float rectLeft = -indicatorSize / 2;
        float rectTop = -indicatorPositionRadius - indicatorSize / 2;
        RectF rect = new RectF(rectLeft, rectTop,
            rectLeft + indicatorSize, rectTop + indicatorSize);
        topDiscCanvas.drawRect(rect, clearPaint);

        topDiscCanvas.restore();

        // Zeichne das Bitmap auf den Haupt-Canvas
        canvas.drawBitmap(topDiscBitmap, 0, 0, null);
        topDiscBitmap.recycle();

        // Zeichne die Umrisse über alles drüber
        canvas.save();
        canvas.translate(centerX, centerY);

        // Äußerer Kreis der oberen Scheibe
        canvas.drawCircle(0, 0, outerRadius, circlePaint);

        // Innerer kleiner Kreis (Zentrum)
        canvas.drawCircle(0, 0, innerRadius, circlePaint);

        // Zeichne Umrisse und Markierungen für die Löcher
        for (int i = 0; i < 19; i++) {
            if (shouldShowHole(i)) {
                float angle = 360f / 19f * i;
                canvas.save();
                canvas.rotate(angle);
                canvas.translate(0, -notePositionRadius);

                // Spezielle Markierungen für Dur und Moll
                if (i == MAJOR_POS) {
                    canvas.drawCircle(0, 0, noteCircleRadius * 1.1f, majorPaint);
                }
                if (i == MINOR_POS) {
                    canvas.drawCircle(0, 0, noteCircleRadius * 1.1f, minorPaint);
                }

                // Umriss des Lochs
                canvas.drawCircle(0, 0, noteCircleRadius, circlePaint);

                canvas.restore();
            }
        }

        // Umriss des Indikator-Rechtecks
        float rectLeft2 = -indicatorSize / 2;
        float rectTop2 = -indicatorPositionRadius - indicatorSize / 2;
        RectF rect2 = new RectF(rectLeft2, rectTop2,
            rectLeft2 + indicatorSize, rectTop2 + indicatorSize);
        canvas.drawRect(rect2, rectPaint);

        canvas.restore();
    }

    /**
     * Zeichnet einen opaken Rahmen um die Scheiben, um herausragende Texte zu verdecken
     */
    private void drawOpaqueFrame(Canvas canvas) {
        // Erstelle ein Bitmap für den Rahmen mit echter Transparenz
        Bitmap frameBitmap = Bitmap.createBitmap(canvas.getWidth(), canvas.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas frameCanvas = new Canvas(frameBitmap);

        // Zeichne den gesamten Bereich mit der Hintergrundfarbe
        frameCanvas.drawColor(Color.parseColor("#F5F5F5"));

        // Schneide einen Kreis in der Größe der Scheiben aus
        frameCanvas.save();
        frameCanvas.translate(centerX, centerY);
        frameCanvas.drawCircle(0, 0, outerRadius, clearPaint);
        frameCanvas.restore();

        // Zeichne das Bitmap auf den Haupt-Canvas
        canvas.drawBitmap(frameBitmap, 0, 0, null);
        frameBitmap.recycle();
    }

    /**
     * Berechnet die nächste Snap-Position und animiert dorthin
     */
    private void snapToNearestPosition() {
        // Stoppe laufende Animation
        if (snapAnimator != null && snapAnimator.isRunning()) {
            snapAnimator.cancel();
        }

        // Bei bottomDiscRotation = -X° ist Note X oben
        // Invertiere für die Berechnung
        float normalizedRotation = -bottomDiscRotation % 360;
        if (normalizedRotation < 0) normalizedRotation += 360;

        // Finde nächste Snap-Position
        float currentPositionFloat = normalizedRotation / ANGLE_PER_POSITION;
        int nearestPosition = Math.round(currentPositionFloat);

        // Zielrotation ist negativ (weil canvas.rotate im Uhrzeigersinn dreht)
        float targetRotation = -(nearestPosition * ANGLE_PER_POSITION);

        // Normalisiere aktuelle Rotation für Vergleich
        float normalizedCurrent = bottomDiscRotation % 360;
        if (normalizedCurrent < 0) normalizedCurrent += 360;

        float normalizedTarget = targetRotation % 360;
        if (normalizedTarget < 0) normalizedTarget += 360;

        // Berechne kürzesten Weg zum Ziel
        float deltaRotation = normalizedTarget - normalizedCurrent;
        if (deltaRotation > 180) deltaRotation -= 360;
        if (deltaRotation < -180) deltaRotation += 360;

        // Wenn bereits sehr nah, nicht animieren
        if (Math.abs(deltaRotation) < 0.5f) {
            bottomDiscRotation = targetRotation;
            invalidate();
            return;
        }

        // Erstelle Animation
        final float startRotation = bottomDiscRotation;
        final float endRotation = bottomDiscRotation + deltaRotation;

        snapAnimator = ValueAnimator.ofFloat(0f, 1f);
        snapAnimator.setDuration(SNAP_DURATION_MS);
        snapAnimator.setInterpolator(new DecelerateInterpolator());

        snapAnimator.addUpdateListener(animation -> {
            float progress = (float) animation.getAnimatedValue();
            bottomDiscRotation = startRotation + (endRotation - startRotation) * progress;
            invalidate();
        });

        snapAnimator.start();
    }

    /**
     * Findet die Note, die an der gegebenen Position getippt wurde
     * @return Index der Note oder -1, wenn keine Note getroffen
     */
    private int findTappedNote(float touchX, float touchY) {
        // Transformiere Touch-Position ins Scheiben-Koordinatensystem
        float x = touchX - centerX;
        float y = touchY - centerY;

        // Berechne Winkel und Distanz vom Zentrum
        // atan2 gibt -90° für oben, 0° für rechts, +90° für unten
        // Canvas rotate gibt 0° für oben, 90° für rechts, 180° für unten
        // Daher: +90° um von atan2 zu Canvas-Koordinaten zu konvertieren
        float touchAngleRaw = (float) Math.toDegrees(Math.atan2(y, x));
        float touchAngle = touchAngleRaw + 90f; // Konvertiere: -90°(oben) → 0°(oben)
        float distance = (float) Math.sqrt(x * x + y * y);

        // Prüfe, ob Touch im Bereich der Noten ist
        float minDistance = notePositionRadius - noteCircleRadius * 1.2f;
        float maxDistance = notePositionRadius + noteCircleRadius * 1.2f;

        if (distance < minDistance || distance > maxDistance) {
            return -1; // Nicht im Noten-Bereich
        }

        // Normalisiere Touch-Winkel (0-360)
        while (touchAngle < 0) touchAngle += 360;
        while (touchAngle >= 360) touchAngle -= 360;

        // Finde die Note, die am nächsten zum Touch-Winkel ist
        // Jede Note i ist bei (i * ANGLE_PER_POSITION + bottomDiscRotation) Grad
        float minAngleDiff = 360;
        int closestNoteIndex = -1;

        for (int i = 0; i < 19; i++) {
            // Berechne wo Note i aktuell auf dem Bildschirm ist (in Canvas-Koordinaten)
            float noteScreenAngle = (i * ANGLE_PER_POSITION + bottomDiscRotation);
            while (noteScreenAngle < 0) noteScreenAngle += 360;
            while (noteScreenAngle >= 360) noteScreenAngle -= 360;

            // Berechne Winkel-Differenz (kürzester Weg)
            float angleDiff = Math.abs(touchAngle - noteScreenAngle);
            if (angleDiff > 180) angleDiff = 360 - angleDiff;

            // Merke die nächste Note
            if (angleDiff < minAngleDiff) {
                minAngleDiff = angleDiff;
                closestNoteIndex = i;
            }
        }

        // Nur akzeptieren, wenn innerhalb von ±15° der Note
        if (minAngleDiff < 15) {
            return closestNoteIndex;
        }

        return -1;
    }

    /**
     * Animiert die Scheibe, sodass die gegebene Note oben beim Indikator steht
     */
    private void rotateToNote(int noteIndex) {
        // Stoppe laufende Animation
        if (snapAnimator != null && snapAnimator.isRunning()) {
            snapAnimator.cancel();
        }

        // Um Note X nach oben zu bringen:
        // - Note X ist bei Position X * ANGLE_PER_POSITION
        // - canvas.rotate dreht im Uhrzeigersinn, Noten wandern gegen Uhrzeigersinn
        // - Um Note X von ihrer Position nach 0° (oben) zu bringen:
        //   Scheibe muss um -(X * ANGLE_PER_POSITION) drehen
        float targetRotation = -(noteIndex * ANGLE_PER_POSITION);

        // Normalisiere aktuelle Rotation
        float normalizedCurrentRotation = bottomDiscRotation % 360;
        if (normalizedCurrentRotation < 0) normalizedCurrentRotation += 360;

        // Normalisiere Zielrotation
        float normalizedTargetRotation = targetRotation % 360;
        if (normalizedTargetRotation < 0) normalizedTargetRotation += 360;

        // Berechne Differenz zur Zielposition
        float deltaRotation = normalizedTargetRotation - normalizedCurrentRotation;

        // Korrigiere für kürzesten Weg (niemals mehr als 180° drehen)
        while (deltaRotation > 180) deltaRotation -= 360;
        while (deltaRotation < -180) deltaRotation += 360;


        // Erstelle Animation
        final float startRotation = bottomDiscRotation;
        final float endRotation = bottomDiscRotation + deltaRotation;

        snapAnimator = ValueAnimator.ofFloat(0f, 1f);
        snapAnimator.setDuration(SNAP_DURATION_MS);
        snapAnimator.setInterpolator(new DecelerateInterpolator());

        snapAnimator.addUpdateListener(animation -> {
            float progress = (float) animation.getAnimatedValue();
            bottomDiscRotation = startRotation + (endRotation - startRotation) * progress;
            invalidate();
        });

        snapAnimator.start();
    }

    @Override
    public boolean performClick() {
        super.performClick();
        return true;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX() - centerX;
        float y = event.getY() - centerY;

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                // Prüfe zuerst, ob der zentrale Play-Button getroffen wurde
                float distanceFromCenter = (float) Math.sqrt(x * x + y * y);
                float buttonRadius = innerRadius * 4f;

                if (distanceFromCenter < buttonRadius) {
                    // Play-Button wurde getroffen - spiele Tonleiter
                    performClick();
                    playCurrentScale();
                    return true;
                }

                // Prüfe, ob Touch im drehbaren Bereich ist (außerhalb des Buttons, aber innerhalb der Scheibe)
                if (distanceFromCenter < outerRadius && distanceFromCenter > buttonRadius) {
                    // Stoppe laufende Snap-Animation
                    if (snapAnimator != null && snapAnimator.isRunning()) {
                        snapAnimator.cancel();
                    }
                    isDragging = true;
                    touchDownX = event.getX();
                    touchDownY = event.getY();
                    // Konvertiere atan2 zu Canvas-Koordinaten: +90°
                    lastTouchAngle = (float) Math.toDegrees(Math.atan2(y, x)) + 90f;
                    return true;
                }
                break;

            case MotionEvent.ACTION_MOVE:
                if (isDragging) {
                    // Prüfe, ob sich der Finger weit genug bewegt hat für einen Drag
                    float moveDistance = (float) Math.sqrt(
                        Math.pow(event.getX() - touchDownX, 2) +
                        Math.pow(event.getY() - touchDownY, 2)
                    );

                    if (moveDistance > TAP_THRESHOLD) {
                        // Es ist ein Drag, drehe die Scheibe
                        // Konvertiere atan2 zu Canvas-Koordinaten: +90°
                        float currentAngle = (float) Math.toDegrees(Math.atan2(y, x)) + 90f;
                        float deltaAngle = currentAngle - lastTouchAngle;

                        // Normalisiere den Winkel
                        if (deltaAngle > 180) deltaAngle -= 360;
                        if (deltaAngle < -180) deltaAngle += 360;

                        bottomDiscRotation += deltaAngle;
                        lastTouchAngle = currentAngle;

                        invalidate(); // Neuzeichnen
                    }
                    return true;
                }
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (isDragging) {
                    // Prüfe, ob es ein Tap oder Drag war
                    float moveDistance = (float) Math.sqrt(
                        Math.pow(event.getX() - touchDownX, 2) +
                        Math.pow(event.getY() - touchDownY, 2)
                    );

                    if (moveDistance <= TAP_THRESHOLD) {
                        // Es war ein Tap - prüfe, ob eine Note getroffen wurde
                        int tappedNote = findTappedNote(event.getX(), event.getY());
                        if (tappedNote >= 0) {
                            performClick();
                            handleNoteTap(tappedNote);
                        }
                    } else {
                        // Es war ein Drag - Snap zur nächsten Position
                        performClick();
                        snapToNearestPosition();
                    }
                }
                isDragging = false;
                break;
        }

        return super.onTouchEvent(event);
    }

    /**
     * Behandelt den Tap auf eine Note basierend auf dem aktuellen Modus
     */
    private void handleNoteTap(int tappedNoteIndex) {
        switch (tapMode) {
            case PLAY_CHORD:
                playChordForNote(tappedNoteIndex);
                break;
            case ROTATE_TO_DUR:
                rotateToNote(tappedNoteIndex);
                break;
            case ROTATE_TO_MOLL:
                // Der getippte Buchstabe soll zum roten Loch (Position 14 = A) rotieren
                // Wenn tappedNoteIndex zur Position MINOR_POS (14) soll:
                // Wir müssen Note X nach oben bringen, wobei X = (tappedNoteIndex - MINOR_POS)
                // Beispiel: Tippe auf D (Position 3), soll zu Position 14
                //           Also muss Note bei Position (3-14+19)%19 = 8 (F) nach oben
                //           Dann steht D bei der roten Position 14

                // Berechne welche Note nach oben muss, damit getippte Note bei MINOR_POS steht
                int noteToRotateUp = (tappedNoteIndex - MINOR_POS + 19) % 19;
                rotateToNote(noteToRotateUp);
                break;
        }
    }

    /**
     * Spielt den Akkord für die getippte Note.
     * Bestimmt automatisch, ob Dur oder Moll gespielt werden soll.
     */
    private void playChordForNote(int noteIndex) {
        if (chordPlayer == null) {
            return;
        }

        // Bestimme Akkord-Typ basierend auf Einstellung
        boolean isMajor;

        switch (chordType) {
            case ALL_MAJOR:
                isMajor = true; // Immer Dur
                break;

            case ALL_MINOR:
                isMajor = false; // Immer Moll
                break;

            case HARMONIC:
            default:
                // Harmonische Logik: Berechne basierend auf Tonika
                float normalizedRotation = -bottomDiscRotation % 360;
                if (normalizedRotation < 0) normalizedRotation += 360;
                int tonika = Math.round(normalizedRotation / ANGLE_PER_POSITION) % 19;
                isMajor = shouldPlayMajorChord(noteIndex, tonika);
                break;
        }

        // Spiele den Akkord
        chordPlayer.playChord(noteIndex, isMajor);
    }

    /**
     * Spielt die Tonleiter für die getippte Note mit visueller Hervorhebung.
     */
    private void playScaleForNote(int noteIndex) {
        if (chordPlayer == null) {
            return;
        }

        // Konvertiere ScaleType zu ChordPlayer.ScaleType
        ChordPlayer.ScaleType playerScaleType;
        switch (scaleType) {
            case MAJOR:
                playerScaleType = ChordPlayer.ScaleType.MAJOR;
                break;
            case NATURAL_MINOR:
                playerScaleType = ChordPlayer.ScaleType.NATURAL_MINOR;
                break;
            case HARMONIC_MINOR:
                playerScaleType = ChordPlayer.ScaleType.HARMONIC_MINOR;
                break;
            default:
                playerScaleType = ChordPlayer.ScaleType.MAJOR;
                break;
        }

        // Spiele Tonleiter mit Callback für visuelle Hervorhebung
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

    /**
     * Berechnet den Note-Index aus Grundton und Intervall in Halbtönen.
     * Vereinfachte Version: Verwendet das 19-Noten-Array direkt.
     */
    private int calculateNoteIndexFromInterval(int rootIndex, int intervalInHalftones) {
        // Mapping von Halbtönen zu Position-Offsets im 19er-Array
        // Dies ist eine Annäherung für die visuelle Hervorhebung
        int[] halftoneToPositionOffset = {
            0,  // 0 Halbtöne = gleiche Note
            2,  // 1 Halbton = +2 Positionen (ungefähr)
            3,  // 2 Halbtöne = D
            5,  // 3 Halbtöne = Eb
            6,  // 4 Halbtöne = E
            8,  // 5 Halbtöne = F
            9,  // 6 Halbtöne = F#
            11, // 7 Halbtöne = G
            13, // 8 Halbtöne = Ab
            14, // 9 Halbtöne = A
            16, // 10 Halbtöne = B
            17, // 11 Halbtöne = H
            0   // 12 Halbtöne = Oktave (wieder bei Grundton)
        };

        if (intervalInHalftones < 0 || intervalInHalftones >= halftoneToPositionOffset.length) {
            return rootIndex;
        }

        int positionOffset = halftoneToPositionOffset[intervalInHalftones];
        return (rootIndex + positionOffset) % 19;
    }

    /**
     * Bestimmt, ob ein Dur- oder Moll-Akkord gespielt werden soll.
     * Neue Regel-Definition basierend auf absoluten Positionen im Array:
     *
     * Bei C oben (Tonika = 0):
     * - Tonika: C (0) = Dur
     * - Subdominante: F (8) = Dur
     * - Dominante: G (11) = Dur
     * - Dominante des Parallel-Molls: E (6) = Dur
     * - Parallel-Moll: A (14) = Moll
     * - Subdominante des Parallel-Molls: D (3) = Moll
     * - Alle anderen: Dur (inkl. H bei Position 17)
     */
    private boolean shouldPlayMajorChord(int noteIndex, int tonika) {
        // Berechne die Moll-Positionen relativ zur Tonika
        // Die Abstände sind fest basierend auf dem Array:
        // C(0) → D(3) = +3, C(0) → E(6) = +6, C(0) → F(8) = +8, C(0) → G(11) = +11, C(0) → A(14) = +14

        // Parallel-Moll: +14 Positionen von Tonika (A wenn C = Tonika)
        int parallelMoll = (tonika + 14) % 19;

        // Subdominante des Parallel-Molls: +3 Positionen von Tonika (D wenn C = Tonika)
        int subdominanteMoll = (tonika + 3) % 19;

        // Prüfe ob Moll-Akkord
        if (noteIndex == parallelMoll || noteIndex == subdominanteMoll) {
            return false; // Moll
        }

        // Alle anderen: Dur
        return true;
    }

    /**
     * Datenklasse für musikalische Positionen
     */
    private static class MusicalPosition {
        String note;
        boolean hole;
        String sharpFlat;

        MusicalPosition(String note, boolean hole, String sharpFlat) {
            this.note = note;
            this.hole = hole;
            this.sharpFlat = sharpFlat;
        }
    }
}

