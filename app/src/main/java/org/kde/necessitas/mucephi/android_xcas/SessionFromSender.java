/*  Copyright (C) 2011 Leonel Hernández Sandoval.
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

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AlertDialog;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by leonel on 29/11/17.
 */
public class SessionFromSender {


    public interface OnLoadFromSender{
        void loadInBackground(List<String> lists);
        void onFinishLoading();
    }

    public static void load(final Context context, final OnLoadFromSender onLoadFromSender) {


        Intent intent = ((Activity) context).getIntent();
        String action = intent.getAction();
        String type = intent.getType();

        if (Intent.ACTION_VIEW.equals(action) && type != null) {
            if ("application/cas".equals(type)) {

                List<String> listOperations = loadFromIntent(context);

                final AlertDialog progress = new AlertDialog.Builder(context)
                        .setMessage("Loading session...")
                        .setCancelable(false)
                        .create();
                progress.show();

                ExecutorService executor = Executors.newSingleThreadExecutor();
                final Handler mainHandler = new Handler(Looper.getMainLooper());
                executor.execute(new Runnable() {
                    @Override
                    public void run() {
                        onLoadFromSender.loadInBackground(listOperations);
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                onLoadFromSender.onFinishLoading();
                                try {
                                    progress.dismiss();
                                } catch (Exception ignored) {
                                }
                            }
                        });
                        executor.shutdown();
                    }
                });
            }
        }
    }

    private static List<String> loadFromIntent(Context context) {

        BufferedReader br = null;
        List<String> list = new ArrayList<String>();

        Uri data = null;
        try {
            data = context instanceof Activity
                    ? ((Activity) context).getIntent().getData()
                    : null;
        } catch (Exception e) {
            e.printStackTrace();
            return list;
        }
        if (data == null) {
            return list;
        }

        try {
            InputStream is = context.getContentResolver().openInputStream(data);
            if (is == null) {
                return list;
            }
            br = new BufferedReader(new InputStreamReader(is));
            list = parseSessionLines(br);

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (br != null) br.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return list;
    }

    /**
     * Extracts every other line (input lines) from a {@code .cas} reader.
     * Extracted for unit testing without an Android runtime.
     */
    static List<String> parseSessionLines(BufferedReader br) throws Exception {
        List<String> list = new ArrayList<String>();
        String line;
        boolean keepInput = true;
        while ((line = br.readLine()) != null) {
            if (keepInput && !line.trim().isEmpty()) {
                list.add(line);
            }
            keepInput = !keepInput;
        }
        return list;
    }
}
