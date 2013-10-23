package com.gh4a.loader;

import android.content.Context;
import android.support.v4.content.AsyncTaskLoader;
import android.util.Log;

import com.gh4a.Constants;

public abstract class BaseLoader<T> extends AsyncTaskLoader<LoaderResult<T>> {

    public BaseLoader(Context context) {
        super(context);
    }

    @Override
    public LoaderResult<T> loadInBackground() {
        try {
            T data = doLoadInBackground();
            return new LoaderResult<T>(data);
        } catch (Exception e) {
            Log.e(Constants.LOG_TAG, e.getMessage(), e);
            return new LoaderResult<T>(e);
        }
    }
    
    public abstract T doLoadInBackground() throws Exception;
}
