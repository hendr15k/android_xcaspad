package org.kde.necessitas.mucephi.android_xcas.adapteroperations;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Plain-Java operation list backing {@link AdapterOperations}.
 *
 * Extracted so the remove/insert/swap behaviour is unit testable without an
 * Android runtime (RecyclerView notify methods are final and cannot run in
 * local JVM tests).
 */
public class OperationList {

    /** View-type constants mirrored by the adapter. */
    public static final int VIEW_TYPE_NORMAL = 0;
    public static final int VIEW_TYPE_3D = 1;

    public interface ChangeListener {
        void onDatasetChanged();
    }

    private final List<HolderOperation> dataset;
    private ChangeListener changeListener;

    public OperationList(List<HolderOperation> dataset) {
        this.dataset = dataset != null ? dataset : new ArrayList<HolderOperation>();
    }

    public void setChangeListener(ChangeListener listener) {
        this.changeListener = listener;
    }

    public int getItemCount() {
        return dataset.size();
    }

    public HolderOperation getItem(int position) {
        return dataset.get(position);
    }

    public List<HolderOperation> getDataset() {
        return dataset;
    }

    public HolderOperation remove(int position) {
        if (position < 0 || position >= dataset.size()) {
            return null;
        }
        HolderOperation removed = dataset.remove(position);
        notifyChanged();
        return removed;
    }

    public void insert(int position, HolderOperation op) {
        if (op == null) {
            return;
        }
        if (position < 0 || position > dataset.size()) {
            position = dataset.size();
        }
        dataset.add(position, op);
        notifyChanged();
    }

    public void swap(int i, int j) {
        Collections.swap(dataset, i, j);
        notifyChanged();
    }

    private void notifyChanged() {
        if (changeListener != null) {
            changeListener.onDatasetChanged();
        }
    }
}
