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

    public ChordDiscView(Context context) {
        super(context);
        initPaints();
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
            if (POSITIONS[i].hole) {
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
            if (POSITIONS[i].hole) {
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
                // Prüfe, ob Touch im drehbaren Bereich ist
                float distance = (float) Math.sqrt(x * x + y * y);
                if (distance < outerRadius && distance > innerRadius) {
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
                            // Note getroffen - rotiere zu dieser Position
                            performClick();
                            rotateToNote(tappedNote);
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

