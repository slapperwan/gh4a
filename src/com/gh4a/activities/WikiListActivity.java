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
package com.gh4a.activities;

import java.io.FileNotFoundException;
import java.util.List;

import org.xml.sax.SAXException;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.Loader;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

import com.gh4a.BaseActivity;
import com.gh4a.Constants;
import com.gh4a.R;
import com.gh4a.adapter.CommonFeedAdapter;
import com.gh4a.adapter.RootAdapter;
import com.gh4a.holder.Feed;
import com.gh4a.loader.FeedLoader;
import com.gh4a.loader.LoaderCallbacks;
import com.gh4a.loader.LoaderResult;
import com.gh4a.utils.IntentUtils;
import com.gh4a.utils.UiUtils;
import com.gh4a.widget.DividerItemDecoration;
import com.gh4a.widget.SwipeRefreshLayout;

public class WikiListActivity extends BaseActivity
        implements SwipeRefreshLayout.ChildScrollDelegate, RootAdapter.OnItemClickListener<Feed> {
    private String mUserLogin;
    private String mRepoName;

    private RecyclerView mRecyclerView;
    private CommonFeedAdapter mAdapter;

    private LoaderCallbacks<List<Feed>> mFeedCallback = new LoaderCallbacks<List<Feed>>(this) {
        @Override
        protected Loader<LoaderResult<List<Feed>>> onCreateLoader() {
            String url = "https://github.com/" + mUserLogin + "/" + mRepoName + "/wiki.atom";
            return new FeedLoader(WikiListActivity.this, url);
        }

        @Override
        protected void onResultReady(List<Feed> result) {
            fillData(result);
            setContentEmpty(false);
            setContentShown(true);
        }

        @Override
        protected boolean onError(Exception e) {
            if (e instanceof SAXException || e instanceof FileNotFoundException) {
                setContentEmpty(true);
                setContentShown(true);
                return true;
            }
            return false;
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.generic_list);
        setEmptyText(R.string.no_wiki_updates_found);
        setContentShown(false);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle(R.string.recent_wiki);
        actionBar.setSubtitle(mUserLogin + "/" + mRepoName);
        actionBar.setDisplayHomeAsUpEnabled(true);

        mAdapter = new CommonFeedAdapter(this, false);
        mAdapter.setOnItemClickListener(this);

        mRecyclerView = (RecyclerView) findViewById(R.id.list);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mRecyclerView.addItemDecoration(new DividerItemDecoration(this));
        mRecyclerView.setAdapter(mAdapter);
        setChildScrollDelegate(this);

        getSupportLoaderManager().initLoader(0, null, mFeedCallback);
    }

    @Override
    protected void onInitExtras(Bundle extras) {
        super.onInitExtras(extras);
        mUserLogin = extras.getString(Constants.Repository.OWNER);
        mRepoName = extras.getString(Constants.Repository.NAME);
    }

    @Override
    public boolean canChildScrollUp() {
        return UiUtils.canViewScrollUp(mRecyclerView);
    }

    @Override
    public void onRefresh() {
        mAdapter.clear();
        setContentShown(false);
        setContentEmpty(false);
        getSupportLoaderManager().getLoader(0).onContentChanged();
        super.onRefresh();
    }

    @Override
    public void onItemClick(Feed feed) {
        openViewer(feed);
    }

    private void fillData(List<Feed> result) {
        mAdapter.clear();
        if (result == null) {
            return;
        }

        mAdapter.addAll(result);

        String initialPage = getIntent().getStringExtra(Constants.Object.OBJECT_SHA);
        if (initialPage != null) {
            for (Feed feed : result) {
                if (initialPage.equals(feed.getId())) {
                    openViewer(feed);
                    break;
                }
            }
        }
    }

    private void openViewer(Feed feed) {
        Intent intent = new Intent(this, WikiActivity.class);
        intent.putExtra(Constants.Blog.TITLE, feed.getTitle());
        intent.putExtra(Constants.Blog.CONTENT, feed.getContent());
        intent.putExtra(Constants.Repository.OWNER, mUserLogin);
        intent.putExtra(Constants.Repository.NAME, mRepoName);
        intent.putExtra(Constants.Blog.LINK, feed.getLink());
        startActivity(intent);
    }

    @Override
    protected Intent navigateUp() {
        return IntentUtils.getRepoActivityIntent(this, mUserLogin, mRepoName, null);
    }
}
