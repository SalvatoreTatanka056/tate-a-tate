	package com.chatlayoutexample;

	import android.app.Activity;
	import android.app.NotificationChannel;
	import android.app.NotificationManager;
	import android.content.ClipData;
	import android.content.ClipboardManager;
	import android.content.ComponentName;
	import android.content.Context;
	import android.content.Intent;
	import android.content.ServiceConnection;
	import android.graphics.Rect;
	import android.location.Location;
	import android.location.LocationListener;
	import android.location.LocationManager;
	import android.net.Uri;
	import android.os.Bundle;
	import android.os.Environment;

	import android.os.Handler;
	import android.os.IBinder;
	import android.provider.Settings;
	import android.text.TextUtils;
	import android.util.Log;
	import android.view.Gravity;
	import android.view.Menu;
	import android.view.MenuItem;
	import android.view.View;
	import android.widget.AdapterView;
	import android.widget.Button;
	import android.widget.EditText;
	import android.widget.ImageButton;
	import android.widget.ListAdapter;
	import android.widget.ListView;
	import android.widget.ProgressBar;
	import android.widget.RelativeLayout;
	import android.widget.Toast;

	import com.google.android.gms.auth.api.signin.GoogleSignIn;
	import com.google.android.gms.auth.api.signin.GoogleSignInClient;
	import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
	import com.google.android.gms.common.api.GoogleApiClient;
	import com.google.android.gms.common.api.Scope;
	import com.google.api.client.extensions.android.http.AndroidHttp;
	import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
	import com.google.api.client.json.gson.GsonFactory;
	import com.google.api.services.drive.Drive;
	import com.google.api.services.drive.DriveScopes;
	import com.google.api.services.drive.model.File;

	import androidx.annotation.NonNull;
	import androidx.appcompat.app.AppCompatActivity;
	import androidx.core.app.NotificationCompat;


	import org.apache.commons.codec.binary.Base64;

	import java.io.BufferedReader;
	import java.io.FileInputStream;
	import java.io.FileNotFoundException;
	import java.io.FileOutputStream;
	import java.io.FileReader;
	import java.io.IOException;
	import java.io.InputStreamReader;
	import java.io.OutputStreamWriter;
	import java.io.UnsupportedEncodingException;
	import java.security.InvalidAlgorithmParameterException;
	import java.security.InvalidKeyException;
	import java.security.NoSuchAlgorithmException;
	import java.security.spec.InvalidKeySpecException;
	import java.security.spec.InvalidParameterSpecException;
	import java.text.DateFormat;
	import java.text.SimpleDateFormat;
	import java.util.ArrayList;
	import java.util.Collections;
	import java.util.Date;
	import java.util.List;
	import java.util.Locale;
	import java.util.Timer;
	import java.util.TimerTask;

	import javax.crypto.BadPaddingException;
	import javax.crypto.Cipher;
	import javax.crypto.IllegalBlockSizeException;
	import javax.crypto.NoSuchPaddingException;
	import javax.crypto.SecretKey;
	import javax.crypto.spec.SecretKeySpec;

	import se.simbio.encryption.Encryption;


	public class ChatActivityView extends AppCompatActivity  {


		private Button sendBtn;
		private ListView messagesContainer;
		private ChatAdapter adapter;
		private ArrayList<ChatMessage> chatHistory;
		private static final String TAG = "ChatActivity";
		private ArrayList<String> mLines;
		private  String stringaPassata;

		@Override
		protected void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			setContentView(R.layout.activity_chat_view);

			messagesContainer = (ListView) findViewById(R.id.messagesContainer);
			sendBtn = (Button) findViewById(R.id.chatSendButton);


			Bundle dati = getIntent().getExtras();
			stringaPassata = dati.getString("IdSelezionato");

			chatHistory = new ArrayList<ChatMessage>();

			mLines =  readFromFile();
			for (String string : mLines)
			{
				String[] arrId = string.split("\t");

				if(arrId[0].compareTo(stringaPassata)==0) {

					ChatMessage msg = new ChatMessage();

					if(arrId[2].compareTo("false")==0) {

						msg.setMe(false);
						msg.setMessage(arrId[1]);
						//msg.setDate(DateFormat.getDateTimeInstance().format(new Date()));

					}
					else
					{

						msg.setMe(true);
						msg.setMessage(arrId[1]);
						//msg.setDate(DateFormat.getDateTimeInstance().format(new Date()));
					}
					chatHistory.add(msg);
				}
			}

			adapter = new ChatAdapter(ChatActivityView.this, new ArrayList<ChatMessage>());
			messagesContainer.setAdapter(adapter);

			for (int i = 0; i < chatHistory.size(); i++) {
				ChatMessage message = chatHistory.get(i);
				displayMessage(message);


			}
		}

		private ArrayList<String> readFromFile() {

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




		public void displayMessage(ChatMessage message) {
			adapter.add(message);
			adapter.notifyDataSetChanged();
			scroll();
		}

		private void scroll() {
			messagesContainer.setSelection(messagesContainer.getCount() - 1);
		}



	}
