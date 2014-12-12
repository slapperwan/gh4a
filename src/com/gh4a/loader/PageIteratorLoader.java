package com.gh4a.loader;

import java.util.ArrayList;
import java.util.Collection;

import org.eclipse.egit.github.core.client.PageIterator;

import android.content.Context;
import android.support.v4.content.AsyncTaskLoader;
import android.util.Log;

import com.gh4a.Constants;

public class PageIteratorLoader<T> extends AsyncTaskLoader<Collection<T>> {
    private PageIterator<T> mPageIterator;
    private ArrayList<T> mPreviouslyLoadedData;

    public PageIteratorLoader(Context context, PageIterator<T> pageIterator) {
        super(context);
        mPageIterator = pageIterator;
        mPreviouslyLoadedData = new ArrayList<>();
        onContentChanged();
    }

    public boolean hasMoreData() {
        return mPageIterator.hasNext();
    }

    @Override
    public void onContentChanged() {
        super.onContentChanged();
        mPageIterator.reset();
        mPreviouslyLoadedData.clear();
    }

    @Override
    protected void onReset() {
        super.onReset();
        mPageIterator.reset();
        mPreviouslyLoadedData.clear();
    }

    @Override
    public Collection<T> loadInBackground() {
        if (mPageIterator.hasNext()) {
            try {
                Collection<T> newData = mPageIterator.next();
                mPreviouslyLoadedData = new ArrayList<>(mPreviouslyLoadedData);
                mPreviouslyLoadedData.addAll(newData);
            } catch (Exception e) {
                Log.e(Constants.LOG_TAG, e.getMessage(), e);
            }
        }

        return mPreviouslyLoadedData;
    }

    @Override
    protected void onStartLoading() {
        if (takeContentChanged()) {
            forceLoad();
        }
    }

    @Override
    protected void onStopLoading() {
        cancelLoad();
    }
}
