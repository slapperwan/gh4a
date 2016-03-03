package com.gh4a.loader;

import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;

import com.gh4a.BaseActivity;

public abstract class LoaderCallbacks<T> implements
        android.support.v4.app.LoaderManager.LoaderCallbacks<LoaderResult<T>> {
    public interface ParentCallback {
        BaseActivity getBaseActivity();
        LoaderManager getSupportLoaderManager();
    }

    private ParentCallback mCb;
    private int mId = -1;

    public LoaderCallbacks(ParentCallback cb) {
        mCb = cb;
    }

    public void restartLoading() {
        if (mId >= 0) {
            LoaderManager lm = mCb.getSupportLoaderManager();
            lm.restartLoader(mId, null, this);
        }
    }

    @Override
    public Loader<LoaderResult<T>> onCreateLoader(int id, Bundle args) {
        mId = id;
        mCb.getBaseActivity().registerLoader(this);
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
        mId = -1;
        mCb.getBaseActivity().unregisterLoader(this);
    }

    protected abstract Loader<LoaderResult<T>> onCreateLoader();
    protected abstract void onResultReady(T result);

    protected boolean onError(Exception e) {
        return false;
    }
}
