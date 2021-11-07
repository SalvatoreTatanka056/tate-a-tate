package com.chatlayoutexample;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.os.Bundle;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;


public class ListaUtenti extends AppCompatActivity {

    Handler handler;
    private ImageButton m_btnExit;
    CustomAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.list_utenti);

        ListView listView = (ListView) findViewById(R.id.listViewDemo);
        List<Contatto> list = new LinkedList<Contatto>();

        list.add(new Contatto("+", "Nuova Chat", "═══════════════════════════"));
        list.add(new Contatto("Giovanni", "Rossi", "1234567890"));
        list.add(new Contatto("Giuseppe", "Bianchi", "1234567890"));
        list.add(new Contatto("Leonardo", "Da Vinci", "1234567890"));
        list.add(new Contatto("Mario", "Rossi", "1234567890"));
        list.add(new Contatto("Aldo", "Rossi", "1234567890"));

        adapter = new CustomAdapter(this, R.layout.list_item_utenti, list);

        listView.setAdapter(adapter);


        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Contatto data = (Contatto) adapterView.getItemAtPosition(i);

                listView.setItemChecked(i, true);
                //adapterView.getAdapter().getItem(i);
                if(data.getCognome().compareTo("Nuova Chat")==0)
                {
                    Intent intent=new Intent(getApplicationContext(),ChatActivity.class);
                    startActivity(intent);

                }

                //ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                //ClipData clip = ClipData.newPlainText("ID", data.getCognome().toString());
                //clipboard.setPrimaryClip(clip);

                Toast.makeText(getBaseContext(), data.getCognome(), Toast.LENGTH_SHORT).show();
            }
        });

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_list, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case R.id.MENU_1:

                Intent intent=new Intent(getApplicationContext(),ChatActivity.class);
                startActivity(intent);

/*
Codice di gestione della voce MENU_1
*/
                break;
            case R.id.MENU_2:
/*
Codice di gestione della voce MENU_2
*/
        }
        return true;
    }
}

