package org.kde.necessitas.mucephi.android_xcas;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.List;

/**
 * Ordered string-list codec for {@link History} and {@link Bookmarks}.
 *
 * {@code SharedPreferences.getStringSet} does not preserve iteration order,
 * so persisting expression lists as a string set scrambles newest-first
 * ordering on every process restart. A single JSON-array string keeps the
 * insertion order intact and survives commas, quotes and newlines inside
 * expressions.
 */
public final class HistoryCodec {

    private HistoryCodec() {
    }

    public static String encode(List<String> entries) {
        JSONArray array = new JSONArray();
        if (entries != null) {
            for (String entry : entries) {
                if (entry != null) {
                    array.put(entry);
                }
            }
        }
        return array.toString();
    }

    public static List<String> decode(String encoded) {
        List<String> out = new ArrayList<>();
        if (encoded == null || encoded.isEmpty()) {
            return out;
        }
        try {
            JSONArray array = new JSONArray(encoded);
            for (int i = 0; i < array.length(); i++) {
                String value = array.optString(i, null);
                if (value != null) {
                    out.add(value);
                }
            }
        } catch (Exception ignored) {
            // Corrupted entry: callers fall back to an empty list.
        }
        return out;
    }
}
