package com.gh4a.adapter;

import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.gh4a.R;
import com.gh4a.db.BookmarksProvider;
import com.gh4a.db.BookmarksProvider.Columns;
import com.gh4a.utils.StringUtils;
import com.gh4a.utils.UiUtils;

public class BookmarkAdapter extends RecyclerView.Adapter<BookmarkAdapter.ViewHolder> implements
        View.OnClickListener {
    public interface OnItemClickListener {
        void onItemClick(long id, String url);
    }

    private final OnItemClickListener mItemClickListener;
    private final int mRepoIconResId;
    private final int mUserIconResId;
    private final LayoutInflater mInflater;
    private Cursor mCursor;

    private int mIdColumnIndex;
    private int mTypeColumnIndex;
    private int mNameColumnIndex;
    private int mExtraColumnIndex;
    private int mUrlColumnIndex;

    public BookmarkAdapter(Context context, OnItemClickListener listener) {
        super();
        setHasStableIds(true);
        mInflater = LayoutInflater.from(context);
        mRepoIconResId = UiUtils.resolveDrawable(context, R.attr.repoBookmarkIcon);
        mUserIconResId = UiUtils.resolveDrawable(context, R.attr.userBookmarkIcon);
        mItemClickListener = listener;
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
        }
        notifyDataSetChanged();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.row_bookmark, parent, false);
        ViewHolder vh = new ViewHolder(view);
        if (mItemClickListener != null) {
            view.setOnClickListener(this);
            view.setTag(vh);
        }
        return vh;
    }


    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        if (!mCursor.moveToPosition(position)) {
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
        return mCursor.moveToPosition(position) ? mCursor.getLong(mIdColumnIndex) : -1;
    }

    @Override
    public void onClick(View view) {
        ViewHolder vh = (ViewHolder) view.getTag();
        int position = vh.getAdapterPosition();
        if (position != RecyclerView.NO_POSITION && mCursor.moveToPosition(position)) {
            mItemClickListener.onItemClick(mCursor.getLong(mIdColumnIndex),
                    mCursor.getString(mUrlColumnIndex));
        }
    }

    public void onItemMoved(Context context, int fromPos, int toPos) {
        if (mCursor.moveToPosition(fromPos)) {
            long firstId = mCursor.getLong(mIdColumnIndex);
            BookmarksProvider.reorderBookmark(context, firstId, toPos);
        }

        if (mCursor.moveToPosition(toPos)) {
            long secondId = mCursor.getLong(mIdColumnIndex);
            BookmarksProvider.reorderBookmark(context, secondId, fromPos);
        }
    }

    public void onItemSwiped(RecyclerView.ViewHolder viewHolder) {
        int position = viewHolder.getAdapterPosition();
        if (position == RecyclerView.NO_POSITION || !mCursor.moveToPosition(position)) {
            return;
        }

        final Context context = viewHolder.itemView.getContext();
        final long id = mCursor.getLong(mIdColumnIndex);
        final String name = mCursor.getString(mNameColumnIndex);
        final int type = mCursor.getInt(mTypeColumnIndex);
        final String url = mCursor.getString(mUrlColumnIndex);
        final String extraData = mCursor.getString(mExtraColumnIndex);

        Uri uri = ContentUris.withAppendedId(BookmarksProvider.Columns.CONTENT_URI, id);
        context.getContentResolver().delete(uri, null, null);

        Snackbar.make(viewHolder.itemView, R.string.bookmark_removed, Snackbar.LENGTH_LONG)
                .setAction(R.string.undo, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        BookmarksProvider.saveBookmark(context, name, type, url, extraData, false);
                    }
                }).show();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private ImageView mIcon;
        private TextView mTitle;
        private TextView mExtra;

        public ViewHolder(View view) {
            super(view);
            mIcon = (ImageView) view.findViewById(R.id.iv_icon);
            mTitle = (TextView) view.findViewById(R.id.tv_title);
            mExtra = (TextView) view.findViewById(R.id.tv_extra);
        }
    }
}
