package org.kde.necessitas.mucephi.android_xcas.aidehelp;

import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.StringReader;
import java.lang.reflect.Field;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class AideParserTest {

    @Before
    public void setup() throws Exception {
        AideParser.reset();
        // Ensure a fresh dataset list for each test.
        Field f = AideParser.class.getDeclaredField("mDataset");
        f.setAccessible(true);
        f.set(null, new java.util.ArrayList<JSONObject>());
    }

    private static List<JSONObject> parse(String content, String lang) throws Exception {
        AideParser.parseLines(new BufferedReader(new StringReader(content)), lang);
        Field f = AideParser.class.getDeclaredField("mDataset");
        f.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<JSONObject> dataset = (List<JSONObject>) f.get(null);
        return dataset;
    }

    @Test
    public void testParseLines_success() throws Exception {
        String json = "{\"langs\": {\"en\": \"desc\"}, \"related\": [\"rel1\"], \"examples\": [\"ex1\"]}\n";
        List<JSONObject> dataset = parse(json, "en");

        assertNotNull(dataset);
        assertEquals(1, dataset.size());

        JSONObject obj = dataset.get(0);
        assertEquals("desc", obj.getString("describe"));

        @SuppressWarnings("unchecked")
        List<String> related = (List<String>) obj.get("related");
        assertEquals(1, related.size());
        assertEquals("rel1", related.get(0));

        @SuppressWarnings("unchecked")
        List<String> examples = (List<String>) obj.get("examples");
        assertEquals(1, examples.size());
        assertEquals("ex1", examples.get(0));
    }

    @Test
    public void testParseLines_multipleLines() throws Exception {
        String json = "{\"langs\": {\"en\": \"a\"}, \"related\": [], \"examples\": []}\n"
                + "{\"langs\": {\"en\": \"b\"}, \"related\": [], \"examples\": []}\n";
        List<JSONObject> dataset = parse(json, "en");

        assertEquals(2, dataset.size());
        assertEquals("a", dataset.get(0).getString("describe"));
        assertEquals("b", dataset.get(1).getString("describe"));
    }

    @Test
    public void testParseLines_emptyInput() throws Exception {
        List<JSONObject> dataset = parse("", "en");

        assertNotNull(dataset);
        assertTrue(dataset.isEmpty());
    }

    @Test
    public void testParseLines_missingLangThrows() {
        String json = "{\"langs\": {\"en\": \"desc\"}, \"related\": [], \"examples\": []}\n";
        try {
            parse(json, "xx-missing");
            assertTrue("expected JSONException for missing language", false);
        } catch (Exception expected) {
            // Missing language key must surface instead of silently producing wrong output.
            assertTrue(expected instanceof org.json.JSONException);
        }
    }
}
