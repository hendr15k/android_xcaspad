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

import android.content.Context;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import org.kde.necessitas.mucephi.android_xcas.adapteroperations.HolderOperation;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.List;

/**
 * Persists the user's session history to a {@code .cas} text file so
 * they can share or back it up.
 *
 * Saves the session in app-specific external or internal storage.
 */
public class SaveSession {

    private static final String MIME_TYPE = "application/cas";
    private static final String FILE_EXTENSION = ".cas";
    private static final String LOG_TAG = "SaveSession";

    private SaveSession() {
    }

    /**
     * Persist the given operations to a {@code .cas} file in the app-specific
     * storage and surface the result via a Toast.
     *
     * @param context any context; only the application context is retained.
     * @param data    ordered list of operations to serialise.
     */
    public static void download(final Context context, final List<HolderOperation> data) {
        final Context appContext = context.getApplicationContext();

        try {
            File path = appContext.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
            if (path == null) {
                path = appContext.getFilesDir();
            }

            if (path == null) {
                Log.e(LOG_TAG, "Storage directory unavailable");
                Toast.makeText(appContext,
                        appContext.getString(R.string.save_session_failed),
                        Toast.LENGTH_LONG).show();
                return;
            }

            if (!path.exists() && !path.mkdirs()) {
                Log.e(LOG_TAG, "Could not create " + path);
                Toast.makeText(appContext,
                        appContext.getString(R.string.save_session_failed),
                        Toast.LENGTH_LONG).show();
                return;
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
            } finally {
                closeQuietly(writer);
                closeQuietly(fOut);
            }

            Toast.makeText(appContext,
                    appContext.getString(R.string.save_session_success, file.getName()),
                    Toast.LENGTH_LONG).show();

            Log.i(LOG_TAG, "Session saved as " + file.getName());
        } catch (Exception e) {
            Log.e(LOG_TAG, "Failed to save session", e);
            Toast.makeText(appContext,
                    appContext.getString(R.string.save_session_failed) + ": " + e.getMessage(),
                    Toast.LENGTH_LONG).show();
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
