package com.gh4a.loader;

import android.os.Bundle;
import android.support.v4.content.Loader;

import com.gh4a.BaseActivity;

public abstract class LoaderCallbacks<T> implements
        android.support.v4.app.LoaderManager.LoaderCallbacks<LoaderResult<T>> {
    public interface ParentCallback {
        BaseActivity getBaseActivity();
        void onRefresh();
    }

    private final ParentCallback mCb;

    public LoaderCallbacks(ParentCallback cb) {
        mCb = cb;
    }

    @Override
    public Loader<LoaderResult<T>> onCreateLoader(int id, Bundle args) {
        return onCreateLoader();
    }

    @Override
    public void onLoadFinished(Loader<LoaderResult<T>> loader, LoaderResult<T> result) {
        BaseActivity activity = mCb.getBaseActivity();
        if (result.isSuccess()) {
            onResultReady(result.getData());
        } else if (result.isAuthError()) {
            activity.handleAuthFailureDuringLoad();
        } else if (!onError(result.getException())) {
            activity.handleLoadFailure(result.getException());
        }
    }

    @Override
    public void onLoaderReset(Loader<LoaderResult<T>> loader) {
    }

    protected abstract Loader<LoaderResult<T>> onCreateLoader();
    protected abstract void onResultReady(T result);

    protected boolean onError(Exception e) {
        return false;
    }
}
