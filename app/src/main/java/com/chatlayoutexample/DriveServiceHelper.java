package com.chatlayoutexample;

import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.util.Pair;
import android.widget.Toast;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.api.client.http.ByteArrayContent;
import com.google.api.client.http.FileContent;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.Collections;
import java.util.Date;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;


public class DriveServiceHelper {
    private final Executor mExecutor = Executors.newSingleThreadExecutor();
    private final Drive mDriveService;
    public String mIdFileCurrent;
    private String mIdFolderCurrent;
    private String mIdFolderCurrentConnect;
    public String mGlobalIdFolder;
    public boolean bflagStartTimer=true;
    public String mLinkAudio;
    public String mIdFileAudio;


    public DriveServiceHelper(Drive driveService) {
        mDriveService = driveService;
    }

    /**
     * Creates a text file in the user's My Drive folder and returns its file ID.
     */
    public Task<String> createFile(String NomeFile,String Message) {
        return Tasks.call(mExecutor, () -> {

            File metadata = new File()
                    .setParents(Collections.singletonList(getmIdFolderCurrent()))
                    .setMimeType("text/plain")
                    .setName(NomeFile);

            File googleFile = mDriveService.files().create(metadata).execute();
            if (googleFile == null) {
                throw new IOException("Null result when requesting file creation.");
            }
            mIdFileCurrent = googleFile.getId();
            saveFile(NomeFile,Message,mIdFileCurrent);

            return googleFile.getId();
        });
    }

    public Task<String>  uploadFile() {
        return Tasks.call(mExecutor, () -> {
            File fileMetadata = new File()
                    .setMimeType("audio/amr")
                    .setName("music.amr");

            java.io.File filePath = new java.io.File("/sdcard/Music/music.amr");
            FileContent mediaContent = new FileContent("audio/amr",filePath);

            File file = mDriveService.files().create(fileMetadata, mediaContent).setFields("id").execute();

            mLinkAudio= String.format("https://drive.google.com/file/d/%s/view?usp=sharing",file.getId());
            //mLinkAudio= String.format("drive.google.com/uc?export=download&id=%s",file.getId());
            mIdFileAudio = file.getId();

            return file.getId();
        });
    }


    boolean DownloadFileFromGoogleDrive(String sFileId, String sDestinationPath) {
        try {
            OutputStream oOutputStream = new FileOutputStream(sDestinationPath);
            mDriveService.files().get(sFileId).executeMediaAndDownloadTo(oOutputStream);
            oOutputStream.flush();
            oOutputStream.close();
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public Task<String> createFileConnect(String NomeFile,String Message,String FolderId) {
        return Tasks.call(mExecutor, () -> {
            File metadata = new File()
                    .setParents(Collections.singletonList(getmIdFolderCurrent()))
                    .setMimeType("text/plain")
                    .setName(NomeFile);

            File googleFile = mDriveService.files().create(metadata).execute();
            if (googleFile == null) {
                throw new IOException("Null result when requesting file creation.");
            }

            mIdFileCurrent = getmIdFolderCurrent();
            saveFileConnect(NomeFile,Message,mIdFileCurrent,FolderId);

            bflagStartTimer=true;

            return googleFile.getId();
        });
    }


    public Task<String> createFolder(String NomeFolder,String Tipo) {
        return Tasks.call(mExecutor, () -> {

            File fileMetadata = new File();
            fileMetadata.setName(NomeFolder);
            fileMetadata.setParents(Collections.singletonList("root"));
            fileMetadata.setMimeType("application/vnd.google-apps.folder");

            File  googleFile = mDriveService.files().create(fileMetadata)
                    .setFields("id")
                    .execute();
            /*File metadata = new File()
                    .setParents(Collections.singletonList("root"))
                    .setMimeType("application/vnd.google-apps.folder")
                    .setName(NomeFile);

            File googleFile = mDriveService.files().create(metadata).execute();*/
            if (googleFile == null) {
                throw new IOException("Null result when requesting file creation.");
            }

            setmIdFolderCurrent(googleFile.getId());
            googleFile.getId();

            if(Tipo.compareToIgnoreCase("C")==0)
            {
                mGlobalIdFolder = googleFile.getId();
            }

            return googleFile.getId();
        });
    }

    public String getmIdFolderCurrent() {
        return mIdFolderCurrent;
    }

    public void setmIdFolderCurrent(String str) {
        mIdFolderCurrent = str;

    }

    public Task<Void> DeleteFile(String FileId) {
        return Tasks.call(mExecutor, () ->{
             mDriveService.files().delete(FileId).execute();
            return null;
        });
    }
    /**
     * Opens the file identified by {@code fileId} and returns a {@link Pair} of its name and
     * contents.
     */
    public Task<Pair<String, String>> readFile(String fileId) {
        return Tasks.call(mExecutor, () -> {
            // Retrieve the metadata as a File object.
            File metadata = mDriveService.files().get(fileId).execute();
            String name = metadata.getName();

            // Stream the file contents to a String.
            try (InputStream is = mDriveService.files().get(fileId).executeMediaAsInputStream();
                 BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
                StringBuilder stringBuilder = new StringBuilder();
                String line;

                while ((line = reader.readLine()) != null) {
                    stringBuilder.append(line);
                }
                String contents = stringBuilder.toString();

                return Pair.create(name, contents);
            }
        });
    }

    /**
     * Updates the file identified by {@code fileId} with the given {@code name} and {@code
     * content}.
     */
    public Task<Void> saveFile( String name, String content,String IdFile) {
        return Tasks.call(mExecutor, () -> {
            // Create a File containing any metadata changes.
            File metadata = new File().setName(name);
            // Convert content to an AbstractInputStreamContent instance.
            ByteArrayContent contentStream = ByteArrayContent.fromString("text/plain", content);
            // Update the metadata and contents.
            mDriveService.files().update(IdFile, metadata, contentStream).execute();

            return null;
        });
    }

        public Task<Void> saveFileConnect( String name, String content,String IdFile,String FolderId) {
            return Tasks.call(mExecutor, () -> {
                // Create a File containing any metadata changes.
                File metadata = new File().setName(name);

                // Convert content to an AbstractInputStreamContent instance.
                ByteArrayContent contentStream = ByteArrayContent.fromString("text/plain", content);

                // Update the metadata and contents.
                mDriveService.files().update(IdFile, metadata, contentStream).execute();

                setmIdFolderCurrent(FolderId);
                return null;
            });
        }

    /**
     * Returns a {@link FileList} containing all the visible files in the user's My Drive.
     *
     * <p>The returned list will only contain files visible to this app, i.e. those which were
     * created by this app. To perform operations on files not created by the app, the project must
     * request Drive Full Scope in the <a href="https://play.google.com/apps/publish">Google
     * Developer's Console</a> and be submitted to Google for verification.</p>
     */
    public Task<FileList> queryFiles() {
        return Tasks.call(mExecutor, new Callable<FileList>() {
            @Override
            public FileList call() throws Exception {


                String fileQuery = "'" + getmIdFolderCurrent() + "' in parents and trashed=false";
                return  mDriveService.files().list().setQ( fileQuery ).execute();

               //return mDriveService.files().list().setSpaces("drive").execute();
               // return mDriveService.files().list().setSpaces("drive").execute();

            }
        });
    }

    public Task<FileList> queryFilesCount() {
        return Tasks.call(mExecutor, new Callable<FileList>() {
            @Override
            public FileList call() throws Exception {

                String fileQuery = "'" + getmIdFolderCurrent() + "' in parents and trashed=false";
                return  mDriveService.files().list().setQ( fileQuery ).execute();
            }
        });
    }

    /**
     * Returns an {@link Intent} for opening the Storage Access Framework file picker.
     */
    public Intent createFilePickerIntent() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/plain");

        return intent;
    }

    /**
     * Opens the file at the {@code uri} returned by a Storage Access Framework {@link Intent}
     * created by {@link #createFilePickerIntent()} using the given {@code contentResolver}.
     */
    public Task<Pair<String, String>> openFileUsingStorageAccessFramework(
            ContentResolver contentResolver, Uri uri) {
        return Tasks.call(mExecutor, () -> {
            // Retrieve the document's display name from its metadata.
            String name;
            try (Cursor cursor = contentResolver.query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    name = cursor.getString(nameIndex);
                } else {
                    throw new IOException("Empty cursor returned for file.");
                }
            }

            // Read the document's contents as a String.
            String content;
            try (InputStream is = contentResolver.openInputStream(uri);
                 BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
                StringBuilder stringBuilder = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    stringBuilder.append(line);
                }
                content = stringBuilder.toString();
            }

            return Pair.create(name, content);
        });
    }
}
