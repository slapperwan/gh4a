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
package com.gh4a;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSession;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.SAXException;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.view.MenuItem;
import com.gh4a.adapter.CommonFeedAdapter;
import com.gh4a.feeds.FeedHandler;
import com.gh4a.holder.Feed;

public class WikiListActivity extends BaseSherlockFragmentActivity {
    
    private String mUserLogin;
    private String mRepoName;
    private String mWikiUrl;
    private int page = 1;
    private ListView mListView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        setTheme(Gh4Application.THEME);
        super.onCreate(savedInstanceState);

        mUserLogin = getIntent().getStringExtra(Constants.Repository.REPO_OWNER);
        mRepoName = getIntent().getStringExtra(Constants.Repository.REPO_NAME);
        mWikiUrl = "https://github.com/" + mUserLogin + "/" + mRepoName + "/wiki.atom?page=";
        
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
        CommonFeedAdapter adapter = new CommonFeedAdapter(this, new ArrayList<Feed>(), false, false, R.layout.row_simple_3);
        mListView.setAdapter(adapter);
        mListView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                Feed feed = (Feed) adapterView.getAdapter().getItem(position);
                Intent intent = new Intent().setClass(WikiListActivity.this, WikiActivity.class);
                intent.putExtra(Constants.Blog.TITLE, feed.getTitle());
                intent.putExtra(Constants.Blog.CONTENT, feed.getContent());
                intent.putExtra(Constants.Repository.REPO_OWNER, mUserLogin);
                intent.putExtra(Constants.Repository.REPO_NAME, mRepoName);
                intent.putExtra(Constants.Blog.LINK, feed.getLink());
                startActivity(intent);
            }
        });
        
        new LoadWikiTask(this).execute("true");
    }

    private static class LoadWikiTask extends
            AsyncTask<String, Void, List<Feed>> {

        private WeakReference<WikiListActivity> mTarget;
        private boolean mException;
        private boolean mWikiNotFound;
        
        public LoadWikiTask(WikiListActivity activity) {
            mTarget = new WeakReference<WikiListActivity>(activity);
        }

        @Override
        protected List<Feed> doInBackground(String... params) {
            if (mTarget.get() != null) {
                BufferedInputStream bis = null;
                try {
                    URL url = new URL(mTarget.get().mWikiUrl + mTarget.get().page);
                    HttpsURLConnection request = (HttpsURLConnection) url.openConnection();
                    
                    request.setHostnameVerifier(DO_NOT_VERIFY);
                    request.setRequestMethod("GET");
                    request.setDoOutput(true);
                    
//                    if (ApplicationConstants.CONNECT_TIMEOUT > -1) {
//                        request.setConnectTimeout(ApplicationConstants.CONNECT_TIMEOUT);
//                    }
//
//                    if (ApplicationConstants.READ_TIMEOUT > -1) {
//                        request.setReadTimeout(ApplicationConstants.READ_TIMEOUT);
//                    }
                    request.connect();
                    bis = new BufferedInputStream(request.getInputStream());
                    
                    SAXParserFactory factory = SAXParserFactory.newInstance();
                    SAXParser parser = factory.newSAXParser();
                    FeedHandler handler = new FeedHandler();
                    parser.parse(bis, handler);
                    return handler.getFeeds();
                }
                catch (MalformedURLException e) {
                    Log.e(Constants.LOG_TAG, e.getMessage(), e);
                    mException = true;
                    return null;
                }
                catch (IOException e) {
                    Log.e(Constants.LOG_TAG, e.getMessage(), e);
                    mException = true;
                    return null;
                }
                catch (ParserConfigurationException e) {
                    Log.e(Constants.LOG_TAG, e.getMessage(), e);
                    mException = true;
                    return null;
                }
                catch (SAXException e) {
                    Log.e(Constants.LOG_TAG, e.getMessage(), e);
                    mWikiNotFound = true;
                    mException = true;
                    return null;
                }
                finally {
                    if (bis != null) {
                        try {
                            bis.close();
                        }
                        catch (IOException e) {
                            Log.e(Constants.LOG_TAG, e.getMessage(), e);
                        }
                    }
                }
            }
            else {
                return null;
            }
        }

        @Override
        protected void onPreExecute() {
        }

        @Override
        protected void onPostExecute(List<Feed> result) {
            if (mTarget.get() != null) {
                mTarget.get().hideLoading();
                if (mWikiNotFound) {
                    mTarget.get().getApplicationContext().notFoundMessage(mTarget.get(), "Wiki");
                }
                else if (mException) {
                    mTarget.get().showError();
                }
                else {
                    mTarget.get().fillData(result);
                    mTarget.get().page++;
                }
            }
        }
    }

    private void fillData(List<Feed> result) {
        if (result != null) {
            List<Feed> blogs = ((CommonFeedAdapter) mListView.getAdapter()).getObjects();
            blogs.addAll(result);
            ((CommonFeedAdapter) mListView.getAdapter()).notifyDataSetChanged();
        }
    }
    
    final static HostnameVerifier DO_NOT_VERIFY = new HostnameVerifier() {
        public boolean verify(String hostname, SSLSession session) {
                return true;
        }
    };
    
    @Override
    public boolean setMenuOptionItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                getApplicationContext().openRepositoryInfoActivity(this, mUserLogin, mRepoName, Intent.FLAG_ACTIVITY_CLEAR_TOP);
                return true;     
            default:
                return true;
        }
    }
}
