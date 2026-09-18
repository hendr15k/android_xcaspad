package org.kde.necessitas.mucephi.android_xcas;

import org.kde.necessitas.mucephi.android_xcas.adapteroperations.HolderOperation;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Case-insensitive session search across inputs, outputs and history.
 *
 * Extracted from {@code XcasPadActivity} so the matching behaviour is unit
 * testable without an Android runtime.
 */
public final class SessionSearch {

    private SessionSearch() {
    }

    public static List<String> findMatches(List<HolderOperation> operations,
                                           List<String> history,
                                           String query) {
        List<String> matches = new ArrayList<>();
        if (query == null) {
            return matches;
        }
        String needle = query.trim().toLowerCase(Locale.ROOT);
        if (needle.isEmpty()) {
            return matches;
        }
        if (operations != null) {
            for (HolderOperation op : operations) {
                if (op == null) {
                    continue;
                }
                addIfMatch(matches, op.getStrInput(), needle);
                addIfOutputMatch(matches, op, needle);
            }
        }
        if (history != null) {
            for (String h : history) {
                addIfMatch(matches, h, needle);
            }
        }
        return matches;
    }

    private static void addIfMatch(List<String> matches, String candidate, String needle) {
        if (candidate != null
                && candidate.toLowerCase(Locale.ROOT).contains(needle)
                && !matches.contains(candidate)) {
            matches.add(candidate);
        }
    }

    private static void addIfOutputMatch(List<String> matches, HolderOperation op, String needle) {
        String output = op.getStrOutput();
        if (output == null || output.toLowerCase(Locale.ROOT).indexOf(needle) < 0) {
            return;
        }
        String input = op.getStrInput();
        if (input != null && !matches.contains(input)) {
            matches.add(input);
        } else if (input == null && !matches.contains(output)) {
            matches.add(output);
        }
    }
}
