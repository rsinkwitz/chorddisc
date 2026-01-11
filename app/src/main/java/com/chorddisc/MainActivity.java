package com.chorddisc;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Erstelle und setze die Custom View
        ChordDiscView chordDiscView = new ChordDiscView(this);
        setContentView(chordDiscView);
    }
}

