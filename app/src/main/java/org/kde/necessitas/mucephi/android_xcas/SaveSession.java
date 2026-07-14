/*
 *  Copyright (C) 2011 Leonel Hernández Sandoval.
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.kde.necessitas.mucephi.android_xcas;

import android.app.DownloadManager;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Toast;

import org.kde.necessitas.mucephi.android_xcas.adapteroperations.HolderOperation;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.List;

/**
 * Persists the user's session history to a {@code .cas} text file so
 * they can share or back it up.
 *
 * On Android 10+ (API 29) the write goes through
 * {@link MediaStore.Downloads#EXTERNAL_CONTENT_URI} which does not
 * require any runtime permission. Older Android versions fall back to
 * a direct {@link FileOutputStream} in the public Downloads directory
 * and then queue the file with the platform {@link DownloadManager} so
 * the system files app can display it.
 */
public class SaveSession {

    private static final String MIME_TYPE = "application/cas";
    private static final String FILE_EXTENSION = ".cas";
    private static final String LOG_TAG = "SaveSession";

    private SaveSession() {
    }

    /**
     * Persist the given operations to a {@code .cas} file in the public
     * Downloads collection and surface the result via a Toast.
     *
     * @param context any context; only the application context is retained.
     * @param data    ordered list of operations to serialise.
     */
    public static void download(final Context context, final List<HolderOperation> data) {
        final Context appContext = context.getApplicationContext();
        final String baseName = "session";

        try {
            Uri target;
            String displayName;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                target = writeViaMediaStore(appContext, baseName, data);
                displayName = baseName + FILE_EXTENSION;
            } else {
                File legacyFile = writeViaLegacyFile(appContext, data);
                if (legacyFile == null) {
                    Toast.makeText(appContext,
                            appContext.getString(R.string.save_session_failed),
                            Toast.LENGTH_LONG).show();
                    return;
                }
                target = legacyToDownloadManager(appContext, legacyFile);
                displayName = legacyFile.getName();
            }

            if (target == null) {
                Toast.makeText(appContext,
                        appContext.getString(R.string.save_session_failed),
                        Toast.LENGTH_LONG).show();
                return;
            }

            final String shownName = displayName;
            Toast.makeText(appContext,
                    appContext.getString(R.string.save_session_success, shownName),
                    Toast.LENGTH_LONG).show();

            Log.i(LOG_TAG, "Session saved as " + shownName);
            openDownloads(appContext);
        } catch (Exception e) {
            Log.e(LOG_TAG, "Failed to save session", e);
            Toast.makeText(appContext,
                    appContext.getString(R.string.save_session_failed) + ": " + e.getMessage(),
                    Toast.LENGTH_LONG).show();
        }
    }

    @androidx.annotation.RequiresApi(Build.VERSION_CODES.Q)
    private static Uri writeViaMediaStore(Context appContext, String baseName,
                                          List<HolderOperation> data) throws IOException {
        final ContentResolver resolver = appContext.getContentResolver();
        final ContentValues values = new ContentValues();
        values.put(MediaStore.Downloads.DISPLAY_NAME, baseName + FILE_EXTENSION);
        values.put(MediaStore.Downloads.MIME_TYPE, MIME_TYPE);
        values.put(MediaStore.Downloads.RELATIVE_PATH,
                Environment.DIRECTORY_DOWNLOADS);

        final Uri uri = resolver.insert(
                MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
        if (uri == null) {
            Log.e(LOG_TAG, "MediaStore.insert returned null");
            return null;
        }

        OutputStream os = null;
        OutputStreamWriter writer = null;
        try {
            os = resolver.openOutputStream(uri, "w");
            if (os == null) {
                return null;
            }
            writer = new OutputStreamWriter(os);
            writeCasBody(writer, data);
            writer.flush();
            return uri;
        } finally {
            closeQuietly(writer);
            closeQuietly(os);
        }
    }

    private static File writeViaLegacyFile(Context appContext,
                                           List<HolderOperation> data) throws IOException {
        final File path = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOWNLOADS);
        if (path == null) {
            Log.e(LOG_TAG, "External Downloads directory unavailable");
            return null;
        }
        if (!path.exists() && !path.mkdirs()) {
            Log.e(LOG_TAG, "Could not create " + path);
            return null;
        }

        File file = uniqueFile(path);
        FileOutputStream fOut = null;
        OutputStreamWriter writer = null;
        try {
            fOut = new FileOutputStream(file);
            writer = new OutputStreamWriter(fOut);
            writeCasBody(writer, data);
            writer.flush();
            fOut.flush();
            return file;
        } finally {
            closeQuietly(writer);
            closeQuietly(fOut);
        }
    }

    private static File uniqueFile(File path) {
        File file = new File(path, "session" + FILE_EXTENSION);
        int index = 0;
        while (file.exists()) {
            file = new File(path, "session(" + index + ")" + FILE_EXTENSION);
            index++;
        }
        return file;
    }

    private static void writeCasBody(OutputStreamWriter writer,
                                     List<HolderOperation> data) throws IOException {
        for (HolderOperation op : data) {
            writer.append(op.getStrInput());
            writer.append("\n");
            writer.append(op.getStrOutput());
            writer.append("\n");
        }
    }

    private static Uri legacyToDownloadManager(Context appContext, File file) {
        DownloadManager dm = (DownloadManager) appContext.getSystemService(
                Context.DOWNLOAD_SERVICE);
        if (dm == null) {
            return Uri.fromFile(file);
        }
        dm.addCompletedDownload(file.getName(), "Xcas Pad", true, MIME_TYPE,
                file.getAbsolutePath(), file.length(), true);
        return Uri.fromFile(file);
    }

    private static void openDownloads(Context appContext) {
        try {
            Intent view = new Intent(DownloadManager.ACTION_VIEW_DOWNLOADS);
            view.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            appContext.startActivity(view);
        } catch (Exception ignored) {
        }
    }

    private static void closeQuietly(java.io.Closeable closeable) {
        if (closeable == null) {
            return;
        }
        try {
            closeable.close();
        } catch (IOException ignored) {
        }
    }
}
