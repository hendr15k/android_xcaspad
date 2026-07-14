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
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Persistent collection of user-bookmarked expressions.
 *
 * Stored as a string set in SharedPreferences. Distinct from the {@link History}
 * class which records every input expression automatically; bookmarks are
 * explicitly tagged by the user and survive in their own list.
 */
public final class Bookmarks {

    private static final String KEY_BOOKMARKS = "bookmarked_inputs";

    private static final int MAX_ENTRIES = 200;

    private static Bookmarks instance;

    private final SharedPreferences prefs;
    private final LinkedHashSet<String> entries = new LinkedHashSet<>();

    private Bookmarks(Context context) {
        this.prefs = context.getApplicationContext()
                .getSharedPreferences(AppSpace.PREFS_XCASPAD, Context.MODE_PRIVATE);
        Set<String> saved = prefs.getStringSet(KEY_BOOKMARKS, null);
        if (saved != null) {
            entries.addAll(saved);
        }
    }

    public static synchronized Bookmarks get(Context context) {
        if (instance == null) {
            instance = new Bookmarks(context);
        }
        return instance;
    }

    public synchronized boolean isBookmarked(String input) {
        if (input == null) {
            return false;
        }
        return entries.contains(input.trim());
    }

    public synchronized boolean toggle(String input) {
        if (input == null) {
            return false;
        }
        String trimmed = input.trim();
        if (trimmed.isEmpty()) {
            return false;
        }
        boolean nowBookmarked;
        if (entries.contains(trimmed)) {
            entries.remove(trimmed);
            nowBookmarked = false;
        } else {
            entries.add(trimmed);
            while (entries.size() > MAX_ENTRIES) {
                String first = entries.iterator().next();
                entries.remove(first);
            }
            nowBookmarked = true;
        }
        persist();
        return nowBookmarked;
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
                .putStringSet(KEY_BOOKMARKS, new LinkedHashSet<>(entries))
                .apply();
    }
}
