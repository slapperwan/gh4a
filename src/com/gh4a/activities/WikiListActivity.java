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

import java.util.List;

import org.xml.sax.SAXException;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.Loader;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

import com.actionbarsherlock.app.ActionBar;
import com.gh4a.Constants;
import com.gh4a.Gh4Application;
import com.gh4a.R;
import com.gh4a.adapter.CommonFeedAdapter;
import com.gh4a.holder.Feed;
import com.gh4a.loader.FeedLoader;
import com.gh4a.loader.LoaderCallbacks;
import com.gh4a.loader.LoaderResult;

public class WikiListActivity extends BaseSherlockFragmentActivity {
    
    private String mUserLogin;
    private String mRepoName;
    private ListView mListView;

    private LoaderCallbacks<List<Feed>> mFeedCallback = new LoaderCallbacks<List<Feed>>() {
        @Override
        public Loader<LoaderResult<List<Feed>>> onCreateLoader(int id, Bundle args) {
            String url = "https://github.com/" + mUserLogin + "/" + mRepoName + "/wiki.atom";
            return new FeedLoader(WikiListActivity.this, url);
        }
        @Override
        public void onResultReady(LoaderResult<List<Feed>> result) {
            hideLoading();
            if (result.getException() instanceof SAXException) {
                Gh4Application.get(WikiListActivity.this).notFoundMessage(WikiListActivity.this,
                        getString(R.string.recent_wiki));
            } else if (!result.handleError(WikiListActivity.this)) {
                fillData(result.getData());
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        setTheme(Gh4Application.THEME);
        super.onCreate(savedInstanceState);

        mUserLogin = getIntent().getStringExtra(Constants.Repository.REPO_OWNER);
        mRepoName = getIntent().getStringExtra(Constants.Repository.REPO_NAME);
        
        if (!isOnline()) {
            setErrorView();
            return;
        }
        
        setContentView(R.layout.generic_list);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle(R.string.recent_wiki);
        actionBar.setSubtitle(mUserLogin + "/" + mRepoName);
        actionBar.setDisplayHomeAsUpEnabled(true);
        
        mListView = (ListView) findViewById(R.id.list_view);
        //mListView.setOnScrollListener(new WikiScrollListener(this));
        CommonFeedAdapter adapter = new CommonFeedAdapter(this, false, false, R.layout.row_simple_3);
        mListView.setAdapter(adapter);
        mListView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                Feed feed = (Feed) adapterView.getAdapter().getItem(position);
                openViewer(feed);
            }
        });

        getSupportLoaderManager().initLoader(0, null, mFeedCallback);
    }

    private void fillData(List<Feed> result) {
        if (result != null) {
            CommonFeedAdapter adapter = (CommonFeedAdapter) mListView.getAdapter();
            adapter.addAll(result);
            adapter.notifyDataSetChanged();

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
    }

    private void openViewer(Feed feed) {
        Intent intent = new Intent(this, WikiActivity.class);
        intent.putExtra(Constants.Blog.TITLE, feed.getTitle());
        intent.putExtra(Constants.Blog.CONTENT, feed.getContent());
        intent.putExtra(Constants.Repository.REPO_OWNER, mUserLogin);
        intent.putExtra(Constants.Repository.REPO_NAME, mRepoName);
        intent.putExtra(Constants.Blog.LINK, feed.getLink());
        startActivity(intent);
    }

    @Override
    protected void navigateUp() {
        Gh4Application.get(this).openRepositoryInfoActivity(this,
                mUserLogin, mRepoName, Intent.FLAG_ACTIVITY_CLEAR_TOP);
    }
}
