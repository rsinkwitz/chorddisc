package com.chorddisc;

import android.os.Bundle;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private ChordDiscView chordDiscView;
    private ChordPlayer chordPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialisiere ChordPlayer
        chordPlayer = new ChordPlayer(this);

        // Erstelle ChordDiscView
        chordDiscView = new ChordDiscView(this, chordPlayer);

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

        // Setup Button für Tonleiter abspielen
        Button playScaleButton = findViewById(R.id.buttonPlayScale);
        playScaleButton.setOnClickListener(v -> chordDiscView.playCurrentScale());

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
                // Harmonie nur bei Dur verfügbar
                harmonicButton.setEnabled(true);
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
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (chordPlayer != null) {
            chordPlayer.release();
        }
    }
}

