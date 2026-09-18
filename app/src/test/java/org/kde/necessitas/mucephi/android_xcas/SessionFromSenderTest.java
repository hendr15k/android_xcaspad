package org.kde.necessitas.mucephi.android_xcas;

import org.junit.Test;

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class SessionFromSenderTest {

    @Test
    public void testParseSessionLines_success() throws Exception {
        String mockData = "line1\nignore_this_line\nline2\nignore_this_too\nline3\n";
        List<String> result = SessionFromSender.parseSessionLines(
                new BufferedReader(new StringReader(mockData)));

        assertNotNull(result);
        assertEquals(3, result.size());
        assertEquals("line1", result.get(0));
        assertEquals("line2", result.get(1));
        assertEquals("line3", result.get(2));
    }

    @Test
    public void testParseSessionLines_skipsBlankInputLines() throws Exception {
        String mockData = "  \noutput1\nline2\noutput2\n";
        List<String> result = SessionFromSender.parseSessionLines(
                new BufferedReader(new StringReader(mockData)));

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("line2", result.get(0));
    }

    @Test
    public void testParseSessionLines_emptyInput() throws Exception {
        List<String> result = SessionFromSender.parseSessionLines(
                new BufferedReader(new StringReader("")));

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }
}
