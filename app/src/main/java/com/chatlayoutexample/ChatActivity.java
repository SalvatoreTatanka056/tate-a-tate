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

	import java.io.FileOutputStream;
	import java.io.IOException;
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


	public class ChatActivity extends AppCompatActivity implements LocationListener {

		private static SecretKeySpec secret;
		//private static String password = "Majabella@56";
		static  byte[]  key = "!@#$!@#$%^&**&^%".getBytes();
		final static String algorithm="AES";

		private GoogleApiClient mGoogleApiClient;
		private Location mLastLocation;
		private EditText messageET;
		private ListView messagesContainer;
		private Button sendBtn;
		private ChatAdapter adapter;
		private ArrayList<ChatMessage> chatHistory;
		private static final String TAG = "ChatActivity";
		private static final int REQUEST_CODE_SIGN_IN = 1;
		private static final int REQUEST_CODE_OPEN_DOCUMENT = 2;
		private DriveServiceHelper mDriveServiceHelper;
		private String mOpenFileId;
		private EditText mFileTitleEditText;
		private EditText mDocContentEditText;
		private EditText mIdIntelocutore;
		private ImageButton mBtnCollega;
		private ImageButton mBtnPlay;
		private ImageButton mBtnIncolla;
		private ImageButton mBtnCancella;
		private ProgressBar mPrgMain;
		public boolean flagFolder = false;
		public List<String> mlistId;
		public String mId;
		public String mContentFile;
		public String mNomeFile;
		public String mNomeFileConnect;
		public int miStart = 0;
		public int mIDMessagioIniziale = 0;
		public int mIntCountFile = 0;
		public boolean bFlagFileExist;
		public int i = 0;
		private String providerId = LocationManager.GPS_PROVIDER;
		private LocationManager locationManager = null;
		private static final int MIN_DIST = 20;
		private static final int MIN_PERIOD = 30000;
		private String mIndirizzo;
		double destLat, destLong;
		private Encryption encryption = Encryption.getDefault("Key", "Salt", new byte[16]);
		NotificationManager notificationManager ;
		private boolean mflagAttiva = true;

		private Timer myTimer;
		private static final int ESTIMATED_TOAST_HEIGHT_DIPS = 48;

		@Override
		protected void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			setContentView(R.layout.activity_chat);

			mIdIntelocutore = (EditText) findViewById(R.id.editTextTextMultiLine);
			mPrgMain = (ProgressBar) findViewById(R.id.prgMain);
			mBtnCollega = (ImageButton) findViewById(R.id.BtnExit);
			mBtnCancella = (ImageButton) findViewById(R.id.btnCancella);
			mBtnCollega.setOnClickListener(view -> query());
			mBtnCollega.setVisibility(View.INVISIBLE);

			mIdIntelocutore.setOnFocusChangeListener(new View.OnFocusChangeListener() {
				@Override
				public void onFocusChange(View v, boolean hasFocus) {
					if (hasFocus == true) {
						showCheatSheet(v, "Id Intelocutore.");
						// Show tool tip
					} else {
						// Hide tool tip
						showCheatSheet(v, "");
					}
				}
			});

			mBtnIncolla = (ImageButton) findViewById(R.id.btnIncolla);
			mBtnIncolla.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {

					if (mIdIntelocutore.getText().length() > 0) {
						mIdIntelocutore.setSelectAllOnFocus(true);
						mIdIntelocutore.requestFocus();
						ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
						mIdIntelocutore.setText(clipboard.getText().toString());
					}

				}
			});

			mBtnPlay = (ImageButton) findViewById(R.id.btnPlay);
			mBtnPlay.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
					ClipData clip = ClipData.newPlainText("ID", mIdIntelocutore.getText().toString());
					clipboard.setPrimaryClip(clip);
					Toast.makeText(getBaseContext(), "Copia Id effettuata.", Toast.LENGTH_SHORT).show();
					mIdIntelocutore.setSelectAllOnFocus(true);
					mIdIntelocutore.requestFocus();

				}
			});

			java.io.File fileConnectChat = new java.io.File(Environment.getExternalStorageDirectory() + java.io.File.separator + "Download" + "/CONNECT");
			fileConnectChat.mkdirs();

			java.io.File fileChat = new java.io.File(Environment.getExternalStorageDirectory() + java.io.File.separator + "Download" + "/CONNECT_CHAR");
			fileChat.mkdirs();

			initControls();
			requestSignIn();

			messageET.setEnabled(false);

			messagesContainer.setOnItemClickListener(new AdapterView.OnItemClickListener() {
				@Override
				public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
					ChatMessage data = (ChatMessage) adapterView.getItemAtPosition(i);
					//adapterView.getAdapter().getItem(i);

					ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
					ClipData clip = ClipData.newPlainText("ID", data.getMessage().toString());
					clipboard.setPrimaryClip(clip);

					Toast.makeText(getBaseContext(), data.getMessage(), Toast.LENGTH_SHORT).show();
				}
			});

			mBtnCancella.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {

					ChatAdapter MessageAdapter = (ChatAdapter) messagesContainer.getAdapter();
					MessageAdapter.clear();
					adapter.notifyDataSetChanged();

				}
			});

		 //locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		 //locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 5000, 10, this);

			notificationManager = getSystemService(NotificationManager.class);


		}

		Handler handler;

		private static boolean showCheatSheet(View view, CharSequence text) {
			if (TextUtils.isEmpty(text)) {
				return false;
			}

			final int[] screenPos = new int[2];
			final Rect displayFrame = new Rect();
			view.getLocationOnScreen(screenPos);
			view.getWindowVisibleDisplayFrame(displayFrame);

			final Context context = view.getContext();
			final int viewWidth = view.getWidth();
			final int viewHeight = view.getHeight();
			final int viewCenterX = screenPos[0] + viewWidth / 2;
			final int screenWidth = context.getResources().getDisplayMetrics().widthPixels;
			final int estimatedToastHeight = (int) (ESTIMATED_TOAST_HEIGHT_DIPS
					* context.getResources().getDisplayMetrics().density);

			Toast cheatSheet = Toast.makeText(context, text, Toast.LENGTH_SHORT);
			boolean showBelow = screenPos[1] < estimatedToastHeight;
			if (showBelow) {

				cheatSheet.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL,
						viewCenterX - screenWidth / 2,
						screenPos[1] - displayFrame.top + viewHeight);
			} else {

				cheatSheet.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL,
						viewCenterX - screenWidth / 2,
						screenPos[1] - displayFrame.top - estimatedToastHeight);
			}

			cheatSheet.show();
			return true;
		}

		private void TimerMethod() {
			this.runOnUiThread(Timer_Tick);
		}

		private Runnable Timer_Tick = new Runnable() {
			public void run() {
				if (mDriveServiceHelper.bflagStartTimer) {
					mBtnCollega.callOnClick();
				}
			}
		};

		@Override
		public void onActivityResult(int requestCode, int resultCode, Intent resultData) {
			switch (requestCode) {
				case REQUEST_CODE_SIGN_IN:
					if (resultCode == Activity.RESULT_OK && resultData != null) {
						handleSignInResult(resultData);
					}
					break;

				case REQUEST_CODE_OPEN_DOCUMENT:
					if (resultCode == Activity.RESULT_OK && resultData != null) {
						Uri uri = resultData.getData();
						if (uri != null) {
							openFileFromFilePicker(uri);
						}
					}
					break;
			}
			super.onActivityResult(requestCode, resultCode, resultData);
		}

		private void requestSignIn() {
			Log.d(TAG, "Requesting sign-in");

			GoogleSignInOptions signInOptions =
					new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
							.requestEmail()
							.requestScopes(new Scope(DriveScopes.DRIVE_FILE))
							.build();
			GoogleSignInClient client = GoogleSignIn.getClient(this, signInOptions);

			startActivityForResult(client.getSignInIntent(), REQUEST_CODE_SIGN_IN);
		}

		public String getMyPhoneNumber() {
			String device_unique_id = Settings.Secure.getString(getBaseContext().getContentResolver(),
					Settings.Secure.ANDROID_ID);
			return device_unique_id;
		}


		private void handleSignInResult(Intent result) {
			GoogleSignIn.getSignedInAccountFromIntent(result)
					.addOnSuccessListener(googleAccount -> {
						Log.d(TAG, "Signed in as " + googleAccount.getEmail());

						// Use the authenticated account to sign in to the Drive service.
						GoogleAccountCredential credential =
								GoogleAccountCredential.usingOAuth2(
										this, Collections.singleton(DriveScopes.DRIVE_FILE));
						credential.setSelectedAccount(googleAccount.getAccount());
						Drive googleDriveService =
								new Drive.Builder(
										AndroidHttp.newCompatibleTransport(),
										new GsonFactory(),
										credential)
										.setApplicationName("Drive API Migration")
										.build();


						mDriveServiceHelper = new DriveServiceHelper(googleDriveService);

						/* --- flag per controllare e salvare in vecchio Id creato */
						/* --- e riconnettere con il vecchio ID se si salva la chat */

						SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.ITALY);
						String time = sdf.format(new Date());

						String Folder = String.format("CHAT_%s", time);
						String FolderC = String.format("CHAT_CONNECT_%s", time);
						createFolder(FolderC, "F");
						mNomeFile = String.format("%s_C", getMyPhoneNumber());

						createFile(mNomeFile, "");
						createFolder(Folder, "C");
						mBtnPlay.setEnabled(true);
						sendBtn.setEnabled(true);

						/* --- */

					})
					.addOnFailureListener(exception -> Log.e(TAG, "Unable to sign in.", exception));
		}

		private void openFilePicker() {
			if (mDriveServiceHelper != null) {
				Log.d(TAG, "Opening file picker.");

				Intent pickerIntent = mDriveServiceHelper.createFilePickerIntent();

				// The result of the SAF Intent is handled in onActivityResult.
				startActivityForResult(pickerIntent, REQUEST_CODE_OPEN_DOCUMENT);
			}
		}

		private void openFileFromFilePicker(Uri uri) {
			if (mDriveServiceHelper != null) {
				Log.d(TAG, "Opening " + uri.getPath());

				mDriveServiceHelper.openFileUsingStorageAccessFramework(getContentResolver(), uri)
						.addOnSuccessListener(nameAndContent -> {
							String name = nameAndContent.first;
							String content = nameAndContent.second;
							// Files opened through SAF cannot be modified.
							setReadOnlyMode();
						})
						.addOnFailureListener(exception ->
								Log.e(TAG, "Unable to open file from picker.", exception));
			}
		}

		private void createFile(String NomeFile, String Message) {
			if (mDriveServiceHelper != null) {
				Log.d(TAG, "Creating a file.");
				flagFolder = false;

				String encrypted = encryption.encryptOrNull(Message);

				mDriveServiceHelper.createFile(NomeFile, encrypted)
						.addOnSuccessListener(fileId -> readFile(fileId))
						.addOnFailureListener(exception ->
								Toast.makeText(this, String.valueOf(exception.toString()), Toast.LENGTH_LONG).show());
			}
		}

		private void createFileConnect(String NomeFile, String Message, String Folder) {
			if (mDriveServiceHelper != null) {
				Log.d(TAG, "Creating a file.");
				flagFolder = false;

				mDriveServiceHelper.createFileConnect(NomeFile, Message, Folder)
						.addOnSuccessListener(fileId -> readFileNULL(fileId))
						.addOnFailureListener(exception ->
								Toast.makeText(this, String.valueOf(exception.toString()), Toast.LENGTH_LONG).show());
			}
		}

		private void createFolder(String NomeFile, String Tipo) {
			if (mDriveServiceHelper != null) {
				Log.d(TAG, "Creating a file.");
				flagFolder = true;

				mDriveServiceHelper.createFolder(NomeFile, Tipo)
						.addOnSuccessListener(fileId -> readFile(fileId))
						.addOnFailureListener(exception ->
								Toast.makeText(this, String.valueOf(exception.toString()), Toast.LENGTH_LONG).show());
			}
		}

		private void readFile(String fileId) {

			if (flagFolder) {
				mId += mDriveServiceHelper.getmIdFolderCurrent() + " ";
				mIDMessagioIniziale++;

			}
			if (mIDMessagioIniziale == 3) {
				mIdIntelocutore.setText(mId.substring(4));
				//mIdIntelocutore.setText(mId);

				mIDMessagioIniziale = 0;
				messageET.setEnabled(true);
				mPrgMain.setVisibility(View.INVISIBLE);

				myTimer = new Timer();
				myTimer.schedule(new TimerTask() {
					@Override
					public void run() {
						TimerMethod();
					}

				}, 2000, 3000);

			}
		}

		@Override
		protected void onPause() {
			super.onPause();
			mflagAttiva = false;
		}

		@Override
		protected void onResume() {
			super.onResume();
			mflagAttiva = true;
		}


		private void readFileNULL(String fileId) {
			mDriveServiceHelper.bflagStartTimer = true;
		}

		private void readFileDrive(String fileId) {

			if (mDriveServiceHelper != null) {
				Log.d(TAG, "Reading file " + fileId);

				mDriveServiceHelper.readFile(fileId)
						.addOnSuccessListener(nameAndContent -> {
							String ContentFile =encryption.decryptOrNull(nameAndContent.second);

							if(mflagAttiva == false) {

								String CHANNEL_ID = "my_channel_id";
								String channel_name = "channel_name";
								String channel_description = "channel_description";
								if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
									NotificationChannel channel = new NotificationChannel(CHANNEL_ID, channel_name, NotificationManager.IMPORTANCE_DEFAULT);
									channel.setDescription(channel_description);
									notificationManager.createNotificationChannel(channel);
								}
								NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
										.setSmallIcon(android.R.drawable.star_on)
										.setContentTitle("tate-a-tate: Nuova notifica!!")
										.setContentText(ContentFile)
										.setPriority(NotificationCompat.PRIORITY_DEFAULT);
								notificationManager.notify(0, builder.build());
							}

							ChatMessage chatMessage = new ChatMessage();
							chatMessage.setId(122);//dummy
							chatMessage.setMessage(ContentFile);
							chatMessage.setDate(DateFormat.getDateTimeInstance().format(new Date()));
							chatMessage.setMe(false);

							if (ContentFile.isEmpty() == false) {
								displayMessage(chatMessage);
								DeleteFile(fileId);
							}
						})
						.addOnFailureListener(exception ->
								Log.e(TAG, "Couldn't read file.", exception));
			}
		}

		public void queryCount(String[] IdFolders) {
			if (mDriveServiceHelper != null) {
				Log.d(TAG, "Querying for files.");
				// bflagStartTimer=false;

				mDriveServiceHelper.setmIdFolderCurrent(IdFolders[1]);
				mDriveServiceHelper.queryFilesCount()
						.addOnSuccessListener(fileList -> {

							List<File> lstFile = fileList.getFiles();
							mIntCountFile = lstFile.size();
							bFlagFileExist = false;
							if (mIntCountFile < 2) {

								for (File file : fileList.getFiles()) {
									mNomeFileConnect = String.format("%s_C", getMyPhoneNumber());
									if (file.getName().compareToIgnoreCase(mNomeFileConnect) == 0) {
										bFlagFileExist = true;
									}
								}

								if (!bFlagFileExist) {

									createFileConnect(String.format("%s_C", getMyPhoneNumber()), "", IdFolders[2]);
								} else {
									mDriveServiceHelper.setmIdFolderCurrent(mDriveServiceHelper.mGlobalIdFolder);
								}

							} else {
								Toast.makeText(getBaseContext(), "ATTENZIONE impossibile entrare nella stanza", Toast.LENGTH_LONG);
								mDriveServiceHelper.setmIdFolderCurrent(mDriveServiceHelper.mGlobalIdFolder);
							}
						})
						.addOnFailureListener(exception -> {
							Toast.makeText(this, String.valueOf(exception.toString()), Toast.LENGTH_LONG).show();
						});
			}
		}

		private void query() {
			if (mDriveServiceHelper != null) {
				Log.d(TAG, "Querying for files.");

				mDriveServiceHelper.queryFiles()
						.addOnSuccessListener(fileList -> {
							for (File file : fileList.getFiles()) {
								if (file.getName().compareToIgnoreCase(mNomeFile) != 0) {
									readFileDrive(file.getId());
								}
							}

						})
						.addOnFailureListener(exception -> {
							Toast.makeText(this, String.valueOf(exception.toString()), Toast.LENGTH_LONG).show();
						});
			}
		}

		public void DeleteFile(String FileId) {
			mDriveServiceHelper.DeleteFile(FileId)
					.addOnFailureListener(exception ->
							Log.e(TAG, "Unable to save file via REST.", exception));

		}

		private void setReadOnlyMode() {
			mFileTitleEditText.setEnabled(false);
			mDocContentEditText.setEnabled(false);
			mOpenFileId = null;
		}

		private void setReadWriteMode(String fileId) {
			mFileTitleEditText.setEnabled(true);
			mDocContentEditText.setEnabled(true);
			mOpenFileId = fileId;
		}


		@Override
		public void onDestroy() {
			super.onDestroy();

			String[] IdFolders = mId.split(" ");

			DeleteFile(IdFolders[1]);
			DeleteFile(IdFolders[2]);

			Toast.makeText(getBaseContext(), "Chiusura Chat, chiudere e riaprire.", Toast.LENGTH_LONG);
		}

		@Override
		public boolean onCreateOptionsMenu(Menu menu) {
			// Inflate the menu; this adds items to the action bar if it is present.
			getMenuInflater().inflate(R.menu.menu_chat, menu);
			return true;
		}

		public LocationListener ll;
		public LocationManager lm;

		public static final String CALCULATOR_PACKAGE = "com.android.calculator2";
		public static final String CALCULATOR_CLASS = "com.android.calculator2.Calculator";


		@Override
		public boolean onOptionsItemSelected(MenuItem item) {
			int id = item.getItemId();

			if (id == R.id.action_settings) {
				mId = mIdIntelocutore.getText().toString();
				String[] IdFolders = mId.split(" ");

				mDriveServiceHelper.bflagStartTimer = false;
				queryCount(IdFolders);

				return true;
			}

			if (id == R.id.action_gps) {

				messageET.setText("http://maps.google.com/maps?q=40.9148405,14.5496018&ll=" + Double.toString(destLat) + "," + Double.toString(destLong) + "&z=17");

				return true;
			}

			if (id == R.id.action_calc) {

				//Intent nuovaPagina = new Intent(this, HelpActivity.class);
				//startActivity(nuovaPagina);

				return true;
			}

			if (id == R.id.action_shared) {

				ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);

				Intent sendIntent = new Intent();
				sendIntent.setAction(Intent.ACTION_SEND);
				sendIntent.putExtra(Intent.EXTRA_TEXT,clipboard.getText().toString());
				sendIntent.setType("text/plain");

				Intent shareIntent = Intent.createChooser(sendIntent, null);
				startActivity(shareIntent);

				return true;

			}

			if (id == R.id.action_contatto) {

				//Intent nuovaPagina = new Intent(this, InputActivity.class);
				//startActivity(nuovaPagina);

				mPrgMain.setVisibility(View.VISIBLE);

				writeToFile();

				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}

				mPrgMain.setVisibility(View.INVISIBLE);

				return true;
			}

			return super.onOptionsItemSelected(item);
		}

		public void writeToFile(/*String strNota*/)
		{

			// Get the directory for the user's public pictures directory.
			java.io.File fileConnectChat = new java.io.File(Environment.getExternalStorageDirectory() + java.io.File.separator + "Download" + "/CONVERSAZIONI");

			// Make sure the path directory exists.
			if(!fileConnectChat.exists())
			{
				// Make it, if it doesn't exit
				fileConnectChat.mkdirs();

			}

			final java.io.File file = new java.io.File(fileConnectChat, "conversazioni.txt");

			try
			{
				if(!file.exists())
				{
					file.createNewFile();
				}
				FileOutputStream fOut = new FileOutputStream(file,true);
				OutputStreamWriter myOutWriter = new OutputStreamWriter(fOut);

				ListAdapter adapter_tmp = messagesContainer.getAdapter();
				for (int i = 0; i<adapter_tmp.getCount();i++ ) {
					ChatMessage cMsg = (ChatMessage) adapter_tmp.getItem(i);

					String[] IdFolders = mId.split(" ");
					String pp = String.format("%s\t%s\t%s\n",IdFolders[1],cMsg.getMessage().toString(),cMsg.getIsme());
					myOutWriter.append(pp);

				}

				myOutWriter.close();

				fOut.flush();
				fOut.close();
			}
			catch (IOException e)
			{
				Log.e("Exception", "File write failed: " + e.toString());
			}
		}


		private void onGPSLocationChanged(Location location) {
			if (location != null) {
				double pLong = location.getLongitude();
				double pLat = location.getLatitude();


			}
		}

		private void initControls() {
			messagesContainer = (ListView) findViewById(R.id.messagesContainer);
			messageET = (EditText) findViewById(R.id.messageEdit);
			sendBtn = (Button) findViewById(R.id.chatSendButton);

			RelativeLayout container = (RelativeLayout) findViewById(R.id.container);

			loadDummyHistory();

			sendBtn.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {

					String messageText = messageET.getText().toString();
					if (TextUtils.isEmpty(messageText)) {
						return;
					}

					mNomeFile = String.format("%s_A", getMyPhoneNumber());
					createFile(mNomeFile, messageText);
					ChatMessage chatMessage = new ChatMessage();
					chatMessage.setId(122);//dummy
					chatMessage.setMessage(messageText);
					chatMessage.setDate(DateFormat.getDateTimeInstance().format(new Date()));
					chatMessage.setMe(true);

					messageET.setText("");

					displayMessage(chatMessage);
				}
			});

			sendBtn.setEnabled(false);
			messageET.setEnabled(false);
		}

		public void displayMessage(ChatMessage message) {
			adapter.add(message);
			adapter.notifyDataSetChanged();
			scroll();
		}

		private void scroll() {
			messagesContainer.setSelection(messagesContainer.getCount() - 1);
		}

		private void loadDummyHistory() {

			chatHistory = new ArrayList<ChatMessage>();

			ChatMessage msg = new ChatMessage();
			msg.setId(1);
			msg.setMe(false);
			msg.setMessage("Ciao");
			msg.setDate(DateFormat.getDateTimeInstance().format(new Date()));
			chatHistory.add(msg);
			ChatMessage msg1 = new ChatMessage();
			msg1.setId(2);
			msg1.setMe(false);
			msg1.setMessage("Ciao come stai?");
			msg1.setDate(DateFormat.getDateTimeInstance().format(new Date()));
			chatHistory.add(msg1);

			adapter = new ChatAdapter(ChatActivity.this, new ArrayList<ChatMessage>());
			messagesContainer.setAdapter(adapter);

			for (int i = 0; i < chatHistory.size(); i++) {
				ChatMessage message = chatHistory.get(i);
				displayMessage(message);
			}

		}

		@Override
		public void onLocationChanged(@NonNull Location location) {
			destLat = location.getLatitude();
			destLong = location.getLongitude();

		}

		// Method to start the service
		public void startService(View view) {
			startService(new Intent(getBaseContext(), tateatateService.class));
		}

		// Method to stop the service
		public void stopService(View view) {
			stopService(new Intent(getBaseContext(), tateatateService.class));
		}


	}
