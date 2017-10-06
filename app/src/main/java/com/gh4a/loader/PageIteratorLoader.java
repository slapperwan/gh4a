package com.gh4a.loader;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import android.content.Context;
import android.support.v4.content.AsyncTaskLoader;
import android.util.Log;

import com.gh4a.Gh4Application;
import com.meisolsson.githubsdk.model.Page;

public abstract class PageIteratorLoader<T> extends AsyncTaskLoader<LoaderResult<PageIteratorLoader<T>.LoadedPage>> {
    private int mNextPage;
    private ArrayList<T> mPreviouslyLoadedData;

    public class LoadedPage {
        public final Collection<T> results;
        public final boolean hasMoreData;
        private LoadedPage(Collection<T> r, boolean hmd) {
            results = r;
            hasMoreData = hmd;
        }
    }

    public PageIteratorLoader(Context context) {
        super(context);
        mNextPage = 0;
        mPreviouslyLoadedData = new ArrayList<>();
        onContentChanged();
    }

    @Override
    public void onContentChanged() {
        super.onContentChanged();
        mNextPage = 0;
        mPreviouslyLoadedData.clear();
    }

    @Override
    protected void onReset() {
        super.onReset();
        mNextPage = 0;
        mPreviouslyLoadedData.clear();
    }

    @Override
    public LoaderResult<LoadedPage> loadInBackground() {
        if (mNextPage >= 0) {
            try {
                Page<T> page = loadPage(mNextPage);
                mPreviouslyLoadedData = new ArrayList<>(mPreviouslyLoadedData);
                mPreviouslyLoadedData.addAll(page.items());

                Integer next = page.next();
                mNextPage = next != null ? next : -1;
            } catch (Exception e) {
                Log.e(Gh4Application.LOG_TAG, e.getMessage(), e);
                return new LoaderResult<>(e);
            }
        }

        return new LoaderResult<>(new LoadedPage(mPreviouslyLoadedData, mNextPage >= 0));
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

    protected abstract Page<T> loadPage(int page) throws IOException;
}
