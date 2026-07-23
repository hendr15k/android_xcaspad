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
import android.content.SharedPreferences;
import android.util.Base64;
import android.util.Log;

import org.kde.necessitas.mucephi.android_xcas.adapteroperations.HolderOperation;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Persists the current session so it survives process death without requiring
 * {@code WRITE_EXTERNAL_STORAGE} permissions.
 *
 * Each input string and its pretty-print bitmap (PNG bytes) are stored in a
 * single {@link SharedPreferences} entry as a Base64 encoded Java-serialized
 * list. Capacity is capped so storage cannot grow unbounded.
 */
public final class SessionPersistence {

    private static final String TAG = "SessionPersistence";
    private static final String KEY_ENTRIES = "current_session_entries_v1";
    private static final int MAX_ENTRIES = 200;

    private static SessionPersistence instance;

    private final SharedPreferences prefs;

    private SessionPersistence(Context context) {
        this.prefs = context.getApplicationContext()
                .getSharedPreferences(AppSpace.PREFS_XCASPAD, Context.MODE_PRIVATE);
    }

    public static synchronized SessionPersistence get(Context context) {
        if (instance == null) {
            instance = new SessionPersistence(context);
        }
        return instance;
    }

    /**
     * Snapshot of a single entry, suitable for serialization.
     */
    public static class Entry implements Serializable {
        private static final long serialVersionUID = 1L;

        public String input;
        public String output;
        public byte[] bitmap;

        public Entry() {
        }

        public Entry(String input, String output, byte[] bitmap) {
            this.input = input;
            this.output = output;
            this.bitmap = bitmap;
        }
    }

    public synchronized void save(List<HolderOperation> operations) {
        if (operations == null || operations.isEmpty()) {
            prefs.edit().remove(KEY_ENTRIES).apply();
            return;
        }

        List<Entry> entries = new ArrayList<>();
        int start = Math.max(0, operations.size() - MAX_ENTRIES);
        for (int i = start; i < operations.size(); i++) {
            HolderOperation op = operations.get(i);
            if (op == null) {
                continue;
            }
            entries.add(new Entry(
                    op.getStrInput(),
                    op.getStrOutput(),
                    flatten(op.getBmpInput())));
        }

        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            ObjectOutputStream out = new ObjectOutputStream(bytes);
            out.writeObject(entries);
            out.close();
            String encoded = Base64.encodeToString(bytes.toByteArray(), Base64.DEFAULT);
            prefs.edit().putString(KEY_ENTRIES, encoded).apply();
        } catch (Exception e) {
            Log.e(TAG, "save failed", e);
        }
    }

    @SuppressWarnings("unchecked")
    public synchronized List<Entry> restore() {
        String encoded = prefs.getString(KEY_ENTRIES, null);
        if (encoded == null) {
            return null;
        }
        try {
            byte[] bytes = Base64.decode(encoded, Base64.DEFAULT);
            java.io.ObjectInputStream in = new java.io.ObjectInputStream(
                    new java.io.ByteArrayInputStream(bytes));
            Object obj = in.readObject();
            in.close();
            if (obj instanceof List) {
                List<Entry> list = (List<Entry>) obj;
                return list.isEmpty() ? null : list;
            }
        } catch (Exception e) {
            Log.w(TAG, "restore failed; clearing corrupted entry", e);
            prefs.edit().remove(KEY_ENTRIES).apply();
        }
        return null;
    }

    public synchronized void clear() {
        prefs.edit().remove(KEY_ENTRIES).apply();
    }

    private static byte[] flatten(android.graphics.Bitmap bmp) {
        if (bmp == null) {
            return null;
        }
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            bmp.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, out);
            return out.toByteArray();
        } catch (Exception e) {
            return null;
        }
    }

    public static Map<String, Object> describe(List<Entry> entries) {
        Map<String, Object> meta = new HashMap<>();
        meta.put("size", entries == null ? 0 : entries.size());
        return meta;
    }
}
