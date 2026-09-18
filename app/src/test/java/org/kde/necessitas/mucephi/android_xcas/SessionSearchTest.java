package org.kde.necessitas.mucephi.android_xcas;

import org.junit.Test;
import org.kde.necessitas.mucephi.android_xcas.adapteroperations.HolderOperation;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SessionSearchTest {

    private static HolderOperation op(String input, String output) {
        HolderOperation op = new HolderOperation();
        op.setStrInput(input);
        op.setStrOutput(output);
        return op;
    }

    @Test
    public void testMatchesInputCaseInsensitive() {
        List<HolderOperation> ops = Arrays.asList(op("Simplify(x^2+2*x+1)", "x+1"));
        List<String> matches = SessionSearch.findMatches(ops, Collections.<String>emptyList(), "simplify");
        assertEquals(1, matches.size());
        assertEquals("Simplify(x^2+2*x+1)", matches.get(0));
    }

    @Test
    public void testMatchesOutput() {
        List<HolderOperation> ops = Arrays.asList(op("factor(12)", "2^2*3"));
        List<String> matches = SessionSearch.findMatches(ops, Collections.<String>emptyList(), "2^2");
        assertEquals(1, matches.size());
        assertEquals("factor(12)", matches.get(0));
    }

    @Test
    public void testMatchesHistoryAndDedupes() {
        List<HolderOperation> ops = Arrays.asList(op("plot(sin(x))", "plot"));
        List<String> history = Arrays.asList("plot(sin(x))", "plot(cos(x))");
        List<String> matches = SessionSearch.findMatches(ops, history, "PLOT");
        assertEquals(2, matches.size());
        assertTrue(matches.contains("plot(sin(x))"));
        assertTrue(matches.contains("plot(cos(x))"));
    }

    @Test
    public void testBlankQueryYieldsNoMatches() {
        List<HolderOperation> ops = Arrays.asList(op("1+1", "2"));
        assertTrue(SessionSearch.findMatches(ops, Arrays.asList("1+1"), "  ").isEmpty());
        assertTrue(SessionSearch.findMatches(ops, Arrays.asList("1+1"), null).isEmpty());
    }

    @Test
    public void testNullOperationEntriesSkipped() {
        List<HolderOperation> ops = Arrays.asList(null, op(null, null));
        assertTrue(SessionSearch.findMatches(ops, null, "x").isEmpty());
    }
}
