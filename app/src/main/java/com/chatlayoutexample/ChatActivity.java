package com.chatlayoutexample;

import android.Manifest;
import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.graphics.Rect;
import android.location.Address;
import android.location.Criteria;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;

import android.os.Handler;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.text.Editable;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.tasks.Task;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;


import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.function.Consumer;

class Task1 extends AsyncTask {

    private ChatActivity Frm ;
    Task1(ChatActivity DriveSH){Frm= DriveSH;}

    @Override
    protected Object doInBackground(Object... params) {
        // TODO Auto-generated method stub
        Log.v ("gaurav", "Thread task 1 is : " + Thread.currentThread().getName());

        //Frm.queryCount();

        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        Log.v ("gaurav", "Log after sleeping");

        return null;
    }
}

public class ChatActivity extends AppCompatActivity {

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
    private ProgressBar mPrgMain;
    public boolean flagFolder = false;
    public List<String> mlistId;
    public String mId;
    public String mContentFile;
    public String mNomeFile;
    public String mNomeFileConnect;
    public int miStart=0;
    public int mIDMessagioIniziale = 0;
    public int mIntCountFile=0;
    public boolean bFlagFileExist;
    public  int i=0;
    private String providerId = LocationManager.GPS_PROVIDER;
    private LocationManager locationManager=null;
    private static final int MIN_DIST=20;
    private static final int MIN_PERIOD=30000;

    private Timer myTimer;
    private static final int ESTIMATED_TOAST_HEIGHT_DIPS = 48;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        mIdIntelocutore = (EditText) findViewById(R.id.editTextTextMultiLine);
        mPrgMain =(ProgressBar)  findViewById(R.id.prgMain);
        mBtnCollega = (ImageButton) findViewById(R.id.BtnCollega);
        mBtnCollega.setOnClickListener(view -> query());
        mBtnCollega.setVisibility(View.INVISIBLE);

        mIdIntelocutore.setOnFocusChangeListener(new View.OnFocusChangeListener()
        {
            @Override
            public void onFocusChange(View v, boolean hasFocus)
            {
                if (hasFocus == true)
                {
                    showCheatSheet(v,"Id Intelocutore.");
                    // Show tool tip
                }else{
                    // Hide tool tip
                    showCheatSheet(v,"");
                }
            }
        });


        mBtnIncolla = (ImageButton) findViewById(R.id.btnIncolla);
        mBtnIncolla.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mIdIntelocutore.setSelectAllOnFocus(true);
                mIdIntelocutore.requestFocus();
                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                mIdIntelocutore.setText( clipboard.getText().toString());

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
                 ChatMessage data =(ChatMessage) adapterView.getItemAtPosition(i);
                 //adapterView.getAdapter().getItem(i);

                 ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                 ClipData clip = ClipData.newPlainText("ID",data.getMessage().toString());
                 clipboard.setPrimaryClip(clip);

                 //String message = entry.getMessage();

                 Toast.makeText(getBaseContext(), data.getMessage(), Toast.LENGTH_SHORT).show();
             }
         });

    }

    private static boolean showCheatSheet(View view, CharSequence text) {
        if (TextUtils.isEmpty(text)) {
            return false;
        }

        final int[] screenPos = new int[2]; // origin is device display
        final Rect displayFrame = new Rect(); // includes decorations (e.g. status bar)
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
            // Show below
            // Offsets are after decorations (e.g. status bar) are factored in
            cheatSheet.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL,
                    viewCenterX - screenWidth / 2,
                    screenPos[1] - displayFrame.top + viewHeight);
        } else {
            // Show above
            // Offsets are after decorations (e.g. status bar) are factored in
            // NOTE: We can't use Gravity.BOTTOM because when the keyboard is up
            // its height isn't factored in.
            cheatSheet.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL,
                    viewCenterX - screenWidth / 2,
                    screenPos[1] - displayFrame.top - estimatedToastHeight);
        }

        cheatSheet.show();
        return true;
    }


    private void TimerMethod()
    {
        this.runOnUiThread(Timer_Tick);
    }


    private Runnable Timer_Tick = new Runnable() {
        public void run() {
            if( mDriveServiceHelper.bflagStartTimer) {
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

    /**
     * Starts a sign-in activity using {@link #REQUEST_CODE_SIGN_IN}.
     */
    private void requestSignIn() {
        Log.d(TAG, "Requesting sign-in");

        GoogleSignInOptions signInOptions =
                new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .requestEmail()
                        .requestScopes(new Scope(DriveScopes.DRIVE_FILE))
                        .build();
        GoogleSignInClient client = GoogleSignIn.getClient(this, signInOptions);

        // The result of the sign-in Intent is handled in onActivityResult.
        startActivityForResult(client.getSignInIntent(), REQUEST_CODE_SIGN_IN);
        }

        public String getMyPhoneNumber(){
            String device_unique_id = Settings.Secure.getString(getBaseContext().getContentResolver(),
                    Settings.Secure.ANDROID_ID);
            return device_unique_id;
        }

    /**
     * Handles the {@code result} of a completed sign-in activity initiated from {@link
     * #requestSignIn()}.
     */
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

                    // The DriveServiceHelper encapsulates all REST API and SAF functionality.
                    // Its instantiation is required before handling any onClick actions.
                    mDriveServiceHelper = new DriveServiceHelper(googleDriveService);

                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.ITALY);
                    String time=sdf.format(new Date());

                    String Folder = String.format("CHAT_%s",time);
                    String FolderC = String.format("CHAT_CONNECT_%s",time);
                    createFolder(FolderC,"F");
                    mNomeFile = String.format("%s_C", getMyPhoneNumber());
                    createFile(mNomeFile,"");
                    createFolder(Folder,"C");
                    mBtnPlay.setEnabled(true);
                    sendBtn.setEnabled(true);

                })
                .addOnFailureListener(exception -> Log.e(TAG, "Unable to sign in.", exception));
    }

    /**
     * Opens the Storage Access Framework file picker using {@link #REQUEST_CODE_OPEN_DOCUMENT}.
     */
    private void openFilePicker() {
        if (mDriveServiceHelper != null) {
            Log.d(TAG, "Opening file picker.");

            Intent pickerIntent = mDriveServiceHelper.createFilePickerIntent();

            // The result of the SAF Intent is handled in onActivityResult.
            startActivityForResult(pickerIntent, REQUEST_CODE_OPEN_DOCUMENT);
        }
    }

    /**
     * Opens a file from its {@code uri} returned from the Storage Access Framework file picker
     * initiated by {@link #openFilePicker()}.
     */
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

    /**
     * Creates a new file via the Drive REST API.
     */
    private void createFile(String NomeFile,String Message) {
        if (mDriveServiceHelper != null) {
            Log.d(TAG, "Creating a file.");
            flagFolder=false;

            mDriveServiceHelper.createFile(NomeFile,Message)
                    .addOnSuccessListener(fileId -> readFile(fileId))
                    .addOnFailureListener(exception ->
                            Toast.makeText(this, String.valueOf(exception.toString()),Toast.LENGTH_LONG).show());
        }
    }

    private void createFileConnect(String NomeFile,String Message,String Folder) {
        if (mDriveServiceHelper != null) {
            Log.d(TAG, "Creating a file.");
            flagFolder=false;

            mDriveServiceHelper.createFileConnect(NomeFile,Message,Folder)
                    .addOnSuccessListener(fileId -> readFileNULL(fileId))
                    .addOnFailureListener(exception ->
                            Toast.makeText(this, String.valueOf(exception.toString()),Toast.LENGTH_LONG).show());
        }
    }

    private void createFolder(String NomeFile,String Tipo) {
        if (mDriveServiceHelper != null) {
            Log.d(TAG, "Creating a file.");
            flagFolder=true;

            mDriveServiceHelper.createFolder(NomeFile,Tipo)
                    .addOnSuccessListener(fileId -> readFile(fileId))
                    .addOnFailureListener(exception ->
                            Toast.makeText(this, String.valueOf(exception.toString()),Toast.LENGTH_LONG).show());
        }
    }

    /**
     * Retrieves the title and content of a file identified by {@code fileId} and populates the UI.
     */
    private void readFile(String fileId) {

            if(flagFolder) {
                mId += mDriveServiceHelper.getmIdFolderCurrent() + " ";
                mIDMessagioIniziale++;

            }
            if(mIDMessagioIniziale==3)
            {
                mIdIntelocutore.setText(mId.substring(4));
                //mIdIntelocutore.setText(mId);

                mIDMessagioIniziale=0;
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

    private void readFileNULL(String fileId) {

        mDriveServiceHelper.bflagStartTimer=true;

    }

    private void readFileDrive(String fileId) {

        if (mDriveServiceHelper != null) {
            Log.d(TAG, "Reading file " + fileId);

            mDriveServiceHelper.readFile(fileId)
                    .addOnSuccessListener(nameAndContent -> {
                        String ContentFile = nameAndContent.second;

                        ChatMessage chatMessage = new ChatMessage();
                        chatMessage.setId(122);//dummy
                        chatMessage.setMessage(ContentFile);
                        chatMessage.setDate(DateFormat.getDateTimeInstance().format(new Date()));
                        chatMessage.setMe(false);

                        if(ContentFile.isEmpty() == false) {
                            displayMessage(chatMessage);
                            DeleteFile(fileId);
                        }

                    })
                    .addOnFailureListener(exception ->
                            Log.e(TAG, "Couldn't read file.", exception));
        }
    }

    public void queryCount(String [] IdFolders) {
        if (mDriveServiceHelper != null) {
            Log.d(TAG, "Querying for files.");
           // bflagStartTimer=false;

            mDriveServiceHelper.setmIdFolderCurrent(IdFolders[1]);
            mDriveServiceHelper.queryFilesCount()
            .addOnSuccessListener(fileList -> {

                List<File> lstFile = fileList.getFiles();
                mIntCountFile=  lstFile.size();
                bFlagFileExist=false;
                if(mIntCountFile < 2) {

                   for (File file : fileList.getFiles()) {
                        mNomeFileConnect = String.format("%s_C", getMyPhoneNumber());
                        if (file.getName().compareToIgnoreCase(mNomeFileConnect) == 0) {
                            bFlagFileExist = true;
                        }
                    }

                    if(!bFlagFileExist) {

                         createFileConnect(String.format("%s_C", getMyPhoneNumber()), "", IdFolders[2]);
                     }
                    else
                    {
                        mDriveServiceHelper.setmIdFolderCurrent(mDriveServiceHelper.mGlobalIdFolder);
                    }

                }
                else {
                    Toast.makeText(getBaseContext(),"ATTENZIONE impossibile entrare nella stanza",Toast.LENGTH_LONG);
                    mDriveServiceHelper.setmIdFolderCurrent(mDriveServiceHelper.mGlobalIdFolder);
                }
            })
            .addOnFailureListener(exception -> {
                Toast.makeText(this, String.valueOf(exception.toString()),Toast.LENGTH_LONG).show();
            });
        }
    }

    /**
     * Queries the Drive REST API for files visible to this app and lists them in the content view.
     */
    private void query() {
        if (mDriveServiceHelper != null) {
            Log.d(TAG, "Querying for files.");

            mDriveServiceHelper.queryFiles()
                    .addOnSuccessListener(fileList -> {
                        for (File file : fileList.getFiles()) {
                            if(file.getName().compareToIgnoreCase(mNomeFile) != 0 ) {
                                readFileDrive(file.getId());
                            }
                        }

                    })
                    .addOnFailureListener(exception -> {
                        Toast.makeText(this, String.valueOf(exception.toString()),Toast.LENGTH_LONG).show();
                    });
            }
    }

    public void DeleteFile(String FileId) {
        mDriveServiceHelper.DeleteFile(FileId)
           .addOnFailureListener(exception ->
                Log.e(TAG, "Unable to save file via REST.", exception));

    }

    /**
     * Updates the UI to read-only mode.
     */
    private void setReadOnlyMode() {
        mFileTitleEditText.setEnabled(false);
        mDocContentEditText.setEnabled(false);
        mOpenFileId = null;
    }

    /**
     * Updates the UI to read/write mode on the document identified by {@code fileId}.
     */
    private void setReadWriteMode(String fileId) {
        mFileTitleEditText.setEnabled(true);
        mDocContentEditText.setEnabled(true);
        mOpenFileId = fileId;
    }

    @Override
    public void onPause() {
        super.onPause();

    }

   @Override

   public void onDestroy() {
       super.onDestroy();

       String[] IdFolders = mId.split(" ");

       DeleteFile(IdFolders[1]);
       DeleteFile(IdFolders[2]);

       Toast.makeText(getBaseContext(),"Chiusura Chat, chiudere e riaprire.",Toast.LENGTH_LONG);

   }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_chat, menu);
        return true;
    }


public LocationListener locationListener;

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            mId = mIdIntelocutore.getText().toString();
            String[] IdFolders = mId.split(" ");

            mDriveServiceHelper.bflagStartTimer= false;
            queryCount(IdFolders);

            return true;
        }

        if (id == R.id.action_gps) {

         /*   locationManager  = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
            locationListener = new LocationListener() {

                @Override
                public void onLocationChanged(Location location) {

                    updateLocation(location);

                    //Toast.makeText(getBaseContext(),mAddress,Toast.LENGTH_LONG);

                    mIdIntelocutore.setText(mAddress);

                }
                @Override public void onStatusChanged(String provider, int status, Bundle extras) {

                }
                @Override
                public void onProviderEnabled(String provider) {
                }
                @Override
                public void onProviderDisabled(String provider) {
                }

            };

            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){

                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);

                Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

                updateLocation(lastKnownLocation);

            }else {

                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            }*/


            return true;
        }



        return super.onOptionsItemSelected(item);
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
                createFile(mNomeFile,messageText);
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

    private void loadDummyHistory(){

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

        for(int i=0; i<chatHistory.size(); i++) {
            ChatMessage message = chatHistory.get(i);
            displayMessage(message);
        }

    }

  /*  @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){

            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){

                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
            }
        }
    }

    public String mAddress;
*/

  /*  public void updateLocation ( Location location){

        Geocoder geocoder = new Geocoder(getApplicationContext(), Locale.getDefault());
        try {
            List<Address> listAddresses = geocoder.getFromLocation(location.getLatitude(),location.getLongitude(),1);

            String address = "Could not find location :(";

            if (listAddresses != null && listAddresses.size() > 0) {

                if (listAddresses.get(0).getThoroughfare() != null) {

                    address = listAddresses.get(0).getThoroughfare() + " ";
                }

                if (listAddresses.get(0).getLocality() != null) {

                    address += listAddresses.get(0).getLocality() + " ";
                }

                if (listAddresses.get(0).getPostalCode() != null) {

                    address += listAddresses.get(0).getPostalCode() + " ";
                }

                if (listAddresses.get(0).getAdminArea() != null) {

                    address += listAddresses.get(0).getAdminArea();
                }
            }

            mAddress = address;

        } catch (Exception e) {

            e.printStackTrace();
        }
    }*/

}
