package org.kde.necessitas.mucephi.android_xcas.aidehelp;

import android.content.Context;
import android.content.res.AssetManager;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class AideParserTest {

    private Context mockContext;
    private AssetManager mockAssetManager;

    @Before
    public void setup() {
        mockContext = mock(Context.class);
        mockAssetManager = mock(AssetManager.class);
        when(mockContext.getAssets()).thenReturn(mockAssetManager);
        AideParser.reset();
    }

    @Test
    public void testGetJSONhelpDataset_success() throws Exception {
        String json = "{\"langs\": {\"en\": \"desc\"}, \"related\": [\"rel1\"], \"examples\": [\"ex1\"]}\n";
        InputStream stream = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
        when(mockAssetManager.open("help_xcas.json")).thenReturn(stream);

        List<JSONObject> dataset = AideParser.getJSONhelpDataset(mockContext, "en");

        assertNotNull(dataset);
        assertEquals(1, dataset.size());

        JSONObject obj = dataset.get(0);
        assertEquals("desc", obj.getString("describe"));

        List<String> related = (List<String>) obj.get("related");
        assertEquals(1, related.size());
        assertEquals("rel1", related.get(0));

        List<String> examples = (List<String>) obj.get("examples");
        assertEquals(1, examples.size());
        assertEquals("ex1", examples.get(0));
    }

    @Test
    public void testGetJSONhelpDataset_caching() throws Exception {
        String json = "{\"langs\": {\"en\": \"desc\"}, \"related\": [], \"examples\": []}\n";
        InputStream stream = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
        when(mockAssetManager.open("help_xcas.json")).thenReturn(stream);

        List<JSONObject> dataset1 = AideParser.getJSONhelpDataset(mockContext, "en");
        List<JSONObject> dataset2 = AideParser.getJSONhelpDataset(mockContext, "en");

        assertSame(dataset1, dataset2);
        verify(mockAssetManager, times(1)).open("help_xcas.json");
    }

    @Test
    public void testGetJSONhelpDataset_nullStream() throws Exception {
        when(mockAssetManager.open("help_xcas.json")).thenReturn(null);

        List<JSONObject> dataset = AideParser.getJSONhelpDataset(mockContext, "en");

        assertNotNull(dataset);
        assertTrue(dataset.isEmpty());
    }

    @Test
    public void testGetJSONhelpDataset_exception() throws Exception {
        when(mockAssetManager.open("help_xcas.json")).thenThrow(new java.io.IOException("Test Exception"));

        List<JSONObject> dataset = AideParser.getJSONhelpDataset(mockContext, "en");

        assertNotNull(dataset);
        assertTrue(dataset.isEmpty());
    }
}
