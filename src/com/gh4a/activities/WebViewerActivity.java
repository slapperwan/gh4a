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
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.content.Loader;
import android.text.Html;
import android.text.TextUtils;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.TextView;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.gh4a.Constants;
import com.gh4a.Gh4Application;
import com.gh4a.LoadingFragmentActivity;
import com.gh4a.ProgressDialogTask;
import com.gh4a.R;
import com.gh4a.loader.CommitCommentListLoader;
import com.gh4a.loader.ContentLoader;
import com.gh4a.loader.LoaderCallbacks;
import com.gh4a.loader.LoaderResult;
import com.gh4a.utils.FileUtils;
import com.gh4a.utils.IntentUtils;
import com.gh4a.utils.StringUtils;
import com.gh4a.utils.ThemeUtils;
import com.gh4a.utils.ToastUtils;

import org.eclipse.egit.github.core.CommitComment;
import org.eclipse.egit.github.core.RepositoryContents;
import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.service.CommitService;
import org.eclipse.egit.github.core.util.EncodingUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public abstract class WebViewerActivity extends LoadingFragmentActivity {
    protected String mRepoOwner;
    protected String mRepoName;
    protected String mPath;
    protected String mSha;
    protected String mRef;
    protected WebView mWebView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        setTheme(Gh4Application.THEME);
        super.onCreate(savedInstanceState);

        Bundle data = getIntent().getExtras();
        mRepoOwner = data.getString(Constants.Repository.OWNER);
        mRepoName = data.getString(Constants.Repository.NAME);
        mPath = data.getString(Constants.Object.PATH);
        mSha = data.getString(Constants.Object.OBJECT_SHA);
        mRef = data.getString(Constants.Object.REF);

        if (hasErrorView()) {
            return;
        }

        setContentView(R.layout.web_viewer);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle(FileUtils.getFileName(mPath));
        actionBar.setSubtitle(mRepoOwner + "/" + mRepoName);
        actionBar.setDisplayHomeAsUpEnabled(true);

        setContentShown(false);

        setupWebView();
    }

    @SuppressWarnings("deprecation")
    @SuppressLint("SetJavaScriptEnabled")
    private void setupWebView() {
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

        mWebView.setBackgroundColor(ThemeUtils.getWebViewBackgroundColor(Gh4Application.THEME));
        setWebViewClient();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        String urlBase = "https://github.com/" + mRepoOwner + "/" + mRepoName;
        String url = urlBase + getUrl();

        switch (item.getItemId()) {
            case R.id.browser:
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                startActivity(browserIntent);
                return true;
            case R.id.share:
                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("text/plain");
                shareIntent.putExtra(Intent.EXTRA_SUBJECT, getShareSubject());
                shareIntent.putExtra(Intent.EXTRA_TEXT, url);
                shareIntent = Intent.createChooser(shareIntent, getString(R.string.share_title));
                startActivity(shareIntent);
                return true;
            case R.id.search:
                doSearch();
                return true;
            case 10:
                Intent historyIntent = new Intent(this, CommitHistoryActivity.class);
                historyIntent.putExtra(Constants.Repository.OWNER, mRepoOwner);
                historyIntent.putExtra(Constants.Repository.NAME, mRepoName);
                historyIntent.putExtra(Constants.Object.PATH, mPath);
                historyIntent.putExtra(Constants.Object.REF, mRef);
                historyIntent.putExtra(Constants.Object.OBJECT_SHA, mSha);
                startActivity(historyIntent);
                return true;
            case 11:
                Intent viewIntent = new Intent(this, FileViewerActivity.class);
                viewIntent.putExtra(Constants.Repository.OWNER, mRepoOwner);
                viewIntent.putExtra(Constants.Repository.NAME, mRepoName);
                viewIntent.putExtra(Constants.Object.PATH, mPath);
                viewIntent.putExtra(Constants.Object.REF, mSha);
                viewIntent.putExtra(Constants.Object.OBJECT_SHA, mSha);
                startActivity(viewIntent);
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

    public abstract void setWebViewClient();

    public abstract String getUrl();

    public abstract String getShareSubject();
}
