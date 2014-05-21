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

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.content.Loader;
import android.util.Pair;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.gh4a.Constants;
import com.gh4a.Gh4Application;
import com.gh4a.LoadingFragmentActivity;
import com.gh4a.R;
import com.gh4a.loader.GistLoader;
import com.gh4a.loader.LoaderCallbacks;
import com.gh4a.loader.LoaderResult;
import com.gh4a.utils.IntentUtils;
import com.gh4a.utils.StringUtils;
import com.gh4a.utils.ThemeUtils;

import org.eclipse.egit.github.core.Gist;
import org.eclipse.egit.github.core.GistFile;

public class GistViewerActivity extends LoadingFragmentActivity {
    private String mUserLogin;
    private String mFileName;
    private String mGistId;
    private GistFile mGistFile;

    private WebView mWebView;

    private LoaderCallbacks<Gist> mGistCallback = new LoaderCallbacks<Gist>() {
        @Override
        public Loader<LoaderResult<Gist>> onCreateLoader(int id, Bundle args) {
            return new GistLoader(GistViewerActivity.this, mGistId);
        }
        @Override
        public void onResultReady(LoaderResult<Gist> result) {
            boolean success = !result.handleError(GistViewerActivity.this);
            if (success) {
                mGistFile = result.getData().getFiles().get(mFileName);
                fillData(mGistFile.getContent(), true);
            }
            setContentEmpty(!success);
            setContentShown(true);
        }
    };

    private WebViewClient mWebViewClient = new WebViewClient() {
        @Override
        public void onPageFinished(WebView webView, String url) {
            setContentShown(true);
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(intent);
            return true;
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        setTheme(Gh4Application.THEME);
        super.onCreate(savedInstanceState);

        mUserLogin = getIntent().getExtras().getString(Constants.User.LOGIN);
        mFileName = getIntent().getExtras().getString(Constants.Gist.FILENAME);
        mGistId = getIntent().getExtras().getString(Constants.Gist.ID);

        if (hasErrorView()) {
            return;
        }

        setContentView(R.layout.web_viewer);
        setContentShown(false);

        ActionBar mActionBar = getSupportActionBar();
        mActionBar.setTitle(mFileName);
        mActionBar.setDisplayHomeAsUpEnabled(true);

        getSupportLoaderManager().initLoader(0, null, mGistCallback);
    }

    @SuppressWarnings("deprecation")
    @SuppressLint("SetJavaScriptEnabled")
    private void fillData(String data, boolean highlight) {
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

        Pair<String, Boolean> result = StringUtils.highlightSyntax(data, highlight, mFileName, null, null, null);
        String highlightedText = result.first;
        boolean themed = result.second;

        if(themed){
            mWebView.setBackgroundColor(ThemeUtils.getWebViewBackgroundColor(Gh4Application.THEME));
        }

        mWebView.loadDataWithBaseURL("file:///android_asset/", highlightedText, "text/html", "utf-8", "");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getSupportMenuInflater();
        inflater.inflate(R.menu.download_menu, menu);

        menu.removeItem(R.id.download);
        menu.removeItem(R.id.share);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
            menu.removeItem(R.id.search);
        }

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    protected void navigateUp() {
        IntentUtils.openGistActivity(this, mUserLogin, mGistId, Intent.FLAG_ACTIVITY_CLEAR_TOP);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.browser:
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(mGistFile.getRawUrl()));
                startActivity(browserIntent);
                return true;
            case R.id.search:
                doSearch();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("deprecation")
    @TargetApi(11)
    private void doSearch() {
        if (mWebView != null) {
            mWebView.showFindDialog(null, true);
        }
    }
}