/*
 *  Copyright (C) 2011 Leonel Hernández Sandoval.
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 3 of the License, or
 *  (at your option) any later version.
 */

package org.kde.necessitas.mucephi.android_xcas;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import androidx.core.content.FileProvider;

import org.kde.necessitas.mucephi.android_xcas.adapteroperations.HolderOperation;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.List;

/**
 * Serialises the user's session as a Markdown document and hands it off to the
 * system share sheet via {@link FileProvider}. The resulting {@code .md} file
 * lives in the app cache so no {@code WRITE_EXTERNAL_STORAGE} permission is
 * needed and the share is revocable the moment the user installs/uninstalls.
 */
public final class SessionShareHelper {

    private static final String MIME_TYPE = "text/markdown";
    private static final String FILE_EXTENSION = ".md";

    private SessionShareHelper() {
    }

    /**
     * Build a Markdown representation of the session and trigger the share
     * sheet. No-op if there is no data to share or the file cannot be written.
     */
    public static void share(Context context, List<HolderOperation> data) {
        if (context == null || data == null || data.isEmpty()) {
            return;
        }

        File shareDir = new File(context.getCacheDir(), "shared");
        if (!shareDir.exists() && !shareDir.mkdirs()) {
            return;
        }

        File outFile = new File(shareDir, "session_" + System.currentTimeMillis() + FILE_EXTENSION);
        FileOutputStream fos = null;
        OutputStreamWriter writer = null;
        try {
            fos = new FileOutputStream(outFile);
            writer = new OutputStreamWriter(fos, "UTF-8");
            writeMarkdownBody(writer, data);
            writer.flush();
            fos.flush();
        } catch (IOException ex) {
            closeQuietly(writer);
            closeQuietly(fos);
            return;
        }
        closeQuietly(writer);
        closeQuietly(fos);

        Uri uri;
        try {
            uri = FileProvider.getUriForFile(context,
                    context.getPackageName() + ".fileprovider", outFile);
        } catch (Exception ignored) {
            return;
        }

        Intent share = new Intent(Intent.ACTION_SEND);
        share.setType(MIME_TYPE);
        share.putExtra(Intent.EXTRA_STREAM, uri);
        share.putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.share_session_title));
        share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try {
            context.startActivity(Intent.createChooser(share,
                    context.getString(R.string.share_session_title)));
        } catch (Exception ignored) {
        }
    }

    private static void writeMarkdownBody(OutputStreamWriter writer,
                                          List<HolderOperation> data) throws IOException {
        writer.append("# Xcas Pad Session");
        writer.append('\n');
        int index = 1;
        for (HolderOperation op : data) {
            String input = op.getStrInput();
            String output = op.getStrOutput();
            if (isBlank(input) && isBlank(output)) {
                continue;
            }
            writer.append("## Entry ").append(String.valueOf(index)).append('\n');
            writer.append("**Input**\n\n");
            writer.append("```cas\n");
            if (input != null) {
                writer.append(input);
            }
            writer.append("\n```\n\n");
            writer.append("**Output**\n\n");
            writer.append("```\n");
            if (output != null) {
                writer.append(output);
            }
            writer.append("\n```\n\n");
            index++;
        }
    }

    private static boolean isBlank(String value) {
        if (value == null) {
            return true;
        }
        for (int i = 0, n = value.length(); i < n; i++) {
            if (!Character.isWhitespace(value.charAt(i))) {
                return false;
            }
        }
        return true;
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
