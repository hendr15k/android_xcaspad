package org.kde.necessitas.mucephi.android_xcas.adapteroperations;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class AdapterOperationsTest {

    private OperationList list;
    private List<HolderOperation> dataset;
    private int changeCount;

    @Before
    public void setup() {
        dataset = new ArrayList<>();
        dataset.add(new HolderOperation());
        dataset.add(new HolderOperation());
        dataset.add(new HolderOperation());

        changeCount = 0;
        list = new OperationList(dataset);
        list.setChangeListener(new OperationList.ChangeListener() {
            @Override
            public void onDatasetChanged() {
                changeCount++;
            }
        });
    }

    @Test
    public void testRemove() {
        assertEquals(3, list.getItemCount());

        list.remove(1);

        assertEquals(2, list.getItemCount());
        assertEquals(2, dataset.size());
        assertEquals(1, changeCount);
    }

    @Test
    public void testRemoveOutOfBounds() {
        assertEquals(3, list.getItemCount());

        assertNull(list.remove(5));
        assertEquals(3, list.getItemCount());

        assertNull(list.remove(-1));
        assertEquals(3, list.getItemCount());
        assertEquals(0, changeCount);
    }

    @Test
    public void testSwap() {
        HolderOperation op1 = dataset.get(0);
        HolderOperation op2 = dataset.get(1);

        list.swap(0, 1);

        assertSame(op2, list.getItem(0));
        assertSame(op1, list.getItem(1));
        assertEquals(1, changeCount);
    }

    @Test
    public void testInsertClampsOutOfRange() {
        HolderOperation op = new HolderOperation();
        list.insert(99, op);

        assertEquals(4, list.getItemCount());
        assertSame(op, list.getItem(3));

        list.insert(0, null);
        assertEquals(4, list.getItemCount());
    }

    @Test
    public void testViewTypeFor3D() {
        HolderOperation op = new HolderOperation();
        op.setPlot3DData(new org.kde.necessitas.mucephi.android_xcas.Plot3DRenderer.Plot3DData());
        dataset.add(op);

        int idx = dataset.size() - 1;
        assertTrue(op.getPlot3DData() != null);
        assertEquals(OperationList.VIEW_TYPE_3D, 1);
        assertEquals(OperationList.VIEW_TYPE_NORMAL, 0);
        assertEquals(4, list.getItemCount());
    }
}
