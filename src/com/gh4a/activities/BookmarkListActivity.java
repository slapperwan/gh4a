package com.gh4a.activities;

import java.net.URISyntaxException;

import android.app.AlertDialog;
import android.content.ContentUris;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import com.actionbarsherlock.app.ActionBar;
import com.gh4a.Gh4Application;
import com.gh4a.LoadingFragmentActivity;
import com.gh4a.R;
import com.gh4a.adapter.BookmarkAdapter;
import com.gh4a.db.BookmarksProvider.Columns;

public class BookmarkListActivity extends LoadingFragmentActivity implements
        AdapterView.OnItemClickListener, AdapterView.OnItemLongClickListener, LoaderCallbacks<Cursor> {
    private static final String TAG = "BookmarkListActivity";

    private BookmarkAdapter mAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        setTheme(Gh4Application.THEME);
        super.onCreate(savedInstanceState);

        setContentView(R.layout.generic_list);
        setContentShown(false);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle(R.string.bookmarks);
        actionBar.setDisplayHomeAsUpEnabled(true);

        ListView listView = (ListView) findViewById(R.id.list_view);
        mAdapter = new BookmarkAdapter(this);
        listView.setAdapter(mAdapter);
        listView.setOnItemClickListener(this);
        listView.setOnItemLongClickListener(this);

        getSupportLoaderManager().initLoader(0, null, this);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Cursor cursor = (Cursor) mAdapter.getItem(position);
        String uri = cursor.getString(cursor.getColumnIndexOrThrow(Columns.URI));

        try {
            Intent intent = Intent.parseUri(uri, 0);
            startActivity(intent);
        } catch (URISyntaxException e) {
            Log.e(TAG, "Couldn't parse bookmark URI " + uri);
        }
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> adapterView, View view, int position, final long id) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.remove_bookmark)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setMessage(R.string.remove_bookmark_confirm)
                .setCancelable(false)
                .setPositiveButton(getString(R.string.yes), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        Uri uri = ContentUris.withAppendedId(Columns.CONTENT_URI, id);
                        getContentResolver().delete(uri, null, null);
                    }
                })
                .setNegativeButton(getString(R.string.no), null)
                .show();
        return true;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new CursorLoader(this, Columns.CONTENT_URI, null, null, null, null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        mAdapter.swapCursor(data);
        setContentShown(true);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mAdapter.swapCursor(null);
    }

    @Override
    protected void navigateUp() {
        finish();
    }
}