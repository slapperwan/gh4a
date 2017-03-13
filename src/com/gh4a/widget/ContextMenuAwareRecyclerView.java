package com.gh4a.widget;

import android.content.Context;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.ContextMenu;
import android.view.View;

public class ContextMenuAwareRecyclerView extends RecyclerView {
    private RecyclerContextMenuInfo mContextMenuInfo;

    public ContextMenuAwareRecyclerView(Context context) {
        super(context);
    }

    public ContextMenuAwareRecyclerView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public ContextMenuAwareRecyclerView(Context context, @Nullable AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected ContextMenu.ContextMenuInfo getContextMenuInfo() {
        return mContextMenuInfo;
    }

    @Override
    public boolean showContextMenuForChild(View originalView) {
        final int position = getChildAdapterPosition(originalView);
        if (position != NO_POSITION) {
            final long id = getAdapter().getItemId(position);
            mContextMenuInfo = new RecyclerContextMenuInfo(position, id);
            return super.showContextMenuForChild(originalView);
        }
        return false;
    }

    public static class RecyclerContextMenuInfo implements ContextMenu.ContextMenuInfo {
        public final int position;
        public final long id;

        private RecyclerContextMenuInfo(int position, long id) {
            this.position = position;
            this.id = id;
        }
    }
}
