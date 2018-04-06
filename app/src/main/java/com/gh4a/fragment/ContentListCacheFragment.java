package com.gh4a.fragment;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;

import com.meisolsson.githubsdk.model.Content;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ContentListCacheFragment extends Fragment {
    private final Map<String, ArrayList<Content>> mContentCache =
            new LinkedHashMap<String, ArrayList<Content>>() {
                private static final long serialVersionUID = -2379579224736389357L;
                private static final int MAX_CACHE_ENTRIES = 100;

                @Override
                protected boolean removeEldestEntry(Map.Entry<String, ArrayList<Content>> eldest) {
                    return size() > MAX_CACHE_ENTRIES;
                }
            };

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    public void addToCache(String path, List<Content> contents) {
        mContentCache.put(path, new ArrayList<>(contents));
    }

    public ArrayList<Content> getFromCache(String path) {
        return mContentCache.get(path);
    }

    public void clear() {
        mContentCache.clear();
    }
}
