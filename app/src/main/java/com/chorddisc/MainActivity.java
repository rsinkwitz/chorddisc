package com.chorddisc;

import android.os.Bundle;
import android.widget.FrameLayout;
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

        // Setup RadioGroup
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

        // Setze Standard-Modus
        chordDiscView.setTapMode(ChordDiscView.TapMode.PLAY_CHORD);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (chordPlayer != null) {
            chordPlayer.release();
        }
    }
}

