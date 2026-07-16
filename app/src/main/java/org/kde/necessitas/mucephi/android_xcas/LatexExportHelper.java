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
 * Serialises the user's session as a complete LaTeX document and hands it off
 * to the system share sheet via {@link FileProvider}. The resulting {@code .tex}
 * file can be compiled directly with pdfLaTeX or opened in Overleaf/TeXstudio.
 */
public final class LatexExportHelper {

    private static final String MIME_TYPE = "application/x-tex";
    private static final String FILE_EXTENSION = ".tex";

    private LatexExportHelper() {
    }

    /**
     * Build a LaTeX document from the session and trigger the share sheet.
     * No-op if there is no data to share or the file cannot be written.
     */
    public static void share(Context context, List<HolderOperation> data) {
        if (context == null || data == null || data.isEmpty()) {
            return;
        }

        File shareDir = new File(context.getCacheDir(), "shared");
        if (!shareDir.exists() && !shareDir.mkdirs()) {
            return;
        }

        File outFile = new File(shareDir, "xcas_session_" + System.currentTimeMillis() + FILE_EXTENSION);
        FileOutputStream fos = null;
        OutputStreamWriter writer = null;
        try {
            fos = new FileOutputStream(outFile);
            writer = new OutputStreamWriter(fos, "UTF-8");
            writeLatexDocument(writer, data);
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
        share.putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.export_latex_title));
        share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try {
            context.startActivity(Intent.createChooser(share,
                    context.getString(R.string.export_latex_title)));
        } catch (Exception ignored) {
        }
    }

    private static void writeLatexDocument(OutputStreamWriter w,
                                           List<HolderOperation> data) throws IOException {
        w.append("\\documentclass[11pt,a4paper]{article}\n");
        w.append("\\usepackage[utf8]{inputenc}\n");
        w.append("\\usepackage[T1]{fontenc}\n");
        w.append("\\usepackage{amsmath,amssymb}\n");
        w.append("\\usepackage{listings}\n");
        w.append("\\usepackage{xcolor}\n");
        w.append("\\usepackage{geometry}\n");
        w.append("\\geometry{margin=2.5cm}\n");
        w.append("\n");
        w.append("\\lstset{\n");
        w.append("  basicstyle=\\ttfamily\\small,\n");
        w.append("  backgroundcolor=\\color{gray!10},\n");
        w.append("  frame=single,\n");
        w.append("  breaklines=true,\n");
        w.append("  columns=fullflexible\n");
        w.append("}\n");
        w.append("\n");
        w.append("\\title{Xcas Pad Session}\n");
        w.append("\\date{\\today}\n");
        w.append("\\begin{document}\n");
        w.append("\\maketitle\n");

        int index = 1;
        for (HolderOperation op : data) {
            String input = op.getStrInput();
            String output = op.getStrOutput();
            if (isBlank(input) && isBlank(output)) {
                continue;
            }
            w.append("\\subsection*{Entry ").append(String.valueOf(index)).append("}\n");
            w.append("\n");
            w.append("\\textbf{Input:}\n");
            w.append("\\begin{lstlisting}\n");
            if (input != null) {
                w.append(input);
            }
            w.append("\n\\end{lstlisting}\n");
            w.append("\n");
            w.append("\\textbf{Output:}\n");
            w.append("\\begin{lstlisting}\n");
            if (output != null) {
                w.append(output);
            }
            w.append("\n\\end{lstlisting}\n");
            w.append("\n");
            index++;
        }

        w.append("\\end{document}\n");
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
