package com.chatlayoutexample;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
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

import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;


public class ListaUtenti extends AppCompatActivity {

    Handler handler;
    private ImageButton m_btnExit;
    CustomAdapter adapter;
    private Context mContext;
    List<String> mLines;
    List<Contatto> list = new LinkedList<Contatto>();
    ListView listView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.list_utenti);

        listView = (ListView) findViewById(R.id.listViewDemo);

        String strOld ="KEY";
        list.clear();
        list.add(new Contatto("+", "Nuova Chat", "Nuova Chat"));

        mLines =  readFromFile(getBaseContext());
        for (String string : mLines)
        {
            String[] arrId = string.split("\t");
            if(strOld.compareTo(arrId[0] )!=0) {
                list.add(new Contatto("","", arrId[0]));
                strOld =  arrId[0];
            }
        }
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
                else
                {
                    Intent intentView=new Intent(getApplicationContext(),ChatActivityView.class);
                    intentView.putExtra("IdSelezionato", data.getTelefono());
                    startActivity(intentView);

                }

                //ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                //ClipData clip = ClipData.newPlainText("ID", data.getCognome().toString());
                //clipboard.setPrimaryClip(clip);
                //Toast.makeText(getBaseContext(), data.getCognome(), Toast.LENGTH_SHORT).show();
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

        ArrayList<String> arrIdNome;
        switch (id) {
            case R.id.MENU_1:

                Intent intent=new Intent(getApplicationContext(),ChatActivity.class);
                startActivity(intent);

                break;
            case R.id.MENU_2:

                String strOld ="KEY";
                list.clear();
                list.add(new Contatto("+", "Nuova Chat", "Nuova Chat"));

                mLines =  readFromFile(getBaseContext());
                int i = 0;
                for (String string : mLines)
                {
                    String[] arrId = string.split("\t");
                    if(strOld.compareTo(arrId[0] )!=0) {
                        list.add(new Contatto("", "", arrId[0]));
                        strOld =  arrId[0];
                    }
                }
                adapter = new CustomAdapter(this, R.layout.list_item_utenti, list);
                listView.setAdapter(adapter);
                break;
        }
        return true;
    }


    private ArrayList<String> readFromFile(Context context) {

        ArrayList<String> ret = new ArrayList<>();
        
            //java.io.File sdcard = Environment.getExternalStorageDirectory() ;
            java.io.File sdcard  = new java.io.File(Environment.getExternalStorageDirectory() + java.io.File.separator + "Download" + "/CONVERSAZIONI");

            java.io.File  file = new java.io.File(sdcard, "conversazioni.txt");

            StringBuilder text = new StringBuilder();

            try {
                BufferedReader br = new BufferedReader(new FileReader(file));
                String line;

                while ((line = br.readLine()) != null) {
                    ret.add(line);

                }
                br.close();
            } catch (IOException e) {
                //You'll need to add proper error handling here
            }

        return ret;
    }


    public String read_file(Context context, String filename) {
        try {
            FileInputStream fis = context.openFileInput(filename);
            InputStreamReader isr = new InputStreamReader(fis, "UTF-8");
            BufferedReader bufferedReader = new BufferedReader(isr);
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                sb.append(line).append("\n");
            }
            return sb.toString();
        } catch (FileNotFoundException e) {
            return "";
        } catch (UnsupportedEncodingException e) {
            return "";
        } catch (IOException e) {
            return "";
        }
    }
}

