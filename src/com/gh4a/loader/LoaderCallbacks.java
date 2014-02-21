package com.gh4a.loader;

import android.os.Bundle;
import android.support.v4.content.Loader;

public abstract class LoaderCallbacks<T> implements
        android.support.v4.app.LoaderManager.LoaderCallbacks<LoaderResult<T>> {
    public LoaderCallbacks() {
    }

    @Override
    public abstract Loader<LoaderResult<T>> onCreateLoader(int id, Bundle args);

    @Override
    public void onLoadFinished(Loader<LoaderResult<T>> loader, LoaderResult<T> result) {
        onResultReady(result);
    }

    @Override
    public void onLoaderReset(Loader<LoaderResult<T>> loader) {
    }

    public abstract void onResultReady(LoaderResult<T> result);
}
