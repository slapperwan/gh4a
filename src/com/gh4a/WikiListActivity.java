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
import java.util.HashMap;
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
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView.OnItemClickListener;

import com.gh4a.adapter.CommonFeedAdapter;
import com.gh4a.feeds.FeedHandler;
import com.gh4a.holder.BreadCrumbHolder;
import com.gh4a.holder.Feed;
import com.github.api.v2.services.constant.ApplicationConstants;

public class WikiListActivity extends BaseActivity {
    
    private String mUserLogin;
    private String mRepoName;
    private String mWikiUrl;
    private int page = 1;
    private LoadingDialog mLoadingDialog;
    private boolean mLoading;
    private boolean mReload;
    private ListView mListView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.generic_list);
        setUpActionBar();

        mUserLogin = getIntent().getStringExtra(Constants.Repository.REPO_OWNER);
        mRepoName = getIntent().getStringExtra(Constants.Repository.REPO_NAME);
        mWikiUrl = "https://github.com/" + mUserLogin + "/" + mRepoName + "/wiki.atom?page=";
        
        setBreadCrumb();
        
        mListView = (ListView) findViewById(R.id.list_view);
        //mListView.setOnScrollListener(new WikiScrollListener(this));
        CommonFeedAdapter adapter = new CommonFeedAdapter(this, new ArrayList<Feed>(), false, false);
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

    protected void setBreadCrumb() {
        BreadCrumbHolder[] breadCrumbHolders = new BreadCrumbHolder[2];

        // common data
        HashMap<String, String> data = new HashMap<String, String>();
        data.put(Constants.User.USER_LOGIN, mUserLogin);
        data.put(Constants.Repository.REPO_NAME, mRepoName);

        // User
        BreadCrumbHolder b = new BreadCrumbHolder();
        b.setLabel(mUserLogin);
        b.setTag(Constants.User.USER_LOGIN);
        b.setData(data);
        breadCrumbHolders[0] = b;

        // Repo
        b = new BreadCrumbHolder();
        b.setLabel(mRepoName);
        b.setTag(Constants.Repository.REPO_NAME);
        b.setData(data);
        breadCrumbHolders[1] = b;

        createBreadcrumb(getResources().getString(R.string.recent_wiki), breadCrumbHolders);
    }
    
    private static class LoadWikiTask extends
            AsyncTask<String, Void, List<Feed>> {

        /** The target. */
        private WeakReference<WikiListActivity> mTarget;

        /** The exception. */
        private boolean mException;

        /** The hide main view. */
        private boolean mHideMainView;
        
        private boolean mWikiNotFound;
        
        /**
         * Instantiates a new load tree list task.
         * 
         * @param activity the activity
         */
        public LoadWikiTask(WikiListActivity activity) {
            mTarget = new WeakReference<WikiListActivity>(activity);
        }

        /*
         * (non-Javadoc)
         * @see android.os.AsyncTask#doInBackground(Params[])
         */
        @Override
        protected List<Feed> doInBackground(String... params) {
            if (mTarget.get() != null) {
                this.mHideMainView = Boolean.valueOf(params[0]);
                BufferedInputStream bis = null;
                try {
                    URL url = new URL(mTarget.get().mWikiUrl + mTarget.get().page);
                    HttpsURLConnection request = (HttpsURLConnection) url.openConnection();
                    
                    request.setHostnameVerifier(DO_NOT_VERIFY);
                    request.setRequestMethod("GET");
                    request.setDoOutput(true);
                    
                    if (ApplicationConstants.CONNECT_TIMEOUT > -1) {
                        request.setConnectTimeout(ApplicationConstants.CONNECT_TIMEOUT);
                    }

                    if (ApplicationConstants.READ_TIMEOUT > -1) {
                        request.setReadTimeout(ApplicationConstants.READ_TIMEOUT);
                    }
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

        /*
         * (non-Javadoc)
         * @see android.os.AsyncTask#onPreExecute()
         */
        @Override
        protected void onPreExecute() {
            if (mTarget.get() != null) {
                if (mTarget.get().page == 1) {
                    mTarget.get().mLoadingDialog = LoadingDialog.show(mTarget.get(), true, true,
                            mHideMainView);
                }
                else {
                    TextView loadingView = (TextView) mTarget.get().findViewById(R.id.tv_loading);
                    loadingView.setVisibility(View.VISIBLE);
                }
            }
        }

        /*
         * (non-Javadoc)
         * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
         */
        @Override
        protected void onPostExecute(List<Feed> result) {
            if (mTarget.get() != null) {
                if (mWikiNotFound) {
                    mTarget.get().getApplicationContext().notFoundMessage(mTarget.get(), "Wiki");
                    if (mTarget.get().mLoadingDialog != null && mTarget.get().mLoadingDialog.isShowing()) {
                        mTarget.get().mLoadingDialog.dismiss();
                    }
                }
                else if (mException) {
                    mTarget.get().showError();
                }
                else {
                    if (mTarget.get().mLoadingDialog != null && mTarget.get().mLoadingDialog.isShowing()) {
                        mTarget.get().mLoadingDialog.dismiss();
                    }
        
                    TextView loadingView = (TextView) mTarget.get().findViewById(R.id.tv_loading);
                    loadingView.setVisibility(View.GONE);
                    mTarget.get().fillData(result);
                    mTarget.get().page++;
                    mTarget.get().mLoading = false;
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
    
    private static class WikiScrollListener implements OnScrollListener {

        /** The target. */
        private WeakReference<WikiListActivity> mTarget;

        /**
         * Instantiates a new repository scoll listener.
         *
         * @param activity the activity
         * @param searchKey the search key
         * @param language the language
         */
        public WikiScrollListener(WikiListActivity activity) {
            super();
            mTarget = new WeakReference<WikiListActivity>(activity);
        }

        /*
         * (non-Javadoc)
         * @see
         * android.widget.AbsListView.OnScrollListener#onScrollStateChanged(
         * android.widget.AbsListView, int)
         */
        @Override
        public void onScrollStateChanged(AbsListView view, int scrollState) {
            if (mTarget.get() != null) {
                if (mTarget.get().mReload && scrollState == SCROLL_STATE_IDLE) {
                    new LoadWikiTask(mTarget.get()).execute("false");
                    mTarget.get().mReload = false;
                }
            }
        }

        /*
         * (non-Javadoc)
         * @see
         * android.widget.AbsListView.OnScrollListener#onScroll(android.widget
         * .AbsListView, int, int, int)
         */
        @Override
        public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount,
                int totalItemCount) {
            if (mTarget.get() != null) {
                if (!mTarget.get().mLoading && firstVisibleItem != 0
                        && ((firstVisibleItem + visibleItemCount) == totalItemCount)) {
                    mTarget.get().mReload = true;
                    mTarget.get().mLoading = true;
                }
            }
        }
    }
    
    final static HostnameVerifier DO_NOT_VERIFY = new HostnameVerifier() {
        public boolean verify(String hostname, SSLSession session) {
                return true;
        }
    };
}
