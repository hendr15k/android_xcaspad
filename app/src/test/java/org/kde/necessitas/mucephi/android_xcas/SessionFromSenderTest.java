package org.kde.necessitas.mucephi.android_xcas;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.net.Uri;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

public class SessionFromSenderTest {

    @Mock
    private Activity mockActivity;

    @Mock
    private Intent mockIntent;

    @Mock
    private Uri mockUri;

    @Mock
    private ContentResolver mockContentResolver;

    private Method loadFromIntentMethod;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);

        // Access the private method via reflection
        loadFromIntentMethod = SessionFromSender.class.getDeclaredMethod("loadFromIntent", android.content.Context.class);
        loadFromIntentMethod.setAccessible(true);

        // Common mocking setup
        when(mockActivity.getIntent()).thenReturn(mockIntent);
        when(mockIntent.getData()).thenReturn(mockUri);
        when(mockActivity.getContentResolver()).thenReturn(mockContentResolver);
    }

    @Test
    public void testLoadFromIntent_ExceptionHandling() throws Exception {
        // Arrange
        when(mockContentResolver.openInputStream(mockUri)).thenThrow(new java.io.FileNotFoundException("Simulated test exception"));

        // Act
        @SuppressWarnings("unchecked")
        List<String> result = (List<String>) loadFromIntentMethod.invoke(null, mockActivity);

        // Assert
        assertNotNull(result);
        assertTrue("Result should be empty when an exception occurs", result.isEmpty());
    }

    @Test
    public void testLoadFromIntent_Success() throws Exception {
        // Arrange
        String mockData = "line1\nignore_this_line\nline2\nignore_this_too\nline3\n";
        InputStream mockInputStream = new ByteArrayInputStream(mockData.getBytes());
        when(mockContentResolver.openInputStream(mockUri)).thenReturn(mockInputStream);

        // Act
        @SuppressWarnings("unchecked")
        List<String> result = (List<String>) loadFromIntentMethod.invoke(null, mockActivity);

        // Assert
        assertNotNull(result);
        assertEquals(3, result.size());
        assertEquals("line1", result.get(0));
        assertEquals("line2", result.get(1));
        assertEquals("line3", result.get(2));
    }
}
