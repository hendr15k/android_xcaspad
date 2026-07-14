package org.kde.necessitas.mucephi.android_xcas.adapteroperations;

import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.ItemTouchHelper;

/**
 * Created by leonel on 28/11/17.
 */

public class TouchCallback extends ItemTouchHelper.Callback{

    public interface OnRemoveListener {
        void onItemRemoved(int position, HolderOperation removed);
    }

    private final AdapterOperations mAdapter;
    private final OnRemoveListener onRemove;

    public TouchCallback(AdapterOperations adapter) {
        this(adapter, null);
    }

    public TouchCallback(AdapterOperations adapter, OnRemoveListener onRemove) {
        mAdapter = adapter;
        this.onRemove = onRemove;
    }

    @Override
    public boolean isLongPressDragEnabled() {
        return true;
    }

    @Override
    public boolean isItemViewSwipeEnabled() {
        return true;
    }

    @Override
    public int getMovementFlags(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
        int dragFlags = ItemTouchHelper.UP | ItemTouchHelper.DOWN;
        int swipeFlags = ItemTouchHelper.START | ItemTouchHelper.END;
        return makeMovementFlags(dragFlags, swipeFlags);
    }

    @Override
    public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {

        int fromPosition = viewHolder.getAdapterPosition();
        int toPosition = target.getAdapterPosition();

        if (fromPosition < toPosition) {
            for (int i = fromPosition; i < toPosition; i++) {
                mAdapter.swap(i, i + 1);
            }
        } else {
            for (int i = fromPosition; i > toPosition; i--) {
                mAdapter.swap(i, i - 1);
            }
        }

        mAdapter.notifyItemMoved(fromPosition, toPosition);
        return true;
    }

    @Override
    public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
        int pos = viewHolder.getAdapterPosition();
        HolderOperation removed = mAdapter.remove(pos);
        if (onRemove != null) {
            onRemove.onItemRemoved(pos, removed);
        }
    }
}
