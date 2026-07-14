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
import android.graphics.Bitmap;
import android.net.Uri;

import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Utility for writing a {@link Bitmap} to the app cache and invoking the
 * system share sheet via {@link FileProvider} so the file can be sent to
 * other applications (email, messengers, etc.) without requiring
 * {@code WRITE_EXTERNAL_STORAGE}.
 */
public final class ImageShareHelper {

    private ImageShareHelper() {
    }

    public static void share(Context context, Bitmap bitmap, String caption) {
        if (context == null || bitmap == null) {
            return;
        }
        File shareDir = new File(context.getCacheDir(), "shared");
        if (!shareDir.exists() && !shareDir.mkdirs()) {
            return;
        }
        File outFile = new File(shareDir, "cas_output_" + System.currentTimeMillis() + ".png");
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(outFile);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.flush();
        } catch (IOException ex) {
            closeQuietly(fos);
            return;
        }
        closeQuietly(fos);

        Uri uri;
        try {
            uri = FileProvider.getUriForFile(context,
                    context.getPackageName() + ".fileprovider", outFile);
        } catch (Exception ignored) {
            return;
        }

        Intent share = new Intent(Intent.ACTION_SEND);
        share.setType("image/png");
        share.putExtra(Intent.EXTRA_STREAM, uri);
        share.putExtra(Intent.EXTRA_TEXT, caption != null ? caption : "");
        share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try {
            context.startActivity(Intent.createChooser(share,
                    context.getString(R.string.share_image_title)));
        } catch (Exception ignored) {
        }
    }

    private static void closeQuietly(FileOutputStream fos) {
        if (fos != null) {
            try { fos.close(); } catch (IOException ignored) {}
        }
    }
}
