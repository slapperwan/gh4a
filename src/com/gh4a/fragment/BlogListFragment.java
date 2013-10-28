/*
 * Copyright 2011 Azwan Adli Abdullah
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.gh4a.fragment;

import java.util.List;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

import com.gh4a.Constants;
import com.gh4a.R;
import com.gh4a.activities.BlogActivity;
import com.gh4a.adapter.CommonFeedAdapter;
import com.gh4a.holder.Feed;
import com.gh4a.loader.FeedLoader;
import com.gh4a.loader.LoaderCallbacks;
import com.gh4a.loader.LoaderResult;

public class BlogListFragment extends BaseFragment implements OnItemClickListener {
    private static final String BLOG = "https://github.com/blog.atom";

    private ListView mListView;
    private CommonFeedAdapter mAdapter;

    private LoaderCallbacks<List<Feed>> mFeedCallback = new LoaderCallbacks<List<Feed>>() {

        @Override
        public Loader<LoaderResult<List<Feed>>> onCreateLoader(int id, Bundle args) {
            return new FeedLoader(getActivity(), BLOG);
        }

        @Override
        public void onResultReady(LoaderResult<List<Feed>> result) {
            if (result.getData() != null) {
                fillData(result.getData());
            }
        }
    };

    public static BlogListFragment newInstance() {
        BlogListFragment f = new BlogListFragment();
        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.generic_list, container, false);
        mListView = (ListView) v.findViewById(R.id.list_view);
        return v;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mAdapter = new CommonFeedAdapter(getSherlockActivity());
        mListView.setAdapter(mAdapter);
        mListView.setOnItemClickListener(this);

        getLoaderManager().initLoader(0, null, mFeedCallback).forceLoad();
    }

    private void fillData(List<Feed> result) {
        if (result != null) {
            mAdapter.addAll(result);
            mAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
        Feed blog = mAdapter.getItem(position);
        Intent intent = new Intent(getSherlockActivity(), BlogActivity.class);
        intent.putExtra(Constants.Blog.TITLE, blog.getTitle());
        intent.putExtra(Constants.Blog.CONTENT, blog.getContent());
        intent.putExtra(Constants.Blog.LINK, blog.getLink());
        startActivity(intent);
    }
}