package com.gh4a.fragment;

import android.content.ContentUris;
import android.content.DialogInterface;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;

import com.gh4a.BrowseFilter;
import com.gh4a.R;
import com.gh4a.adapter.BookmarkAdapter;
import com.gh4a.db.BookmarksProvider;

public class BookmarkListFragment extends LoadingListFragmentBase implements
        LoaderManager.LoaderCallbacks<Cursor>, BookmarkAdapter.OnItemClickListener {
    public static BookmarkListFragment newInstance() {
        return new BookmarkListFragment();
    }

    private BookmarkAdapter mAdapter;

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setContentShown(false);
        getLoaderManager().initLoader(0, null, this);
    }

    @Override
    protected void onRecyclerViewInflated(RecyclerView view, LayoutInflater inflater) {
        super.onRecyclerViewInflated(view, inflater);
        mAdapter = new BookmarkAdapter(getActivity(), this);
        view.setAdapter(mAdapter);
        updateEmptyState();
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new CursorLoader(getActivity(), BookmarksProvider.Columns.CONTENT_URI,
                null, null, null, null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        mAdapter.swapCursor(data);
        setContentShown(true);
        updateEmptyState();
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mAdapter.swapCursor(null);
        updateEmptyState();
    }

    @Override
    public void onRefresh() {
        hideContentAndRestartLoaders(0);
    }

    @Override
    protected int getEmptyTextResId() {
        return R.string.no_bookmarks;
    }

    @Override
    public void onItemClick(long id, String url) {
        startActivity(BrowseFilter.makeRedirectionIntent(getActivity(), Uri.parse(url), null));
    }

    @Override
    public void onItemLongClick(final long id) {
        new AlertDialog.Builder(getActivity())
                .setMessage(R.string.remove_bookmark_confirm)
                .setIconAttribute(android.R.attr.alertDialogIcon)
                .setCancelable(false)
                .setPositiveButton(getString(R.string.remove), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        Uri uri = ContentUris.withAppendedId(BookmarksProvider.Columns.CONTENT_URI, id);
                        getActivity().getContentResolver().delete(uri, null, null);
                    }
                })
                .setNegativeButton(getString(R.string.cancel), null)
                .show();
    }
}
