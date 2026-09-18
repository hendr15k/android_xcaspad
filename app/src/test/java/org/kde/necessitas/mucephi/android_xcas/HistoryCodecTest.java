package org.kde.necessitas.mucephi.android_xcas;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class HistoryCodecTest {

    @Test
    public void testRoundTripPreservesOrder() {
        List<String> entries = Arrays.asList("simplify(x^2)", "plot(sin(x))", "solve(x^2=4,x)");
        String encoded = HistoryCodec.encode(entries);
        assertEquals(entries, HistoryCodec.decode(encoded));
    }

    @Test
    public void testSpecialCharactersSurvive() {
        List<String> entries = Arrays.asList(
                "a,\"b\"",
                "line1\nline2",
                "ünïcödé π ∑",
                "  padded  ");
        assertEquals(entries, HistoryCodec.decode(HistoryCodec.encode(entries)));
    }

    @Test
    public void testNullAndEmpty() {
        assertEquals("[]", HistoryCodec.encode(null));
        assertEquals("[]", HistoryCodec.encode(new java.util.ArrayList<String>()));
        assertTrue(HistoryCodec.decode(null).isEmpty());
        assertTrue(HistoryCodec.decode("").isEmpty());
    }

    @Test
    public void testCorruptedInputYieldsEmptyList() {
        assertTrue(HistoryCodec.decode("not-json{{{").isEmpty());
        assertTrue(HistoryCodec.decode("[unclosed").isEmpty());
    }
}
