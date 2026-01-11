package com.chorddisc;

import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.widget.FrameLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private ChordDiscView chordDiscView;
    private ChordPlayer chordPlayer;
    private boolean isEnglish = false; // Wird in onCreate basierend auf System-Sprache gesetzt

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Erkenne System-Sprache und setze Default
        String systemLang = Locale.getDefault().getLanguage();
        isEnglish = !systemLang.equals("de"); // Nur Deutsch = false, alles andere = true

        // Setze Locale OHNE UI-Update (Views existieren noch nicht)
        Locale locale = new Locale(isEnglish ? "en" : "de");
        Locale.setDefault(locale);
        Resources resources = getResources();
        Configuration config = new Configuration(resources.getConfiguration());
        config.setLocale(locale);
        resources.updateConfiguration(config, resources.getDisplayMetrics());

        setContentView(R.layout.activity_main);

        // Initialisiere ChordPlayer
        chordPlayer = new ChordPlayer(this);

        // Erstelle ChordDiscView
        chordDiscView = new ChordDiscView(this, chordPlayer);

        // Setze initiale Notation basierend auf Sprache
        chordDiscView.setNotationLanguage(!isEnglish); // true für Deutsch, false für Englisch

        // Füge ChordDiscView zum Container hinzu
        FrameLayout container = findViewById(R.id.chordDiscContainer);
        container.addView(chordDiscView);

        // Setup RadioGroup für Tap-Modi
        RadioGroup tapModeRadioGroup = findViewById(R.id.tapModeRadioGroup);
        tapModeRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.radioPlayChord) {
                chordDiscView.setTapMode(ChordDiscView.TapMode.PLAY_CHORD);
            } else if (checkedId == R.id.radioRotateToDur) {
                chordDiscView.setTapMode(ChordDiscView.TapMode.ROTATE_TO_DUR);
            } else if (checkedId == R.id.radioRotateToMoll) {
                chordDiscView.setTapMode(ChordDiscView.TapMode.ROTATE_TO_MOLL);
            }
        });


        // Setup RadioGroup für Akkord-Typ
        RadioGroup chordTypeRadioGroup = findViewById(R.id.chordTypeRadioGroup);
        chordTypeRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.radioAllMajor) {
                chordDiscView.setChordType(ChordDiscView.ChordType.ALL_MAJOR);
            } else if (checkedId == R.id.radioAllMinor) {
                chordDiscView.setChordType(ChordDiscView.ChordType.ALL_MINOR);
            } else if (checkedId == R.id.radioHarmonic) {
                chordDiscView.setChordType(ChordDiscView.ChordType.HARMONIC);
            }
        });

        // Setup RadioGroup für Tonleiter-Typ
        RadioGroup scaleTypeRadioGroup = findViewById(R.id.scaleTypeRadioGroup);
        RadioButton harmonicButton = findViewById(R.id.radioHarmonic);

        scaleTypeRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.radioMajorScale) {
                chordDiscView.setScaleType(ChordDiscView.ScaleType.MAJOR);
                // Bei Dur: Harmonie verfügbar und automatisch aktivieren
                harmonicButton.setEnabled(true);
                chordTypeRadioGroup.check(R.id.radioHarmonic);
            } else if (checkedId == R.id.radioNaturalMinor) {
                chordDiscView.setScaleType(ChordDiscView.ScaleType.NATURAL_MINOR);
                // Bei Moll: Harmonie deaktivieren
                harmonicButton.setEnabled(false);
                if (chordTypeRadioGroup.getCheckedRadioButtonId() == R.id.radioHarmonic) {
                    chordTypeRadioGroup.check(R.id.radioAllMinor);
                }
            } else if (checkedId == R.id.radioHarmonicMinor) {
                chordDiscView.setScaleType(ChordDiscView.ScaleType.HARMONIC_MINOR);
                // Bei Moll: Harmonie deaktivieren
                harmonicButton.setEnabled(false);
                if (chordTypeRadioGroup.getCheckedRadioButtonId() == R.id.radioHarmonic) {
                    chordTypeRadioGroup.check(R.id.radioAllMinor);
                }
            }
        });

        // Setze Standard-Modi
        chordDiscView.setTapMode(ChordDiscView.TapMode.PLAY_CHORD);
        chordDiscView.setChordType(ChordDiscView.ChordType.HARMONIC);
        chordDiscView.setScaleType(ChordDiscView.ScaleType.MAJOR);

        // Setup Toggle für Transparenz
        SwitchCompat toggleTransparency = findViewById(R.id.toggleTransparency);
        toggleTransparency.setOnCheckedChangeListener((buttonView, isChecked) ->
            chordDiscView.setTopDiscTransparent(isChecked)
        );

        // Setup Toggle für Sprache
        SwitchCompat toggleLanguage = findViewById(R.id.toggleLanguage);
        toggleLanguage.setChecked(isEnglish); // Setze initialen Zustand
        toggleLanguage.setOnCheckedChangeListener((buttonView, isChecked) -> {
            isEnglish = isChecked;
            setLocale(isChecked ? "en" : "de");
        });
    }

    /**
     * Setzt die Sprache der App und aktualisiert die UI
     */
    private void setLocale(String languageCode) {
        Locale locale = new Locale(languageCode);
        Locale.setDefault(locale);

        Resources resources = getResources();
        Configuration config = new Configuration(resources.getConfiguration());
        config.setLocale(locale);

        // Aktualisiere die Konfiguration
        resources.updateConfiguration(config, resources.getDisplayMetrics());

        // Aktualisiere Notennotation in ChordDiscView (nur wenn bereits initialisiert)
        if (chordDiscView != null) {
            chordDiscView.setNotationLanguage(languageCode.equals("de"));
        }

        // Aktualisiere alle UI-Texte (nur wenn Views existieren)
        // Prüfe ob findViewById etwas zurückgibt
        if (findViewById(R.id.titleText) != null) {
            updateUITexts();
        }
    }

    /**
     * Aktualisiert alle UI-Texte nach Sprachwechsel
     */
    private void updateUITexts() {
        // Titel
        TextView titleText = findViewById(R.id.titleText);
        titleText.setText(R.string.title);

        // Labels
        TextView scaleTypeLabel = findViewById(R.id.scaleTypeLabel);
        scaleTypeLabel.setText(R.string.scale_type_label);

        TextView tapModeLabel = findViewById(R.id.tapModeLabel);
        tapModeLabel.setText(R.string.tap_mode_label);

        TextView chordTypeLabel = findViewById(R.id.chordTypeLabel);
        chordTypeLabel.setText(R.string.chord_type_label);

        TextView transparencyLabel = findViewById(R.id.transparencyLabel);
        transparencyLabel.setText(R.string.toggle_transparency);

        TextView languageLabel = findViewById(R.id.languageLabel);
        languageLabel.setText(R.string.toggle_language);

        // RadioButtons - Scale Type
        RadioButton radioMajorScale = findViewById(R.id.radioMajorScale);
        radioMajorScale.setText(R.string.scale_major);

        RadioButton radioNaturalMinor = findViewById(R.id.radioNaturalMinor);
        radioNaturalMinor.setText(R.string.scale_natural_minor);

        RadioButton radioHarmonicMinor = findViewById(R.id.radioHarmonicMinor);
        radioHarmonicMinor.setText(R.string.scale_harmonic_minor);

        // RadioButtons - Tap Mode
        RadioButton radioPlayChord = findViewById(R.id.radioPlayChord);
        radioPlayChord.setText(R.string.tap_play_chord);

        RadioButton radioRotateToDur = findViewById(R.id.radioRotateToDur);
        radioRotateToDur.setText(R.string.tap_rotate_to_dur);

        RadioButton radioRotateToMoll = findViewById(R.id.radioRotateToMoll);
        radioRotateToMoll.setText(R.string.tap_rotate_to_minor);

        // RadioButtons - Chord Type
        RadioButton radioHarmonic = findViewById(R.id.radioHarmonic);
        radioHarmonic.setText(R.string.chord_harmony);

        RadioButton radioAllMajor = findViewById(R.id.radioAllMajor);
        radioAllMajor.setText(R.string.chord_major);

        RadioButton radioAllMinor = findViewById(R.id.radioAllMinor);
        radioAllMinor.setText(R.string.chord_minor);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (chordPlayer != null) {
            chordPlayer.release();
        }
    }
}

