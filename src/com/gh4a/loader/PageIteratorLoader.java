package com.gh4a.loader;

import java.util.List;

import org.eclipse.egit.github.core.client.PageIterator;

import android.content.Context;
import android.support.v4.content.AsyncTaskLoader;
import android.util.Log;

import com.gh4a.Constants;

public class PageIteratorLoader<T> extends AsyncTaskLoader<List<T>> {

    private PageIterator<T> mPageIterator;
    
    public PageIteratorLoader(Context context, PageIterator<T> pageIterator) {
        super(context);
        mPageIterator = pageIterator;
    }
    
    @Override
    public List<T> loadInBackground() {
        if (mPageIterator.hasNext()) {
            try {
                return (List<T>) mPageIterator.next();
            }
            catch (Exception e) {
                Log.e(Constants.LOG_TAG, e.getMessage(), e);
                return null;
            }
        }
        else {
            return null;
        }
    }

}
