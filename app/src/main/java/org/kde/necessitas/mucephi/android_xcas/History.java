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

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Lightweight, persistent history of past input expressions.
 *
 * Stored as a string set in SharedPreferences. Newest entry is kept at the
 * top. A cap prevents the list from growing indefinitely.
 */
public final class History {

    public static final int DEFAULT_MAX_ENTRIES = 100;
    public static final int MIN_ENTRIES = 10;
    public static final int MAX_ENTRIES = 500;

    private static final String KEY_INPUT_HISTORY = "input_history";
    private static final String KEY_MAX_ENTRIES = "history_max_entries";

    private static History instance;

    private final SharedPreferences prefs;
    private final LinkedHashSet<String> entries = new LinkedHashSet<>();

    private History(Context context) {
        this.prefs = context.getApplicationContext()
                .getSharedPreferences(AppSpace.PREFS_XCASPAD, Context.MODE_PRIVATE);
        Set<String> saved = prefs.getStringSet(KEY_INPUT_HISTORY, null);
        if (saved != null) {
            entries.addAll(saved);
        }
    }

    public static synchronized History get(Context context) {
        if (instance == null) {
            instance = new History(context);
        }
        return instance;
    }

    public synchronized int getMaxEntries() {
        int value = prefs.getInt(KEY_MAX_ENTRIES, DEFAULT_MAX_ENTRIES);
        if (value < MIN_ENTRIES) {
            return MIN_ENTRIES;
        }
        if (value > MAX_ENTRIES) {
            return MAX_ENTRIES;
        }
        return value;
    }

    public synchronized void setMaxEntries(int maxEntries) {
        int clamped = maxEntries;
        if (clamped < MIN_ENTRIES) {
            clamped = MIN_ENTRIES;
        }
        if (clamped > MAX_ENTRIES) {
            clamped = MAX_ENTRIES;
        }
        if (clamped == getMaxEntries()) {
            return;
        }
        prefs.edit().putInt(KEY_MAX_ENTRIES, clamped).apply();
        while (entries.size() > clamped) {
            String first = entries.iterator().next();
            entries.remove(first);
        }
        persist();
    }

    public synchronized void add(String input) {
        if (input == null) {
            return;
        }
        String trimmed = input.trim();
        if (trimmed.isEmpty()) {
            return;
        }
        if (entries.contains(trimmed)) {
            entries.remove(trimmed);
        }
        entries.add(trimmed);
        while (entries.size() > getMaxEntries()) {
            String first = entries.iterator().next();
            entries.remove(first);
        }
        persist();
    }

    public synchronized void remove(String input) {
        if (entries.remove(input)) {
            persist();
        }
    }

    public synchronized void clear() {
        if (entries.isEmpty()) {
            return;
        }
        entries.clear();
        persist();
    }

    public synchronized List<String> snapshot() {
        List<String> list = new ArrayList<>(entries);
        Collections.reverse(list);
        return list;
    }

    private void persist() {
        prefs.edit()
                .putStringSet(KEY_INPUT_HISTORY, new LinkedHashSet<>(entries))
                .apply();
    }
}