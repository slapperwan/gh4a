package com.gh4a.widget;

import android.content.Context;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
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
    public boolean showContextMenuForChild(View view) {
        if (view.getLayoutParams() instanceof RecyclerView.LayoutParams) {
            RecyclerView.LayoutParams params = (RecyclerView.LayoutParams) view.getLayoutParams();
            final int position = params.getBindingAdapterPosition();
            if (position == NO_POSITION) {
                return false;
            }
            final long id = getAdapter().getItemId(position);
            mContextMenuInfo = new RecyclerContextMenuInfo(position, id);
        }
        return super.showContextMenuForChild(view);
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
