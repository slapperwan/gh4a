package com.gh4a;

import android.os.Bundle;

import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.client.PageIterator;
import org.eclipse.egit.github.core.client.PagedRequest;

public class PageIteratorWithSaveableState<V> extends PageIterator<V> {
    private static final String STATE_KEY_NEXT_PAGE = "next_page";
    private static final String STATE_KEY_LAST_PAGE = "last_page";
    private static final String STATE_KEY_NEXT = "next";
    private static final String STATE_KEY_LAST = "last";
    private static final String STATE_KEY_PAGE_SIZE = "page_size";

    public PageIteratorWithSaveableState(PagedRequest<V> request, GitHubClient client) {
        super(request, client);
    }

    public Bundle saveState() {
        Bundle state = new Bundle();
        state.putInt(STATE_KEY_NEXT_PAGE, nextPage);
        state.putInt(STATE_KEY_LAST_PAGE, lastPage);
        state.putInt(STATE_KEY_PAGE_SIZE, request.getPageSize());
        state.putString(STATE_KEY_NEXT, next);
        state.putString(STATE_KEY_LAST, last);
        return state;
    }

    public void restoreState(Bundle state) {
        if (state == null || state.getInt(STATE_KEY_PAGE_SIZE) != request.getPageSize()) {
            return;
        }
        nextPage = state.getInt(STATE_KEY_NEXT_PAGE, nextPage);
        lastPage = state.getInt(STATE_KEY_NEXT_PAGE, lastPage);
        String newNext = state.getString(STATE_KEY_NEXT);
        String newLast = state.getString(STATE_KEY_NEXT);
        if (newNext != null) {
            next = newNext;
        }
        if (newLast != null) {
            last = newLast;
        }
    }
}
