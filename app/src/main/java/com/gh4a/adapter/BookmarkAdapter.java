package com.gh4a.adapter;

import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.gh4a.R;
import com.gh4a.db.BookmarksProvider;
import com.gh4a.db.BookmarksProvider.Columns;
import com.gh4a.utils.StringUtils;
import com.gh4a.utils.UiUtils;

import java.util.ArrayList;
import java.util.List;

public class BookmarkAdapter extends RecyclerView.Adapter<BookmarkAdapter.ViewHolder> implements
        View.OnClickListener {
    public interface OnItemInteractListener {
        void onItemClick(long id, String url);
        void onItemDrag(RecyclerView.ViewHolder viewHolder);
    }

    private final OnItemInteractListener mItemInteractListener;
    private final int mRepoIconResId;
    private final int mUserIconResId;
    private final LayoutInflater mInflater;
    private Cursor mCursor;

    private int mIdColumnIndex;
    private int mTypeColumnIndex;
    private int mNameColumnIndex;
    private int mExtraColumnIndex;
    private int mUrlColumnIndex;

    private final List<Integer> mPositions = new ArrayList<>();

    public BookmarkAdapter(Context context, OnItemInteractListener listener) {
        super();
        setHasStableIds(true);
        mInflater = LayoutInflater.from(context);
        mRepoIconResId = UiUtils.resolveDrawable(context, R.attr.repoBookmarkIcon);
        mUserIconResId = UiUtils.resolveDrawable(context, R.attr.userBookmarkIcon);
        mItemInteractListener = listener;
    }

    public void swapCursor(Cursor cursor) {
        if (cursor == mCursor) {
            return;
        }
        if (mCursor != null) {
            mCursor.close();
        }
        mCursor = cursor;
        if (mCursor != null) {
            mIdColumnIndex = cursor.getColumnIndexOrThrow(Columns._ID);
            mTypeColumnIndex = cursor.getColumnIndexOrThrow(Columns.TYPE);
            mNameColumnIndex = cursor.getColumnIndexOrThrow(Columns.NAME);
            mExtraColumnIndex = cursor.getColumnIndexOrThrow(Columns.EXTRA);
            mUrlColumnIndex = cursor.getColumnIndexOrThrow(Columns.URI);

            mPositions.clear();
            for (int i = 0; i < mCursor.getCount(); i++) {
                mPositions.add(i);
            }
        }
        notifyDataSetChanged();
    }

    public void updateOrder(Context context) {
        for (int newPosition = 0; newPosition < mPositions.size(); newPosition++) {
            Integer oldPosition = mPositions.get(newPosition);
            if (newPosition != oldPosition && mCursor.moveToPosition(oldPosition)) {
                long id = mCursor.getLong(mIdColumnIndex);
                BookmarksProvider.reorderBookmark(context, id, newPosition);
            }
        }
    }

    private boolean moveCursorToPosition(int position) {
        return mCursor.moveToPosition(mPositions.get(position));
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.row_bookmark, parent, false);
        ViewHolder vh = new ViewHolder(view, mItemInteractListener);
        if (mItemInteractListener != null) {
            view.setOnClickListener(this);
            view.setTag(vh);
        }
        return vh;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        if (!moveCursorToPosition(position)) {
            return;
        }

        int type = mCursor.getInt(mTypeColumnIndex);
        String name = mCursor.getString(mNameColumnIndex);
        String extraData = mCursor.getString(mExtraColumnIndex);

        switch (type) {
            case Columns.TYPE_REPO: holder.mIcon.setImageResource(mRepoIconResId); break;
            case Columns.TYPE_USER: holder.mIcon.setImageResource(mUserIconResId); break;
            default: holder.mIcon.setImageDrawable(null); break;
        }

        holder.mTitle.setText(name);
        if (StringUtils.isBlank(extraData)) {
            holder.mExtra.setVisibility(View.GONE);
        } else {
            holder.mExtra.setText(extraData);
            holder.mExtra.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public int getItemCount() {
        return mCursor != null ? mCursor.getCount() : 0;
    }

    @Override
    public long getItemId(int position) {
        return moveCursorToPosition(position) ? mCursor.getLong(mIdColumnIndex) : -1;
    }

    @Override
    public void onClick(View view) {
        ViewHolder vh = (ViewHolder) view.getTag();
        int position = vh.getAdapterPosition();
        if (position != RecyclerView.NO_POSITION && moveCursorToPosition(position)) {
            mItemInteractListener.onItemClick(mCursor.getLong(mIdColumnIndex),
                    mCursor.getString(mUrlColumnIndex));
        }
    }

    public void onItemMoved(int fromPos, int toPos) {
        mPositions.add(toPos, mPositions.remove(fromPos));
        notifyItemMoved(fromPos, toPos);
    }

    public void onItemSwiped(RecyclerView.ViewHolder viewHolder) {
        int position = viewHolder.getAdapterPosition();
        if (position == RecyclerView.NO_POSITION || !moveCursorToPosition(position)) {
            return;
        }

        final Context context = viewHolder.itemView.getContext();
        final long id = mCursor.getLong(mIdColumnIndex);
        final String name = mCursor.getString(mNameColumnIndex);
        final int type = mCursor.getInt(mTypeColumnIndex);
        final String url = mCursor.getString(mUrlColumnIndex);
        final String extraData = mCursor.getString(mExtraColumnIndex);

        updateOrder(context);

        Uri uri = ContentUris.withAppendedId(BookmarksProvider.Columns.CONTENT_URI, id);
        context.getContentResolver().delete(uri, null, null);

        Snackbar.make(viewHolder.itemView, R.string.bookmark_removed, Snackbar.LENGTH_LONG)
                .setAction(R.string.undo,
                        v -> BookmarksProvider.saveBookmark(context, name, type, url, extraData, false))
                .show();
    }

    static class ViewHolder extends RecyclerView.ViewHolder implements View.OnTouchListener {

        private final OnItemInteractListener mItemInteractListener;
        private final ImageView mIcon;
        private final TextView mTitle;
        private final TextView mExtra;

        public ViewHolder(View view, OnItemInteractListener itemInteractListener) {
            super(view);
            mItemInteractListener = itemInteractListener;
            mIcon = view.findViewById(R.id.iv_icon);
            mTitle = view.findViewById(R.id.tv_title);
            mExtra = view.findViewById(R.id.tv_extra);
            view.findViewById(R.id.iv_drag_handle).setOnTouchListener(this);
        }

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
                mItemInteractListener.onItemDrag(this);
            }
            return false;
        }
    }
}
