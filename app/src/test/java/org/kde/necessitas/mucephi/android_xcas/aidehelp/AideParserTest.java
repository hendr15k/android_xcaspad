package org.kde.necessitas.mucephi.android_xcas.aidehelp;

import android.content.Context;
import android.content.res.AssetManager;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.json.JSONObject;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.when;

public class AideParserTest {

    @Mock
    Context mockContext;

    @Mock
    AssetManager mockAssetManager;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);
        when(mockContext.getAssets()).thenReturn(mockAssetManager);
        AideParser.reset(); // ensure fresh state
    }

    @Test
    public void testExceptionHandlingWithMalformedJson() throws Exception {
        String malformedJsonString = "{\"langs\": {\"en\": \"Test describe\"}, \"related\": [\"rel1\"], \"examples\": [\"ex1\"]} \n" + // valid line
                "{\"invalid\": }"; // invalid json line

        InputStream stream = new ByteArrayInputStream(malformedJsonString.getBytes(StandardCharsets.UTF_8));
        when(mockAssetManager.open("help_xcas.json")).thenReturn(stream);

        List<JSONObject> dataset = AideParser.getJSONhelpDataset(mockContext, "en");

        // Should return a dataset with just the one valid parsed function (or possibly empty if exception happens on first, but since the first line is valid, it adds one then throws exception on second, returning the list with 1 element).
        // Wait, JSON in file is one json object per line.
        assertNotNull(dataset);
        assertEquals("Should parse the first line and then stop on the second without crashing", 1, dataset.size());
    }

    @Test
    public void testExceptionHandlingWithTotallyInvalidJson() throws Exception {
        String malformedJsonString = "this is not json";

        InputStream stream = new ByteArrayInputStream(malformedJsonString.getBytes(StandardCharsets.UTF_8));
        when(mockAssetManager.open("help_xcas.json")).thenReturn(stream);

        List<JSONObject> dataset = AideParser.getJSONhelpDataset(mockContext, "en");

        assertNotNull(dataset);
        assertEquals("Should return empty list on parsing error", 0, dataset.size());
    }
}
