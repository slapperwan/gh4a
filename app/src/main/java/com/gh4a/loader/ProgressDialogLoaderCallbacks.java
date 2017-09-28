package com.gh4a.loader;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.content.Loader;

import com.gh4a.R;
import com.gh4a.utils.UiUtils;

public abstract class ProgressDialogLoaderCallbacks<T> extends LoaderCallbacks<T> {
    private final Context mContext;
    private Dialog mProgressDialog;

    public ProgressDialogLoaderCallbacks(Context context, ParentCallback cb) {
        super(cb);
        mContext = context;
    }

    @Override
    public Loader<LoaderResult<T>> onCreateLoader(int id, Bundle args) {
        Loader<LoaderResult<T>> result = super.onCreateLoader(id, args);

        mProgressDialog = UiUtils.createProgressDialog(mContext, R.string.loading_msg);
        mProgressDialog.show();

        return result;
    }

    @Override
    public void onLoadFinished(Loader<LoaderResult<T>> loader, LoaderResult<T> result) {
        super.onLoadFinished(loader, result);
        closeProgressDialog();
    }

    @Override
    public void onLoaderReset(Loader<LoaderResult<T>> loader) {
        super.onLoaderReset(loader);
        closeProgressDialog();
    }

    private void closeProgressDialog() {
        if (mProgressDialog != null) {
            mProgressDialog.dismiss();
            mProgressDialog = null;
        }
    }
}
