package com.gh4a.loader;

import java.util.List;

import org.eclipse.egit.github.core.client.PageIterator;

import android.content.Context;
import android.support.v4.content.AsyncTaskLoader;

public class PageIteratorLoader<T> extends AsyncTaskLoader<List<T>> {

    private PageIterator<T> mPageIterator;
    
    public PageIteratorLoader(Context context, PageIterator<T> pageIterator) {
        super(context);
        mPageIterator = pageIterator;
    }
    
    @Override
    public List<T> loadInBackground() {
        if (mPageIterator.hasNext()) {
            return (List<T>) mPageIterator.next();
        }
        else {
            return null;
        }
    }

}
