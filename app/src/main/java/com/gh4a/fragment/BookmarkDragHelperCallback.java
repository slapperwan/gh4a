package com.gh4a.fragment;

import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;

import com.gh4a.adapter.BookmarkAdapter;

public class BookmarkDragHelperCallback extends ItemTouchHelper.SimpleCallback {
    private final BookmarkAdapter mAdapter;

    public BookmarkDragHelperCallback(BookmarkAdapter adapter) {
        super(ItemTouchHelper.UP | ItemTouchHelper.DOWN,
                ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT);
        mAdapter = adapter;
    }

    @Override
    public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder,
            RecyclerView.ViewHolder target) {
        int fromPos = viewHolder.getAdapterPosition();
        int toPos = target.getAdapterPosition();

        mAdapter.onItemMoved(recyclerView.getContext(), fromPos, toPos);
        return false;
    }

    @Override
    public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
        mAdapter.onItemSwiped(viewHolder);
    }

    @Override
    public boolean isLongPressDragEnabled() {
        return false;
    }
}
