package org.kde.necessitas.mucephi.android_xcas;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class Plot3DRendererTest {

    private static String buildSurface(double[][] grid) {
        StringBuilder sb = new StringBuilder("hypersurface(group[");
        for (double[] p : grid) {
            sb.append("point[").append(p[0]).append(",").append(p[1]).append(",").append(p[2]).append("],");
        }
        if (sb.charAt(sb.length() - 1) == ',') {
            sb.setLength(sb.length() - 1);
        }
        sb.append("])");
        return sb.toString();
    }

    private static double[][] makeGrid(int rows, int cols) {
        double[][] grid = new double[rows * cols][3];
        int k = 0;
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                double x = j;
                double y = i;
                grid[k][0] = x;
                grid[k][1] = y;
                grid[k][2] = x * x + y * y;
                k++;
            }
        }
        return grid;
    }

    @Test
    public void testIs3DPlotResult() {
        assertTrue(Plot3DRenderer.is3DPlotResult("hypersurface(group[point[0,0,0]])"));
        assertFalse(Plot3DRenderer.is3DPlotResult("pnt(pnt[curve(group["));
        assertFalse(Plot3DRenderer.is3DPlotResult(null));
        assertFalse(Plot3DRenderer.is3DPlotResult("2"));
    }

    @Test
    public void testParseNullForNon3D() {
        assertNull(Plot3DRenderer.parse("pnt(pnt[curve(group["));
        assertNull(Plot3DRenderer.parse(null));
    }

    @Test
    public void testParseGridAndBounds() {
        double[][] grid = makeGrid(4, 5);
        Plot3DRenderer.Plot3DData data = Plot3DRenderer.parse(buildSurface(grid));

        assertNotNull(data);
        assertEquals(20, data.points.size());
        assertEquals(4, data.rows);
        assertEquals(5, data.cols);

        assertEquals(0.0, data.minX, 1e-9);
        assertEquals(4.0, data.maxX, 1e-9);
        assertEquals(0.0, data.minY, 1e-9);
        assertEquals(3.0, data.maxY, 1e-9);
        assertEquals(0.0, data.minZ, 1e-9);
        assertEquals(25.0, data.maxZ, 1e-9);

        for (boolean v : data.valid) {
            assertTrue(v);
        }
    }

    @Test
    public void testParseDropsNonNumericPoints() {
        String result = "hypersurface(group[point[0,0,0],point[1,1,1],point[NaN,NaN,NaN],point[2,2,4]])";
        // The NaN point is not matched by the numeric regex and is dropped,
        // leaving only 3 points which cannot form a 2D grid.
        assertNull(Plot3DRenderer.parse(result));
    }

    @Test
    public void testParseReturnsNullWhenTooFewPoints() {
        assertNull(Plot3DRenderer.parse("hypersurface(group[point[0,0,0]])"));
    }
}
