package com.chatlayoutexample;

import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListAdapter;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;


import static android.R.id.message;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

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

                if(!txtNote.getText().toString().trim().isEmpty())
                {
                    writeToFile(txtNote.getText().toString());
                    finish();
                }

            }
        });
    }

    public void writeToFile(String strNota)
    {

        java.io.File fileConnectChat = new java.io.File(Environment.getExternalStorageDirectory() + java.io.File.separator + "Download" + "/CONVERSAZIONI");

        if(!fileConnectChat.exists())
        {
            fileConnectChat.mkdirs();
        }

        final java.io.File file = new java.io.File(fileConnectChat, "frasi.txt");

        try
        {
            if(!file.exists())
            {
                file.createNewFile();
            }

            FileOutputStream fOut = new FileOutputStream(file,true);
            OutputStreamWriter myOutWriter = new OutputStreamWriter(fOut);

            String pp = String.format("%s\n",strNota);
            myOutWriter.append(pp);

            myOutWriter.close();

            fOut.flush();
            fOut.close();
        }
        catch (IOException e)
        {
            Log.e("Exception", "File write failed: " + e.toString());
        }
    }
}
