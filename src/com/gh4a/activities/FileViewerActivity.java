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

import org.eclipse.egit.github.core.RepositoryContents;
import org.eclipse.egit.github.core.util.EncodingUtils;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.content.Loader;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.gh4a.Constants;
import com.gh4a.Gh4Application;
import com.gh4a.R;
import com.gh4a.loader.ContentLoader;
import com.gh4a.loader.LoaderCallbacks;
import com.gh4a.loader.LoaderResult;
import com.gh4a.utils.FileUtils;
import com.gh4a.utils.StringUtils;

public class FileViewerActivity extends BaseSherlockFragmentActivity {
    protected String mRepoOwner;
    protected String mRepoName;
    private String mPath;
    private String mRef;
    private String mSha;
    private String mName;
    private RepositoryContents mContent;

    private WebView mWebView;

    private WebViewClient mWebViewClient = new WebViewClient() {
        @Override
        public void onPageFinished(WebView webView, String url) {
            hideLoading();
        }
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(intent);
            return true;
        }
    };

    private LoaderCallbacks<List<RepositoryContents>> mFileCallback =
            new LoaderCallbacks<List<RepositoryContents>>() {
        @Override
        public Loader<LoaderResult<List<RepositoryContents>>> onCreateLoader(int id, Bundle args) {
            return new ContentLoader(FileViewerActivity.this, mRepoOwner, mRepoName, mPath, mRef);
        }
        @Override
        public void onResultReady(LoaderResult<List<RepositoryContents>> result) {
            hideLoading();
            if (!isLoaderError(result)) {
                List<RepositoryContents> data = result.getData();
                if (data != null && !data.isEmpty()) {
                    mContent = data.get(0);
                    fillData(true);
                }
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        setTheme(Gh4Application.THEME);
        super.onCreate(savedInstanceState);

        mRepoOwner = getIntent().getStringExtra(Constants.Repository.REPO_OWNER);
        mRepoName = getIntent().getStringExtra(Constants.Repository.REPO_NAME);
        mPath = getIntent().getStringExtra(Constants.Object.PATH);
        mRef = getIntent().getStringExtra(Constants.Object.REF);
        mSha = getIntent().getStringExtra(Constants.Object.OBJECT_SHA);
        mName = getIntent().getStringExtra(Constants.Object.NAME);
        
        if (!isOnline()) {
            setErrorView();
            return;
        }
        
        setContentView(R.layout.web_viewer);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle(mName);
        actionBar.setSubtitle(mRepoOwner + "/" + mRepoName);
        actionBar.setDisplayHomeAsUpEnabled(true);
        
        showLoading();
        getSupportLoaderManager().initLoader(0, null, mFileCallback);
        getSupportLoaderManager().getLoader(0).forceLoad();
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void fillData(boolean highlight) {
        String data = new String(EncodingUtils.fromBase64(mContent.getContent()));
        mWebView = (WebView) findViewById(R.id.web_view);

        WebSettings s = mWebView.getSettings();
        s.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.NORMAL);
        s.setAllowFileAccess(true);
        s.setBuiltInZoomControls(true);
        s.setLightTouchEnabled(true);
        s.setLoadsImagesAutomatically(true);
        s.setSupportZoom(true);
        s.setSupportMultipleWindows(true);
        s.setJavaScriptEnabled(true);
        s.setUseWideViewPort(true);

        mWebView.setWebViewClient(mWebViewClient);
        if (FileUtils.isImage(mName)) {
            String htmlImage = StringUtils.highlightImage("https://github.com/" + mRepoOwner + "/" + mRepoName + "/raw/" + mRef + "/" + mPath);
            mWebView.loadDataWithBaseURL("file:///android_asset/", htmlImage, "text/html", "utf-8", "");
        }
        else {
            String highlighted = StringUtils.highlightSyntax(data, highlight, mName);
            mWebView.loadDataWithBaseURL("file:///android_asset/", highlighted, "text/html", "utf-8", "");
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getSupportMenuInflater();
        inflater.inflate(R.menu.download_menu, menu);
        
        if (Gh4Application.THEME != R.style.LightTheme) {
            menu.getItem(0).setIcon(R.drawable.download_dark);
            menu.getItem(1).setIcon(R.drawable.web_site_dark);
            menu.getItem(2).setIcon(R.drawable.action_search_dark);
            menu.getItem(3).setIcon(R.drawable.social_share_dark);
        }
        
        menu.removeItem(R.id.download);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
            menu.removeItem(R.id.search);
        }

        menu.add(0, 10, Menu.NONE, getString(R.string.history))
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    protected void navigateUp() {
        Gh4Application.get(this).openRepositoryInfoActivity(this,
                mRepoOwner, mRepoName, Intent.FLAG_ACTIVITY_CLEAR_TOP);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        String blobUrl = "https://github.com/" + mRepoOwner + "/" + mRepoName + "/blob/" + mRef + "/" + mPath;
        switch (item.getItemId()) {
            case R.id.browser:
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(blobUrl));
                startActivity(browserIntent);
                return true;
            case R.id.share:
                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("text/plain");
                shareIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.share_file_subject, mName, mRepoOwner + "/" + mRepoName));
                shareIntent.putExtra(Intent.EXTRA_TEXT,  blobUrl);
                shareIntent = Intent.createChooser(shareIntent, getString(R.string.share_title));
                startActivity(shareIntent);
                return true;
            case R.id.search:
                doSearch();
                return true;
            case 10:
                Intent intent = new Intent().setClass(FileViewerActivity.this, CommitHistoryActivity.class);
                intent.putExtra(Constants.Repository.REPO_OWNER, mRepoOwner);
                intent.putExtra(Constants.Repository.REPO_NAME, mRepoName);
                intent.putExtra(Constants.Object.PATH, mPath);
                intent.putExtra(Constants.Object.REF, mRef);
                intent.putExtra(Constants.Object.OBJECT_SHA, mSha);
                startActivity(intent);
        }
        return super.onOptionsItemSelected(item);
    }

    @TargetApi(11)
    private void doSearch() {
        if (mWebView != null) {
            mWebView.showFindDialog(null, true);
        }
    }
}
