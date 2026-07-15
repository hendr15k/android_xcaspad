package org.kde.necessitas.mucephi.android_xcas.adapteroperations;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.anyInt;

public class AdapterOperationsTest {

    private AdapterOperations adapter;
    private List<HolderOperation> dataset;

    @Before
    public void setup() {
        dataset = new ArrayList<>();
        dataset.add(new HolderOperation());
        dataset.add(new HolderOperation());
        dataset.add(new HolderOperation());

        adapter = spy(new AdapterOperations(dataset, null, null));

        // Mock final methods to do nothing instead of interacting with missing Android environment
        doNothing().when(adapter).notifyDataSetChanged();
        doNothing().when(adapter).notifyItemMoved(anyInt(), anyInt());
    }

    @Test
    public void testRemove() {
        assertEquals(3, adapter.getItemCount());

        adapter.remove(1);

        assertEquals(2, adapter.getItemCount());
        assertEquals(2, dataset.size());
    }

    @Test
    public void testRemoveOutOfBounds() {
        assertEquals(3, adapter.getItemCount());

        adapter.remove(5);
        assertEquals(3, adapter.getItemCount());

        adapter.remove(-1);
        assertEquals(3, adapter.getItemCount());
    }

    @Test
    public void testSwap() {
        HolderOperation op1 = dataset.get(0);
        HolderOperation op2 = dataset.get(1);

        adapter.swap(0, 1);

        assertEquals(op2, adapter.getItem(0));
        assertEquals(op1, adapter.getItem(1));
    }
}
