package com.chatlayoutexample;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.NetworkRequest;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.Scope;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ChatActivity extends AppCompatActivity {

    private static final String TAG = "ChatActivity";

    // UI Controls
    private Toolbar toolbar;
    private TextView txtStatus;
    private ProgressBar mPrgMain;
    private LinearLayout bannerNoConnection;
    private EditText mIdIntelocutore;
    private ImageButton mBtnPlay;
    private ImageButton mBtnIncolla;
    private ImageButton mBtnWhatsApp;
    private MaterialButton mBtnCollega;
    private RecyclerView messagesContainer;
    private LinearLayout emptyStateLayout;
    private EditText messageET;
    private FloatingActionButton sendBtn;

    // Chat Data & Adapter
    private ChatAdapter adapter;
    private LinearLayoutManager layoutManager;
    private final ArrayList<ChatMessage> chatHistory = new ArrayList<>();

    // Drive & Session State
    private DriveServiceHelper mDriveServiceHelper;
    private String mId = "";
    private String mNomeFile = "";
    private String mNomeFileConnect = "";
    private int mIntCountFile = 0;
    private boolean bFlagFileExist = false;

    // Network Monitoring
    private ConnectivityManager.NetworkCallback networkCallback;
    private boolean isNetworkConnected = true;

    // Polling Handler & Runnable
    private final Handler pollHandler = new Handler(Looper.getMainLooper());
    private boolean isPollingActive = false;
    private final Runnable pollRunnable = new Runnable() {
        @Override
        public void run() {
            if (isPollingActive && isNetworkConnected && mDriveServiceHelper != null && mDriveServiceHelper.bflagStartTimer) {
                query();
            }
            if (isPollingActive) {
                pollHandler.postDelayed(this, 3000);
            }
        }
    };

    // Activity Result Launcher
    private final ActivityResultLauncher<Intent> signInLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    handleSignInResult(result.getData());
                } else {
                    updateStatus("Accesso Google annullato.", false);
                    mPrgMain.setVisibility(View.GONE);
                    Toast.makeText(this, "Accesso a Google Drive necessario per la chat.", Toast.LENGTH_LONG).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        initViews();
        setupRecyclerView();
        setupListeners();
        registerNetworkMonitoring();

        // Controllo immediato connettività di rete
        if (!isNetworkAvailable()) {
            setOfflineState(true);
        } else {
            updateStatus("Connessione a Google Drive…", true);
            requestSignIn();
        }
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        txtStatus = findViewById(R.id.txtStatus);
        mPrgMain = findViewById(R.id.prgMain);
        bannerNoConnection = findViewById(R.id.bannerNoConnection);
        mIdIntelocutore = findViewById(R.id.editTextTextMultiLine);
        mBtnPlay = findViewById(R.id.btnPlay);
        mBtnIncolla = findViewById(R.id.btnIncolla);
        mBtnWhatsApp = findViewById(R.id.btnWhatsApp);
        mBtnCollega = findViewById(R.id.BtnCollega);
        messagesContainer = findViewById(R.id.messagesContainer);
        emptyStateLayout = findViewById(R.id.emptyStateLayout);
        messageET = findViewById(R.id.messageEdit);
        sendBtn = findViewById(R.id.chatSendButton);

        messageET.setEnabled(false);
        sendBtn.setEnabled(false);
        mBtnPlay.setEnabled(false);
        mBtnWhatsApp.setEnabled(false);
    }

    private void setupRecyclerView() {
        layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        messagesContainer.setLayoutManager(layoutManager);

        adapter = new ChatAdapter(this, chatHistory);
        adapter.setOnDataChangedListener(this::updateEmptyState);
        messagesContainer.setAdapter(adapter);

        updateEmptyState();
    }

    private void updateEmptyState() {
        if (emptyStateLayout != null) {
            emptyStateLayout.setVisibility(adapter.getItemCount() == 0 ? View.VISIBLE : View.GONE);
        }
    }

    private void setupListeners() {
        mBtnPlay.setOnClickListener(v -> {
            String idToCopy = mIdIntelocutore.getText().toString().trim();
            if (!TextUtils.isEmpty(idToCopy)) {
                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                String shareText = "💬 Unisciti alla mia chat privata Tête-à-Tête!\nID Stanza: " + idToCopy + "\nCopia questo messaggio e incollalo nell'app per connetterti.";
                ClipData clip = ClipData.newPlainText("ID_STANZA_TETE", shareText);
                if (clipboard != null) {
                    clipboard.setPrimaryClip(clip);
                    Toast.makeText(this, R.string.toast_id_copied, Toast.LENGTH_SHORT).show();
                }
            }
        });

        mBtnIncolla.setOnClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            if (clipboard != null && clipboard.hasPrimaryClip() && clipboard.getPrimaryClip() != null) {
                CharSequence text = clipboard.getPrimaryClip().getItemAt(0).getText();
                if (text != null) {
                    String extractedId = extractIdFromText(text.toString());
                    mIdIntelocutore.setText(extractedId);
                    Toast.makeText(this, R.string.toast_id_pasted, Toast.LENGTH_SHORT).show();
                }
            }
        });

        mBtnWhatsApp.setOnClickListener(v -> {
            String idToCopy = mIdIntelocutore.getText().toString().trim();
            if (!TextUtils.isEmpty(idToCopy)) {
                String shareText = "💬 Unisciti alla mia chat privata Tête-à-Tête!\nID Stanza: " + idToCopy + "\nCopia questo messaggio e incollalo nell'app per connetterti.";
                Intent whatsappIntent = new Intent(Intent.ACTION_SEND);
                whatsappIntent.setType("text/plain");
                whatsappIntent.putExtra(Intent.EXTRA_TEXT, shareText);
                whatsappIntent.setPackage("com.whatsapp");
                try {
                    startActivity(whatsappIntent);
                } catch (android.content.ActivityNotFoundException ex) {
                    Intent shareIntent = new Intent(Intent.ACTION_SEND);
                    shareIntent.setType("text/plain");
                    shareIntent.putExtra(Intent.EXTRA_TEXT, shareText);
                    startActivity(Intent.createChooser(shareIntent, "Condividi ID Stanza"));
                }
            } else {
                Toast.makeText(this, "ID stanza non ancora disponibile.", Toast.LENGTH_SHORT).show();
            }
        });

        mBtnCollega.setOnClickListener(v -> connectToRoom());
        sendBtn.setOnClickListener(v -> sendMessage());
    }

    private String extractIdFromText(String text) {
        if (TextUtils.isEmpty(text)) return "";
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("([a-zA-Z0-9_\\-]{15,})\\s+([a-zA-Z0-9_\\-]{15,})");
        java.util.regex.Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return matcher.group(1) + " " + matcher.group(2);
        }
        return text.trim();
    }

    // --- Controllo Connessione di Rete ---

    public boolean isNetworkAvailable() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Network network = cm.getActiveNetwork();
            if (network == null) return false;
            NetworkCapabilities capabilities = cm.getNetworkCapabilities(network);
            return capabilities != null && (
                    capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                    (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                     capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                     capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)));
        } else {
            NetworkInfo activeNetworkInfo = cm.getActiveNetworkInfo();
            return activeNetworkInfo != null && activeNetworkInfo.isConnected();
        }
    }

    private void registerNetworkMonitoring() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return;

        networkCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(@NonNull Network network) {
                runOnUiThread(() -> {
                    if (!isNetworkConnected) {
                        isNetworkConnected = true;
                        setOfflineState(false);
                        Snackbar.make(messagesContainer, R.string.connection_restored_msg, Snackbar.LENGTH_SHORT).show();

                        if (mDriveServiceHelper == null) {
                            requestSignIn();
                        } else {
                            startPolling();
                        }
                    }
                });
            }

            @Override
            public void onLost(@NonNull Network network) {
                runOnUiThread(() -> {
                    isNetworkConnected = false;
                    setOfflineState(true);
                    Snackbar.make(messagesContainer, R.string.no_connection_msg, Snackbar.LENGTH_LONG).show();
                    stopPolling();
                });
            }
        };

        try {
            NetworkRequest request = new NetworkRequest.Builder()
                    .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    .build();
            cm.registerNetworkCallback(request, networkCallback);
        } catch (Exception e) {
            Log.w(TAG, "Impossibile registrare NetworkCallback: " + e.getMessage());
        }
    }

    private void unregisterNetworkMonitoring() {
        if (networkCallback != null) {
            ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm != null) {
                try {
                    cm.unregisterNetworkCallback(networkCallback);
                } catch (Exception ignored) {}
            }
        }
    }

    private void setOfflineState(boolean isOffline) {
        if (bannerNoConnection != null) {
            bannerNoConnection.setVisibility(isOffline ? View.VISIBLE : View.GONE);
        }
        if (isOffline) {
            updateStatus(getString(R.string.no_connection_msg), false);
        } else {
            updateStatus(getString(R.string.status_ready), false);
        }
    }

    private void updateStatus(String status, boolean isLoading) {
        runOnUiThread(() -> {
            if (txtStatus != null) {
                txtStatus.setText(status);
            }
            if (mPrgMain != null) {
                mPrgMain.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            }
        });
    }

    private String getDeviceIdentifier() {
        return Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
    }

    private void requestSignIn() {
        if (!isNetworkAvailable()) {
            setOfflineState(true);
            return;
        }

        GoogleSignInOptions signInOptions =
                new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .requestEmail()
                        .requestScopes(new Scope(DriveScopes.DRIVE_FILE))
                        .build();

        GoogleSignInClient client = GoogleSignIn.getClient(this, signInOptions);
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(this);

        if (account != null && GoogleSignIn.hasPermissions(account, new Scope(DriveScopes.DRIVE_FILE))) {
            setupDriveService(account);
        } else {
            signInLauncher.launch(client.getSignInIntent());
        }
    }

    private void handleSignInResult(Intent resultData) {
        GoogleSignIn.getSignedInAccountFromIntent(resultData)
                .addOnSuccessListener(this::setupDriveService)
                .addOnFailureListener(exception -> {
                    Log.e(TAG, "Accesso a Google fallito", exception);
                    updateStatus("Autenticazione fallita.", false);
                    Toast.makeText(this, "Errore di accesso: " + exception.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void setupDriveService(GoogleSignInAccount googleAccount) {
        Log.d(TAG, "Accesso eseguito come: " + googleAccount.getEmail());
        updateStatus("Inizializzazione stanze Drive…", true);

        GoogleAccountCredential credential =
                GoogleAccountCredential.usingOAuth2(
                        this, Collections.singleton(DriveScopes.DRIVE_FILE));
        credential.setSelectedAccount(googleAccount.getAccount());

        Drive googleDriveService = new Drive.Builder(
                new NetHttpTransport(),
                new GsonFactory(),
                credential)
                .setApplicationName("TeteATete Chat")
                .build();

        mDriveServiceHelper = new DriveServiceHelper(googleDriveService);

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.ITALY);
        String time = sdf.format(new Date());

        String folder = String.format("CHAT_%s", time);
        String folderC = String.format("CHAT_CONNECT_%s", time);

        mDriveServiceHelper.createFolder(folderC, "F")
                .continueWithTask(task -> {
                    String folderCId = task.getResult();
                    mId = (folderCId != null ? folderCId : "") + " ";
                    return mDriveServiceHelper.createFolder(folder, "C");
                })
                .addOnSuccessListener(folderId -> {
                    mId += (folderId != null ? folderId : "");
                    String cleanId = mId.trim();
                    if (cleanId.startsWith("null")) {
                        cleanId = cleanId.substring(4).trim();
                    }
                    mIdIntelocutore.setText(cleanId);
                    updateStatus(getString(R.string.status_ready), false);

                    mDriveServiceHelper.setmIdFolderCurrent(folderId);
                    mNomeFile = String.format("%s_C", getDeviceIdentifier());
                    createFile(mNomeFile, "");

                    mBtnPlay.setEnabled(true);
                    mBtnWhatsApp.setEnabled(true);
                    sendBtn.setEnabled(true);
                    messageET.setEnabled(true);

                    startPolling();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Errore inizializzazione stanze: " + (e != null ? e.getMessage() : "sconosciuto"), e);
                    String errorMsg = e != null ? e.getMessage() : "Errore sconosciuto";
                    updateStatus("Errore: " + errorMsg, false);
                    if (!isNetworkAvailable()) {
                        setOfflineState(true);
                    } else {
                        Toast.makeText(this, "Errore Drive: " + errorMsg, Toast.LENGTH_LONG).show();
                    }

                    if (e instanceof com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException) {
                        try {
                            startActivityForResult(
                                    ((com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException) e).getIntent(),
                                    1001
                            );
                        } catch (Exception ex) {
                            Log.e(TAG, "Impossibile avviare intent di recupero autorizzazione", ex);
                        }
                    }
                });
    }

    private void startPolling() {
        if (!isPollingActive && isNetworkConnected) {
            isPollingActive = true;
            pollHandler.postDelayed(pollRunnable, 2000);
        }
    }

    private void stopPolling() {
        isPollingActive = false;
        pollHandler.removeCallbacks(pollRunnable);
    }

    private void createFile(String nomeFile, String message) {
        if (mDriveServiceHelper != null) {
            mDriveServiceHelper.createFile(nomeFile, message)
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Errore creazione file: " + e.getMessage());
                        if (!isNetworkAvailable()) {
                            setOfflineState(true);
                        }
                    });
        }
    }

    private void createFileConnect(String nomeFile, String message, String folder) {
        if (mDriveServiceHelper != null) {
            mDriveServiceHelper.createFileConnect(nomeFile, message, folder)
                    .addOnSuccessListener(fileId -> {
                        if (mDriveServiceHelper != null) {
                            mDriveServiceHelper.bflagStartTimer = true;
                        }
                        updateStatus(getString(R.string.status_connected), false);
                        Toast.makeText(this, "Connessione alla stanza completata!", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        updateStatus("Errore connessione stanza", false);
                        if (!isNetworkAvailable()) {
                            setOfflineState(true);
                        } else {
                            Toast.makeText(this, "Errore: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
        }
    }

    private void connectToRoom() {
        if (!isNetworkAvailable()) {
            setOfflineState(true);
            Toast.makeText(this, R.string.no_connection_send_err, Toast.LENGTH_LONG).show();
            return;
        }

        String inputId = mIdIntelocutore.getText().toString().trim();
        if (TextUtils.isEmpty(inputId)) {
            Toast.makeText(this, "Inserisci o incolla l'ID della stanza prima di connettere.", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] idFolders = inputId.split(" ");
        if (idFolders.length < 2) {
            Toast.makeText(this, "ID stanza non valido.", Toast.LENGTH_SHORT).show();
            return;
        }

        updateStatus(getString(R.string.toast_connecting), true);
        if (mDriveServiceHelper != null) {
            mDriveServiceHelper.bflagStartTimer = false;
        }
        queryCount(idFolders);
    }

    public void queryCount(String[] idFolders) {
        if (mDriveServiceHelper == null) return;

        mDriveServiceHelper.setmIdFolderCurrent(idFolders[0]);
        mDriveServiceHelper.queryFilesCount()
                .addOnSuccessListener(fileList -> {
                    List<File> lstFile = fileList.getFiles();
                    mIntCountFile = (lstFile != null) ? lstFile.size() : 0;
                    bFlagFileExist = false;

                    if (mIntCountFile < 2) {
                        mNomeFileConnect = String.format("%s_C", getDeviceIdentifier());
                        if (lstFile != null) {
                            for (File file : lstFile) {
                                if (mNomeFileConnect.equalsIgnoreCase(file.getName())) {
                                    bFlagFileExist = true;
                                    break;
                                }
                            }
                        }

                        if (!bFlagFileExist && idFolders.length > 1) {
                            createFileConnect(mNomeFileConnect, "", idFolders[1]);
                        } else {
                            mDriveServiceHelper.setmIdFolderCurrent(mDriveServiceHelper.mGlobalIdFolder);
                            updateStatus(getString(R.string.status_connected), false);
                        }
                    } else {
                        Toast.makeText(this, R.string.toast_room_full, Toast.LENGTH_LONG).show();
                        mDriveServiceHelper.setmIdFolderCurrent(mDriveServiceHelper.mGlobalIdFolder);
                        updateStatus(getString(R.string.status_ready), false);
                    }
                })
                .addOnFailureListener(exception -> {
                    updateStatus("Errore verifica stanza", false);
                    if (!isNetworkAvailable()) {
                        setOfflineState(true);
                    } else {
                        Toast.makeText(this, "Errore: " + exception.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void query() {
        if (mDriveServiceHelper == null || !isNetworkAvailable()) return;

        mDriveServiceHelper.queryFiles()
                .addOnSuccessListener(fileList -> {
                    if (fileList != null && fileList.getFiles() != null) {
                        for (File file : fileList.getFiles()) {
                            if (!file.getName().equalsIgnoreCase(mNomeFile)) {
                                readFileDrive(file.getId());
                            }
                        }
                    }
                })
                .addOnFailureListener(exception -> {
                    Log.w(TAG, "Query file Drive in background fallita: " + exception.getMessage());
                    if (!isNetworkAvailable()) {
                        setOfflineState(true);
                    }
                });
    }

    private void readFileDrive(String fileId) {
        if (mDriveServiceHelper == null) return;

        mDriveServiceHelper.readFile(fileId)
                .addOnSuccessListener(nameAndContent -> {
                    String contentFile = nameAndContent.second;
                    if (!TextUtils.isEmpty(contentFile)) {
                        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
                        ChatMessage chatMessage = new ChatMessage(
                                System.currentTimeMillis(),
                                false,
                                contentFile,
                                timeFormat.format(new Date())
                        );

                        displayMessage(chatMessage);
                        removeDriveFile(fileId);
                    }
                })
                .addOnFailureListener(exception -> Log.e(TAG, "Impossibile leggere file: " + fileId, exception));
    }

    private void sendMessage() {
        // Controllo connettività prima dell'invio
        if (!isNetworkAvailable()) {
            setOfflineState(true);
            Snackbar.make(messagesContainer, R.string.no_connection_send_err, Snackbar.LENGTH_LONG).show();
            return;
        }

        String messageText = messageET.getText().toString().trim();
        if (TextUtils.isEmpty(messageText)) {
            return;
        }

        mNomeFile = String.format("%s_A", getDeviceIdentifier());
        createFile(mNomeFile, messageText);

        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
        ChatMessage chatMessage = new ChatMessage(
                System.currentTimeMillis(),
                true,
                messageText,
                timeFormat.format(new Date())
        );

        messageET.setText("");
        displayMessage(chatMessage);
    }

    public void displayMessage(ChatMessage message) {
        runOnUiThread(() -> {
            adapter.add(message);
            messagesContainer.scrollToPosition(adapter.getItemCount() - 1);
        });
    }

    public void removeDriveFile(String fileId) {
        if (mDriveServiceHelper != null && isNetworkAvailable()) {
            mDriveServiceHelper.DeleteFile(fileId);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (isNetworkAvailable()) {
            setOfflineState(false);
            if (mDriveServiceHelper != null) {
                startPolling();
            }
        } else {
            setOfflineState(true);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopPolling();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopPolling();
        unregisterNetworkMonitoring();

        if (mDriveServiceHelper != null && !TextUtils.isEmpty(mId)) {
            String[] idFolders = mId.split(" ");
            for (String folderId : idFolders) {
                if (!TextUtils.isEmpty(folderId) && !"null".equalsIgnoreCase(folderId)) {
                    removeDriveFile(folderId);
                }
            }
            mDriveServiceHelper.shutdown();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_chat, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_settings) {
            connectToRoom();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
