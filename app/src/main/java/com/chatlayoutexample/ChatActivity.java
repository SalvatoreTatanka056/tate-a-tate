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

/**
 * ChatActivity gestisce l'interfaccia principale della chat "Tête-à-Tête".
 * <p>
 * Questa Activity consente agli utenti di stabilire una connessione peer-to-peer privata
 * utilizzando Google Drive come backend di memorizzazione e sincronizzazione dei messaggi.
 * Gestisce l'autenticazione Google, la creazione e la connessione alle stanze di chat,
 * il monitoraggio della connettività di rete e il polling in tempo reale dei messaggi.
 * </p>
 */
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

    /**
     * Chiamato quando l'Activity viene creata. Inizializza la UI, la RecyclerView,
     * i listener degli eventi, il monitoraggio della rete e avvia il processo di autenticazione Google Drive.
     *
     * @param savedInstanceState Stato salvato precedente dell'Activity, se presente.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        initViews();
        setupRecyclerView();
        setupListeners();
        registerNetworkMonitoring();
        checkFirstRunOnboarding();

        // Controllo immediato connettività di rete
        if (!isNetworkAvailable()) {
            setOfflineState(true);
        } else {
            updateStatus("Connessione a Google Drive…", true);
            requestSignIn();
        }
    }

    /**
     * Controlla se è il primo avvio dell'applicazione per mostrare il tutorial di onboarding.
     */
    private void checkFirstRunOnboarding() {
        android.content.SharedPreferences prefs = getSharedPreferences("tate_prefs", MODE_PRIVATE);
        boolean isFirstRun = prefs.getBoolean("is_first_run", true);
        if (isFirstRun) {
            prefs.edit().putBoolean("is_first_run", false).apply();
            showOnboardingDialog();
        }
    }

    /**
     * Mostra il dialog di benvenuto e guida all'uso (Onboarding).
     */
    private void showOnboardingDialog() {
        new android.app.AlertDialog.Builder(this)
                .setTitle("👋 Benvenuto in Tête-à-Tête")
                .setMessage("Tête-à-Tête è la chat privata e sicura che utilizza Google Drive come stanza privata.\n\n" +
                        "1️⃣ **Crea la stanza**: L'app genera automaticamente la tua stanza sul cloud.\n" +
                        "2️⃣ **Invita l'interlocutore**: Inserisci il numero WhatsApp del tuo amico e inviagli l'ID stanza con un tap.\n" +
                        "3️⃣ **Chattate in libertà**: I messaggi sono sincronizzati in tempo reale in modo completamente privato!")
                .setPositiveButton("Inizia a chattare", (dialog, which) -> dialog.dismiss())
                .setCancelable(false)
                .show();
    }

    /**
     * Inizializza i componenti grafici dell'interfaccia utente (View binding manuale)
     * e disabilita inizialmente i controlli non ancora pronti.
     */
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

    /**
     * Configura la RecyclerView e l'adapter per la visualizzazione dei messaggi della chat.
     */
    private void setupRecyclerView() {
        layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        messagesContainer.setLayoutManager(layoutManager);

        adapter = new ChatAdapter(this, chatHistory);
        adapter.setOnDataChangedListener(this::updateEmptyState);
        messagesContainer.setAdapter(adapter);

        updateEmptyState();
    }

    /**
     * Aggiorna la visibilità dello stato vuoto (empty state) in base al numero di messaggi presenti.
     */
    private void updateEmptyState() {
        if (emptyStateLayout != null) {
            emptyStateLayout.setVisibility(adapter.getItemCount() == 0 ? View.VISIBLE : View.GONE);
        }
    }

    /**
     * Configura i listener di click per i pulsanti di interazione (copia ID, incolla ID, condivisione WhatsApp,
     * collegamento alla stanza e invio messaggi).
     */
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
            if (TextUtils.isEmpty(idToCopy)) {
                Toast.makeText(this, "ID stanza non ancora disponibile.", Toast.LENGTH_SHORT).show();
                return;
            }

            android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
            builder.setTitle("Invia ID Stanza su WhatsApp");
            builder.setMessage("Inserisci il numero di telefono del destinatario con prefisso (es. +39...):");

            final EditText input = new EditText(this);
            input.setInputType(android.text.InputType.TYPE_CLASS_PHONE);
            input.setHint("+393331234567");
            int padding = (int) (16 * getResources().getDisplayMetrics().density);

            LinearLayout container = new LinearLayout(this);
            container.setOrientation(LinearLayout.VERTICAL);
            container.setPadding(padding, 0, padding, 0);
            container.addView(input);
            builder.setView(container);

            builder.setPositiveButton("Invia al numero", (dialog, which) -> {
                String phone = input.getText().toString().trim();
                String shareText = "💬 Unisciti alla mia chat privata Tête-à-Tête!\nID Stanza: " + idToCopy + "\nCopia questo messaggio e incollalo nell'app per connetterti.";

                if (!TextUtils.isEmpty(phone)) {
                    String cleanedPhone = phone.replaceAll("[^0-9+]", "");
                    String url = "https://api.whatsapp.com/send?phone=" + cleanedPhone + "&text=" + android.net.Uri.encode(shareText);
                    try {
                        Intent intent = new Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url));
                        startActivity(intent);
                    } catch (Exception e) {
                        Toast.makeText(this, "Impossibile aprire WhatsApp: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                } else {
                    shareViaWhatsAppStandard(shareText);
                }
            });

            builder.setNeutralButton("Scegli contatto", (dialog, which) -> {
                String shareText = "💬 Unisciti alla mia chat privata Tête-à-Tête!\nID Stanza: " + idToCopy + "\nCopia questo messaggio e incollalo nell'app per connetterti.";
                shareViaWhatsAppStandard(shareText);
            });

            builder.setNegativeButton("Annulla", (dialog, which) -> dialog.cancel());
            builder.show();
        });

        mBtnCollega.setOnClickListener(v -> connectToRoom());
        sendBtn.setOnClickListener(v -> sendMessage());
    }

    /**
     * Estrae l'ID della stanza da un testo condiviso o incollato.
     *
     * @param text Testo sorgente da cui estrarre l'ID.
     * @return L'ID estratto o il testo originale formattato.
     */
    private String extractIdFromText(String text) {
        if (TextUtils.isEmpty(text)) return "";
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("([a-zA-Z0-9_\\-]{15,})\\s+([a-zA-Z0-9_\\-]{15,})");
        java.util.regex.Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return matcher.group(1) + " " + matcher.group(2);
        }
        return text.trim();
    }

    /**
     * Condivide l'ID della stanza tramite il selettore di condivisione standard o WhatsApp.
     *
     * @param shareText Testo da condividere.
     */
    private void shareViaWhatsAppStandard(String shareText) {
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
    }

    // --- Controllo Connessione di Rete ---

    /**
     * Verifica se è presente una connessione internet attiva sul dispositivo.
     *
     * @return true se la connessione è attiva e dispensa internet, false altrimenti.
     */
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

    /**
     * Registra un NetworkCallback per monitorare in tempo reale lo stato della connessione di rete.
     */
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

    /**
     * Annulla la registrazione del NetworkCallback per prevenire memory leak.
     */
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

    /**
     * Aggiorna lo stato visivo dell'interfaccia quando il dispositivo entra o esce dalla modalità offline.
     *
     * @param isOffline true se offline, false se online.
     */
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

    /**
     * Aggiorna il testo dello status bar e la visibilità della barra di progressione.
     *
     * @param status   Messaggio di stato da mostrare.
     * @param isLoading true per mostrare la ProgressBar, false per nasconderla.
     */
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

    /**
     * Restituisce un identificativo univoco del dispositivo basato su ANDROID_ID.
     *
     * @return ID univoco del dispositivo.
     */
    private String getDeviceIdentifier() {
        return Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
    }

    /**
     * Richiede l'autenticazione dell'utente tramite Google Sign-In con i permessi Google Drive richiesti.
     */
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

    /**
     * Gestisce il risultato dell'intento di autenticazione Google Sign-In.
     *
     * @param resultData Dati di ritorno dell'intent di login.
     */
    private void handleSignInResult(Intent resultData) {
        GoogleSignIn.getSignedInAccountFromIntent(resultData)
                .addOnSuccessListener(this::setupDriveService)
                .addOnFailureListener(exception -> {
                    Log.e(TAG, "Accesso a Google fallito", exception);
                    updateStatus("Autenticazione fallita.", false);
                    Toast.makeText(this, "Errore di accesso: " + exception.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    /**
     * Inizializza il servizio Google Drive e crea le cartelle dedicate alla stanza di chat sul cloud.
     *
     * @param googleAccount Account Google autenticato dell'utente.
     */
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

    /**
     * Avvia il timer di polling periodico per controllare la presenza di nuovi messaggi su Google Drive.
     */
    private void startPolling() {
        if (!isPollingActive && isNetworkConnected) {
            isPollingActive = true;
            pollHandler.postDelayed(pollRunnable, 2000);
        }
    }

    /**
     * Ferma il polling periodico dei messaggi.
     */
    private void stopPolling() {
        isPollingActive = false;
        pollHandler.removeCallbacks(pollRunnable);
    }

    /**
     * Crea un file di testo su Google Drive nella cartella corrente.
     *
     * @param nomeFile Nome del file da creare.
     * @param message  Contenuto testuale del file.
     */
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

    /**
     * Crea un file di connessione nella cartella remota dell'interlocutore per stabilire la connessione bidirezionale.
     *
     * @param nomeFile Nome del file di connessione.
     * @param message  Contenuto opzionale.
     * @param folder   ID della cartella remota dell'interlocutore.
     */
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

    /**
     * Tenta di connettersi a una stanza esistente utilizzando l'ID inserito dall'utente.
     */
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

    /**
     * Verifica il conteggio dei file nella cartella remota per validare la capienza della stanza (max 2 partecipanti).
     *
     * @param idFolders Array contenente gli ID delle cartelle della stanza.
     */
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

    /**
     * Esegue una query periodica su Google Drive per verificare la presenza di nuovi file/messaggi dai partecipanti.
     */
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

    /**
     * Legge il contenuto di un file Google Drive specifico e lo mostra come messaggio ricevuto.
     *
     * @param fileId ID del file su Google Drive.
     */
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

    /**
     * Invia un messaggio di chat creando un nuovo file su Google Drive e aggiornando la UI locale.
     */
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

    /**
     * Aggiunge un messaggio alla cronologia della chat e scorre la RecyclerView fino all'ultimo elemento.
     *
     * @param message Oggetto ChatMessage da visualizzare.
     */
    public void displayMessage(ChatMessage message) {
        runOnUiThread(() -> {
            adapter.add(message);
            messagesContainer.scrollToPosition(adapter.getItemCount() - 1);
        });
    }

    /**
     * Rimuove un file o una cartella da Google Drive.
     *
     * @param fileId ID del file da eliminare.
     */
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
