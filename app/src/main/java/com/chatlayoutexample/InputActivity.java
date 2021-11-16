package com.chatlayoutexample;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;


import static android.R.id.message;

public class InputActivity extends AppCompatActivity {
    Button send;
    EditText txtNote;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.input_note);

        txtNote=(EditText) findViewById(R.id.UserInput);

        send = (Button) findViewById(R.id.btnConferma);
        send.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                if(!txtNote.getText().toString().trim().isEmpty()) {
                    //DUMMY
                }

            }
        });
    }
}
