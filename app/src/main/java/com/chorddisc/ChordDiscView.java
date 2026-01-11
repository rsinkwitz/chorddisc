package com.chorddisc;

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
        float normalizedRotation = bottomDiscRotation % 360;
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
                    isDragging = true;
                    lastTouchAngle = (float) Math.toDegrees(Math.atan2(y, x));
                    return true;
                }
                break;

            case MotionEvent.ACTION_MOVE:
                if (isDragging) {
                    float currentAngle = (float) Math.toDegrees(Math.atan2(y, x));
                    float deltaAngle = currentAngle - lastTouchAngle;

                    // Normalisiere den Winkel
                    if (deltaAngle > 180) deltaAngle -= 360;
                    if (deltaAngle < -180) deltaAngle += 360;

                    bottomDiscRotation += deltaAngle;
                    lastTouchAngle = currentAngle;

                    invalidate(); // Neuzeichnen
                    return true;
                }
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (isDragging) {
                    performClick();
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

