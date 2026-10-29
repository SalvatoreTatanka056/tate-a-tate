package com.chatlayoutexample;

import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.util.Pair;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.api.client.http.ByteArrayContent;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Utility per eseguire operazioni asincrone su Google Drive tramite REST API v3.
 */
public class DriveServiceHelper {
    private final ExecutorService mExecutor = Executors.newSingleThreadExecutor();
    private final Drive mDriveService;

    public String mIdFileCurrent;
    private String mIdFolderCurrent;
    public String mGlobalIdFolder;
    public volatile boolean bflagStartTimer = true;

    public DriveServiceHelper(Drive driveService) {
        this.mDriveService = driveService;
    }

    public synchronized String getmIdFolderCurrent() {
        return mIdFolderCurrent;
    }

    public synchronized void setmIdFolderCurrent(String str) {
        this.mIdFolderCurrent = str;
    }

    /**
     * Crea un file di testo nella cartella Drive corrente.
     */
    public Task<String> createFile(String nomeFile, String message) {
        return Tasks.call(mExecutor, () -> {
            String parentFolder = getmIdFolderCurrent();
            File metadata = new File()
                    .setName(nomeFile)
                    .setMimeType("text/plain");

            if (parentFolder != null && !parentFolder.isEmpty()) {
                metadata.setParents(Collections.singletonList(parentFolder));
            }

            File googleFile = mDriveService.files().create(metadata).execute();
            if (googleFile == null) {
                throw new IOException("Creazione file fallita su Google Drive.");
            }

            mIdFileCurrent = googleFile.getId();
            if (message != null && !message.isEmpty()) {
                saveFile(nomeFile, message, mIdFileCurrent);
            }
            return googleFile.getId();
        });
    }

    /**
     * Crea un file di connessione alla stanza.
     */
    public Task<String> createFileConnect(String nomeFile, String message, String folderId) {
        return Tasks.call(mExecutor, () -> {
            String parentFolder = getmIdFolderCurrent();
            File metadata = new File()
                    .setName(nomeFile)
                    .setMimeType("text/plain");

            if (parentFolder != null && !parentFolder.isEmpty()) {
                metadata.setParents(Collections.singletonList(parentFolder));
            }

            File googleFile = mDriveService.files().create(metadata).execute();
            if (googleFile == null) {
                throw new IOException("Creazione file di connessione fallita.");
            }

            mIdFileCurrent = getmIdFolderCurrent();
            saveFileConnect(nomeFile, message, mIdFileCurrent, folderId);
            bflagStartTimer = true;

            return googleFile.getId();
        });
    }

    /**
     * Crea una cartella in Google Drive.
     */
    public Task<String> createFolder(String nomeFolder, String tipo) {
        return Tasks.call(mExecutor, () -> {
            File fileMetadata = new File()
                    .setName(nomeFolder)
                    .setParents(Collections.singletonList("root"))
                    .setMimeType("application/vnd.google-apps.folder");

            File googleFile = mDriveService.files().create(fileMetadata)
                    .setFields("id")
                    .execute();

            if (googleFile == null || googleFile.getId() == null) {
                throw new IOException("Creazione cartella fallita.");
            }

            setmIdFolderCurrent(googleFile.getId());

            if ("C".equalsIgnoreCase(tipo)) {
                mGlobalIdFolder = googleFile.getId();
            }

            return googleFile.getId();
        });
    }

    /**
     * Trova una cartella esistente per nome o la crea se non esiste.
     */
    public Task<String> findOrCreateFolder(String folderName, String tipo) {
        return Tasks.call(mExecutor, () -> {
            String query = "name = '" + folderName + "' and mimeType = 'application/vnd.google-apps.folder' and trashed = false";
            FileList result = mDriveService.files().list()
                    .setQ(query)
                    .setSpaces("drive")
                    .setFields("files(id, name)")
                    .execute();

            if (result != null && result.getFiles() != null && !result.getFiles().isEmpty()) {
                String folderId = result.getFiles().get(0).getId();
                setmIdFolderCurrent(folderId);
                if ("C".equalsIgnoreCase(tipo)) {
                    mGlobalIdFolder = folderId;
                }
                return folderId;
            } else {
                File fileMetadata = new File()
                        .setName(folderName)
                        .setParents(Collections.singletonList("root"))
                        .setMimeType("application/vnd.google-apps.folder");

                File googleFile = mDriveService.files().create(fileMetadata)
                        .setFields("id")
                        .execute();

                if (googleFile == null || googleFile.getId() == null) {
                    throw new IOException("Creazione cartella fallita.");
                }

                setmIdFolderCurrent(googleFile.getId());
                if ("C".equalsIgnoreCase(tipo)) {
                    mGlobalIdFolder = googleFile.getId();
                }
                return googleFile.getId();
            }
        });
    }

    /**
     * Elimina un file o cartella identificato da FileId.
     */
    public Task<Void> DeleteFile(String fileId) {
        return Tasks.call(mExecutor, () -> {
            if (fileId != null && !fileId.trim().isEmpty()) {
                try {
                    mDriveService.files().delete(fileId).execute();
                } catch (IOException ignored) {
                    // Ignora se il file è già stato rimosso o consumato
                }
            }
            return null;
        });
    }

    /**
     * Legge il contenuto testuale di un file Drive.
     */
    public Task<Pair<String, String>> readFile(String fileId) {
        return Tasks.call(mExecutor, () -> {
            File metadata = mDriveService.files().get(fileId).execute();
            String name = metadata != null ? metadata.getName() : "";

            try (InputStream is = mDriveService.files().get(fileId).executeMediaAsInputStream();
                 BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
                StringBuilder stringBuilder = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    stringBuilder.append(line);
                }
                return Pair.create(name, stringBuilder.toString());
            }
        });
    }

    /**
     * Aggiorna il contenuto di un file Drive.
     */
    public Task<Void> saveFile(String name, String content, String idFile) {
        return Tasks.call(mExecutor, () -> {
            File metadata = new File().setName(name);
            ByteArrayContent contentStream = ByteArrayContent.fromString("text/plain", content != null ? content : "");
            mDriveService.files().update(idFile, metadata, contentStream).execute();
            return null;
        });
    }

    public Task<Void> saveFileConnect(String name, String content, String idFile, String folderId) {
        return Tasks.call(mExecutor, () -> {
            File metadata = new File().setName(name);
            ByteArrayContent contentStream = ByteArrayContent.fromString("text/plain", content != null ? content : "");
            mDriveService.files().update(idFile, metadata, contentStream).execute();
            setmIdFolderCurrent(folderId);
            return null;
        });
    }

    /**
     * Esegue la query per ottenere i file nella cartella corrente.
     */
    public Task<FileList> queryFiles() {
        return Tasks.call(mExecutor, () -> {
            String folder = getmIdFolderCurrent();
            if (folder == null || folder.isEmpty()) {
                return new FileList();
            }
            String fileQuery = "'" + folder + "' in parents and trashed=false";
            return mDriveService.files().list()
                    .setQ(fileQuery)
                    .setFields("files(id, name, createdTime)")
                    .execute();
        });
    }

    public Task<FileList> queryFilesCount() {
        return Tasks.call(mExecutor, () -> {
            String folder = getmIdFolderCurrent();
            if (folder == null || folder.isEmpty()) {
                return new FileList();
            }
            String fileQuery = "'" + folder + "' in parents and trashed=false";
            return mDriveService.files().list()
                    .setQ(fileQuery)
                    .setFields("files(id, name)")
                    .execute();
        });
    }

    public Intent createFilePickerIntent() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/plain");
        return intent;
    }

    public Task<Pair<String, String>> openFileUsingStorageAccessFramework(
            ContentResolver contentResolver, Uri uri) {
        return Tasks.call(mExecutor, () -> {
            String name = "";
            try (Cursor cursor = contentResolver.query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (nameIndex >= 0) {
                        name = cursor.getString(nameIndex);
                    }
                }
            }

            try (InputStream is = contentResolver.openInputStream(uri);
                 BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
                StringBuilder stringBuilder = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    stringBuilder.append(line);
                }
                return Pair.create(name, stringBuilder.toString());
            }
        });
    }

    /**
     * Rilascia le risorse dell'executor.
     */
    public void shutdown() {
        mExecutor.shutdown();
    }
}
